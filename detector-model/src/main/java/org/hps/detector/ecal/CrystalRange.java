package org.hps.detector.ecal;

import org.jdom.Element;

public class CrystalRange {

    int xIndexMax;
    int xIndexMin;
    int yIndexMax;
    int yIndexMin;

    public CrystalRange(final Element elem) throws Exception {
        xIndexMin = xIndexMax = yIndexMin = yIndexMax = 0;

        if (elem.getAttribute("ixmin") != null) {
            xIndexMin = elem.getAttribute("ixmin").getIntValue();
        } else {
            throw new RuntimeException("Missing ixmin parameter.");
        }

        if (elem.getAttribute("ixmax") != null) {
            xIndexMax = elem.getAttribute("ixmax").getIntValue();
        } else {
            throw new RuntimeException("Missing ixmax parameter.");
        }

        if (elem.getAttribute("iymin") != null) {
            yIndexMin = elem.getAttribute("iymin").getIntValue();
        } else {
            throw new RuntimeException("Missing ixmax parameter.");
        }

        if (elem.getAttribute("iymax") != null) {
            yIndexMax = elem.getAttribute("iymax").getIntValue();
        } else {
            throw new RuntimeException("Missing iymax parameter.");
        }
    }
    
    public int getXIndexMax() {
        return xIndexMax;
    }
    
    public int getYIndexMax() {
        return yIndexMax;
    }
    
    public int getXIndexMin() {
        return xIndexMin;
    }
    
    public int getYIndexMin() {
        return yIndexMin;
    }
}