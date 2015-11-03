package org.hps.record.daqconfig;

import java.util.ArrayList;
import java.util.List;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.record.evio.EvioBankTag;
import org.hps.record.evio.EvioEventProcessor;
import org.hps.record.evio.EvioEventUtilities;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;

/**
 * Modified from code in {@link org.hps.evio.TriggerConfigEvioReader} to extract trigger
 * config without an output LCSim event.
 * 
 * @author Jeremy McCormick, SLAC
 */
public class DAQConfigEvioProcessor extends EvioEventProcessor {
           
    private List<EvioDAQParser> triggerConfig = new ArrayList<EvioDAQParser>();

    @Override
    public void process(EvioEvent evioEvent) {        
        for (BaseStructure bank : evioEvent.getChildrenList()) {
            if (bank.getChildCount() <= 0)
                continue;
            int crate = bank.getHeader().getTag();
            for (BaseStructure subBank : bank.getChildrenList()) {
                if (EvioBankTag.TRIGGER_CONFIG.equals(subBank)) {
                    if (subBank.getStringData() == null) {                        
                        throw new RuntimeException("Trigger config bank is missing string data.");
                    }
                    createTriggerConfig(evioEvent, crate, subBank);
                }
            }
        }
    }
    
    private void createTriggerConfig(EvioEvent evioEvent, int crate, BaseStructure subBank) {
        
        // Get run number from EVIO event.
        int runNumber = EvioEventUtilities.getRunNumber(evioEvent);
        
        // Initialize the conditions system if necessary as the DAQ config parsing classes use it.
        DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();
        if (!conditionsManager.isInitialized() || conditionsManager.getRun() != runNumber) {
            try {
                conditionsManager.setXmlConfig("/org/hps/conditions/config/conditions_database_no_svt.xml");
                DatabaseConditionsManager.getInstance().setDetector("HPS-dummy-detector", runNumber);
            } catch (ConditionsNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        
        // Create the trigger config from the EVIO data.
        triggerConfig = new ArrayList<EvioDAQParser>();
        triggerConfig.add(new EvioDAQParser());
        triggerConfig.get(0).parse(
                crate, 
                runNumber, 
                subBank.getStringData());
    }
}