package epromise.mrcoder.blog.ir.epromise;

/**
 * Created by emran on 12/10/18.
 */
class PromiseHandleImp<T> implements PromiseHandle<T> {

    private final Promise<T> promise;
    T value;
    Throwable rejectValue;
    boolean fulfilled = false;

    PromiseHandleImp(Promise<T> promise){

        this.promise = promise;
    }

    @Override
    public void resolve(T value) {
        synchronized (promise.fulfilLock) {
            if (fulfilled)
                throw new PromiseStateException(PromiseStateException.MSG_PROMISE_ALREADY_FULFILLED);

            fulfilled = true;
        }

        this.value = value;
        while (!promise.getThenPromises().isEmpty()) {
            Promise p = promise.getThenPromises().remove();
            p.run();
        }
    }

    @Override
    public void reject(Throwable e) {
        synchronized (promise.fulfilLock) {
            if (fulfilled)
                throw new PromiseStateException(PromiseStateException.MSG_PROMISE_ALREADY_FULFILLED);

            fulfilled = true;
        }

        this.rejectValue = e;

        runCatchPromises(promise);
        while (!promise.getThenPromises().isEmpty()) {
            Promise p = promise.getThenPromises().remove();
            p.getPromiseHandle().reject(this.rejectValue);
        }
    }

    private void runCatchPromises(Promise rejectedPromise) {
        while (!rejectedPromise.getCatchPromises().isEmpty()) {
            Promise catchPromise = (Promise) rejectedPromise.getCatchPromises().remove();
            catchPromise.run();

            while (!rejectedPromise.getThenPromises().isEmpty())
                runCatchPromises((Promise) rejectedPromise.getThenPromises().remove());
        }
    }
}
