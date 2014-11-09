package weka.filters.unsupervised.instance;

import junit.framework.Test;
import junit.framework.TestSuite;
import weka.core.Instances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

/**
 * Tests Flash. Run from the command line with:
 * <p>
 * java weka.filters.unsupervised.instance.FlashTest
 *
 * @author Christian Windolf
 * @author Simon Könnecke
 * @author Andre Breitenfeld
 * @version $Revision: 1.0 $
 */
public class FlashTest extends AbstractFilterTest {

    public FlashTest(String name) {
        super(name);
    }

    @Override
    public Filter getFilter() {
        return null;
    }
}
