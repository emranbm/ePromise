package epromise.mrcoder.blog.ir.epromise;


import android.os.AsyncTask;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by emran on 12/2/18.
 */

@RunWith(MockitoJUnitRunner.class)
public class PromiseTest {

    @Test
    public void promiseRuns(){
        final CountDownLatch lock = new CountDownLatch(1);
        Promise<String> p = new Promise<>(new PromiseCallback<String>() {
            @Override
            public void handle(PromiseHandle<String> handle) {
                lock.countDown();
            }
        }, true, new FakeRunner());

        try {
            boolean lockReleased = lock.await(500, TimeUnit.MILLISECONDS);
            if(!lockReleased)
                Assert.fail("Promise body didn't called.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
