package org.hps.record.evio;

import org.hps.record.AbstractRecordProcessor;
import org.jlab.coda.jevio.EvioEvent;

/**
 * This is the basic abstract class that processors of 
 * <tt>EvioEvent</tt> objects should extend.
 */
public abstract class EvioEventProcessor extends AbstractRecordProcessor<EvioEvent> {    
}