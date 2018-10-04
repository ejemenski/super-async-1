package org.superasync;


import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.*;

public class SuperAsyncTest {

    private int count = 0;

    @Test
    public void simpleTest() {
        testInstance(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return true;
            }
        }).execute(new ResultConsumer<Boolean>() {
            @Override
            public void onResult(Boolean result) {
                Assert.assertTrue(result);
            }
        });
    }

    @Test
    public void andThen() {
        testInstance(new Callable<Integer>() {
            @Override
            public Integer call() {
                return 1;
            }
        }).andThen(new Transformation<Integer, Integer>() {
            @Override
            public Integer perform(Integer arg) {
                return arg + 1;
            }
        }).execute(new ResultConsumer<Integer>() {
            @Override
            public void onResult(Integer result) {
                Assert.assertEquals(Integer.valueOf(2), result);
            }
        });
    }

    @Test
    public void zipTest() {
        testInstance(new Callable<Integer>() {
            @Override
            public Integer call() {
                return 1;
            }
        }).zipWith(new Callable<Integer>() {
            @Override
            public Integer call() {
                return 2;
            }
        }, new ZipFunc<Integer, Integer, Integer>() {
            @Override
            public Integer zip(Integer integer, Integer integer2) {
                return integer + integer2;
            }
        }).execute(new ResultConsumer<Integer>() {
            @Override
            public void onResult(Integer result) {
                Assert.assertEquals(Integer.valueOf(3), result);
            }
        });
    }

    @Test
    public void retrySimple() throws ExecutionException, InterruptedException, TimeoutException {
        Assert.assertEquals(Boolean.TRUE, SuperAsync.newInstance(Executors.newSingleThreadExecutor(), new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                if (++count <= 2) {
                    throw new Exception();
                }
                return true;
            }
        }).retryWhen(new RetryCondition() {
            @Override
            public long check(Throwable e, int count) {
                return count <= 2 ? 30 : DONT_RETRY;
            }
        }).execute().get(1000, TimeUnit.MILLISECONDS));
    }

    private <T> SuperAsync<T> testInstance(Callable<T> callable) {
        return SuperAsync.newInstance(DefaultExecutorProviderHolder.SYNC_EXECUTOR, callable);
    }
}