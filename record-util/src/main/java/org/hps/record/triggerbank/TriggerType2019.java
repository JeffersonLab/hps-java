package org.hps.record.triggerbank;

/**
 * @author Norman A Graf
 */
/* VTP_HPS_PRESCALE               0        0   # Single 0 Top    ( 150-8191) MeV (-31,31)   Low energy cluster                  */
 /* VTP_HPS_PRESCALE               1        0   # Single 1 Top    ( 300-3000) MeV (  5,31)   e+                                  */
 /* VTP_HPS_PRESCALE               2        0   # Single 2 Top    ( 300-3000) MeV (  5,31)   e+ : Position dependent energy cut  */
 /* VTP_HPS_PRESCALE               3        0   # Single 3 Top    ( 300-3000) MeV (  5,31)   e+ : HODO L1*L2  Match with cluster */
 /* VTP_HPS_PRESCALE               4        0   # Single 0 Bot    ( 150-8191) MeV (-31,31)   Low energy cluster                  */
 /* VTP_HPS_PRESCALE               5        0   # Single 1 Bot    ( 300-3000) MeV (  5,31)   e+                                  */
 /* VTP_HPS_PRESCALE               6        0   # Single 2 Bot    ( 300-3000) MeV (  5,31)   e+ : Position dependent energy cut  */
 /* VTP_HPS_PRESCALE               7        0   # Single 3 Bot    ( 300-3000) MeV (  5,31)   e+ : HODO L1*L2  Match with cluster */
 /* VTP_HPS_PRESCALE               8        0   # Pair 0          A'                                                             */
 /* VTP_HPS_PRESCALE               9        0   # Pair 1          Moller                                                         */
 /* VTP_HPS_PRESCALE               10       0   # Pair 2          pi0                                                            */
 /* VTP_HPS_PRESCALE               11       0   # Pair 3          -                                                              */
 /* VTP_HPS_PRESCALE               12       0   # LED                                                                            */
 /* VTP_HPS_PRESCALE               13       0   # Cosmic                                                                         */
 /* VTP_HPS_PRESCALE               14       0   # Hodoscope                                                                      */
 /* VTP_HPS_PRESCALE               15       0   # Pulser                                                                         */
 /* VTP_HPS_PRESCALE               16       0   # Multiplicity-0 2 Cluster Trigger                                               */
 /* VTP_HPS_PRESCALE               17       0   # Multiplicity-1 3 Cluster trigger                                               */
 /* VTP_HPS_PRESCALE               18       0   # FEE Top       ( 2600-5200)                                                     */
 /* VTP_HPS_PRESCALE               19       0   # FEE Bot       ( 2600-5200)                                                     */
public enum TriggerType2019 {

    SINGLE0TOP(0), SINGLE1TOP(1), SINGLE2TOP(2), SINGLE3TOP(3),
    SINGLE0BOT(4), SINGLE1BOT(5), SINGLE2BOT(6), SINGLE3BOT(7),
    PAIR0(8), PAIR1(9), PAIR2(10), PAIR3(11),
    LED(12), COSMIC(13), HODOSCOPE(14), PULSER(15), MULT0(16), MULT1(17), FEETOP(18), FEEBOT(19);

    private int bit;

    /**
     * Constructor with bit number which is used to shift the input TI bits.
     *
     * @param bit the bit number for shifting the TI bits
     */
    private TriggerType2019(final int value) {
        bit = value;
    }

    /**
     * Get the bit number.
     *
     * @return the bit number
     */
    public int getBit() {
        return this.bit;
    }

    @Override
    public String toString() {
        switch (this) {
            case SINGLE0TOP:
                System.out.println("Single 0 Top Trigger");
                break;
            case SINGLE1TOP:
                System.out.println("Single 1 Top Trigger");
                break;
            case SINGLE2TOP:
                System.out.println("Single 2 Top Trigger");
                break;
            case SINGLE3TOP:
                System.out.println("Single 3 Top Trigger");
                break;
            case SINGLE0BOT:
                System.out.println("Single 0 Bottom Trigger");
                break;
            case SINGLE1BOT:
                System.out.println("Single 1 Bottom Trigger");
                break;
            case SINGLE2BOT:
                System.out.println("Single 2 Bottom Trigger");
                break;
            case SINGLE3BOT:
                System.out.println("Single 3 Bottom Trigger");
                break;
            case PAIR0:
                System.out.println("Pair 0 Trigger");
                break;
            case PAIR1:
                System.out.println("Pair 1 Trigger");
                break;
            case PAIR2:
                System.out.println("Pair 2 Top Trigger");
                break;
            case PAIR3:
                System.out.println("Pair 3 Top Trigger");
                break;
            case LED:
                System.out.println("LED Trigger");
                break;
            case COSMIC:
                System.out.println("Cosmic Trigger");
                break;
            case HODOSCOPE:
                System.out.println("Hodoscope Trigger");
                break;
            case PULSER:
                System.out.println("Pulser Trigger");
                break;
            case MULT0:
                System.out.println("Multiplicity 0 Trigger");
                break;
            case MULT1:
                System.out.println("Multiplicity 1 Trigger");
                break;
            case FEETOP:
                System.out.println("FEE Top Trigger");
                break;
            case FEEBOT:
                System.out.println("FEE Bottom Trigger");
                break;
        }
        return super.toString();
    }
}
