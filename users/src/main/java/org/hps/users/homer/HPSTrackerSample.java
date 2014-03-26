/*
 * Description : based on the C++ version from Ryan Herbst
 * Sample Container
 * Sample Data consists of the following: Z[xx:xx] = Zeros, O[xx:xx] = Ones
 *    Sample[0] = O[0], Z[0], Hybrid[1:0], Z[0], ApvChip[2:0], Z[0], Channel[6:0], FpgaAddress[15:0]
 *    Sample[1] = Z[1:0], Sample1[13:0]], Z[1:0], Sample0[13:0]
 *    Sample[2] = Z[1:0], Sample3[13:0]], Z[1:0], Sample2[13:0]
 *    Sample[3] = Z[1:0], Sample5[13:0]], Z[1:0], Sample4[13:0]
 * 
 */
package org.hps.users.homer;

/**
 *
 * @author neal
 */
public class HPSTrackerSample {
    // Local data

    protected int ldata_[] = new int[4];
    // Data pointer
    protected int data_[];

    public void setData(int data[]) {
        data_ = data;
    }
    //! Get hybrid index.
    public int hybrid() {
        return ((data_[0] >> 28) & 0x3);
    }
    //! Get apv index.

    public int apv() {
        return ((data_[0] >> 24) & 0x7);
    }

    //! Get channel index.
    public int channel() {
        return ((data_[0] >> 16) & 0x7F);
    }

    //! Get FpgaAddress value from header.
    public int fpgaAddress() {
        return (data_[0] & 0xFFFF);
    }
    //! Get adc value at index.

    public int value(int index) {
        switch (index) {
            case 0:
                return (data_[1] & 0x3FFF);
            case 1:
                return ((data_[1] >> 16) & 0x3FFF);
            case 2:
                return (data_[2] & 0x3FFF);
            case 3:
                return ((data_[2] >> 16) & 0x3FFF);
            case 4:
                return (data_[3] & 0x3FFF);
            case 5:
                return ((data_[3] >> 16) & 0x3FFF);
            default:
                return (0);
        }
    }
}
