package org.superasync;

public interface Scheduler {
  Completable.Cancellable.ErrorEmitting schedule(Runnable task, long delay);
}
