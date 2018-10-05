package org.superasync;

public class ExecutorProviderStaticRef {

  private static ExecutorProvider executorProvider;

  public static void setExecutorProvider(ExecutorProvider executorProvider) {
    ExecutorProviderStaticRef.executorProvider = executorProvider;
  }

  static ExecutorProvider getExecutorProvider() {
    if (executorProvider != null) {
      return executorProvider;
    }
    return DefaultExecutorProviderHolder.getDefaultExecutorProvider();
  }
}
