package org.hps.record.evio;

import org.jlab.coda.jevio.BaseStructure;

/**
 * Convenience enum for representing bank tag values in HPS-formatted EVIO events.
 * <p>
 * Documentation of the bank tags and contents can be found at<br/>
 * <a href="https://confluence.slac.stanford.edu/display/hpsg/EVIO+Data+Format">EVIO Data Format</a>.
 *
 * @author Jeremy McCormick, SLAC
 */
public enum EvioBankTag {

    /** EPICS header bank. */
    EPICS_HEADER(57618),
    /** EPICS mother bank. */
    EPICS_MOTHER(129),
    /** EPICS string data bank. */
    EPICS_STRING(57620),
    /** Event ID bank containing the EVIO event number in the run. */
    EVENT_ID(0xC000),
    /** Head bank with run number, etc. */
    HEAD(0xe10F),
    /** Scaler data bank. */
    SCALERS(57621),
    /** Trigger configuration bank. */
    TRIGGER_CONFIG(0xE10E);

    /**
     * The bank's tag value.
     */
    private int bankTag;

    /**
     * Create a new bank enum.
     *
     * @param bankTag the bank value (tag value in the bank's header)
     */
    private EvioBankTag(final int bankTag) {
        this.bankTag = bankTag;
    }

    /**
     * Find a bank matching the tag value by looking through a bank and all its child banks.
     *
     * @param startBank the starting bank
     * @return the first bank matching the tag or <code>null<code> if not found
     */
    public BaseStructure findBank(final BaseStructure startBank) {
        BaseStructure foundBank = null;
        System.out.println("findBank: " + startBank.getHeader().getTag());
        if (this.isBankTag(startBank)) {
            foundBank = startBank;
            System.out.println("found bank: " + foundBank.getHeader().getTag());
        } else if (startBank.getChildrenList() != null) {
            System.out.println("looking in children ...");
            for (final BaseStructure subBank : startBank.getChildrenList()) {
                System.out.println("looking in bank: " + subBank.getHeader().getTag());
                foundBank = this.findBank(subBank);
                if (foundBank != null) {
                    break;
                }
            }
        }
        return foundBank;
    }

    /**
     * Get the bank tag value.
     *
     * @return the bank tag value
     */
    public int getBankTag() {
        return bankTag;
    }

    /**
     * Return <code>true</code> if bank's tag matches this one.
     *
     * @param bank the EVIO data bank
     * @return <code>true</code> if bank's tag matches this one
     */
    public boolean isBankTag(final BaseStructure bank) {
        return bank.getHeader().getTag() == bankTag;
    }

    /**
     * Return <code>true</code> if the tag value matches this one.
     *
     * @param bankTag the bank tag value
     * @return <code>true</code> if the bank tag value matches this one
     */
    public boolean isBankTag(final int bankTag) {
        return bankTag == this.getBankTag();
    }
}
