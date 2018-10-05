package org.superasync;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

class AndThenSuperAsync<U, V> extends SuperAsync<V> {

  private final SuperAsync<U> original;
  private final Transformation<U, V> transformation;

  AndThenSuperAsync(Executor executor, SuperAsync<U> original,
      Transformation<U, V> transformation) {
    super(executor);
    this.original = original;
    this.transformation = transformation;
  }

  @Override
  void execute(final Callback<V> callback, final Canceller canceller) {
    original.execute(new Callback<U>() {
      @Override
      public void onResult(final U result) {
        submit(new Callable<V>() {
          @Override
          public V call() throws Exception {
            return transformation.perform(result);
          }
        }, callback, canceller);
      }

      @Override
      public void onError(Throwable e) {
        callback.onError(e);
      }
    }, canceller);
  }
}
