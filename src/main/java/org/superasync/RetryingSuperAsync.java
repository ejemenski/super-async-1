package org.superasync;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

class RetryingSuperAsync<V> extends SuperAsync<V> {

  private final SuperAsync<V> superAsync;
  private final RetryCondition condition;

  RetryingSuperAsync(Executor executor, SuperAsync<V> superAsync, RetryCondition condition) {
    super(executor);
    this.superAsync = superAsync;
    this.condition = condition;
  }

  @Override
  public void execute(final Callback<V> callback, Canceller canceller) {
    AtomicInteger count = new AtomicInteger(0);
    superAsync.execute(new ErrorConsumerInner(callback, count, canceller), canceller);
  }

  private class ErrorConsumerInner implements Callback<V> {
    private final Callback<V> original;
    private final AtomicInteger count;
    private final Canceller canceller;

    ErrorConsumerInner(Callback<V> original, AtomicInteger count,
        Canceller canceller) {
      this.original = original;
      this.count = count;
      this.canceller = canceller;
    }

    @Override
    public void onResult(V result) {
      original.onResult(result);
    }

    @Override
    public void onError(Throwable e) {
      final long delay = condition.check(e, count.incrementAndGet());
      if (delay < 0) {
        original.onError(e);
      } else {
        if (delay == 0L) {
          superAsync.execute(this, canceller);
        } else {

          Completable.Cancellable.ErrorEmitting cancellable =
              ExecutorProviderStaticRef.getExecutorProvider().scheduler().schedule(new Runnable() {
                @Override
                public void run() {
                  superAsync.execute(ErrorConsumerInner.this, canceller);
                }
              }, delay);

          cancellable.setErrorConsumer(original);

          canceller.add(cancellable);
        }
      }
    }
  }
}
