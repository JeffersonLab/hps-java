package org.hps.conditions.svt;

import org.hps.conditions.database.Converter;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.MultipleCollectionsAction;
import org.hps.conditions.database.Table;
import org.hps.util.Pair;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;

/**
 * This class encapsulates the Test Run SVT DAQ map.
 *
 * @author <a href="mailto:omoreno1@ucsc.edu">Omar Moreno</a>
 */
@Table(names = { "test_run_svt_daq_map" })
@Converter(multipleCollectionsAction = MultipleCollectionsAction.LAST_CREATED)
public final class TestRunSvtDaqMapping extends AbstractSvtDaqMapping {

    /**
     * The collection implementation for {@link TestRunSvtDaqMapping} objects.
     */
    public static class TestRunSvtDaqMappingCollection extends AbstractSvtDaqMappingCollection<TestRunSvtDaqMapping> {

        /**
         * Get a test run DAQ pair (FPGA and Hybrid ID) for the given {@linkplain HpsTestRunSiSensor}.
         *
         * @param sensor a sensor of type {@link HpsTestRunSiSensor}
         * @return the DAQ pair associated with the sensor
         */
        @Override
        public Pair<Integer, Integer> getDaqPair(final HpsSiSensor sensor) {

            final String svtHalf = sensor.isTopLayer() ? TOP_HALF : BOTTOM_HALF;
            for (TestRunSvtDaqMapping daqMapping : this) {

                if (svtHalf.equals(daqMapping.getSvtHalf()) && daqMapping.getLayerNumber() == sensor.getLayerNumber()) {

                    return new Pair<Integer, Integer>(daqMapping.getFpgaID(), daqMapping.getHybridID());
                }
            }
            return null;
        }

        /**
         * Get the orientation of a sensor using the FPGA and Hybrid ID. If the FPGA and Hybrid ID combination is not
         * found, return null.
         * 
         * @param daqPair (Pair<FPGA ID, Hybrid ID>) for a given sensor
         * @return "A" if sensor orientation is Axial; "S" if Stereo; null if daqPair doesn't exist
         */
        @Override
        public String getOrientation(final Pair<Integer, Integer> daqPair) {
            for (final TestRunSvtDaqMapping daqMapping : this) {
                if (daqPair.getFirstElement() == ((TestRunSvtDaqMapping) daqMapping).getFpgaID()
                        && daqPair.getSecondElement() == ((TestRunSvtDaqMapping) daqMapping).getHybridID()) {
                    return daqMapping.getOrientation();
                }
            }
            return null;
        }

        /**
         * Convert {@link TestRunSvtDaqMapping} to a string.
         * 
         * @return This object converted to a string.
         */
        public String toString() {
            final StringBuffer buffer = new StringBuffer();
            buffer.append("FPGA ID: ");
            buffer.append(" ");
            buffer.append("Hybrid ID: ");
            buffer.append(" ");
            buffer.append("SVT half: ");
            buffer.append(" ");
            buffer.append("Layer");
            buffer.append(" ");
            buffer.append("Orientation: ");
            buffer.append(" ");
            buffer.append('\n');
            buffer.append("----------------------");
            buffer.append('\n');
            for (TestRunSvtDaqMapping daqMapping : this) {
                final TestRunSvtDaqMapping testRunDaqMapping = (TestRunSvtDaqMapping) daqMapping;
                buffer.append(testRunDaqMapping.getFpgaID());
                buffer.append("    ");
                buffer.append(testRunDaqMapping.getHybridID());
                buffer.append("    ");
                buffer.append(testRunDaqMapping.getSvtHalf());
                buffer.append("    ");
                buffer.append(String.format("%-2d", testRunDaqMapping.getLayerNumber()));
                buffer.append("    ");
                buffer.append(testRunDaqMapping.getOrientation());
                buffer.append("    ");
                buffer.append('\n');
            }
            return buffer.toString();
        }
    }

    /**
     * Get the FPGA ID.
     *
     * @return the FPGA ID
     */
    @Field(names = { "fpga" })
    public int getFpgaID() {
        return getFieldValue("fpga");
    }

    /**
     * Get the Hybrid ID.
     *
     * @return the Hybrid ID
     */
    @Field(names = { "hybrid" })
    public int getHybridID() {
        return getFieldValue("hybrid");
    }
}
