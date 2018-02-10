package org.hps.readout.ecal.updated;

import org.hps.readout.ReadoutDataManager;
import org.hps.readout.SLICDataReadoutDriver;
import org.lcsim.event.SimCalorimeterHit;

/**
 * <code>SimCalorimeterHitReadoutDriver</code> handles SLIC objects
 * in input Monte Carlo files of type {@link
 * org.lcsim.event.SimCalorimeterHit SimCalorimeterHit}.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @see org.hps.readout.SLICDataReadoutDriver
 */
public class SimCalorimeterHitReadoutDriver extends SLICDataReadoutDriver<SimCalorimeterHit> {
    /**
     * Instantiate an instance of {@link
     * org.hps.readout.SLICDataReadoutDriver SLICDataReadoutDriver}
     * for objects of type {@link
     * org.lcsim.event.SimCalorimeterHit SimCalorimeterHit} and set
     * the appropriate LCIO flags.
     */
    public SimCalorimeterHitReadoutDriver() {
        super(SimCalorimeterHit.class, 0xe0000000);
    }
    
    @Override
    protected void writeData(java.util.List<SimCalorimeterHit> data) {
        writer.write("Event ??? - " + ReadoutDataManager.getCurrentTime());
        writer.write("Output");
        for(SimCalorimeterHit hit : data) {
            writer.write(String.format("%f;%f;%d", hit.getRawEnergy(), hit.getTime(), hit.getCellID()));
        }
    }
}