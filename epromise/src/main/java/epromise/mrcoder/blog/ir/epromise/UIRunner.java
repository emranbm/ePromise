package epromise.mrcoder.blog.ir.epromise;

import android.os.Handler;
import android.os.Looper;

/**
 * Created by emran on 6/25/19.
 */
class UIRunner implements Runner{

    private final Handler handler;

    UIRunner(){
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void run(Runnable runnable) {
        handler.post(runnable);
    }
}
