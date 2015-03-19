/*
 * ARX: Weka Anonymization Filter
 * Copyright (C) 2014 Christian Windolf, Simon Koennecke, Andre Breitenfeld
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.deidentifier.arx;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;

import org.deidentifier.arx.AttributeType.Hierarchy;
import org.deidentifier.arx.AttributeType.Hierarchy.DefaultHierarchy;
import org.deidentifier.arx.io.CSVDataInput;

import weka.core.Attribute;
import weka.core.Instances;

/**
 * TODO
 *
 * @author Andre Breitenfeld
 * @author Simon Koennecke
 * @author Christian Windolf
 *
 */
public class HierarchyBuilder {

    /**
     * Name of the relation.
     */
	protected String relation;

    /**
     * Path to the folder containing the hierarchies.
     */
	protected File folder;

    /**
     * Constructor of the hierarchy builder.
     * @param folder Path to the folder containing the hierarchies.
     * @param relation Name of the relation.
     */
	public HierarchyBuilder(File folder, String relation) {
		this.folder = folder;
		this.relation = relation;
	}

    /**
     * This method creates an hierarchy instance for a given attribute.
     * @param att Attribute for which the hierarchy should be created
     * @param instances
     * @return Returns an ARX hierarchy instance.
     * @throws IOException if it was not possible to locate the hierarchy file.
     */
	public Hierarchy getHierarchy(Attribute att, Instances instances) throws IOException {
		File f = new File(this.folder, this.relation + "_hierarchy_"
				+ att.name() + ".csv");
		DefaultHierarchy hierarchy = new DefaultHierarchy();
		if (f.exists()) {
			CSVDataInput input = new CSVDataInput(f, ';');
			Iterator<String[]> iterator = input.iterator();
			int length = 0;
			while (iterator.hasNext()) {
				String[] line = iterator.next();
				if (length == 0) {
					length = line.length;
				} else if (line.length != length) {
					throw new IllegalArgumentException(
							"All lines in a hierarchy for attribute"
							+ "\"" + att.name() + "\" must have the "
									+ "same amount of entries. Until now, all had "
									+ length + " entries and now one with "
									+ line.length + " entries appeared");
				}
				hierarchy.add(line);
			}
			String[] missingValues = new String[length];
			for (int i = 0; i < length - 1; i++) {
				missingValues[i] = "?";
			}
			missingValues[missingValues.length - 1] = "*";
			hierarchy.add(missingValues);
		} else {
            // check type of attribute
			if (att.isNominal()) {
				Enumeration<Object> enumeration = att.enumerateValues();

				while (enumeration.hasMoreElements()) {
					Object value = enumeration.nextElement();
					hierarchy.add(value.toString(), "*");
				}
                // add asterisk as highest level of generalization
				hierarchy.add("?", "*");
			} else if (att.isNumeric()){
				int index = att.index();
				HashSet<Integer> hashset = new HashSet<Integer>(); 
				for(int i = 0; i < instances.numInstances(); i++){
					int value = (int) instances.instance(i).value(index);
					if(!hashset.contains(new Integer(value))){
						hierarchy.add(String.valueOf(value), "*");
						hashset.add(new Integer(value));
					} 
					
				}
                // add asterisk as highest level of generalization
				hierarchy.add("?", "*");
			}
		}
		
		return hierarchy;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("HierarchyFactory{");
		builder.append("folder: ").append(folder.getAbsolutePath());
		if (folder.isDirectory()) {
			builder.append(" (is directory),");
		} else if (folder.exists()) {
			builder.append(" (is file),");
		} else {
			builder.append(" (does not exist),");
		}
		builder.append("relation: \"").append(relation).append("\"}");
		return builder.toString();

	}

}
