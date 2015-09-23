package org.hps.record.triggerbank;

import java.util.Date;
import org.lcsim.event.GenericObject;

/**
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 */
public class HeadBankData extends AbstractIntData {

    public static final int BANK_TAG = 0xe10f;
    public static final int BANK_SIZE = 5;

    public static final int VERSION_NUM = 0;
    public static final int RUN_NUM = 1;
    public static final int EVENT_NUM = 2;
    public static final int UNIX_TIME = 3;
    public static final int EVENT_TYPE = 4;

    public HeadBankData(int[] bank) {
        super(bank);
        decodeData();
    }

    public HeadBankData(GenericObject data) {
        super(data, BANK_TAG);
        decodeData();
    }

    public int getRunNum() {
        return bank[RUN_NUM];
    }

    public int getEventNum() {
        return bank[EVENT_NUM];
    }

    public long getUnixTime() {
        return bank[UNIX_TIME] & 0xffffffffL;
    }

    public Date getDate() {
        return getDate(this);
    }

    public static int getRunNum(GenericObject object) {
        return AbstractIntData.getBankInt(object, RUN_NUM);
    }

    public static int getEventNum(GenericObject object) {
        return AbstractIntData.getBankInt(object, EVENT_NUM);
    }

    public static long getUnixTime(GenericObject object) {
        return AbstractIntData.getBankInt(object, UNIX_TIME) & 0xffffffffL;
    }

    public static Date getDate(GenericObject object) {
        long unixTime = getUnixTime(object);
        if (unixTime == 0) {
            return null;
        } else {
            return new Date(unixTime * 1000);
        }
    }

    @Override
    public int getTag() {
        return BANK_TAG;
    }

    @Override
    protected final void decodeData() { //doesn't actually do anything since there is no decoding done on the ints
        if (this.bank.length != BANK_SIZE) {
            throw new RuntimeException("Invalid Data Length:  " + bank.length);
        }
    }
}
