package org.lcsim.geometry.compact.converter.lcdd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jdom.DataConversionException;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.lcsim.geometry.compact.converter.lcdd.util.Box;
import org.lcsim.geometry.compact.converter.lcdd.util.LCDD;
import org.lcsim.geometry.compact.converter.lcdd.util.Material;
import org.lcsim.geometry.compact.converter.lcdd.util.PhysVol;
import org.lcsim.geometry.compact.converter.lcdd.util.Position;
import org.lcsim.geometry.compact.converter.lcdd.util.Rotation;
import org.lcsim.geometry.compact.converter.lcdd.util.SensitiveDetector;
import org.lcsim.geometry.compact.converter.lcdd.util.Volume;

/**
 * An LCDD converter for a Silicon endcap tracker model based on Bill Cooper's design from <a href=
 * "http://ilcagenda.linearcollider.org/materialDisplay.py?contribId=58&sessionId=1&materialId=slides&confId=2784"
 * >Boulder SiD Workshop 2008</a>.
 * 
 * @author jeremym
 */
public class HPSTracker extends LCDDSubdetector
{

    Map<String, ModuleParameters> moduleParameters = new HashMap<String, ModuleParameters>();
    Map<String, Volume> modules = new HashMap<String, Volume>();
    Material vacuum;

    public HPSTracker(Element node) throws JDOMException
    {
        super(node);
    }
    
    public boolean isTracker()
    {
        return true;
    }

    void addToLCDD(LCDD lcdd, SensitiveDetector sd) throws JDOMException
    {
        int sysId = node.getAttribute("id").getIntValue();
        String subdetName = node.getAttributeValue("name");
        vacuum = lcdd.getMaterial("Vacuum");
        boolean reflect;

        if (node.getAttribute("reflect") != null)
        {
            reflect = node.getAttribute("reflect").getBooleanValue();
        }
        else
        {
            reflect = true;
        }
    

        for (Iterator i = node.getChildren("module").iterator(); i.hasNext();)
        {
            Element module = (Element) i.next();
            String moduleName = module.getAttributeValue("name");
            moduleParameters.put(moduleName, new ModuleParameters(module));
            modules.put(moduleName, makeModule(moduleParameters.get(moduleName), sd, lcdd));
        }
        
        // layer
        for (Iterator i = node.getChildren("layer").iterator(); i.hasNext();)
        {
        	// Modules are numbered from 0 starting in each layer.
            int moduleNumber = 0;

            Element layerElement = (Element) i.next();
            int layerId = layerElement.getAttribute("id").getIntValue();
            
            System.out.println("<layer id=\"" + layerId + "\">");
            
            int ringCount = 0;

            // quadrant (???)
            for (Iterator j = layerElement.getChildren("quadrant").iterator(); j.hasNext();)
            {
                Element ringElement = (Element) j.next();
                
                double zLayer = ringElement.getAttribute("z").getDoubleValue();
                double dz = ringElement.getAttribute("dz").getDoubleValue();
                double xStart = ringElement.getAttribute("xStart").getDoubleValue();
                double xStep = ringElement.getAttribute("xStep").getDoubleValue();
                int nx = ringElement.getAttribute("nx").getIntValue();
                double yStart = ringElement.getAttribute("yStart").getDoubleValue();
                int ny = ringElement.getAttribute("ny").getIntValue();
                double yStep = ringElement.getAttribute("yStep").getDoubleValue();
                double top_phi0 = 0;                
                if (ringElement.getAttribute("top_phi0") != null)
                {
                    top_phi0 = ringElement.getAttribute("top_phi0").getDoubleValue();
                }
                 double bot_phi0 = 0;                
                if (ringElement.getAttribute("bot_phi0") != null)
                {
                    bot_phi0 = ringElement.getAttribute("bot_phi0").getDoubleValue();
                }
                 double theta = 0;                
                if (ringElement.getAttribute("theta") != null)
                {
                    theta = ringElement.getAttribute("theta").getDoubleValue();
                }
                
                String module = ringElement.getAttributeValue("module");

                Volume moduleVolume = modules.get(module);
                
                if (moduleVolume == null)
                {
                    throw new RuntimeException("Module " + module + " was not found.");
                }

                double x, y, z;
                z = zLayer;
                x = xStart;

                // nx
                for (int k = 0; k < nx; k++ )
                {
                    y = yStart;
                    // ny
                    for (int kk = 0; kk < ny; kk++ )
                    {
                        String moduleBaseName = subdetName + "_layer" + layerId + "_module" + moduleNumber;

                        Position p = new Position(moduleBaseName + "_position");

                        p.setX(x);
                        p.setY(y);
                        p.setZ(z + dz);
                        //System.out.println("layer, module = " + layerId + ", " + moduleNumber);
                        //System.out.println("module pos = " + x + " " + y + " " + z);
                        Rotation rot = new Rotation(moduleBaseName + "_rotation");
                        rot.setX(0);
                        rot.setY(theta);
                        //rot.setZ(phi0);
                        // Y side along world's X axis.  
                        // FIXME Should phi0 actually be subtracted???
                        rot.setZ(-(Math.PI / 2 + top_phi0));
                        //System.out.println("module rot = " + 0 + ", " + theta + "," + -(Math.PI / 2 + top_phi0));

                        lcdd.add(p);
                        lcdd.add(rot);

                        PhysVol pv = new PhysVol(moduleVolume, lcdd.getTrackingVolume(), p, rot);
                        pv.addPhysVolID("system", sysId);
                        pv.addPhysVolID("barrel", 0);
                        pv.addPhysVolID("layer", layerId);
                        pv.addPhysVolID("module", moduleNumber);
                                                
                        System.out.print("    <module_placement name=\"" + module + "\" ");
                        System.out.print("id=\"" + moduleNumber + "\" ");
                        System.out.print("x=\"" + p.getAttribute("x").getDoubleValue() + "\" ");
                        System.out.print("y=\"" + p.getAttribute("y").getDoubleValue() + "\" ");
                        System.out.print("z=\"" + p.getAttribute("z").getDoubleValue() + "\" ");
                        System.out.print("rx=\"" + rot.getAttribute("x").getDoubleValue() + "\" ");
                        System.out.print("ry=\"" + rot.getAttribute("y").getDoubleValue() + "\" ");
                        System.out.print("rz=\"" + rot.getAttribute("z").getDoubleValue() + "\"/>");
                        System.out.println();
                        
                        ++moduleNumber;     
                        
                        if (reflect)
                        {
                            Position pr = new Position(moduleBaseName + "_reflect_position");
                            pr.setX(x);
                            pr.setY(-y);
                            pr.setZ(z + dz);
                            //System.out.println("module @ " + x + " " + -y + " " + (z + dz));
                            Rotation rotr = new Rotation(moduleBaseName + "_reflect_rotation");                          
                            rotr.setX(0);
                            rotr.setY(theta);
                            rotr.setZ(-Math.PI/2 - bot_phi0);

                            lcdd.add(pr);
                            lcdd.add(rotr);

                            PhysVol pvr = new PhysVol(moduleVolume, lcdd.getTrackingVolume(), pr, rotr);
                            pvr.addPhysVolID("system", sysId);
                            pvr.addPhysVolID("barrel", 0);
                            pvr.addPhysVolID("layer", layerId);
                            pvr.addPhysVolID("module", moduleNumber);
                            
                            System.out.print("    <module_placement name=\"" + module + "\" ");
                            System.out.print("id=\"" + moduleNumber + "\" ");
                            System.out.print("x=\"" + pr.getAttribute("x").getDoubleValue() + "\" ");
                            System.out.print("y=\"" + pr.getAttribute("y").getDoubleValue() + "\" ");
                            System.out.print("z=\"" + pr.getAttribute("z").getDoubleValue() + "\" ");
                            System.out.print("rx=\"" + rotr.getAttribute("x").getDoubleValue() + "\" ");
                            System.out.print("ry=\"" + rotr.getAttribute("y").getDoubleValue() + "\" ");
                            System.out.print("rz=\"" + rotr.getAttribute("z").getDoubleValue() + "\"/>");
                            System.out.println();
                            
                            ++moduleNumber;                            
                        }
                        
                        dz = -dz;
                        y += yStep;                        
                    }
                    x += xStep;
                }
            }
            System.out.println("</layer>");
        }
    }

    private Volume makeModule(ModuleParameters params, SensitiveDetector sd, LCDD lcdd)
    {
        double thickness = params.getThickness();
        double x, y;
        //x = params.getDimension(0);
        //y = params.getDimension(1);
        y = params.getDimension(0); // Y is in X plane in world coordinates.
        x = params.getDimension(1); // X is in Y plane in world coordinates.
        //System.out.println("making module with x = " + x + " and y = " + y);
        Box box = new Box(params.getName() + "Box", x, y, thickness);
        lcdd.add(box);

        Volume moduleVolume = new Volume(params.getName() + "Volume", box, vacuum);
        makeModuleComponents(moduleVolume, params, sd, lcdd);
        lcdd.add(moduleVolume);

        if (params.getVis() != null)
        {
            moduleVolume.setVisAttributes(lcdd.getVisAttributes(params.getVis()));
        }

        return moduleVolume;
    }

    private void makeModuleComponents(Volume moduleVolume, ModuleParameters moduleParameters, SensitiveDetector sd, LCDD lcdd)
    {
        Box envelope = (Box) lcdd.getSolid(moduleVolume.getSolidRef());

        double moduleX = envelope.getX();
        double moduleY = envelope.getY();

        double posZ = -moduleParameters.getThickness() / 2;

        String moduleName = moduleVolume.getVolumeName();

        int sensor = 0;
        for (ModuleComponentParameters component : moduleParameters)
        {

            double thickness = component.getThickness();

            Material material = null;
            try
            {
                material = lcdd.getMaterial(component.getMaterialName());
            }
            catch (JDOMException except)
            {
                throw new RuntimeException(except);
            }
            boolean sensitive = component.isSensitive();
            int componentNumber = component.getComponentNumber();

            posZ += thickness / 2;

            String componentName = moduleName + "_component" + componentNumber;

            //System.out.println("making " + componentName + " with x = " + moduleX + " and y = " + moduleY);
            Box componentBox = new Box(componentName + "Box", moduleX, moduleY, thickness);
            lcdd.add(componentBox);

            Volume componentVolume = new Volume(componentName, componentBox, material);

            Position position = new Position(componentName + "_position", 0., 0., posZ);
            lcdd.add(position);
            Rotation rotation = new Rotation(componentName + "_rotation", 0., 0., 0.);
            lcdd.add(rotation);

            PhysVol pv = new PhysVol(componentVolume, moduleVolume, position, rotation);
            pv.addPhysVolID("component", componentNumber);

            if (sensitive)
            {
                if (sensor > 1)
                {
                    throw new RuntimeException("Maximum of 2 sensors per module.");
                }

                // Build a child sensor volume to allow dead areas.

                String sensorName = componentName + "Sensor" + sensor;

                // Flipped these around!!!
                double sensorX = component.getDimensionY();
                double sensorY = component.getDimensionX();
                /*
                
                if (sensorX > moduleX)
                    throw new RuntimeException("Sensor X dimension " + sensorX + " is too big for module.");

                
                if (sensorY > moduleY)
                    throw new RuntimeException("Sensor Y dimension " + sensorY + " is too big for module.");
                    */

                Box sensorBox = new Box(sensorName + "Box", sensorX, sensorY, thickness);
                lcdd.add(sensorBox);

                Volume sensorVol = new Volume(sensorName, sensorBox, material);
                sensorVol.setSensitiveDetector(sd);
                lcdd.add(sensorVol);

                Position sensorPosition = new Position(sensorName + "Position", 0, 0, 0);
                lcdd.add(sensorPosition);
                Rotation sensorRotation = new Rotation(sensorName + "Rotation", 0, 0, 0);
                lcdd.add(sensorRotation);

                PhysVol sensorPhysVol = new PhysVol(sensorVol, componentVolume, sensorPosition, sensorRotation);
                sensorPhysVol.addPhysVolID("sensor", sensor);

                ++sensor;
            }

            // Add component volume after (possible) sensor child volume.
            lcdd.add(componentVolume);

            // Set vis attributes of component.
            if (component.getVis() != null)
            {
                componentVolume.setVisAttributes(lcdd.getVisAttributes(component.getVis()));
            }

            // Step to next component placement position.
            posZ += thickness / 2;
        }
    }

    static class ModuleComponentParameters
    {
        protected String materialName;
        protected double thickness;
        protected boolean sensitive;
        protected int componentNumber;
        protected String vis;
        protected double dimX, dimY;

        public ModuleComponentParameters(double dimX, double dimY, double thickness, String materialName, int componentNumber, boolean sensitive, String vis)
        {
            this.dimX = dimX;
            this.dimY = dimY;
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

        public double getDimensionX()
        {
            return dimX;
        }

        public double getDimensionY()
        {
            return dimY;
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

    static class ModuleParameters extends ArrayList<ModuleComponentParameters>
    {
        double thickness;
        String name;
        double dimensions[] = new double[3];
        String vis;

        public ModuleParameters(Element element)
        {
            name = element.getAttributeValue("name");

            if (element.getAttribute("vis") != null)
                this.vis = element.getAttribute("vis").getValue();

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
            else if (element.getChild("box") != null)
            {
                Element box = element.getChild("box");
                try
                {
                    dimensions[0] = box.getAttribute("x").getDoubleValue();
                    dimensions[1] = box.getAttribute("y").getDoubleValue();
                }
                catch (DataConversionException x)
                {
                    throw new RuntimeException(x);
                }
            }

            int cntr = 0;
            for (Object o : element.getChildren("module_component"))
            {
                try
                {

                    Element e = (Element) o;

                    double thickness = e.getAttribute("thickness").getDoubleValue();

                    String materialName = e.getAttributeValue("material");

                    boolean sensitive = false;
                    if (e.getAttribute("sensitive") != null)
                        sensitive = e.getAttribute("sensitive").getBooleanValue();
                    String componentVis = null;
                    if (e.getAttribute("vis") != null)
                        componentVis = e.getAttribute("vis").getValue();

                    // Sensors may have reduced dimensions for dead area.
                    double x = dimensions[0]; // default
                    double y = dimensions[1]; // default
                    if (sensitive && e.getChild("dimensions") != null)
                    {
                        Element dimensions = e.getChild("dimensions");
                        x = dimensions.getAttribute("x").getDoubleValue();
                        y = dimensions.getAttribute("y").getDoubleValue();
                        // System.out.println("x,y="+x+","+y);
                    }
                    add(new ModuleComponentParameters(x, y, thickness, materialName, cntr, sensitive, componentVis));
                }
                catch (JDOMException x)
                {
                    throw new RuntimeException(x);
                }
                ++cntr;
            }

            calculateThickness();
        }

        public void calculateThickness()
        {
            thickness = 0.; // reset thickness
            for (ModuleComponentParameters p : this)
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
}
