package org.lcsim.geometry.compact.converter;

import java.util.ArrayList;

import org.jdom.DataConversionException;
import org.jdom.Element;
import org.jdom.JDOMException;

// TODO: Move to different package.

public class SiTrackerModuleParameters
extends ArrayList<SiTrackerModuleComponentParameters>
{
    double thickness=0.;
    String name;
    double dimensions[] = new double[3];
    String vis;
    
    public SiTrackerModuleParameters(Element element)
    {
        name = element.getAttributeValue("name");
        
        if (element.getAttribute("vis") != null)
        	this.vis = element.getAttribute("vis").getValue();
        
        int cntr=0;
        for (Object o : element.getChildren("module_component"))
        {
            try {

                Element e = (Element)o;

                double thickness = e.getAttribute("thickness").getDoubleValue();

                String materialName = e.getAttributeValue("material");

                boolean sensitive = false;
                if (e.getAttribute("sensitive") != null)
                    sensitive = e.getAttribute("sensitive").getBooleanValue();
                String componentVis = null;
                if (e.getAttribute("vis") != null)
                	componentVis = e.getAttribute("vis").getValue();
                add(new SiTrackerModuleComponentParameters(thickness, materialName, cntr, sensitive, componentVis));
            }
            catch (JDOMException x)
            {
                throw new RuntimeException(x);
            }
            ++cntr;
        }
        
        // Optional dimension parameters (not always present).
        if (element.getChild("trd") != null)
        {
            Element trd = element.getChild("trd");
            try 
            {
                dimensions[0] = trd.getAttribute("x1").getDoubleValue();
                dimensions[1] = trd.getAttribute("x2").getDoubleValue();
                dimensions[2] = trd.getAttribute("z").getDoubleValue();
            }
            catch (DataConversionException x)
            {
                throw new RuntimeException(x);
            }
        }
        
        calculateThickness();
    }

    public void calculateThickness()
    {
        thickness = 0.; // reset thickness
        for (SiTrackerModuleComponentParameters p : this)
        {
            thickness += p.getThickness();
        }
    }

    public double getThickness()
    {
        return thickness;
    }
    
    public String getName()
    {
        return name;
    }
    
    public double getDimension(int i)
    {
        if (i > (dimensions.length - 1) || i < 0)
            throw new RuntimeException("Invalid dimensions index: " + i);
        return dimensions[i];
    }
    
    public String getVis()
    {
    	return vis;
    }
}