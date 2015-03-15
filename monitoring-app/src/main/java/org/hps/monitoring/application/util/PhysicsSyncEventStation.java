/**
 * 
 */
package org.hps.monitoring.application.util;

import java.io.IOException;
import java.util.List;

import org.hps.evio.TriggerConfigEvioReader;
import org.hps.monitoring.application.util.EventTagFilter.SyncTagFilter;
import org.hps.monitoring.application.util.RunnableEtStation.RunnableEtStationConfiguration;
import org.hps.recon.ecal.daqconfig.ConfigurationManager;
import org.hps.recon.ecal.daqconfig.EvioDAQParser;
import org.hps.record.evio.EvioEventConstants;
import org.jlab.coda.et.EtEvent;
import org.jlab.coda.et.EtSystem;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;
import org.lcsim.event.base.BaseLCSimEvent;

/**
 * This is an ET station that looks for DAQ configuration events
 * from the ET server and updates the global DAQ configuration if
 * it finds one.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class PhysicsSyncEventStation extends RunnableEtStation {
           
    private static final String TRIGGER_CONFIG = "TriggerConfig";    
    TriggerConfigEvioReader configReader = new TriggerConfigEvioReader();
            
    public PhysicsSyncEventStation(EtSystem system, String name, int order) {   
        config = new RunnableEtStationConfiguration();
        config.order = order;
        config.name = name;
        config.prescale = 1;
        config.readEvents = 100;
        config.system = system;
        config.validate();
        
        filter = new SyncTagFilter();
    }
        
    public void processEvent(EtEvent event) {
        int eventTag = event.getControl()[0];
        System.out.println(this.config.name + " accepted event tag: " + eventTag);
        EvioEvent evioEvent = null;
        try {
            System.out.println("parsing EVIO event ...");
            evioEvent = new EvioReader(event.getDataBuffer()).parseNextEvent();
        } catch (IOException | EvioException e) {
            throw new RuntimeException(e);
        }
        System.out.println("dumping EVIO event ...");
        System.out.println(evioEvent.toXML());
        try {
            System.out.println("done dumping EVIO event");
            BaseLCSimEvent lcsimEvent = new BaseLCSimEvent(9999, 9999, "dummy", 0, false);
            System.out.println("reading DAQ config ...");
            configReader.getDAQConfig(evioEvent, lcsimEvent);
            System.out.println("DAQ config read okay!");
            if (lcsimEvent.hasCollection(EvioDAQParser.class, TRIGGER_CONFIG)) {
                System.out.println("found config in dummy lcsim event");
                List<EvioDAQParser> configList = lcsimEvent.get(EvioDAQParser.class, TRIGGER_CONFIG);
                if (!configList.isEmpty()) {
                    System.out.println("updating config in ConfigurationManager ...");
                    ConfigurationManager.updateConfiguration(configList.get(0));
                    System.out.println("config updated in manager!");
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load DAQ config from sync event ...");
            e.printStackTrace();
        }
    }       
}