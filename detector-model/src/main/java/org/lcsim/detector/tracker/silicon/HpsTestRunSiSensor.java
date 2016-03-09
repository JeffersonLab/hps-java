package org.lcsim.detector.tracker.silicon;

import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IIdentifier;


/**
 * This class extends {@link HpsSiSensor} with conditions specific to HPS SVT half-modules
 * (sensors) used during the test run.  Each half-module is uniquely identified by 
 * an FPGA/Hybrid ID pair which is then related to calibration conditions such as
 * baseline, noise gain etc.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
public class HpsTestRunSiSensor extends HpsSiSensor {
    
    
    protected int fpgaID;
    protected int hybridID;
            
    
    /**
     * This class constructor matches the signature of <code>SiSensor</code>.
     * @param sensorid The sensor ID.
     * @param name The name of the sensor.
     * @param parent The parent DetectorElement.
     * @param support The physical support path.
     * @param id The identifier of the sensor.
     */
    public HpsTestRunSiSensor(
            int sensorid,
            String name,
            IDetectorElement parent,
            String support,
            IIdentifier id)
    {
        super(sensorid, name, parent, support, id);
    }
    
    

    /**
     * Get the FPGA ID associated with this sensor.
     * 
     * @return The FPGA ID
     */
    public int getFpgaID() {
        return fpgaID;
    }

    /**
     * Get the hybrid ID associated with this sensor.
     * 
     * @return The hybrid ID
     */
    public int getHybridID() {
        return hybridID;
    }

    @Override
    public int getFebID(){
        throw new RuntimeException("This method is not supported for the HpsTestRunSiSensor.");
    }
    
    @Override
    public int getFebHybridID(){
        throw new RuntimeException("This method is not supported for the HpsTestRunSiSensor.");
    }

    /**
     * Set the FPGA ID associated with this sensor.
     * 
     * @param The FPGA ID
     */
    public void setFpgaID(int fpgaID) {
        this.fpgaID = fpgaID;
    }

    /**
     * Set the hybrid ID associated with this sensor.
     * 
     * @param The hybrid ID.
     */
    public void setHybridID(int hybridID) {
        this.hybridID = hybridID;
    }
    
    @Override
    public void setFebID(int febID) {
        throw new RuntimeException("This method is not supported for the HpsTestRunSiSensor.");
    }
    
    @Override
    public void setFebHybridID(int febHybridID) {
        throw new RuntimeException("This method is not supported for the HpsTestRunSiSensor.");
    }
}
