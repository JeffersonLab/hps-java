package org.hps.record.triggerbank;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.ArrayUtils;
import org.hps.record.evio.EvioEventConstants;
import org.lcsim.event.GenericObject;

/**
 * Parse VTP bank and converts it into <code>VTPCluster</code>,
 * <code>VTPSinglesTrigger</code>, <code>VTPPairsTrigger</code>,
 * <code>VTPCalibrationTrigger</code>, <code>VTPMultiplicityTrigger</code> and
 * <code>VTPFEETrigger</code>. 
 * Data Format: bit31 = 1 - Data defining word bit31 = 0 - Data continuation word.
 *
 * For bit31 = 1, bit 30-27 contain the data type: Data Types: 
 * 0 Block Header 
 * 1 Block Trailer 
 * 2 Event Header 
 * 3 Trigger Time 
 * 12 Expansion (Data SubType) 
 * 12.2 HPS Cluster 
 * 12.3 HPS Single Trigger 
 * 12.4 HPS Pair Trigger 
 * 12.5 HPS Calibration Trigger 
 * 12.6 HPS Cluster Multiplicity Trigger 
 * 12.7 HPS FEE Trigger 
 * 14 Data Not Valid (empty module) 
 * 15 Filler Word (non-data)
 * 
 * @author Tongtong Cao <caot@jlab.org>
 */

public class VTPData implements GenericObject {
    /**
     * Setup logger.
     */
    private static final Logger LOGGER = Logger.getLogger(VTPData.class.getPackage().getName());

    /**
     * The EvIO bank header tag for VTP data banks.
     */
    public static final int BANK_TAG = EvioEventConstants.VTP_BANK_TAG; // VTP bank tag

    /**
     * The data bank of TOP VTP.
     */
    private int[] bank;
    private long triggerTime = 0;

    // Collections for storing the decoded VTP bank data.
    private final List<VTPCluster> clusterList = new ArrayList<VTPCluster>();
    private final List<VTPSinglesTrigger> singlesTriggerList = new ArrayList<VTPSinglesTrigger>();
    private final List<VTPPairsTrigger> pairsTriggerList = new ArrayList<VTPPairsTrigger>();
    private final List<VTPCalibrationTrigger> calibrationTriggerList = new ArrayList<VTPCalibrationTrigger>();
    private final List<VTPMultiplicityTrigger> multiplicityTriggerList = new ArrayList<VTPMultiplicityTrigger>();
    private final List<VTPFEETrigger> fEETriggerList = new ArrayList<VTPFEETrigger>();

    public VTPData(GenericObject vtp_generic, GenericObject vtp_generic_rocid) {
        bank = getBank(vtp_generic, vtp_generic_rocid);
        decodeData();
    }

    /**
     * Parses the bank so the object can be used in analysis.
     */
    public final void decodeData() {
        for (int i = 0; i < bank.length; i++) {
            if ((bank[i] & 1 << 31) != 0) { // Type data set.
                int type = bank[i] >> 27 & 0x0F;
                int subtype = -1;

                switch (type) {
                    case 3: // Trigger time
                        triggerTime = (bank[i] & 0x00FFFFFF) + ((bank[i + 1] & 0x00FFFFFF) << 24);
                        i++;
                        break;
                    case 12: // Expansion type
                        subtype = bank[i] >> 23 & 0x0F;
                        switch (subtype) {
                            case 2: // HPS Cluster
                                VTPCluster cluster = new VTPCluster(bank[i], bank[i + 1]);
                                clusterList.add(cluster);
                                i++;
                                break;
                            case 3: // HPS Single Trigger
                                VTPSinglesTrigger singlesTrigger = new VTPSinglesTrigger(bank[i]);
                                singlesTriggerList.add(singlesTrigger);
                                break;
        
                            case 4: // HPS Pair Trigger
                                VTPPairsTrigger pairsTrigger = new VTPPairsTrigger(bank[i]);
                                pairsTriggerList.add(pairsTrigger);
                                break;
                            case 5: // HPS Calibration Trigger
                                VTPCalibrationTrigger calibrationTrigger = new VTPCalibrationTrigger(bank[i]);
                                calibrationTriggerList.add(calibrationTrigger);
                                break;
                            case 6: // HPS Cluster Multiplicity Trigger
                                VTPMultiplicityTrigger multiplicityTrigger = new VTPMultiplicityTrigger(bank[i]);
                                multiplicityTriggerList.add(multiplicityTrigger);
                                break;
                            case 7: // HPS FEE Trigger
                                VTPFEETrigger fEETrigger = new VTPFEETrigger(bank[i]);
                                fEETriggerList.add(fEETrigger);
                                break;
                        }
                    default:
                        LOGGER.log(Level.FINE, "At " + i + " invalid or ignored HPS type: " + type + " subtype: " + subtype
                                + "; subtype = -1 means no subtype");
                }
            }
        }
    }

    public static int getTag() {
        return BANK_TAG;
    }

    /**
     * Return the int bank of TOP VTP from LCIO.
     *
     * @param object, object
     */
    public static int[] getBank(GenericObject vtp_generic, GenericObject vtp_generic_rocid) {
        int[] bank = new int[] {};
        for (int i = 0; i < vtp_generic.getNInt(); i++) {
            // TOP and BOT VTP banks are identical, so only store the TOP VTP bank.
            if (vtp_generic_rocid.getIntVal(i) == EvioEventConstants.VTP_TOP_RocID)
                bank = ArrayUtils.addAll(bank, vtp_generic.getIntVal(i + 1));
        }
        return bank;
    }

    public long getTriggerTime() {
        return triggerTime;
    }

    /**
     * Gets the list of clusters reported by VTP.
     * 
     * @return Returns the clusters as a <code>List</code> collection of
     *         <code>VTPCluster</code> objects.
     */
    public List<VTPCluster> getClusters() {
        return clusterList;
    }

    /**
     * Gets the list of singles triggers reported by VTP.
     * 
     * @return Returns the singles triggers as a <code>List</code> collection of
     *         <code>VTPSinglesTrigger</code> objects.
     */
    public List<VTPSinglesTrigger> getSinglesTriggers() {
        return singlesTriggerList;
    }

    /**
     * Gets the list of pairs triggers reported by VTP.
     * 
     * @return Returns the pairs triggers as a <code>List</code> collection of
     *         <code>VTPPairsTrigger</code> objects.
     */
    public List<VTPPairsTrigger> getPairsTriggers() {
        return pairsTriggerList;
    }

    /**
     * Gets the list of calibration triggers reported by VTP.
     * 
     * @return Returns the calibration triggers as a <code>List</code> collection of
     *         <code>VTPCalibrationTrigger</code> objects.
     */
    public List<VTPCalibrationTrigger> getCalibrationTriggers() {
        return calibrationTriggerList;
    }

    /**
     * Gets the list of cluster multiplicity triggers reported by VTP.
     * 
     * @return Returns the multiplicity triggers as a <code>List</code> collection
     *         of <code>VTPMultiplicityTrigger</code> objects.
     */
    public List<VTPMultiplicityTrigger> getMultiplicityTriggers() {
        return multiplicityTriggerList;
    }

    /**
     * Gets the list of FEE triggers reported by VTP.
     * 
     * @return Returns the FEE triggers as a <code>List</code> collection of
     *         <code>VTPFEETrigger</code> objects.
     */
    public List<VTPFEETrigger> getFEETriggers() {
        return fEETriggerList;
    }

    @Override
    public int getNInt() {
        return bank.length;
    }

    @Override
    public int getNFloat() {
        return 0;
    }

    @Override
    public int getNDouble() {
        return 0;
    }

    @Override
    public int getIntVal(int index) {
        return bank[index];
    }

    @Override
    public float getFloatVal(int index) {
        throw new UnsupportedOperationException("No float values in " + this.getClass().getSimpleName());
    }

    @Override
    public double getDoubleVal(int index) {
        throw new UnsupportedOperationException("No double values in " + this.getClass().getSimpleName());
    }

    @Override
    public boolean isFixedSize() {
        return true;
    }

}
