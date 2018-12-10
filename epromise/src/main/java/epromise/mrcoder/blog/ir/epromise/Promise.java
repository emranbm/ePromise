package epromise.mrcoder.blog.ir.epromise;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by emran on 12/1/18.
 */

public class Promise<T> {

    private final PromiseCallback<T> callback;
    private final PromiseHandleImp<T> promiseHandle = new PromiseHandleImp<>(this);
    private final Runner runner;
    private final ConcurrentLinkedQueue<Promise> thenPromises = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Promise> catchPromises = new ConcurrentLinkedQueue<>();

    final Object fulfilLock = new Object();

    private Promise(final PromiseCallback<T> callback, boolean runImmediately) {
        this.callback = callback;
        this.runner = new AsyncRunner();

        if (runImmediately)
            run();

    }

    public Promise(final PromiseCallback<T> callback) {
        this(callback, true);
    }

    void run() {
        runner.run(new Runnable() {
            @Override
            public void run() {
                callback.handle(promiseHandle);
            }
        });
    }

    PromiseHandleImp<T> getPromiseHandle(){
        return promiseHandle;
    }

    ConcurrentLinkedQueue<Promise> getThenPromises(){
        return thenPromises;
    }

    ConcurrentLinkedQueue<Promise> getCatchPromises(){
        return catchPromises;
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

    boolean isRejected() {
        return promiseHandle.rejectValue != null;
    }

    boolean isResolved() {
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
}
