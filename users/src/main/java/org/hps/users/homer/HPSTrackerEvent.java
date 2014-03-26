/*
 * Description : based on the C++ version from Ryan Herbst
 * 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 *  Description :
 * Event Container
 * Event Data consists of the following: Z[xx:xx] = Zeros
 *    Frame Size = 1 x 32-bits (32-bit dwords)
 *    Header = 8 x 32-bits
 *    Header[0] = T[0], Z[14:0], FpgaAddress[15:0] - T = 1 For TI FPGA
 *    Header[1] = Sequence[31:0]
//
 * The rest of the event header depends on the T flag, For T = 0:
//
 *    Header[2] = TempB[15:0], TempA[15:0]
 *    Header[3] = TempD[15:0], TempC[15:0]
 *    Header[4] = TempF[15:0], TempE[15:0]
 *    Header[5] = TempH[15:0], TempG[15:0]
 *    Header[6] = TempJ[15:0], TempI[15:0]
 *    Header[7] = TempL[15:0], TempK[15:0]
 *       Samples... (See HPSTrackerSample.java)
 *   Tail = 1 x 32-bits
 *       Should be zero
 */
package org.hps.users.homer;

/**
 *
 * @author neal
 */
public class HPSTrackerEvent extends HPSTrackerSample {

    double temp;
    double tk;
    double res;
    double volt;
    int idx;
    
    // Temperature Constants
    double coeffA_ = -1.4141963e1;
    double coeffB_ = 4.4307830e3;
    double coeffC_ = -3.4078983e4;
    double coeffD_ = -8.8941929e6;
    double t25_ = 10000.0;
    double k0_ = 273.15;
    double vmax_ = 2.5;
    double vref_ = 2.5;
    double rdiv_ = 10000;
    double minTemp_ = -50;
    double maxTemp_ = 150;
    double incTemp_ = 0.01;
    int adcCnt_ = 4096;
    
    // Temperature lookup table
    double tempTable_[] = new double[adcCnt_];
    
    // Local trigger data
    int tiData_[] = new int[7]; // What size can this grow to????
    
    // Frame Constants
    int headSize_ = 8;
    int tailSize_ = 1;
    int sampleSize_ = 4;
    
    // frame size must be set externally
    int size_ = 0;

    public void TrackerEvent() {

        // Fill temperature lookup table
        temp = minTemp_;
        while (temp < maxTemp_) {
            tk = k0_ + temp;
            res = t25_ * Math.exp(coeffA_ + (coeffB_ / tk) + (coeffC_ / (tk * tk)) + (coeffD_ / (tk * tk * tk)));
            volt = (res * vmax_) / (rdiv_ + res);
            idx = (int) ((volt / vref_) * (double) (adcCnt_ - 1));
            if (idx < adcCnt_) {
                tempTable_[idx] = temp;
            }
            temp += incTemp_;
        }
    }

    // Get TI flag from header
    public boolean isTiFrame() {
        return ((data_[0] & 0x80000000) != 0);
    }

// Get FpgaAddress value from header.
    public int fpgaAddress() {
        return (data_[0] & 0xFFFF);
    }

// Get sequence count from header.
    public int sequence() {
        return (data_[1]);
    }

// Get trigger block from header.
    int[] tiData() {
        for (int iti = 0; iti < tiData_.length; iti++) {
            tiData_[iti] = data_[2 + iti];
        }
        return (tiData_);
    }

    // Set address of data buffer
    public void setData(int indat[]) {
        data_ = indat;
    }

// Set frame size
    public void setSize(int sz) {
        size_ = sz;
    }

    // Get sample size value from header.
    public int sampleSize() {
        return ((data_[0] >> 8) & 0xF);
    }

// Get temperature values from header.
    public double temperature(int index) {
        if (isTiFrame()) {
            return (0.0);
        } else {
            switch (index) {
                case 0:
                    return (tempTable_[(data_[2] & 0x3FFF)]);
                case 1:
                    return (tempTable_[((data_[2] >> 16) & 0x3FFF)]);
                case 2:
                    return (tempTable_[(data_[3] & 0x3FFF)]);
                case 3:
                    return (tempTable_[((data_[3] >> 16) & 0x3FFF)]);
                case 4:
                    return (tempTable_[(data_[4] & 0x3FFF)]);
                case 5:
                    return (tempTable_[((data_[4] >> 16) & 0x3FFF)]);
                case 6:
                    return (tempTable_[(data_[5] & 0x3FFF)]);
                case 7:
                    return (tempTable_[((data_[5] >> 16) & 0x3FFF)]);
                case 8:
                    return (tempTable_[(data_[6] & 0x3FFF)]);
                case 9:
                    return (tempTable_[((data_[6] >> 16) & 0x3FFF)]);
                case 10:
                    return (tempTable_[(data_[7] & 0x3FFF)]);
                case 11:
                    return (tempTable_[((data_[7] >> 16) & 0x3FFF)]);
                default:
                    return (0.0);
            }
        }

    }

// Get sample count
    public int count() {
//        return (128);
//        int size_ = sampleSize();
//        System.out.println("count: size_ = " + size_);
        return ((size_ - (headSize_ + tailSize_)) / sampleSize_);
    }
// Get sample at index

    public HPSTrackerSample sample(int index) {
        if (index >= count()) { // should be count()
            return (null);
        } else {
            HPSTrackerSample sample_ = new HPSTrackerSample();
            for (int ii = 0; ii < sampleSize_; ii++) {
                ldata_[ii] = data_[headSize_ + (index * sampleSize_) + ii];
//                System.out.println("ldata[" + ii + "] = " + ldata_[ii]);
            }
//            sample_.setData(data_[headSize_ + (index * sampleSize_)]);
            sample_.setData(ldata_);
            return (sample_);
        }
    }
}
