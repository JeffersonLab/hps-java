package org.lcsim.geometry.compact.converter.lcdd;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import java.util.Iterator;

import org.jdom.DataConversionException;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.lcsim.detector.Transform3D;
import org.lcsim.geometry.compact.converter.HPSTrackerBuilder;
import org.lcsim.geometry.compact.converter.HPSTrackerLCDDBuilder;
import org.lcsim.geometry.compact.converter.LCDDGhostSurveyVolume;
import org.lcsim.geometry.compact.converter.LCDDSurveyVolume;
import org.lcsim.geometry.compact.converter.SurveyCoordinateSystem;
import org.lcsim.geometry.compact.converter.lcdd.util.Box;
import org.lcsim.geometry.compact.converter.lcdd.util.LCDD;
import org.lcsim.geometry.compact.converter.lcdd.util.PhysVol;
import org.lcsim.geometry.compact.converter.lcdd.util.Position;
import org.lcsim.geometry.compact.converter.lcdd.util.Rotation;
import org.lcsim.geometry.compact.converter.lcdd.util.SensitiveDetector;
import org.lcsim.geometry.compact.converter.lcdd.util.Volume;
import org.lcsim.geometry.util.TransformationUtils;

/**
 * 
 * Convert an HPS tracker "2014" to the LCDD format.
 * 
 * @author Per Hansson <phansson@slac.stanford.edu>
 *
 */
public abstract class HPSTracker2014Base extends LCDDSubdetector {

    protected boolean _debug = false;
    protected static HPSTrackerLCDDBuilder builder;
    private final boolean buildBeamPlane = false;
    private final double beamPlaneWidth = 385.00;
    private final double beamPlaneLength = 1216.00;
    private final double beamPlaneThickness = 0.00000001;

    public HPSTracker2014Base(Element c) throws JDOMException {
        super(c);
    }
    
    
    /**
     * Set the @HPSTrackerLCDDBuilder for this converter.
     * @param lcdd
     * @param sens
     * @return the builder.
     */
    abstract protected HPSTrackerLCDDBuilder initializeBuilder(LCDD lcdd, SensitiveDetector sens);
    
    
    public boolean isTracker() {
        return true;
    }
    
    /**
     * Build the LCDD for the subdetector.
     * @param lcdd - the LCDD file being created.
     * @param sens - the SD for this subdetector.
     */
    
    public void addToLCDD(LCDD lcdd, SensitiveDetector sens) throws JDOMException
    {
        
        
        
        /// General comments
        // Roll: rotation around x
        // pitch: rotation around y
        // yaw: rotation around z
        
        // kinematic mounts:
        // ball (constraints x,y,z)
        // vee  (constraints pitch & yaw)
        // flat (constraints roll)
        ///
        
        
        
        // ID of the detector.
        int id = node.getAttribute("id").getIntValue();

        // Name of the detector.
        String detector_name = node.getAttributeValue("name");

        if(_debug) System.out.printf("%s: detector id %d name %s",getClass().getSimpleName(), id,detector_name);
        
        
        // Pick the mother volume (tracking volume).
        Volume trackingVolume = lcdd.pickMotherVolume(this);

        
        if(_debug) System.out.printf("%s: setup and build the LCDD geometry\n", getClass().getSimpleName());

        // setup and build the LCDD geometry
        builder = initializeBuilder(lcdd, sens);
        //builder = new HPSTestRunTracker2014LCDDBuilder(_debug,node,lcdd,sens);
        
        //builder.setLCDD(lcdd);
        //builder.setSensitiveDetector(sens);
        builder.build(trackingVolume);
        
        if(_debug) System.out.printf("%s: DONE setup and build the LCDD geometry\n", getClass().getSimpleName());
        
   

        if(buildBeamPlane ) {
           makeBeamPlane(trackingVolume, lcdd, sens);
        }
        
        
        // Actually build the LCDD
        setupPhysicalVolumes();
        
        
    }
    

    
    
    /**
     *  Top function to add objects to the LCDD file using the geometry builder class. 
     */
    protected void setupPhysicalVolumes() {
        
        if(_debug) System.out.printf("%s: buildLCDD\n", getClass().getSimpleName());
        
        // Get a reference to the LCDD
        LCDD lcdd  = builder.getLCDD();
        SensitiveDetector sd  = builder.getSensitiveDetector();

        // Reference to the top level object in the builder class
        // In this case it is the base volume holding the entire tracker
        LCDDSurveyVolume lcddObj  = (LCDDSurveyVolume) builder.getBaseLCDD();
        
        // Add the base volume and all its daughters to the LCDD
        setupPhysicalVolumes(lcddObj,lcdd,sd);
        
        if(_debug) System.out.printf("%s: buildLCDD DONE\n", getClass().getSimpleName());
        
    }
    
   

    /**
     * Add a @LCDDBaseGeom geometry object to the LCDD file.
     * @param lcddObj to add
     * @param lcdd file
     */
    private void setupPhysicalVolumes(LCDDSurveyVolume lcddObj, LCDD lcdd, SensitiveDetector sd) {
        
        if(_debug) System.out.printf("%s: adding %s to LCDD\n", getClass().getSimpleName(),lcddObj.getName());

        boolean validLCDD = true;
        
        if(lcddObj instanceof LCDDGhostSurveyVolume) {
        
            if(_debug) System.out.printf("%s: %s is a ghost volume, don't add to LCDD\n", getClass().getSimpleName(),lcddObj.getName());
            validLCDD = false;
            
        } else {

            // Special case for top level volume which is a non-ghost but is already there.
            if(lcddObj.getName().contains("tracking")) {
            
                if(_debug) System.out.printf("%s: %s is the tracking volume, don't add to LCDD\n", getClass().getSimpleName(),lcddObj.getName());
                validLCDD = false;
                    
            } else {

                //X-check
                if(lcddObj instanceof LCDDGhostSurveyVolume )
                    throw new RuntimeException("trying to add a ghost volume (" + lcddObj.getName() + ") to LCDD!?");
                
                // add box, pos, rotation and create phys volume
                lcdd.add(lcddObj.getBox());
                lcdd.add(lcddObj.getPos());
                lcdd.add(lcddObj.getRot());
                lcddObj.buildPhysVolume();

                // setup the properties of the phys volume
                try {
                    setPhysicalVolumeProperties(lcddObj, sd);
                } catch (DataConversionException e) {
                    e.printStackTrace();
                }
            }
        }
        
        // add daughters to this volume
        
        if(_debug) System.out.printf("%s: add %d daughters to %s\n", getClass().getSimpleName(),lcddObj.getDaughters().size(),lcddObj.getName());

        for(LCDDSurveyVolume daughter : lcddObj.getDaughters()) {
            setupPhysicalVolumes(daughter,lcdd, sd);
        }
        
        // finally add volume
        if(validLCDD) {
            if(_debug) 
                System.out.printf("%s: adding volume %s\n", getClass().getSimpleName(),lcddObj.getName());
            if(!lcddObj.getVisName().isEmpty()) {
              if(_debug) 
                System.out.printf("%s: set vis %s for volume %s\n", getClass().getSimpleName(), lcddObj.getVisName(),lcddObj.getName());
                lcddObj.getVolume().setVisAttributes(lcdd.getVisAttributes(lcddObj.getVisName()));
              if(_debug) 
                if(lcdd.getVisAttributes(lcddObj.getVisName())!=null) {
                    System.out.printf("%s: found vis %s\n", getClass().getSimpleName(), lcdd.getVisAttributes(lcddObj.getVisName()).getAttributeValue("name"));
                } else {
                    System.out.printf("%s: vis not found\n", getClass().getSimpleName());
                }
            }
            lcdd.add(lcddObj.getVolume());
        } else {
            if(_debug) System.out.printf("%s: don't add volume %s\n", getClass().getSimpleName(),lcddObj.getName());
        }
        if(_debug) System.out.printf("%s: DONE adding %s\n", getClass().getSimpleName(),lcddObj.getName());
    }
    
    
    /**
     * Set properties of the physical volume.
     * @param surveyVolume
     */
    private void setPhysicalVolumeProperties(LCDDSurveyVolume surveyVolume, SensitiveDetector sd) throws DataConversionException {
        
        if(_debug) System.out.printf("%s: setPhysVolumeProperties for name %s\n", getClass().getSimpleName(),surveyVolume.getName());
        
        String name = surveyVolume.getName();
        if(HPSTrackerBuilder.isHalfModule(surveyVolume.getName())) {
            setHalfModulePhysicalVolumeProperties(surveyVolume);
        }
        else if(HPSTrackerBuilder.isActiveSensor(surveyVolume.getName())) {
            setActiveSensorPhysicalVolumeProperties(surveyVolume, sd);
        }
        else if(HPSTrackerBuilder.isSensor(surveyVolume.getName())) {
            setSensorPhysicalVolumeProperties(surveyVolume);
        }
        else if(name.endsWith("lamination")) {
            surveyVolume.getPhysVolume().addPhysVolID("component", 2);
        }
        else if(name.endsWith("cf")) {
            surveyVolume.getPhysVolume().addPhysVolID("component", 1);
        }
        else if(name.endsWith("hybrid")) {
            surveyVolume.getPhysVolume().addPhysVolID("component", 3);
        }

        if(_debug) {
            System.out.printf("%s: %d physvolid's\n", getClass().getSimpleName(),surveyVolume.getPhysVolume().getChildren("physvolid").size());
            //geomObj.getPhysVolume().getChildren("physvolid field_name="sensor" value="0"")
            for (Iterator i = surveyVolume.getPhysVolume().getChildren("physvolid").iterator(); i.hasNext();) {        
                Element e = (Element)i.next();
                System.out.printf("%s: %s %d\n", getClass().getSimpleName(),e.getAttributeValue("field_name"),e.getAttribute("value").getIntValue());
            }
        
        if(_debug) System.out.printf("%s: DONE setPhysVolumeProperties for name %s\n", getClass().getSimpleName(),surveyVolume.getName());
            
        }

        
    }

    private void setSensorPhysicalVolumeProperties(LCDDSurveyVolume surveyVolume) {
            surveyVolume.getPhysVolume().addPhysVolID("component", 0);
    }


    private void setActiveSensorPhysicalVolumeProperties(LCDDSurveyVolume surveyVolume, SensitiveDetector sd) {
        surveyVolume.getPhysVolume().addPhysVolID("sensor", 0);
        surveyVolume.getVolume().setSensitiveDetector(sd);
    }

    abstract protected int getModuleNumber(String surveyVolume);
        
   
    
    
    private void setHalfModulePhysicalVolumeProperties(LCDDSurveyVolume surveyVolume) throws DataConversionException {
            PhysVol physVol = surveyVolume.getPhysVolume();
            int sysId = node.getAttribute("id").getIntValue();
            
            //use the old definition of layer number to be consistent
            int layer = builder._builder.getOldGeomDefLayerFromVolumeName(surveyVolume.getName());
            if(_debug) System.out.printf("%s: physVolId layer = %d (compare with new layer %d)\n", getClass().getSimpleName(),layer, HPSTrackerBuilder.getLayerFromVolumeName(surveyVolume.getName()));
            
            //Find the module number
            int moduleNumber = getModuleNumber(surveyVolume.getName());
            
            physVol.addPhysVolID("system", sysId);
            physVol.addPhysVolID("barrel", 0);
            surveyVolume.getPhysVolume().addPhysVolID("layer", layer);
            surveyVolume.getPhysVolume().addPhysVolID("module", moduleNumber);
            

    }


    protected void makeBeamPlane(Volume motherVolume,  LCDD lcdd, SensitiveDetector sens) throws JDOMException {
        Hep3Vector ball_pos_beamplane = new BasicHep3Vector(-1.0*beamPlaneWidth/2.0,0.0,beamPlaneLength/2.0);
        Hep3Vector vee_pos_beamplane = new BasicHep3Vector(ball_pos_beamplane.x()+beamPlaneWidth,ball_pos_beamplane.y(),ball_pos_beamplane.z());
        Hep3Vector flat_pos_beamplane = new BasicHep3Vector(ball_pos_beamplane.x(), ball_pos_beamplane.y(), ball_pos_beamplane.z()-beamPlaneLength/2.0);
        makeBeamPlane(motherVolume, ball_pos_beamplane, vee_pos_beamplane, flat_pos_beamplane, lcdd, sens);
    }

    
    
    protected void makeBeamPlane(Volume motherVolume, Hep3Vector ball_pos_base_plate, Hep3Vector vee_pos_base_plate,Hep3Vector flat_pos_base_plate, LCDD lcdd, SensitiveDetector sens) throws JDOMException {
    
    
    if(_debug) {
        System.out.println("--- makeBeamPlane ----");
        
    }
    
    // create the coordinate system of the beam plane in the tracking volume
    // since this is a dummy volume it is based on the position of the base plate coordinate system
    // width - u
    // length - v
    // thickness - w
    Hep3Vector ball_pos_beamplane = ball_pos_base_plate;
    Hep3Vector vee_pos_beamplane = vee_pos_base_plate; 
    Hep3Vector flat_pos_beamplane = flat_pos_base_plate;        
    SurveyCoordinateSystem beamplane_coord = new SurveyCoordinateSystem(ball_pos_beamplane, vee_pos_beamplane, flat_pos_beamplane);     
    Transform3D trans_beamplane_to_tracking = beamplane_coord.getTransformation();
    
    String volName = "beamPlaneVol";
    //Box box = new Box(volName + "Box", HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width, HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length*2, beamPlaneThickness);
    Box box = new Box(volName + "Box", beamPlaneWidth, beamPlaneLength*2, beamPlaneThickness);
    lcdd.add(box);
    Volume volume = new Volume(volName + "_volume", box, lcdd.getMaterial("Vacuum"));
    
    
    if(_debug) {
        System.out.println(String.format("ball_pos_beamplane %s", ball_pos_beamplane.toString()));
        System.out.println(String.format("vee_pos_beamplane %s", vee_pos_beamplane.toString()));
        System.out.println(String.format("flat_pos_beamplane %s", flat_pos_beamplane.toString()));
        System.out.println(String.format("beamplane_coord:\n%s", beamplane_coord.toString()));
    }

    
    // Find distance to center in the local coordinate system 
    Hep3Vector box_center_base_local = new BasicHep3Vector(beamPlaneWidth/2.0, beamPlaneLength/2.0, beamPlaneThickness/2.0);
    
    //translate to the mother coordinate system
    Hep3Vector box_center_base = trans_beamplane_to_tracking.transformed(box_center_base_local);
    
    if(_debug) {
        System.out.println(String.format("box_center_base_local  %s", box_center_base_local.toString()));
        System.out.println(String.format("box_center_base        %s", box_center_base.toString()));
    }
    
    // Create the LCDD position
    Position pos = new Position(volName + "_position",box_center_base.x(), box_center_base.y(), box_center_base.z());
    
    //Find LCDD Euler rotation angles from coordinate system unit vectors
    Hep3Vector lcdd_rot_angles = TransformationUtils.getCardanAngles(beamplane_coord.v(), beamplane_coord.w(), new BasicHep3Vector(0,1,0),new BasicHep3Vector(0,0,1));
    Rotation rot = new Rotation(volName + "_rotation",lcdd_rot_angles.x(), lcdd_rot_angles.y(), lcdd_rot_angles.z());
    lcdd.add(pos);
    lcdd.add(rot);
    
    // Create the physical volume
    PhysVol basePV = new PhysVol(volume, motherVolume, pos, rot);
    if(_debug) {
         System.out.println("Created physical vomume " + basePV.getName());
     }
    
     volume.setVisAttributes(lcdd.getVisAttributes("BeamPlaneVis"));

     lcdd.add(volume);
    
    }
    
    
   
    
    
    
}