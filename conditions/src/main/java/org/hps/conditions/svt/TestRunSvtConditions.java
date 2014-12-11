package org.hps.conditions.svt;

import org.hps.conditions.svt.TestRunSvtChannel.TestRunSvtChannelCollection;
import org.hps.conditions.svt.TestRunSvtDaqMapping.TestRunSvtDaqMappingCollection;

import org.hps.conditions.svt.TestRunSvtT0Shift.TestRunSvtT0ShiftCollection;

// TODO: Move all constants to their own class
import static org.hps.conditions.svt.SvtChannel.MAX_NUMBER_OF_SAMPLES;

/**
 * This class contains all test run SVT conditions data by readout channel.
 * {@link TestRunSvtChannel} objects from the SVT channel map should be used to
 * lookup the conditions using the
 * {@link #getChannelConstants(TestRunSvtChannel)} method.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
public class TestRunSvtConditions extends AbstractSvtConditions {

    /**
     * Get the {@link TestRunSvtDaqMappingCollection} associated with these
     * conditions.
     * 
     * @return The SVT DAQ map.
     */
    @Override
    public TestRunSvtDaqMappingCollection getDaqMap() {
        return (TestRunSvtDaqMappingCollection) daqMap;
    }

    /**
     * Get the {@link TestRunSvtChannelCollection} for this set of conditions.
     * 
     * @return The SVT channel map.
     */
    @Override
    public TestRunSvtChannelCollection getChannelMap() {
        return (TestRunSvtChannelCollection) channelMap;
    }

    /**
     * Get the {@link TestRunSvtT0ShiftCollection} associated with these
     * conditions.
     * 
     * @return The {@link TestRunSvtT0ShiftCollection}
     */
    @Override
    public TestRunSvtT0ShiftCollection getT0Shifts() {
        return (TestRunSvtT0ShiftCollection) t0Shifts;
    }

    /**
     * Convert this object to a human readable string. This method prints a
     * formatted table of channel data independently of how its member objects
     * implement their string conversion method. For now, it does not print the
     * time shifts by sensor as all other information is by channel.
     * 
     * @return This object converted to a string, without the DAQ map. TODO:
     *         Make this look more human readable. At the moment, reading this
     *         requires a huge terminal window.
     */
    public String toString() {
        StringBuffer buff = new StringBuffer();

        buff.append('\n');
        buff.append("Printing SVTConditions ...");
        buff.append('\n');
        buff.append('\n');

        // Table header:
        buff.append("Channel ID");
        buff.append("     ");
        buff.append("FPGA ID");
        buff.append("  ");
        buff.append("Hybrid ID");
        buff.append("   ");
        buff.append("Channel");
        buff.append("  ");
        buff.append("Pedestal sample 0");
        buff.append("     ");
        buff.append("Pedestal sample 1");
        buff.append("     ");
        buff.append("Pedestal sample 2");
        buff.append("     ");
        buff.append("Pedestal sample 3");
        buff.append("     ");
        buff.append("Pedestal sample 4");
        buff.append("     ");
        buff.append("Pedestal sample 5");
        buff.append("     ");
        buff.append("Noise sample 0");
        buff.append("     ");
        buff.append("Noise sample 1");
        buff.append("     ");
        buff.append("Noise sample 2");
        buff.append("     ");
        buff.append("Noise sample 3");
        buff.append("     ");
        buff.append("Noise sample 4");
        buff.append("     ");
        buff.append("Noise sample 5");
        buff.append("     ");
        buff.append("Gain");
        buff.append("   ");
        buff.append("Offset");
        buff.append("    ");
        buff.append("Amplitude");
        buff.append("  ");
        buff.append("t0");
        buff.append("       ");
        buff.append("tp");
        buff.append("    ");
        buff.append("Bad Channels");
        buff.append('\n');
        for (int i = 0; i < 115; i++) {
            buff.append("-");
        }
        buff.append('\n');
        // Loop over channels.
        for (TestRunSvtChannel channel : this.getChannelMap()) {

            System.out.println("Channel: " + channel.toString());

            // Get the conditions for the channel.
            ChannelConstants constants = getChannelConstants(channel);
            SvtGain gain = constants.getGain();
            SvtShapeFitParameters shapeFit = constants.getShapeFitParameters();
            SvtCalibration calibration = constants.getCalibration();

            // Channel data.
            buff.append(String.format("%-6d %-5d %-8d %-8d ", channel.getChannelID(), channel.getFpgaID(), channel.getHybridID(), channel.getChannel()));

            // Calibration.
            for (int sample = 0; sample < MAX_NUMBER_OF_SAMPLES; sample++) {
                buff.append(calibration.getPedestal(sample));
                buff.append("      ");
            }
            for (int sample = 0; sample < MAX_NUMBER_OF_SAMPLES; sample++) {
                buff.append(calibration.getNoise(sample));
                buff.append("      ");
            }

            // Gain.
            buff.append(String.format("%-6.4f %-9.4f ", gain.getGain(), gain.getOffset()));

            // Pulse shape.
            buff.append(String.format("%-10.4f %-8.4f %-8.4f", shapeFit.getAmplitude(), shapeFit.getT0(), shapeFit.getTp()));

            // Bad channel.
            buff.append(constants.isBadChannel());

            buff.append('\n');
        }

        buff.append('\n');
        buff.append("Printing SVT DAQ Map...");
        buff.append('\n');
        buff.append('\n');
        buff.append(getDaqMap());

        return buff.toString();
    }

}
