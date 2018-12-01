package epromise.mrcoder.blog.ir.epromise;

import android.os.AsyncTask;

import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * Created by emran on 12/1/18.
 */

public class Promise<T> {

    private final Runnable runnable;

    public Promise(Runnable runnable){
        this.runnable = runnable;
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {

            }
        });
    }
}
