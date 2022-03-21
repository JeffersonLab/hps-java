package org.hps.monitoring.application.util;

import java.io.IOException;
import java.util.List;

import org.hps.evio.TriggerConfigEvioReader;
import org.hps.record.daqconfig.ConfigurationManager;
import org.hps.record.daqconfig.EvioDAQParser;
import org.hps.record.et.EtEventProcessor;
import org.hps.record.evio.EvioEventUtilities;
import org.jlab.coda.et.EtEvent;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;
import org.lcsim.event.base.BaseLCSimEvent;

/**
 * This is an ET event processor that will load DAQ configuration into the global manager from EVIO physics SYNC events,
 * which have an event type in which bits 6 and 7 are set to 1.
 *
 * @see org.hps.record.daqconfig.ConfigurationManager
 * @see org.hps.record.daqconfig.EvioDAQParser
 */
// FIXME: This class is currently unused.
public final class SyncEventProcessor extends EtEventProcessor {

    /**
     * The name of the trigger configuration collection.
     */
    private static final String TRIGGER_CONFIG = "TriggerConfig";

    /**
     * The trigger configuration reader.
     */
    private final TriggerConfigEvioReader configReader = new TriggerConfigEvioReader();

    /**
     * Process an ET event and if there is a trigger configuration present, parse it and update the configuration in the
     * global manager.
     */
    @Override
    public void process(final EtEvent event) {
        EvioEvent evioEvent = null;
        try {
            evioEvent = new EvioReader(event.getDataBuffer()).parseNextEvent();
        } catch (IOException | EvioException e) {
            throw new RuntimeException(e);
        }
        try {
            // Create a dummy LCIO event to satisfy the configuration reader's interface.
            final BaseLCSimEvent dummyLcsimEvent = new BaseLCSimEvent(EvioEventUtilities.getRunNumber(evioEvent),
                    evioEvent.getEventNumber(), "DUMMY", 0, false);

            // Create the DAQ configuration object in the LCIO event.
            this.configReader.getDAQConfig(evioEvent, dummyLcsimEvent);

            // Update the global configuration if a configuration was created.
            if (dummyLcsimEvent.hasCollection(EvioDAQParser.class, TRIGGER_CONFIG)) {
                final List<EvioDAQParser> configList = dummyLcsimEvent.get(EvioDAQParser.class, TRIGGER_CONFIG);
                if (!configList.isEmpty()) {
                    ConfigurationManager.updateConfiguration(configList.get(0));
                }
            }
        } catch (final Exception e) {
            System.err.println("Failed to load DAQ config from sync event ...");
            e.printStackTrace();
        }
    }
}
