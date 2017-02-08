package org.hps.evio;

import org.hps.record.LCSimEventBuilder;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.conditions.ConditionsEvent;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.conditions.ConditionsManagerImplementation;
import org.lcsim.conditions.ConditionsReader;
import org.lcsim.event.EventHeader;
import org.lcsim.event.base.BaseLCSimEvent;
import org.lcsim.util.loop.DummyConditionsConverter;
import org.lcsim.util.loop.DummyDetector;

/**
 * This is an event builder that does nothing except create empty events.
 * It uses a "dummy" LCSim detector. 
 */
public class DummyEventBuilder implements LCSimEventBuilder {

    String dummyName = "NONE";
    
    public DummyEventBuilder() {
        setDummyDetector();
    }

    private void setDummyDetector() {
        ConditionsManager cond = ConditionsManager.defaultInstance();
        ConditionsReader dummyReader = ConditionsReader.createDummy();
        ((ConditionsManagerImplementation)cond).setConditionsReader(dummyReader, dummyName);
        DummyDetector detector = new DummyDetector(dummyName);
        cond.registerConditionsConverter(new DummyConditionsConverter(detector));
    }
    
    @Override
    public EventHeader makeLCSimEvent(EvioEvent evioEvent) {
        return new BaseLCSimEvent(0, evioEvent.getHeader().getNumber(), dummyName);
    }
    
    @Override
    public void readEvioEvent(EvioEvent evioEvent) {
    }

    @Override
    public void conditionsChanged(ConditionsEvent conditionsEvent) {
    }   
}