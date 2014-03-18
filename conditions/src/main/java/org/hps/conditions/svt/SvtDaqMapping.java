package org.hps.conditions.svt;

import org.hps.conditions.AbstractConditionsObject;


public class SvtDaqMapping extends AbstractConditionsObject {

    public int getHalf() {
        return getFieldValue("half");
    }
    
    public int getLayerNumber() {
        return getFieldValue("layer");
    }
    
    public int getFpgaNumber() {
        return getFieldValue("fpga");
    }
    
    public int getHybridNumber() {
        return getFieldValue("hybrid");
    }    
}
