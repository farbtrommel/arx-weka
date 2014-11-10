package weka.filters.unsupervised.instance;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

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
	private static final String DATA_PATH = "weka/filters/data/";

	public FlashTest(String name) {
		super(name);
	}

	@Override
	public Filter getFilter() {
		Flash f = new Flash();
		f.setDataHierarchyFolder(hierarchyFolder);
		return f;
	}
	
	protected File hierarchyFolder;

	protected void setUp() throws Exception {
		hierarchyFolder = File.createTempFile("flash", Long.toString(System.currentTimeMillis()));
		m_Filter = getFilter();
		m_Instances = new Instances(
				new BufferedReader(
						new InputStreamReader(
								ClassLoader
										.getSystemResourceAsStream(DATA_PATH + "FlashTest.arff"))));
		m_Instances.setClassIndex(1);
		m_OptionTester = getOptionTester();
		m_GOETester = getGOETester();
		m_FilteredClassifier = getFilteredClassifier();
		
		
		
		if(hierarchyFolder.exists()){
			throw new IOException("I need an empty folder " + hierarchyFolder.getAbsolutePath() + " but it already exists");
		}
		if(!hierarchyFolder.mkdir()){
			throw new IOException("I need to be able to create the folder " + hierarchyFolder.getAbsolutePath() + " but I can't (check write permission?)");
		}
		copyHierarchies("age");
		copyHierarchies("car");
		copyHierarchies("children");
		copyHierarchies("name");
		copyHierarchies("zip");
		
		
		
	}
	
	private void copyHierarchies(String hierarchy) throws IOException{
		String filename = "test_hierachry_" + hierarchy + ".csv";
		InputStream in = ClassLoader.getSystemResourceAsStream(DATA_PATH + filename);
		File file = new File(hierarchyFolder, filename);
		file.createNewFile();
		OutputStream out = new FileOutputStream(file);
		byte[] buffer = new byte[1024];
		int len = in.read(buffer);
		while(len != -1) {
			out.write(buffer, 0, len);
			len = in.read(buffer);
		}
		in.close();
		out.close();
		
	}

	protected void tearDown() {
		m_Filter = null;
		m_Instances = null;
		m_OptionTester = null;
		m_GOETester = null;
		m_FilteredClassifier = null;
	}
	
	
	
	

}
