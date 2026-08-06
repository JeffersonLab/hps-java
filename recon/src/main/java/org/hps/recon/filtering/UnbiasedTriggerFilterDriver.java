package org.hps.recon.filtering;

import java.util.logging.Logger;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.util.Driver;
import org.hps.record.epics.EpicsData;
import org.hps.record.scalers.ScalerData;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TSData2019;

/**
 * Keep pulser triggered events. Also keep EPICS events, and Scaler events. Drop all other events.
 *
 * Set debug="true" in the steering file to enable per-event logging.
 * A summary of accepted/rejected counts is always printed at end-of-data.
 */
public class UnbiasedTriggerFilterDriver extends Driver {

    private static final Logger LOGGER = Logger.getLogger(UnbiasedTriggerFilterDriver.class.getName());

    // Configurable via steering file: <driver><name>UnbiasedTriggerFilterDriver</name><debug>true</debug></driver>
    private boolean debug = false;

    // Counters
    private long nTotal        = 0;
    private long nEpics        = 0;
    private long nScaler       = 0;
    private long nNoTSBank     = 0;
    private long nPulser       = 0;
    private long nFaradayCup   = 0;
    private long nDropped      = 0;

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    @Override
    public void startOfData() {
        System.out.println("[UnbiasedTriggerFilterDriver] Driver started. debug=" + debug);
    }

    public void process(EventHeader event) {

        nTotal++;
        int evNum = event.getEventNumber();

        // 1. keep all events with EPICS data (could also use event tag = 31):
        if (EpicsData.read(event) != null) {
            nEpics++;
            if (debug) System.out.println("[UnbiasedTriggerFilterDriver] Event " + evNum + ": ACCEPTED (EPICS data)");
            return;
        }

        // 2. keep all events with Scaler data:
        if (ScalerData.read(event) != null) {
            nScaler++;
            if (debug) System.out.println("[UnbiasedTriggerFilterDriver] Event " + evNum + ": ACCEPTED (Scaler data)");
            return;
        }

        // 3. drop event if it doesn't have a TriggerBank
        if (!event.hasCollection(GenericObject.class, "TSBank")) {
            nNoTSBank++;
            nDropped++;
            if (debug) System.out.println("[UnbiasedTriggerFilterDriver] Event " + evNum + ": REJECTED (no TSBank)");
            throw new Driver.NextEventException();
        }

        // 4. keep event if it was from a Pulser or FaradayCup trigger:
        for (GenericObject gob : event.get(GenericObject.class, "TSBank")) {
            if (!(AbstractIntData.getTag(gob) == TSData2019.BANK_TAG))
                continue;
            TSData2019 tsd = new TSData2019(gob);
            if (tsd.isPulserTrigger()) {
                nPulser++;
                if (debug) System.out.println("[UnbiasedTriggerFilterDriver] Event " + evNum + ": ACCEPTED (Pulser trigger)");
                return;
            }
            if (tsd.isFaradayCupTrigger()) {
                nFaradayCup++;
                if (debug) System.out.println("[UnbiasedTriggerFilterDriver] Event " + evNum + ": ACCEPTED (FaradayCup trigger)");
                return;
            }
        }

        // 5. Else, drop event:
        nDropped++;
        if (debug) System.out.println("[UnbiasedTriggerFilterDriver] Event " + evNum + ": REJECTED (no Pulser/FaradayCup trigger in TSBank)");
        throw new Driver.NextEventException();
    }

    @Override
    public void endOfData() {
        long nAccepted = nTotal - nDropped;
        double efficiency = (nTotal > 0) ? 100.0 * nAccepted / nTotal : 0.0;
        System.out.println("========== UnbiasedTriggerFilterDriver Summary ==========");
        System.out.println("  Total events processed : " + nTotal);
        System.out.println(String.format("  Accepted               : %d  (%.2f%%)", nAccepted, efficiency));
        System.out.println("    -> EPICS             : " + nEpics);
        System.out.println("    -> Scaler            : " + nScaler);
        System.out.println("    -> Pulser trigger    : " + nPulser);
        System.out.println("    -> FaradayCup trigger: " + nFaradayCup);
        System.out.println(String.format("  Rejected               : %d  (%.2f%%)", nDropped, 100.0 - efficiency));
        System.out.println("    -> No TSBank         : " + nNoTSBank);
        System.out.println("    -> Other triggers    : " + (nDropped - nNoTSBank));
        System.out.println("=========================================================");
    }
}
