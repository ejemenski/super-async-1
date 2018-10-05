package org.superasync;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

class SingleSuperAsync<V> extends SuperAsync<V> {

  private final Callable<V> task;

  SingleSuperAsync(Executor executor, Callable<V> task) {
    super(executor);
    this.task = task;
  }

  @Override
  public void execute(Callback<V> callback, Canceller canceller) {
    submit(task, callback, canceller);
  }
}
