package org.hps.readout.ecal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.record.triggerbank.TestRunTriggerData;
import org.lcsim.event.EventHeader;
import org.lcsim.lcio.LCIOWriter;

/**
 * Makes trigger decision and sends trigger to readout drivers. Prints triggers
 * to file if file path specified. Writes trigger events to LCIO if file path
 * specified. To implement: extend this class and write your own
 * triggerDecision().
 */
public abstract class TriggerDriver extends TriggerableDriver {

    private final boolean _DEBUG = false;
    protected String outputFileName = null;
    protected PrintWriter outputStream = null;
    protected int numTriggers;
    private static int lastTrigger = Integer.MIN_VALUE;
    private int deadTime = 0;
    private int prescale = 1;
    private int prescaleCounter = 0;
    private static boolean triggerBit = false;
    private String lcioFile = null;
    LCIOWriter lcioWriter = null;
    private static final List<TriggerableDriver> triggerables = new ArrayList<TriggerableDriver>();

    public TriggerDriver() {
        triggerDelay = 50.0;
    }

    public void setLcioFile(String lcioFile) {
        this.lcioFile = lcioFile;
    }

    /**
     * Set dead time; 0 for no dead time
     *
     * @param deadTime Minimum number of clock ticks between triggers
     */
    public void setDeadTime(int deadTime) {
        this.deadTime = deadTime;
    }

    /**
     * Set prescale. Only trigger on every Nth event that passes the trigger
     * decision. The default is 1 (no prescale - trigger on every event).
     *
     * @param prescale
     */
    public void setPrescale(int prescale) {
        this.prescale = prescale;
    }

    public void setOutputFileName(String outputFileName) {
        this.outputFileName = outputFileName;
    }

    @Override
    public void startOfData() {
//        addTriggerable(this);

        if (outputFileName != null) {
            try {
                outputStream = new PrintWriter(new PrintStream(outputFileName), true);
            } catch (FileNotFoundException ex) {
                throw new RuntimeException("Invalid outputFilePath!");
            }
        } else {
            if (_DEBUG) {
                outputStream = new PrintWriter(System.out, true);
            }
        }

        if (lcioFile != null) {
            try {
                lcioWriter = new LCIOWriter(new File(lcioFile));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        numTriggers = 0;
    }

    @Override
    public void process(EventHeader event) {
//        triggerBit = false; //reset trigger
        //System.out.println(this.getClass().getCanonicalName() + " - process");
        if ((lastTrigger == Integer.MIN_VALUE || ClockSingleton.getClock() - lastTrigger > deadTime) && triggerDecision(event)) {
            prescaleCounter++;
            if (prescaleCounter == prescale) {
                prescaleCounter = 0;
                sendTrigger();
                this.addTrigger();
                for (TriggerableDriver triggerable : triggerables) {
                    ReadoutTimestamp.addTimestamp(triggerable, event);
                }
                ReadoutTimestamp.addTimestamp(this, event);
                triggerBit = true;
                lastTrigger = ClockSingleton.getClock();
                numTriggers++;
                if (_DEBUG) {
                    System.out.printf(this.getClass().getSimpleName() + ": Trigger on event %d\n", event.getEventNumber());
                }
                if (outputStream != null) {
                    outputStream.printf("Trigger on event %d\n", event.getEventNumber());
                }

                // If an ECal trigger signal has been sent store the trigger
                // time offset by the trigger latencies
                if (_DEBUG) {
                    System.out.println(this.getClass().getSimpleName() + ": Trigger added on event " + event.getEventNumber());
                }

                if (outputStream != null) {
                    outputStream.printf("trigger sent to ET event builder on event %d\n", event.getEventNumber());
                }
                makeTriggerData(event, "TriggerStatus");
                if (lcioWriter != null) {
                    try {
                        lcioWriter.write(event);
                    } catch (IOException ex) {
                        Logger.getLogger(TriggerDriver.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }

        // Check if there are any pending trigger bank triggers to process
        checkTrigger(event);
    }

    protected static boolean sendTrigger() {
        for (TriggerableDriver triggerable : triggerables) {
            if (!triggerable.isLive()) {
                return false;
            }
        }
        for (TriggerableDriver triggerable : triggerables) {
            triggerable.addTrigger();
        }
        return true;
    }

    public static void addTriggerable(TriggerableDriver triggerable) {
        triggerables.add(triggerable);
    }

    @Override
    protected void processTrigger(EventHeader event) {
        if (outputStream != null) {
            outputStream.printf("Trigger bank trigger sent on event %d\n", event.getEventNumber());
        }
        makeTriggerData(event, "TriggerBank");
    }

    protected abstract boolean triggerDecision(EventHeader event);

    /**
     * Make a dummy TriggerData
     */
    protected void makeTriggerData(EventHeader event, String collectionName) {
        TestRunTriggerData tData = new TestRunTriggerData(new int[8]);
        List<TestRunTriggerData> triggerList = new ArrayList<TestRunTriggerData>();
        triggerList.add(tData);
        event.put(collectionName, triggerList, TestRunTriggerData.class, 0);
    }

    @Override
    public void endOfData() {
        if (outputStream != null) {
            outputStream.printf("Trigger count: %d\n", numTriggers);
            outputStream.close();
        }
        if (lcioWriter != null) {
            try {
                lcioWriter.close();
            } catch (IOException ex) {
                Logger.getLogger(TriggerDriver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        System.out.printf(this.getClass().getSimpleName() + ": Trigger count: %d\n", numTriggers);
    }

    @Deprecated
    public static boolean triggerBit() {
        return triggerBit;
    }

    public static void resetTrigger() {
        triggerBit = false;
    }

    @Override
    public int getTimestampType() {
        return ReadoutTimestamp.SYSTEM_TRIGGERBITS;
    }
}
