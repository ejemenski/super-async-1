package org.superasync;

import java.util.concurrent.Executor;

public interface ExecutorProvider {
  Executor io();

  Executor sequential();

  Executor computation();

  Executor defaultObserving();

  Scheduler scheduler();
}
