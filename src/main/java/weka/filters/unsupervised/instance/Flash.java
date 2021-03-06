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
package weka.filters.unsupervised.instance;

import java.io.File;
import java.util.*;

import org.deidentifier.arx.*;
import org.deidentifier.arx.criteria.*;

import org.deidentifier.arx.metric.Metric;
import org.deidentifier.arx.ARFF2ARX;
import weka.core.*;
import weka.filters.SimpleBatchFilter;
import weka.filters.UnsupervisedFilter;
import weka.gui.GUIChooser;
import weka.gui.explorer.Explorer;


/**
 * This package provides algorithms to apply k-Anonymity, l-Diversity and t-Closeness with traversal strategy "Flash"
 * for an optimal transformation to an anonymized dataset with respect to minimal information loss.
 * The algorithms originate from the ARX Data Anonymization Toolkit, please read for further information's: http://arx.deidentifier.org/development/algorithms/.
 *
 * @author Andre Breitenfeld
 * @author Simon Koennecke
 * @author Christian Windolf
 */
public class Flash extends SimpleBatchFilter implements UnsupervisedFilter {

    private static final long serialVersionUID = -8436320277782547844L;

    protected Range _quasiIdentifiers = new Range("first-last");
    protected Range _sensitiveAttributes = new Range("");
    protected int _k = KL_MIN_VALUE;
    protected int _l = KL_MIN_VALUE;
    protected double _t = T_MIN_VALUE;
    protected int _lVariant = 0;
    protected double _maxOutliers = 0.0d;
    protected EnumSet<Criterion> _criteria = EnumSet.of(Criterion.kAnonymity);
    protected File _hierarchyFolder = new File(System.getProperty("user.dir"));
    protected ARXConfiguration config;
    protected static final Metric DEFAULT_METRIC = Metric.createHeightMetric();

    /**
     * Enumeration of anonymity criterion.
     */
    private enum Criterion {
        kAnonymity, // k-anonymity
        lDiversity, // l-diversity
        tCloseness  // t-closeness
    }

    /**
     * Array with tags for l-diversity variants.
     */
    public static final Tag[] L_DIVERSITY_VARIANTS = {
            new Tag(0, "Distinct L-Diversity"),
            new Tag(1, "Entropy L-Diversity")
    };

    /* <Constants> */
    protected static final int historySize = 200;
    protected static final double snapshotSizeDataset = 0.2d;
    protected static final double snapshotSizeSnapshot = 0.2d;

    protected static final int KL_MAX_VALUE = 100;
    protected static final int KL_MIN_VALUE = 2;
    protected static final double T_MAX_VALUE = 1.0d;
    protected static final double T_MIN_VALUE = 0.001d;
    /* </Constants> */

    /**
     * Encapsulates literals for tool tips.
     */
    private static final class TipText {
        final static String k = "Parameter k for k-anonymity";
        final static String l = "Parameter l for l-diversity";
        final static String t = "Parameter t for T-Closeness";
        final static String lVariant = "Variant of l-diversity";
        final static String hierarchyFolder = "Specifies the folder which contains the hierarchies data.";
        final static String quasiIdentifier = "The quasi-identifying attributes (names of the columns seperated by ',').";
        final static String sensitiveAttributes = "The sensitive attributes (names of the columns seperated by ',').";
        final static String enableKAnonymity = "Enable k-Anonymity";
        final static String enableLDiversity = "Enable l-Diversity";
        final static String enableTCloseness = "Enable t-Closeness";
        final static String maxOutliers = "Maximum relative amount of dropped data rows";
    }

    /**
     * Returns the global info.
     *
     * @return The global info.
     */
    @Override
    public String globalInfo() {
        return "The Flash Algorithm for provide privarcy criteria";
    }

    /**
     * Returns the tip text for the hierarchy folder property.
     *
     * @return String with corresponding tip text
     */
    public String dataHierarchyFolderTipText() {
        return TipText.hierarchyFolder;
    }

    /**
     * Gets the data hierarchy folder.
     *
     * @return Returns a file object
     */
    public File getDataHierarchyFolder() {
        return _hierarchyFolder;
    }

    /**
     * Sets the data hierarchy folder.
     *
     * @param value Fle object
     */
    public void setDataHierarchyFolder(File value) {
        _hierarchyFolder = value;
    }

    /**
     * Returns the tip text for the quasi-identifiers.
     *
     * @return String with corresponding tip text
     */
    public String dataQuasiIdentifierTipText() {
        return TipText.quasiIdentifier;
    }

    /**
     * Gets the quasi identifiers.
     *
     * @return Range string with quasi identifiers
     */
    public String getDataQuasiIdentifier() {
        return _quasiIdentifiers.getRanges();
    }

    /**
     * Sets the quasi identifiers.
     *
     * @param value Range string with quasi identifiers
     */
    public void setDataQuasiIdentifier(String value) {
        _quasiIdentifiers.setRanges(value);
    }

    /**
     * Returns the tip text for the sensitive attributes.
     *
     * @return String with corresponding tip text
     */
    public String dataSensitiveAttributesTipText() {
        return TipText.sensitiveAttributes;
    }

    /**
     * Gets the sensitive attributes.
     *
     * @return Range string with sensitive attributes
     */
    public String getDataSensitiveAttributes() {
        return _sensitiveAttributes.getRanges();
    }

    /**
     * Sets the sensitive attributes.
     *
     * @param value Range string with sensitive attributes
     */
    public void setDataSensitiveAttributes(String value) {
        _sensitiveAttributes.setRanges(value);
    }

    /**
     * Returns the tip text for enabling k-anonymity.
     *
     * @return String with corresponding tip text
     */
    public String enableKAnonymityTipText() {
        return TipText.enableKAnonymity;
    }

    /**
     * Get k-Anonymity activation.
     *
     * @return Boolean indicate
     */
    public boolean getEnableKAnonymity() {
        return this._criteria.contains(Criterion.kAnonymity);
    }

    /**
     * Enables or disables k-Anonymity.
     *
     * @param value Boolean value
     */
    public void setEnableKAnonymity(boolean value) {
        this.enableCriteria(Criterion.kAnonymity, value);
    }

    /**
     * Returns the tip text for enabling l-diversity.
     *
     * @return String with corresponding tip text
     */
    public String enableLDiversityTipText() {
        return TipText.enableLDiversity;
    }

    /**
     * Get l-Diversity activation.
     *
     * @return Boolean indicate
     */
    public boolean getEnableLDiversity() {
        return this._criteria.contains(Criterion.lDiversity);
    }

    /**
     * Enables or disables l-Diversity.
     *
     * @param value Boolean value
     */
    public void setEnableLDiversity(boolean value) {
        this.enableCriteria(Criterion.lDiversity, value);
    }

    /**
     * Returns the tip text for enabling t-closeness.
     *
     * @return String with corresponding tip text
     */
    public String enableTClosenessTipText() {
        return TipText.enableTCloseness;
    }

    /**
     * Get t-Closeness activation.
     *
     * @return Boolean indicate
     */
    public boolean getEnableTCloseness() {
        return this._criteria.contains(Criterion.tCloseness);
    }

    /**
     * Enables or disables t-Closeness.
     *
     * @param value Boolean value
     */
    public void setEnableTCloseness(boolean value) {
        this.enableCriteria(Criterion.tCloseness, value);
    }

    /**
     * Returns the tip text for the value of k.
     *
     * @return String with corresponding tip text
     */
    public String valueKTipText() {
        return TipText.k;
    }

    /**
     * Gets the value for k.
     *
     * @return Integer for k
     */
    public int getValueK() {
        return Math.max(Math.min(this._k, KL_MAX_VALUE), KL_MIN_VALUE);
    }

    /**
     * Sets the value for k.
     *
     * @param value Integer for k
     */
    public void setValueK(int value) {
        _k = value;
    }

    /**
     * Returns the tip text for the value of l.
     *
     * @return String with corresponding tip text
     */
    public String valueLTipText() {
        return TipText.l;
    }

    /**
     * Gets the value for l.
     *
     * @return Integer for l
     */
    public int getValueL() {
        return Math.max(Math.min(this._l, KL_MAX_VALUE), KL_MIN_VALUE);
    }

    /**
     * Sets the value for l.
     *
     * @param value Integer for l
     */
    public void setValueL(int value) {
        _l = value;
    }

    /**
     * Returns the tip text for the value of t.
     *
     * @return String with corresponding tip text
     */
    public String valueTTipText() {
        return TipText.t;
    }

    /**
     * Gets the value for t.
     *
     * @return Integer for t
     */
    public double getValueT() {
        return Math.max(Math.min(this._t, T_MAX_VALUE), T_MIN_VALUE);
    }

    /**
     * Sets the value for t.
     *
     * @param value Integer for t
     */
    public void setValueT(double value) {
        _t = value;
    }

    /**
     * Returns the tip text for the variant of l-diversity.
     *
     * @return String with corresponding tip text
     */
    public String variantTipText() {
        return TipText.lVariant;
    }

    /**
     * Gets the selected variant for l-Diversity.
     *
     * @return l-Diversity variant
     */
    public SelectedTag getVariant() {
        return new SelectedTag(this._lVariant, L_DIVERSITY_VARIANTS);
    }

    public void setVariant(SelectedTag value) {
        if (value.getTags() == L_DIVERSITY_VARIANTS) {
            this._lVariant = value.getSelectedTag().getID();
        }
    }

    /**
     * Enables or disables the specified anonymity criterion.
     *
     * @param c     The criterion to enable or disable.
     * @param value The state of activation.
     */
    private void enableCriteria(Criterion c, boolean value) {
        if (value)
            this._criteria.add(c);
        else
            this._criteria.remove(c);
    }

    public String maxOutliersTipText() {
        return TipText.maxOutliers;
    }

    /**
     * Gets the value for max outliers.
     *
     * @return Double for max outliers
     */
    public double getMaxOutliers() {
        return this._maxOutliers;
    }

    /**
     * Sets the value for max outliers.
     *
     * @param maxOutliers Double for max outliers
     */
    public void setMaxOutliers(double maxOutliers) {
        this._maxOutliers = maxOutliers;
    }

    public static void main(String[] args) {
        //GUIChooser.main(args);
        runFilter(new Flash(), args);
    }

    @Override
    public Enumeration<Option> listOptions() {
        Vector<Option> newVector = new Vector<Option>();
        Enumeration<Option> options = super.listOptions();

        while (options.hasMoreElements()) {
            newVector.addElement(options.nextElement());
        }

        newVector.addElement(new Option("\tSpecify hierarchy folder", "H", 1, "-H <h>"));
        newVector.addElement(new Option("\tSpecify quasi-identifying attributes", "Q", 1, "-Q <q>"));
        newVector.addElement(new Option("\tSpecify sensitive attributes", "S", 1, "-S <s>"));
        newVector.addElement(new Option("\tSpecify k-Anonymity (default 2)", "K", 1, "-K <k>"));
        newVector.addElement(new Option("\tSpecify l-diversity (default 2)", "L", 1, "-L <l>"));
        newVector.addElement(new Option("\tSpecify l-diversity variant [ Distinct=0 | Entropy=1 ]", "V", 1, "-V <v>"));
        newVector.addElement(new Option("\tSpecify t-Closeness (default " + T_MIN_VALUE + ")", "T", 1, "-T <t>"));
        newVector.addElement(new Option("\tSpecify max outliers (default 0.0)", "O", 1, "-O <o>"));
        return newVector.elements();
    }

    @Override
    public void setOptions(String[] options) throws Exception {
        super.setOptions(options);

        // hierarchy folder
        String tmpStr = Utils.getOption('H', options);
        if (tmpStr.length() != 0) {
            this._hierarchyFolder = new File(tmpStr);
        }

        // quasi-identifying attributes
        tmpStr = Utils.getOption('Q', options);
        if (tmpStr.length() != 0) {
            this.setDataQuasiIdentifier(tmpStr);
        }

        // sensitive attributes
        tmpStr = Utils.getOption('S', options);
        if (tmpStr.length() != 0) {
            this.setDataSensitiveAttributes(tmpStr);
        }

        // k-anonymity
        tmpStr = Utils.getOption('K', options);
        this.setEnableKAnonymity(tmpStr.length() != 0);
        if (this.getEnableKAnonymity()) {
            this.setValueK((Integer.parseInt(tmpStr)));
        }
        // l-diversity
        tmpStr = Utils.getOption('L', options);
        this.setEnableLDiversity(tmpStr.length() != 0);
        if (this.getEnableLDiversity()) {
            this.setValueL((Integer.parseInt(tmpStr)));
            // l-diversity variant
            tmpStr = Utils.getOption('V', options);
            if (tmpStr.length() != 0) {
                int variantIndex = Integer.parseInt(tmpStr);
                if (variantIndex >= 0 && variantIndex < L_DIVERSITY_VARIANTS.length) {
                    this._lVariant = L_DIVERSITY_VARIANTS[variantIndex].getID();
                }
            }
        }
        // t-closeness
        tmpStr = Utils.getOption('T', options);
        this.setEnableTCloseness(tmpStr.length() != 0);
        if (this.getEnableKAnonymity()) {
            if (!tmpStr.isEmpty()) {
                this.setValueT((Float.parseFloat(tmpStr)));
            }
        }
        // max outliers
        tmpStr = Utils.getOption("O", options);
        if (tmpStr.length() != 0) {
            this.setMaxOutliers((Float.parseFloat(tmpStr)));
        }
    }

    @Override
    public String[] getOptions() {
        List<String> options = new LinkedList<String>(Arrays.asList(super.getOptions()));

        if (this.getEnableKAnonymity() && this.getValueK() > 0) {
            options.add("[K=" + this.getValueK() + "]");
        }
        if (this.getEnableLDiversity()) {
            options.add("[L=" + this.getValueL() + "]");
            options.add("[Variant=" + L_DIVERSITY_VARIANTS[this.getVariant().getSelectedTag().getID()].getReadable() + "]");
        }
        if (this.getEnableTCloseness()) {
            options.add("[T=" + this.getValueT() + "]");
        }

        if (this.getMaxOutliers() > 0) {
            options.add("[MaxOutliers=" + this.getMaxOutliers() + "]");
        }

        return options.toArray(new String[0]);
    }

    public Capabilities getCapabilities() {
        Capabilities result = super.getCapabilities();
        result.enableAllAttributes();
        result.enableAllClasses();
        result.enable(Capabilities.Capability.NO_CLASS); // filter doesn't need class to be set
        return result;
    }

    /**
     * Returns the revision string.
     *
     * @return The revision string.
     */
    public String getRevision() {
        return RevisionUtils.extract("$Revision: 1.0.1 $");
    }

    /**
     * Determine the Output format.
     *
     * @param inputFormat The input format.
     * @return The output format.
     */
    protected Instances determineOutputFormat(Instances inputFormat) throws Exception {
        return new Instances(inputFormat, 0);
    }

    /**
     * Process the given instance to apply anonymization algorithm.
     *
     * @param instances The instance on which k-Anonymity, l-diversity or t-closeness should be applied.
     * @return An anonymized instance.
     * @throws IllegalArgumentException, if incorrect parameters are provided
     */
    public Instances process(Instances instances) throws Exception {
        if (instances.numInstances() <= 1) {
            return instances;
        }
        final Instances output = new Instances(instances);

        this._quasiIdentifiers.setUpper(output.numAttributes() - 1);
        this._sensitiveAttributes.setUpper(output.numAttributes() - 1);

        List<String> saColumns = new LinkedList<String>();
        List<String> qiColumns = new LinkedList<String>();

        for (int index : this._sensitiveAttributes.getSelection()) {
            saColumns.add(output.attribute(index).name());
        }

        for (int index : this._quasiIdentifiers.getSelection()) {
            if (saColumns.indexOf(output.attribute(index).name()) == -1) {
                qiColumns.add(output.attribute(index).name());
            }
        }

        this.checkParameters();

        ARFF2ARX converter = new ARFF2ARX(instances).setQi(qiColumns.toArray(new String[qiColumns.size()]));
        config = ARXConfiguration.create();
        config.setMaxOutliers(_maxOutliers);
        config.setMetric(DEFAULT_METRIC);

        // checking which anonymity criterion to apply
        if (this.getEnableKAnonymity()) {
            // add criteria - k-anonymity
            config.addCriterion(new KAnonymity(this.getValueK()));
        }
        if (this.getEnableLDiversity() || this.getEnableTCloseness()) {
            converter.setSensitive(saColumns.toArray(new String[saColumns.size()]));
            // add criteria - l-diversity
            if (this.getEnableLDiversity()) {
                for (String attr : saColumns) {
                    switch (this.getVariant().getSelectedTag().getID()) {
                        case 0: // Distinct L-Diversity
                            config.addCriterion(new DistinctLDiversity(attr, this.getValueL()));
                            break;
                        case 1: // Entropy L-Diversity
                            config.addCriterion(new EntropyLDiversity(attr, this.getValueL()));
                            break;
                    }
                }
            }

            if (this.getEnableTCloseness()) {
                // add criteria - t-closeness for each sensitive attribute
                for (String attr : saColumns) {
                    config.addCriterion(new EqualDistanceTCloseness(attr, this.getValueT()));
                }
            }
        }

        converter.init(_hierarchyFolder, instances.relationName());

        final ARXAnonymizer anonymizer = new ARXAnonymizer();
        anonymizer.setSuppressionString("*");
        anonymizer.setMaximumSnapshotSizeDataset(snapshotSizeDataset);
        anonymizer.setMaximumSnapshotSizeSnapshot(snapshotSizeSnapshot);
        anonymizer.setHistorySize(historySize);
        anonymizer.setRemoveOutliers(true);

        ARXResult result;
        if (this.getDebug()) {
            long start = System.currentTimeMillis();
            result = anonymizer.anonymize(converter.getData(), config);
            long duration = System.currentTimeMillis() - start;
            System.out.println(this.getClass().toString() + duration);
        } else {
            result = anonymizer.anonymize(converter.getData(), config);
        }

        // create output instance
        DataHandle handle = result.getOutput();
        String[] quasiIdentifiers = converter.getQi();
        for (String qi : quasiIdentifiers) {
            // check if attributes was generalized
            if (result.getGlobalOptimum().getGeneralization(qi) > 0) {
                int index = output.attribute(qi).index();
                // collect nominal values
                Set<String> attrSet = new HashSet<String>();
                for (int r = 0; r < handle.getNumRows(); r++) {
                    String value = handle.getValue(r, index);
                    if (!attrSet.contains(value)) {
                        attrSet.add(value);
                    }
                }
                // replace original attribute
                List<String> nominalValues = new LinkedList<String>(attrSet);
                Collections.sort(nominalValues);
                output.replaceAttributeAt(new Attribute(qi, nominalValues), index);

                // insert value
                for (int r = 0; r < handle.getNumRows(); r++) {
                    String value = handle.getValue(r, index);
                    output.get(r).setValue(index, value);
                }
            }
        }

        // set the final output format
        this.setOutputFormat(new Instances(output, 0));

        return output;
    }

    /**
     * Checks if the given parameters are suitable.
     */
    protected void checkParameters() {
        if (this._criteria.isEmpty()) {
            throw new IllegalArgumentException("Please choose at least one privacy criterion");
        }
        if ((this.getEnableTCloseness() || this.getEnableLDiversity())
                && this._sensitiveAttributes.getSelection().length == 0) {
            throw new IllegalArgumentException("Please specify at least one sensitive attribute " +
                    "for criteria l-Diversity or T-Closeness please ");
        }
        if (this.getValueL() > this.getValueK()) {
            throw new IllegalArgumentException(
                    "l must be less or equal to k, but " + this.getValueL() + " > " + this.getValueK());
        }
        if (!_hierarchyFolder.isDirectory()) {
            throw new IllegalArgumentException("The hierarchy folder must be a directory");
        }
        if (_maxOutliers < 0 || _maxOutliers >= 1) {
            throw new IllegalArgumentException("maxOutliers must be between 0 and 1");
        }
    }

}