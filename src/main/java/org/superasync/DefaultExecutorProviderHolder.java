package org.superasync;

import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class DefaultExecutorProviderHolder {

  static final Executor SYNC_EXECUTOR = new Executor() {
    @Override
    public void execute(Runnable command) {
      command.run();
    }
  };

  private static final ExecutorProvider DEFAULT_EXECUTOR_PROVIDER = new ExecutorProvider() {

    private final Executor io = Executors.newCachedThreadPool();
    private final Executor sequential = Executors.newSingleThreadExecutor();
    private final Executor computational =
        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private final ScheduledExecutorService scheduledExecutorService =
        new ScheduledThreadPoolExecutor(1) {
          @Override
          protected <V> RunnableScheduledFuture<V> decorateTask(Runnable runnable,
              RunnableScheduledFuture<V> task) {
            return new DecoratedRunnableScheduledFuture<V>(task);
          }
        };
    private final Scheduler scheduler = new Scheduler() {
      @Override
      public Completable.Cancellable.ErrorEmitting schedule(Runnable task, long delay) {
        return (DecoratedRunnableScheduledFuture) scheduledExecutorService.schedule(task, delay,
            TimeUnit.MILLISECONDS);
      }
    };
    private final Executor observing;

    {
      observing = SYNC_EXECUTOR;
    }

    @Override
    public Executor io() {
      return io;
    }

    @Override
    public Executor sequential() {
      return sequential;
    }

    @Override
    public Executor computation() {
      return computational;
    }

    @Override
    public Executor defaultObserving() {
      return observing;
    }

    @Override
    public Scheduler scheduler() {
      return scheduler;
    }
  };

  static ExecutorProvider getDefaultExecutorProvider() {
    return DEFAULT_EXECUTOR_PROVIDER;
  }

  private static class DecoratedRunnableScheduledFuture<T> extends IntPublisher<ErrorConsumer>
      implements RunnableScheduledFuture<T>,
      Completable.Cancellable.ErrorEmitting, Task {

    private final RunnableScheduledFuture<T> original;

    DecoratedRunnableScheduledFuture(RunnableScheduledFuture<T> original) {
      super(0);
      this.original = original;
    }

    @Override
    public boolean isPeriodic() {
      return original.isPeriodic();
    }

    @Override
    public long getDelay(TimeUnit unit) {
      return original.getDelay(unit);
    }

    @Override
    public int compareTo(Delayed o) {
      return original.compareTo(o);
    }

    @Override
    public void run() {
      original.run();
      publish(1);
    }

    @Override
    void notifySubscriber(Integer value, Wrapper wrapper) {
      if (value == 1) {
        try {
          get();
        } catch (ExecutionException e) {
          wrapper.getObject().onError(e);
          wrapper.remove();
        } catch (InterruptedException ignore) {
        }
      }
    }

    @Override
    public void setErrorConsumer(ErrorConsumer errorConsumer) {
      subscribe(errorConsumer);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      return original.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
      return original.isCancelled();
    }

    @Override
    public boolean isDone() {
      return original.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
      return original.get();
    }

    @Override
    public T get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
      return original.get(timeout, unit);
    }
  }
}
