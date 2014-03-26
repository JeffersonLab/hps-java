package org.hps.recon.tracking.apv25;

//--- org.lcsim ---//
import org.lcsim.detector.tracker.silicon.SiSensor;

/**
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id: Apv25DigitalData.java,v 1.3 2012/08/17 01:17:08 omoreno Exp $  
 */
public class Apv25DigitalData {
    
    // TODO: Add ability to associate the analog data to a SiSensor and APV25
    private SiSensor sensor = null;
    private int apv25;
    
    // APV25 output stream
    private double[] apv25DigitalOutput = new double[140];
    private double[] header = new double[3];
    private double[] address = new double[8];
    private double   error;
    private double[] samples = new double[128];
    
    /**
     * Default Ctor
     */
    public Apv25DigitalData(SiSensor sensor, int apv25, double[] apv25DigitalOutput){
        
        // Check if the output format is valid
        if(apv25DigitalOutput.length != this.apv25DigitalOutput.length) 
            throw new RuntimeException("APV25 output format is invalid!");
        
        System.arraycopy(apv25DigitalOutput, 0, this.apv25DigitalOutput, 0, apv25DigitalOutput.length);
        System.arraycopy(apv25DigitalOutput, 0, header, 0, header.length);
        System.arraycopy(apv25DigitalOutput, 3, address, 0, address.length);
        error   = apv25DigitalOutput[11];
        System.arraycopy(apv25DigitalOutput, 12, samples, 0, samples.length);
        
        // Set the sensor and APV number associated with this data
        this.sensor = sensor;
        this.apv25 = apv25;
    }
    
    /**
     * 
     */
    public double[] getHeader(){
        return header;
    }
    
    /**
     * 
     */
    public double[] getAddress(){
        return address;
    }
    
    /**
     * 
     */
    public double getErrorBit(){
        return error;
    }
    
    /**
     * 
     */
    public double[] getSamples(){
        return samples;
    }
    
    /**
     * 
     */
    public double[] getApv25DigitalOutput(){
        return apv25DigitalOutput;
    }
    
    /**
     * 
     */
    public SiSensor getSensor(){
        return this.sensor;
    }
    
    /**
     * 
     */
    public int getApv(){
        return this.apv25;
    }
}
