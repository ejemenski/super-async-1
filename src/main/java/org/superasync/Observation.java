package org.superasync;

class Observation<V> {

  private final Removable removable;
  private final SuperFuture<V> future;

  Observation(Removable removable, SuperFuture<V> future) {
    this.removable = removable;
    this.future = future;
  }

  void stopObservation() {
    removable.remove();
  }

  SuperFuture future() {
    return future;
  }

  ;
}
