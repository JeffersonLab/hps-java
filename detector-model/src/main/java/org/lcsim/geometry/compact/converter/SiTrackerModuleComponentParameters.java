package org.lcsim.geometry.compact.converter;

// TODO: Move to different package.
public class SiTrackerModuleComponentParameters
{
    String materialName;
    double thickness;
    boolean sensitive;
    int componentNumber;
    String vis;

    public SiTrackerModuleComponentParameters(double thickness, String materialName, int componentNumber, boolean sensitive, String vis)
    {
        this.thickness = thickness;
        this.materialName = materialName;
        this.sensitive = sensitive;
        this.componentNumber = componentNumber;
        this.vis = vis;
    }

    public double getThickness()
    {
        return thickness;
    }

    public String getMaterialName()
    {
        return materialName;
    }

    public boolean isSensitive()
    {
        return sensitive;
    }

    public int getComponentNumber()
    {
        return componentNumber;
    }
    
    public String getVis()
    {
    	return vis;
    }
}
