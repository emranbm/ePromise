package epromise.mrcoder.blog.ir.epromise;

/**
 * Created by emran on 12/3/18.
 */

public class FakeRunner implements Runner {
    @Override
    public void run(Runnable runnable) {
        runnable.run();
    }
}
