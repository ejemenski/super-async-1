package org.superasync;

import java.util.concurrent.*;

public class SuperFuture<V> implements Future<V>, Completable.Cancellable {

    private static final int WAITING = 0, SET = 1, EXCEPTIONAL = 2, CANCELLED = 3;

    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    private final PublisherInner publisher = new PublisherInner();
    private final Callback<V> callbackInterface = new CallbackInterface();
    private final org.superasync.Cancellable cancellationDelegate;

    SuperFuture(org.superasync.Cancellable cancellationDelegate) {
        this.cancellationDelegate = cancellationDelegate;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (publisher.compareAndPublish(StateHolder.WAITING, StateHolder.CANCELLED)) {
            done();
            cancellationDelegate.cancel(mayInterruptIfRunning);
            return true;
        }
        return false;
    }

    @Override
    public boolean isCancelled() {
        return publisher.getValue().state == CANCELLED;
    }

    @Override
    public boolean isDone() {
        return publisher.getValue().state != WAITING;
    }

    private void set(V value) {
        if (publisher.compareAndPublish(StateHolder.WAITING, StateHolder.newResult(value))) {
            done();
        }
    }

    private void setException(Throwable e) {
        if (publisher.compareAndPublish(StateHolder.WAITING, StateHolder.newExceptional(e))) {
            done();
        }
    }

    Callback<V> asCallback() {
        return callbackInterface;
    }

    private void done() {
        countDownLatch.countDown();
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        countDownLatch.await();
        return report();
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (!countDownLatch.await(timeout, unit)) {
            throw new TimeoutException();
        }
        return report();
    }

    private V report() throws ExecutionException {
        StateHolder stateHolder = publisher.getValue();
        switch (stateHolder.state) {
            case SET:
                return stateHolder.getResult();
            case EXCEPTIONAL:
                throw new ExecutionException(stateHolder.getException());
            case CANCELLED:
                throw new CancellationException();
        }
        throw new IllegalStateException("cannot report in state WAITING");
    }


    public Observation<V> observe(ResultConsumer<V> resultConsumer) {
        return observe(resultConsumer, null);
    }

    public Observation<V> observe(ResultConsumer<V> resultConsumer, ErrorConsumer errorConsumer) {
        return observe(resultConsumer, errorConsumer, null);
    }

    public Observation<V> observe(ResultConsumer<V> resultConsumer, ErrorConsumer errorConsumer, Executor observingExecutor) {
        Observer<V> observer = new Observer<V>(
                observingExecutor != null ? observingExecutor : ExecutorProviderStaticRef.getExecutorProvider().defaultObserving(),
                resultConsumer,
                errorConsumer);
        Removable w = publisher.subscribe(observer);
        return new Observation<V>(w, this);
    }

    private class PublisherInner extends TypedPublisher<StateHolder, Observer<V>> {

        PublisherInner() {
            super(StateHolder.WAITING);
        }

        @Override
        void notifySubscriber(StateHolder stateHolder, Wrapper wrapper) {
            Observer<V> observer = wrapper.getObject();
            switch (stateHolder.state) {
                case SET:
                    //noinspection unchecked
                    observer.onResult((V) stateHolder.getResult());
                    break;
                case EXCEPTIONAL:
                    observer.onError((Throwable) stateHolder.getException());
                    break;
            }
            wrapper.remove();
        }
    }

    private class CallbackInterface implements Callback<V> {
        @Override
        public void onResult(V result) {
            set(result);
        }

        @Override
        public void onError(Throwable e) {
            setException(e);
        }
    }

    private static class StateHolder {
        final static StateHolder WAITING = new StateHolder(SuperFuture.WAITING, null);
        final static StateHolder CANCELLED = new StateHolder(SuperFuture.CANCELLED, null);

        static StateHolder newResult(Object result) {
            return new StateHolder(SET, result);
        }

        static StateHolder newExceptional(Throwable e) {
            return new StateHolder(EXCEPTIONAL, e);
        }

        private final Object outcome;
        private final int state;

        private StateHolder(int state, Object outcome) {
            this.state = state;
            this.outcome = outcome;
        }

        <V> V getResult() {
            if (state == SET) {
                //noinspection unchecked
                return (V) outcome;
            }
            return null;
        }

        Throwable getException() {
            if (state == EXCEPTIONAL) {
                return (Throwable) outcome;
            }
            return null;
        }
    }
}
