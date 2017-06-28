package org.lcsim.geometry.subdetector;

import org.jdom.Element;
import org.jdom.JDOMException;

public class Hodoscope_v1 extends AbstractSubdetector {

    Hodoscope_v1(Element node) throws JDOMException {
        super(node);
    }

    public boolean isCalorimeter() {
        return true;
    }
}
