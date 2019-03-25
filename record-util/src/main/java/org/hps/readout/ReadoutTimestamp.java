package org.hps.readout;

import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;

/**
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: ReadoutTimestamp.java,v 1.1 2013/03/20 00:09:42 meeg Exp $
 */
public class ReadoutTimestamp implements GenericObject {

    public static final String collectionName = "ReadoutTimestamps";
    public static final int SYSTEM_TRIGGERBITS = 0;
    public static final int SYSTEM_TRACKER = 1;
    public static final int SYSTEM_ECAL = 2;
    public static final int SYSTEM_TRIGGERTIME = 3;
    private int system;
    private double time;

    public ReadoutTimestamp(int system, double time) {
        this.system = system;
        this.time = time;
    }

    public static double getTimestamp(int system, EventHeader event) {
        if (event.hasCollection(GenericObject.class, collectionName)) {
            List<GenericObject> timestamps = event.get(GenericObject.class, collectionName);
            for (GenericObject timestamp : timestamps) {
                if (timestamp.getIntVal(0) == system) {
                    return timestamp.getDoubleVal(0);
                }
            }
            return 0;
        } else {
            return 0;
        }
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
