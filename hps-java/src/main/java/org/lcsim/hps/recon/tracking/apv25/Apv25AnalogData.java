package org.lcsim.hps.recon.tracking.apv25;

//--- org.lcsim ---//
import org.lcsim.detector.tracker.silicon.SiSensor;

/**
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id: Apv25AnalogData.java,v 1.3 2012/08/17 01:17:08 omoreno Exp $ 
 */
public class Apv25AnalogData {
    
    private SiSensor sensor = null;
    private int apv25;
    
    // APV25 output stream
    private double[] apv25AnalogOutput = new double[140];
    private double[] header = {4.0, 4.0, 4.0};
    private double[] address = {-4.0, -4.0, -4.0, -4.0, -4.0, -4.0, -4.0, -4.0};
    private double   error = 4.0;
    private double[] samples = new double[128];
    
    /**
     * Default Ctor
     */
    public Apv25AnalogData(){
        


        // Copy the header, address, error and samples
        System.arraycopy(header, 0, apv25AnalogOutput, 0, header.length);
        System.arraycopy(address, 0, apv25AnalogOutput, 3, address.length);
        apv25AnalogOutput[11] = error;
        
        // Create the array which will contain the output.  The array values will range
        // from -4 microAmps to 4 microAmps
        for(int index = 12; index < apv25AnalogOutput.length; index++){
            apv25AnalogOutput[index] = -4.0; // microAmps
        }
    }
    
    /**
     * 
     */
    public Apv25AnalogData(SiSensor sensor, int apv25){
        this();
        
        // Set the sensor and APV number associated with this data
        this.sensor = sensor;
        this.apv25 = apv25;
    }
    
    /**
     * 
     */
    public void setChannelData(int channel, double data){
        apv25AnalogOutput[12 + channel] += data;
    }
    
    /**
     * 
     */
    public void setSensor(SiSensor sensor){
        this.sensor = sensor;
    }
    
    /**
     * 
     */
    public void setApv(int apv25){
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
        System.arraycopy(apv25AnalogOutput, 12, samples, 0, samples.length);
        return samples;
    }
    
    /**
     * 
     */
    public double[] getApv25AnalogOutput(){
        return this.apv25AnalogOutput;
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
    public int getApvNumber(){
        return this.apv25;
    }
}
