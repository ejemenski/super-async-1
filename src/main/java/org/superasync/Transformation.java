package org.superasync;

public interface Transformation<A, B> {
  B perform(A arg) throws Exception;
}
