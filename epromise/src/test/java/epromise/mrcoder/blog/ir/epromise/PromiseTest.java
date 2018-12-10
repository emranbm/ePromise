package epromise.mrcoder.blog.ir.epromise;


import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * Created by emran on 12/2/18.
 */

@RunWith(RobolectricTestRunner.class)
public class PromiseTest {

    @Test
    public void promiseRuns() {
        final CountDownLatch lock = new CountDownLatch(1);
        Promise<String> p = new Promise<>(new PromiseCallback<String>() {
            @Override
            public void handle(PromiseHandle<String> handle) {
                lock.countDown();
            }
        });

        await(lock, "Promise body didn't called.");
    }

    @Test
    public void thenRunsBeforeFulfilment() {
        final CountDownLatch lock = new CountDownLatch(1);
        final CountDownLatch lock2 = new CountDownLatch(1);
        final Promise<String> p = new Promise<>(new PromiseCallback<String>() {
            @Override
            public void handle(PromiseHandle<String> handle) {
                await(lock);
                handle.resolve("hello");
            }
        });

        p.then(new ThenCallback<String, String>() {
            @Override
            public void handle(String value, PromiseHandle<String> handle) {
                lock2.countDown();
            }
        });

        lock.countDown();

        await(lock2, "Pre-fulfilment `then` didn't get called.");
    }

    @Test
    public void thenRunsAfterFulfilment() {
        final CountDownLatch lock = new CountDownLatch(1);
        final CountDownLatch lock2 = new CountDownLatch(1);
        Promise<String> p = new Promise<>(new PromiseCallback<String>() {
            @Override
            public void handle(PromiseHandle<String> handle) {
                handle.resolve("value1");
                lock.countDown();
            }
        });

        await(lock);

        p.then(new ThenCallback<String, String>() {
            @Override
            public void handle(String value, PromiseHandle<String> handle) {
                lock2.countDown();
            }
        });

        await(lock2, "Post-fulfilment `then` didn't get called.");
    }

    @Test
    public void catchRuns() {
        final CountDownLatch lock = new CountDownLatch(1);
        Promise p = new Promise(new PromiseCallback() {
            @Override
            public void handle(PromiseHandle handle) {
                handle.reject(new Exception());
            }
        });

        p.catchReject(new ThenCallback<Throwable, Void>() {
            @Override
            public void handle(Throwable value, PromiseHandle<Void> handle) {
                lock.countDown();
            }
        });

        await(lock, "Catch not called.");
    }

    @Test
    public void rejectionPropagates() {
        final CountDownLatch lock = new CountDownLatch(1);
        Promise p = new Promise(new PromiseCallback() {
            @Override
            public void handle(PromiseHandle handle) {
                handle.reject(new Exception());
            }
        });

        p.then(new ThenCallback() {
            @Override
            public void handle(Object value, PromiseHandle handle) {
                handle.resolve(null);
            }
        }).then(new ThenCallback() {
            @Override
            public void handle(Object value, PromiseHandle handle) {
                handle.resolve(null);
            }
        }).catchReject(new ThenCallback<Throwable, Void>() {
            @Override
            public void handle(Throwable value, PromiseHandle<Void> handle) {
                lock.countDown();
            }
        });

        await(lock, "Rejection not propagated through continuous thens.");
    }

    private static void await(CountDownLatch lock, String timeoutMsg) {
        try {
            boolean lockReleased = lock.await(500, TimeUnit.MILLISECONDS);
            if (!lockReleased) {
                if (timeoutMsg != null)
                    Assert.fail(timeoutMsg);
                else
                    throw new RuntimeException("Lock timeout");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void await(CountDownLatch lock) {
        await(lock, null);
    }
}
