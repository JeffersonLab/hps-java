/**
 * 
 */
package org.lcsim.geometry.compact.converter;

import hep.physics.vec.BasicHep3Vector;

import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationOrder;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.jdom.Element;

/**
 * 
 * Geometry information for the HPS Test run tracker
 * 
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class HPSTestRunTracker2014GeometryDefinition extends HPSTrackerGeometryDefinition {



    
    //General
    protected static final boolean useSiStripsConvention = true;
    protected static final boolean use30mradRotation = true;
    protected static final boolean useFakeHalfModuleAxialPos = false;

    // Global position references   
    protected static final double target_pos_wrt_base_plate_x = 162.3; //from Marco's 3D model
    protected static final double target_pos_wrt_base_plate_y = 80.55; //from Tim's sketchup //68.75; //from Marco's 3D model
    protected static final double target_pos_wrt_base_plate_z = 926.59; //from Marco's 3D model
    


    public HPSTestRunTracker2014GeometryDefinition(boolean debug, Element node) {
        super(debug, node);
        doAxial = true;
        doStereo = true;
        doColdBlock = false;
        doBottom = true;
        doTop = true;
        layerBitMask = 0x1F;    //0x1;//
    }


    /* (non-Javadoc)
     * @see org.lcsim.geometry.compact.converter.HPSTrackerBuilder#build()
     */
    public void build() {

        if(isDebug()) System.out.printf("%s: constructing the geometry objects\n", this.getClass().getSimpleName());

        // Build the geometry from the basic building blocks in the geometry definition class
        // Keep the order correct.
        // Each item has knowledge of its mother but not its daughters
        TrackingVolume tracking = new TrackingVolume("trackingVolume",null);
        surveyVolumes.add(tracking);

        TrackerEnvelope base = new TrackerEnvelope("base",tracking);
        surveyVolumes.add(base);
        
        BasePlate basePlate = new BasePlate("baseplate",base, "Aluminum");
        surveyVolumes.add(basePlate);
        
        CSupport cSupport = new CSupport("c_support", base);
        surveyVolumes.add(cSupport);        
        
        AlignmentCorrection alignmentCorrectionSupportBottom = getL13UChannelAlignmentCorrection(false);
        SupportBottom supportBottom = new SupportBottom("support_bottom", base, alignmentCorrectionSupportBottom, cSupport);
        surveyVolumes.add(supportBottom);
        // The support survey positions are now with respect to its mother and not the reference coord. system.
        // So to get the reference for the support plate I don't need to apply that extra transformation
        
        SupportPlateBottom supportPlateBottom = new SupportPlateBottom("support_plate_bottom", base, supportBottom, "Aluminum");
        surveyVolumes.add(supportPlateBottom);        
        
        AlignmentCorrection alignmentCorrectionSupportTop = getL13UChannelAlignmentCorrection(true);
        SupportTop supportTop = new SupportTop("support_top", base, alignmentCorrectionSupportTop, cSupport);
        surveyVolumes.add(supportTop);
        
        SupportPlateTop supportPlateTop = new SupportPlateTop("support_plate_top", base, supportTop, "Aluminum");
        surveyVolumes.add(supportPlateTop);

        for(int l=1; l<=5;++l) {
            if(doLayer(l)) {
                if(doBottom) makeModuleBundle(l,"bottom");
                if(doTop)    makeModuleBundle(l,"top");
            }
        }       

        if(isDebug()) {
            System.out.printf("%s: DONE constructing the geometry objects\n", this.getClass().getSimpleName());
            System.out.printf("%s: List of all the geometry objects built\n", this.getClass().getSimpleName());
            for(SurveyVolume bg : surveyVolumes) {
                System.out.printf("-------\n%s\n", bg.toString());
            }
        }

    }

    
    
    
    /**
     * Create the module. 
     * @param layer - of the module
     * @param half - top or bottom half of the tracker
     */
    protected void makeModuleBundle(int layer, String half) 
    {

        if(isDebug()) System.out.printf("%s: makeModule for layer %d %s \n", this.getClass().getSimpleName(), layer, half);


        // build the module name
        String volName = "module_L"+ layer + (half=="bottom"?"b":"t");      

        boolean isL13 = ( layer >=1 && layer <=3 ) ? true : false;          

        // find the mother and reference geometry
        // Note that the reference geometry is the support plate and since that is assumed to be 
        // created through it's references we don't need more than one reference to reach the mother coordinate system
        final SurveyVolume mother;
        final SurveyVolume ref;
        if(half == "bottom") {
            mother = getSurveyVolume(TrackerEnvelope.class);
            ref = getSurveyVolume(SupportPlateBottom.class);
        } else {
            mother= getSurveyVolume(TrackerEnvelope.class);
            ref = getSurveyVolume(SupportPlateTop.class);
        }

        //Create the module
        TestRunModule module;
        if(isL13) {
            module = new TestRunModuleL13(volName, mother, ref, layer, half);
        } else {
            module = new TestRunModuleL45(volName, mother, ref, layer, half);
        }


        // create the bundle for this module
        TestRunModuleBundle bundle = new TestRunModuleBundle(module);
        addModuleBundle(bundle);

        if(doAxial) makeHalfModule("axial", module);
        if(doColdBlock) makeColdBlock(module);
        if(doStereo) makeHalfModule("stereo", module);


        if(isDebug()) {
            System.out.printf("%s: created module bundle:\n", this.getClass().getSimpleName());
            bundle.print();
        }

    }

    /**
     * Create the cold block object.
     * @param mother to the cold block
     */
    protected void makeColdBlock(TestRunModule mother) { 
    
    
        String moduleName = mother.getName();
    
        if(isDebug()) System.out.printf("%s: makeColdBlock for %s \n", this.getClass().getSimpleName(), moduleName);
    
    
        String volName = moduleName + "_coldblock";
    
        // find layer
        int layer = getLayerFromVolumeName(moduleName);
    
        // Build the half-module
        TestRunColdBlock coldBlock;
    
        if(layer >= 1 && layer <=3) {
            coldBlock = new TestRunColdBlockL13(volName, mother, layer);
        } else if(layer >= 4 && layer <=5) {
            coldBlock = new TestRunColdBlockL45(volName, mother, layer);
        } else {
            throw new RuntimeException("wrong layer for " + volName);
        }
    
        TestRunModuleBundle bundle = (TestRunModuleBundle) getModuleBundle(mother);
        bundle.coldBlock = coldBlock;
    }
    


   
    public static class TrackerEnvelope extends SurveyVolume {
        // height of the dummy box holding the entire SVT: 
        // this means the bottom of the base plate to the the inner surface of of the PS vac box for now
        public static final double base_height = PS_vac_box_inner_height - BasePlate.base_plate_offset_height; 
        public static final double base_width = BasePlate.base_plate_width;
        public static final double base_length = BasePlate.base_plate_length;

        public TrackerEnvelope(String name, SurveyVolume mother) {
            super(name,mother, null);
            init();
        }
        protected void setPos() {
            final double ball_pos_base_x = -1.0*target_pos_wrt_base_plate_x;
            final double ball_pos_base_y = -1.0*target_pos_wrt_base_plate_y;
            final double ball_pos_base_z = target_pos_wrt_base_plate_z;     
            final double vee_pos_base_x = ball_pos_base_x + BasePlate.base_plate_width;
            final double vee_pos_base_y = ball_pos_base_y;
            final double vee_pos_base_z = ball_pos_base_z;
            final double flat_pos_base_x = ball_pos_base_x;
            final double flat_pos_base_y = ball_pos_base_y;
            final double flat_pos_base_z = ball_pos_base_z - BasePlate.base_plate_length;
            setBallPos(ball_pos_base_x,ball_pos_base_y,ball_pos_base_z);
            setVeePos(vee_pos_base_x,vee_pos_base_y,vee_pos_base_z);
            setFlatPos(flat_pos_base_x, flat_pos_base_y, flat_pos_base_z);
        }
        protected void setCenter() {
            setCenter(base_width/2.0, base_length/2.0, base_height/2.0 - BasePlate.base_plate_thickness);
        }
        protected void setBoxDim() {
            setBoxDim(base_width,base_length,base_height);
        }
    }



    public static class BasePlate extends SurveyVolume {
        // Base plate references    
        public static final double base_plate_thickness = 0.25*inch;
        public static final double base_plate_width = 385.00;
        public static final double base_plate_length = 1216.00;
        //height from vacuum chamber surface
        protected static final double base_plate_offset_height = 2.0; //from Marco's 3D model
        public BasePlate(String name, SurveyVolume mother, String material) {
            super(name,mother, null);
            init();
            setMaterial(material);
        }
        protected void setPos() {
            setBallPos(0,0,0);
            setVeePos(base_plate_width,ballPos.y(),ballPos.z());
            setFlatPos(ballPos.x(),base_plate_length,ballPos.z());
        }
        protected void setCenter() {
            setCenter(base_plate_width/2.0, base_plate_length/2.0, -base_plate_thickness/2.0);
        }
        protected void setBoxDim() {
            setBoxDim(base_plate_width,base_plate_length, base_plate_thickness);
        }
    }




    public static class CSupport extends SurveyVolume {
        // This is the sequence of locating the support plate positions:
        // The c-support pin positions are found
        // the points on the axis of rotation are used as references for building the box surrounding the support plates (incl sensors).
        // this should make it more straightforward when applying a tilt angle
        // c-support:
        // ball position is C-support pin position on electron side on the base plate surface
        // vee position is C-support pin position on positron side on the base plate surface
        // flat position is a randomly chosen point perpendicular to ball to vee vector and offset 10mm along the plate. 
        // Note that the flat here sets the tilt angle of the support plates.

        // c-support references
        // pin position on base plate surface
        private static final double ball_pos_csup_pin_bottom_x = 51.15;
        private static final double ball_pos_csup_pin_bottom_y = 115.02;
        private static final double ball_pos_csup_pin_bottom_z = 0.0;
        private static final double vee_pos_csup_pin_bottom_x = 271.05;
        private static  double vee_pos_csup_pin_bottom_y = 121.62;
        private static  double vee_pos_csup_pin_bottom_z = 0.0;


        public CSupport(String name, SurveyVolume mother) {
            super(name,mother, null);
            init();
        }           
        private void calcAndSetFlatPos() {
            if(use30mradRotation) {
                // find the rotation to place the flat point
                Rotation rot1_csup = 
                        new Rotation(
                                new Vector3D(vee_pos_csup_pin_bottom_x-ball_pos_csup_pin_bottom_x,
                                        vee_pos_csup_pin_bottom_y-ball_pos_csup_pin_bottom_y,
                                        vee_pos_csup_pin_bottom_z-ball_pos_csup_pin_bottom_z),
                                        new Vector3D(1,0,0));

                Vector3D flat_pos_csup_pin_bottom_3D_rot = rot1_csup.applyTo(new Vector3D(0,10.0,0));
                // translate
                double flat_pos_csup_pin_bottom_x = ball_pos_csup_pin_bottom_x + flat_pos_csup_pin_bottom_3D_rot.getX();
                double flat_pos_csup_pin_bottom_y = ball_pos_csup_pin_bottom_y + flat_pos_csup_pin_bottom_3D_rot.getY();
                double flat_pos_csup_pin_bottom_z = ball_pos_csup_pin_bottom_z + flat_pos_csup_pin_bottom_3D_rot.getZ();

                setFlatPos(flat_pos_csup_pin_bottom_x,flat_pos_csup_pin_bottom_y,flat_pos_csup_pin_bottom_z);
                if(debug) System.out.println("rotated setPos for csupport: \n" + getFlatPos().toString());

            } else {

                //vee_pos_csup_pin_bottom_x = ball_pos_csup_pin_bottom_x + 0;
                vee_pos_csup_pin_bottom_y = ball_pos_csup_pin_bottom_y;
                vee_pos_csup_pin_bottom_z = ball_pos_csup_pin_bottom_z + 0;

                double flat_pos_csup_pin_bottom_x = ball_pos_csup_pin_bottom_x + 0;
                double flat_pos_csup_pin_bottom_y = ball_pos_csup_pin_bottom_y + 10.0;
                double flat_pos_csup_pin_bottom_z = ball_pos_csup_pin_bottom_z + 0;

                setFlatPos(flat_pos_csup_pin_bottom_x,flat_pos_csup_pin_bottom_y,flat_pos_csup_pin_bottom_z);
                if(debug) System.out.println("setPos for csupport: \n" + getFlatPos().toString());

            }
        }
        protected void setPos() {
            if(debug) System.out.printf("%s: setPos for %s\n",this.getClass().getSimpleName(),getName());
            calcAndSetFlatPos();
            setBallPos(ball_pos_csup_pin_bottom_x,ball_pos_csup_pin_bottom_y,ball_pos_csup_pin_bottom_z);
            setVeePos(vee_pos_csup_pin_bottom_x,vee_pos_csup_pin_bottom_y,vee_pos_csup_pin_bottom_z);
            if(debug) {
                System.out.printf("%s: survey positions for %s\n",this.getClass().getSimpleName(),getName());
                printSurveyPos();
            }
        }
        protected void setCenter() {
            // this is never used since it's only a reference volume
            setCenter(null);
        }
        protected void setBoxDim() {
        }

    }




    public static class SupportTop extends SurveyVolume {
        // Top only needs a vertical offset to be specified
        private static final double ball_pos_csup_bearings_top_z = 146.4;
        //these are for the box surrounding the whole support including modules
        protected static final double support_top_length = SupportBottom.support_bottom_length;
        protected static final double support_top_width = SupportBottom.support_bottom_width;
        protected static final double support_top_height = SupportBottom.support_bottom_height;

        public SupportTop(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection) {
            super(name,mother, alignmentCorrection);
            init();
        }
        public SupportTop(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, SurveyVolume referenceGeom) {
            super(name,mother,alignmentCorrection, referenceGeom);
            init();
        }
        public SupportTop(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, List<SurveyVolume> referenceGeom) {
            super(name,mother,alignmentCorrection, referenceGeom);
            init();
        }

        protected void setPos() {
            if(debug) System.out.printf("%s: setPos for %s\n",this.getClass().getSimpleName(),getName());
            // the top has the same bearing positions as the bottom c-support except for the offset vertically from base plate
            // the tilt angle is independent though.
            setBallPos(SupportBottom.ball_pos_csup_bearings_bottom_x, SupportBottom.ball_pos_csup_bearings_bottom_y, ball_pos_csup_bearings_top_z);
            setVeePos(SupportBottom.vee_pos_csup_bearings_bottom_x, SupportBottom.vee_pos_csup_bearings_bottom_y, ball_pos_csup_bearings_top_z);
            // build the rotation to find the proper location of the flat
            Rotation rot_csup_top = 
                    new Rotation(RotationOrder.XYZ, 
                            SupportPlateTop.support_plate_top_tilt_angle, 0.0, 0.0 );

            // apply to flat local position (as for bottom it is an arbitrary offset)
            Vector3D flat_pos_csup_bearings_top_3D_rot = 
                    rot_csup_top.applyTo(new Vector3D(0.0,10.0,0.0));

            // translate the flat position
            final double flat_pos_csup_bearings_top_x = getBallPos().x() + flat_pos_csup_bearings_top_3D_rot.getX();
            final double flat_pos_csup_bearings_top_y = getBallPos().y() + flat_pos_csup_bearings_top_3D_rot.getY();
            final double flat_pos_csup_bearings_top_z = getBallPos().z() + flat_pos_csup_bearings_top_3D_rot.getZ();
            setFlatPos(flat_pos_csup_bearings_top_x,flat_pos_csup_bearings_top_y,flat_pos_csup_bearings_top_z);

            // since we don't care (no volume is built) about the local position of the bearings in the pin coord system we'll get rid of it
            // and find the bearings position in the base coordinate system directly
            if(referenceGeom==null) {
                throw new RuntimeException("No ref found for " + getName());
            }
            /*
            for(SurveyVolume ref : referenceGeom) {

                if(debug) {
                    System.out.printf("%s: survey positions before ref %s transform\n",this.getClass().getSimpleName(),ref.getName());
                    printSurveyPos();
                }

                if(debug) System.out.printf("%s: Ref %s coord\n%s\n",this.getClass().getSimpleName(), ref.getName(),ref.getCoord().toString());

                ref.getCoord().getTransformation().transform(ballPos);
                ref.getCoord().getTransformation().transform(veePos);
                ref.getCoord().getTransformation().transform(flatPos);

                if(debug) {
                    System.out.printf("%s: survey positions after ref %s transform\n",this.getClass().getSimpleName(),ref.getName());
                    printSurveyPos();
                }
            }
            */
            if(debug) {
                System.out.printf("%s: survey positions for %s\n",this.getClass().getSimpleName(),getName());
                printSurveyPos();
            }
        }
        protected void setCenter() {
            setCenter(support_top_width/2.0+1.0, support_top_length/2.0 + (17.00-10.50/2.0), -1.0 * (support_top_height/2.0 - (12.70-6.66-1.34)));
        }
        protected void setBoxDim() {
            setBoxDim(support_top_width,support_top_length,support_top_height);
        }
        /*
        protected void applyReferenceTransformation() {
         
            if(debug) {
                System.out.printf("%s: coord system before ref transformations:\n%s\n",this.getClass().getSimpleName(),getCoord().toString());
            }

            for(SurveyVolume ref : referenceGeom) {

                if(debug) {
                    //System.out.printf("%s: survey positions before ref %s transform\n",this.getClass().getSimpleName(),ref.getName());
                    //printSurveyPos();
                    System.out.printf("%s: coord system before ref %s transform:\n%s\n",this.getClass().getSimpleName(),ref.getName(),getCoord().toString());
                }

                if(debug) System.out.printf("%s: Ref %s coord\n%s\n",this.getClass().getSimpleName(), ref.getName(),ref.getCoord().toString());
                
                getCoord().transform(ref.getCoord().getTransformation());
                
                //ref.getCoord().getTransformation().transform(ballPos);
                //ref.getCoord().getTransformation().transform(veePos);
                //ref.getCoord().getTransformation().transform(flatPos);

                if(debug) {
                    //System.out.printf("%s: survey positions after ref %s transform\n",this.getClass().getSimpleName(),ref.getName());
                    //printSurveyPos();
                    System.out.printf("%s: coord system after ref %s transform:\n%s\n",this.getClass().getSimpleName(),ref.getName(),getCoord().toString());
                }
            }
            
            if(debug) {
                System.out.printf("%s: coord system after ref transformations:\n%s\n",this.getClass().getSimpleName(),getCoord().toString());
            }
            
            
        }
         */
    }



    public static class SupportBottom extends SurveyVolume {
        // "bearings" are points on axis of rotation on the inside of the c-support frame where the insert get's attached
        // this is referenced to the pin position of the c-support
        private static final double ball_pos_csup_bearings_bottom_x = 240.0 - 265.0 + 14.0;
        private static final double ball_pos_csup_bearings_bottom_y = (-6.0 + 22.0);
        private static final double ball_pos_csup_bearings_bottom_z = 14.7;     
        private static final double vee_pos_csup_bearings_bottom_x = 240.0- 129.0;
        private static final double vee_pos_csup_bearings_bottom_y = (-6.0 + 22.0);
        private static final double vee_pos_csup_bearings_bottom_z = 14.7;

        //these are for the box surrounding the whole support including modules
        protected static final double support_bottom_length = SupportPlateBottom.support_plate_bottom_length;
        protected static final double support_bottom_width = (25.0-5.0) + TestRunModuleL13.module_box_L13_length;
        protected static final double support_bottom_height = SupportPlateBottom.support_plate_bottom_height - SupportPlateBottom.support_plate_pocket_depth + TestRunModuleL13.module_box_L13_width + SupportPlateBottom.pedestal_height_L1;


        public SupportBottom(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, SurveyVolume referenceGeom) {
            super(name,mother,alignmentCorrection, referenceGeom);
            init();
        }

        protected void setPos() {

            if(debug) System.out.printf("%s: setPos for %s\n",this.getClass().getSimpleName(),getName());
            
            // now create the support box which will have it's coordinates at the rotation axis so that the flat determines the tilt of the plates
            // it is referenced locally to the c-support pin coordinate system here

            // build the rotation to find the proper location of the flat
            Rotation rot_csup = 
                    new Rotation(RotationOrder.XYZ, 
                            SupportPlateBottom.support_plate_bottom_tilt_angle, 0.0, 0.0 );
            // apply to flat local position
            Vector3D flat_pos_csup_bearings_bottom_3D_rot = 
                    rot_csup.applyTo(new Vector3D(0.0,10.0,0.0));
            // translate
            final double flat_pos_csup_bearings_bottom_x = ball_pos_csup_bearings_bottom_x + flat_pos_csup_bearings_bottom_3D_rot.getX();
            final double flat_pos_csup_bearings_bottom_y = ball_pos_csup_bearings_bottom_y + flat_pos_csup_bearings_bottom_3D_rot.getY();
            final double flat_pos_csup_bearings_bottom_z = ball_pos_csup_bearings_bottom_z + flat_pos_csup_bearings_bottom_3D_rot.getZ();

            // make vectors
            setBallPos(ball_pos_csup_bearings_bottom_x,ball_pos_csup_bearings_bottom_y,ball_pos_csup_bearings_bottom_z);
            setVeePos(vee_pos_csup_bearings_bottom_x,vee_pos_csup_bearings_bottom_y,vee_pos_csup_bearings_bottom_z);    
            setFlatPos(flat_pos_csup_bearings_bottom_x,flat_pos_csup_bearings_bottom_y,flat_pos_csup_bearings_bottom_z);


            // create the coordinate system of the c-support bearings
            //HPSTestRunTracker2014GeomDef.Coord csup_bearings_bottom_coord = new HPSTestRunTracker2014GeomDef.Coord(ball_pos_csup_bearings_bottom, vee_pos_csup_bearings_bottom, flat_pos_csup_bearings_bottom);       

            // since we don't care (no volume is built) about the local position of the bearings in the pin coord system we'll get rid of it
            // and find the bearings position in the base coordinate system directly
            if(referenceGeom==null) {
                throw new RuntimeException("No ref found for " + getName());
            }
            /*
            for(SurveyVolume ref : referenceGeom) {

                if(debug) {
                    System.out.printf("%s: survey positions before ref %s transform\n",this.getClass().getSimpleName(),ref.getName());
                    printSurveyPos();
                }

                if(debug) System.out.printf("%s: Ref %s coord\n%s\n",this.getClass().getSimpleName(), ref.getName(),ref.getCoord().toString());

                ref.getCoord().getTransformation().transform(ballPos);
                ref.getCoord().getTransformation().transform(veePos);
                ref.getCoord().getTransformation().transform(flatPos);

                if(debug) {
                    System.out.printf("%s: survey positions after ref %s transform\n",this.getClass().getSimpleName(),ref.getName());
                    printSurveyPos();
                }
            }
            */
            if(debug) {
                System.out.printf("%s: survey positions for %s\n",this.getClass().getSimpleName(),getName());
                printSurveyPos();
            }

        }
        protected void setCenter() {
            setCenter(support_bottom_width/2.0+1.0, support_bottom_length/2.0 + (17.00-10.50/2.0), support_bottom_height/2.0 - (12.70-6.66-1.34));
        }
        protected void setBoxDim() {
            setBoxDim(support_bottom_width,support_bottom_length,support_bottom_height);
        }

    }


    protected static abstract class SupportPlate extends SurveyVolume {
        protected static final double support_plate_pocket_depth = 6.65; // Tim's sketchup, drawing says 6.66mm?
        protected static final double pedestal_height_L1 = 11.00;
        protected static final double pedestal_height_L2 = 9.50;
        protected static final double pedestal_height_L3 = 8.00;
        protected static final double pedestal_height_L4 = 10.00;
        protected static final double pedestal_height_L5 = 7.00;
        public SupportPlate(SurveyVolume mother, SurveyVolume referenceGeom, String name, String material) {
            super(name,mother,null, referenceGeom);
            setMaterial(material);
        }
        public SupportPlate(SurveyVolume mother, List<SurveyVolume> referenceGeom, String name, String material) {
            super(name,mother,null, referenceGeom);
            setMaterial(material);
        }

    }





    public static class SupportPlateBottom extends SupportPlate {
        // support plate references
        // use a settable rotation to effectively determine the flat and therefore the tilt of the support 
        protected static final double support_plate_bottom_tilt_angle = 0.0; 
        protected static final double support_plate_bottom_height = 12.7;
        protected static final double support_plate_bottom_length = 736.1;
        protected static final double support_plate_bottom_width = 120.0;

        public SupportPlateBottom(String name, SurveyVolume mother, SurveyVolume referenceGeom, String material) {
            super(mother, referenceGeom, name, material);
            init();             
        }
        public SupportPlateBottom(String name, SurveyVolume mother, List<SurveyVolume> referenceGeom, String material) {
            super(mother, referenceGeom, name, material);
            init();             
        }
        protected void setPos() {
            if(debug) System.out.printf("%s: setPos for %s\n",this.getClass().getSimpleName(),getName());
            
            ballPos = new BasicHep3Vector(1.0, (17.0-5.0), 6.66+1.34); 
            veePos = new BasicHep3Vector(ballPos.x() + support_plate_bottom_length, ballPos.y(),ballPos.z());
            flatPos = new BasicHep3Vector(ballPos.x(), ballPos.y() + support_plate_bottom_length,ballPos.z());

            if(referenceGeom==null) {
                throw new RuntimeException("No ref found for " + getName());
            }
            /*
            for(SurveyVolume ref : referenceGeom) {

                if(debug) {
                    System.out.printf("%s: survey positions before ref %s transform\n",this.getClass().getSimpleName(),ref.getName());
                    printSurveyPos();
                }

                if(debug) System.out.printf("%s: Ref %s coord\n%s\n",this.getClass().getSimpleName(), ref.getName(),ref.getCoord().toString());

                ref.getCoord().getTransformation().transform(ballPos);
                ref.getCoord().getTransformation().transform(veePos);
                ref.getCoord().getTransformation().transform(flatPos);

                if(debug) {
                    System.out.printf("%s: survey positions after ref %s transform\n",this.getClass().getSimpleName(),ref.getName());
                    printSurveyPos();
                }
            }
            */
            if(debug) {
                System.out.printf("%s: survey positions for %s\n",this.getClass().getSimpleName(),getName());
                printSurveyPos();
            }
        }
        protected void setCenter() {
            setCenter(support_plate_bottom_width/2.0, support_plate_bottom_length/2.0, -1.0 * support_plate_bottom_height/2.0);
        }
        @Override
        protected void setBoxDim() {
            setBoxDim(support_plate_bottom_width,support_plate_bottom_length,support_plate_bottom_height);
        }
    }


    public static class SupportPlateTop extends SupportPlate {
        // support plate references
        // use a settable rotation to effectively determine the flat and therefore the tilt of the support 
        protected static final double support_plate_top_tilt_angle = 0.0; 
        protected static final double support_plate_top_length = SupportPlateBottom.support_plate_bottom_length;
        protected static final double support_plate_top_width = SupportPlateBottom.support_plate_bottom_width;
        protected static final double support_plate_top_height = SupportPlateBottom.support_plate_bottom_height;

        public SupportPlateTop(String name, SurveyVolume mother, SurveyVolume referenceGeom, String material) {
            super(mother,referenceGeom, name,material);
            init();
        }
        protected void setPos() {
            if(debug) System.out.printf("%s: setPos for %s\n",this.getClass().getSimpleName(),getName());
            
            ballPos = new BasicHep3Vector(1.0, (17.0-5.0), -1.0 * (6.66+1.34)); 
            veePos = new BasicHep3Vector(ballPos.x() + support_plate_top_width, ballPos.y(),ballPos.z());
            flatPos = new BasicHep3Vector(ballPos.x(), ballPos.y() + support_plate_top_length,ballPos.z());

            if(referenceGeom==null) {
                throw new RuntimeException("No ref found for " + getName());
            }
            /*
            for(SurveyVolume ref : referenceGeom) {

                if(debug) System.out.printf("%s: survey positions before ref %s transform\n",this.getClass().getSimpleName(),ref.getName());
                if(debug) printSurveyPos();

                if(debug) System.out.printf("%s: Ref %s coord\n%s\n",this.getClass().getSimpleName(), ref.getName(),ref.getCoord().toString());

                ref.getCoord().getTransformation().transform(ballPos);
                ref.getCoord().getTransformation().transform(veePos);
                ref.getCoord().getTransformation().transform(flatPos);

                if(debug) System.out.printf("%s: survey positions after ref %s transform\n",this.getClass().getSimpleName(),ref.getName());
                if(debug) printSurveyPos();
            }
            */
            if(debug) {
                System.out.printf("%s: survey positions for %s\n",this.getClass().getSimpleName(),getName());
                printSurveyPos();
            }
        }
        protected void setCenter() {
            setCenter(support_plate_top_width/2.0, support_plate_top_length/2.0,  support_plate_top_height/2.0);
        }
        @Override
        protected void setBoxDim() {
            setBoxDim(support_plate_top_width, support_plate_top_length, support_plate_top_height);
        }
    }




    public static class TestRunModuleL45 extends TestRunModule {

        protected static final double module_box_L45_length = 205.2 + box_extra_length; // includes lexan spacer and cold block
        protected static final double module_box_L45_height = 12.5 + box_extra_height; // includes screws height
        protected static final double module_box_L45_width = 65.3-12.0 +  box_extra_width; 
        protected static final double dist_lower_sensor_edge_to_cold_block_mounting_surface = 7.662;


        public TestRunModuleL45(String name, SurveyVolume mother, int layer,String half) {
            super(name, mother, layer, half);
        }
        public TestRunModuleL45(String name, SurveyVolume mother, SurveyVolume ref, int layer, String half) {
            super(name, mother, ref, layer, half);
        }
        protected double getColdBlockThickness() {
            return TestRunColdBlockL45.coldblock_L45_thickness;
        }
        protected double getModuleBoxLength() {
            return module_box_L45_length;
        }
        protected double getModuleBoxWidth() {
            return module_box_L45_width;
        }
        protected double getModuleBoxHeight() {
            return module_box_L45_height;
        }
        protected double get_dist_lower_sensor_edge_to_cold_block_mounting_surface() {
            return dist_lower_sensor_edge_to_cold_block_mounting_surface;
        }

    }

    public static class TestRunModuleL13 extends TestRunModule {
        protected static final double module_box_L13_length = 205.2 + box_extra_length; // includes lexan spacer and cold block
        protected static final double module_box_L13_height = 12.5 + box_extra_height; // includes screws height
        protected static final double module_box_L13_width = 71.3 - 13.0 + box_extra_width; // height from cold block to encapsulate the whole module
        protected static final double dist_lower_sensor_edge_to_cold_block_mounting_surface = 12.66;

        public TestRunModuleL13(String name, SurveyVolume mother, int layer, String half) {
            super(name, mother, layer, half);
        }
        public TestRunModuleL13(String name, SurveyVolume mother, SurveyVolume ref, int layer, String half) {
            super(name, mother, ref, layer, half);
        }
        protected double getColdBlockThickness() {
            return TestRunColdBlockL13.coldblock_L13_thickness;
        }   
        protected double getModuleBoxLength() {
            return module_box_L13_length;
        }
        protected double getModuleBoxWidth() {
            return module_box_L13_width;
        }
        protected double getModuleBoxHeight() {
            return module_box_L13_height;
        }
        protected double get_dist_lower_sensor_edge_to_cold_block_mounting_surface() {
            return dist_lower_sensor_edge_to_cold_block_mounting_surface;
        }
    }


    public static abstract class TestRunModule extends BaseModule {
        protected final static double box_extra_length = 10.0;// random at this point
        protected final static double box_extra_width = 15.0;// random at this point
        protected final static double box_extra_height = 1.0;// random at this point

        public TestRunModule(String name, SurveyVolume mother, int layer, String half) {
            super(name, mother,null,layer, half);
            init();
        }           
        public TestRunModule(String name, SurveyVolume mother, SurveyVolume ref, int layer, String half) {
            super(name, mother,null,ref,layer, half);
            init();
        }           
        protected abstract double getColdBlockThickness();
        protected abstract double getModuleBoxLength();
        protected abstract double getModuleBoxWidth();
        protected abstract double getModuleBoxHeight();
        protected abstract double get_dist_lower_sensor_edge_to_cold_block_mounting_surface();

        protected void setBoxDim() {
            setBoxDim(getModuleBoxLength(),getModuleBoxHeight(),getModuleBoxWidth());
        }
        protected void setCenter() {
            setCenter(getModuleBoxLength()/2.0-5.0, 0.0, getModuleBoxWidth()/2.0-box_extra_width/5.0); 
        }           
        protected void setPos() {

            if(debug) System.out.printf("%s: setPos for %s\n",this.getClass().getSimpleName(),getName());
            

            if(isBottom()) {
                switch (getLayer()) {
                case 1:
                    ballPos = new BasicHep3Vector(25.0, 661.1, SupportPlateBottom.pedestal_height_L1-SupportPlateBottom.support_plate_pocket_depth);
                    veePos = new BasicHep3Vector(95.0, 661.1, SupportPlateBottom.pedestal_height_L1-SupportPlateBottom.support_plate_pocket_depth);
                    flatPos = new BasicHep3Vector(60.0, 667.10, SupportPlateBottom.pedestal_height_L1-SupportPlateBottom.support_plate_pocket_depth);
                    break;
                case 2:
                    ballPos = new BasicHep3Vector(25.0, 561.1, SupportPlateBottom.pedestal_height_L2-SupportPlateBottom.support_plate_pocket_depth);
                    veePos = new BasicHep3Vector(95.0, 561.1, SupportPlateBottom.pedestal_height_L2-SupportPlateBottom.support_plate_pocket_depth);
                    flatPos = new BasicHep3Vector(60.0, 567.10, SupportPlateBottom.pedestal_height_L2-SupportPlateBottom.support_plate_pocket_depth);   
                    break;
                case 3:
                    ballPos = new BasicHep3Vector(25.0, 461.1, SupportPlateBottom.pedestal_height_L3-SupportPlateBottom.support_plate_pocket_depth);
                    veePos = new BasicHep3Vector(95.0, 461.1, SupportPlateBottom.pedestal_height_L3-SupportPlateBottom.support_plate_pocket_depth);
                    flatPos = new BasicHep3Vector(60.0, 467.10, SupportPlateBottom.pedestal_height_L3-SupportPlateBottom.support_plate_pocket_depth);
                    break;
                case 4:
                    ballPos = new BasicHep3Vector(25.0, 261.1, SupportPlateBottom.pedestal_height_L4-SupportPlateBottom.support_plate_pocket_depth);
                    veePos = new BasicHep3Vector(95.0, 261.1, SupportPlateBottom.pedestal_height_L4-SupportPlateBottom.support_plate_pocket_depth);
                    flatPos = new BasicHep3Vector(60.0, 267.10, SupportPlateBottom.pedestal_height_L4-SupportPlateBottom.support_plate_pocket_depth);
                    break;
                case 5:
                    ballPos = new BasicHep3Vector(25.0, 61.1, SupportPlateBottom.pedestal_height_L5-SupportPlateBottom.support_plate_pocket_depth);
                    veePos = new BasicHep3Vector(95.0, 61.1, SupportPlateBottom.pedestal_height_L5-SupportPlateBottom.support_plate_pocket_depth);
                    flatPos = new BasicHep3Vector(60.0, 67.10, SupportPlateBottom.pedestal_height_L5-SupportPlateBottom.support_plate_pocket_depth);
                    break;
                default:
                    System.out.printf("ERROR invalid layer %d for half %s\n",getLayer(),getHalf());
                    System.exit(1);
                    break;
                }

            } else {
                // top
                // top has a fixed offset of 15mm along plate on module pocket positions w.r.t. bottom
                // top local coordinates is rotation pi around u-vec so need to adjust pocket depth coordinate

                switch (getLayer()) {
                case 1:
                    ballPos = new BasicHep3Vector(25.0, 676.1, -1.0 * (SupportPlateBottom.pedestal_height_L1-SupportPlateBottom.support_plate_pocket_depth));
                    veePos = new BasicHep3Vector(95.0, 676.1, -1.0 * (SupportPlateBottom.pedestal_height_L1-SupportPlateBottom.support_plate_pocket_depth));
                    flatPos = new BasicHep3Vector(60.0, 670.1, -1.0 * (SupportPlateBottom.pedestal_height_L1-SupportPlateBottom.support_plate_pocket_depth));
                    break;
                case 2:
                    ballPos = new BasicHep3Vector(25.0, 576.1, -1.0 * (SupportPlateBottom.pedestal_height_L2-SupportPlateBottom.support_plate_pocket_depth));
                    veePos = new BasicHep3Vector(95.0, 576.1, -1.0 * (SupportPlateBottom.pedestal_height_L2-SupportPlateBottom.support_plate_pocket_depth));
                    flatPos = new BasicHep3Vector(60.0, 570.1, -1.0 * (SupportPlateBottom.pedestal_height_L2-SupportPlateBottom.support_plate_pocket_depth));
                    break;
                case 3:
                    ballPos = new BasicHep3Vector(25.0, 476.1, -1.0 * (SupportPlateBottom.pedestal_height_L3-SupportPlateBottom.support_plate_pocket_depth));
                    veePos = new BasicHep3Vector(95.0, 476.1, -1.0 * (SupportPlateBottom.pedestal_height_L3-SupportPlateBottom.support_plate_pocket_depth));
                    flatPos =new BasicHep3Vector(60.0, 470.1, -1.0 * (SupportPlateBottom.pedestal_height_L3-SupportPlateBottom.support_plate_pocket_depth));
                    break;
                case 4:
                    ballPos = new BasicHep3Vector(25.0, 276.1, -1.0 * (SupportPlateBottom.pedestal_height_L4-SupportPlateBottom.support_plate_pocket_depth));
                    veePos = new BasicHep3Vector(95.0, 276.1, -1.0 * (SupportPlateBottom.pedestal_height_L4-SupportPlateBottom.support_plate_pocket_depth));
                    flatPos = new BasicHep3Vector(60.0, 270.1, -1.0 * (SupportPlateBottom.pedestal_height_L4-SupportPlateBottom.support_plate_pocket_depth));
                    break;
                case 5:
                    ballPos = new BasicHep3Vector(25.0, 76.1, -1.0 * (SupportPlateBottom.pedestal_height_L5-SupportPlateBottom.support_plate_pocket_depth));
                    veePos = new BasicHep3Vector(95.0, 76.1, -1.0 * (SupportPlateBottom.pedestal_height_L5-SupportPlateBottom.support_plate_pocket_depth));
                    flatPos = new BasicHep3Vector(60.0, 70.1, -1.0 * (SupportPlateBottom.pedestal_height_L5-SupportPlateBottom.support_plate_pocket_depth));
                    break;
                default:
                    System.out.printf("ERROR invalid layer %d for half %s\n",getLayer(),getHalf());
                    System.exit(1);
                    break;
                }
            }

            if(debug) {
                System.out.printf("%s: survey positions for %s\n",this.getClass().getSimpleName(),getName());
                printSurveyPos();
            }

            /*
            // walk through the reference volumes
            if(referenceGeom!=null) {
                for(SurveyVolume ref : referenceGeom) {

                    if(debug) {
                        System.out.printf("%s: survey positions before ref %s transform\n",this.getClass().getSimpleName(),ref.getName());
                        printSurveyPos();
                    }

                    if(debug) System.out.printf("%s: Ref %s coord\n%s\n",this.getClass().getSimpleName(), ref.getName(),ref.getCoord().toString());

                    ref.getCoord().getTransformation().transform(ballPos);
                    ref.getCoord().getTransformation().transform(veePos);
                    ref.getCoord().getTransformation().transform(flatPos);

                    if(debug) {
                        System.out.printf("%s: survey positions after ref %s transform\n",this.getClass().getSimpleName(),ref.getName());
                        printSurveyPos();
                    }
                }
            }
            */

        }

    }


    public static abstract class BaseModule extends SurveyVolume {
        protected int layer;
        protected String half;

        public BaseModule(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, int layer, String half) {
            super(name, mother, alignmentCorrection);
            setLayer(layer);
            setHalf(half);
            isValid();
        }
        public BaseModule(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, SurveyVolume ref, int layer, String half) {
            super(name, mother, alignmentCorrection, ref);
            setLayer(layer);
            setHalf(half);
            isValid();
        }
        private void isValid() {
            if(half!="bottom" && half!="top") {
                System.out.printf("ERROR invalid half %s for BaseModule\n",half);
                System.exit(1);
            }
        }
        public int getLayer() {
            return layer;
        }
        public void setLayer(int layer) {
            this.layer = layer;
        }

        public String getHalf() {
            return half;
        }

        public void setHalf(String half) {
            this.half = half;
        }

        public boolean isBottom() {
            return getHalf() == "bottom" ? true : false;
        }

    }



    public abstract static class TestRunHalfModule extends BaseModule {

        // Find the coordinate system of the half-modules w.r.t. to the module survey points
        // We are going to know the sensor center position w.r.t. module coordinate system so the half-module 
        // is really just a dummy volume to contain the daughters. Therefore place it at the same place 
        // as where the sensor coordinate system will be to make things simpler.

        // Distance from sensor to CF edge: 180mm
        // Distance from CF edge to screw hole: 30mm
        // Distance from screw hole to edge of cold block: 33.75mm
        // Distance from edge of cold block to hole/ball position: 5mm
        protected static final double dist_sensor_center_to_coldblock_hole_vdir = (180.0 - 30.0 + (33.75 - 5.0)) - Sensor.length/2.0;   
        protected static final double half_module_thickness = TestRunHalfModule.getHybridThickness() + TestRunHalfModule.getCFThickness() + HalfModuleLamination.thickness;
        protected static final double half_module_length = TestRunHalfModule.getCFLength();
        protected static final double half_module_width = 6.83 + Sensor.width;

        protected double stereo_angle = 0.0;

        public TestRunHalfModule(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, int layer, String half) {
            super(name,mother, alignmentCorrection, layer, half);
        }

        protected void setCenter() {
            // Find distance to center in the local coordinate system 
            // Note that this can be different between axial and stereo since the survey positions determine the local coordinate 
            // system now.
            // I'm not sure this feels good but this has to be done somewhere
//            double box_center_local_x =  TestRunHalfModule.getLength()/2.0 - ( (170.00 + 10.00) - Sensor.getSensorLength()/2.0); 
//            double box_center_local_y = -1.0*TestRunHalfModule.getThickness()/2.0 + (TestRunHalfModule.getCFThickness() + HalfModuleLamination.kapton_thickness + Sensor.getSensorThickness()/2.0);
//            double box_center_local_z = TestRunHalfModule.getWidth()/2.0 - ( 12.66 - (8.83 -3.00) + Sensor.width/2.0 ); 
            
            double box_center_local_x =  TestRunHalfModule.getLength()/2.0 - ( (170.00 + 10.00) - Sensor.length/2.0); 
            double box_center_local_y = - Sensor.getSensorThickness()/2.0 - HalfModuleLamination.thickness - CarbonFiber.thickness + half_module_thickness/2.0; 
            double box_center_local_z = TestRunHalfModule.getWidth()/2.0 - ( 12.66 - (8.83 -3.00) + Sensor.width/2.0 ); 
            
            
            if(useSiStripsConvention) {
                //setCenter(box_center_local_z, box_center_local_x, box_center_local_y); 
                setCenter(-1.0*box_center_local_z, box_center_local_x, box_center_local_y); 
            } else {
                setCenter(box_center_local_x, box_center_local_y, box_center_local_z); 
            }
        }
        protected void setBoxDim() {
            //setBoxDim(getLength(), getThickness(), getWidth());
            
            if(useSiStripsConvention) {
                setBoxDim(getWidth(),getLength(),getThickness());
                //setBoxDim(getSensorWidth(),getSensorLength(),getSensorThickness());
            } else {
                setBoxDim(getLength(), getThickness(), getWidth());
                //setBoxDim(getSensorLength(),getSensorThickness(),getSensorWidth());
            }
            
        }
        protected double getStereoAngle() {
            return stereo_angle;
        }
        protected void setStereoAngle(double stereo_angle) {
            this.stereo_angle = stereo_angle;
        }
        public static double getCFThickness() {
            return CarbonFiber.thickness;
        }
        public static double getCFLength() {
            return CarbonFiber.length;
        }
        public static double getCFWidth() {
            return CarbonFiber.width;
        }
        public static double getHybridLength() {
            return Hybrid.hybrid_length;
        }
        public static double getHybridWidth() {
            return Hybrid.hybrid_width;
        }
        public static double getHybridThickness() {
            return Hybrid.hybrid_thickness;
        }
        public static double getThickness() {
            return half_module_thickness;
        }
        public static double getLength() {
            return half_module_length;
        }
        public static double getWidth() {
            return half_module_width;
        }


    }


    public static class TestRunHalfModuleAxial extends TestRunHalfModule {

        public TestRunHalfModuleAxial(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, int layer, String half) {
            super(name, mother, alignmentCorrection, layer, half);
            init();
        }

        protected void setPos() {
            if(debug) System.out.printf("%s: setPos for %s\n",this.getClass().getSimpleName(),getName());
            
            final double coldBlockThick = getLayer() <=3 ? TestRunColdBlockL13.coldblock_L13_thickness : TestRunColdBlockL45.coldblock_L45_thickness;
            final double dist_lower_sensor_edge_to_cold_block_mounting_surface = getLayer() <=3 ? TestRunModuleL13.dist_lower_sensor_edge_to_cold_block_mounting_surface : TestRunModuleL45.dist_lower_sensor_edge_to_cold_block_mounting_surface;

            double ball_pos_halfmod_local_x =  dist_sensor_center_to_coldblock_hole_vdir;
            double ball_pos_halfmod_local_y =  -1.0* (coldBlockThick/2.0 + TestRunHalfModule.getCFThickness() + HalfModuleLamination.thickness + Sensor.getSensorThickness()/2.0);
            if(useFakeHalfModuleAxialPos) {
                ball_pos_halfmod_local_x = ball_pos_halfmod_local_x*2.0;
                ball_pos_halfmod_local_y = -2.0*ball_pos_halfmod_local_y;
            }               
            final double ball_pos_halfmod_local_z =  dist_lower_sensor_edge_to_cold_block_mounting_surface + Sensor.width/2.0;
            
            
            double vee_pos_halfmod_local_x;
            double vee_pos_halfmod_local_y;
            double vee_pos_halfmod_local_z;
            double flat_pos_halfmod_local_x;
            double flat_pos_halfmod_local_y;
            double flat_pos_halfmod_local_z;


            if(useSiStripsConvention) {
//                vee_pos_halfmod_local_x =  ball_pos_halfmod_local_x;
//                vee_pos_halfmod_local_y =  ball_pos_halfmod_local_y;
//                vee_pos_halfmod_local_z =  ball_pos_halfmod_local_z + Sensor.width/2.0;
//                flat_pos_halfmod_local_x =  ball_pos_halfmod_local_x + Sensor.getSensorLength()/2.0;
//                flat_pos_halfmod_local_y =  ball_pos_halfmod_local_y;
//                flat_pos_halfmod_local_z =  ball_pos_halfmod_local_z;        
                vee_pos_halfmod_local_x =  ball_pos_halfmod_local_x;
                vee_pos_halfmod_local_y =  ball_pos_halfmod_local_y;
                vee_pos_halfmod_local_z =  ball_pos_halfmod_local_z - Sensor.width/2.0;
                flat_pos_halfmod_local_x =  ball_pos_halfmod_local_x + Sensor.length/2.0;
                flat_pos_halfmod_local_y =  ball_pos_halfmod_local_y;
                flat_pos_halfmod_local_z =  ball_pos_halfmod_local_z;        

            } else {
                vee_pos_halfmod_local_x =  ball_pos_halfmod_local_x + Sensor.length/2.0;
                vee_pos_halfmod_local_y =  ball_pos_halfmod_local_y;
                vee_pos_halfmod_local_z =  ball_pos_halfmod_local_z;
                flat_pos_halfmod_local_x =  ball_pos_halfmod_local_x;
                flat_pos_halfmod_local_y =  ball_pos_halfmod_local_y + Sensor.getSensorThickness()/2.0;
                flat_pos_halfmod_local_z =  ball_pos_halfmod_local_z;        
            }
            ballPos = new BasicHep3Vector(ball_pos_halfmod_local_x, ball_pos_halfmod_local_y, ball_pos_halfmod_local_z);
            veePos = new BasicHep3Vector(vee_pos_halfmod_local_x, vee_pos_halfmod_local_y,vee_pos_halfmod_local_z);
            flatPos = new BasicHep3Vector(flat_pos_halfmod_local_x, flat_pos_halfmod_local_y,flat_pos_halfmod_local_z);
            
            
            if(debug) {
                System.out.printf("%s: survey positions for %s\n",this.getClass().getSimpleName(),getName());
                printSurveyPos();
            }

        }
        
        
        
    }


    public static class TestRunHalfModuleStereo extends TestRunHalfModule {

        public TestRunHalfModuleStereo(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, int layer, String half) {
            super(name, mother, alignmentCorrection, layer, half);
            if(layer<=3) stereo_angle = -0.1;
            else if(layer>=4&&layer<=5) stereo_angle = -0.05;
            else throw new RuntimeException("Layer " + layer + " is not defined.");
            init();
            //setExplicitRotation();
        }

        protected void setPos() {
            
            if(debug) System.out.printf("%s: setPos for %s\n",this.getClass().getSimpleName(),getName());
            
            //very similar to axial, see note below

            final double coldBlockThick = getLayer() <=3 ? TestRunColdBlockL13.coldblock_L13_thickness : TestRunColdBlockL45.coldblock_L45_thickness;
            final double dist_lower_sensor_edge_to_cold_block_mounting_surface = getLayer() <=3 ? TestRunModuleL13.dist_lower_sensor_edge_to_cold_block_mounting_surface : TestRunModuleL45.dist_lower_sensor_edge_to_cold_block_mounting_surface;

//            final double ball_pos_halfmod_local_x =  dist_sensor_center_to_coldblock_hole_vdir;
//            // note minus sign to separate from axial
//            final double ball_pos_halfmod_local_y =  -1.0 * (-1.0* (coldBlockThick/2.0 + TestRunHalfModule.getCFThickness() + HalfModuleLamination.kapton_thickness + Sensor.getSensorThickness()/2.0));
//            final double ball_pos_halfmod_local_z =  dist_lower_sensor_edge_to_cold_block_mounting_surface + Sensor.width/2.0;
//            final double vee_pos_halfmod_local_x =  ball_pos_halfmod_local_x + Sensor.getSensorLength()/2.0;
//            final double vee_pos_halfmod_local_y =  ball_pos_halfmod_local_y;
//            final double vee_pos_halfmod_local_z =  ball_pos_halfmod_local_z;
//            final double flat_pos_halfmod_local_x =  ball_pos_halfmod_local_x;
//            final double flat_pos_halfmod_local_y =  ball_pos_halfmod_local_y + Sensor.getSensorThickness()/2.0;
//            final double flat_pos_halfmod_local_z =  ball_pos_halfmod_local_z;        
//            ballPos = new BasicHep3Vector(ball_pos_halfmod_local_x, ball_pos_halfmod_local_y, ball_pos_halfmod_local_z);
//            veePos = new BasicHep3Vector(vee_pos_halfmod_local_x, vee_pos_halfmod_local_y,vee_pos_halfmod_local_z);
//            flatPos = new BasicHep3Vector(flat_pos_halfmod_local_x, flat_pos_halfmod_local_y,flat_pos_halfmod_local_z);
            
            double ball_pos_halfmod_local_x;
            double ball_pos_halfmod_local_y;
            double ball_pos_halfmod_local_z;
            double vee_pos_halfmod_local_x;
            double vee_pos_halfmod_local_y;
            double vee_pos_halfmod_local_z;
            double flat_pos_halfmod_local_x;
            double flat_pos_halfmod_local_y;
            double flat_pos_halfmod_local_z;

            ball_pos_halfmod_local_x =  dist_sensor_center_to_coldblock_hole_vdir;
            // note minus sign to separate from axial
            ball_pos_halfmod_local_y =  -1.0 * (-1.0* (coldBlockThick/2.0 + TestRunHalfModule.getCFThickness() + HalfModuleLamination.thickness + Sensor.getSensorThickness()/2.0));
            ball_pos_halfmod_local_z =  dist_lower_sensor_edge_to_cold_block_mounting_surface + Sensor.width/2.0;
            
            if(useSiStripsConvention) {

//                vee_pos_halfmod_local_x =  ball_pos_halfmod_local_x ;
//                vee_pos_halfmod_local_y =  ball_pos_halfmod_local_y;
//                vee_pos_halfmod_local_z =  ball_pos_halfmod_local_z + Sensor.width/2.0;
//                flat_pos_halfmod_local_x =  ball_pos_halfmod_local_x + Sensor.getSensorLength()/2.0;
//                flat_pos_halfmod_local_y =  ball_pos_halfmod_local_y;
//                flat_pos_halfmod_local_z =  ball_pos_halfmod_local_z;      



                //                vee_pos_halfmod_local_x =  ball_pos_halfmod_local_x;
                //                vee_pos_halfmod_local_y =  ball_pos_halfmod_local_y;
                //                vee_pos_halfmod_local_z =  ball_pos_halfmod_local_z - Sensor.width/2.0;
                //                flat_pos_halfmod_local_x =  ball_pos_halfmod_local_x + Sensor.getSensorLength()/2.0;
                //                flat_pos_halfmod_local_y =  ball_pos_halfmod_local_y;
                //                flat_pos_halfmod_local_z =  ball_pos_halfmod_local_z;            

                
                vee_pos_halfmod_local_x =  ball_pos_halfmod_local_x;
                vee_pos_halfmod_local_y =  ball_pos_halfmod_local_y;
                vee_pos_halfmod_local_z =  ball_pos_halfmod_local_z - Sensor.width/2.0;
                flat_pos_halfmod_local_x =  ball_pos_halfmod_local_x + Sensor.length/2.0;
                flat_pos_halfmod_local_y =  ball_pos_halfmod_local_y;
                flat_pos_halfmod_local_z =  ball_pos_halfmod_local_z;        
                

            } else {
                
                 vee_pos_halfmod_local_x =  ball_pos_halfmod_local_x + Sensor.length/2.0;
                 vee_pos_halfmod_local_y =  ball_pos_halfmod_local_y;
                 vee_pos_halfmod_local_z =  ball_pos_halfmod_local_z;
                 flat_pos_halfmod_local_x =  ball_pos_halfmod_local_x;
                 flat_pos_halfmod_local_y =  ball_pos_halfmod_local_y + Sensor.getSensorThickness()/2.0;
                 flat_pos_halfmod_local_z =  ball_pos_halfmod_local_z;      
                
                
                
//                
//                vee_pos_halfmod_local_x =  ball_pos_halfmod_local_x + Sensor.getSensorLength()/2.0;
//                vee_pos_halfmod_local_y =  ball_pos_halfmod_local_y;
//                vee_pos_halfmod_local_z =  ball_pos_halfmod_local_z;
//                flat_pos_halfmod_local_x =  ball_pos_halfmod_local_x;
//                flat_pos_halfmod_local_y =  ball_pos_halfmod_local_y + Sensor.getSensorThickness()/2.0;
//                flat_pos_halfmod_local_z =  ball_pos_halfmod_local_z;        
            }
           
            
            
            
            
            ballPos = new BasicHep3Vector(ball_pos_halfmod_local_x, ball_pos_halfmod_local_y, ball_pos_halfmod_local_z);
            veePos = new BasicHep3Vector(vee_pos_halfmod_local_x, vee_pos_halfmod_local_y,vee_pos_halfmod_local_z);
            flatPos = new BasicHep3Vector(flat_pos_halfmod_local_x, flat_pos_halfmod_local_y,flat_pos_halfmod_local_z);
            
            
            if(debug) {
                System.out.printf("%s: survey positions for %s\n",this.getClass().getSimpleName(),getName());
                printSurveyPos();
            }
        
        }





        protected void applyGenericCoordinateSystemCorrections() {
            // Apply whatever corrections we want to the final volume as created
            // Maybe alignment corrections too but should be done in the top level

            // Rotate these into the right place for the stereo
            // My rotations here are active rotations in the mother coordinate system frame
            // Sloppy description of the frame
            // u: direction along long edge of half module i.e. along strips
            // v: normal to sensor plane
            // w: perpendicular to the sensor

            // flip around u 
            Rotation r1 = new Rotation(new Vector3D(1,0,0),Math.PI);
            // apply stereo angle around v
            Rotation r2 = new Rotation(new Vector3D(0,1,0),stereo_angle);
            // Build full rotation
            Rotation r = r2.applyTo(r1);
            //Rotation r = r1;
            if(debug) System.out.printf("%s: Coord before corrections\n%s\n", getClass().getSimpleName(),getCoord().toString());
            if(debug) System.out.printf("%s: box center before corrections\n%s\n", getClass().getSimpleName(),getBoxDim().toString());
            getCoord().rotateApache(r);
            if(debug) System.out.printf("%s: Coord after corrections\n%s\n", getClass().getSimpleName(),getCoord().toString());
            if(debug) System.out.printf("%s: box center after corrections\n%s\n", getClass().getSimpleName(),getBoxDim().toString());


        }


    }

    public static abstract class TestRunColdBlock extends SurveyVolume {        
        private int layer;
        public TestRunColdBlock(String name, SurveyVolume mother, int layer) {
            super(name, mother, null);
            setLayer(layer);
            init();
        }
        protected abstract double getWidth();
        protected abstract double getLength();
        protected abstract double getHeight();
        public int getLayer() {
            return layer;
        }
        public void setLayer(int layer) {
            this.layer = layer;
        }
        protected void setCenter() {
            setCenter(getLength()/2.0, 0.0, getWidth()/2.0); 
        }
        protected void setPos() {
            if(debug) System.out.printf("%s: setPos for %s\n",this.getClass().getSimpleName(),getName());
            
            // cold block position w.r.t. module box coordinate system
            // this is a dummy coordinate system, make it simple
            // edge of cold block on the mounting surface
            final double ball_pos_coldblock_local_x =  -5.00; 
            final double ball_pos_coldblock_local_y =  0.00;
            final double ball_pos_coldblock_local_z =  0.00;
            final double vee_pos_coldblock_local_x =  ball_pos_coldblock_local_x + 1.0; //arbitrary distance 
            final double vee_pos_coldblock_local_y =  ball_pos_coldblock_local_y;  
            final double vee_pos_coldblock_local_z =  ball_pos_coldblock_local_z;  
            final double flat_pos_coldblock_local_x =  ball_pos_coldblock_local_x;  
            final double flat_pos_coldblock_local_y =  ball_pos_coldblock_local_y + 1.0;  //arbitrary distance 
            final double flat_pos_coldblock_local_z =  ball_pos_coldblock_local_z;  
            setBallPos(ball_pos_coldblock_local_x, ball_pos_coldblock_local_y, ball_pos_coldblock_local_z);
            setVeePos(vee_pos_coldblock_local_x, vee_pos_coldblock_local_y,vee_pos_coldblock_local_z);
            setFlatPos(flat_pos_coldblock_local_x, flat_pos_coldblock_local_y,flat_pos_coldblock_local_z);
            
            if(debug) {
                System.out.printf("%s: survey positions for %s\n",this.getClass().getSimpleName(),getName());
                printSurveyPos();
            }
            
        }
        protected void setBoxDim() {
            setBoxDim(getLength(), getHeight(), getWidth());
        }
    }

    public static class TestRunColdBlockL13 extends TestRunColdBlock {          
        protected static final double coldblock_L13_length = 82.00;
        protected static final double coldblock_L13_width = 52.50;
        protected static final double coldblock_L13_thickness = 6.00;

        public TestRunColdBlockL13(String name, SurveyVolume mother, int layer) {
            super(name, mother, layer);
        }
        protected double getWidth() {
            return coldblock_L13_width;
        }
        protected double getLength() {
            return coldblock_L13_length;
        }
        protected double getHeight() {
            return coldblock_L13_thickness;
        }
        protected double getThickness() {
            return getHeight();
        }
    }

    public static class TestRunColdBlockL45 extends TestRunColdBlock {          
        protected static final double coldblock_L45_length = 82.00;
        protected static final double coldblock_L45_width = 51.00;
        protected static final double coldblock_L45_thickness = 6.00;

        public TestRunColdBlockL45(String name, SurveyVolume mother, int layer) {
            super(name, mother, layer);
        }
        protected double getWidth() {
            return coldblock_L45_width;
        }
        protected double getLength() {
            return coldblock_L45_length;
        }
        protected double getHeight() {
            return coldblock_L45_thickness;
        }
        protected double getThickness() {
            return getHeight();
        }
    }


    /**
     * Silicon sensor @SurveyVolume.
     * The coordinate system is located at the same position and orientation as the half-module.
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     *
     */
    public static class Sensor extends HalfModuleComponent {
        static final double length= 100.00; 
        static final double width = 40.34; 
        static final double thickness = 0.32;
        static final double height = thickness;
        public Sensor(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, int id) {
            super(name, mother, alignmentCorrection, id);
            init();
        }
        public static double getSensorThickness() {
            return height;
        }
        protected void setPos() {
            
            if(debug) System.out.printf("%s: setPos for %s\n",this.getClass().getSimpleName(),getName());

            setBallPos(0,0,0);
            setVeePos(ballPos.x() + width/2.0, ballPos.y(), ballPos.z());
            setFlatPos(ballPos.x(),ballPos.y() + length/2.0, ballPos.z());                   
            
//            if(useSiStripsConvention) {
//                setBallPos(0,0,0);
//                setVeePos(ballPos.x(), ballPos.y(), ballPos.z() + getSensorWidth()/2.0);
//                setFlatPos(ballPos.x() + getSensorLength()/2.0,ballPos.y(), ballPos.z());                 
//            } else {
//                setBallPos(0,0,0);
//                setVeePos(ballPos.x() + getSensorLength()/2.0, ballPos.y(), ballPos.z());
//                setFlatPos(ballPos.x(),ballPos.y() + getSensorThickness()/2.0, ballPos.z());
//            }

            if(debug) {
                System.out.printf("%s: survey positions for %s\n",this.getClass().getSimpleName(),getName());
                printSurveyPos();
            }
            
        }
        protected void setCenter() {
            setCenter(0,0,0);
        }
        protected void setBoxDim() {
            if(useSiStripsConvention) {
                setBoxDim(width,length,thickness);
            } else {
                setBoxDim(length,thickness,width);
            }
        }
        protected double getThickness() {
            return thickness;
        }
        protected double getHeigth() {
            return thickness;
        }
        protected double getWidth() {
            return width;
        }
        protected double getLength() {
            return length;
        }           
    }

    /**
     * Active part of the @Sensor @SurveyVolume.
     * The coordinate system is located at the same position and orientation as the sensor.
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     *
     */
    public static class ActiveSensor extends SurveyVolume {
        private static final double length= 98.33;
        private static final double width = 38.3399;
        private static final double thickness = Sensor.thickness;
        public ActiveSensor(String name, SurveyVolume m) {
            super(name, m, null);
            init();
        }
        public static double getActiveSensorLength() {
            return length;
        }
        public static double getActiveSensorWidth() {
            return width;
        }
        public static double getActiveSensorHeight() {
            return thickness;
        }
        public static double getActiveSensorThickness() {
            return getActiveSensorHeight();
        }
        protected void setPos() {
            
            if(debug) System.out.printf("%s: setPos for %s\n",this.getClass().getSimpleName(),getName());

            ballPos = new BasicHep3Vector(0,0,0);
            veePos = new BasicHep3Vector(getActiveSensorWidth()/2.0,0,0);
            flatPos = new BasicHep3Vector(0,getActiveSensorLength()/2.0,0);

//            if(useSiStripsConvention) {
//                ballPos = new BasicHep3Vector(0,0,0);
//                veePos = new BasicHep3Vector(getActiveSensorWidth()/2.0,0,0);
//                flatPos = new BasicHep3Vector(0,getActiveSensorLength()/2.0,0);
//            } else {
//                ballPos = new BasicHep3Vector(0,0,0);
//                veePos = new BasicHep3Vector(getActiveSensorWidth()/2.0,0,0);
//                flatPos = new BasicHep3Vector(0,getActiveSensorLength()/2.0,0);
//            }
            
            if(debug) {
                System.out.printf("%s: survey positions for %s\n",this.getClass().getSimpleName(),getName());
                printSurveyPos();
            }
        }
        protected void setCenter() {
            setCenter(0,0,0);
        }
        protected void setBoxDim() {

            setBoxDim(getActiveSensorWidth(), getActiveSensorLength(), getActiveSensorThickness());

//            if(useSiStripsConvention) {
//                setBoxDim(getActiveSensorWidth(), getActiveSensorLength(), getActiveSensorThickness());
//            } else {
//                setBoxDim(getActiveSensorLength(),getActiveSensorThickness(),getActiveSensorWidth());
//            }
        }
    }

 
    /**
     * Kapton insulation @SurveyVolume for the half-module
     * The coordinate system is located at the same position and orientation as the sensor.
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     *
     */
    public static class HalfModuleLamination extends HalfModuleComponent {
        protected static final double length = Sensor.length; //184.0;
        protected static final double width = Sensor.width - 2.34;//40.0; // -2.0; // width under the sensor, 2mm wider under hybrid.
        protected static final double thickness = 0.050;
        public HalfModuleLamination(String name, SurveyVolume m, int id) {
            super(name, m, null, id);
            init();
        }
        protected void setPos() {

            if(debug) System.out.printf("%s: setPos for %s\n",this.getClass().getSimpleName(),getName());

            //             double ball_pos_kapton_local_x =  -1 * (180.0 - Sensor.getSensorLength()/2.0) + 8.5;
            //             double ball_pos_kapton_local_y =  (Sensor.getSensorThickness()/2.0 + HalfModuleLamination.kapton_thickness/2.0);
            //             double ball_pos_kapton_local_z = -1 * (Sensor.width/2.0 + 12.66) + 8.83 - 3.00 + 6.00 ;
            //             double vee_pos_kapton_local_x =  ball_pos_kapton_local_x + 1.0; // arbitrary distance
            //             double vee_pos_kapton_local_y =  ball_pos_kapton_local_y;
            //             double vee_pos_kapton_local_z =  ball_pos_kapton_local_z;
            //             double flat_pos_kapton_local_x =  ball_pos_kapton_local_x;
            //             double flat_pos_kapton_local_y =  ball_pos_kapton_local_y + HalfModuleLamination.kapton_thickness/2.0; // arbitrary distance
            //             double flat_pos_kapton_local_z =  ball_pos_kapton_local_z;

//            double ball_pos_kapton_local_x =  -1 * (Sensor.width/2.0 + 12.66) + 8.83 - 3.00 + 6.00;
//            double ball_pos_kapton_local_y =  -1 * (180.0 - Sensor.getSensorLength()/2.0) + 8.5;
//            double ball_pos_kapton_local_z = (Sensor.getSensorThickness()/2.0 + HalfModuleLamination.kapton_thickness/2.0);
//            double vee_pos_kapton_local_x =  ball_pos_kapton_local_x + Sensor.width/2.0; // arbitrary distance
//            double vee_pos_kapton_local_y =  ball_pos_kapton_local_y;
//            double vee_pos_kapton_local_z =  ball_pos_kapton_local_z;
//            double flat_pos_kapton_local_x =  ball_pos_kapton_local_x;
//            double flat_pos_kapton_local_y =  ball_pos_kapton_local_y + Sensor.getSensorLength(); // arbitrary distance
//            double flat_pos_kapton_local_z =  ball_pos_kapton_local_z;

            //double ball_pos_kapton_local_x =  Sensor.width/2.0 + 6.83 - 6.0 - width/2.0;
            //double ball_pos_kapton_local_y =  Sensor.length/2.0 - 170.0 - 10.0 + 8.5 + length/2.0;
            //double ball_pos_kapton_local_z = -1.0 * (Sensor.getSensorThickness()/2.0 + HalfModuleLamination.thickness/2.0);

            double ball_pos_kapton_local_x =  0;
            double ball_pos_kapton_local_y =  0;
            double ball_pos_kapton_local_z = -1.0 * (Sensor.getSensorThickness()/2.0 + HalfModuleLamination.thickness/2.0);
            
            //ballPos = new BasicHep3Vector(ball_pos_kapton_local_x,ball_pos_kapton_local_y,ball_pos_kapton_local_z);
            //veePos = new BasicHep3Vector(vee_pos_kapton_local_x,vee_pos_kapton_local_y,vee_pos_kapton_local_z);
            //flatPos = new BasicHep3Vector(flat_pos_kapton_local_x,flat_pos_kapton_local_y,flat_pos_kapton_local_z);

            ballPos = new BasicHep3Vector(ball_pos_kapton_local_x,ball_pos_kapton_local_y,ball_pos_kapton_local_z);
            veePos = new BasicHep3Vector(ballPos.x() + 1.0,ballPos.y(),ballPos.z());
            flatPos = new BasicHep3Vector(ballPos.x(),ballPos.y()+ 1.0,ballPos.z());
            
            
            if(debug) {
                System.out.printf("%s: survey positions for %s\n",this.getClass().getSimpleName(),getName());
                printSurveyPos();
            }
        }
        protected void setCenter() {
            setCenter(0.0, 0.0, 0.0);
            //setCenter(getWidth()/2.0, getLength()/2.0,0.0);
            //setCenter(getWidth()/2.0, getLength()/2.0,0.0);
        }
        protected  double getThickness() {
            return thickness;
        }
        protected  double getHeigth() {
            return getThickness();
        }
        protected  double getWidth() {
            return width;
        }
        protected  double getLength() {
            return length;
        }
        protected void setBoxDim() {
            setBoxDim(getWidth(),getLength(),getThickness());
            //setBoxDim(getLength(),getThickness(),getWidth());
        }
    }

    /**
     * Carbon fiber backing @SurveyVolume for the half-module
     * The coordinate system is located at the same position and orientation as the sensor.
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     *
     */
    public static class CarbonFiber extends HalfModuleComponent {
        protected static  final double length = Sensor.length;
        protected static  final double width = 36.02;
        protected static  final double thickness = 0.203;
        public CarbonFiber(String name, SurveyVolume m, int id) {
            super(name, m, null, id);
            init();
        }
        protected void setPos() {
            
            if(debug) System.out.printf("%s: setPos for %s\n",this.getClass().getSimpleName(),getName());
            
//            final double ball_pos_cf_local_x =  -1 * (180.0 - Sensor.getSensorLength()/2.0);
//            final double ball_pos_cf_local_y =  (Sensor.getSensorThickness()/2.0 + HalfModuleLamination.kapton_thickness + TestRunHalfModule.getCFThickness()/2.0);
//            final double ball_pos_cf_local_z = -1 * (Sensor.width/2.0 + 12.66) + 8.83 - 3.00;
//            final double vee_pos_cf_local_x =  ball_pos_cf_local_x + 1.0; // arbitrary distance
//            final double vee_pos_cf_local_y =  ball_pos_cf_local_y;
//            final double vee_pos_cf_local_z =  ball_pos_cf_local_z;
//            final double flat_pos_cf_local_x =  ball_pos_cf_local_x;
//            final double flat_pos_cf_local_y =  ball_pos_cf_local_y + TestRunHalfModule.getCFThickness()/2.0; // arbitrary distance
//            final double flat_pos_cf_local_z =  ball_pos_cf_local_z;
//            setBallPos(ball_pos_cf_local_x,ball_pos_cf_local_y,ball_pos_cf_local_z);
//            setVeePos(vee_pos_cf_local_x,vee_pos_cf_local_y,vee_pos_cf_local_z);
//            setFlatPos(flat_pos_cf_local_x,flat_pos_cf_local_y,flat_pos_cf_local_z);
           
//            final double ball_pos_cf_local_x =  -1 * (Sensor.width/2.0 + 12.66) + 8.83 - 3.00;
//            final double ball_pos_cf_local_y =  -1 * (180.0 - Sensor.getSensorLength()/2.0);
//            final double ball_pos_cf_local_z = (Sensor.getSensorThickness()/2.0 + HalfModuleLamination.kapton_thickness + TestRunHalfModule.getCFThickness()/2.0);
//            final double vee_pos_cf_local_x =  ball_pos_cf_local_x + Sensor.width/2.0; // arbitrary distance
//            final double vee_pos_cf_local_y =  ball_pos_cf_local_y;
//            final double vee_pos_cf_local_z =  ball_pos_cf_local_z;
//            final double flat_pos_cf_local_x =  ball_pos_cf_local_x;
//            final double flat_pos_cf_local_y =  ball_pos_cf_local_y + Sensor.getSensorLength()/2.0; // arbitrary distance
//            final double flat_pos_cf_local_z =  ball_pos_cf_local_z;
//            setBallPos(ball_pos_cf_local_x,ball_pos_cf_local_y,ball_pos_cf_local_z);
//            setVeePos(vee_pos_cf_local_x,vee_pos_cf_local_y,vee_pos_cf_local_z);
//            setFlatPos(flat_pos_cf_local_x,flat_pos_cf_local_y,flat_pos_cf_local_z);
 
            
            //final double ball_pos_cf_local_x =  Sensor.width/2.0 + 6.83 - width/2.0;
            //final double ball_pos_cf_local_y =  Sensor.length/2.0 - 170.0 - 10.0 + length/2.0;
            //final double ball_pos_cf_local_z =  -1 * ( Sensor.getSensorThickness()/2.0 + HalfModuleLamination.thickness + TestRunHalfModule.getCFThickness()/2.0 );

            final double ball_pos_cf_local_x =  0;
            final double ball_pos_cf_local_y =  0;
            final double ball_pos_cf_local_z =  -1 * ( Sensor.getSensorThickness()/2.0 + HalfModuleLamination.thickness + TestRunHalfModule.getCFThickness()/2.0 );
            
            ballPos = new BasicHep3Vector(ball_pos_cf_local_x, ball_pos_cf_local_y, ball_pos_cf_local_z);
            veePos = new BasicHep3Vector(ballPos.x() + 1.0, ballPos.y(), ballPos.z());
            flatPos = new BasicHep3Vector(ballPos.x(), ballPos.y() + 1.0, ballPos.z());
            
            
            
            if(debug) {
                System.out.printf("%s: survey positions for %s\n",this.getClass().getSimpleName(),getName());
                printSurveyPos();
            }
            
        }
        protected void setCenter() {
            setCenter(0.0, 0.0, 0.0);
            //setCenter(getWidth()/2.0, getLength()/2.0, 0.0);
            //setCenter(getLength()/2.0, 0.0, getWidth()/2.0);
        }
        protected double getThickness() {
            return thickness;
        }
        protected double getWidth() {
            return width;
        }
        protected double getLength() {
            return length;
        }
        protected double getHeigth() {
            return getThickness();
        }
        protected void setBoxDim() {
            setBoxDim(getWidth(),getLength(),getThickness());
            //setBoxDim(getLength(),getThickness(),getWidth());
        }
    }

    /**
     * Hybrid @SurveyVolume for the half-module
     * The coordinate system is located at the same position and orientation as the sensor.
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     *
     */
    public static class Hybrid extends HalfModuleComponent {
        protected static final double hybrid_length = 170.0 - Sensor.length; // sensor b-to-b with hybrid
        protected static final double hybrid_width  = Sensor.width;
        protected static final double hybrid_thickness = 4.0/64.0*inch;
        public Hybrid(String name, SurveyVolume m, int id) {
            super(name, m, null, id);
            init();
        }
        protected void setPos() {
            
            if(debug) System.out.printf("%s: setPos for %s\n",this.getClass().getSimpleName(),getName());
            
//            final double ball_pos_hybrid_local_x =  -1 * (170.0 - Sensor.getSensorLength()/2.0);
//            final double ball_pos_hybrid_local_y =  (Sensor.getSensorThickness()/2.0 - TestRunHalfModule.getHybridThickness()/2.0);
//            final double ball_pos_hybrid_local_z = -1 * (Sensor.width/2.0);
//            final double vee_pos_hybrid_local_x =  ball_pos_hybrid_local_x + 1.0; // arbitrary distance
//            final double vee_pos_hybrid_local_y =  ball_pos_hybrid_local_y;
//            final double vee_pos_hybrid_local_z =  ball_pos_hybrid_local_z;
//            final double flat_pos_hybrid_local_x =  ball_pos_hybrid_local_x;
//            final double flat_pos_hybrid_local_y =  ball_pos_hybrid_local_y + TestRunHalfModule.getHybridThickness()/2.0; // arbitrary distance
//            final double flat_pos_hybrid_local_z =  ball_pos_hybrid_local_z;
//            setBallPos(ball_pos_hybrid_local_x,ball_pos_hybrid_local_y,ball_pos_hybrid_local_z);
//            setVeePos(vee_pos_hybrid_local_x,vee_pos_hybrid_local_y,vee_pos_hybrid_local_z);
//            setFlatPos(flat_pos_hybrid_local_x,flat_pos_hybrid_local_y,flat_pos_hybrid_local_z);
            
//            final double ball_pos_hybrid_local_x =  -1 * (Sensor.width/2.0);
//            final double ball_pos_hybrid_local_y =  -1 * (170.0 - Sensor.getSensorLength()/2.0);
//            final double ball_pos_hybrid_local_z = (Sensor.getSensorThickness()/2.0 - TestRunHalfModule.getHybridThickness()/2.0);
//            final double vee_pos_hybrid_local_x =  ball_pos_hybrid_local_x + Sensor.width/2.0; // arbitrary distance
//            final double vee_pos_hybrid_local_y =  ball_pos_hybrid_local_y;
//            final double vee_pos_hybrid_local_z =  ball_pos_hybrid_local_z;
//            final double flat_pos_hybrid_local_x =  ball_pos_hybrid_local_x;
//            final double flat_pos_hybrid_local_y =  ball_pos_hybrid_local_y + Sensor.getSensorLength()/2.0; // arbitrary distance
//            final double flat_pos_hybrid_local_z =  ball_pos_hybrid_local_z;
//            setBallPos(ball_pos_hybrid_local_x,ball_pos_hybrid_local_y,ball_pos_hybrid_local_z);
//            setVeePos(vee_pos_hybrid_local_x,vee_pos_hybrid_local_y,vee_pos_hybrid_local_z);
//            setFlatPos(flat_pos_hybrid_local_x,flat_pos_hybrid_local_y,flat_pos_hybrid_local_z);
            
            final double ball_pos_hybrid_local_x =  0.0;
            final double ball_pos_hybrid_local_y =  Sensor.length/2.0 - 170.0 + hybrid_length/2.0;
            final double ball_pos_hybrid_local_z = -1.0*Sensor.getSensorThickness()/2.0 + hybrid_thickness/2.0;
            
            ballPos = new BasicHep3Vector(ball_pos_hybrid_local_x,ball_pos_hybrid_local_y, ball_pos_hybrid_local_z);
            veePos = new BasicHep3Vector(ballPos.x() + 1.0, ballPos.y(), ballPos.z());
            flatPos = new BasicHep3Vector(ballPos.x(), ballPos.y() + 1.0, ballPos.z());
            
            
            
            if(debug) {
                System.out.printf("%s: survey positions for %s\n",this.getClass().getSimpleName(),getName());
                printSurveyPos();
            }
        }
        protected void setCenter() {
            setCenter(0.0, 0.0, 0.0);
            //setCenter(getWidth()/2.0, getLength()/2.0, 0.0);
            //setCenter(getLength()/2.0, 0.0, getWidth()/2.0);
        }
        protected double getThickness() {
            return hybrid_thickness;
        }
        protected double getHeigth() {
            return getThickness();
        }
        protected double getWidth() {
            return hybrid_width;
        }
        protected double getLength() {
            return hybrid_length;
        }
        protected void setBoxDim() {
            setBoxDim(getWidth(), getLength(),getThickness());
            //setBoxDim(getLength(),getThickness(), getWidth());
        }
    }


    /**
     * Base class for components of a half-module @SurveyVolume
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     *
     */
    public static abstract class HalfModuleComponent extends SurveyVolume {
        int id = -1;
        public HalfModuleComponent(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, int id) {
            super(name, mother, alignmentCorrection);
            this.id = id;
        }
        protected abstract double getThickness();
        protected abstract double getHeigth();
        protected abstract double getWidth();
        protected abstract double getLength();
        public int getId() {
            return id;
        }
    }       

    
    
   

    public static class TestRunHalfModuleBundle extends HalfModuleBundle {
        protected SurveyVolume carbonFiber = null;
        protected SurveyVolume hybrid = null;
        TestRunHalfModuleBundle(SurveyVolume hm) {         
            super(hm);
        }
    }


    protected TestRunHalfModuleAxial createTestRunHalfModuleAxial(String volName,
            BaseModule mother, AlignmentCorrection alignmentCorrection,
            int layer, String half) {
      return new TestRunHalfModuleAxial(volName, mother, alignmentCorrection, layer, half);
        
    }


    protected TestRunHalfModuleStereo createTestRunHalfModuleStereo(
            String volName, BaseModule mother,
            AlignmentCorrection alignmentCorrection, int layer, String half) {
        return new TestRunHalfModuleStereo(volName, mother, alignmentCorrection, layer, half);
    }


    /* (non-Javadoc)
     * @see org.lcsim.geometry.compact.converter.HPSTrackerGeometryDefinition#getHalfModuleBundle(org.lcsim.geometry.compact.converter.HPSTestRunTracker2014GeometryDefinition.BaseModule, java.lang.String)
     */
    protected HalfModuleBundle getHalfModuleBundle(BaseModule module, String halfModuleName) {
        BaseModuleBundle m = getModuleBundle(module.getLayer(), module.getHalf());
        HalfModuleBundle hm = null;
        // TODO this needs to change when I build quarter-modules for the long half-modules.
        if(m!=null) {
            if( m instanceof TestRunModuleBundle) {   
                TestRunModuleBundle mtr = (TestRunModuleBundle) m;
                if(halfModuleName.contains("axial")) {
                    hm = mtr.halfModuleAxial;
                }
                else if(halfModuleName.contains("stereo")) {
                    hm = mtr.halfModuleStereo;
                }
                else {
                    throw new RuntimeException("No axial or stereo string found in half module bundle name " + halfModuleName);
                }
            }
            else {
                throw new RuntimeException("The type of this module bundle is incorrect. Should be a TestRunModuleBundle.");
            }
        } else {
            throw new RuntimeException("No module found for " + module.getLayer() + " and half " + module.getHalf());
        }
        return hm;
    }


    /* (non-Javadoc)
     * @see org.lcsim.geometry.compact.converter.HPSTrackerBuilder#getOldGeomDefLayerFromVolumeName(java.lang.String)
     */
    public int getOldGeomDefLayerFromVolumeName(String name) {
        
        String half = getHalfFromName(name);
        int l = getLayerFromVolumeName(name);
        boolean isTopLayer = false;
        if(half=="top") isTopLayer=true;
        else if(half=="bottom") isTopLayer = false;
        else throw new RuntimeException("no half found from " + name);
        boolean isAxial = isAxialFromName(name);
        return getOldLayerDefinition(isTopLayer, l, isAxial);
    }

    /* (non-Javadoc)
     * @see org.lcsim.geometry.compact.converter.HPSTrackerBuilder#getOldLayerDefinition(boolean, int, boolean)
     */
    public int getOldLayerDefinition(boolean isTopLayer, int l, boolean isAxial) {
        int layer=-1;
        if(isAxial) {
            if(isTopLayer) {
                layer = 2*l-1;
            }
            else {
                layer = 2*l;
            }
        } else {
            if(isTopLayer) {
                layer = 2*l;
            } else {
                layer = 2*l-1;
            }
        }
        return layer;
    }


    
    /* (non-Javadoc)
     * @see org.lcsim.geometry.compact.converter.HPSTrackerBuilder#getMillepedeLayer(java.lang.String)
     */
    public int getMillepedeLayer(String name) {
       return getOldGeomDefLayerFromVolumeName(name);
    }
    




}




