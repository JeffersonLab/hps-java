package org.lcsim.detector.converter.compact;

import hep.physics.matrix.BasicMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.VecOp;

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

import org.lcsim.detector.IPhysicalVolume;
import org.lcsim.detector.IPhysicalVolumePath;
import org.lcsim.detector.IRotation3D;
import org.lcsim.detector.ITranslation3D;
import org.lcsim.detector.LogicalVolume;
import org.lcsim.detector.PhysicalVolume;
import org.lcsim.detector.RotationGeant;
import org.lcsim.detector.RotationPassiveXYZ;
import org.lcsim.detector.Transform3D;
import org.lcsim.detector.Translation3D;
import org.lcsim.detector.identifier.ExpandedIdentifier;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.detector.material.IMaterial;
import org.lcsim.detector.material.MaterialStore;
import org.lcsim.detector.solids.Box;
import org.lcsim.detector.solids.Polygon3D;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.SiStrips;
import org.lcsim.detector.tracker.silicon.SiTrackerIdentifierHelper;
import org.lcsim.detector.tracker.silicon.SiTrackerModule;
import org.lcsim.geometry.compact.Detector;
import org.lcsim.geometry.compact.Subdetector;
import org.lcsim.geometry.subdetector.HPSTracker;

public class HPSTrackerConverter extends AbstractSubdetectorConverter
{
    Map<String, ModuleParameters> moduleParameters = new HashMap<String, ModuleParameters>();
    Map<String, LogicalVolume> modules = new HashMap<String, LogicalVolume>();
    IMaterial vacuum;
    
    public Class getSubdetectorType()
    {
        return HPSTracker.class;
    }

    public IIdentifierHelper makeIdentifierHelper(Subdetector subdetector, SystemMap systemMap)
    {
        return new SiTrackerIdentifierHelper(subdetector.getDetectorElement(), makeIdentifierDictionary(subdetector), systemMap);
    }

    public void convert(Subdetector subdet, Detector detector)
    {
        try
        {
            Element node = subdet.getNode();
            String subdetName = node.getAttributeValue("name");
            vacuum = MaterialStore.getInstance().get("Air");

            boolean reflect = true;
            if (node.getAttribute("reflect") != null)
            {
                reflect = node.getAttribute("reflect").getBooleanValue();
            }

           

            IDetectorElement subdetDetElem = subdet.getDetectorElement();
            DetectorIdentifierHelper helper = (DetectorIdentifierHelper) subdetDetElem.getIdentifierHelper();
            int nfields = helper.getIdentifierDictionary().getNumberOfFields();
            IDetectorElement endcapPos = null;
            IDetectorElement endcapNeg = null;
            try
            {
                // Positive endcap DE
                IExpandedIdentifier endcapPosId = new ExpandedIdentifier(nfields);
                endcapPosId.setValue(helper.getFieldIndex("system"), subdet.getSystemID());
                endcapPosId.setValue(helper.getFieldIndex("barrel"), helper.getBarrelValue());
                endcapPos = new DetectorElement(subdet.getName() + "_positive", subdetDetElem);
                endcapPos.setIdentifier(helper.pack(endcapPosId));
                if (reflect)
                {
                    IExpandedIdentifier endcapNegId = new ExpandedIdentifier(nfields);
                    endcapNegId.setValue(helper.getFieldIndex("system"), subdet.getSystemID());
                    endcapNegId.setValue(helper.getFieldIndex("barrel"), helper.getBarrelValue());
                    endcapNeg = new DetectorElement(subdet.getName() + "_negative", subdetDetElem);
                    endcapNeg.setIdentifier(helper.pack(endcapNegId));
                }
            }
            catch (Exception x)
            {
                throw new RuntimeException(x);
            }

            for (Iterator i = node.getChildren("module").iterator(); i.hasNext();)
            {
                Element module = (Element) i.next();
                String moduleName = module.getAttributeValue("name");
                moduleParameters.put(moduleName, new ModuleParameters(module));
                modules.put(moduleName, makeModule(moduleParameters.get(moduleName)));
            }

            for (Iterator i = node.getChildren("layer").iterator(); i.hasNext();)
            {
                Element layerElement = (Element) i.next();

                int layerId = layerElement.getAttribute("id").getIntValue();

                // Positive endcap layer.
                IExpandedIdentifier layerPosId = new ExpandedIdentifier(nfields);
                layerPosId.setValue(helper.getFieldIndex("system"), subdet.getSystemID());
                layerPosId.setValue(helper.getFieldIndex("barrel"), helper.getBarrelValue());
                layerPosId.setValue(helper.getFieldIndex("layer"), layerId);
                IDetectorElement layerPos = new DetectorElement(endcapPos.getName() + "_layer" + layerId, endcapPos, helper.pack(layerPosId));

                // Negative endcap layer.
                IDetectorElement layerNeg = null;
                if (reflect)
                {
                    IExpandedIdentifier layerNegId = new ExpandedIdentifier(nfields);
                    layerNegId.setValue(helper.getFieldIndex("system"), subdet.getSystemID());
                    layerNegId.setValue(helper.getFieldIndex("barrel"), helper.getBarrelValue());
                    layerNegId.setValue(helper.getFieldIndex("layer"), layerId);
                    layerNeg = new DetectorElement(endcapNeg.getName() + "_layer_reflected" + layerId, endcapNeg, helper.pack(layerNegId));
                }

                int moduleNumber = 0;
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
                    String module = ringElement.getAttributeValue("module");
                    LogicalVolume moduleVolume = modules.get(module);
                    if (moduleVolume == null)
                    {
                        throw new RuntimeException("Module " + module + " was not found.");
                    }
                    double theta = 0;
                    if (ringElement.getAttribute("theta") != null)
                    {
                        theta = ringElement.getAttribute("theta").getDoubleValue();
                    }
                    
                    ModuleParameters modPars = moduleParameters.get(module);

                    double x, y, z;
                    z = zLayer;
                    x = xStart;
                    // System.out.println("Making modules...nx=" + nx + ";ny=" + ny);
                    for (int k = 0; k < nx; k++ )
                    {
                        y = yStart;
                        for (int kk = 0; kk < ny; kk++ )
                        {
                            String moduleBaseName = subdetName + "_layer" + layerId + "_module" + moduleNumber;
                            Translation3D p = new Translation3D(x, y, z + dz);
                            //RotationGeant rot = new RotationGeant(0, theta,-(Math.PI / 2 + top_phi0));
                            RotationGeant rot = new RotationGeant(0, theta,-Math.PI/2 - top_phi0);
                            new PhysicalVolume(new Transform3D(p, rot), moduleBaseName, moduleVolume, detector.getTrackingVolume().getLogicalVolume(), 0);
                            String path = "/" + detector.getTrackingVolume().getName() + "/" + moduleBaseName;
                            new SiTrackerModule(moduleBaseName, layerPos, path, moduleNumber);
                            ++moduleNumber;                           
                            if (reflect)
                            {
                                Translation3D pr = new Translation3D(x, -y, z + dz);                                                         
                                  //first x, then y, then z...
                                //RotationGeant rotr = new RotationGeant(0, theta, Math.PI/2 - bot_phi0);                                                                
                                RotationGeant rotr = new RotationGeant(0, theta, -Math.PI/2 - bot_phi0);
                                String path2 = "/" + detector.getTrackingVolume().getName() + "/" + moduleBaseName + "_reflected";
                                new PhysicalVolume(new Transform3D(pr, rotr), moduleBaseName + "_reflected", moduleVolume, detector.getTrackingVolume().getLogicalVolume(), k);
                                new SiTrackerModule(moduleBaseName + "_reflected", layerNeg, path2, moduleNumber);
                            }

                            dz = -dz;
                            y += yStep;
                            ++moduleNumber;
                        }
                        x += xStep;
                    }
                }
            }
        }
        catch (JDOMException except)
        {
            throw new RuntimeException(except);
        }
        makeSensors(subdet);
    }

    private LogicalVolume makeModule(ModuleParameters params)
    {
        double thickness = params.getThickness();
        double x, y;
        //x = params.getDimension(0);
        //y = params.getDimension(1);        
        y = params.getDimension(0); // Y is long dimension along world's X axis.
        x = params.getDimension(1); // X is short dimension along world Y axis.
        
        Box box = new Box(params.getName() + "Box", x / 2, y / 2, thickness / 2);
        LogicalVolume volume = new LogicalVolume(params.getName() + "Volume", box, vacuum);
        
        makeModuleComponents(volume, params);
        
        return volume;
    }

    private void makeModuleComponents(LogicalVolume moduleVolume, ModuleParameters moduleParameters)
    {
        //double moduleX = moduleParameters.getDimension(0);
        //double moduleY = moduleParameters.getDimension(1);        
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
                //if (sensorX > moduleX)
                //    throw new RuntimeException("Sensor X dimension " + sensorX + " is too big for module.");
                double sensorY = component.getDimensionX(); // Flipped so Y is actually X.
                //if (sensorY > moduleY)
                //    throw new RuntimeException("Sensor Y dimension " + sensorY + " is too big for module.");
                Box sensorBox = new Box(sensorName + "Box", sensorX / 2, sensorY / 2, componentThickness / 2);
                LogicalVolume sensorVol = new LogicalVolume(sensorName, sensorBox, material);                
                Translation3D sensorPosition = new Translation3D(0, 0, 0);
                RotationGeant sensorRotation = new RotationGeant(0, 0, zrot);
                //PhysicalVolume sensorPhysVol = 
                new PhysicalVolume(new Transform3D(sensorPosition, sensorRotation), sensorName, sensorVol, componentVolume, sensorNumber);                
                // TODO Could make sensors here?
                ++sensorNumber;
            }
            Translation3D position = new Translation3D(0., 0., posZ);
            RotationGeant rotation = new RotationGeant(0, 0, zrot);
            PhysicalVolume pv = new PhysicalVolume(new Transform3D(position, rotation), componentName, componentVolume, moduleVolume, componentNumber);
            pv.setSensitive(sensitive);
            posZ += componentThickness / 2;
        }
    }

    // Called after geometry is in place to create the Sensor DetectorElements.
    private void makeSensors(Subdetector subdet)
    {
        SiTrackerIdentifierHelper helper = (SiTrackerIdentifierHelper) subdet.getDetectorElement().getIdentifierHelper();
        for (IDetectorElement endcap : subdet.getDetectorElement().getChildren())
        {
            for (IDetectorElement layer : endcap.getChildren())
            {
                for (IDetectorElement module : layer.getChildren())
                {
                    IPhysicalVolume modulePhysVol = module.getGeometry().getPhysicalVolume();
                    IPhysicalVolumePath modulePath = module.getGeometry().getPath();
                    int sensorId = 0;
                    for (IPhysicalVolume componentPhysVol : modulePhysVol.getLogicalVolume().getDaughters())
                    {
                        // Setup the sensor.
                        if (componentPhysVol.getLogicalVolume().getDaughters().size() != 0)
                        {
                            IPhysicalVolume sensorPhysVol = componentPhysVol.getLogicalVolume().getDaughters().get(0);
                            
                            IIdentifierDictionary iddict = subdet.getDetectorElement().getIdentifierHelper().getIdentifierDictionary();

                            ExpandedIdentifier expId = new ExpandedIdentifier(iddict.getNumberOfFields());
                            expId.setValue(iddict.getFieldIndex("system"), subdet.getSystemID());

                            if (helper.isEndcapPositive(endcap.getIdentifier()))
                            {
                                expId.setValue(iddict.getFieldIndex("barrel"), helper.getEndcapPositiveValue());
                            }
                            else if (helper.isEndcapNegative(endcap.getIdentifier()))
                            {
                                expId.setValue(iddict.getFieldIndex("barrel"), helper.getEndcapNegativeValue());
                            }
                            else if (helper.isBarrel(endcap.getIdentifier()))
                            {
                                expId.setValue(iddict.getFieldIndex("barrel"), helper.getBarrelValue());
                            }
                            else
                            {
                                throw new RuntimeException(endcap.getName() + " is not a positive or negative endcap!");
                            }
                            expId.setValue(iddict.getFieldIndex("layer"), layer.getIdentifierHelper().getValue(layer.getIdentifier(), "layer"));
                            expId.setValue(iddict.getFieldIndex("module"), ((SiTrackerModule) module).getModuleId());
                            expId.setValue(iddict.getFieldIndex("sensor"), sensorId);

                            IIdentifier id = iddict.pack(expId);

                            String sensorPath = modulePath.toString() + "/" + componentPhysVol.getName() + "/" + sensorPhysVol.getName();
                            String sensorName = module.getName() + "_sensor" + sensorId;

                            // Create the sensor.
                            SiSensor sensor = new SiSensor(sensorId, sensorName, module, sensorPath, id);
                            
                            // Configure parameters of strips, etc.                              
                            //configSensor(sensor);                            

                            // Increment sensor numbering.
                            ++sensorId;
                        }
                    //    }
                    }
                }
            }
        }
    }
   
    // Parameters...    
    private double readoutCapacitanceIntercept = 0;
    private double readoutCapacitanceSlope = 0.16;
    private double senseCapacitanceIntercept = 0;
    private double senseCapacitanceSlope = 0.16;
    private double readoutStripPitch = 0.060;
    private double senseStripPitch = 0.030;
    private double readoutTransferEfficiency = 0.986;
    private double senseTransferEfficiency =  0.419;

    // TODO: Move this method to a Driver class.
    /*
    private void configSensor(SiSensor sensor)
    {
        //
        Box sensorSolid = (Box) sensor.getGeometry().getLogicalVolume().getSolid();                                                        
        
        Polygon3D pside = sensorSolid.getFacesNormalTo(new BasicHep3Vector(0, 0, 1)).get(0);
        Polygon3D nside = sensorSolid.getFacesNormalTo(new BasicHep3Vector(0, 0, -1)).get(0);         

        sensor.setBiasSurface(ChargeCarrier.HOLE, pside);     // P side collects holes.
        sensor.setBiasSurface(ChargeCarrier.ELECTRON, nside); // N side collects electrons.

        // Setup electrodes on the XY front and back surfaces of the Box. 
        ITranslation3D electrodesPosition = new Translation3D(VecOp.mult(-pside.getDistance(), pside.getNormal()));  // Translate to outside of polygon (always the same).
        
        // Strip angle.
        IRotation3D electrodesRotation = new RotationPassiveXYZ(0.0, 0.0, 0.0); // Strips aligned to edge of sensor, ALWAYS.
        Transform3D electrodesTransform = new Transform3D(electrodesPosition, electrodesRotation);
        
        // Free calculation of readout electrodes, sense electrodes determined thereon.
        SiStrips readoutElectrodes = new SiStrips(ChargeCarrier.HOLE, readoutStripPitch, sensor, electrodesTransform);
        SiStrips senseElectrodes = new SiStrips(ChargeCarrier.HOLE, senseStripPitch, (readoutElectrodes.getNCells()*2-1), sensor, electrodesTransform);
        //

        // Readout electrode parameters.
        readoutElectrodes.setCapacitanceIntercept(readoutCapacitanceIntercept);
        readoutElectrodes.setCapacitanceSlope(readoutCapacitanceSlope);
        
        // Sense electrode parameters.
        senseElectrodes.setCapacitanceIntercept(senseCapacitanceIntercept);
        senseElectrodes.setCapacitanceSlope(senseCapacitanceSlope); 

        // Set sense and readout electrodes.
        sensor.setSenseElectrodes(senseElectrodes);
        sensor.setReadoutElectrodes(readoutElectrodes);

        // Charge transfer efficiency.
        double[][] transferEfficiencies = {{readoutTransferEfficiency, senseTransferEfficiency}};
        sensor.setTransferEfficiencies(ChargeCarrier.HOLE, new BasicMatrix(transferEfficiencies));
    }
    */
    
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
