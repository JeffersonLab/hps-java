package org.lcsim.hps.evio;

import org.lcsim.event.GenericObject;

/**
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: TriggerData.java,v 1.3 2012/08/03 23:14:39 meeg Exp $
 */
public class TriggerData implements GenericObject {

    public static final int OR_TRIG = 3;
    public static final int TOP_TRIG = 4;
    public static final int BOT_TRIG = 5;
    public static final int AND_TRIG = 6;
    public static final int TIME = 7;
    public static final int TRIG_BANK_SIZE = 8;
    public static final String TRIG_COLLECTION = "TriggerBank";
    private int[] bank;

    public TriggerData(int[] bank) {
        this.bank = bank;
    }

    public int getTime() {
        return getIntVal(TIME);
    }

    public int getOrTrig() {
        return getIntVal(OR_TRIG);
    }

    public int getTopTrig() {
        return getIntVal(TOP_TRIG);
    }

    public int getBotTrig() {
        return getIntVal(BOT_TRIG);
    }

    public int getAndTrig() {
        return getIntVal(AND_TRIG);
    }

    public int[] getBank() {
        return bank;
    }

    public static int getTime(GenericObject object) {
        return object.getIntVal(TIME);
    }

    public static int getOrTrig(GenericObject object) {
        return object.getIntVal(OR_TRIG);
    }

    public static int getTopTrig(GenericObject object) {
        return object.getIntVal(TOP_TRIG);
    }

    public static int getBotTrig(GenericObject object) {
        return object.getIntVal(BOT_TRIG);
    }

    public static int getAndTrig(GenericObject object) {
        return object.getIntVal(AND_TRIG);
    }

    public static int[] getBank(GenericObject object) {
        int[] bank = new int[8];
        for (int i = 0; i < 8; i++) {
            bank[i] = object.getIntVal(i);
        }
        return bank;
    }

    @Override
    public int getNInt() {
        return TRIG_BANK_SIZE;
    }

    @Override
    public int getNFloat() {
        return 0;
    }

    @Override
    public int getNDouble() {
        return 0;
    }

    @Override
    public int getIntVal(int index) {
        return bank[index];
    }

    @Override
    public float getFloatVal(int index) {
        throw new UnsupportedOperationException("No float values in " + this.getClass().getSimpleName());
    }

    @Override
    public double getDoubleVal(int index) {
        throw new UnsupportedOperationException("No double values in " + this.getClass().getSimpleName());
    }

    @Override
    public boolean isFixedSize() {
        return true;
    }
}
