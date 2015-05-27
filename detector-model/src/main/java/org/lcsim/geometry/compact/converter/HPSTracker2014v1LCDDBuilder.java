package org.lcsim.geometry.compact.converter;

import org.jdom.Element;
import org.lcsim.geometry.compact.converter.lcdd.util.LCDD;
import org.lcsim.geometry.compact.converter.lcdd.util.SensitiveDetector;

public class HPSTracker2014v1LCDDBuilder extends HPSTracker2014LCDDBuilder {

    public HPSTracker2014v1LCDDBuilder(boolean debugFlag, Element node,
            LCDD lcdd, SensitiveDetector sens) {
        super(debugFlag, node, lcdd, sens);
    }

    public void setBuilder() {
        setBuilder(createGeometryDefinition(_debug, node));
    }
    
    @Override
    public HPSTrackerGeometryDefinition createGeometryDefinition(boolean debug,
        Element node) {
        return new HPSTracker2014v1GeometryDefinition(_debug, node);
    }
}
