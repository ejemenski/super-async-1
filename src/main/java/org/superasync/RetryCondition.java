package org.superasync;

public interface RetryCondition {

  long DONT_RETRY = -1;

  long check(Throwable e, int count);
}
