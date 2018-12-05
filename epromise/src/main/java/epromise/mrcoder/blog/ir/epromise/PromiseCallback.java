package epromise.mrcoder.blog.ir.epromise;

/**
 * Created by emran on 12/1/18.
 */

public interface PromiseCallback<T> {

    void handle(PromiseHandle<T> handle);
}
