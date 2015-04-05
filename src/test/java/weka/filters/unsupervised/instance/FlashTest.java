package weka.filters.unsupervised.instance;

import static weka.core.Attribute.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import weka.core.Attribute;
import weka.core.Instance;
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
		Flash f = new Flash();
		f.setDataHierarchyFolder(hierarchyFolder);
		f.setDataSensitiveAttributes("6,7");
		return f;
	}
	
	protected File hierarchyFolder;

	protected void setUp() throws Exception {
		hierarchyFolder = new File(System.getProperty("java.io.tmpdir"), "flash." + System.currentTimeMillis());
		
		if(!hierarchyFolder.mkdir()){
			throw new IOException("I need to be able to create the folder " + hierarchyFolder.getAbsolutePath() + " but I can't (check write permission?)");
		}
		
		m_Filter = getFilter();
		m_Instances = new Instances(
				new BufferedReader(
						new InputStreamReader(
								ClassLoader
										.getSystemResourceAsStream("FlashTest.arff"))));
		m_Instances.setClassIndex(1);
		m_OptionTester = getOptionTester();
		m_GOETester = getGOETester();
		m_FilteredClassifier = null;
		
		copyHierarchies("age");
		copyHierarchies("car");
		copyHierarchies("children");
		copyHierarchies("name");
		copyHierarchies("zip");
			
	}
	

	
	/**
	 * Copies hierarchies from the class path to the temporary 
	 * folder {@link #hierarchyFolder}.
	 * @param hierarchy
	 * @throws IOException
	 */
	private void copyHierarchies(String hierarchy) throws IOException{
		String filename = "test_hierarchy_" + hierarchy + ".csv";
		InputStream in = ClassLoader.getSystemResourceAsStream(filename);
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
		String[] files = hierarchyFolder.list();
		for(String file : files){
			File f = new File(hierarchyFolder, file);
			f.delete();
		}
		hierarchyFolder.delete();
		m_Filter = null;
		m_Instances = null;
		m_OptionTester = null;
		m_GOETester = null;
		m_FilteredClassifier = null;
	}
	

	/**
	 * Check that the number of instances remains the same
	 */
	public void testKAnonymityFilterAmount(){
		Instances result = useFilter();
		assertEquals(m_Instances.numInstances(), result.numInstances());
	}
	
	public void testKAnonymity(){
		Instances result = useFilter();
		assertTrue("The result has an identifier that occurs less than 2 times", checkKAnonymity(result, 2,0,1,2,3,4));
	}
	
	private boolean checkKAnonymity(Instances instances, int k, int ...identifiers ){
		Map<String, Integer> counters = new HashMap<String, Integer>();
		
		for(int i = 0; i < instances.numInstances(); i++){
			StringBuilder identifier = new StringBuilder();
			for(int id : identifiers){
				Instance instance = instances.instance(i);
				Attribute attr = instance.attribute(id);
				if(attr.type() == NUMERIC){
					identifier.append(instance.value(id));
				} else {
					identifier.append(instance.stringValue(id));
				}
				
			}
			Integer counter = counters.get(identifier.toString());
			if(counter == null){
				counter = new Integer(1);
			} else {
				counter = new Integer(counter + 1);
			}
			counters.put(identifier.toString(), counter);
		}
		for(Entry<String, Integer> entry : counters.entrySet()){
			if(entry.getValue() < k){
				return false;
			}
		}
		
		return true;
		
	}
	
	
	
	

}
