package org.lcsim.geometry.compact.converter;

import org.jdom.Element;

public class HPSTracker2014v1JavaBuilder extends HPSTracker2014JavaBuilder {

    public HPSTracker2014v1JavaBuilder(boolean debugFlag, Element node) {
        super(debugFlag, node);
    }

    @Override
    public HPSTrackerGeometryDefinition createGeometryDefinition(boolean debug, Element node) {
        return new HPSTracker2014v1GeometryDefinition(debug, node);
    }
    
    
}
