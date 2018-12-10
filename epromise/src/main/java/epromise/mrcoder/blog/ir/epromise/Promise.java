package epromise.mrcoder.blog.ir.epromise;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by emran on 12/1/18.
 */

public class Promise<T> {

    private final PromiseCallback<T> callback;
    private final PromiseHandleImp promiseHandle = new PromiseHandleImp();
    private final ConcurrentLinkedQueue<Promise> thenPromises = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Promise> catchPromises = new ConcurrentLinkedQueue<>();
    private final Runner runner;

    private final Object fulfilLock = new Object();

    private Promise(final PromiseCallback<T> callback, boolean runImmediately) {
        this.callback = callback;
        this.runner = new AsyncRunner();

        if (runImmediately)
            run();

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

        Promise<V> p = new Promise<>(new PromiseCallback<V>() {
            @Override
            public void handle(PromiseHandle<V> handle) {
                // This promise runs when the value is resolved and ready.
                callback.handle(promiseHandle.value, handle);
            }
        }, false);

        synchronized (fulfilLock) {
            if (promiseHandle.fulfilled)
                p.run();
            else
                thenPromises.add(p);
        }

        return p;
    }

    public <V> Promise<V> catchReject(final ThenCallback<T, V> callback) {

        Promise<V> p = new Promise<>(new PromiseCallback<V>() {
            @Override
            public void handle(PromiseHandle<V> handle) {
                // This promise runs when rejected.
                callback.handle(promiseHandle.value, handle);
            }
        }, false);

        synchronized (fulfilLock) {
            if (promiseHandle.fulfilled)
                p.run();
            else
                catchPromises.add(p);
        }

        return p;
    }

    private class PromiseHandleImp implements PromiseHandle<T> {

        private T value;
        private Throwable rejectValue;
        private boolean fulfilled = false;

        @Override
        public void resolve(T value) {
            synchronized (fulfilLock) {
                if (fulfilled)
                    throw new PromiseStateException(PromiseStateException.MSG_PROMISE_ALREADY_FULFILLED);

                fulfilled = true;
            }

            this.value = value;
            int c = 0;
            while (!thenPromises.isEmpty()) {
                Promise p = thenPromises.remove();
                p.run();
                c++;
                if (c > 10)
                    throw new RuntimeException("Kooft2");
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
            while (!thenPromises.isEmpty()) {
                Promise p = thenPromises.remove();
                p.promiseHandle.reject(this.rejectValue);
            }
        }
    }
}
