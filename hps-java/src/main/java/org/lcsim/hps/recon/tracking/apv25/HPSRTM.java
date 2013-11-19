
package org.lcsim.hps.recon.tracking.apv25;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version  $Id: HPSRTM.java,v 1.3 2013/03/15 21:05:28 meeg Exp $
 */
public class HPSRTM {
    
    private Map<Integer, double[]> analogData;
    private Map<Integer, double[]> digitalData;

    private static double INPUT_STAGE_GAIN = 2;
    private static double RESISTOR_VALUE = 100; // Ohms
    
    double adcHighRef = 1000;   // mVolts
    double adcLowRef = -1000;   // mVolts
    double adcResolution = 0;  //bits
    double adcVoltageResolution = 0; // Volts
    int voltageIntervals;
    
    
    /**
     * Constructor
     */
    public HPSRTM(int bits)
    {
        // To do: In order to increase speed, HashMap should be initialized
        // to a specified capacity
        digitalData = new HashMap<Integer, double[]>();
        
        adcResolution = bits;
        voltageIntervals = (int) Math.pow(2, bits);

        adcVoltageResolution 
           =  (adcHighRef - adcLowRef)/voltageIntervals; // mV
        
    }
    
    //--- Methods ---//
    //---------------//
    
    
    /**
     * 
     * @param data 
     */
    public Map<Integer, double[]> digitize( Map<Integer, double[]> data )
    {
        digitalData = data;

        // Amplify the incoming analog signal
        amplifySignal();
        
        // Loop over all apv25 analog signals and digitize them
        for(Map.Entry<Integer, double[]> entry : digitalData.entrySet()){
            
            
            
            // Aquire the amplified signal
            double[] digitalSignal = entry.getValue();

            // Digitize the apv25 output
            for(int index = 0; index < digitalSignal.length; index++){
                
                digitalSignal[index]
                   = Math.floor((digitalSignal[index] 
                                    - adcLowRef)/adcVoltageResolution);
            }

            digitalData.put(entry.getKey(), digitalSignal);
            
        }

        return digitalData;
    }
    
    /**
     * 
     */
    public void amplifySignal()
    {
        
        // Loop over all apv25 analog data
        for(Map.Entry<Integer, double[]> entry : digitalData.entrySet() )
        {
            // Obtain the apv25 output
            double[] apv25Output = entry.getValue();
            
            for(int index = 0; index < apv25Output.length; index++ ){
                
                // Convert input current to voltage
                apv25Output[index] *= RESISTOR_VALUE;
                
                // Amplify the input signal
                apv25Output[index] *= INPUT_STAGE_GAIN;
            }

            // Store the amplified APV25 output
            digitalData.put(entry.getKey(), apv25Output);
            
        }
    }
    
    /**
     * 
     */
    public void setResolution( int bits )
    {
        adcResolution = bits;
    }
    
    /**
     * 
     */
   public void printData()
   {
       
       double[] data = digitalData.get("1");
       System.out.println(data.length);
       System.out.println(" ]");
   }
}
