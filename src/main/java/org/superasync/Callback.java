package org.superasync;

interface Callback<V> extends ErrorConsumer {
  void onResult(V result);

  void onError(Throwable e);
}
