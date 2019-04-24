/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.evio;

//import static org.hps.evio.EventConstants.ECAL_BANK_NUMBER;
//import static org.hps.evio.EventConstants.ECAL_BOTTOM_BANK_TAG;
//import static org.hps.evio.EventConstants.ECAL_PULSE_INTEGRAL_BANK_TAG;
//import static org.hps.evio.EventConstants.ECAL_PULSE_INTEGRAL_FORMAT;
//import static org.hps.evio.EventConstants.ECAL_TOP_BANK_TAG;

//import org.hps.conditions.database.DatabaseConditionsManager;
//import org.hps.conditions.ecal.EcalConditions;
//import org.jlab.coda.jevio.BaseStructure;
//import org.jlab.coda.jevio.CompositeData;
//import org.jlab.coda.jevio.DataType;
import org.jlab.coda.jevio.EventBuilder;
//import org.jlab.coda.jevio.EvioBank;
//import org.jlab.coda.jevio.EvioException;
import org.lcsim.event.EventHeader;
//import org.lcsim.event.RawCalorimeterHit;
//import org.lcsim.event.RawTrackerHit;
//import org.lcsim.geometry.Detector;
//import org.lcsim.geometry.IDDecoder;
//import org.lcsim.geometry.Subdetector;
//import org.lcsim.lcio.LCIOConstants;

/**
 *
 * @author rafopar
 */
public class HodoHitWriter implements HitWriter {

    private String hitCollectionName = "HodoReadoutHits";
    private int mode = EventConstants.HODO_PULSE_INTEGRAL_MODE;
    
    
    private int verbosity = 1;

    public HodoHitWriter() {
    }

    @Override
    public boolean hasData(EventHeader event) {

        return true; // Temporrary, just for not givin syntax error
    }

    @Override
    public void writeData(EventHeader event, EventBuilder builder) {
        System.out.println("Kuku");
    }
    
    @Override
    public void writeData(EventHeader event, EventHeader toEvent) {
    }    

    @Override
    public void setVerbosity(int verbosity) {
        this.verbosity = verbosity;
    }

}
