package org.superasync;

import java.util.concurrent.Executor;

public class Observer<V> implements Callback<V> {

  private final Executor executor;
  private final ResultConsumer<V> resultConsumer;
  private final ErrorConsumer errorConsumer;

  Observer(Executor executor, ResultConsumer<V> resultConsumer,
      ErrorConsumer errorConsumer) {
    this.executor = executor;
    this.resultConsumer = resultConsumer;
    this.errorConsumer = errorConsumer;
  }

  @Override
  public void onResult(final V result) {
    deliverResult(result);
  }

  @Override
  public void onError(final Throwable e) {
    deliverError(e);
  }

  private void deliverResult(final V result) {
    if (resultConsumer == null) {
      return;
    }
    executor.execute(new Runnable() {

      @Override
      public void run() {
        try {
          resultConsumer.onResult(result);
        } catch (Exception e) {
          errorConsumer.onError(e);
        }
      }
    });
  }

  private void deliverError(final Throwable error) {
    if (errorConsumer == null) {
      throw new RuntimeException(error);
    }
    executor.execute(new Runnable() {
      @Override
      public void run() {
        errorConsumer.onError(error);
      }
    });
  }
}
