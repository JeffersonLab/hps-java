package org.hps.readout.svt;

//--- Constants ---//

import static org.hps.conditions.deprecated.HPSSVTConstants.APV_MASK;
import static org.hps.conditions.deprecated.HPSSVTConstants.CHANNEL_MASK;
import static org.hps.conditions.deprecated.HPSSVTConstants.FPGA_MASK;
import static org.hps.conditions.deprecated.HPSSVTConstants.HYBRID_MASK;
import static org.hps.conditions.deprecated.HPSSVTConstants.SAMPLE_MASK;
import static org.hps.conditions.deprecated.HPSSVTConstants.TEMP_MASK;
import static org.hps.conditions.deprecated.HPSSVTConstants.TOTAL_APV25_CHANNELS;
import static org.hps.conditions.deprecated.HPSSVTConstants.TOTAL_APV25_PER_HYBRID;
import static org.hps.conditions.deprecated.HPSSVTConstants.TOTAL_NUMBER_OF_SAMPLES;

/**
 * 
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id: HPSSVTData.java,v 1.8 2012/08/16 01:06:30 meeg Exp $
 */
public class SVTData {

    // 4x32
    int[] data = new int[4];
    // Time of the hit
    int hitTime = 0;

    /**
     * 
     * Creates an SVT data packet from
     * 
     * @param hybridNumber Hybrid number (0-3)
     * @param apvNumber APV25 chip number (0-4)
     * @param channelNumber Sensor strip number (0-127)
     * @param fpgaAddress FPGA address
     * @param adc ADC samples obtained by sampling the shaper output. Currently, six samples are
     *        obtained per raw hit.
     */
    public SVTData(int hybridNumber, int apvNumber, int channelNumber, int fpgaAddress, short[] adc) {
        this.createSVTDataPacket(hybridNumber, apvNumber, channelNumber, fpgaAddress, adc);
    }

    /**
     * Creates and SVT data packet from existing SVT data
     * 
     * @param data The packed data as int array of size 4
     */
    public SVTData(int[] data) {
        if (data.length != 4) {
            throw new RuntimeException("Data sample size is not valid!");
        }

        this.data = data;
    }

    /**
     * Get the packed data for this sample
     * 
     * @return sample The packed data as an int array of size 4.
     */
    public int[] getData() {
        return data;
    }

    /**
     * Creates and SVT data packet
     */
    private void createSVTDataPacket(int hybridNumber, int apvNumber, int channelNumber, int fpgaAddress, short[] adc) {
        createSVTDataPacket(hybridNumber, apvNumber, channelNumber, fpgaAddress, adc, data);
    }

    public static void createSVTDataPacket(int hybridNumber, int apvNumber, int channelNumber, int fpgaAddress, short[] adc, int[] data) {
        /*
         * Sample Data consists of the following: Z[xx:xx] = Zeros, O[xx:xx] = Ones data[0] = O[0],
         * Z[0], Hybrid[1:0], Z[0], ApvChip[2:0], Z[0], Channel[6:0], FpgaAddress[15:0] data[1] =
         * Z[1:0], Sample1[13:0]], Z[1:0], Sample0[13:0] data[2] = Z[1:0], Sample3[13:0]], Z[1:0],
         * Sample2[13:0] data[3] = Z[1:0], Sample5[13:0]], Z[1:0], Sample4[13:0]
         */

        // --- data[0] ---//
        // -----------------//

        // The most significant digit of data[0] is set to 1
        data[0] |= 0x80000000;

        // Insert the hybrid number
        data[0] = (data[0] &= ~(HYBRID_MASK << 28)) | ((hybridNumber & HYBRID_MASK) << 28);

        // Insert the APV number
        data[0] = (data[0] &= ~(APV_MASK << 24)) | ((apvNumber & APV_MASK) << 24);

        // Insert the channel number
        data[0] = (data[0] &= ~(CHANNEL_MASK << 16)) | ((channelNumber & CHANNEL_MASK) << 16);

        // Insert the FPGA address
        data[0] = (data[0] &= ~FPGA_MASK) | (fpgaAddress & FPGA_MASK);

        // --- data[1] ----//
        // ------------------//

        // Add data 0
        data[1] = (data[1] &= ~SAMPLE_MASK) | (adc[0] & SAMPLE_MASK);

        // Add data 1
        data[1] = (data[1] &= ~(SAMPLE_MASK << 16)) | ((adc[1] & SAMPLE_MASK) << 16);

        // --- data[2] ----//
        // ------------------//

        // Add sample 2
        data[2] = (data[2] &= ~SAMPLE_MASK) | (adc[2] & SAMPLE_MASK);

        // Add sample 3
        data[2] = (data[2] &= ~(SAMPLE_MASK << 16)) | ((adc[3] & SAMPLE_MASK) << 16);

        // --- data[3] ----//
        // ------------------//

        // Add sample 4
        data[3] = (data[3] &= ~SAMPLE_MASK) | (adc[4] & SAMPLE_MASK);

        // Add sample 5
        data[3] = (data[3] &= ~(SAMPLE_MASK << 16)) | ((adc[5] & SAMPLE_MASK) << 16);
    }

    /**
     * Get the hybrid number associated with this SVT data packet
     * 
     * @return hybrid number (0-3)
     */
    public int getHybridNumber() {
        return getHybridNumber(data);
    }

    public static int getHybridNumber(int[] data) {
        return (data[0] >>> 28) & HYBRID_MASK;
    }

    /**
     * Get the APV number associated with this SVT data packet
     * 
     * @return APV number (0-4)
     */
    public int getAPVNumber() {
        return getAPVNumber(data);
    }

    public static int getAPVNumber(int[] data) {
        return (data[0] >>> 24) & APV_MASK;
    }

    /**
     * Get the channel number associated with this SVT data packet
     * 
     * @return channel number (0-127)
     */
    public int getChannelNumber() {
        return getChannelNumber(data);
    }

    public static int getChannelNumber(int[] data) {
        return (data[0] >>> 16) & CHANNEL_MASK;
    }

    /**
     * Get the FPGA address associated with this SVT data packet
     * 
     * @return FPGA address
     */
    public int getFPGAAddress() {
        return getFPGAAddress(data);
    }

    public static int getFPGAAddress(int[] data) {
        return data[0] & FPGA_MASK;
    }

    /**
     * Get the nth SVT sample
     * 
     * @param n The sample number of interest. Valid values are 0 to 5
     * @throws RuntimeException if the sample number is out of range
     * @return ADC value of the nth sample
     * 
     * 
     */
    public int getSample(int n) {
        return getSample(n, data);
    }

    public static int getSample(int n, int[] data) {

        switch (n) {
            case 0:
                return data[1] & SAMPLE_MASK;
            case 1:
                return (data[1] >>> 16) & SAMPLE_MASK;
            case 2:
                return data[2] & SAMPLE_MASK;
            case 3:
                return (data[2] >>> 16) & SAMPLE_MASK;
            case 4:
                return data[3] & SAMPLE_MASK;
            case 5:
                return (data[3] >>> 16) & SAMPLE_MASK;
            default:
                throw new RuntimeException("Invalid sample number! Valid range of values for n are from 0 - 5");
        }
    }

    /**
     * Get all SVT samples
     * 
     * @return An array containing all SVT Shaper signal samples
     */
    public short[] getAllSamples() {
        return getAllSamples(data);
    }

    public static short[] getAllSamples(int[] data) {
        short[] samples = new short[TOTAL_NUMBER_OF_SAMPLES];
        // Get all SVT Samples
        for (int index = 0; index < TOTAL_NUMBER_OF_SAMPLES; index++) {
            samples[index] = (short) getSample(index, data);
        }

        return samples;
    }

    /**
     * Get the hit time at which the hit occurred
     * 
     * @return The time at which the hit occurred with respect to the trigger
     */
    public int getHitTime() {
        return hitTime;
    }

    /**
     * Set the hit time at which the hit occurred
     * 
     * @param hitTime : Time at which the hit occurred
     */
    public void setHitTime(int hitTime) {
        this.hitTime = hitTime;
    }

    /**
     *
     */
    public static double[] getTemperature(int[] data) {
        double[] temperatures = new double[(data.length) * 2];

        int tempIndex = 0;
        for (int index = 0; index < data.length; index++) {
            temperatures[tempIndex] = data[index] & TEMP_MASK;
            temperatures[tempIndex + 1] = (data[index] >>> 16) & TEMP_MASK;
            tempIndex += 2;
        }

        return temperatures;
    }

    /**
     * Get the sensor (a k a physical) channel corresponding to a raw chip channel
     * 
     * @param apv : APV25 chip number
     * @param channel : APV25 raw channel number
     * 
     * @return sensor channel number
     */
    public static int getSensorChannel(int apv, int channel) {
        int sensorChannel = (TOTAL_APV25_PER_HYBRID - apv - 1) * TOTAL_APV25_CHANNELS + channel;
        if (sensorChannel < 0 || sensorChannel >= TOTAL_APV25_PER_HYBRID * TOTAL_APV25_CHANNELS) {
            throw new RuntimeException("sensor channel " + sensorChannel + " is outside of valid range! APV " + apv + ", channel " + channel);
        }
        return sensorChannel;
    }

    public static int getAPV(int sensorChannel) {
        return TOTAL_APV25_PER_HYBRID - (sensorChannel / TOTAL_APV25_CHANNELS) - 1;
    }

    public static int getAPVChannel(int sensorChannel) {
        return sensorChannel % TOTAL_APV25_CHANNELS;
    }

    public static int getSensorChannel(int[] data) {
        return getSensorChannel(getAPVNumber(data), getChannelNumber(data));
    }

    public int getSensorChannel() {
        return getSensorChannel(data);
    }
}
