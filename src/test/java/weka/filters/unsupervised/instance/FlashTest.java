package weka.filters.unsupervised.instance;

import java.io.BufferedReader;
import java.io.InputStreamReader;

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
		return new Flash();
	}

	protected void setUp() throws Exception {
		m_Filter = getFilter();
		m_Instances = new Instances(
				new BufferedReader(
						new InputStreamReader(
								ClassLoader
										.getSystemResourceAsStream("weka/filters/data/FlashTest.arff"))));
		m_Instances.setClassIndex(1);
		m_OptionTester = getOptionTester();
		m_GOETester = getGOETester();
		m_FilteredClassifier = getFilteredClassifier();
	}

	protected void tearDown() {
		m_Filter = null;
		m_Instances = null;
		m_OptionTester = null;
		m_GOETester = null;
		m_FilteredClassifier = null;
	}
	
	
	
	

}
