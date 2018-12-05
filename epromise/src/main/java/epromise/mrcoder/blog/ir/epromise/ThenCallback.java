package epromise.mrcoder.blog.ir.epromise;

/**
 * Created by emran on 12/1/18.
 */

public interface ThenCallback<V, T> {

    void handle(final V value, PromiseHandle<T> handle);
}
