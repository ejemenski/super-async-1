package org.superasync;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

public abstract class SuperAsync<V> {

  public static <V> SuperAsync<V> newInstance(Executor executor, Callable<V> callable) {
    return new SingleSuperAsync<V>(executor, callable);
  }

  public static <V> SuperAsync<V> newIO(Callable<V> callable) {
    return newInstance(ExecutorProviderStaticRef.getExecutorProvider().io(), callable);
  }

  public static <V> SuperAsync<V> newSequential(Callable<V> callable) {
    return newInstance(ExecutorProviderStaticRef.getExecutorProvider().sequential(), callable);
  }

  public static <V> SuperAsync<V> newComputation(Callable<V> callable) {
    return newInstance(ExecutorProviderStaticRef.getExecutorProvider().computation(), callable);
  }

  private final Executor executor;

  SuperAsync(Executor executor) {
    this.executor = executor;
  }

  void submit(Callable<V> callable, Callback<V> callback, Canceller canceller) {
    Task task = Task.Factory.fromCallable(callable, callback);
    canceller.add(task);
    executor.execute(task);
  }

  public final SuperFuture<V> execute() {
    Canceller canceller = new Canceller();
    SuperFuture<V> future = new SuperFuture<V>(canceller);
    execute(future.asCallback(), canceller);
    return future;
  }

  public final Observation<V> execute(ResultConsumer<V> resultConsumer) {
    return execute(resultConsumer, null);
  }

  public final Observation<V> execute(ResultConsumer<V> resultConsumer,
      ErrorConsumer errorConsumer) {
    return execute(resultConsumer, errorConsumer, null);
  }

  public final Observation<V> execute(ResultConsumer<V> resultConsumer,
      ErrorConsumer errorConsumer, Executor observingExecutor) {
    Canceller canceller = new Canceller();
    SuperFuture<V> future = new SuperFuture<V>(canceller);
    Observation<V> observation = future.observe(resultConsumer, errorConsumer, observingExecutor);
    execute(future.asCallback(), canceller);
    return observation;
  }

  abstract void execute(Callback<V> callback, Canceller canceller);

  public final <U> SuperAsync<U> andThen(final Transformation<V, U> transformation) {
    return new AndThenSuperAsync<V, U>(executor, this, transformation);
  }

  public final <U, R> SuperAsync<R> zipWith(Callable<U> other, ZipFunc<V, U, R> zipFunc) {
    return new ZipSuperAsync<V, U, R>(executor, this, new SingleSuperAsync<U>(executor, other),
        zipFunc);
  }

  public final SuperAsync<V> retryWhen(RetryCondition retryCondition) {
    return new RetryingSuperAsync<V>(executor, this, retryCondition);
  }
}
