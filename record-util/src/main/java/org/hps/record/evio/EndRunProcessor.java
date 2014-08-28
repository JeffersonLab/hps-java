package org.hps.record.evio;

import org.hps.evio.EventConstants;
import org.hps.record.EndRunException;
import org.hps.record.composite.CompositeRecord;
import org.hps.record.composite.CompositeRecordProcessor;

/**
 * This is a CompositeRecordProcessor for ending the run when an EVIO
 * end event is received.  It should be placed last in the chain of
 * processors so that all the other registered processors are executed
 * beforehand because it throws an Exception.
 */
public class EndRunProcessor extends CompositeRecordProcessor {
    
    @Override
    public void processEvent(CompositeRecord event) throws EndRunException {
        if (event.getEvioEvent() != null)
            if (EventConstants.isEndEvent(event.getEvioEvent()))
                throw new EndRunException(
                        "EVIO end run event received.",
                        event.getEvioEvent().getIntData()[1]); // FIXME: Is this right?

    }
}
