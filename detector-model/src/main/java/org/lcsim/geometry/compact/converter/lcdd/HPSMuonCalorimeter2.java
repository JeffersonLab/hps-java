package org.lcsim.geometry.compact.converter.lcdd;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.lcsim.geometry.compact.converter.lcdd.util.LCDD;
import org.lcsim.geometry.compact.converter.lcdd.util.SensitiveDetector;
import org.lcsim.geometry.compact.converter.lcdd.util.Volume;

public class HPSMuonCalorimeter2 extends LCDDSubdetector
{
	HPSMuonCalorimeter2(Element e) throws JDOMException 
	{
		super(e);
	}

	void addToLCDD(LCDD lcdd, SensitiveDetector sens) throws JDOMException 
	{
        String name = node.getAttributeValue("name");
        System.out.println("HPSMuonCalorimeter2.addToLCDD - " + name);
        int id = node.getAttribute("id").getIntValue();
        Volume mother = lcdd.pickMotherVolume(this);
        
        Element parameters = node.getChild("parameters");
        if (parameters == null) {
        	throw new RuntimeException("parameters element missing");
        }
                
        double frontFaceToTarget = parameters.getAttribute("front_face_to_target").getDoubleValue();
        double deadZoneAngle = parameters.getAttribute("dead_zone_angle").getDoubleValue();
        double stripThickness = parameters.getAttribute("strip_thickness").getDoubleValue();
        double stripSpacingZ = parameters.getAttribute("strip_spacing_z").getDoubleValue();
        double stripSpacingY = parameters.getAttribute("strip_spacing_y").getDoubleValue();
        double stripSpacingX = parameters.getAttribute("strip_spacing_x").getDoubleValue();
        
        System.out.println("frontFaceToTarget = " + frontFaceToTarget);
        System.out.println("deadZoneAngle = " + deadZoneAngle);
        System.out.println("stripThickness = " + stripThickness);
        System.out.println("stripSpacingX = " + stripSpacingX);
        System.out.println("stripSpacingY = " + stripSpacingY);        
        System.out.println("stripSpacingZ = " + stripSpacingZ);
        
        for (Object layerObject : node.getChildren("layer")) {
        	Element layerElement = (Element)layerObject;
        	int layerId = layerElement.getAttribute("id").getIntValue();
        	System.out.println("layer = " + layerId);
        	for (Object sliceObject : layerElement.getChildren("slice")) {
        		Element sliceElement = (Element)sliceObject;
        		if (sliceElement.getAttribute("thickness") != null) {
        			double thickness = sliceElement.getAttribute("thickness").getDoubleValue();
        			System.out.println("slice thickness = " + thickness);
        		}
        	}
        }
	}
}
