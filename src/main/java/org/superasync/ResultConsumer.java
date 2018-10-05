package org.superasync;

public interface ResultConsumer<V> {
  void onResult(V result) throws Exception;
}
