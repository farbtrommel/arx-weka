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
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.deidentifier.arx.AttributeType.Hierarchy;

import cern.colt.Arrays;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Class to convert an ARFF instance to input for ARX anonymizer.
 *
 * @author Andre Breitenfeld
 * @author Simon Koennecke
 * @author Christian Windolf
 */
public class ARFF2ARX {

    protected Instances instances;

    protected String[] qi = new String[]{};

    protected String[] sensitive = new String[]{};

    protected String[] header;

    protected Map<String, Hierarchy> hierarchyMap;

    protected Data data;

    /**
     * This constructor exists only for JUNIT tests
     * Don't invoke it, if you want to work productively with
     * this class
     */
    protected ARFF2ARX() {
    }

    /**
     * Default constructor of this converter.
     *
     * @param instances The ARFF instance.
     */
    public ARFF2ARX(Instances instances) {
        this.instances = instances;

        header = new String[instances.numAttributes()];
        for (int i = 0; i < header.length; i++) {
            header[i] = instances.attribute(i).name();
        }
    }

    public ARFF2ARX setQi(String qi) {
        this.qi = splitToArray(qi);
        return this;
    }

    public ARFF2ARX setQi(String[] qi) {
        this.qi = qi;
        return this;
    }

    public ARFF2ARX setSensitive(String sensitive) {
        this.sensitive = splitToArray(sensitive);
        return this;
    }

    public ARFF2ARX setSensitive(String[] sensitive) {
        this.sensitive = sensitive;
        return this;
    }

    /**
     * @param dir
     * @param relation
     * @return
     * @throws IOException
     */
    public ARFF2ARX init(File dir, String relation) throws IOException {
        if (this.qi == null) {
            throw new IllegalStateException("please set quasi identifying attributes first!");
        }

        if (relation == null) {
            relation = "";
        }
        relation = relation.trim();

        hierarchyMap = new HashMap<String, Hierarchy>();
        HierarchyBuilder hBuilder = new HierarchyBuilder(dir, relation);
        for (String q : qi) {
            hierarchyMap.put(q, hBuilder.getHierarchy(instances.attribute(q), instances));
        }

        List<String[]> rawData = new ArrayList<String[]>(instances.numInstances() + 1);
        rawData.add(header);

        for (int i = 0; i < instances.numInstances(); i++) {
            String[] row = convertRow(instances.instance(i));
            if (row != null) {
                rawData.add(row);
            }
        }

        data = Data.create(rawData);
        DataDefinition definition = data.getDefinition();
        for (String attr : header) {
            if (isQiAttribute(attr)) {
                definition.setAttributeType(attr, hierarchyMap.get(attr));
            } else if (isSensitive(attr)) {
                definition.setAttributeType(attr, AttributeType.SENSITIVE_ATTRIBUTE);
            } else {
                definition.setAttributeType(attr, AttributeType.INSENSITIVE_ATTRIBUTE);
            }
        }
        return this;
    }

    /**
     * @param instance
     * @return
     */
    protected String[] convertRow(Instance instance) {
        String[] row = new String[instance.numAttributes()];
        for (int i = 0; i < instance.numAttributes(); i++) {
            if (instance.attribute(i).type() == Attribute.NOMINAL || instance.attribute(i).type() == Attribute.STRING) {
                row[i] = instance.stringValue(i);
            } else {
                row[i] = String.valueOf((int) instance.value(i));
            }
        }
        return row;
    }

    /**
     * Determines if a attribute is a sensitive attribute.
     *
     * @param attr The attribute to check.
     * @return Returns true if the given attribute is a sensitive attribute.
     */
    public boolean isSensitive(String attr) {
        return contains(attr, sensitive);
    }

    /**
     * Determines if a attribute is a sensitive attribute.
     *
     * @param attr The attribute to check.
     * @return Returns true if the given attribute is an qasi-identifier.
     */
    public boolean isQiAttribute(String attr) {
        return contains(attr, qi);
    }

    /**
     * @return Returns the instance.
     */
    public Instances getInstances() {
        return instances;
    }

    /**
     * @return Returns the header as string array.
     */
    public String[] getHeader() {
        return header;
    }

    /**
     * @return Returns the quasi-identifier as string array.
     */
    public String[] getQi() {
        return qi;
    }

    public Map<String, Hierarchy> getHierarchyMap() {
        return hierarchyMap;
    }

    public Data getData() {
        return data;
    }

    /**
     * @return Returns the sensitive attributes as string array.
     */
    public String[] getSensitive() {
        return sensitive;
    }

    /**
     * @param s
     * @return
     */
    protected String[] splitToArray(String s) {
        if (s == null || s.trim().equals("")) {
            return new String[0];
        } else {
            String[] array = s.split(",");
            List<String> list = new LinkedList<String>();
            for (int i = 0; i < array.length; i++) {
                String value = array[i].replaceAll("^\\s+", "").replaceAll("\\s+$", "");
                if (!value.trim().equals("")) {
                    if (instances != null && instances.attribute(value) == null) {
                        throw new IllegalArgumentException("The attribute \"" + value + "\" does not exist!");
                    }
                    list.add(value);
                }
            }
            String[] result = new String[list.size()];
            ListIterator<String> iterator = list.listIterator();
            while (iterator.hasNext()) {
                result[iterator.nextIndex()] = iterator.next();
            }
            return result;
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("ARFF2ARX{instances: ");
        if (instances == null) {
            builder.append("null, ");
        } else {
            builder.append("{rows: ").append(instances.numInstances());
            builder.append(", columns: ").append(instances.numAttributes()).append("}, ");
        }
        builder.append("qi: ");
        if (qi == null || qi.length == 0) {
            builder.append("null, ");
        } else {
            builder.append(Arrays.toString(qi)).append(", ");
        }
        builder.append("sensitive: ");
        if (sensitive == null || sensitive.length == 0) {
            builder.append("null}");
        } else {
            builder.append(Arrays.toString(sensitive)).append("}");
        }

        return builder.toString();
    }

    /**
     * This method determines if an string is element of an array.
     *
     * @param s     Element to locate in the array.
     * @param array Array with string entries.
     * @return Returns true is the given string is in the array.
     */
    protected boolean contains(String s, String[] array) {
        for (String value : array) {
            if (value.equals(s)) {
                return true;
            }
        }
        return false;
    }


}