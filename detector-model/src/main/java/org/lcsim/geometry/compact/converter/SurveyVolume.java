package org.lcsim.geometry.compact.converter;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.jdom.Element;
import org.lcsim.detector.Translation3D;
import org.lcsim.geometry.compact.converter.HPSTracker2014GeometryDefinition.SvtBox;
import org.lcsim.geometry.util.TransformationUtils;

/**
 * 
 * Contains the geometry information that is used to build any volume. 
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

    /**
     * 
     * Initialize the volume. 
     * This needs to be called at the top level implementation of the {@link SurveyVolume} to properly setup
     * the coordinate systems. It takes care of applying user supplied custom transformations and alignment corrections
     * in the order given in the function below. That order must be preserved to get a uniform behavior. 
     * 
     */
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
    

    private void applySurvey(Element node) {

        if(debug) System.out.printf("%s: apply survey from compact.\n", this.getClass().getSimpleName());

        // Check that XML file is read into memory and available
        if(node==null) {
            
            if(debug) System.out.printf("%s: WARNING: no XML file for survey information available.\n", this.getClass().getSimpleName());
    
        } else {
            
            SurveyResult surveyResult = SurveyResult.findResultFromDetector(node, getName());
            
            if(surveyResult!=null) {
                if(debug) System.out.printf("%s: found survey results: \n%s \n", this.getClass().getSimpleName(), surveyResult.toString());
                
                // Adjust coordinate system to match the one used in the geometry.
                // This depends on the particular volume we are in.
                
                if(HPSTrackerBuilder.isModule(name)) {

                    if(HPSTrackerBuilder.isTopFromName(name)) {
                        
                        if(debug) System.out.printf("%s: treating it as a top module\n", this.getClass().getSimpleName());
                        
                        // The U-channel coordinate system is flipped 90deg clockwise around survey x-axis
                        
                        Rotation rotation1 = new Rotation(new Vector3D(1, 0, 0),-Math.PI/2.0);
                        surveyResult.rotateOrigin(rotation1);
                        surveyResult.rotateUnitVectors(rotation1);
                        
                        if(debug) System.out.printf("%s: UPDATE1 found survey results: \n%s \n", this.getClass().getSimpleName(), surveyResult.toString());
                        
                        // The unit vectors of the survey module coordinate system (pin bases) are different:
                        // survey x-axis is module v-axis
                        // survey y-axis is module -1*u-axis
                        // survey z-axis is module w-axis

                        Hep3Vector y = new BasicHep3Vector(surveyResult.getX().v());
                        Hep3Vector x = new BasicHep3Vector(VecOp.mult(-1, surveyResult.getY()).v());
                        surveyResult.setX(x);
                        surveyResult.setY(y);
                        
                    }
                    else {
                    
                        if(debug) System.out.printf("%s: treating it as a bottom module\n", this.getClass().getSimpleName());

                        // The survey u-channel coordinate system needs two rotations to correspond to the one used here
                        Rotation rotation1 = new Rotation(new Vector3D(1, 0, 0), Math.PI/2.0);
                        Rotation rotation2 = new Rotation(new Vector3D(0, 0, 1), Math.PI);
                        Rotation rotation = rotation2.applyTo(rotation1);
                        surveyResult.rotateOrigin(rotation);
                        surveyResult.rotateUnitVectors(rotation);

                        if(debug) System.out.printf("%s: UPDATE1 found survey results: \n%s \n", this.getClass().getSimpleName(), surveyResult.toString());
                        
                        // The unit vectors of the survey module coordinate system (pin bases) are different:
                        // survey x-axis is module v-axis
                        // survey y-axis is module -1*u-axis
                        // survey z-axis is module w-axis
                        
                        Hep3Vector x = new BasicHep3Vector( VecOp.mult(-1, surveyResult.getY()).v());
                        Hep3Vector y = new BasicHep3Vector( surveyResult.getX().v() );
                        surveyResult.setX(x);
                        surveyResult.setY(y);

                        if(debug) System.out.printf("%s: UPDATE2 found survey results: \n%s \n", this.getClass().getSimpleName(), surveyResult.toString());
                    }
                   


                } else if(HPSTrackerBuilder.isHalfModule(name)) {

                    if(debug) System.out.printf("%s: treating it as a half-module\n", this.getClass().getSimpleName());

                    // Adjust origin to the sensor center
                    surveyResult.setOrigin(VecOp.add(surveyResult.getOrigin(), VecOp.mult(-0.160, surveyResult.getZ())));

                    // rotate and flip axis to adhere to the definitions of the u,v,w used for the SurveyVolume
                    Rotation rotation = new Rotation(new Vector3D(0,0,1),Math.PI/2.0);
                    surveyResult.rotateOrigin(rotation);                
                    surveyResult.rotateUnitVectors(rotation);
                    Hep3Vector x = new BasicHep3Vector( VecOp.mult(-1, surveyResult.getY()).v());
                    Hep3Vector y = new BasicHep3Vector( surveyResult.getX().v() );
                    surveyResult.setX(x);
                    surveyResult.setY(y);

                    if(debug) System.out.printf("%s: updated found survey results: \n%s \n", this.getClass().getSimpleName(), surveyResult.toString());
                
                    
                    
                    
                    
                } else if(HPSTrackerBuilder.isSupportRingKinMount(name)) {

                    if(debug) System.out.printf("%s: treating it as support ring kinematic mount\n", this.getClass().getSimpleName());



                    // Survey SVT box origin  translated to the edge of the SVT box
                    surveyResult.setOrigin(VecOp.sub(surveyResult.getOrigin(), new BasicHep3Vector(0, 0, -0.375*HPSTrackerGeometryDefinition.inch)));

                    if(debug) System.out.printf("%s: UPDATE1 found survey results: \n%s \n", this.getClass().getSimpleName(), surveyResult.toString());


                    // Survey SVT box origin translated to the center of the Svt Box used in the geometry
                    surveyResult.setOrigin(VecOp.sub(surveyResult.getOrigin(),new BasicHep3Vector(0, 0, SvtBox.length/2.0)));


                    if(debug) System.out.printf("%s: UPDATE2 found survey results: \n%s \n", this.getClass().getSimpleName(), surveyResult.toString());

                    // rotate origin into the SVT box coordinates
                    Rotation r1 = new Rotation(new Vector3D(1, 0, 0), Math.PI/2.0);
                    surveyResult.rotateOrigin(r1);
                    surveyResult.rotateUnitVectors(r1);

                    if(debug) System.out.printf("%s: UPDATE3 found survey results: \n%s \n", this.getClass().getSimpleName(), surveyResult.toString());



                    // Swap definition of unit axis to the one used in the U-channels
                    if(HPSTrackerBuilder.isTopFromName(name)) {

                      //Hep3Vector x = new BasicHep3Vector( VecOp.mult(-1,surveyResult.getX()).v() );
                        Hep3Vector y = new BasicHep3Vector( VecOp.mult(-1,surveyResult.getZ()).v() );
                        Hep3Vector z = new BasicHep3Vector( surveyResult.getY().v() );
                        //surveyResult.setX(x);
                        surveyResult.setY(y);
                        surveyResult.setZ(z);

                    } else {

                        //Hep3Vector x = new BasicHep3Vector( VecOp.mult(-1,surveyResult.getX()).v() );
                        Hep3Vector y = new BasicHep3Vector( VecOp.mult(-1,surveyResult.getZ()).v() );
                        Hep3Vector z = new BasicHep3Vector( surveyResult.getY().v() );
                        //surveyResult.setX(x);
                        surveyResult.setY(y);
                        surveyResult.setZ(z);

                    }




                } else if(HPSTrackerBuilder.isUChannelSupport(name) ) {

                   
                    
                    int layer = HPSTrackerBuilder.getUChannelSupportLayer(name);

                    if(layer >= 4 ) {
                        if(debug) System.out.printf("%s: treating it as a L4-6 U-channel (%d)\n", this.getClass().getSimpleName(), layer);


                        // Survey SVT box origin  translated to the edge of the SVT box
                        surveyResult.setOrigin(VecOp.sub(surveyResult.getOrigin(), new BasicHep3Vector(0, 0, -0.375*HPSTrackerGeometryDefinition.inch)));

                        if(debug) System.out.printf("%s: UPDATE1 found survey results: \n%s \n", this.getClass().getSimpleName(), surveyResult.toString());

                        // Survey SVT box origin translated to the center of the Svt Box used in the geometry
                        surveyResult.setOrigin(VecOp.sub(surveyResult.getOrigin(),new BasicHep3Vector(0, 0, SvtBox.length/2.0)));


                        if(debug) System.out.printf("%s: UPDATE2 found survey results: \n%s \n", this.getClass().getSimpleName(), surveyResult.toString());

                        // rotate origin into the SVT box coordinates
                        Rotation r1 = new Rotation(new Vector3D(1, 0, 0), Math.PI/2.0);
                        surveyResult.rotateOrigin(r1);
                        surveyResult.rotateUnitVectors(r1);

                        if(debug) System.out.printf("%s: UPDATE3 found survey results: \n%s \n", this.getClass().getSimpleName(), surveyResult.toString());

                        // Swap definition of unit axis to the one used in the U-channels
                        if(HPSTrackerBuilder.isTopFromName(name)) {

                            Hep3Vector y = new BasicHep3Vector( surveyResult.getZ().v() );
                            Hep3Vector z = new BasicHep3Vector( VecOp.mult(-1, surveyResult.getY()).v() );
                            surveyResult.setY(y);
                            surveyResult.setZ(z);

                        } else {

                            Hep3Vector x = new BasicHep3Vector( VecOp.mult(-1,surveyResult.getX()).v() );
                            Hep3Vector y = new BasicHep3Vector( surveyResult.getZ().v() );
                            Hep3Vector z = new BasicHep3Vector( surveyResult.getY().v() );
                            surveyResult.setX(x);
                            surveyResult.setY(y);
                            surveyResult.setZ(z);

                        }
                        

                    } else {
                        
                        if(debug) System.out.printf("%s: treating it as a L1-3 U-channel (%d)\n", this.getClass().getSimpleName(), layer);
                        
                        // Rotate the survey origin into the kinematic mount coordinate frame used here
                        Rotation r = new Rotation(new Vector3D(1,0,0), Math.PI/2.0);
                        surveyResult.rotateOrigin(r);
                        surveyResult.rotateUnitVectors(r);
                        
                        if(debug) System.out.printf("%s: UPDATE1 found survey results: \n%s \n", this.getClass().getSimpleName(), surveyResult.toString());

                        
                        // Swap definition of unit axis to the one used in the U-channels
                        if(HPSTrackerBuilder.isTopFromName(name)) {

                            Hep3Vector y = new BasicHep3Vector( surveyResult.getZ().v() );
                            Hep3Vector z = new BasicHep3Vector( VecOp.mult(-1, surveyResult.getY()).v() );
                            surveyResult.setY(y);
                            surveyResult.setZ(z);

                        } else {

                            Hep3Vector y = new BasicHep3Vector( surveyResult.getZ().v() );
                            Hep3Vector z = new BasicHep3Vector( surveyResult.getY().v() );
                            Hep3Vector x = new BasicHep3Vector( VecOp.mult(-1,surveyResult.getX()).v() );
                            surveyResult.setX(x);
                            surveyResult.setY(y);
                            surveyResult.setZ(z);

                        }
                        
                    }


                    
                    
                } else {
                    
                    throw new RuntimeException("I don't think there is a surveyresult defined for this type from " + name);
                    
                }
                
                if(debug) System.out.printf("%s:  survey results after corrections: \n%s \n", this.getClass().getSimpleName(), surveyResult.toString());

                
                
                // Need to go through the reference/ghost geometries if they exist
                
                if(referenceGeom!=null) {
                    if(debug) System.out.printf("%s: apply reference transformation for %s\n",this.getClass().getSimpleName(),getName());
                    if(debug) System.out.printf("%s: survey system before %d ref transformations:\n%s\n",this.getClass().getSimpleName(),referenceGeom.size(),surveyResult.toString());
                    for(SurveyVolume ref : referenceGeom) {
                        if(debug) {
                            System.out.printf("%s: survey system before ref %s transform:\n%s\n",this.getClass().getSimpleName(),ref.getName(),surveyResult.toString());
                            System.out.printf("%s: Ref %s coord\n%s\n",this.getClass().getSimpleName(), ref.getName(),ref.getCoord().toString());
                        }
                        surveyResult.transform(ref.getCoord().getTransformation());
                        
                        if(debug) System.out.printf("%s: survey system after ref %s transform:\n%s\n",this.getClass().getSimpleName(),ref.getName(),surveyResult.toString());

                    }

                    if(debug) System.out.printf("%s: survey system after ref transformations:\n%s\n",this.getClass().getSimpleName(),surveyResult.toString());

                } else {

                    if(debug) System.out.printf("%s: no reference transformation exists for %s\n",this.getClass().getSimpleName(),getName());

                }


                if(debug) System.out.printf("%s: apply to \n%s \n", this.getClass().getSimpleName(), this.getCoord().toString());

                // get translation and apply it
                Translation3D transToSurvey = surveyResult.getTranslationFrom(getCoord());
                getCoord().translate(transToSurvey);

                if(debug) System.out.printf("%s: after translation to survey \n%s \n", this.getClass().getSimpleName(), this.getCoord().toString());

                // get rotation and apply it
                Rotation rotToSurvey = surveyResult.getRotationFrom(getCoord());
                getCoord().rotateApache(rotToSurvey);

                if(debug) System.out.printf("%s: after rotation to survey \n%s \n", this.getClass().getSimpleName(), this.getCoord().toString());

                
                
                
                

            } else {
                if(debug) System.out.printf("%s: no survey results for %s in node %s \n", this.getClass().getSimpleName(), getName(), node.getName());
            }
        }
        
        if(debug) System.out.printf("%s: DONE apply survey from compact.\n", this.getClass().getSimpleName());

        
    }

    /**
     * Apply a generic correction to the coordinate system of this volume. 
     */
    protected void applyGenericCoordinateSystemCorrections() {
        //do nothing here unless overridden
       
    }
    
    /**
     * Applies a user supplied reference transformation to the module. 
     * This is convenient as it allows for intermediary "virtual" mother volumes to be used 
     * in referencing a volume to it's physcial mother volume.
     */
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
    
    /**
     * Apply @link AlignmentCorrection to the volume if they are supplied. 
     * 
     */
    private void applyLocalAlignmentCorrections() {
        
        // Apply alignment corrections to local coordinate system that is already built
        boolean debug_local = false;
        if(this.coord==null) 
            throw new RuntimeException("no coordinate system was set before trying to apply alignment corrections.");

        if(alignmentCorrections!=null) {

            
            if(alignmentCorrections.getNode()!=null) {
                
                if(debug_local || debug) System.out.printf("%s: Apply survey results to %s\n",this.getClass().getSimpleName(),this.getName());
                
                applySurvey(alignmentCorrections.getNode());

                if(debug_local || debug) System.out.printf("%s: DONE Apply survey results to %s\n",this.getClass().getSimpleName(),this.getName());
                
            }
            
            
            
            
            
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
        else {
            s += getName() + " origin " + getCoord().origin() + " u " + getCoord().u()+ " v " + getCoord().v()+ " w " + getCoord().w();
        }
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
            String unitVecStr = "";
            if(getCoord()!=null) {
                Hep3Vector u_m = HPSTrackerBuilder.rotateToParent(getCoord().u(), this, m.getName());
                Hep3Vector v_m = HPSTrackerBuilder.rotateToParent(getCoord().v(), this, m.getName());
                Hep3Vector w_m = HPSTrackerBuilder.rotateToParent(getCoord().w(), this, m.getName());
                unitVecStr += String.format("u %s v %s w %s", u_m.toString(), v_m.toString(), w_m.toString());            
            }
            s += String.format("%s origin in %s : %s (mm) %s\n",getName(), m.getName(), origin_m.toString(), unitVecStr);            
            //origin_m = VecOp.mult(0.0393701, origin_m);
            //s += String.format("%s origin in %s : (%.4f %.4f %.4f) (inch)\n",getName(), m.getName(), origin_m.x(),origin_m.y(),origin_m.z());            
            m = m.getMother();
        }
        
        
        return s;
    }
    

    
}