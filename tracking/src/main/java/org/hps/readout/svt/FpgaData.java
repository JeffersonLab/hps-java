package org.hps.readout.svt;

//--- java ---//
import static org.hps.conditions.deprecated.HPSSVTConstants.ADC_TEMP_COUNT;
import static org.hps.conditions.deprecated.HPSSVTConstants.BETA;
import static org.hps.conditions.deprecated.HPSSVTConstants.CONST_A;
import static org.hps.conditions.deprecated.HPSSVTConstants.MAX_TEMP;
import static org.hps.conditions.deprecated.HPSSVTConstants.MIN_TEMP;
import static org.hps.conditions.deprecated.HPSSVTConstants.R_DIV;
import static org.hps.conditions.deprecated.HPSSVTConstants.TEMP_INC;
import static org.hps.conditions.deprecated.HPSSVTConstants.TEMP_K0;
import static org.hps.conditions.deprecated.HPSSVTConstants.TEMP_MASK;
import static org.hps.conditions.deprecated.HPSSVTConstants.V_MAX;
import static org.hps.conditions.deprecated.HPSSVTConstants.V_REF;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//--- org.lcsim ---//
import org.lcsim.event.GenericObject;

//-- Constants ---//

/**
 * Generic object to contain hybrid temperatures and data tail value. Converts and ADC value to a
 * temperature in celsius
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id: FpgaData.java,v 1.3 2012/08/16 01:06:30 meeg Exp $
 */
// FIXME: This seems like it might belong in org.hps.evio where it is used most.
// It is also used by the apv25 package so leaving here for now. --JM
public class FpgaData implements GenericObject {

    int fpgaID;
    List<Double> temperatures = new ArrayList<Double>();
    int tail;
    private static double[] temperatureTable = null;

    /**
     * 
     * @param temperature : array containing hybrid temperatures
     * @param tail : word present at the end of a FPGA data set
     */
    public FpgaData(int fpgaID, int[] data, int tail) {
        this.fpgaID = fpgaID;

        int[] temperature = extractTemperature(data);

        // Fill the temperature lookup table
        fillTemperatureTable();

        this.tail = tail;

        // Fill the temperature list
        for (int index = 0; index < temperature.length; index++) {
            temperatures.add(intToTemperature(temperature[index]));
        }
    }

    public FpgaData(int fpgaID, double[] temperatures, int tail) {
        this.fpgaID = fpgaID;

        this.tail = tail;

        this.temperatures.clear();
        // Fill the temperature list
        for (int index = 0; index < temperatures.length; index++) {
            this.temperatures.add(temperatures[index]);
        }
    }

    /**
     * Extract temperatures from the SVT data stream
     * 
     * @param data : array containing temperature data
     * @return temperatures
     * 
     */
    public static int[] extractTemperature(int[] data) {
        int[] temperatures = new int[(data.length) * 2];

        int tempIndex = 0;
        for (int index = 0; index < data.length; index++) {
            temperatures[tempIndex] = data[index] & TEMP_MASK;
            temperatures[tempIndex + 1] = (data[index] >>> 16) & TEMP_MASK;
            tempIndex += 2;
        }
        return temperatures;
    }

    /**
     * Temperature lookup table. Takes an ADC value and returns a temperature in Celsius
     */
    private static void fillTemperatureTable() {

        if (temperatureTable == null) {
            temperatureTable = new double[ADC_TEMP_COUNT];

            double tempK, res, volt;
            int idx;
            double temp = MIN_TEMP;

            while (temp < MAX_TEMP) {

                tempK = TEMP_K0 + temp;
                res = CONST_A * Math.exp(BETA / tempK);
                volt = (res * V_MAX) / (R_DIV + res);
                idx = (int) ((volt / V_REF) * (double) (ADC_TEMP_COUNT - 1));
                if (idx < ADC_TEMP_COUNT) {
                    temperatureTable[idx] = temp;
                }
                temp += TEMP_INC;
            }
        }
    }

    public static double intToTemperature(int tempIndex) {
        fillTemperatureTable();
        return temperatureTable[tempIndex];
    }

    public static int temperatureToInt(double temperature) {
        fillTemperatureTable();
        return Math.abs(Arrays.binarySearch(temperatureTable, temperature));
    }

    public int[] extractData() {
        fillTemperatureTable();

        int[] header = new int[(temperatures.size() + 1) / 2];
        for (int i = 0; i < temperatures.size(); i++) {
            if (i % 2 == 0) {
                header[i / 2] = (header[i / 2] &= ~TEMP_MASK) | (temperatureToInt(temperatures.get(i)) & TEMP_MASK);
            } else {
                header[i / 2] = (header[i / 2] &= ~(TEMP_MASK << 16)) | ((temperatureToInt(temperatures.get(i)) & TEMP_MASK) << 16);
            }
        }
        return header;
    }

    public int getFpga() {
        return fpgaID;
    }

    public int getTail() {
        return tail;
    }

    /**
     * Get the temperature at a given index
     */
    @Override
    public double getDoubleVal(int index) {
        return temperatures.get(index);
    }

    @Override
    public float getFloatVal(int index) {
        return 0;
    }

    /**
     * Get the tail value
     */
    @Override
    public int getIntVal(int index) {
        switch (index) {
            case 0:
                return getFpga();
            case 1:
                return getTail();
            default:
                throw new ArrayIndexOutOfBoundsException(index);
        }
    }

    @Override
    public int getNDouble() {
        return temperatures.size();
    }

    @Override
    public int getNFloat() {
        return 0;
    }

    @Override
    public int getNInt() {
        return 2;
    }

    @Override
    public boolean isFixedSize() {
        return false;
    };
}
