package org.hps.conditions.svt;

import org.hps.conditions.database.Field;
import org.hps.conditions.database.Table;
import org.hps.util.Pair;

/**
 * This class encapsulates the Test Run SVT DAQ map.
 *
 * @author Omar Moreno, UCSC
 */
@Table(names = {"test_run_svt_daq_map"})
public final class TestRunSvtDaqMapping extends AbstractSvtDaqMapping {

    /**
     * The collection implementation for {@link TestRunSvtDaqMapping} objects.
     */
    public static class TestRunSvtDaqMappingCollection extends AbstractSvtDaqMappingCollection<TestRunSvtDaqMapping> {
       
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
                if (daqPair.getFirstElement() == daqMapping.getFpgaID()
                        && daqPair.getSecondElement() == daqMapping.getHybridID()) {
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
        @Override
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
            for (final TestRunSvtDaqMapping daqMapping : this) {
                final TestRunSvtDaqMapping testRunDaqMapping = daqMapping;
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
    @Field(names = {"fpga"})
    public Integer getFpgaID() {
        return this.getFieldValue("fpga");
    }

    /**
     * Get the Hybrid ID.
     *
     * @return the Hybrid ID
     */
    @Field(names = {"hybrid"})
    public Integer getHybridID() {
        return this.getFieldValue("hybrid");
    }
}
