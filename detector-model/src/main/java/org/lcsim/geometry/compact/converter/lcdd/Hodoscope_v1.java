package org.lcsim.geometry.compact.converter.lcdd;

import org.jdom.Element;
import org.jdom.JDOMException;

import org.lcsim.geometry.compact.converter.lcdd.util.LCDD;
import org.lcsim.geometry.compact.converter.lcdd.util.SensitiveDetector;

public class Hodoscope_v1 extends LCDDSubdetector {
    
    Hodoscope_v1(Element node) throws JDOMException {
        super(node); 
    }
    
    void addToLCDD(LCDD lcdd, SensitiveDetector sens) throws JDOMException {
        System.out.println("Hopdoscop_v1.addToLCDD");
    }
    
    public boolean isCalorimeter() {
        return true;
    }
}
