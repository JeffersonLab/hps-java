package org.hps.record.et;

import org.hps.record.AbstractRecordProcessor;
import org.jlab.coda.et.EtEvent;

/**
 * This is the basic abstract class that processors of <tt>EtEvent</tt> objects should extend.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public abstract class EtEventProcessor extends AbstractRecordProcessor<EtEvent> {
}