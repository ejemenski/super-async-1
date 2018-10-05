package org.superasync;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

public class ZipSuperAsync<U, V, R> extends SuperAsync<R> {

  private final SuperAsync<U> superAsync1;
  private final SuperAsync<V> superAsync2;
  private final ZipFunc<U, V, R> zipFunc;

  ZipSuperAsync(Executor executor, SuperAsync<U> superAsync1, SuperAsync<V> superAsync2,
      ZipFunc<U, V, R> zipFunc) {
    super(executor);
    this.superAsync1 = superAsync1;
    this.superAsync2 = superAsync2;
    this.zipFunc = zipFunc;
  }

  @Override
  public void execute(Callback<R> callback, Canceller canceller) {

    AtomicReference<Object> firstOne = new AtomicReference<Object>(null);

    superAsync1.execute(new ResultConsumer1(callback, canceller, firstOne), canceller);
    superAsync2.execute(new ResultConsumer2(callback, canceller, firstOne), canceller);
  }

  private class ResultConsumer1 implements Callback<U> {

    private final Canceller canceller;
    private final Callback<R> callback;
    private final AtomicReference<Object> firstOne;

    ResultConsumer1(Callback<R> callback,
        Canceller canceller,
        AtomicReference<Object> firstOne) {
      this.callback = callback;
      this.canceller = canceller;
      this.firstOne = firstOne;
    }

    @Override
    public void onResult(final U result) {
      if (!firstOne.compareAndSet(null, result)) {
        submit(new Callable<R>() {
          @Override
          public R call() throws Exception {
            //noinspection unchecked
            return zipFunc.zip(result, (V) firstOne.get());
          }
        }, callback, canceller);
      }
    }

    @Override
    public void onError(Throwable e) {
      callback.onError(e);
    }
  }

  private class ResultConsumer2 implements Callback<V> {

    private final Canceller canceller;
    private final Callback<R> callback;
    private final AtomicReference<Object> firstOne;

    ResultConsumer2(Callback<R> callback,
        Canceller canceller,
        AtomicReference<Object> firstOne) {
      this.callback = callback;
      this.canceller = canceller;
      this.firstOne = firstOne;
    }

    @Override
    public void onResult(final V result) {
      if (!firstOne.compareAndSet(null, result)) {
        submit(new Callable<R>() {
          @Override
          public R call() throws Exception {
            //noinspection unchecked
            return zipFunc.zip((U) firstOne.get(), result);
          }
        }, callback, canceller);
      }
    }

    @Override
    public void onError(Throwable e) {
      callback.onError(e);
    }
  }
}
