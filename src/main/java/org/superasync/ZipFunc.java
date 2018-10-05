package org.superasync;

public interface ZipFunc<A, B, C> {
  C zip(A a, B b) throws Exception;
}
