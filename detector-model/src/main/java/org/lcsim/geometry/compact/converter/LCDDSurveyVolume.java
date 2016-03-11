package org.lcsim.geometry.compact.converter;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jdom.JDOMException;
import org.lcsim.detector.Transform3D;
import org.lcsim.geometry.compact.converter.lcdd.util.Box;
import org.lcsim.geometry.compact.converter.lcdd.util.LCDD;
import org.lcsim.geometry.compact.converter.lcdd.util.Material;
import org.lcsim.geometry.compact.converter.lcdd.util.PhysVol;
import org.lcsim.geometry.compact.converter.lcdd.util.Position;
import org.lcsim.geometry.compact.converter.lcdd.util.Rotation;
import org.lcsim.geometry.compact.converter.lcdd.util.Volume;
import org.lcsim.geometry.util.TransformationUtils;

/**
 * Interface to the LCDD converter geometry for the geometry definition. 
 *   
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 */
public class LCDDSurveyVolume extends SurveyVolumeImpl  {
    Box box= null;
    Volume volume = null;
    private Position pos = null;
    private Rotation rot = null;
    private PhysVol physVolume = null;
    LCDD lcdd = null;
    private LCDDSurveyVolume mother = null;
    protected Map<String,Integer> physVolId = null;
    public List<LCDDSurveyVolume> daughters = new ArrayList<LCDDSurveyVolume>();
    /**
     *  Default constructor
     *  @param surveyVolume - core geometry definitions
     */
    public LCDDSurveyVolume(SurveyVolume surveyVolume) {
       super(surveyVolume);
    }
    
    /**
     * Initialize this object with a known volume and no mother. Typically the world volume would use this.
     * @param surveyVolume - core geometry definitions
     * @param volume - given volume
     */
    public LCDDSurveyVolume(SurveyVolume surveyVolume, Volume volume) {
        super(surveyVolume);
        if(isDebug()) System.out.printf("%s: constructing LCDD object %s with volume name %s\n", this.getClass().getSimpleName(),surveyVolume.getName(),volume.getName());
        setVolume(volume);
        if(isDebug()) System.out.printf("%s: DONE constructing LCDD object %s\n", this.getClass().getSimpleName(),surveyVolume.getName());
        Hep3Vector lcdd_rot_angles = TransformationUtils.getCardanAngles(surveyVolume.getCoord().v(), surveyVolume.getCoord().w(), new BasicHep3Vector(0,1,0),new BasicHep3Vector(0,0,1));
        setPos(new Position(getName() + "_position", 0, 0, 0));
        setRot(new Rotation(getName() + "_rotation",lcdd_rot_angles.x(), lcdd_rot_angles.y(), lcdd_rot_angles.z()));
        if(isDebug()) System.out.printf("%s: DONE  %s\n", this.getClass().getSimpleName(),surveyVolume.getName());
    }
    
    /**
     * Interface to the LCDD converter geometry for the geometry definition. 
     * @param surveyVolume - core geometry definition
     * @param lcdd - lcdd file 
     * @param mother - reference to mother LCDD definition
     */
    public LCDDSurveyVolume(SurveyVolume surveyVolume, LCDD lcdd, LCDDSurveyVolume mother) {
        super(surveyVolume);
        if(isDebug()) System.out.printf("%s: constructing LCDD object %s with mother %s\n", this.getClass().getSimpleName(),surveyVolume.getName(),mother==null?"null":mother.getName());
        this.lcdd = lcdd;
        setMother(mother);
        mother.addDaughter(this);
        buildBox();
        buildVolume();
        setPositionAndRotation(surveyVolume);
        //buildPhysVolume(mother);
        if(isDebug()) System.out.printf("%s: DONE constructing LCDD object %s\n", this.getClass().getSimpleName(),surveyVolume.getName());
    }

    
    public void buildPhysVolume() {

        if(isDebug()) System.out.printf("%s: build phys volume for %s with mother %s and physical mother %s\n", this.getClass().getSimpleName(),getName(),getMother().getName(),getPhysMother().getName());
        LCDDSurveyVolume physMother =  getPhysMother();
        setPhysVolume(new PhysVol(volume, physMother.getVolume(), getPos(), getRot()));
        //if(isDebug()) System.out.printf("%s: build phys volume for %s\n", this.getClass().getSimpleName(),getName());
        //setPhysVolume(new PhysVol(volume, getMother().getVolume(), getPos(), getRot()));
    }
    public void buildBox() {
        if(isDebug()) System.out.printf("%s: build box for %s\n", getClass().getSimpleName(),getName());
        setBox(new Box(getName() + "Box", getBoxDim().x(), getBoxDim().y(), getBoxDim().z())); 
    }
    public void buildVolume() {
        if(isDebug()) System.out.printf("%s: build volume for %s with material %s\n", this.getClass().getSimpleName(),getName(),getMaterial());
        try {
            Material mat = lcdd.getMaterial(getMaterial());
            setVolume(new Volume(getName() + "_volume", box, mat));
        } catch (JDOMException e) {
            e.printStackTrace();
        }
    }
    
    
    public void setPositionAndRotation(SurveyVolume base) {
        if(isDebug()) System.out.printf("%s: set position and rotation for volume %s\n", this.getClass().getSimpleName(),getName());
        
        // NOTE:
        // This sets position and reference w.r.t. mother coordinate system. 
        // If I'm not building that volume this will be wrong. 
        // TODO Similar to in the JAVA converter this should be something like the physical mother.
        
        if(base.getMother()==null) throw new RuntimeException("trying to set coordinates w/o mother defined for "+base.getName());
        
        // Vector from origin to center of box locally 
        Hep3Vector box_center_base_local = base.getCenter();
        
        //translate to the mother coordinate system
        LCDDSurveyVolume physMother = getPhysMother();
        if(isDebug()) System.out.printf("%s: physical mother to transform to is %s; find the transform to it\n", this.getClass().getSimpleName(),physMother.getName());
        Transform3D trf = HPSTrackerBuilder.getTransform(base.getCoord().getTransformation(),base.getMother(),physMother.getName()); 
        if(isDebug()) System.out.printf("%s: found transform to physical mother \n%s\n\n", this.getClass().getSimpleName(),trf.toString());
        
        // find the position of the center in the physical mother coord
        Hep3Vector box_center_base = trf.transformed(box_center_base_local);
        
        // find the position of the center of the box in the mother coordinate system, make sure to use the physical mother coordinates
        if(isDebug()) System.out.printf("%s: find center of box in physical mother coord %s \n", this.getClass().getSimpleName(),physMother.getName());
        // hack since my getTransform function needs a mother TODO Fix this!
        SurveyVolume gm = base;
        if(isDebug()) System.out.printf("%s: look for physical mother %s starting from mother %s \n", this.getClass().getSimpleName(),physMother.getName(),gm.getMother()!=null?gm.getMother().getName():"-- no mother --");
        while((gm=gm.getMother()).getName()!=physMother.getName()) {
            if(isDebug()) System.out.printf("%s: gm is %s \n", this.getClass().getSimpleName(),gm.getName());
            //gm = gm.getMother();
        }
        if(isDebug()) System.out.printf("%s: found physical mother %s with center at %s \n", this.getClass().getSimpleName(),gm.getName(), gm.getCenter());

        Hep3Vector mother_center = gm.getCenter();
        
        // find the position of the center in the mother coord
        Hep3Vector box_center = VecOp.sub(box_center_base, mother_center);
        
        //Find LCDD Euler rotation angles from coordinate system unit vectors
        //Note that this has to be rotation wrt to physical mother and not just mother as normally is the case
        if(isDebug()) System.out.printf("%s: find LCDD Cardan rotation angles - need to find mother to physical mother transform \n", this.getClass().getSimpleName(),physMother.getName());
        Hep3Vector base_u = base.getCoord().u();
        Hep3Vector base_v = base.getCoord().v();
        Hep3Vector base_w = base.getCoord().w();
        if(isDebug()) System.out.printf("%s: unit vectors in mother coord: %s, %s, %s\n", this.getClass().getSimpleName(),base_u.toString(),base_v.toString(),base_w.toString());
        Hep3Vector unit_u = new BasicHep3Vector(1,0,0);
        Hep3Vector unit_v = new BasicHep3Vector(0,1,0);
        Hep3Vector unit_w = new BasicHep3Vector(0,0,1);
        if(!base.getMother().getName().equals(physMother.getName())) {
            if(isDebug()) System.out.printf("%s: Need to get unit vectors in physical mother %s coord system\n", this.getClass().getSimpleName(),physMother.getName());
            Transform3D trf_mother = HPSTrackerBuilder.getTransform(base.getMother().getCoord().getTransformation(),base.getMother().getMother(),physMother.getName()); 
            if(isDebug()) System.out.printf("%s: found transform from mother to physical mother \n%s\n", this.getClass().getSimpleName(),trf_mother.toString());
            //unit_u = VecOp.unit(trf_mother.rotated(unit_u));
            //unit_v = VecOp.unit(trf_mother.rotated(unit_v));
            //unit_w = VecOp.unit(trf_mother.rotated(unit_w));
            base_u = VecOp.unit(trf_mother.rotated(base_u));
            base_v = VecOp.unit(trf_mother.rotated(base_v));
            base_w = VecOp.unit(trf_mother.rotated(base_w));
            
        } else {
            if(isDebug()) System.out.printf("%s: mother and physical mother is the same so unit vectors didn't change\n",getClass().getSimpleName());
        }
        
        if(isDebug()) {
            if(isDebug()) System.out.printf("%s: final unit vectors to get Cardan angles from : \n%s, %s, %s -> %s, %s, %s \n", 
                                            this.getClass().getSimpleName(),
                                            base_u.toString(),base_v.toString(),base_w.toString(),              
                                            unit_u.toString(),unit_v.toString(),unit_w.toString());
            //System.out.printf("%s: unit vectors u %s v %s w %s\n", this.getClass().getSimpleName(),base.getCoord().u().toString(),base.getCoord().v().toString(),base.getCoord().w().toString());
        }
        Hep3Vector lcdd_rot_angles = TransformationUtils.getCardanAngles(base_u, base_v, base_w, unit_u, unit_v, unit_w);
        
        
        // Create the LCDD position and rotation
        setPos(new Position(getName() + "_position",box_center.x(), box_center.y(), box_center.z()));
        setRot(new Rotation(getName() + "_rotation",lcdd_rot_angles.x(), lcdd_rot_angles.y(), lcdd_rot_angles.z()));
        
        if(isDebug()) {
            System.out.printf("%s: SurveyVolume information for %s:\n", this.getClass().getSimpleName(), base.getName());
            System.out.printf("%s: box_center_base_local  %s\n", this.getClass().getSimpleName(), box_center_base_local.toString());
            System.out.printf("%s: box_center_base        %s\n", this.getClass().getSimpleName(), box_center_base.toString());
            System.out.printf("%s: mother center          %s\n", this.getClass().getSimpleName(), mother_center.toString());
            System.out.printf("%s: box_center             %s\n", this.getClass().getSimpleName(), box_center.toString());
            System.out.printf("%s: pos                    %s\n", this.getClass().getSimpleName(), getPos().toString());
            System.out.printf("%s: euler                  %s\n", this.getClass().getSimpleName(), lcdd_rot_angles.toString());
            System.out.printf("%s: rot                    %s\n", this.getClass().getSimpleName(), getRot().toString());
            
            //calculate the position in tracking volume separately as a xcheck
            Hep3Vector box_center_tracking_xcheck = HPSTrackerBuilder.transformToTracking(box_center_base_local, base);
            System.out.printf("%s: box_center_tracking_xcheck  %s (for %s)\n", this.getClass().getSimpleName(), box_center_tracking_xcheck.toString(), base.getName());
        }
        
    }
    /**
     * Find the first non-ghost volume among parents.  
     * @return mother object
     */
    public LCDDSurveyVolume getPhysMother() {
        //if(isDebug()) System.out.printf("%s: finding physical mother to %s\n", this.getClass().getSimpleName(), getName());
        if(mother==null) throw new RuntimeException("Trying to get phys mother but there is no mother!");
        if(mother instanceof LCDDGhostSurveyVolume) {
            return mother.getPhysMother();
        } else {
            //if(isDebug()) System.out.printf("%s: found a non-ghost volume: %s\n", this.getClass().getSimpleName(), mother.getName());
            return mother;
        }
    }
    
    public Volume getVolume() {
        return volume;
    }
    public void setVolume(Volume volume) {
        this.volume = volume;
    }
    public Box getBox() {
        return box;
    }
    public void setBox(Box b) {
        box = b;
    }   
    public Position getPos() {
        return pos;
    }
    public void setPos(Position pos) {
        this.pos = pos;
    }
    public Rotation getRot() {
        return rot;
    }
    public void setRot(Rotation rot) {
        this.rot = rot;
    }
    public LCDDSurveyVolume getMother() {
        return mother;
    }
    public void setMother(LCDDSurveyVolume mother) {
        this.mother = mother;
    }
    public PhysVol getPhysVolume() {
        return physVolume;
    }
    public void setPhysVolume(PhysVol physVolume) {
        this.physVolume = physVolume;
    }
    public List<LCDDSurveyVolume> getDaughters() {
        return daughters;
    }
    public void addDaughter(LCDDSurveyVolume o) {
        getDaughters().add(o);
    }
     public String toString() {
        String s = getClass().getSimpleName() +": " + getName() + "\n";
        if(getPos()!=null && getRot()!=null)    {
            double x = Double.valueOf(getPos().getAttributeValue("x"));
            double y = Double.valueOf(getPos().getAttributeValue("y"));
            double z = Double.valueOf(getPos().getAttributeValue("z"));
            s += "Position: " + String.format("(%.4f %.4f %.4f)\n", x,y,z);
            x = Double.valueOf(getRot().getAttributeValue("x"));
            y = Double.valueOf(getRot().getAttributeValue("y"));
            z = Double.valueOf(getRot().getAttributeValue("z"));
            s += "Rotation: " + String.format("(%.4f %.4f %.4f)\n", x,y,z);
        } else {
            s += " - no position/rotation info -\n";
        }
        return s;
    }
}