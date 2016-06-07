package org.lcsim.detector.converter.compact;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jdom.DataConversionException;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.lcsim.detector.DetectorElement;
import org.lcsim.detector.DetectorIdentifierHelper;
import org.lcsim.detector.DetectorIdentifierHelper.SystemMap;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.ILogicalVolume;
import org.lcsim.detector.IPhysicalVolume;
import org.lcsim.detector.IRotation3D;
import org.lcsim.detector.ITranslation3D;
import org.lcsim.detector.LogicalVolume;
import org.lcsim.detector.PhysicalVolume;
import org.lcsim.detector.RotationGeant;
import org.lcsim.detector.Transform3D;
import org.lcsim.detector.Translation3D;
import org.lcsim.detector.converter.compact.subdetector.HpsTracker2;
import org.lcsim.detector.identifier.ExpandedIdentifier;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.detector.material.IMaterial;
import org.lcsim.detector.material.MaterialStore;
import org.lcsim.detector.solids.Box;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.HpsTestRunSiSensor;
import org.lcsim.detector.tracker.silicon.SiTrackerIdentifierHelper;
import org.lcsim.detector.tracker.silicon.SiTrackerModule;
import org.lcsim.geometry.compact.Detector;
import org.lcsim.geometry.compact.Subdetector;
import org.lcsim.geometry.subdetector.HPSTracker2;

/**
 * Converts an HPSTracker2 XML description into Java runtime objects.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 *
 */
public class HPSTracker2Converter extends AbstractSubdetectorConverter {
    
    private Map<String, ModuleParameters> moduleParameters = new HashMap<String, ModuleParameters>();
    private Map<String, LogicalVolume> modules = new HashMap<String, LogicalVolume>();
    private IMaterial trackingMaterial;
    private static final boolean debug = false;    
    
    public Class getSubdetectorType() {
        return HPSTracker2.class;
    }
    
    public IIdentifierHelper makeIdentifierHelper(Subdetector subdetector, SystemMap systemMap) {
        return new SiTrackerIdentifierHelper(subdetector.getDetectorElement(), makeIdentifierDictionary(subdetector), systemMap);
    }

    public void convert(Subdetector subdet, Detector detector) {
        trackingMaterial = MaterialStore.getInstance().get("TrackingMaterial");
        if (trackingMaterial == null) {
            trackingMaterial = MaterialStore.getInstance().get("Air");
        }
        
        // Get the tracking volume for module placement.
        ILogicalVolume trackingVolume = detector.getTrackingVolume().getLogicalVolume();
                
        // Get ID helper and dictionary for subdetector.
        DetectorIdentifierHelper helper = (DetectorIdentifierHelper) subdet.getDetectorElement().getIdentifierHelper();
        IIdentifierDictionary iddict = subdet.getDetectorElement().getIdentifierHelper().getIdentifierDictionary();
        int nfields = helper.getIdentifierDictionary().getNumberOfFields();
        
        // Get XML node for this subdetector.
        Element node = subdet.getNode();
        
        // Create the module logical volumes.
        for (Iterator i = node.getChildren("module").iterator(); i.hasNext();) {
            Element module = (Element) i.next();
            String moduleName = module.getAttributeValue("name");
            moduleParameters.put(moduleName, new ModuleParameters(module));
            modules.put(moduleName, makeModule(moduleParameters.get(moduleName)));
        }
        
        try {
            for (Iterator i = node.getChildren("layer").iterator(); i.hasNext();) {
                
                Element layerElement = (Element) i.next();
                int layerId = -1;
                layerId = layerElement.getAttribute("id").getIntValue();
                
                // Layer identifier.
                IExpandedIdentifier layerPosId = new ExpandedIdentifier(nfields);
                layerPosId.setValue(helper.getFieldIndex("system"), subdet.getSystemID());
                layerPosId.setValue(helper.getFieldIndex("barrel"), helper.getBarrelValue());
                layerPosId.setValue(helper.getFieldIndex("layer"), layerId);
                
                // DetectorElement for layer.
                IDetectorElement layerDe = new DetectorElement(subdet.getName() + "_layer" + layerId, subdet.getDetectorElement(), helper.pack(layerPosId));

                // Loop over modules within layer.
                for (Iterator j = layerElement.getChildren("module_placement").iterator(); j.hasNext();) {

                    Element modulePlacementElement = (Element)j.next();
                    String moduleName = modulePlacementElement.getAttributeValue("name");
                    int moduleNumber = modulePlacementElement.getAttribute("id").getIntValue();

                    // Get the position and rotation parameters.
                    double x, y, z, rx, ry, rz;
                    x = y = z = rx = ry = rz = 0;
                    
                    // If not specified, default value of zero will be used for each parameter.
                    if (modulePlacementElement.getAttribute("x") != null) {
                        x = modulePlacementElement.getAttribute("x").getDoubleValue();
                    }
                    if (modulePlacementElement.getAttribute("y") != null) {
                        y = modulePlacementElement.getAttribute("y").getDoubleValue();
                    }
                    if (modulePlacementElement.getAttribute("z") != null) {
                        z = modulePlacementElement.getAttribute("z").getDoubleValue();
                    }
                    if (modulePlacementElement.getAttribute("rx") != null) {
                        rx = modulePlacementElement.getAttribute("rx").getDoubleValue();
                    }
                    if (modulePlacementElement.getAttribute("ry") != null) {
                        ry = modulePlacementElement.getAttribute("ry").getDoubleValue();
                    }
                    if (modulePlacementElement.getAttribute("rz") != null) {
                        rz = modulePlacementElement.getAttribute("rz").getDoubleValue();
                    }
                    
                    ITranslation3D pos = new Translation3D(x, y, z);
                    IRotation3D rot = new RotationGeant(rx, ry, rz);
                    
                    String modulePlacementName = subdet.getName() + "_" + moduleName + "_layer" + layerId + "_module" + moduleNumber;
                    
                    LogicalVolume lv = modules.get(moduleName);
                    IPhysicalVolume modulePhysVol = new PhysicalVolume(new Transform3D(pos,rot), modulePlacementName, lv, trackingVolume, 0);
                    
                    if (debug)
                        System.out.println("made module: " + modulePhysVol.getName());
                    
                    // Module DetectorElement.
                    String modulePath = "/" + detector.getTrackingVolume().getName() + "/" + modulePlacementName;                                                        
                    SiTrackerModule moduleDe = new SiTrackerModule(modulePlacementName, layerDe, modulePath, moduleNumber);
                    
                    if (debug)
                        System.out.println("created new SiTrackerModule called " + modulePlacementName + " with path: " + modulePath);
                                        
                    // Make SiSensor DetectorElements.
                    int sensorNumber = 0;
                    for (IPhysicalVolume componentPhysVol : modulePhysVol.getLogicalVolume().getDaughters()) {
                        // Setup the sensor.
                        if (componentPhysVol.getLogicalVolume().getDaughters().size() != 0) {
                            
                            // Sensor physical volume.
                            IPhysicalVolume sensorPhysVol = componentPhysVol.getLogicalVolume().getDaughters().get(0);
                            
                            ExpandedIdentifier expId = new ExpandedIdentifier(iddict.getNumberOfFields());
                            
                            // Setup SiSensor's identifier.
                            expId.setValue(iddict.getFieldIndex("system"), subdet.getSystemID());
                            expId.setValue(iddict.getFieldIndex("barrel"), 0);                            
                            expId.setValue(iddict.getFieldIndex("layer"), helper.getValue(layerDe.getIdentifier(), "layer"));
                            expId.setValue(iddict.getFieldIndex("module"), ((SiTrackerModule) moduleDe).getModuleId());
                            expId.setValue(iddict.getFieldIndex("sensor"), sensorNumber);

                            // Packed identifier.
                            IIdentifier sensorId = iddict.pack(expId);

                            // Sensor paths.
                            String sensorPath = modulePath.toString() + "/" + componentPhysVol.getName() + "/" + sensorPhysVol.getName();
                            String sensorName = moduleDe.getName() + "_sensor" + sensorNumber;
                            
                            // Create the sensor.
                            HpsSiSensor sensor =  null; 
                            if(moduleParameters.get(moduleName).getType().equals(HpsTestRunSiSensor.class.getSimpleName())){
                                sensor = new HpsTestRunSiSensor(sensorNumber, sensorName, moduleDe, sensorPath, sensorId);
                            } else { 
                                sensor = new HpsSiSensor(sensorNumber, sensorName, moduleDe, sensorPath, sensorId);
                            }
                                      
                            if (debug)
                                System.out.println("created sensor " + sensor.getName());
                            
                            // Increment sensor numbering.
                            ++sensorNumber;
                        }
                    }
                }
            }
        } 
        catch (DataConversionException e) {
            throw new RuntimeException(e);
        }
        
        // Create the stereo layers 
        ((HpsTracker2) subdet.getDetectorElement()).createStereoLayers();
    }    
    
    private LogicalVolume makeModule(ModuleParameters params)
    {
        double thickness = params.getThickness();
        double x, y;
        y = params.getDimension(0); // Y is long dimension along world's X axis.
        x = params.getDimension(1); // X is short dimension along world Y axis.
        
        Box box = new Box(params.getName() + "Box", x / 2, y / 2, thickness / 2);
        LogicalVolume volume = new LogicalVolume(params.getName() + "Volume", box, trackingMaterial);
        
        makeModuleComponents(volume, params);
        
        return volume;
    }

    private void makeModuleComponents(LogicalVolume moduleVolume, ModuleParameters moduleParameters)
    {
        double moduleY = moduleParameters.getDimension(0);
        double moduleX = moduleParameters.getDimension(1);
        Box box = (Box)moduleVolume.getSolid();
        double moduleZ = box.getZHalfLength() * 2;                        
        double posZ = -moduleZ / 2;
        String moduleName = moduleVolume.getName();
        int sensorNumber = 0;       
        for (ModuleComponentParameters component : moduleParameters)
        {
            double componentThickness = component.getThickness();
            IMaterial material = MaterialStore.getInstance().get(component.getMaterialName());
            if (material == null)
            {
                throw new RuntimeException("The material " + component.getMaterialName() + " does not exist in the materials database.");
            }           
            boolean sensitive = component.isSensitive();            
            int componentNumber = component.getComponentNumber();
            posZ += componentThickness / 2;
            String componentName = moduleName + "_component" + componentNumber;
            Box componentBox = new Box(componentName + "Box", moduleX / 2, moduleY / 2, componentThickness / 2);
            LogicalVolume componentVolume = new LogicalVolume(componentName, componentBox, material);
            double zrot = 0;
            if (sensitive)
            {
                if (sensorNumber > 1)
                {
                    throw new RuntimeException("Exceeded maximum of 2 sensors per module.");
                }
                // Flip 180 deg for 1st sensor.
                if (sensorNumber == 0)
                {
                    zrot = Math.PI;
                }                
                String sensorName = componentName + "Sensor" + sensorNumber;
                double sensorX = component.getDimensionY(); // Flipped so X is actually Y.
                double sensorY = component.getDimensionX(); // Flipped so Y is actually X.
                Box sensorBox = new Box(sensorName + "Box", sensorX / 2, sensorY / 2, componentThickness / 2);
                LogicalVolume sensorVol = new LogicalVolume(sensorName, sensorBox, material);                
                Translation3D sensorPosition = new Translation3D(0, 0, 0);
                RotationGeant sensorRotation = new RotationGeant(0, 0, zrot);
                new PhysicalVolume(new Transform3D(sensorPosition, sensorRotation), sensorName, sensorVol, componentVolume, sensorNumber);                
                ++sensorNumber;
            }
            Translation3D position = new Translation3D(0., 0., posZ);
            RotationGeant rotation = new RotationGeant(0, 0, zrot);
            PhysicalVolume pv = new PhysicalVolume(new Transform3D(position, rotation), componentName, componentVolume, moduleVolume, componentNumber);
            pv.setSensitive(sensitive);
            posZ += componentThickness / 2;
        }
    }
    
    private static class ModuleComponentParameters {
        private String materialName;
        private double thickness;
        private boolean sensitive;
        private int componentNumber;
        private String vis;
        private double dimX, dimY;

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
        private double thickness;
        private String name;
        private double dimensions[] = new double[3];
        private String vis;
        private String type = ""; 

        ModuleParameters(Element element) {
            name = element.getAttributeValue("name");
            
            if (element.getAttribute("vis") != null)
                this.vis = element.getAttribute("vis").getValue();
            
            if(element.getAttribute("type") != null)
                this.type = element.getAttributeValue("type");
            
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
        
        String getType() { 
            return type;
        }
    }
    
    public IDetectorElement makeSubdetectorDetectorElement(Detector detector, Subdetector subdetector)
    {
        IDetectorElement subdetectorDE =
                new HpsTracker2(subdetector.getName(), detector.getDetectorElement());
        subdetector.setDetectorElement(subdetectorDE);
        return subdetectorDE;
    }
    
}
