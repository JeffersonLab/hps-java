package org.lcsim.geometry.subdetector;

import org.jdom.Element;
import org.jdom.JDOMException;

public class Hodoscope_v1 extends AbstractCalorimeter {

    Hodoscope_v1(Element node) throws JDOMException {
        super(node);
    }

    /**
     * FIXME: These methods should really have dummy implementations in AbstractCalorimeter.
     *        Update the lcsim AbstractCalorimeter type to fix this.
     */

    public int getNumberOfSides() {
        return 0;
    }

    public double getSectionPhi() {
        return 0.;
    }

    public double getZLength() {
        return 0.;
    }

    public double getOuterZ() {
        return 0.;
    }

    public double getInnerZ() {
        return 0.;
    }

    public double getOuterRadius() {
        return 0.;
    }

    public double getInnerRadius() {
        return 0.;
    }
}
