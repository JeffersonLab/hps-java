package org.lcsim.detector.converter.compact;

import org.jdom.Element;
import org.lcsim.detector.DetectorElement;
import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.DetectorIdentifierHelper;
import org.lcsim.detector.DetectorIdentifierHelper.SystemMap;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.IGeometryInfo;
import org.lcsim.detector.ILogicalVolume;
import org.lcsim.detector.IPhysicalVolume;
import org.lcsim.detector.PhysicalVolume;
import org.lcsim.detector.Transform3D;
import org.lcsim.detector.converter.compact.subdetector.HpsTracker2;
import org.lcsim.detector.identifier.ExpandedIdentifier;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.detector.material.IMaterial;
import org.lcsim.detector.material.MaterialStore;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiTrackerIdentifierHelper;
import org.lcsim.detector.tracker.silicon.SiTrackerModule;
import org.lcsim.geometry.compact.Detector;
import org.lcsim.geometry.compact.Subdetector;
import org.lcsim.geometry.compact.converter.HPSTrackerBuilder;
import org.lcsim.geometry.compact.converter.HPSTrackerJavaBuilder;
import org.lcsim.geometry.compact.converter.JavaGhostSurveyVolume;
import org.lcsim.geometry.compact.converter.JavaSurveyVolume;

/**
 * Converts the compact description into Java runtime objects
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public abstract class HPSTracker2014ConverterBase extends AbstractSubdetectorConverter {

    protected boolean _debug = false;
    protected IMaterial trackingMaterial = null;
    protected static HPSTrackerJavaBuilder builder;

    /**
     * Default constructor.
     */
    public HPSTracker2014ConverterBase() {
        super();
    }

    /**
     * Initialize builder for this converter.
     * @param node
     * @return builder.
     */
    abstract protected HPSTrackerJavaBuilder initializeBuilder(Element node);
    
    
    /**
     * Abstract method to create the correct type of {@link HpsSiSensor}.
     * @param sensorid
     * @param name
     * @param parent
     * @param support
     * @param id
     * @return the created sensor.
     */
    abstract HpsSiSensor createSiSensor(int sensorid, String name, IDetectorElement parent, String support, IIdentifier id);
    
    
    /* (non-Javadoc)
     * @see org.lcsim.detector.converter.compact.AbstractSubdetectorConverter#makeIdentifierHelper(org.lcsim.geometry.compact.Subdetector, org.lcsim.detector.DetectorIdentifierHelper.SystemMap)
     */
    public IIdentifierHelper makeIdentifierHelper(Subdetector subdetector, SystemMap systemMap) {
    	return new SiTrackerIdentifierHelper(subdetector.getDetectorElement(), makeIdentifierDictionary(subdetector), systemMap);
    }

    

    /* (non-Javadoc)
     * @see org.lcsim.detector.converter.compact.AbstractSubdetectorConverter#convert(org.lcsim.geometry.compact.Subdetector, org.lcsim.geometry.compact.Detector)
     */
    public void convert(Subdetector subdet, Detector detector) {

        if(_debug) System.out.printf("%s: convert %s \n", getClass().getSimpleName(), subdet.getName());


        // check tracking material
        trackingMaterial = MaterialStore.getInstance().get("Vacuum");
        if(trackingMaterial==null) {
            throw new RuntimeException("error the tracking material was not found!");
        }

        // Get XML node for this subdetector.
        Element node = subdet.getNode();

        // Get the tracking volume for module placement.
        ILogicalVolume trackingVolume = detector.getTrackingVolume().getLogicalVolume();



        // build the local geometry
        builder = initializeBuilder(node);
        //builder = new HPSTestRunTracker2014JavaBuilder(_debug,node);


        // Set subdetector for later reference
        builder.setSubdetector(subdet);

        // Get ID helper and dictionary for subdetector.
        builder.setDetectorIdentifierHelper( (DetectorIdentifierHelper) subdet.getDetectorElement().getIdentifierHelper());
        builder.setIdentifierDictionary(subdet.getDetectorElement().getIdentifierHelper().getIdentifierDictionary());



        // Build the detector here
        // setup and build the LCDD geometry
        if(_debug) System.out.printf("%s: setup and build the JAVA geometry\n", getClass().getSimpleName());

        builder.build(trackingVolume);

        if(_debug) System.out.printf("%s: DONE setup and build the JAVA geometry\n", getClass().getSimpleName());

        if(_debug) System.out.printf("%s: setup physical volumes\n", getClass().getSimpleName());
        
        setupPhysicalVolumes();
        
        if(_debug) System.out.printf("%s: DONE setup physical volumes\n", getClass().getSimpleName());
        
        if(_debug) System.out.printf("%s: create stereo layers\n", getClass().getSimpleName());
        
        ((HpsTracker2) subdet.getDetectorElement()).createStereoLayers();
       
        if(_debug) System.out.printf("%s: DONE create stereo layers\n", getClass().getSimpleName());
       
        if(_debug) printDEs();
        
    }


   

    /**
     * Setup physical volumes based on the top level {@link JavaSurveyVolume}.
     */
    private void setupPhysicalVolumes() {

        if(_debug) System.out.printf("%s: setup the detector elements\n", getClass().getSimpleName());

        setupPhysicalVolumes(builder.getBaseTrackerGeometry());

        if(_debug) System.out.printf("%s: DONE setup the detector elements\n", getClass().getSimpleName());

    }


    /**
     * Setup the physical volumes recursively
     * @param surveyVolume - volume to process.
     */
    private void setupPhysicalVolumes(JavaSurveyVolume surveyVolume) {

        if(_debug) System.out.printf("%s: setupDetectorElement for %s\n", getClass().getSimpleName(),surveyVolume.getName());

        // Only certain objects are setup as detector elements
        // Ghost volumes are never setup

        // I do this recursively for daughters now as I wanted it to be generic
        // but for now since I know what the daughters are I could manually 
        // go and add:layer->module->sensor detector elements similar to 
        // the old setup. I wanted this to be more generic in case more structures 
        // are added so I keep track of all the elements in the builder in order 
        // to build a hierarchy.

        if( surveyVolume instanceof JavaGhostSurveyVolume) {

            if(_debug) System.out.printf("%s: %s  is a ghost volume, dont create elements or physvol\n", getClass().getSimpleName(),surveyVolume.getName());

        } else if(surveyVolume.getName().contains("tracking")) {
            if(_debug) System.out.printf("%s: %s  is the tracking volume, dont create elements or physvol\n", getClass().getSimpleName(),surveyVolume.getName());
        } else {

            // build the physical volume
            surveyVolume.buildPhysVolume();
            
            // create detector element
            // create detector element
            if(HPSTrackerBuilder.isBase(surveyVolume.getName())) {

                if(_debug) System.out.printf("%s: create the base detector element\n", getClass().getSimpleName());

                createBaseDetectorElement(surveyVolume);
                
                if(_debug) System.out.printf("%s: DONE create the base detector element\n", getClass().getSimpleName());


            } else if(HPSTrackerBuilder.isHalfModule(surveyVolume.getName())) {

                if(_debug) System.out.printf("%s: create the layer detector element\n", getClass().getSimpleName());

                IDetectorElement layerDe = createLayerDetectorElement(surveyVolume);
                
                if(_debug) System.out.printf("%s: DONE create the layer detector element\n", getClass().getSimpleName());

                if(_debug) System.out.printf("%s: create the module detector element\n", getClass().getSimpleName());
                
                createTrackerModuleDetectorElement(surveyVolume, layerDe);
                
                if(_debug) System.out.printf("%s: DONE create the module detector element\n", getClass().getSimpleName());


            } else if(HPSTrackerBuilder.isSensor(surveyVolume.getName())) {

                if(_debug) System.out.printf("%s: set sensitive volume for sensor %s\n", getClass().getSimpleName(),surveyVolume.getName());

                createSensorDetectorElement(surveyVolume);

                if(_debug) System.out.printf("%s: DONE set sensitive volume for sensor %s\n", getClass().getSimpleName(),surveyVolume.getName());


            } else if(HPSTrackerBuilder.isActiveSensor(surveyVolume.getName())) {
                
                if(_debug) System.out.printf("%s: create the active sensor detector element\n", getClass().getSimpleName());

                createActiveSensorDetectorElement(surveyVolume);
                
                if(_debug) System.out.printf("%s: DONE create the active sensor detector element\n", getClass().getSimpleName());

            } else {
                throw new RuntimeException("I don't think I should reach this? Should " + surveyVolume.getName() + " be a ghost?" );
            }

        }

        // add daughters
        if(_debug) System.out.printf("%s: add %d daughters to %s\n", this.getClass().getSimpleName(),surveyVolume.getDaughters().size(), surveyVolume.getName());
        for(JavaSurveyVolume daughter : surveyVolume.getDaughters()) {
            setupPhysicalVolumes(daughter);
        }

        if(_debug) System.out.printf("%s: DONE setup the detector element for %s\n", this.getClass().getSimpleName(),surveyVolume.getName());

    }

    
       
    
    /**
     * Find the module {@link DetectorElement} in a layer {@link DetectorElement} using the module number id.
     * @param layerDe
     * @param moduleNumber
     * @return the found {@link DetectorElement} or {@code null} if not found.
     */
    private IDetectorElement getModuleDetectorElement(IDetectorElement layerDe, int moduleNumber) {
      //Find the module by looping over the modules and checking module number
        IDetectorElement moduleDe = null;
        for(IDetectorElement e : layerDe.getChildren()) {
            if(e instanceof SiTrackerModule) {
                SiTrackerModule m = (SiTrackerModule)e;
                if(m.getModuleId()==moduleNumber) {
                    moduleDe = m;
                }
            }
        }
        return moduleDe;
    }

    /**
     * Abstract method to find the module number.
     * @param surveyVolume
     * @return module number
     */
    abstract protected int getModuleNumber(String surveyVolume);

    /**
     * Find the layer {@link DetectorElement}.
     * @param surveyVolume
     * @return the {@link DetectorElement}.
     */
    private IDetectorElement getLayerDetectorElement(JavaSurveyVolume surveyVolume) {
        // Helper
        IIdentifierDictionary iddict = builder.getDetectorIdentifierHelper().getIdentifierDictionary();

        // Find the mother: the module detector element
        IExpandedIdentifier layerExpId = new ExpandedIdentifier(iddict.getNumberOfFields());
        layerExpId.setValue(iddict.getFieldIndex("system"), builder.getSubdetector().getSystemID());
        layerExpId.setValue(iddict.getFieldIndex("barrel"), builder.getDetectorIdentifierHelper().getBarrelValue());                            
        //use the old definition of layer number to be consistent
        //int layer = HPSTestRunTracker2014Builder.getLayerFromVolumeName(geometryObject.getName());
        int layer = builder._builder.getOldGeomDefLayerFromVolumeName(surveyVolume.getName());
        layerExpId.setValue(iddict.getFieldIndex("layer"), layer);
        //Find the layer from the ID
        return builder.getLayerDetectorElement(layerExpId);
    }

    
    
    /**
     * Create the {@link HpsSiSensor} detector element.
     * @param surveyVolume
     */
    private void createActiveSensorDetectorElement(JavaSurveyVolume surveyVolume) {
     // Setup the active sensor element
        // to be consistent with old converter I also add the sensor 
        // in the path to the element even though it's not associated with 
        // with a element. I'm not sure why this is done.

        
        if(_debug) System.out.printf("%s: find the active sensor phys vol\n", this.getClass().getSimpleName());             

        // Find active Sensor physical volume.
        // Keep name consistent with old converter
        PhysicalVolume sensorPhysVol = (PhysicalVolume) surveyVolume.getPhysVolume();

        if(sensorPhysVol==null) throw new RuntimeException("cannot find physVol for " + surveyVolume.getName());

        if(_debug) System.out.printf("%s: found %s phys vol\n", this.getClass().getSimpleName(),sensorPhysVol.getName());               

        // find the layer and module detector element
        
        IDetectorElement layerDe = getLayerDetectorElement(surveyVolume);
        
        if(layerDe==null) throw new RuntimeException("Cannot find layer DE");

        //Find the module number
        int moduleNumber = getModuleNumber(surveyVolume.getName());
        
        //Find the module detector element
        IDetectorElement moduleDe = getModuleDetectorElement(layerDe, moduleNumber);
        
        if(moduleDe==null) throw new RuntimeException("Cannot find module DE for " + surveyVolume.getName());

        // Setup SiSensor's identifier.
        IIdentifierDictionary iddict = builder.getIdentifierDictionary();
        IExpandedIdentifier expId = new ExpandedIdentifier(iddict.getNumberOfFields());
        expId.setValue(iddict.getFieldIndex("system"), builder.getSubdetector().getSystemID());
        expId.setValue(iddict.getFieldIndex("barrel"), 0);                            
        expId.setValue(iddict.getFieldIndex("layer"), builder.getDetectorIdentifierHelper().getValue(layerDe.getIdentifier(), "layer"));
        expId.setValue(iddict.getFieldIndex("module"), ((SiTrackerModule) moduleDe).getModuleId());
        // The sensorNumber is always 0 in the old geometry. Keep it that way.
        int sensorNumber = 0;
        expId.setValue(iddict.getFieldIndex("sensor"), sensorNumber);
        

        // Packed identifier.
        IIdentifier sensorId = iddict.pack(expId);

        // Sensor paths.
        String modulePath = moduleDe.getGeometry().getPathString();
        IPhysicalVolume componentPhysVol = surveyVolume.getPhysMother().getPhysVolume();
        String sensorPath = modulePath.toString() + "/" + componentPhysVol.getName() + "/" + sensorPhysVol.getName();
        String sensorName = moduleDe.getName() + "_sensor" + sensorNumber;

        if(_debug) {
            System.out.printf("%s: create HpsSiSensor with old layer id %d with sensorNumber %d name %s moduleDe %s sensorPath %s sensor Id %d \n", getClass().getSimpleName(), 
                                layerDe.getIdentifier(),sensorNumber, sensorName, moduleDe.getName(), sensorPath, sensorNumber);
        }
        System.out.printf("%s: HpsSiSensor old layer id %d and module nr %d and sensor nr %d <-> DE name %s \n", getClass().getSimpleName(), 
                builder.getDetectorIdentifierHelper().getValue(layerDe.getIdentifier(), "layer"), ((SiTrackerModule) moduleDe).getModuleId(), sensorNumber,sensorName);
        
        // Create the sensor.
        int millepedeLayer = builder._builder.getMillepedeLayer(sensorName);
        HpsSiSensor sensor = createSiSensor(sensorNumber, sensorName, moduleDe, sensorPath, sensorId);
        sensor.setMillepedeId(millepedeLayer);
        //if(_debug) System.out.printf("%s: created sensor %s with id %d and expId %s \n", getClass().getSimpleName(), sensor.getName(), sensor.getIdentifier().getValue(), sensor.getExpandedIdentifier().toString());
        if(_debug) System.out.printf("%s: created sensor %s with layer %d and MP layer %d\n", getClass().getSimpleName(), sensor.getName(), sensor.getLayerNumber(),sensor.getMillepedeId());
    
        
    }
    
    
    
    /**
     * Set the sensor {@link PhysicalVolume} to be sensitive.
     * @param surveyVolume
     */
    private void createSensorDetectorElement(JavaSurveyVolume surveyVolume) {

        // set the physical volume to be sensitive
        // TODO this should go into the geometry definition?!
        ((PhysicalVolume)surveyVolume.getPhysVolume()).setSensitive(true);
        
    }

    /**
     * Create the {@link SiTrackerModule}.
     * @param surveyVolume
     * @param layerDe - mother {@link DetectorElement}
     */
    protected void createTrackerModuleDetectorElement(JavaSurveyVolume surveyVolume, IDetectorElement layerDe) {
        // create the "module" detector element 
        // it's under the base element
        
        int moduleNumber = getModuleNumber(surveyVolume.getName());
        
        String modulePlacementName = surveyVolume.getName();// builder.getSubdetector().getName() + "_" + moduleName + "_layer" + layer + "_module" + moduleNumber;
       
        // find the base DE as mother
        IDetectorElement baseDe = builder.getBaseDetectorElement();
        if(baseDe==null) {
            throw new RuntimeException("Base DE couldn't be found. Shouldn't happen!");
        } 
        
        // use base as mother for physical volume
        String modulePath = baseDe.getGeometry().getPathString() + "/" + modulePlacementName;

        if(_debug) {
            System.out.printf("%s: create SiTrackerModule with: placementname %s, modulePath %s, moduleNumber %d  \n", getClass().getSimpleName(),modulePlacementName, modulePath, moduleNumber);
        }
        
        SiTrackerModule moduleDe = new SiTrackerModule(modulePlacementName, layerDe, modulePath, moduleNumber);

        if(_debug) System.out.printf("%s: add module DE to existing ones  \n", getClass().getSimpleName(),modulePlacementName, modulePath, moduleNumber);


        //keep track of the module detector element
        builder.addModuleDetectorElement(moduleDe);
        
    }

    
    
    
    /**
     * Create the layer {@link DetectorElement}
     * @param surveyVolume
     * @return the detector element.
     */
    protected IDetectorElement createLayerDetectorElement(JavaSurveyVolume surveyVolume) {
        int nfields = builder.getDetectorIdentifierHelper().getIdentifierDictionary().getNumberOfFields();
        IExpandedIdentifier layerPosId = new ExpandedIdentifier(nfields);
        layerPosId.setValue(builder.getDetectorIdentifierHelper().getFieldIndex("system"), builder.getSubdetector().getSystemID());
        layerPosId.setValue(builder.getDetectorIdentifierHelper().getFieldIndex("barrel"), builder.getDetectorIdentifierHelper().getBarrelValue());
        //use the old definition of layer number to be consistent
        //int layer = HPSTestRunTracker2014Builder.getLayerFromVolumeName(geometryObject.getName());
        int layer = builder._builder.getOldGeomDefLayerFromVolumeName(surveyVolume.getName());
        layerPosId.setValue(builder.getDetectorIdentifierHelper().getFieldIndex("layer"), layer);
        if(_debug) System.out.printf("%s: layerPosId layer = %d (compare with new layer %d)\n", getClass().getSimpleName(),layer, HPSTrackerBuilder.getLayerFromVolumeName(surveyVolume.getName()));

        // find the base DE as mother
        IDetectorElement baseDe = builder.getBaseDetectorElement();
        if(baseDe==null) {
            throw new RuntimeException("Base DE couldn't be found. Shouldn't happen!");
        } 


        // create the layer detector element and keep track of it
        //IDetectorElement layerDe = builder.getLayerDetectorElement(layerPosId);
        IDetectorElement layerDe = builder.getLayerDetectorElement(layerPosId);

        if(layerDe==null) {
            //layerDe =  new DetectorElement(builder.getSubdetector().getName() + "_layer" + layer, builder.getSubdetector().getDetectorElement(), builder.getDetectorIdentifierHelper().pack(layerPosId));
            layerDe =  new DetectorElement(builder.getSubdetector().getName() + "_layer" + layer, baseDe, builder.getDetectorIdentifierHelper().pack(layerPosId));
            builder.addLayerDetectorElement(layerDe);
        } else {
            if(_debug) System.out.printf("%s: layerDE exists\n", getClass().getSimpleName());
        }

        if(_debug) System.out.printf("%s: created layerDE  %s  \n", getClass().getSimpleName(),layerDe.getName());
        
        return layerDe;
    }
    
    
    
    /**
     * Create the tracker base {@link DetectorElement}
     * @param surveyVolume
     */
    void createBaseDetectorElement(JavaSurveyVolume surveyVolume) {

        int nfields = builder.getDetectorIdentifierHelper().getIdentifierDictionary().getNumberOfFields();
        IExpandedIdentifier layerPosId = new ExpandedIdentifier(nfields);
        layerPosId.setValue(builder.getDetectorIdentifierHelper().getFieldIndex("system"), builder.getSubdetector().getSystemID());
        layerPosId.setValue(builder.getDetectorIdentifierHelper().getFieldIndex("barrel"), builder.getDetectorIdentifierHelper().getBarrelValue());
        int layer = 22; // dummy value
        layerPosId.setValue(builder.getDetectorIdentifierHelper().getFieldIndex("layer"), layer);
        IDetectorElement baseDe = builder.getBaseDetectorElement();
        if(baseDe!=null) {
            throw new RuntimeException("Base exists. Shouldn't happen!");
        } 
        ILogicalVolume trackingVolume = surveyVolume.getPhysMother().getVolume();
        if(!trackingVolume.getName().contains("tracking")) {
            throw new RuntimeException("base phys mother " + surveyVolume.getPhysMother().getName() + " is not tracking volume!?");
        }
        String physVolPath = trackingVolume.getName() + "/" + surveyVolume.getPhysVolume().getName();
        baseDe = new DetectorElement(builder.getSubdetector().getName() + "_base", builder.getSubdetector().getDetectorElement(), physVolPath, builder.getIdentifierDictionary().pack(layerPosId));
        builder.addBaseDetectorElement(baseDe);
        
        if(_debug) System.out.printf("%s: baseDE name %s  \n", getClass().getSimpleName(),baseDe.getName());
    }

    
    public IDetectorElement makeSubdetectorDetectorElement(Detector detector, Subdetector subdetector) {
        
        if(_debug) System.out.printf("%s: makeSubdetectorDetectorElement for subdetector %s\n", getClass().getSimpleName(),subdetector.getName());
        
        IDetectorElement subdetectorDE =
                new HpsTracker2(subdetector.getName(), detector.getDetectorElement());
        subdetector.setDetectorElement(subdetectorDE);
        return subdetectorDE;
    }
    
    private void printDEs() {
        System.out.printf("%s: Print all %d detector elements in store\n", getClass().getSimpleName(),DetectorElementStore.getInstance().size());
        for(IDetectorElement e : DetectorElementStore.getInstance()) {
            System.out.printf("%s: Name: %s \n", getClass().getSimpleName(),e.getName());
           /*
            if(e.getIdentifier()==null) {
                System.out.printf("%s: no id found\n", getClass().getSimpleName());
            } else {
                if(e.getExpandedIdentifier()==null) 
                    System.out.printf("%s: no exp id found\n", getClass().getSimpleName());
                else 
                    System.out.printf("%s: %s \n", getClass().getSimpleName(),e.getExpandedIdentifier().toString());
            } 
            */
            if(e.hasGeometryInfo()) {
                System.out.printf("%s: Position: %s \n", getClass().getSimpleName(),e.getGeometry().getPosition());
                System.out.printf("%s: LocalToGlobal: \n%s \n", getClass().getSimpleName(),((Transform3D)e.getGeometry().getLocalToGlobal()).toString());
                //System.out.printf("%s: GlobalToLocal: \n%s \n", getClass().getSimpleName(),((Transform3D)e.getGeometry().getGlobalToLocal()).toString());
                IGeometryInfo info = e.getGeometry();
                if(info!=null) {
                    while((info=info.parentGeometry())!=null) {
                        System.out.printf("%s: Parent geometry DE: %s \n", getClass().getSimpleName(),info.getDetectorElement().getName());
                        System.out.printf("%s: Parent Position: %s \n", getClass().getSimpleName(),info.getPosition());
                        System.out.printf("%s: Parent LocalToGlobal: \n%s \n", getClass().getSimpleName(),((Transform3D)info.getLocalToGlobal()).toString());

                    }
                }
            }
        }
    }
    

}