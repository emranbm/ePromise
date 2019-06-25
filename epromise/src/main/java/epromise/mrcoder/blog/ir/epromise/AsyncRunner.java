package epromise.mrcoder.blog.ir.epromise;

import android.os.AsyncTask;

/**
 * Created by emran on 12/3/18.
 */

class AsyncRunner implements Runner {
    @Override
    public void run(Runnable runnable) {
        AsyncTask.execute(runnable);
    }
}
