package org.superasync;

public interface ErrorConsumer {
  void onError(Throwable e);
}
