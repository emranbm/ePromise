package epromise.mrcoder.blog.ir.epromise;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;

/**
 * Created by emran on 6/26/19.
 */
@Implements(UIRunner.class)
public class UIRunnerShadow extends Shadow implements Runner {

    @Implementation
    @Override
    public void run(Runnable runnable) {
        new AsyncRunner().run(runnable);
    }
}
