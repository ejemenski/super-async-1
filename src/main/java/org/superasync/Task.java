package org.superasync;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

interface Task extends Runnable, Completable.Cancellable {

  class Factory {
    static <V> Task fromCallable(Callable<V> callable, Callback<V> callback) {
      return new FromCallable<V>(callable, callback);
    }
  }

  class FromCallable<V> implements Task {

    private enum State {INITIAL, RUNNING, DONE}

    private final AtomicReference<State> state = new AtomicReference<State>(State.INITIAL);
    private final Callable<V> task;
    private final Callback<V> callback;
    private volatile Thread runner;
    private V result;

    FromCallable(Callable<V> task, Callback<V> callback) {
      this.task = task;
      this.callback = callback;
    }

    @Override
    public void run() {
      if (!state.compareAndSet(State.INITIAL, State.RUNNING)) {
        return;
      }
      runner = Thread.currentThread();
      try {
        result = task.call();
      } catch (Exception e) {
        if (state.compareAndSet(State.RUNNING, State.DONE)) {
          callback.onError(e);
        }
      }
      if (state.compareAndSet(State.RUNNING, State.DONE)) {
        callback.onResult(result);
      }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {

      State old = state.getAndSet(State.DONE);
      if (old == State.DONE) {
        return false;
      }

      if (mayInterruptIfRunning) {
        Thread r = runner;
        if (r != null) {
          r.interrupt();
        }
      }
      return true;
    }

    @Override
    public boolean isDone() {
      return state.get() == State.DONE;
    }
  }
}
