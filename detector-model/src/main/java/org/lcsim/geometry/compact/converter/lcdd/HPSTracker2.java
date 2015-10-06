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
 * 
 * SVT geometry for HPS Test Run.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @author Matt Graham <mgraham@slac.stanford.edu>
 */
public class HPSTracker2 extends LCDDSubdetector {

    Map<String, ModuleParameters> moduleParameters = new HashMap<String, ModuleParameters>();
    Map<String, Volume> modules = new HashMap<String, Volume>();
    Material vacuum;

    public HPSTracker2(Element node) throws JDOMException {
        super(node);
    }
    
    public boolean isTracker() {
        return true;
    }

    void addToLCDD(LCDD lcdd, SensitiveDetector sd) throws JDOMException {
        
        // Get parameters.
        int sysId = node.getAttribute("id").getIntValue();
        String subdetName = node.getAttributeValue("name");
        vacuum = lcdd.getMaterial("Vacuum");
   
        // Create module logical volumes.
        createModules(lcdd, sd);
        
        // Create module placements in tracking volume.
        createModulePlacements(lcdd, sysId, subdetName);
    }

    // Place modules within the tracking volume.
	private void createModulePlacements(LCDD lcdd, int sysId, String subdetName) throws DataConversionException {
		//Volume trackingVolume = lcdd.getTrackingVolume();
		Volume momVolume = lcdd.pickMotherVolume(this);
		// Loop over layers.
        for (Iterator i = node.getChildren("layer").iterator(); i.hasNext();) {        
        	Element layerElement = (Element)i.next();
        	int layerNumber = layerElement.getAttribute("id").getIntValue();
        	// Loop over modules within layer.
        	for (Iterator j = layerElement.getChildren("module_placement").iterator(); j.hasNext();) {
        	    
        		Element modulePlacementElement = (Element)j.next();
        		String moduleName = modulePlacementElement.getAttributeValue("name");
        		int moduleNumber = modulePlacementElement.getAttribute("id").getIntValue();
        		
        		// Get the position and rotation parameters.  All must be explicitly specified.
        		double x, y, z;
        		double rx, ry, rz;
        		x = modulePlacementElement.getAttribute("x").getDoubleValue();
        		y = modulePlacementElement.getAttribute("y").getDoubleValue();
        		z = modulePlacementElement.getAttribute("z").getDoubleValue();
        		rx = modulePlacementElement.getAttribute("rx").getDoubleValue();
        		ry = modulePlacementElement.getAttribute("ry").getDoubleValue();
        		rz = modulePlacementElement.getAttribute("rz").getDoubleValue();
        		
        		// Place the module with position and rotation from above.
        		String modulePlacementName = subdetName + "_" + moduleName + "_layer" + layerNumber + "_module" + moduleNumber;
        		Position p = new Position(modulePlacementName + "_position", x, y, z);
        		Rotation r = new Rotation(modulePlacementName + "_rotation", rx, ry, rz);
        		lcdd.add(p);
        		lcdd.add(r);        		        		
        		//PhysVol modulePhysVol = new PhysVol(modules.get(moduleName), trackingVolume, p, r);
        		PhysVol modulePhysVol = new PhysVol(modules.get(moduleName), momVolume, p, r);
        		
        		// Add identifier values to the placement volume.
        		modulePhysVol.addPhysVolID("system", sysId);
        		modulePhysVol.addPhysVolID("barrel", 0);
        		modulePhysVol.addPhysVolID("layer", layerNumber);
        		modulePhysVol.addPhysVolID("module", moduleNumber);        		
        	}
        }
	}

    // Create the module logical volumes.
	private void createModules(LCDD lcdd, SensitiveDetector sd) {
        for (Iterator i = node.getChildren("module").iterator(); i.hasNext();) {
            Element module = (Element) i.next();
            String moduleName = module.getAttributeValue("name");
            moduleParameters.put(moduleName, new ModuleParameters(module));
            modules.put(moduleName, makeModule(moduleParameters.get(moduleName), sd, lcdd));
        }
	}

	private Volume makeModule(ModuleParameters params, SensitiveDetector sd, LCDD lcdd) {
		double thickness = params.getThickness();
		double x, y;
		// x = params.getDimension(0);
		// y = params.getDimension(1);
		y = params.getDimension(0); // Y is in X plane in world coordinates.
		x = params.getDimension(1); // X is in Y plane in world coordinates.
		// System.out.println("making module with x = " + x + " and y = " + y);
		Box box = new Box(params.getName() + "Box", x, y, thickness);
		lcdd.add(box);

		Volume moduleVolume = new Volume(params.getName() + "Volume", box, vacuum);
		makeModuleComponents(moduleVolume, params, sd, lcdd);
		lcdd.add(moduleVolume);

		if (params.getVis() != null) {
			moduleVolume.setVisAttributes(lcdd.getVisAttributes(params.getVis()));
		}

		return moduleVolume;
	}

    private void makeModuleComponents(Volume moduleVolume, ModuleParameters moduleParameters, SensitiveDetector sd, LCDD lcdd) {
        Box envelope = (Box) lcdd.getSolid(moduleVolume.getSolidRef());

        double moduleX = envelope.getX();
        double moduleY = envelope.getY();

        double posZ = -moduleParameters.getThickness() / 2;

        String moduleName = moduleVolume.getVolumeName();

        int sensor = 0;
        for (ModuleComponentParameters component : moduleParameters) {

            double thickness = component.getThickness();

            Material material = null;
            try {
                material = lcdd.getMaterial(component.getMaterialName());
            } catch (JDOMException except) {
                throw new RuntimeException(except);
            }
            boolean sensitive = component.isSensitive();
            int componentNumber = component.getComponentNumber();

            posZ += thickness / 2;

            String componentName = moduleName + "_component" + componentNumber;

            Box componentBox = new Box(componentName + "Box", moduleX, moduleY, thickness);
            lcdd.add(componentBox);

            Volume componentVolume = new Volume(componentName, componentBox, material);

            Position position = new Position(componentName + "_position", 0., 0., posZ);
            lcdd.add(position);
            Rotation rotation = new Rotation(componentName + "_rotation", 0., 0., 0.);
            lcdd.add(rotation);

            PhysVol pv = new PhysVol(componentVolume, moduleVolume, position, rotation);
            pv.addPhysVolID("component", componentNumber);

            if (sensitive) {
                if (sensor > 1) {
                    throw new RuntimeException("Maximum of 2 sensors per module.");
                }

                // Build a child sensor volume to allow dead areas.

                String sensorName = componentName + "Sensor" + sensor;

                // Flipped these around!!!
                double sensorX = component.getDimensionY();
                double sensorY = component.getDimensionX();
                
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
            if (component.getVis() != null) {
                componentVolume.setVisAttributes(lcdd.getVisAttributes(component.getVis()));
            }

            // Step to next component placement position.
            posZ += thickness / 2;
        }
    }

    private static class ModuleComponentParameters {
        protected String materialName;
        protected double thickness;
        protected boolean sensitive;
        protected int componentNumber;
        protected String vis;
        protected double dimX, dimY;

        ModuleComponentParameters(double dimX, double dimY, double thickness, String materialName, int componentNumber, boolean sensitive, String vis) {
            this.dimX = dimX;
            this.dimY = dimY;
            this.thickness = thickness;
            this.materialName = materialName;
            this.sensitive = sensitive;
            this.componentNumber = componentNumber;
            this.vis = vis;
        }

        double getThickness() {
            return thickness;
        }

        double getDimensionX() {
            return dimX;
        }

        double getDimensionY() {
            return dimY;
        }

        String getMaterialName() {
            return materialName;
        }

        boolean isSensitive() {
            return sensitive;
        }

        int getComponentNumber() {
            return componentNumber;
        }

        String getVis() {
            return vis;
        }
    }

    private static class ModuleParameters extends ArrayList<ModuleComponentParameters> {
        double thickness;
        String name;
        double dimensions[] = new double[3];
        String vis;

        public ModuleParameters(Element element) {
            name = element.getAttributeValue("name");

            if (element.getAttribute("vis") != null)
                this.vis = element.getAttribute("vis").getValue();

            // Optional dimension parameters (not always present).
            if (element.getChild("trd") != null) {
                Element trd = element.getChild("trd");
                try {
                    dimensions[0] = trd.getAttribute("x1").getDoubleValue();
                    dimensions[1] = trd.getAttribute("x2").getDoubleValue();
                    dimensions[2] = trd.getAttribute("z").getDoubleValue();
                } catch (DataConversionException x) {
                    throw new RuntimeException(x);
                }
            } else if (element.getChild("box") != null) {
                Element box = element.getChild("box");
                try {
                    dimensions[0] = box.getAttribute("x").getDoubleValue();
                    dimensions[1] = box.getAttribute("y").getDoubleValue();
                } catch (DataConversionException x) {
                    throw new RuntimeException(x);
                }
            }

            int cntr = 0;
            for (Object o : element.getChildren("module_component")) {
                try {

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
                    if (sensitive && e.getChild("dimensions") != null) {
                        Element dimensions = e.getChild("dimensions");
                        x = dimensions.getAttribute("x").getDoubleValue();
                        y = dimensions.getAttribute("y").getDoubleValue();
                        // System.out.println("x,y="+x+","+y);
                    }
                    add(new ModuleComponentParameters(x, y, thickness, materialName, cntr, sensitive, componentVis));
                } catch (JDOMException x) {
                    throw new RuntimeException(x);
                }
                ++cntr;
            }

            calculateThickness();
        }

        void calculateThickness() {
            thickness = 0.; // reset thickness
            for (ModuleComponentParameters p : this) {
                thickness += p.getThickness();
            }
        }

        double getThickness() {
            return thickness;
        }

        String getName() {
            return name;
        }

        double getDimension(int i) {
            if (i > (dimensions.length - 1) || i < 0)
                throw new RuntimeException("Invalid dimensions index: " + i);
            return dimensions[i];
        }

        String getVis() {
            return vis;
        }
    }
}