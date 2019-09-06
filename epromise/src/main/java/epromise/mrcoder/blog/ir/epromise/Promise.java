package epromise.mrcoder.blog.ir.epromise;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private Promise(final PromiseCallback<T> callback, boolean runImmediately, Runner runner) {
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

    void run() {
        runner.run(() ->
                callback.handle(promiseHandle));
    }

    PromiseHandleImp<T> getPromiseHandle() {
        return promiseHandle;
    }

    ConcurrentLinkedQueue<Promise> getThenPromises() {
        return thenPromises;
    }

    ConcurrentLinkedQueue<Promise> getCatchPromises() {
        return catchPromises;
    }

    public <V> Promise<V> then(final ThenCallback<T, V> callback) {
        return thenInternal(callback, new UIRunner());
    }

    public <V> Promise<V> thenAsync(final ThenCallback<T, V> callback) {
        return thenInternal(callback, new AsyncRunner());
    }

    public <U, V> Promise<V> catchReject(final ThenCallback<U, V> callback) {
        return catchRejectInternal(callback, new UIRunner());
    }

    public <U, V> Promise<V> catchRejectAsync(final ThenCallback<U, V> callback) {
        return catchRejectInternal(callback, new AsyncRunner());
    }

    boolean isRejected() {
        return promiseHandle.rejectValue != null;
    }

    boolean isResolved() {
        return promiseHandle.value != null;
    }

    private <V> Promise<V> thenInternal(final ThenCallback<T, V> callback, Runner runner) {
        Promise<V> p = new Promise<>(handle -> {
            // This promise runs when the value is resolved and ready.
            callback.handle(promiseHandle.value, handle);
        }, false, runner);

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

    private <U, V> Promise<V> catchRejectInternal(final ThenCallback<U, V> callback, Runner runner) {

        Promise<V> p = new Promise<>(handle -> {
            // This promise runs when rejected.
            U asU = (U) promiseHandle.rejectValue;

            try {
                callback.handle(asU, handle);
            } catch (ClassCastException e) {
                throw new PromiseValueException("The provided reject handler has an incompatible type for the value argument.", e);
            }
        }, false, runner);

        synchronized (fulfilLock) {
            if (promiseHandle.fulfilled) {
                if (isRejected())
                    p.run();
            } else
                catchPromises.add(p);
        }

        return p;
    }

    public static <V> Promise<V> resolve(V value) {
        Promise<V> p = new Promise<>(null, false);
        p.promiseHandle.resolve(value);
        return p;
    }

    public static <V> Promise<V> reject(Object e) {
        Promise<V> p = new Promise<>(null, false);
        p.promiseHandle.reject(e);
        return p;
    }

    public static Promise<ArrayList<Object>> all(Promise... promises) {
        if (promises == null)
            throw new NullPointerException("Promises array is null.");

        return new Promise<>(handle -> {
            CountDownLatch countDownLatch = new CountDownLatch(promises.length);

            final ArrayList<Object> results = new ArrayList<>(promises.length);
            for (Promise p : promises)
                p.thenAsync((value, handle1) -> {
                    results.add(value);
                    countDownLatch.countDown();
                });

            try {
                countDownLatch.await();
                handle.resolve(results);
            } catch (InterruptedException e) {
                e.printStackTrace();
                handle.reject(e);
            }
        });
    }

    public static Promise<Object> any(Promise... promises) {
        final AtomicBoolean anySucceeded = new AtomicBoolean(false);

        return new Promise<>(handle -> {
            for (Promise p : promises)
                p.then((value, h) -> {
                    boolean anySucceededBefore = anySucceeded.getAndSet(true);
                    if (!anySucceededBefore)
                        handle.resolve(value);
                });
        });
    }
}
