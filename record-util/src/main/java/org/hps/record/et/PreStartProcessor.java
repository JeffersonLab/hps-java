package org.hps.record.et;

import java.io.IOException;

import org.hps.record.evio.EvioDetectorConditionsProcessor;
import org.jlab.coda.et.EtEvent;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;

/**
 * An ET processor that will activate the conditions system from PRESTART events.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
// FIXME: This class is currently unused in HPS Java.
public class PreStartProcessor extends EtEventProcessor {

    /**
     * The EVIO processor that will activate the conditions system.
     */
    private final EvioDetectorConditionsProcessor conditionsProcessor;

    /**
     * Class constructor.
     *
     * @param detectorName the name of the detector model
     */
    public PreStartProcessor(final String detectorName) {
        this.conditionsProcessor = new EvioDetectorConditionsProcessor(detectorName);
    }

    /**
     * Process an ET event and activate the conditions system if applicable.
     *
     * @param event the <code>EtEvent</code> to process
     */
    @Override
    public void process(final EtEvent event) {
        EvioEvent evioEvent = null;
        try {
            evioEvent = new EvioReader(event.getDataBuffer()).parseNextEvent();
            this.conditionsProcessor.startRun(evioEvent);
        } catch (IOException | EvioException e) {
            throw new RuntimeException(e);
        }
    }
}
