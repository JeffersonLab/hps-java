package org.hps.readout;

import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;

/**
 * <code>ReadoutTimestamp</code> specifies the simulation time at
 * which a given subsystem produced its readout data.
 * 
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class ReadoutTimestamp implements GenericObject {
    public static final String collectionName = "ReadoutTimestamps";
    /** Specifies the ID for the event write time. */
    public static final int SYSTEM_TRIGGERBITS = 0;
    /** Specifies the ID for the SVT data time. */
    public static final int SYSTEM_TRACKER = 1;
    /** Specifies the ID for the calorimeter data time. */
    public static final int SYSTEM_ECAL = 2;
    /** Specifies the ID for the trigger time. */
    public static final int SYSTEM_TRIGGERTIME = 3;
    /** Specifies the ID for the hodoscope data time. */
    public static final int SYSTEM_HODOSCOPE = 4;
    
    /** The system ID for the timestamp. */
    private int system;
    /** The timestamp time. */
    private double time;
    
    /**
     * Instantiates a new timestamp for the specified system and
     * simulation time.
     * @param system - The system ID.
     * @param time - The simulation time.
     */
    public ReadoutTimestamp(int system, double time) {
        this.system = system;
        this.time = time;
    }
    
    /**
     * Attempts to obtain the timestamp value for a given system in
     * the specified event.
     * @param system - The system ID. This can be one of {@link
     * org.hps.readout.ReadoutTimestamp#SYSTEM_TRIGGERBITS
     * SYSTEM_TRIGGERBITS}, {@link
     * org.hps.readout.ReadoutTimestamp#SYSTEM_TRACKER
     * SYSTEM_TRACKER}, {@link
     * org.hps.readout.ReadoutTimestamp#SYSTEM_ECAL SYSTEM_ECAL},
     * {@link org.hps.readout.ReadoutTimestamp#SYSTEM_TRIGGERTIME
     * SYSTEM_TRIGGERTIME}, or {@link
     * org.hps.readout.ReadoutTimestamp#SYSTEM_HODOSCOPE
     * SYSTEM_HODOSCOPE}.
     * @param event - The event.
     * @return Returns the simulation time for the indicated system,
     * if a timestamp exists for it, and <code>0</code> otherwise.
     */
    public static double getTimestamp(int system, EventHeader event) {
        if(event.hasCollection(GenericObject.class, collectionName)) {
            List<GenericObject> timestamps = event.get(GenericObject.class, collectionName);
            for(GenericObject timestamp : timestamps) {
                if(timestamp.getIntVal(0) == system) {
                    return timestamp.getDoubleVal(0);
                }
            }
            return 0;
        } else { return 0; }
    }
    
    @Override
    public int getNInt() {
        return 1;
    }
    
    @Override
    public int getNFloat() {
        return 0;
    }
    
    @Override
    public int getNDouble() {
        return 1;
    }
    
    @Override
    public int getIntVal(int index) {
        if (index == 0) {
            return system;
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }
    
    @Override
    public float getFloatVal(int index) {
        throw new ArrayIndexOutOfBoundsException();
    }
    
    @Override
    public double getDoubleVal(int index) {
        if (index == 0) {
            return time;
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }
    
    @Override
    public boolean isFixedSize() {
        return true;
    }
}