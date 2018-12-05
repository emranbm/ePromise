package epromise.mrcoder.blog.ir.epromise;

/**
 * Created by emran on 12/1/18.
 */

public class PromiseStateException extends IllegalStateException {

    static final String MSG_PROMISE_ALREADY_FULFILLED = "The promise is already fulfilled.";

    public PromiseStateException(String msg){
        super(msg);

    }
}
