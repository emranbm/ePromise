package epromise.mrcoder.blog.ir.epromise;

import android.os.AsyncTask;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by emran on 12/1/18.
 */

public class Promise<T> {

    private final PromiseCallback<T> callback;
    private final PromiseHandleImp promiseHandle = new PromiseHandleImp();
    private final ConcurrentLinkedQueue<Promise> waitingPromises = new ConcurrentLinkedQueue<>();
    private final Runner runner;

    Promise(final PromiseCallback<T> callback, boolean runImmediately, Runner runner){
        this.callback = callback;
        this.runner = runner;

        if (runImmediately)
            run();
    }

    private Promise(final PromiseCallback<T> callback, boolean runImmediately) {
        this(callback, runImmediately, new AsyncRunner());
    }

    public Promise(final PromiseCallback<T> callback) {
        this(callback, true);
    }

    private void run() {
        runner.run(new Runnable() {
            @Override
            public void run() {
                callback.handle(promiseHandle);
            }
        });
    }

    public <V> Promise<V> then(final ThenCallback<T, V> callback) {

        Promise<V> p = new Promise<V>(new PromiseCallback<V>() {
            @Override
            public void handle(PromiseHandle<V> handle) {
                // This promise runs when the value is resolved and ready.
                callback.handle(promiseHandle.value, handle);
            }
        }, false);

        waitingPromises.add(p);
        return p;
    }

    private class PromiseHandleImp implements PromiseHandle<T>{

        private final Object fulfilLock = new Object();
        T value;
        Throwable rejectValue;
        boolean fulfilled = false;

        @Override
        public void resolve(T value) {
            synchronized (fulfilLock) {
                if (fulfilled)
                    throw new PromiseStateException(PromiseStateException.MSG_PROMISE_ALREADY_FULFILLED);

                fulfilled = true;
            }

            this.value = value;
            while (!waitingPromises.isEmpty()){
                Promise p = waitingPromises.peek();
                p.run();
            }
        }

        @Override
        public void reject(Throwable e) {
            synchronized (fulfilLock) {
                if (fulfilled)
                    throw new PromiseStateException(PromiseStateException.MSG_PROMISE_ALREADY_FULFILLED);

                fulfilled = true;
            }

            this.rejectValue = e;
        }
    }
}
