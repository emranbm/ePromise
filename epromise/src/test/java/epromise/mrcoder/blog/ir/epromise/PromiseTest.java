package epromise.mrcoder.blog.ir.epromise;


import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
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

        try {
            boolean lockReleased = lock.await(500, TimeUnit.MILLISECONDS);
            if (!lockReleased)
                Assert.fail("Promise body didn't called.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void thenRuns() {
        final CountDownLatch lock = new CountDownLatch(1);
        Promise<String> p = new Promise<>(new PromiseCallback<String>() {
            @Override
            public void handle(PromiseHandle<String> handle) {
                handle.resolve("hello");
            }
        });

        p.then(new ThenCallback<String, String>() {
            @Override
            public void handle(String value, PromiseHandle<String> handle) {
                lock.countDown();
            }
        });

        try {
            boolean lockReleased = lock.await(500, TimeUnit.MILLISECONDS);
            if (!lockReleased)
                Assert.fail("Promise body didn't called.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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

        try {
            lock.await();
            p.then(new ThenCallback<String, String>() {
                @Override
                public void handle(String value, PromiseHandle<String> handle) {
                    lock2.countDown();
                }
            });

            boolean lock2Released = lock.await(500, TimeUnit.MILLISECONDS);
            if (!lock2Released)
                Assert.fail("Post-fulfilment `then` didn't get called.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
