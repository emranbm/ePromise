package epromise.mrcoder.blog.ir.epromise;

/**
 * Created by emran on 12/1/18.
 */

public interface PromiseHandle<T> {

    void resolve(T value);

    void reject(Throwable e);
}
