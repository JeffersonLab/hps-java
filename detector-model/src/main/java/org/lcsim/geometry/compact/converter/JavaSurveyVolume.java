package org.lcsim.geometry.compact.converter;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.detector.ILogicalVolume;
import org.lcsim.detector.IPhysicalVolume;
import org.lcsim.detector.IRotation3D;
import org.lcsim.detector.ITranslation3D;
import org.lcsim.detector.LogicalVolume;
import org.lcsim.detector.PhysicalVolume;
import org.lcsim.detector.RotationGeant;
import org.lcsim.detector.Transform3D;
import org.lcsim.detector.Translation3D;
import org.lcsim.detector.material.IMaterial;
import org.lcsim.detector.material.MaterialStore;
import org.lcsim.detector.solids.Box;
import org.lcsim.geometry.util.TransformationUtils;

/**
 *  Interface to the JAVA converter geometry for the geometry definition.  
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 */
public class JavaSurveyVolume extends SurveyVolumeImpl {
	private Box box= null;
	private ILogicalVolume volume = null;
	private ITranslation3D pos = null;
	private IRotation3D rot = null;
	private IPhysicalVolume physVolume = null;
	private JavaSurveyVolume mother = null;
	public List<JavaSurveyVolume> daughters = new ArrayList<JavaSurveyVolume>();
	private int componentId = -1;
	
	/**
	 *  Default constructor
	 */
	public JavaSurveyVolume(SurveyVolume surveyVolume) {
	    super(surveyVolume);
	}

	/**
	 * Construct a JAVA geometry object from its geometry definition and an already built logical volume. 
	 * This is typically used by the tracking volume.
	 * @param surveyVolume - input geometry definition
	 * @param vol - logical volume
	 */
	public JavaSurveyVolume(SurveyVolume surveyVolume, ILogicalVolume vol) {
	    super(surveyVolume);
		if(isDebug()) System.out.printf("%s: JavaBaseGeometry %s (given logical volume %s)\n", this.getClass().getSimpleName(),surveyVolume.getName(),vol.getName());
		// this must be tracking volume. May change in the future and is probably weird to make this requirement here. 
		if(!surveyVolume.getName().contains("tracking")) throw new RuntimeException("this constructor is only used with the tracking volume!?");
		setVolume(vol);
		// since it's tracking volume, set the pos and rotation trivially
		Hep3Vector lcdd_rot_angles = TransformationUtils.getCardanAngles(surveyVolume.getCoord().v(), surveyVolume.getCoord().w(), new BasicHep3Vector(0,1,0),new BasicHep3Vector(0,0,1));
		setPos(new Translation3D(0,0,0));
		setRot(new RotationGeant(lcdd_rot_angles.x(), lcdd_rot_angles.y(), lcdd_rot_angles.z()));
		if(isDebug()) System.out.printf("%s: DONE JavaBaseGeometry %s\n", this.getClass().getSimpleName(),surveyVolume.getName());
	}
	
	/**
	 * Construct a JAVA geometry object from its geometry definition. 
	 * @param surveyVolume - input geometry definition
	 * @param mother - reference to mother JAVA definition
	 * @param volumeId - component id number 
	 */
	public JavaSurveyVolume(SurveyVolume surveyVolume, JavaSurveyVolume mother, int volumeId) {
	    super(surveyVolume);
        if(isDebug()) System.out.printf("%s: JavaBaseGeometry %s (volumeID %d, mother %s)\n", this.getClass().getSimpleName(),surveyVolume.getName(),volumeId,mother==null?"null":mother.getName());
		setComponentId(volumeId);
		setMother(mother);
		mother.addDaughter(this);
		buildBox();
		buildVolume();
		setPositionAndRotation(surveyVolume);
		if(isDebug()) System.out.printf("%s: DONE JavaBaseGeometry %s\n", this.getClass().getSimpleName(),surveyVolume.getName());
	}

	protected boolean hasCoordinateSystemInfo() {
		return pos!=null && rot!=null;
	}
	
	
	public void buildPhysVolume() {
		if(isDebug()) System.out.printf("%s: build phys volume for %s with mother %s and physical mother %s\n", this.getClass().getSimpleName(),getName(),getMother().getName(),getPhysMother().getName());
		JavaSurveyVolume physMother =  getPhysMother();
		setPhysVolume(new PhysicalVolume(new Transform3D(getPos(), getRot()), getName(), volume, physMother.getVolume(),getComponentId()));
	}
	
	public void buildBox() {
		Hep3Vector b = VecOp.mult(0.5,getBoxDim());
		if(isDebug()) System.out.printf("%s: build box for %s with dimensions %s \n", this.getClass().getSimpleName(),getName(), b);
		setBox(new Box(getName() + "Box", b.x(), b.y(), b.z())); 
	}
	public void buildVolume() {
		if(isDebug()) System.out.printf("%s: build volume for %s with material %s\n", this.getClass().getSimpleName(),getName(), MaterialStore.getInstance().get(getMaterial()));
			setVolume(new LogicalVolume(getName() + "_volume", box, MaterialStore.getInstance().get(getMaterial())));
		
	}
	public void setPositionAndRotation(SurveyVolume base) {
		if(isDebug()) System.out.printf("%s: set position and rotation for volume %s\n", this.getClass().getSimpleName(),getName());
		
		// no mother, this must be the world/tracking volume!?
		if(base.getMother()==null) throw new RuntimeException("trying to set coordinates w/o mother defined for "+base.getName());
		
		// Vector from origin to center of box locally 
		Hep3Vector box_center_base_local = base.getCenter();
		
		// find the physical mother i.e. not a ghost volume and compound transformations to it
		JavaSurveyVolume physMother =  getPhysMother();
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

		// now calculate the position of this box center in the mother LCDD coordinates
		Hep3Vector box_center = VecOp.sub(box_center_base, mother_center);

		//Find LCDD Euler rotation angles from coordinate system unit vectors
		//Note that this has to be rotation wrt to physical mother and not just mother as normally is the case
		//Use apache lib to get angles, but in principle I should already have it from the trf above
		//Hep3Vector lcdd_rot_angles = HPSTestRunTracker2014.getEulerAngles(base.getCoord().v(), base.getCoord().w(), new BasicHep3Vector(0,1,0),new BasicHep3Vector(0,0,1));
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


		// Create the LCDD position
		setPos(new Translation3D(box_center.x(), box_center.y(), box_center.z()));
		setRot(new RotationGeant(lcdd_rot_angles.x(), lcdd_rot_angles.y(), lcdd_rot_angles.z()));
		
		if(isDebug()) {
			
		    System.out.printf("%s: SurveyVolume information for %s:\n", this.getClass().getSimpleName(), base.getName());
            System.out.printf("%s: box_center_base_local       %s\n", this.getClass().getSimpleName(), box_center_base_local.toString());
			System.out.printf("%s: box_center_base             %s\n", this.getClass().getSimpleName(), box_center_base.toString());
			System.out.printf("%s: mother center               %s\n", this.getClass().getSimpleName(), base.getMother()==null?" <no mother> ":mother_center.toString());
			System.out.printf("%s: box_center                  %s\n", this.getClass().getSimpleName(), box_center.toString());
			System.out.printf("%s: pos                         %s\n", this.getClass().getSimpleName(), getPos().toString());
			Hep3Vector box_center_tracking_xcheck = HPSTrackerBuilder.transformToTracking(box_center_base_local, base);
            System.out.printf("%s: box_center_tracking_xcheck  %s (for %s)\n", this.getClass().getSimpleName(), box_center_tracking_xcheck==null ? " <null> " : box_center_tracking_xcheck.toString(),base.getName());
            Hep3Vector box_center_envelope_xcheck2 = HPSTrackerBuilder.transformToParent(box_center_base_local, base, "base");
            System.out.printf("%s: box_center_base_xcheck2     %s (for %s)\n", this.getClass().getSimpleName(), box_center_envelope_xcheck2==null ? " <null> " : box_center_envelope_xcheck2.toString(),base.getName());
            if(box_center_envelope_xcheck2!=null) {
                Hep3Vector box_center_envelope_xcheck2_inch = VecOp.mult(0.0393701, box_center_envelope_xcheck2);
                System.out.printf("%s: box_center_base_xcheck2_in  %s (for %s)\n", this.getClass().getSimpleName(), box_center_envelope_xcheck2_inch==null ? " <null> " : box_center_envelope_xcheck2_inch.toString(),base.getName());
            }
            Hep3Vector origin = base.getCoord().origin();
            if(origin!=null) {
                Hep3Vector origin_in = VecOp.mult(0.0393701, origin);
                System.out.printf("%s: origin_in               %s (%s)\n", this.getClass().getSimpleName(), origin_in==null ? " <null> " : origin_in.toString(), base.getName());
            }
            Hep3Vector origin_base = HPSTrackerBuilder.transformToParent(base.getCoord().origin(), base, "base");
            if(origin_base!=null) {   
                Hep3Vector origin_base_in = VecOp.mult(0.0393701, origin_base);
                System.out.printf("%s: origin_base_in          %s\n", this.getClass().getSimpleName(), origin_base_in==null ? " <null> " : origin_base_in.toString());
            }
            System.out.printf("%s: euler                       %s\n", this.getClass().getSimpleName(), lcdd_rot_angles.toString());
			System.out.printf("%s: rot                         %s\n", this.getClass().getSimpleName(), getRot().toString());
			
		}
		
	}

	/**
	 * Find the first non-ghost volume among parents.  
	 * @return mother object
	 */
	public JavaSurveyVolume getPhysMother() {
		//if(isDebug()) System.out.printf("%s: finding physical mother to %s\n", this.getClass().getSimpleName(), getName());
		if(mother==null) throw new RuntimeException("Trying to get phys mother but there is no mother!");
		if(mother instanceof JavaGhostSurveyVolume) {
			return mother.getPhysMother();
		} else {
			//if(isDebug()) System.out.printf("%s: found a non-ghost volume: %s\n", this.getClass().getSimpleName(), mother.getName());
			return mother;
		}
	}
	
	
	public ILogicalVolume getVolume() {
		return volume;
	}
	protected void setVolume(ILogicalVolume volume) {
		this.volume = volume;
	}
	protected Box getBox() {
		return box;
	}
	protected void setBox(Box b) {
		box = b;
	}	
	protected ITranslation3D getPos() {
		return pos;
	}
	protected void setPos(ITranslation3D iTranslation3D) {
		this.pos = iTranslation3D;
	}
	protected IRotation3D getRot() {
		return rot;
	}
	protected void setRot(IRotation3D iRotation3D) {
		this.rot = iRotation3D;
	}
	public JavaSurveyVolume getMother() {
		return mother;
	}
	protected void setMother(JavaSurveyVolume mother) {
		this.mother = mother;
	}
	public IPhysicalVolume getPhysVolume() {
		return physVolume;
	}
	protected void setPhysVolume(PhysicalVolume physVolume) {
		this.physVolume = physVolume;
	}

	public List<JavaSurveyVolume> getDaughters() {
		return daughters;
	}

	protected void addDaughter(JavaSurveyVolume o) {
		getDaughters().add(o);
	}

	public int getComponentId() {
		return componentId;
	}

	public void setComponentId(int componentId) {
		this.componentId = componentId;
	}
	
	public String toString() {
		String s = "JavaBaseGeometry " + getName() + "\n";
		if(getPos()!=null && getRot()!=null) {
			s += "Position: "  + getPos().toString() + "\n";
			s += "Rotation: " + getRot().toString() + "\n";
		} else {
			s+= " - no position/rotation info -\n";
		}
		return s;
	}

}