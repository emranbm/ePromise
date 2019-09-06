package epromise.mrcoder.blog.ir.epromise;


import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * Created by emran on 12/2/18.
 */

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {UIRunnerShadow.class})
public class PromiseTest {

    @Test
    public void promiseRuns() {
        final CountDownLatch lock = new CountDownLatch(1);
        Promise<String> p = new Promise<>(handle -> lock.countDown());

        await(lock, "Promise body didn't called.");
    }

    @Test
    public void thenRunsBeforeFulfilment() {
        final CountDownLatch lock = new CountDownLatch(1);
        final CountDownLatch lock2 = new CountDownLatch(1);
        final Promise<String> p = new Promise<>(handle -> {
            await(lock);
            handle.resolve("hello");
        });

        p.then((ThenCallback<String, String>) (value, handle) -> lock2.countDown());

        lock.countDown();
        Robolectric.flushForegroundThreadScheduler();

        await(lock2, "Pre-fulfilment `then` didn't get called.");
    }

    @Test
    public void thenRunsAfterFulfilment() {
        final CountDownLatch lock = new CountDownLatch(1);
        final CountDownLatch lock2 = new CountDownLatch(1);
        Promise<String> p = new Promise<>(handle -> {
            handle.resolve("value1");
            lock.countDown();
        });

        await(lock);

        p.then((ThenCallback<String, String>) (value, handle) -> lock2.countDown());

        await(lock2, "Post-fulfilment `then` didn't get called.");

    }

    @Test
    public void catchRuns() {
        final CountDownLatch lock = new CountDownLatch(1);
        Promise<Exception> p = new Promise<>(handle -> handle.reject(new Exception()));

        p.catchReject((ThenCallback<Throwable, Void>) (value, handle) -> lock.countDown());

        await(lock, "Catch not called.");
    }

    @Test
    public void rejectionPropagates() {
        final CountDownLatch lock = new CountDownLatch(1);
        Promise<Object> p = new Promise<>(handle -> handle.reject(new Exception()));

        p.then((value, handle) -> handle.resolve(null))
                .then((value, handle) -> handle.resolve(null))
                .catchReject((ThenCallback<Throwable, Void>) (value, handle) -> lock.countDown());

        await(lock, "Rejection not propagated through continuous thens.");
    }

    @Test
    public void allRuns() {
        CountDownLatch lock = new CountDownLatch(1);
        Promise.all(
                new Promise<String>(handle -> {
                    handle.resolve("a");
                }),
                new Promise<String>(handle -> {
                    handle.resolve("b");
                }))
                .then((values, handle) -> {
                    Assert.assertEquals("ab", (String) values.get(0) + values.get(1));
                    lock.countDown();
                });

        await(lock);
    }

//    @Test
//    public void allRejectionWorks(){
//
//        CountDownLatch lock = new CountDownLatch(1);
//        Promise.all(
//                new Promise<String>(handle -> handle.resolve("a")),
//                new Promise<String>(handle -> handle.reject("e")))
//                .catchReject((value, handle) -> {
//                    lock.countDown();
//                });
//
//        await(lock, "Catch not called on 'all' promise.");
//    }

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
