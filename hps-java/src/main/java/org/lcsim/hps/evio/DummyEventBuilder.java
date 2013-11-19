package org.lcsim.hps.evio;

import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.conditions.ConditionsManagerImplementation;
import org.lcsim.conditions.ConditionsReader;
import org.lcsim.event.EventHeader;
import org.lcsim.event.base.BaseLCSimEvent;
import org.lcsim.util.loop.DummyConditionsConverter;
import org.lcsim.util.loop.DummyDetector;

/**
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @version $Id: DummyEventBuilder.java,v 1.3 2012/09/01 00:15:15 meeg Exp $
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
    public void setDetectorName(String detectorName) {}

    @Override
    public void setDebug(boolean debug) {}

	@Override
	public boolean isPhysicsEvent(EvioEvent evioEvent) {
		return true;
	}

    @Override
    public void readEvioEvent(EvioEvent evioEvent) {
    }
}