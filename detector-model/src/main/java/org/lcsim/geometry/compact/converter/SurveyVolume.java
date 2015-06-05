package org.lcsim.geometry.compact.converter;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.lcsim.geometry.util.TransformationUtils;

/**
 * 
 * Class containing the basic geometry information for building a volume based on survey positions.
 * 
 */
public abstract class SurveyVolume {
	protected boolean debug = false;
	private String name;
	private String material = "Vacuum";
	private SurveyVolume mother = null;
	protected List<SurveyVolume> referenceGeom = null;
	private SurveyCoordinateSystem coord;
	protected  Hep3Vector ballPos;
	protected  Hep3Vector veePos;
	protected  Hep3Vector flatPos;
	private Hep3Vector center;
	private Hep3Vector boxDim;
	private AlignmentCorrection alignmentCorrections;
	
	public SurveyVolume(String name, SurveyVolume m, AlignmentCorrection alignmentCorrection) {
		setName(name);
		setMother(m);
		setAlignmentCorrection(alignmentCorrection);
	}
	
	public SurveyVolume(String name, SurveyVolume m, AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
		setName(name);
		setMother(m);
		setAlignmentCorrection(alignmentCorrection);
		addReferenceGeom(ref);
	}
	
	public SurveyVolume(String name, SurveyVolume m, AlignmentCorrection alignmentCorrection, List<SurveyVolume> ref) {
		setName(name);
		setMother(m);
		setAlignmentCorrection(alignmentCorrection);
		addReferenceGeom(ref);
	}
	
	protected abstract void setPos();
	protected abstract void setCenter();
	protected abstract void setBoxDim();

	protected void init() {
	    if(debug) System.out.printf("%s: init SurveyVolume %s\n",this.getClass().getSimpleName(),getName());
        setPos();
		setCoord();
		applyReferenceTransformation();
		setCenter();
		setBoxDim();
		applyGenericCoordinateSystemCorrections();
		applyLocalAlignmentCorrections();
		if(debug) {
		    //printCoordInfo();
		    System.out.printf("%s: init of SurveyVolume %s DONE\n",this.getClass().getSimpleName(),getName());            
		}
	}
	

    protected void applyGenericCoordinateSystemCorrections() {
	    //do nothing here unless overridden
	   
	}
	protected void applyReferenceTransformation() {

	        	    
	    if(referenceGeom!=null) {
	        
	        if(debug) System.out.printf("%s: apply reference transformation for %s\n",this.getClass().getSimpleName(),getName());
	        

	        if(debug) System.out.printf("%s: coord system before %d ref transformations:\n%s\n",this.getClass().getSimpleName(),referenceGeom.size(),getCoord().toString());
	        
	        for(SurveyVolume ref : referenceGeom) {

	            if(debug) {
	                System.out.printf("%s: coord system before ref %s transform:\n%s\n",this.getClass().getSimpleName(),ref.getName(),getCoord().toString());
	                System.out.printf("%s: Ref %s coord\n%s\n",this.getClass().getSimpleName(), ref.getName(),ref.getCoord().toString());
	            }
	            
	            getCoord().transform(ref.getCoord().getTransformation());

	            if(debug) System.out.printf("%s: coord system after ref %s transform:\n%s\n",this.getClass().getSimpleName(),ref.getName(),getCoord().toString());
	            
	        }

	        if(debug) System.out.printf("%s: coord system after ref transformations:\n%s\n",this.getClass().getSimpleName(),getCoord().toString());
	        
	    } else {
	        
	        if(debug) System.out.printf("%s: no reference transformation exists for %s\n",this.getClass().getSimpleName(),getName());
	        
	    }

	}
	
	private void applyLocalAlignmentCorrections() {
	    // Apply alignment corrections to local coordinate system that is already built
	    boolean debug_local = false;
	    if(this.coord==null) 
	        throw new RuntimeException("no coordinate system was set before trying to apply alignment corrections.");

	    if(alignmentCorrections!=null) {

	        if(debug_local || debug) System.out.printf("%s: Apply alignment corrections to %s\n",this.getClass().getSimpleName(),this.getName());

	        // translate
	        if(alignmentCorrections.getTranslation()!=null) {			    

	            if(debug_local || debug) System.out.printf("%s: Apply local translation %s\n", this.getClass().getSimpleName(),alignmentCorrections.getTranslation().toString());			    
	            
	            // rotate into mother coordinate system
	            Hep3Vector translation_mother = getCoord().getTransformation().rotated(alignmentCorrections.getTranslation());
	            
	            if(debug_local || debug) System.out.printf("%s: after rotation apply translation %s to coordinate system\n", this.getClass().getSimpleName(),translation_mother.toString());
	            
	            //apply translation
	            getCoord().translate(translation_mother);

	        } else {
	            if(debug_local || debug) System.out.printf("%s: No translation to coordinate system\n", this.getClass().getSimpleName());
	        }

	        // rotate
	        if(alignmentCorrections.getRotation()!=null) {  
	            
                if(debug_local || debug) {
                    System.out.printf("%s: Apply rotation matrix:\n", this.getClass().getSimpleName());   
                    TransformationUtils.printMatrix(alignmentCorrections.getRotation().getMatrix());
                    System.out.printf("%s: coord system before:\n%s\n", this.getClass().getSimpleName(),getCoord().toString());   
                }

	            
	            // correct rotation of the local unit vectors
	            Vector3D u_rot_local = alignmentCorrections.getRotation().applyTo(new Vector3D(1,0,0));
	            Vector3D v_rot_local = alignmentCorrections.getRotation().applyTo(new Vector3D(0,1,0));
	            Vector3D w_rot_local = alignmentCorrections.getRotation().applyTo(new Vector3D(0,0,1));

	            // rotate the local unit vectors to the mother coordinates
	            
	            Hep3Vector u_rot = getCoord().getTransformation().getRotation().rotated(new BasicHep3Vector(u_rot_local.toArray()));
	            Hep3Vector v_rot = getCoord().getTransformation().getRotation().rotated(new BasicHep3Vector(v_rot_local.toArray()));
	            Hep3Vector w_rot = getCoord().getTransformation().getRotation().rotated(new BasicHep3Vector(w_rot_local.toArray()));
               
	            getCoord().u(u_rot);
	            getCoord().v(v_rot);
	            getCoord().w(w_rot);

                if(debug_local || debug) {
                    System.out.printf("%s: coord system after:\n%s\n", this.getClass().getSimpleName(),getCoord().toString());   
                }

	            
	            // Do some gymnastics from Apache rotation to use the rotation class
	            //double matMP_v[][] = alignmentCorrections.getRotation().getMatrix();
                //Hep3Matrix matMP = new BasicHep3Matrix(matMP_v[0][0], matMP_v[0][1], matMP_v[0][2], 
                //                                       matMP_v[1][0], matMP_v[1][1], matMP_v[1][2],
                //                                       matMP_v[2][0], matMP_v[2][1], matMP_v[2][2]);
                //
                //Rotation3D rotMP = new Rotation3D(matMP);

                // get the rotation correction in the mother coordinate system
                //Rotation3D r = Rotation3D.multiply(getCoord().getTransformation().getRotation(),rotMP);
	            
//                if(debug_local || debug) {
//	                System.out.printf("%s: Apply rotation matrix:\n", this.getClass().getSimpleName());             
//	                double mat[][] = alignmentCorrections.getRotation().getMatrix();
//	                TransformationUtils.printMatrix(mat);
//	                System.out.printf("%s: corresponding Rotation3D object:\n%s\n",this.getClass().getSimpleName(), rotMP.toString());
//	                // Get the Cardan angles of the rotation
//	                double res[] = alignmentCorrections.getRotation().getAngles(RotationOrder.ZYX);
//	                // Since the rotation was created based on active transformations convert to passive right here. 
//	                // This conversion is simply to reverse the order of rotations.
//	                Hep3Vector res_passive = new BasicHep3Vector(res[2],res[1],res[0]);
//	                System.out.printf("%s: Corresponding LCDD Cardan angles: %s\n", this.getClass().getSimpleName(), res_passive.toString());             
//	                System.out.printf("%s: Apply local to mother rotation\n%s\n",this.getClass().getSimpleName(), getCoord().getTransformation().getRotation().toString());
//	                System.out.printf("%s: resulting rotation correction to apply\n%s\n",this.getClass().getSimpleName(), r.toString());
//                    
//	            }

                // Apply correction to coordinate system
	            //getCoord().rotateApache(alignmentCorrections.getRotation());
                //getCoord().rotate(r);

	        } else {
	            if(debug_local || debug) System.out.printf("%s: No rotation to coordinate system\n", this.getClass().getSimpleName());
	        }

	        if(debug_local || debug) System.out.printf("%s: coordinate system after alignment corrections:\n%s\n",this.getClass().getSimpleName(),getCoord().toString());

	    } else {
            if(debug_local || debug) System.out.printf("%s: no alignment corrections exist for %s\n",this.getClass().getSimpleName(),this.getName());
	    }

	}
	
	private void setAlignmentCorrection(AlignmentCorrection alignmentCorrection) {
        this.alignmentCorrections = alignmentCorrection;
    }
    public  void setBallPos(double x, double y, double z) {
		ballPos = new BasicHep3Vector(x,y,z);
	}
	public  void setVeePos(double x, double y, double z) {
		veePos = new BasicHep3Vector(x,y,z);
	}
	public  void setFlatPos(double x, double y, double z) {
		flatPos = new BasicHep3Vector(x,y,z);
	}
	public  Hep3Vector getBallPos() {
		return ballPos;
	}
	public  Hep3Vector getVeePos() {
		return veePos;
	}
	public  Hep3Vector getFlatPos() {
		return flatPos;
	}
	public void setCoord() {
		if(ballPos==null || veePos==null || flatPos==null) {
			throw new RuntimeException("Need to set ball, vee and flat before building coord system!");
		}
		
		coord = new SurveyCoordinateSystem(ballPos, veePos, flatPos);					
		
		if(this.debug) {
		    System.out.printf("%s: setCoord \n%s\n", this.getClass().getSimpleName(), coord.toString());
		}
	}
	public SurveyCoordinateSystem getCoord() {
		if(coord == null) {
			throw new RuntimeException("Need to setCoord!");
		}
		return coord;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Hep3Vector getCenter() {
		return center;
	}
	public void setCenter(Hep3Vector center) {
		this.center = center;
	}
	public void setCenter(double x, double y, double z) {
		this.center = new BasicHep3Vector(x,y,z);
	}
	public Hep3Vector getBoxDim() {
		return boxDim;
	}
	public void setBoxDim(double x, double y, double z) {
		this.boxDim = new BasicHep3Vector(x,y,z);
	}
	public SurveyVolume getMother() {
		return mother;
	}
	public void setMother(SurveyVolume mother) {
		this.mother = mother;
	}
	public void addReferenceGeom(SurveyVolume refGeom) {
	    if(refGeom!=null) { // check that it's not a dummy call
	        if(referenceGeom == null) {
	            referenceGeom = new ArrayList<SurveyVolume>();
	        }
	        referenceGeom.add(refGeom);
	    }
	}
	public void addReferenceGeom(List<SurveyVolume> refGeomList) {
		if(referenceGeom == null) {
			referenceGeom = new ArrayList<SurveyVolume>();
		}
		referenceGeom.addAll(refGeomList);
	}
	public void printSurveyPos() {
	    if(debug) {
	        System.out.printf("%s: Survey pos for %s:\n",getClass().getSimpleName(),getName());
	        System.out.printf("%s: ballPos   %s\n",getClass().getSimpleName(), ballPos.toString());
	        System.out.printf("%s: veePos    %s\n",getClass().getSimpleName(), veePos.toString());
	        System.out.printf("%s: flatPos   %s\n",getClass().getSimpleName(), flatPos.toString());
	    }
	}
	public String getMaterial() {
		return material;
	}
	public void setMaterial(String material) {
		this.material = material;
	}
	public String toString() {
		String s = "==\n" + getName() + " with mother " + (getMother()==null?"<no mother>":getMother().getName()) + ":\n";
		if( getCenter()!=null) s += "Center of box: " + getCenter().toString() + "\n";
        if( getBoxDim()!=null) s += "Box dimensions: " + getBoxDim().toString() + "\n";
		if(this.coord==null)   s += " No coord system \n";
		else s += "Coordinate system:" + getCoord().toString() + "\n";
        s += "AlignmentCorrections: \n";
		if(this.alignmentCorrections!=null) {
		    s += "Milleparameters: ";
		    if(this.alignmentCorrections.getMilleParameters()!=null) {
		        for(MilleParameter mp : this.alignmentCorrections.getMilleParameters()) s += mp.getId() + " ";
		    } else {
		        s += "no MP params associated.";
		    }
		    s +=  "(" + this.getName() + ")" + " \n";
		} else {
		    s+= " no alignment corrections associated.\n";
		}
		SurveyVolume m = getMother();
		while(m!=null) {    
            Hep3Vector origin_m = HPSTrackerBuilder.transformToParent(new BasicHep3Vector(0, 0, 0), this, m.getName());
            s += String.format("%s origin in %s : %s (mm)\n",getName(), m.getName(), origin_m.toString());            
            origin_m = VecOp.mult(0.0393701, origin_m);
            s += String.format("%s origin in %s : (%.4f %.4f %.4f) (inch)\n",getName(), m.getName(), origin_m.x(),origin_m.y(),origin_m.z());            
            m = m.getMother();
		}
		
		
		return s;
	}
	
//	private void printCoordInfo() {
//	    if(debug) {
//	        SurveyVolume m = getMother();
//	        while(m!=null) {    
//	            Hep3Vector origin_m = HPSTrackerBuilder.transformToParent(getCoord().origin(), this, m.getName());
//	            System.out.printf("%s: %s final coord system in %s : %s\n",this.getClass().getSimpleName(),getName(), getMother()==null?" <no mother> ":getMother().getName(),getCoord().toString());            
//	        }
//	        System.out.printf("%s: init of SurveyVolume %s DONE\n",this.getClass().getSimpleName(),getName());            
//	    }
//	}
	
}