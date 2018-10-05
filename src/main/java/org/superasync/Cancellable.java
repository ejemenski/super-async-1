package org.superasync;

interface Cancellable {
  boolean cancel(boolean mayInterruptIfRunning);
}
