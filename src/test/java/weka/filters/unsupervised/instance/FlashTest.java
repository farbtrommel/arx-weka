package weka.filters.unsupervised.instance;

import static weka.core.Attribute.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;
import weka.test.Regression;

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
		f.setEnableLDiversity(true);
		f.setValueL(2);
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
		super.tearDown();
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
	
	public void testLDiversity(){
		Instances result = useFilter();
		assertTrue("The result has less than l different sensitive values of quasi-identifiying class", checkLDiversity(result, 2, 0,1,2,3,4));
	}
	
	
	private boolean checkKAnonymity(Instances instances, int k, int ...identifiers ){
		Map<String, List<String>> map = identifieres2Sensitive(instances, identifiers);
		
		for(Entry<String, List<String>> entry : map.entrySet()){
			if(entry.getValue().size() < k){
				return false;
			}
		}
		
		return true;
	}
	
	private boolean checkLDiversity(Instances instances, int l, int ...identifiers){
		Map<String, List<String>> map = identifieres2Sensitive(instances, identifiers);
		
		for(Entry<String, List<String>> entry : map.entrySet()){
			Set<String> set = new HashSet<String>();
			for(String sensitive : entry.getValue()){
				set.add(sensitive);
			}
			if(set.size() < l){
				return false;
			}
			
		}
		return true;
		
	}
	
	private Map<String, List<String>> identifieres2Sensitive(Instances instances, int ... identifiers){
		Map<String, List<String>> result = new HashMap<String, List<String>>();
		for(int i = 0; i < instances.numInstances(); i++){
			StringBuilder identifier = new StringBuilder();
			Instance instance = instances.instance(i);
			for(int id : identifiers){
				Attribute attr = instance.attribute(id);
				identifier.append(attribute2String(instance, attr));
			}
			
			StringBuilder sensitive = new StringBuilder();
			for(int j = 0; j < instances.numAttributes(); j++){
				if(!Arrays.asList(identifiers).contains(j)){
					sensitive.append(attribute2String(instance, instances.attribute(j)));
				}
			}
			List<String> list = result.get(identifier.toString());
			if(list == null){
				list = new ArrayList<String>(3);
			}
			list.add(sensitive.toString());
			result.put(identifier.toString(), list);
		}
		return result;
	}
	
	private String attribute2String(Instance instance, Attribute attr){
		if(attr.type() == NUMERIC){
			return String.valueOf(instance.value(attr));
		} else {
			return instance.stringValue(attr);
		}
	}
	
	

}
