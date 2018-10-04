package org.superasync;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SuperFuture<V> implements Future<V>, Completable.Cancellable {

    private static final int WAITING = 0, SET = 1, EXCEPTIONAL = 2, CANCELLED = 3;

    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    private final AtomicInteger state = new AtomicInteger(WAITING);
    private Object result;
    private final PublisherInner publisher = new PublisherInner();
    private final Callback<V> callbackInterface = new CallbackInterface();
    private final org.superasync.Cancellable cancellationDelegate;

    SuperFuture(org.superasync.Cancellable cancellationDelegate) {
        this.cancellationDelegate = cancellationDelegate;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (state.compareAndSet(WAITING, CANCELLED)) {
            done();
            cancellationDelegate.cancel(mayInterruptIfRunning);
            return true;
        }
        return false;
    }

    @Override
    public boolean isCancelled() {
        return state.get() == CANCELLED;
    }

    @Override
    public boolean isDone() {
        return state.get() != WAITING;
    }

    private void set(V value) {
        if (state.compareAndSet(WAITING, SET)) {
            this.result = value;
            done();
        }
    }

    private void setException(Throwable e) {
        if (state.compareAndSet(WAITING, EXCEPTIONAL)) {
            this.result = e;
            done();
        }
    }

    Callback<V> asCallback() {
        return callbackInterface;
    }

    private void done() {
        countDownLatch.countDown();
        publisher.publishRevision(state.get());
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
        switch (state.get()) {
            case SET:
                //noinspection unchecked
                return (V) result;
            case EXCEPTIONAL:
                throw new ExecutionException((Throwable) result);
            case CANCELLED:
                throw new CancellationException();
        }
        throw new IllegalStateException("cannot report in state WAITING" + state);
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

    private class PublisherInner extends Publisher<Observer<V>> {

        PublisherInner() {
            super(WAITING);
        }

        @Override
        void notifySubscriber(int revision, Wrapper wrapper) {
            Observer<V> observer = wrapper.getObject();
            switch (revision) {
                case SET:
                    //noinspection unchecked
                    observer.onResult((V) result);
                    break;
                case EXCEPTIONAL:
                    observer.onError((Throwable) result);
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
}
