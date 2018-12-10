package epromise.mrcoder.blog.ir.epromise;

import java.util.Enumeration;
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
            if (promiseHandle.fulfilled) {
                if (isResolved())
                    p.run();
                else if (isRejected())
                    p = Promise.reject(promiseHandle.rejectValue);
                else
                    throw new PromiseStateException("Fulfilled promise is neither resolved nor rejected!");
            } else
                thenPromises.add(p);
        }

        return p;
    }

    public <V> Promise<V> catchReject(final ThenCallback<Throwable, V> callback) {

        Promise<V> p = new Promise<>(new PromiseCallback<V>() {
            @Override
            public void handle(PromiseHandle<V> handle) {
                // This promise runs when rejected.
                callback.handle(promiseHandle.rejectValue, handle);
            }
        }, false);

        synchronized (fulfilLock) {
            if (promiseHandle.fulfilled) {
                if (isRejected())
                    p.run();
            } else
                catchPromises.add(p);
        }

        return p;
    }

    private boolean isRejected() {
        return promiseHandle.rejectValue != null;
    }

    private boolean isResolved() {
        return promiseHandle.value != null;
    }

    public static <V> Promise<V> resolve(V value){
        Promise<V> p = new Promise<>(null,false);
        p.promiseHandle.resolve(value);
        return p;
    }

    public static <V> Promise<V> reject(Throwable e){
        Promise<V> p = new Promise<>(null,false);
        p.promiseHandle.reject(e);
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
            while (!thenPromises.isEmpty()) {
                Promise p = thenPromises.remove();
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

            runCatchPromises(Promise.this);
            while (!thenPromises.isEmpty()) {
                Promise p = thenPromises.remove();
                p.promiseHandle.reject(this.rejectValue);
            }
        }

        private void runCatchPromises(Promise rejectedPromise) {
            while (!rejectedPromise.catchPromises.isEmpty()) {
                Promise catchPromise = (Promise) rejectedPromise.catchPromises.remove();
                catchPromise.run();

                while (!rejectedPromise.thenPromises.isEmpty())
                    runCatchPromises((Promise) rejectedPromise.thenPromises.remove());
            }
        }
    }
}
