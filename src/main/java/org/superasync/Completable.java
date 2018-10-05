package org.superasync;

interface Completable {
  boolean isDone();

  interface Cancellable extends Completable, org.superasync.Cancellable {
    interface ErrorEmitting extends Completable.ErrorEmitting, Cancellable {
    }
  }

  interface ErrorEmitting extends Completable {
    void setErrorConsumer(ErrorConsumer errorConsumer);
  }
}
