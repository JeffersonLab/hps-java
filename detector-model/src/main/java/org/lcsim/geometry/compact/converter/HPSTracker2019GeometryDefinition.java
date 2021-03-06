package org.lcsim.geometry.compact.converter;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.logging.Logger;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.jdom.Element;
import org.lcsim.geometry.compact.converter.HPSTestRunTracker2014GeometryDefinition.BaseModule;
import org.lcsim.geometry.compact.converter.HPSTestRunTracker2014GeometryDefinition.BaseSensor;
//import org.lcsim.geometry.compact.converter.HPSTestRunTracker2014GeometryDefinition.TestRunHalfModuleBundle;
// import org.lcsim.geometry.compact.converter.HPSTestRunTracker2014GeometryDefinition.HalfModuleComponent;
// import org.lcsim.geometry.compact.converter.HPSTestRunTracker2014GeometryDefinition.Sensor;
import org.lcsim.geometry.compact.converter.HPSTrackerGeometryDefinition.TestRunModuleBundle;
import org.lcsim.detector.Transform3D;
import org.lcsim.detector.Rotation3D;
import org.lcsim.detector.Translation3D;

/**
 * Updated geometry information for the HPS tracker 2019
 * 
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 */
public class HPSTracker2019GeometryDefinition extends HPSTracker2014v1GeometryDefinition {
    
    boolean applySurvey     = true;
    boolean applyDesign     = true;
    boolean apply2019Survey = true;

    private static final Logger LOGGER = Logger
            .getLogger(HPSTracker2019GeometryDefinition.class.getPackage().getName());

    public HPSTracker2019GeometryDefinition(boolean debug, Element node) {
        super(debug, node);
        layerBitMask = 0x7F;
        doTop = true;
        doStereo = true;
        
        if (System.getProperty("disableSurvey") != null) {
            applySurvey = false;
        }
        
        if (System.getProperty("disableDesign") != null) {
            applyDesign = false;
        }
        
        if (System.getProperty("disable2019Survey") != null) {
            apply2019Survey = false;
        }

        System.out.println("PF::DEBUG:: APPLY SURVEY CONSTANTS::" + applySurvey);
        System.out.println("PF::DEBUG:: APPLY DESIGN CORRECTIONS::" + applyDesign);
        System.out.println("PF::DEBUG:: APPLY 2019 SURVEY CORRECTIONS::" + apply2019Survey);
        
    }

    @Override
    public void build() {

        // Create alignment correction objects
        // THis is really a ugly approach with MP corrections initialized before
        // and
        // the survey corrections based on the XML node
        // FIX THIS! //TODO
        
        AlignmentCorrection alignmentCorrections = new AlignmentCorrection();
        alignmentCorrections.setNode(node);
        
        //AlignmentCorrection supBotCorr = getL13UChannelAlignmentCorrection(false);
        AlignmentCorrection supBotCorr = getUChannelCorrection(false,80);
        supBotCorr.setNode(node);
        
        //AlignmentCorrection supTopCorr = this.getL13UChannelAlignmentCorrection(true);
        AlignmentCorrection supTopCorr = getUChannelCorrection(true,80);
        supTopCorr.setNode(node);

        AlignmentCorrection supBotCorrBack = getUChannelCorrection(false,90);
        supBotCorrBack.setNode(node);
        AlignmentCorrection supTopCorrBack = getUChannelCorrection(true,90);
        supTopCorr.setNode(node);
        
        
        
        // Build the geometry from the basic building blocks in the geometry
        // definition class
        // Keep the order correct.
        // Each item has knowledge of its mother but not its daughters
        HPSTrackerGeometryDefinition.TrackingVolume tracking = new HPSTrackerGeometryDefinition.TrackingVolume(
                "trackingVolume", null);
        surveyVolumes.add(tracking);
        
        //System.out.println("PF::Constructed TrackingVolume: " +  tracking.toString());
        
        PSVacuumChamber chamber = new PSVacuumChamber("c_hamber", tracking, null);
        surveyVolumes.add(chamber);

        //System.out.println("PF::Constructed PSVacuumChamber: " +  chamber.toString());

        SvtBox svtBox = new SvtBox("base", chamber, null);
        surveyVolumes.add(svtBox);

        //System.out.println("PF::Constructed svtBox: " +  svtBox.toString());

        SvtBoxBasePlate svtBoxBasePlate = new SvtBoxBasePlate("base_plate", svtBox, null);
        surveyVolumes.add(svtBoxBasePlate);

        SupportRingL13BottomKinMount supportRingKinL13Bottom = new SupportRingL13BottomKinMount("c_support_kin_L13b",
                svtBox, supBotCorr);
        surveyVolumes.add(supportRingKinL13Bottom);
        
        //System.out.println("PF::Constructed SupportRingKinL13Bottom: " +  supportRingKinL13Bottom.toString());

        //LOGGER.info("Construct uChannelL14Bottom");
        

        UChannelL13 uChannelL14Bottom = new UChannelL14Bottom("support_bottom_L14", svtBox, alignmentCorrections,
                supportRingKinL13Bottom);
        surveyVolumes.add(uChannelL14Bottom);
        
        //System.out.println("Constructed uChannelL14Bottom: " + uChannelL14Bottom.toString());

        //LOGGER.info("Construct uChannelL14BottomPlate");
        
        UChannelL14Plate uChannelL14BottomPlate = new UChannelL14BottomPlate("support_plate_bottom_L14", svtBox, null,
                uChannelL14Bottom);
        surveyVolumes.add(uChannelL14BottomPlate);
        
        //System.out.println("Constructed uChannelL14BottomPlate: " + uChannelL14BottomPlate.toString());
        
        //LOGGER.info("Constructed uChannelL14BottomPlate: " + uChannelL14BottomPlate.toString());

        SupportRingL13TopKinMount supportRingKinL13Top = new SupportRingL13TopKinMount("c_support_kin_L13t", svtBox,
                supTopCorr);
        surveyVolumes.add(supportRingKinL13Top);
        //System.out.println("PF::Constructed supportRingKinL13Top: " +  supportRingKinL13Top.toString());
        

        UChannelL13 uChannelL14Top = new UChannelL14Top("support_top_L14", svtBox, alignmentCorrections,
                supportRingKinL13Top);
        surveyVolumes.add(uChannelL14Top);
        
        //System.out.println("Constructed uChannelL14Top: " + uChannelL14Top.toString());

        UChannelL14Plate uChannelL14TopPlate = new UChannelL14TopPlate("support_plate_top_L14", svtBox, null,
                                                                       uChannelL14Top);
        surveyVolumes.add(uChannelL14TopPlate);
        
        //System.out.println("Constructed uChannelL14TopPlate: " + uChannelL14TopPlate.toString());

        UChannelL46 uChannelL46Bottom = new UChannelL46Bottom("support_bottom_L46", svtBox, supBotCorrBack);
        surveyVolumes.add(uChannelL46Bottom);

        UChannelL46Plate uChannelL46BottomPlate = new UChannelL46BottomPlate("support_plate_bottom_L46", svtBox, null,
                uChannelL46Bottom);
        surveyVolumes.add(uChannelL46BottomPlate);

        UChannelL46 uChannelL46Top = new UChannelL46Top("support_top_L46", svtBox, supTopCorrBack);
        surveyVolumes.add(uChannelL46Top);

        UChannelL46Plate uChannelL46TopPlate = new UChannelL46TopPlate("support_plate_top_L46", svtBox, null,
                uChannelL46Top);
        surveyVolumes.add(uChannelL46TopPlate);

        LOGGER.info("Construct modules");
        //PF::This is where the geometry hierarchy is broken and the tree becomes counter-intuitive. 
        
        for (int l = 1; l <= 7; ++l) {
            if (doLayer(l)) {
                LOGGER.info("Construct layer " + l + " modules");

                final SurveyVolume moduleMother = getSurveyVolume(SvtBox.class);

                SurveyVolume moduleRef;

                if (doBottom) {
                    if (l < 5)
                        moduleRef = getSurveyVolume(UChannelL14Bottom.class);
                    else
                        moduleRef = getSurveyVolume(UChannelL46Bottom.class);

                    LOGGER.info("PF:: Make the bundle for layer " + l + " bottom with mother " + moduleMother.getName() + " and ref " + moduleRef.getName());
                    makeModuleBundle(l, "bottom", moduleMother, moduleRef);
                }
                
                if (doTop) {
                    if (l < 5)
                        moduleRef = getSurveyVolume(UChannelL14Top.class);
                    else
                        moduleRef = getSurveyVolume(UChannelL46Top.class);
                    
                    LOGGER.info("Make the bundle for layer " + l + " top with mother " + moduleMother.getName() + " and ref " +moduleRef.getName());
                    makeModuleBundle(l, "top", moduleMother, moduleRef);
                }
            }
        }
        
        LOGGER.info(String.format("%s: Constructed %d geometry objects", this.getClass().getSimpleName(),
                surveyVolumes.size()));
        LOGGER.info(String.format("%s: Constructed %d module bundles", this.getClass().getSimpleName(), modules.size()));

        if (isDebug() || true) {
            System.out.printf("%s: DONE constructing the geometry objects\n", this.getClass().getSimpleName());
            System.out.printf("%s: List of the survey volumes built\n", this.getClass().getSimpleName());
            for (SurveyVolume bg : surveyVolumes) {
                System.out.printf("-------\n%s\n", bg.toString());
            }
        }
        if (isDebug()) {
            System.out.printf("%s: List of the module bundles built\n", this.getClass().getSimpleName());
            for (BaseModuleBundle bundle : this.modules) {
                bundle.print();
            }
        }

    }

    /*
     * (non-Javadoc)
     * @see org.lcsim.geometry.compact.converter.HPSTracker2014GeometryDefinition #makeModuleBundle(int, java.lang.String, org.lcsim.geometry.compact.converter.SurveyVolume, org.lcsim.geometry.compact.converter.SurveyVolume)
     */
    @Override
    protected void makeModuleBundle(int layer, String half, SurveyVolume mother, SurveyVolume ref) {

        LOGGER.info("makeModule for layer " + layer + " " + half);

        // Create the module
        BaseModule module = createModule(half, layer, mother, ref);
                
        BaseModuleBundle bundle;

        if (layer <= 2) {
            //bundle = new ShortModuleBundle(module);
            bundle = new ShortModuleBundleOneSensor(module);
            addModuleBundle(bundle);
            if (doAxial) {
                makeShortHalfModule("axial", module);
            }
            if (doStereo) {
                makeShortHalfModule("stereo", module);
            }
        } else if (layer > 2 && layer <= 4) {
            bundle = new TestRunModuleBundle(module);
            addModuleBundle(bundle);
            if (doAxial)
                makeHalfModule("axial", module);
            if (doStereo)
                makeHalfModule("stereo", module);
        } else {
            bundle = new LongModuleBundle(module);
            addModuleBundle(bundle);
            if (doAxial) {
                makeLongHalfModule("axial", "hole", module);
                makeLongHalfModule("axial", "slot", module);
            }
            if (doStereo) {
                makeLongHalfModule("stereo", "hole", module);
                makeLongHalfModule("stereo", "slot", module);
            }
        }

        LOGGER.info("created module bundle:\n" + bundle.toString() + "\n" + "Now there are " + modules.size()
                + " modules");

    }

    /*
     * (non-Javadoc)
     * @see org.lcsim.geometry.compact.converter.HPSTracker2014GeometryDefinition #createModule(java.lang.String, int, org.lcsim.geometry.compact.converter.SurveyVolume, org.lcsim.geometry.compact.converter.SurveyVolume)
     */
    @Override
    protected BaseModule createModule(String half, int layer, SurveyVolume mother, SurveyVolume ref) {

        // build the module name
        String volName = "module_L" + layer + (half == "bottom" ? "b" : "t");
        boolean isTopLayer = true;
        if (half=="bottom")
            isTopLayer=false;

        // find alignment corrections
        // The modules will have layer ID starting from 61 to 67. 
        AlignmentCorrection alignmentCorrection = getHalfModuleAlignmentCorrection(isTopLayer,60+layer);
        alignmentCorrection.setNode(node);

        BaseModule module;
        if (half == "bottom") {
            switch (layer) {
                case 1:
                    module = new ModuleL1Bot(volName, mother, alignmentCorrection, ref);
                    break;
                case 2:
                    module = new ModuleL2Bot(volName, mother, alignmentCorrection, ref);
                    break;
                case 3:
                    module = new ModuleL3Bot(volName, mother, alignmentCorrection, ref);
                    break;
                case 4:
                    module = new ModuleL4Bot(volName, mother, alignmentCorrection, ref);
                    break;
                case 5:
                    module = new ModuleL5Bot(volName, mother, alignmentCorrection, ref);
                    break;
                case 6:
                    module = new ModuleL6Bot(volName, mother, alignmentCorrection, ref);
                    break;
                case 7:
                    module = new ModuleL7Bot(volName, mother, alignmentCorrection, ref);
                    break;
                default:
                    throw new IllegalArgumentException("Can't make layer " + layer);
            }
        } else {
            switch (layer) {
                case 1:
                    module = new ModuleL1Top(volName, mother, alignmentCorrection, ref,applyDesign);
                    break;
                case 2:
                    module = new ModuleL2Top(volName, mother, alignmentCorrection, ref,applyDesign);
                    break;
                case 3:
                    module = new ModuleL3Top(volName, mother, alignmentCorrection, ref);
                    break;
                case 4:
                    module = new ModuleL4Top(volName, mother, alignmentCorrection, ref);
                    break;
                case 5:
                    module = new ModuleL5Top(volName, mother, alignmentCorrection, ref);
                    break;
                case 6:
                    module = new ModuleL6Top(volName, mother, alignmentCorrection, ref);
                    break;
                case 7:
                    module = new ModuleL7Top(volName, mother, alignmentCorrection, ref);
                    break;
                default:
                    throw new IllegalArgumentException("Can't make layer " + layer);
            }
        }
        return module;
    }

    public abstract static class UChannelL14Plate extends HPSTracker2014GeometryDefinition.UChannelL13Plate {

        protected final static double length = HPSTracker2014GeometryDefinition.UChannelL13Plate.length + 50.0;
        protected final static double height = HPSTracker2014GeometryDefinition.UChannelL13Plate.height;
        protected static final double width = HPSTracker2014GeometryDefinition.UChannelL13Plate.width;

        public UChannelL14Plate(String name, SurveyVolume m, AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, m, alignmentCorrection, ref);
        }

        @Override
        public double getLength() {
            return length;
        }

        @Override
        public double getHeight() {
            return height;
        }

        @Override
        public double getWidth() {
            return width;
        }

    }

    public static abstract class UChannelL14 extends HPSTracker2014GeometryDefinition.UChannelL13 {

        protected static final double length = UChannelL14BottomPlate.length;
        private static final double width = UChannelL14BottomPlate.width;
        protected static final double height = HPSTracker2014GeometryDefinition.UChannelL13.height;
        protected static final double side_plate_cone_y = HPSTracker2014GeometryDefinition.UChannelL13.side_plate_cone_y;

        public UChannelL14(String name, SurveyVolume m, AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, m, alignmentCorrection, ref);
        }

        @Override
        protected double getLength() {
            return length;
        }

        @Override
        protected double getWidth() {
            return width;
        }

        @Override
        protected double getHeight() {
            return height;
        }

    }

    public static class UChannelL14BottomPlate extends UChannelL14Plate {

        public UChannelL14BottomPlate(String name, SurveyVolume m, AlignmentCorrection alignmentCorrection,
                SurveyVolume ref) {
            super(name, m, alignmentCorrection, ref);
            init();
        }

        @Override
        protected void setCenter() {

            final double x = 0.0;
            double y = UChannelL14Bottom.cone_to_edge_of_plate_y - getLength() / 2.0;
            // with coordinate system 'y' pointing from L1 to L3 we want to
            // subtract the difference in length
            y -= UChannelL14Bottom.length_diff;
            final double z = -UChannelL13.side_plate_cone_y - getHeight() / 2.0;
            setCenter(x, y, z);
        }

    }

    public static class UChannelL14Bottom extends UChannelL13Bottom {

        protected static final double length = UChannelL14BottomPlate.length;
        private static final double width = UChannelL14BottomPlate.width;
        protected static final double height = HPSTracker2014GeometryDefinition.UChannelL13.height;
        // this length need to be longer by the difference in length
        protected static final double length_diff = length - HPSTracker2014GeometryDefinition.UChannelL13Bottom.length;
        protected static final double cone_to_edge_of_plate_y = HPSTracker2014GeometryDefinition.UChannelL13Bottom.cone_to_edge_of_plate_y
                + length_diff;

        public UChannelL14Bottom(String name, SurveyVolume m, AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, m, alignmentCorrection, ref);
        }

        protected void setCenter() {
            final double x = 0.0;
            double y = cone_to_edge_of_plate_y - getLength() / 2.0;
            // with coordinate system 'y' pointing from L1 to L3 we want to
            // subtract the difference in length
            y -= UChannelL14Bottom.length_diff;
            final double z = -side_plate_cone_y - UChannelL14Plate.height + getHeight() / 2.0;
            setCenter(x, y, z);
        }

        @Override
        protected double getLength() {
            return length;
        }

        @Override
        protected double getWidth() {
            return width;
        }

        @Override
        protected double getHeight() {
            return height;
        }

    }

    public static class UChannelL14TopPlate extends UChannelL14Plate {

        public UChannelL14TopPlate(String name, SurveyVolume m, AlignmentCorrection alignmentCorrection,
                SurveyVolume ref) {
            super(name, m, alignmentCorrection, ref);
            init();
        }

        @Override
        protected void setCenter() {

            final double x = 0.0;
            double y = UChannelL14Top.cone_to_edge_of_plate_y - getLength() / 2.0;
            // with coordinate system 'y' pointing from L1 to L3 we want to
            // subtract the difference in length
            y -= UChannelL14Top.length_diff;
            final double z = -UChannelL13.side_plate_cone_y - getHeight() / 2.0;
            setCenter(x, y, z);
        }

    }

    public static class UChannelL14Top extends UChannelL13Top {

        protected static final double length = UChannelL14TopPlate.length;
        private static final double width = UChannelL14TopPlate.width;
        protected static final double height = HPSTracker2014GeometryDefinition.UChannelL13.height;
        // this length need to be longer by the difference in length
        protected static final double length_diff = length - HPSTracker2014GeometryDefinition.UChannelL13Top.length;
        protected static final double cone_to_edge_of_plate_y = HPSTracker2014GeometryDefinition.UChannelL13Top.cone_to_edge_of_plate_y + length_diff;

        public UChannelL14Top(String name, SurveyVolume m, AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, m, alignmentCorrection, ref);
        }

        protected void setCenter() {
            final double x = 0.0;
            double y = cone_to_edge_of_plate_y - getLength() / 2.0;
            // with coordinate system 'y' pointing from L1 to L3 we want to
            // subtract the difference in length
            y -= UChannelL14Top.length_diff;
            final double z = -side_plate_cone_y - UChannelL14Plate.height + getHeight() / 2.0;
            setCenter(x, y, z);
        }

        @Override
        protected double getLength() {
            return length;
        }

        @Override
        protected double getWidth() {
            return width;
        }

        @Override
        protected double getHeight() {
            return height;
        }
        
    }

    public static class ModuleL1Top extends ShortModuleTop {

        public ModuleL1Top(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            this(name,mother,alignmentCorrection,ref,false);
            init();
        }
        

        public ModuleL1Top(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, SurveyVolume ref, boolean applyDesign) {
            super(name, mother, alignmentCorrection, ref);

            // position this module w.r.t. previous L1 by shifting it along the
            // channel
            // note flip
            shift_along_uchannel      = -50.;
            shift_across_uchannel     = applyDesign ? -0.062372 : 0.0;//4.81;
            shift_vertically_uchannel = 0.0;
            
            
            // note flip wr.t. bottom
            cone_to_hole_along_uchannel         = HPSTracker2014GeometryDefinition.ModuleL1Top.cone_to_hole_along_uchannel   + shift_along_uchannel;
            cone_to_hole_across_uchannel        = HPSTracker2014GeometryDefinition.ModuleL1Top.cone_to_hole_across_uchannel  + shift_across_uchannel; 
            cone_to_hole_vertical_from_uchannel = HPSTracker2014GeometryDefinition.ModuleL1Top.cone_to_hole_vertical_from_uchannel + shift_vertically_uchannel;
            init();
        }
    }
        
    public static class ModuleL2Top extends ShortModuleTop {
        
        // Note the 2014 Geometry definitions of ModuleL1 measures are used here
        public ModuleL2Top(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            this(name,mother,alignmentCorrection,ref,false);
            
        }
        
        public ModuleL2Top(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, SurveyVolume ref, boolean applyDesign) {
            super(name, mother, alignmentCorrection, ref);
            
            shift_along_uchannel      = 0;
            shift_across_uchannel     = -0.250;
            shift_vertically_uchannel = applyDesign ? -0.381 : -0.300; 

            cone_to_hole_along_uchannel         = HPSTracker2014GeometryDefinition.ModuleL1Top.cone_to_hole_along_uchannel + shift_along_uchannel;
            cone_to_hole_across_uchannel        = HPSTracker2014GeometryDefinition.ModuleL1Top.cone_to_hole_across_uchannel + shift_across_uchannel; // change x position layer 1 bot
            cone_to_hole_vertical_from_uchannel = HPSTracker2014GeometryDefinition.ModuleL1Top.cone_to_hole_vertical_from_uchannel + shift_vertically_uchannel;
            
            init();
        }
    }

    public static class ModuleL3Bot extends ModuleL13Bot {

        // Note the L2 measures are used here
        protected final static double cone_to_hole_along_uchannel = HPSTracker2014GeometryDefinition.ModuleL2Bot.cone_to_hole_along_uchannel;
        protected final static double cone_to_hole_vertical_from_uchannel = HPSTracker2014GeometryDefinition.ModuleL2Bot.cone_to_hole_vertical_from_uchannel;
        protected final static double L3_new_vertical_shift = 0.7 - 0.012;

        public ModuleL3Bot(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, mother, alignmentCorrection, ref);
            init();
        }

        protected Hep3Vector getHolePosition() {
            double x = cone_to_hole_across_uchannel;
            double y = cone_to_hole_along_uchannel;
            double z = cone_to_hole_vertical_from_uchannel + L3_new_vertical_shift;
            return new BasicHep3Vector(x, y, z);
        }

    }

    
    
    /*
    //This class is an intermediate class that only works as way to override the applySurvey 
    //in 2019 data taking run. It is necessary because the constants are different
    //NOT CALLED AT THE MOMENT
    public static class ModuleL13Top2019 extends ModuleL13Top {
        
        @Override 
        protected void applySurvey(Element node) {
            
            debug = true;
            if (debug)
                System.out.println("######### PF :: DEBUG :: "+ this.getClass().getSimpleName() + " APPLY SURVEY CONSTANTS NEW NEW NEW");
            
            if (node == null) {
                if (debug)
                    System.out.printf("PF::DEBUG %s: WARNING: no XML file for NEW NEW NEW survey information available.\n", this.getClass().getSimpleName());
            } else { 
                
                SurveyResult surveyResult = SurveyResult.findResultFromDetector(node,getName());
                if (surveyResult != null) {
                    if (debug) 
                        System.out.printf("PF:: DEBUG:: %s: found NEW NEW NEW survey results: \n%s \n", this.getClass().getSimpleName(), surveyResult.toString());
                }
                
                //Half sensors
                if (HPSTrackerBuilder.isHalfModule(getName())) {
                    
                    
                    //Get the reference volume (should be the UChannel support only)
                    SurveyVolume uchannel_ref = null;
                    if (getMother().getReferenceGeom() != null) {
                        uchannel_ref = getMother().getReferenceGeom().get(0);
                    }
                    else {
                        System.out.println("PF::DEBUG-- WARNING WARNING:: NEW NEW NEW no mother"); 
                        return;
                    }
                    
                    if (uchannel_ref == null) {
                        System.out.println("PF::DEBUG-- WARNING WARNING:: NEW NEW NEW ref vol not found"); 
                        return;
                    } 
                    
                    else
                        System.out.println("PF::DEBUG NEW NEW NEW Found Reference frame::" + uchannel_ref.getName());
                    
                    
                    //Get the local to global transformation of the uchannel frame
                    Transform3D ref_transform_l2g = uchannel_ref.getl2gTransform();
                    Transform3D ref_transform_g2l = uchannel_ref.getl2gTransform().inverse();
                    
                    
                    //Get the global to local transformation from jLab to the mother module of the sensor
                    Transform3D mother_transform_G2L = getMother().getl2gTransform().inverse();
                    System.out.println("PF::MOTHER " + getMother().getName() + "G2L transform" );
                    System.out.println(mother_transform_G2L);
                    
                    //Get the surveyed position
                    Hep3Vector surveyedSensorPosition = surveyResult.getOrigin();
                    // Adjust origin to the sensor center  -> Not sure if this is needed. For sure is not correct for the thin sensors. Commented out and left just for reference
                    //surveyResult.setOrigin(VecOp.add(surveyResult.getOrigin(), VecOp.mult(-0.160, surveyResult.getZ()))); 
                    
                    //These sensors only have one reliable measurement: the position along the UChannel. To apply this properly
                    //1) Transform the sensor coordinate in the hps-java UChannel
                    
                    Hep3Vector sensorOrigin = getCoord().origin();
                    getMother().getl2gTransform().transform(sensorOrigin);
                    
                    System.out.println("PF::SENSOR POSITION IN GLOBAL FRAME "+ getMother().getName() + " " +getName());
                    System.out.println(sensorOrigin);
                    
                    
                    //2) Transform the sensor coordinate to the uchannel
                    ref_transform_g2l.transform(sensorOrigin);
                    
                    System.out.println("PF::SENSOR POSITION IN UCHANNEL FRAME "+ getMother().getName() + " " +getName());
                    System.out.println(sensorOrigin);
                    
                    //3) The Y component of the sensorOrigin in this frame should now be set to the surveyed X position
                    Hep3Vector surveyedOrigin = new BasicHep3Vector(sensorOrigin.x(), surveyResult.getOrigin().x(),sensorOrigin.z());
                    
                    System.out.println("PF::SURVEYED SENSOR POSITION IN SURVEY UCHANNEL FRAME "+ getMother().getName() + " " +getName());
                    System.out.println(surveyResult.getOrigin());
                    
                    System.out.println("PF::SURVEYED SENSOR POSITION IN UCHANNEL FRAME "+ getMother().getName() + " " +getName());
                    System.out.println(surveyedOrigin);
                    
                    //4) Transform back into the mother frame
                    
                    ref_transform_l2g.transform(surveyedOrigin);
                    mother_transform_G2L.transform(surveyedOrigin);
                    
                    //5) Translate to surveyed position
                    
                    Translation3D transToSurvey = new Translation3D(VecOp.sub(surveyedOrigin, getCoord().origin()));
                    getCoord().translate(transToSurvey);
                    
                    System.out.println("PF::SURVEYED SENSOR POSITION IN MODULE FRAME "+ getMother().getName() + " " +getName());
                    System.out.println(getCoord().origin());
                    
                    System.out.println("PF::SURVEYED SENSOR POSITION IN GLOBAL FRAME "+ getMother().getName() + " " +getName());
                    getMother().getl2gTransform().transform(surveyedOrigin);
                    System.out.println(surveyedOrigin);
                } // is an half module
            } // found the survey node
        } //applySurvey
    }
    */

    public static class ModuleL3Top extends ModuleL13Top {

        // Note the L2 measures are used here
        protected final static double cone_to_hole_along_uchannel = HPSTracker2014GeometryDefinition.ModuleL2Top.cone_to_hole_along_uchannel;
        protected final static double cone_to_hole_vertical_from_uchannel = HPSTracker2014GeometryDefinition.ModuleL2Top.cone_to_hole_vertical_from_uchannel;
        protected final static double L3_new_vertical_shift = 0.7 + 0.055;

        public ModuleL3Top(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, mother, alignmentCorrection, ref);
            init();
        }

        protected Hep3Vector getHolePosition() {
            double x = cone_to_hole_across_uchannel;
            double y = cone_to_hole_along_uchannel;
            double z = cone_to_hole_vertical_from_uchannel + L3_new_vertical_shift;
            return new BasicHep3Vector(x, y, z);
        }
    }

    public static class ModuleL4Bot extends ModuleL13Bot {

        // Note the L2 measures are used here
        protected final static double cone_to_hole_along_uchannel = HPSTracker2014GeometryDefinition.ModuleL3Bot.cone_to_hole_along_uchannel;
        protected final static double cone_to_hole_vertical_from_uchannel = HPSTracker2014GeometryDefinition.ModuleL3Bot.cone_to_hole_vertical_from_uchannel;
        protected final static double L4_new_vertical_shift = 0.7 + 0.0356;

        public ModuleL4Bot(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, mother, alignmentCorrection, ref);
            init();
        }

        protected Hep3Vector getHolePosition() {
            double x = cone_to_hole_across_uchannel;
            double y = cone_to_hole_along_uchannel;
            double z = cone_to_hole_vertical_from_uchannel + L4_new_vertical_shift;
            return new BasicHep3Vector(x, y, z);
        }
    }
       
    public static class ModuleL4Top extends ModuleL13Top {

        // Note the L2 measures are used here
        protected final static double cone_to_hole_along_uchannel = HPSTracker2014GeometryDefinition.ModuleL3Top.cone_to_hole_along_uchannel;
        protected final static double cone_to_hole_vertical_from_uchannel = HPSTracker2014GeometryDefinition.ModuleL3Top.cone_to_hole_vertical_from_uchannel;
        protected final static double L4_new_vertical_shift = 0.7 + 0.062;

        public ModuleL4Top(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, mother, alignmentCorrection, ref);
            init();
        }

        protected Hep3Vector getHolePosition() {
            double x = cone_to_hole_across_uchannel;
            double y = cone_to_hole_along_uchannel;
            double z = cone_to_hole_vertical_from_uchannel + L4_new_vertical_shift;
            return new BasicHep3Vector(x, y, z);
        }

    }

    public static class ModuleL5Bot extends HPSTracker2014GeometryDefinition.ModuleL4Bot {

        public ModuleL5Bot(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, mother, alignmentCorrection, ref);
        }
    }

    public static class ModuleL5Top extends HPSTracker2014GeometryDefinition.ModuleL4Top {

        public ModuleL5Top(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, mother, alignmentCorrection, ref);
        }
    }

    public static class ModuleL6Bot extends HPSTracker2014GeometryDefinition.ModuleL5Bot {

        public ModuleL6Bot(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, mother, alignmentCorrection, ref);
        }
    }

    public static class ModuleL6Top extends HPSTracker2014GeometryDefinition.ModuleL5Top {

        public ModuleL6Top(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, mother, alignmentCorrection, ref);
        }
    }

    public static class ModuleL7Bot extends HPSTracker2014GeometryDefinition.ModuleL6Bot {

        public ModuleL7Bot(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, mother, alignmentCorrection, ref);
        }
    }

    public static class ModuleL7Top extends HPSTracker2014GeometryDefinition.ModuleL6Top {

        public ModuleL7Top(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, mother, alignmentCorrection, ref);
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.lcsim.geometry.compact.converter.HPSTracker2014GeometryDefinition #getMillepedeLayer(java.lang.String)
     * This function gets the millepedeLayer for a structure from the name. There is an hardcode for 
     * module structures, i.e. module_1b, module_2t ... where the millepede id is set to 60 + X
     * This is a workaround to use the same function but avoid checking sensor-only properties
     */
    
    @Override
    public int getMillepedeLayer(String name) {

        if (isModule(name)) { 
            
            int mpid = getLayerFromVolumeName(name) + 60;
            //System.out.printf("PF::The MPII ID For %s is %d \n",name,mpid);
            return mpid;                
        }
        
        boolean isTopLayer = getHalfFromName(name).equals("top") ? true : false;

        // find layer
        int layer = getLayerFromVolumeName(name);

        // axial or stereo
        boolean isAxial = isAxialFromName(name);

        // use default layer numbering for L1-4
        if (layer < 5) {
            return getOldLayerDefinition(isTopLayer, layer, isAxial);
        }

        // hole or slot
        boolean isHole = isHoleFromName(name);

        return getMillepedeLayer(isTopLayer, layer, isAxial, isHole);

    }

    /**
     * Silicon sensor @SurveyVolume. The coordinate system is located at the same position and orientation as the half-module.
     * 
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     */
    public static class ShortSensor extends BaseSensor {

        // static final double length= 10.00 + 1.00;
        //static final double length = 10.00;
        //static final double length = 10.00*2;
        static final double length = 15.00*2;
        //static final double width = 14.080 + 2 * 0.250;
        static final double width = 14.025 + 2 * 0.250;
        static final double thickness = 0.200; // 0.250;
        static final double height = thickness;

        public ShortSensor(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, int id) {
            super(name, mother, alignmentCorrection, id);
            init();
        }

        public static double getSensorThickness() {
            return height;
        }

        @Override
        protected void setPos() {

            if (debug)
                System.out.printf("%s: setPos for %s\n", this.getClass().getSimpleName(), getName());

            setBallPos(0, 0, 0);
            setVeePos(ballPos.x() + width / 2.0, ballPos.y(), ballPos.z());
            setFlatPos(ballPos.x(), ballPos.y() + length / 2.0, ballPos.z());

            if (debug) {
                System.out.printf("%s: survey positions for %s\n", this.getClass().getSimpleName(), getName());
                printSurveyPos();
            }

        }

        @Override
        protected void setCenter() {
            setCenter(0, 0, 0);
        }

        @Override
        protected void setBoxDim() {
            if (useSiStripsConvention) {
                setBoxDim(width, length, thickness);
            } else {
                setBoxDim(length, thickness, width);
            }
        }

        @Override
        protected double getThickness() {
            return thickness;
        }

        @Override
        protected double getHeigth() {
            return thickness;
        }

        @Override
        protected double getWidth() {
            return width;
        }

        @Override
        protected double getLength() {
            return length;
        }
    }

    /**
     * Active part of the @ShortSensor @SurveyVolume. The coordinate system is located at the same position and orientation as the sensor.
     * 
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     */
    public static class ActiveShortSensor extends SurveyVolume {

        // private static final double length = ShortSensor.length - (100.00 -
        // 98.33);
        private static final double length = ShortSensor.length;
        private static final double width = ShortSensor.width - 2 * 0.250;
        //private static final double width = ShortSensor.width - 2 * 0.250 - 0.055; //subtract strip length since we have n - 1 strips
        private static final double thickness = ShortSensor.thickness;

        public ActiveShortSensor(String name, SurveyVolume m) {
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

            if (debug)
                System.out.printf("%s: setPos for %s\n", this.getClass().getSimpleName(), getName());

            ballPos = new BasicHep3Vector(0, 0, 0);
            veePos = new BasicHep3Vector(getActiveSensorWidth() / 2.0, 0, 0);
            flatPos = new BasicHep3Vector(0, getActiveSensorLength() / 2.0, 0);

            if (debug) {
                System.out.printf("%s: survey positions for %s\n", this.getClass().getSimpleName(), getName());
                printSurveyPos();
            }
        }

        protected void setCenter() {
            setCenter(0, 0, 0);
        }

        protected void setBoxDim() {

            setBoxDim(getActiveSensorWidth(), getActiveSensorLength(), getActiveSensorThickness());

        }
    }

    /**
     * Abstract {@link SurveyVolume} volume defining the coordinate system of module L4-6
     * 
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     */
    public abstract static class ShortModule extends BaseModule {
        
        //Original 2019MRSolt separation
        protected final static double distance_between_stereo_axial_norm_dir  = 0.299 * inch;
        
        //Recomputed separation by Tim (2021/02/25)
        //.188" (support) + 0.100" (2 * glue fixture pedestal)  + 0.010" (2*teflon tape [McMaster 6305A18]) + 0.002" (2*thermal compound) + 0.200 mm (silicon thickness) 
        protected final static double distance_between_stereo_axial_norm_dir_design = 7.82; //[mm]

        // OLD STUFF MOSTLY
        // PF:: This is unused
        protected final static double hole_to_center_of_plate_width_dir = 5.875 * inch;

        //PF:: This is unused
        protected final static double hole_to_module_edge_height_dir = 0.875 * inch;
        
        protected static final double hole_to_module_edge_length_dir = 0.25 * inch;
        

        //Ad-hoc shifts to place the sensors
        protected double shift_along_uchannel; //positive is downstream
        protected double shift_across_uchannel; //positive is beam left
        protected double shift_vertically_uchannel; //positive is towards beam 
                
        //General placement of the sensors
        protected double cone_to_hole_along_uchannel;
        protected double cone_to_hole_across_uchannel;
        protected double cone_to_hole_vertical_from_uchannel;
        
        //These initialised values are "random" - TODO temporary?
        private final static double box_extra_length = 0.0;
        private final static double box_extra_height = -0.45 * inch;
        private final static double box_extra_width = 0.5 * inch;

        private static final double tension_lever_y = 2.5 * inch;
        
        // TODO the dimension of the L4-6 module is completely made up
        public static final double length = 12.25 * inch + box_extra_length;
        public static final double height = 1.0 * inch + box_extra_height;
        public static final double width = tension_lever_y + 0.04 * inch + box_extra_width;

        public ShortModule(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, mother, alignmentCorrection, ref, getLayerFromVolumeName(name), getHalfFromName(name));

        }
        
        //Since the dimensioon of the modules is made up, this method is not meaningful
        protected void setCenter() {
            final double x = -width / 2.0;
            final double y = -hole_to_module_edge_length_dir + length / 2.0;
            final double z = -Math.abs(getHoleModuleCenterOffset());
            // final double z = -hole_to_module_edge_height_dir + height/2.0;
            setCenter(x, y, z);
        }
        
        protected void setBoxDim() {
            setBoxDim(width, length, height);
        }

        protected abstract void setPos();

        protected abstract double getHoleModuleCenterOffset();

        protected Hep3Vector getHole() {
            return new BasicHep3Vector(cone_to_hole_across_uchannel, cone_to_hole_along_uchannel, cone_to_hole_vertical_from_uchannel);
        }
    }

    /**
     * Abstract {@link SurveyVolume} volume defining the coordinate system of bottom modules for L4-6 Reference: @UChannelL46Bottom coordinate system Origin: hole position on mounting surface (electron side) Orientation: u - is normal to the mounting surface pointing vertically down, v - points along module towards positron side.
     * 
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     */
    public static abstract class ShortModuleBot extends ShortModule {
        
        // positions are in the mother (U-channel) coord. system as usual
        
        public ShortModuleBot(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection,
                SurveyVolume ref) {
            super(name, mother, alignmentCorrection, ref);
            
        }
        
        @Override
        protected void setPos() {
            ballPos = getHole();
            veePos = new BasicHep3Vector(ballPos.x(), ballPos.y(), ballPos.z() - 1.0);
            flatPos = new BasicHep3Vector(ballPos.x() - 1.0, ballPos.y(), ballPos.z());
        }
        
        @Override
        protected double getHoleModuleCenterOffset() {
            return UChannelL46Bottom.cone_to_edge_of_plate_y - UChannelL46BottomPlate.L4_module_pin_to_edge_of_plate;
        }
        
    }

    /**
     * Abstract {@link SurveyVolume} volume defining the coordinate system of top modules for L4-6 Reference: @UChannelL46Top coordinate system Origin: hole position on mounting surface (electron side when installed) Orientation: u - is normal to the mounting surface pointing vertically down, v - points along module towards electron side when installed.
     * 
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     */
    public static abstract class ShortModuleTop extends ShortModule {
        
        //PF:: These coordinates are NOT used anywhere. => Should clean up
        
        //protected final static double x = -149.225; // distance from survey ball
                                                    // to hole mounting surface
        //protected final static double y = -9.525; // distance along U-channel
        //protected final static double z = -53.34; // distance normal to the
                                                  // U-channel plate
        
        public ShortModuleTop(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection,
                SurveyVolume ref) {
            super(name, mother, alignmentCorrection, ref);
        }

        @Override
        protected void setPos() {
            ballPos = getHole();
            veePos = new BasicHep3Vector(ballPos.x(), ballPos.y(), ballPos.z() - 1.0);
            flatPos = new BasicHep3Vector(ballPos.x() + 1.0, ballPos.y(), ballPos.z());
        }
        
        @Override
        protected double getHoleModuleCenterOffset() {
            return UChannelL46Top.cone_to_edge_of_plate_y - UChannelL46TopPlate.L4_module_pin_to_edge_of_plate;
        }
    }

    
    //PF:: As it is now, the only information that this class 
    // position this module w.r.t. previous L1 by shifting it along the
    // channel
    public static class ModuleL1Bot extends ShortModuleBot {

        public ModuleL1Bot(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            this(name, mother, alignmentCorrection, ref, false);
        }

        public ModuleL1Bot(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, SurveyVolume ref, boolean applyDesign) {
            super(name, mother, alignmentCorrection, ref);
            
            shift_along_uchannel = -50.;
            shift_vertically_uchannel = 0.0;
            shift_across_uchannel = 0;
            
            cone_to_hole_along_uchannel = HPSTracker2014v1GeometryDefinition.ModuleL1Bot.cone_to_hole_along_uchannel + shift_along_uchannel;
            cone_to_hole_across_uchannel = HPSTracker2014v1GeometryDefinition.ModuleL1Bot.cone_to_hole_across_uchannel + shift_across_uchannel; // change x position layer 1 bot 
            cone_to_hole_vertical_from_uchannel = HPSTracker2014v1GeometryDefinition.ModuleL1Bot.cone_to_hole_vertical_from_uchannel + shift_vertically_uchannel;
            
            init();
        }
    }

    //public static class ModuleL2Bot extends ModuleL13Bot {
    public static class ModuleL2Bot extends ShortModuleBot {

        public ModuleL2Bot(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            this(name, mother, alignmentCorrection, ref, false);
        }
                
        public ModuleL2Bot(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, SurveyVolume ref, boolean applyDesign) {
            super(name, mother, alignmentCorrection, ref);
            
            shift_along_uchannel = 0; 
            shift_across_uchannel = 0.250; 
            shift_vertically_uchannel = applyDesign ? -0.381 : -0.300; 
            
            cone_to_hole_along_uchannel = HPSTracker2014GeometryDefinition.ModuleL1Bot.cone_to_hole_along_uchannel + shift_along_uchannel;
            cone_to_hole_across_uchannel = HPSTracker2014v1GeometryDefinition.ModuleL1Bot.cone_to_hole_across_uchannel + shift_across_uchannel; // change x position layer 1 bot
            cone_to_hole_vertical_from_uchannel = HPSTracker2014GeometryDefinition.ModuleL1Bot.cone_to_hole_vertical_from_uchannel + shift_vertically_uchannel;

            init();
        }

    }

    /**
     * Create the half-module.
     * 
     * @param side - stereo or axial
     * @param type - hole or slot
     * @param mother to the half-module
     */
    
    protected void makeShortHalfModule(String side, BaseModule mother) {

        String moduleName = mother.getName();
        
        if (isDebug() || true)
            System.out.printf("PF:: DEBUG :: %s: makeHalfModule for %s %s %s \n", this.getClass().getSimpleName(), moduleName, side,mother.getName());

        String volName = moduleName + "_halfmodule_" + side;
        
        // top or bottom?
        String half = mother.getHalf();
        boolean isTopLayer = !mother.isBottom();

        // find layer
        int layer = mother.getLayer();
        
        // axial or stereo
        boolean isAxial = isAxialFromName(volName);
        
        //Make Millepede Layer always hole for short sensor
        //boolean isHole = true;

        // find layer according to Millepede layer definition
        //int millepedeLayer = getMillepedeLayer(isTopLayer, layer, isAxial, isHole);
        int millepedeLayer = getMillepedeLayer(volName);

        // find alignment correction to this volume
        AlignmentCorrection alignmentCorrection = getHalfModuleAlignmentCorrection(isTopLayer, millepedeLayer);
        alignmentCorrection.setNode(node);

        // find the module bundle that it will be added to
        // TestRunModuleBundle bundle =
        // (TestRunModuleBundle)getModuleBundle(mother);
        // TestRunHalfModuleBundle halfModuleBundle;
        ShortModuleBundleOneSensor bundle = (ShortModuleBundleOneSensor) getModuleBundle(mother);
        //TestRunModuleBundle bundle = (TestRunModuleBundle) getModuleBundle(mother);

        //Get the reference volume of the mother (to know what UChannel this sensor belongs to)
        SurveyVolume uchannel_ref = null;
        
        if (mother.getReferenceGeom() != null && mother.getReferenceGeom().size() > 0) {
            uchannel_ref = mother.getReferenceGeom().get(0);
            System.out.println("PF::DEBUG::MakeShortHalfModule reference volume:\n" + uchannel_ref.getName());
        }        
        
        
        // Build the half-module bundle and half-module
        // TODO clean this up to a separate method
        ShortHalfModule halfModule;
        HalfModuleBundle halfModuleBundle;
        if (isAxial) {
            if(layer == 1){
                if(isTopLayer){
                    halfModuleBundle = new ShortHalfModuleBundle();
                    halfModule = new ShortAxialHoleHalfModuleL0Top(volName, mother, alignmentCorrection, layer, half,applySurvey);
                    bundle.halfModuleAxial = halfModuleBundle;
                }
                else{
                    halfModuleBundle = new ShortHalfModuleBundle();
                    halfModule = new ShortAxialHoleHalfModuleL0Bot(volName, mother, alignmentCorrection, layer, half,applySurvey);
                    bundle.halfModuleAxial = halfModuleBundle;
                }
            }
            else if(layer == 2){
                if(isTopLayer){
                    halfModuleBundle = new ShortHalfModuleBundle();
                    halfModule = new ShortAxialHoleHalfModuleL1Top(volName, mother, alignmentCorrection, layer, half,applySurvey);
                    bundle.halfModuleAxial = halfModuleBundle;
                }
                else{
                    halfModuleBundle = new ShortHalfModuleBundle();
                    halfModule = new ShortAxialHoleHalfModuleL1Bot(volName, mother, alignmentCorrection, layer, half,applySurvey);
                    bundle.halfModuleAxial = halfModuleBundle;
                }
            }
            else{
                throw new RuntimeException("Short modules should only apply to L0 and L1 modules. Check geometry code.");
            }
        } else {
            if(layer == 1){
                if(isTopLayer){
                    halfModuleBundle = new ShortHalfModuleBundle();
                    halfModule = new ShortStereoHoleHalfModuleL0Top(volName, mother, alignmentCorrection, layer, half,applySurvey);
                    bundle.halfModuleStereo = halfModuleBundle;
                }
                else{
                    halfModuleBundle = new ShortHalfModuleBundle();
                    halfModule = new ShortStereoHoleHalfModuleL0Bot(volName, mother, alignmentCorrection, layer, half,applySurvey);
                    bundle.halfModuleStereo = halfModuleBundle;
                }
            }
            else if(layer == 2){
                if(isTopLayer){
                    halfModuleBundle = new ShortHalfModuleBundle();
                    halfModule = new ShortStereoHoleHalfModuleL1Top(volName, mother, alignmentCorrection, layer, half,applySurvey);
                    bundle.halfModuleStereo = halfModuleBundle;
                }
                else{
                    halfModuleBundle = new ShortHalfModuleBundle();
                    halfModule = new ShortStereoHoleHalfModuleL1Bot(volName, mother, alignmentCorrection, layer, half,applySurvey);
                    bundle.halfModuleStereo = halfModuleBundle;
                }
            }
            else{
                throw new RuntimeException("Short modules should only apply to L0 and L1 modules. Check geometry code.");
            }
        }
        halfModuleBundle.halfModule = halfModule;

        // create the half module components
        makeShortHalfModuleComponentSensor(halfModule);
        // makeShortHalfModuleComponentKapton(halfModule);
        // makeHalfModuleComponentCF(halfModule);
        // makeHalfModuleComponentHybrid(halfModule);

    }
    
    protected void makeShortHalfModuleComponentSensor(BaseModule mother) {

        if (isDebug())
            System.out.printf("%s: makeHalfModuleComponentSensor for %s \n", this.getClass().getSimpleName(),
                    mother.getName());

        String volName = mother.getName() + "_sensor";

        // sensor id is hard coded in old geometry to be zero by counting over
        // the components of the module
        int component_number = 0;

        //
        ShortSensor sensor = new ShortSensor(volName, mother, null, component_number);
        sensor.setMaterial("Silicon");

        HalfModuleBundle hm = getHalfModuleBundle((BaseModule) mother.getMother(), mother.getName());
        hm.sensor = sensor;

        makeShortHalfModuleComponentActiveSensor(sensor);

    }

    private void makeShortHalfModuleComponentActiveSensor(ShortSensor mother) {

        if (isDebug())
            System.out.printf("%s: makeHalfModuleComponentActiveSensor for %s \n", this.getClass().getSimpleName(),
                    mother.getName());

        String volName = mother.getName() + "_active";

        ActiveShortSensor active_sensor = new ActiveShortSensor(volName, mother);
        active_sensor.setMaterial("Silicon");

        HalfModuleBundle hm = getHalfModuleBundle((BaseModule) mother.getMother().getMother(), mother.getMother()
                .getName());
        hm.activeSensor = active_sensor;

    }

    protected void makeShortHalfModuleComponentKapton(BaseModule mother) {

        if (isDebug())
            System.out.printf("%s: makeHalfModuleComponentKapton for %s \n", this.getClass().getSimpleName(),
                    mother.getName());

        String volName = mother.getName() + "_lamination";

        // Build the half-module

        // id is hard coded
        int component_number = 2;

        HalfShortModuleLamination lamination = new HalfShortModuleLamination(volName, mother, component_number);
        lamination.setMaterial("Kapton");

        HalfModuleBundle hm = getHalfModuleBundle((BaseModule) mother.getMother(), mother.getName());
        hm.lamination = lamination;

    }

    /**
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     */
    public static class ShortHalfModuleBundle extends LongHalfModuleBundle {

        public ShortHalfModuleBundle() {
            super();
        }

        public ShortHalfModuleBundle(SurveyVolume hm) {
            super(hm);
        }

    }
    
    /*public static class ShortHalfModuleBundleOneSensor extends TestRunHalfModuleBundle {
        protected static SurveyVolume carbonFiber = null;
    
        public ShortHalfModuleBundleOneSensor() {
            super(carbonFiber);
        }

        public ShortHalfModuleBundleOneSensor(SurveyVolume hm) {
            super(hm);
        }
    }*/

    /**
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     */
    public static class ShortModuleBundle extends LongModuleBundle {

        public ShortModuleBundle(BaseModule m) {
            super(m);
        }

    }
    
    public static class ShortModuleBundleOneSensor extends TestRunModuleBundle {

       // public HalfModuleBundle halfModuleStereo = null;
        //public HalfModuleBundle halfModuleAxial = null;
        //protected SurveyVolume coldBlock = null;

        public ShortModuleBundleOneSensor(BaseModule m) {
            super(m);
        }

        /*public void print() {
            if (module != null)
                System.out.printf("%s: %s\n", this.getClass().getSimpleName(), module.toString());
            if (halfModuleAxial != null)
                halfModuleAxial.print();
            if (coldBlock != null)
                System.out.printf("%s: %s\n", this.getClass().getSimpleName(), coldBlock.getName());
            if (halfModuleStereo != null)
                halfModuleStereo.print();
        }*/

    }

    public static class HalfShortModuleLamination extends HPSTestRunTracker2014GeometryDefinition.HalfModuleComponent {

        protected static final double width = ShortSensor.width;
        protected static final double length = ShortSensor.length;
        protected static final double height = 0.050;

        public HalfShortModuleLamination(String name, SurveyVolume mother, int id) {
            super(name, mother, null, id);
            init();
        }

        protected double getThickness() {
            return height;
        }

        protected double getHeigth() {
            return height;
        }

        protected double getWidth() {
            return width;
        }

        protected double getLength() {
            return length;
        }

        protected void setPos() {
            // offset enough to make them face-to-face
            ballPos = new BasicHep3Vector(0, 0, -(ShortSensor.getSensorThickness() / 2.0 + height / 2.0));
            veePos = new BasicHep3Vector(ballPos.x() + 1, ballPos.y(), ballPos.z());
            flatPos = new BasicHep3Vector(ballPos.x(), ballPos.y() + 1, ballPos.z());
        }

        protected void setCenter() {
            setCenter(0, 0, 0);
        }

        protected void setBoxDim() {
            setBoxDim(width, length, height);
        }
    }

    public abstract static class ShortHalfModule extends BaseModule {
        
        // private static final double randomoffset = 5.0;
        public double width = ShortSensor.width; // + randomoffset;
        public double length = ShortSensor.length;// +
                                                               // randomoffset/10.0;
        public double height = ShortSensor.height;// +
                                                               // HalfLongModuleLamination.height;
        protected double sensor_z = 0.23 * inch;

        public ShortHalfModule(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, int layer,
                String half) {
            super(name, mother, alignmentCorrection, layer, half);
        }

        protected abstract Hep3Vector getSensorPosition();

        protected void setBoxDim() {
            System.out.println("PF:: DEBUG::" + getName() + "Sensor Box Dim width="+width+" length="+length+" height=" + height);
            setBoxDim(width, length, height);
        }

        protected void setCenter() {
            double x = 0.0;
            double y = 0.0;
            //PF::This is 0 by definition.
            //double z = +0.5 * ShortSensor.height - height / 2.0;
            double z = 0.0;
            System.out.println("PF:: DEBUG::" + getName() + "Sensor Center x="+x+" y="+y+" z=" + z);
            setCenter(x, y, z);
        }

        protected void setPos() {
            ballPos = getSensorPosition(); // TODO make this get each coordinate
                                           // instead.
            veePos = new BasicHep3Vector(ballPos.x() + 1, ballPos.y(), ballPos.z());
            flatPos = new BasicHep3Vector(ballPos.x(), ballPos.y() + 1, ballPos.z());
        }

        @Override
        protected void applySurvey(Element node) {
            
            debug = true;
            if (debug)
                System.out.println("######### PF :: DEBUG :: "+ this.getClass().getSimpleName() + " APPLY SURVEY CONSTANTS NEW NEW NEW");
            
            if (node == null) {
                if (debug)
                    System.out.printf("PF::DEBUG %s: WARNING: no XML file for NEW NEW NEW survey information available.\n", this.getClass().getSimpleName());
            } else { 
                
                SurveyResult surveyResult = SurveyResult.findResultFromDetector(node,getName());
                if (surveyResult != null) {
                    if (debug) 
                        System.out.printf("PF:: DEBUG:: %s: found NEW NEW NEW survey results: \n%s \n", this.getClass().getSimpleName(), surveyResult.toString());
                }
                else 
                    return;
                
                                
                //Half sensors
                if (HPSTrackerBuilder.isHalfModule(getName())) {


                    //Get the reference volume (should be the UChannel support only)
                    SurveyVolume uchannel_ref = null;
                    if (getMother().getReferenceGeom() != null) {
                        uchannel_ref = getMother().getReferenceGeom().get(0);
                    }
                    else {
                        System.out.println("PF::DEBUG-- WARNING WARNING:: NEW NEW NEW no mother"); 
                        return;
                    }
                    
                    if (uchannel_ref == null) {
                        System.out.println("PF::DEBUG-- WARNING WARNING:: NEW NEW NEW ref vol not found"); 
                        return;
                    } 
                    
                    else
                        System.out.println("PF::DEBUG NEW NEW NEW Found Reference frame::" + uchannel_ref.getName());
                    
                    
                    //Get the local to global transformation of the uchannel frame
                    Transform3D ref_transform_l2g = uchannel_ref.getl2gTransform();
                    Transform3D ref_transform_g2l = uchannel_ref.getl2gTransform().inverse();
                    
                    
                    //Get the global to local transformation from jLab to the mother module of the sensor
                    Transform3D mother_transform_G2L = getMother().getl2gTransform().inverse();
                    System.out.println("PF::MOTHER " + getMother().getName() + "G2L transform" );
                    System.out.println(mother_transform_G2L);

                    //Get the surveyed position
                    Hep3Vector surveyedSensorPosition = surveyResult.getOrigin();
                    // Adjust origin to the sensor center  -> Not sure if this is needed. For sure is not correct for the thin sensors. Commented out and left just for reference
                    //surveyResult.setOrigin(VecOp.add(surveyResult.getOrigin(), VecOp.mult(-0.160, surveyResult.getZ()))); 
                        
                    
                    
                    //Thin sensors only
                    if (getName().contains("1") || getName().contains("2")) {
                        
                        
                        if (debug)
                            System.out.printf("%s: NEW NEW NEW treating it as a half-module\n", this.getClass().getSimpleName());
                        
                        if (HPSTrackerBuilder.isTopFromName(uchannel_ref.getName())) {
                            if (debug)
                                System.out.printf("%s: NEW NEW NEW The half module is in top volume\n", this.getClass().getSimpleName());
                            
                            
                            //The Survey UChannel seems to be rotated of 90deg around Z axis wrt hps-java uchannel for the top
                            //The transform from uchannel in hps-java to survey uchannel is x->-y, y->x z->z
                            //So a clockwise rotation of 90 deg
                            
                            //Rotation matrix R: 
                            //0   1   0
                            //-1  0   0
                            //0   0   1
                            
                            //This translates to the following rotation
                            Rotation3D hpsJava_ucTop_2_survey_ucTop = (Rotation3D) Rotation3D.passiveZRotation(Math.PI / 2.0) ;
                            
                            //Get the surveyed position in the UC
                            
                            System.out.println("PF::SURVEYED SENSOR POSITION IN SURVEY UCHANNEL FRAME L0 TOP");
                            System.out.println(surveyedSensorPosition);
                            
                            //Compute the surveyed position in the mother of the module
                            // 1) Go from surveyed UChannel frame to UChannel frame
                            Rotation3D hpsJava_surveyucTop_2_hpsJava_ucTop = (Rotation3D) hpsJava_ucTop_2_survey_ucTop.inverse();
                            hpsJava_surveyucTop_2_hpsJava_ucTop.rotate(surveyedSensorPosition);
                            System.out.println("PF::SURVEYED SENSOR POSITION IN JAVA UCHANNEL FRAME " + getName() );
                            System.out.println(surveyedSensorPosition.toString());
                                
                            // 2) Go from the UChannel to the global frame
                            ref_transform_l2g.transform(surveyedSensorPosition);
                            System.out.println("PF::SURVEYED SENSOR POSITION IN GLOBAL FRAME " + getName());
                            System.out.println(surveyedSensorPosition.toString());
                            
                            // 3) Go from the global frame to the module frame
                            mother_transform_G2L.transform(surveyedSensorPosition);
                            System.out.println("PF::SURVEYED SENSOR POSITION IN MODULE FRAME "+ getMother().getName() + " " +getName());
                            System.out.println(surveyedSensorPosition.toString());
                            
                            
                            //getCoord should get the location of the sensor wrt the mother volume (module)
                            //I now translate the coordinates to the surveyed position
                            
                            //Translation3D transToSurvey = surveyResult.getTranslationFrom(getCoord());
                            Translation3D transToSurvey = new Translation3D(VecOp.sub(surveyedSensorPosition, getCoord().origin()));
                            getCoord().translate(transToSurvey);
                            
                            
                            if (debug)
                                System.out.printf("%s: after translation to survey \n%s \n", this.getClass().getSimpleName(), this
                                                  .getCoord().toString());
                        }//Is top
                    }//is a thin sensor
                    
                    /*
                    else if (getName().contains("3") || getName().contains("4")) {  
                        
                        
                        
                    }// is a ly3 or ly4
                    */
                    
                }// is an half module
                
            }//found the node information               
            
        }//apply Survey
    }
    
    public abstract static class ShortAxialHalfModule extends ShortHalfModule {
        
        protected final static double shift_vertically_to_beam_plane = -20.6658;
        protected final static double shift_vertically_to_15mrad = ShortSensor.width / 2.0 + 0.5;
        
        protected double sensor_x;
        protected double sensor_y;
        //protected final static double sensor_z = LongHalfModule.sensor_z;
        protected double sensor_z; 
        
        public ShortAxialHalfModule(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection,
                                    int layer, String half) {
            super(name, mother, alignmentCorrection, layer, half);
            sensor_x = 0.0;
            sensor_y = 0.0;
            sensor_z = LongHalfModule.sensor_z;
        }
        
        @Override
        protected Hep3Vector getSensorPosition() {
            // return new BasicHep3Vector(sensor_x, sensor_y, sensor_z);
            return new BasicHep3Vector(sensor_x, sensor_y, -sensor_z);
        }
        
        //static method to return a default value
        static double getSensorZ() {
            return LongHalfModule.sensor_z;
        }
        
        
    }

    public abstract static class ShortStereoHalfModule extends ShortHalfModule {

        protected final static double stereo_angle = 0.1;
        //protected final static double sensor_z = ShortAxialHalfModule.getSensorZ() + ShortModule.distance_between_stereo_axial_norm_dir;
        protected double sensor_z; 
        protected double sensor_x;
        protected double sensor_y;

        protected Hep3Vector pos_of_rotation = new BasicHep3Vector(-ShortSensor.width / 2-0.5,0, 0);

        public ShortStereoHalfModule(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection,
                int layer, String half) {
            super(name, mother, alignmentCorrection, layer, half);
        }
        
        protected Hep3Vector getSensorPosition() {
            // return new BasicHep3Vector(sensor_x, sensor_y, sensor_z);
            return new BasicHep3Vector(sensor_x, sensor_y, -sensor_z);
        }
        
        //Overrides the method from SurveyVolume class
        @Override
        protected void applyGenericCoordinateSystemCorrections() {
            super.applyGenericCoordinateSystemCorrections();
            stereo_rotation();
        }
        

        protected void stereo_rotation() {

            // Rotate these into the right place for the stereo
            // My rotations here are active rotations in the mother coordinate
            // system frame
            //debug = false;

            if (debug) {
                System.out.printf("%s: ShortStereoSlotHalfModule\n", getClass().getSimpleName());
                System.out.printf("%s: YIHAA 1 coord %s\n", getClass().getSimpleName(), getCoord().toString());
            }

            Hep3Vector o2 = new BasicHep3Vector(getCoord().origin().x(), getCoord().origin().y(), getCoord().origin().z());
            Hep3Vector s = pos_of_rotation;

            if (debug) {
                System.out.printf("%s: YIHAA 1 o2 %s\n", getClass().getSimpleName(), o2.toString());
                System.out.printf("%s: YIHAA 1 s %s\n", getClass().getSimpleName(), s.toString());
            }

            // flip around v ~ along the strips
            //Rotation r1 = new Rotation(new Vector3D(0, 1, 0), Math.PI);
            Rotation r1 = new Rotation(new Vector3D(1, 0, 0), Math.PI);
            // apply stereo angle around w ~ normal to the sensor plane
            Rotation r2 = new Rotation(new Vector3D(0, 0, 1), stereo_angle);
            // Build full rotation
            Rotation r = r2.applyTo(r1);
            
            if (debug) {
                System.out.printf("%s: LongStereoHalfModule Generic Corrections\n", getClass().getSimpleName());
                System.out.printf("%s: Coord before corrections\n%s\n", getClass().getSimpleName(), getCoord().toString());
                System.out.printf("%s: box center before corrections\n%s\n", getClass().getSimpleName(), getBoxDim().toString());
            }

            // flip s
            Hep3Vector sf = new BasicHep3Vector(r1.applyTo(new Vector3D(s.v())).toArray());

            if (debug) {
                System.out.printf("%s: YIHAA 1 sf %s\n", getClass().getSimpleName(), sf.toString());
                System.out.printf("%s: YIHAA 1 -sf %s\n", getClass().getSimpleName(), VecOp.mult(-1, sf).toString());
            }

            Hep3Vector sfp = new BasicHep3Vector(r2.applyTo(new Vector3D(VecOp.mult(-1, sf).v())).toArray());

            if (debug) {
                System.out.printf("%s: YIHAA 1 sf' %s\n", getClass().getSimpleName(), sfp.toString());
                System.out.printf("%s: YIHAA 1 o2+sf %s\n", getClass().getSimpleName(), VecOp.add(o2, sf).toString());
                System.out.printf("%s: YIHAA 1 o2+sf+(sf') %s\n", getClass().getSimpleName(),VecOp.add(VecOp.add(o2, sf), sfp).toString());
                System.out.printf("%s: YIHAA 1 sf+(sf') %s\n", getClass().getSimpleName(), VecOp.add(sf, sfp).toString());
            }

            getCoord().translate(VecOp.add(sf, sfp));
            getCoord().rotateApache(r);

            if (debug) {
                System.out.printf("%s: YIHAA 3 coord %s\n", getClass().getSimpleName(), getCoord().toString());
                System.out.printf("%s: Coord after corrections\n%s\n", getClass().getSimpleName(), getCoord().toString());
                System.out.printf("%s: box center after corrections\n%s\n", getClass().getSimpleName(), getBoxDim().toString());
            }
            
            debug = false;
        }
    }

    public static class ShortAxialHoleHalfModule extends ShortAxialHalfModule {
        
        private final static double sensor_x = HalfModuleAxial.sensor_x + shift_vertically_to_beam_plane + shift_vertically_to_15mrad;
        private final static double sensor_y = HalfModuleAxial.sensor_y;
        // private final static double sensor_z = HalfModuleAxial.sensor_z;
        private final static double sensor_z = ShortAxialHalfModule.getSensorZ();

        public ShortAxialHoleHalfModule(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection,
                int layer, String half) {
            super(name, mother, alignmentCorrection, layer, half);
            init();
        }

    }
    

    public static class ShortAxialHoleHalfModuleL0Top extends ShortAxialHalfModule {
        
        private double survey_shift_x = 0.0; //positive is away from beam (up)
        private double survey_shift_y = 0.0; //positive is positive x shift in JLab coordinates (beam left)
        private double survey_shift_z = 0.0; //positive is downstream shift

        
        //Constructor with survey constants on by default
        public ShortAxialHoleHalfModuleL0Top(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, int layer, String half) {
            this(name, mother, alignmentCorrection, layer, half, true);
            
        }
        
        public ShortAxialHoleHalfModuleL0Top(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection,
                                             int layer, String half, boolean applySurvey) {
            super(name, mother, alignmentCorrection, layer, half);
            
            //Flag to decide if to apply MRSolt 2019 Survey constants or not
            //TODO - place them in a better place
            survey_shift_x = applySurvey ? 0.086 : 0.0;
            survey_shift_y = applySurvey ? 0.0 : 0.0;
            survey_shift_z = applySurvey ? 0.0 : 0.0;
            
            sensor_x = HalfModuleAxial.sensor_x + shift_vertically_to_beam_plane + shift_vertically_to_15mrad + survey_shift_x;
            sensor_y = HalfModuleAxial.sensor_y + survey_shift_y;
            sensor_z = ShortAxialHalfModule.getSensorZ() + survey_shift_z;

            System.out.println("PF:: DEBUG " +getName()+"  SENSOR POSITION PRE-SURVEY");
            System.out.println(sensor_x+ " "+ sensor_y+" "+ sensor_z);
            
            init();
            
        }
        
    }
    

    public static class ShortAxialHoleHalfModuleL0Bot extends ShortAxialHalfModule {
        
        private double survey_shift_x = 0.0; //positive is away from beam (down)
        private double survey_shift_y = 0.0; //positive is positive x shift in JLab coordinates (beam left)
        private double survey_shift_z = 0.0; //positive is upstream shift
        
        
        //Constructor with survey constants applied by default
        public ShortAxialHoleHalfModuleL0Bot(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, int layer, String half) {
            this(name, mother, alignmentCorrection, layer, half, true);
            
        }
        
        public ShortAxialHoleHalfModuleL0Bot(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection,
                                             int layer, String half, boolean applySurvey) {
            super(name, mother, alignmentCorrection, layer, half);
            
            //Flag to decide if to apply MRSolt 2019 Survey constants or not
            //TODO - place them in a better place
            survey_shift_x = applySurvey ? 0.2 : 0.0;
            survey_shift_y = applySurvey ? 0.0 : 0.0;
            survey_shift_z = applySurvey ? 0.0 : 0.0;
            
            sensor_x = HalfModuleAxial.sensor_x + shift_vertically_to_beam_plane + shift_vertically_to_15mrad + survey_shift_x;
            sensor_y = HalfModuleAxial.sensor_y + survey_shift_y;
            sensor_z = ShortAxialHalfModule.getSensorZ() + survey_shift_z;

            init();
        }
        
    }
  
    
    public static class ShortAxialHoleHalfModuleL1Top extends ShortAxialHalfModule {
        
        private double survey_shift_x = 0.0; //positive is away from beam (up)
        private double survey_shift_y = 0.0; //positive is positive x shift in JLab coordinates (beam left)
        private double survey_shift_z = 0.0; //positive is downstream shift
        
        
        //Constructor with survey constants applied by default
        public ShortAxialHoleHalfModuleL1Top(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, int layer, String half) {
            this(name, mother, alignmentCorrection, layer, half, true);
            
        }
        
        public ShortAxialHoleHalfModuleL1Top(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection,
                                             int layer, String half, boolean applySurvey) {
            super(name, mother, alignmentCorrection, layer, half);
            
            //Flag to decide if to apply MRSolt 2019 Survey constants or not
            //TODO - place them in a better place
            survey_shift_x = applySurvey ? 0.366 - 0.245 : 0.0;
            survey_shift_y = applySurvey ? 0.0 : 0.0;
            survey_shift_z = applySurvey ? 0.0 : 0.0;
            
            sensor_x = HalfModuleAxial.sensor_x + shift_vertically_to_beam_plane + shift_vertically_to_15mrad + survey_shift_x;
            sensor_y = HalfModuleAxial.sensor_y + survey_shift_y;
            sensor_z = ShortAxialHalfModule.getSensorZ() + survey_shift_z;

            System.out.println("PF:: DEBUG "+getName()+ " SENSOR POSITION PRE-SURVEY");
            System.out.println(sensor_x+ " "+ sensor_y+" "+ sensor_z);

            init();
        }
        
    }
    
    public static class ShortAxialHoleHalfModuleL1Bot extends ShortAxialHalfModule {

        private double survey_shift_x = 0.0; //positive is away from beam (down)
        private double survey_shift_y = 0.0; //positive is positive x shift in JLab coordinates (beam left)
        private double survey_shift_z = 0.0; //positive is upstream shift
        
                
        //Constructor with survey constants applied by default
        public ShortAxialHoleHalfModuleL1Bot(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, int layer, String half) {
            this(name, mother, alignmentCorrection, layer, half, true);
            
        }
        
        public ShortAxialHoleHalfModuleL1Bot(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection,
                                             int layer, String half, boolean applySurvey) {
            super(name, mother, alignmentCorrection, layer, half);
            
            //Flag to decide if to apply MRSolt 2019 Survey constants or not
            //TODO - place them in a better place
            survey_shift_x = applySurvey ? 0.285 : 0.0;
            survey_shift_y = applySurvey ? 0.0 : 0.0;
            survey_shift_z = applySurvey ? 0.0 : 0.0;
            
            sensor_x = HalfModuleAxial.sensor_x + shift_vertically_to_beam_plane + shift_vertically_to_15mrad + survey_shift_x;
            sensor_y = HalfModuleAxial.sensor_y + survey_shift_y;
            sensor_z = ShortAxialHalfModule.getSensorZ() + survey_shift_z;
            
            init();
        }
    }
    
    public static class ShortStereoHoleHalfModuleL0Top extends ShortStereoHalfModule {

        // private final static double sensor_x = 1.282*inch;
        // private final static double sensor_y = 3.889*inch;
        // protected final static Hep3Vector pos_of_rotation = new
        // BasicHep3Vector(ActiveShortSensor.width/2,ActiveShortSensor.length/2,0);
        //protected final static Hep3Vector pos_of_rotation = new BasicHep3Vector(ShortSensor.width / 2,
        //        ShortSensor.length / 2, 0);
        
        //Constructor with survey constants on by default
        public ShortStereoHoleHalfModuleL0Top(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, int layer, String half) {
            this(name, mother, alignmentCorrection, layer, half, true);
            
        }

        public ShortStereoHoleHalfModuleL0Top(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, int layer, String half, boolean applySurvey) {
            super(name, mother, alignmentCorrection, layer, half);
            
            double survey_shift_x = applySurvey ? 0.113 : 0; //positive is away from beam (up)
            double survey_shift_y = 0.0; //positive is positive x shift in JLab coordinates (beam left)
            double survey_shift_z = 0.0; //positive is downstream shift
                        
            // reference is kind of random I guess
            sensor_x = ShortAxialHoleHalfModule.sensor_x + survey_shift_x;
            sensor_y = ShortAxialHoleHalfModule.sensor_y + survey_shift_y;
            //sensor_z = ShortStereoHalfModule.sensor_z + survey_shift_z; //+ ShortModule.distance_between_stereo_axial_norm_dir;
            sensor_z = LongHalfModule.sensor_z + survey_shift_z + ShortModule.distance_between_stereo_axial_norm_dir;

            
            System.out.println("PF:: DEBUG "+getName()+ " SENSOR POSITION PRE-SURVEY");
            System.out.println(sensor_x+ " "+ sensor_y+" "+ sensor_z);
            init();
            
        }
    }

    public static class ShortStereoHoleHalfModuleL0Bot extends ShortStereoHalfModule {

        //Constructor with survey constants on by default
        public ShortStereoHoleHalfModuleL0Bot(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, int layer, String half) {
            this(name, mother, alignmentCorrection, layer, half, true);
            
        }

        public ShortStereoHoleHalfModuleL0Bot(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, int layer, String half, boolean applySurvey) {
            super(name, mother, alignmentCorrection, layer, half);
            
            double survey_shift_x = applySurvey ? 0.098 : 0.; //positive is away from beam (up)
            double survey_shift_y = 0.0; //positive is positive x shift in JLab coordinates (beam left)
            double survey_shift_z = 0.0; //positive is downstream shift
                        
            // reference is kind of random I guess
            sensor_x = ShortAxialHoleHalfModule.sensor_x + survey_shift_x;
            sensor_y = ShortAxialHoleHalfModule.sensor_y + survey_shift_y;
            //sensor_z = ShortStereoHalfModule.sensor_z + survey_shift_z;//+ ShortModule.distance_between_stereo_axial_norm_dir;
            sensor_z = LongHalfModule.sensor_z + survey_shift_z + ShortModule.distance_between_stereo_axial_norm_dir;
            init();
        }
        
    }

    public static class ShortStereoHoleHalfModuleL1Top extends ShortStereoHalfModule {

        
        //Constructor with survey constants on by default
        public ShortStereoHoleHalfModuleL1Top(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, int layer, String half) {
            this(name, mother, alignmentCorrection, layer, half, true);
            
        }

        public ShortStereoHoleHalfModuleL1Top(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, int layer, String half, boolean applySurvey) {
            super(name, mother, alignmentCorrection, layer, half);
            
            double survey_shift_x = applySurvey ? 0.253 : 0.; //positive is away from beam (up)
            double survey_shift_y = 0.0; //positive is positive x shift in JLab coordinates (beam left)
            double survey_shift_z = 0.0; //positive is downstream shift
                        
            // reference is kind of random I guess
            sensor_x = ShortAxialHoleHalfModule.sensor_x + survey_shift_x;
            sensor_y = ShortAxialHoleHalfModule.sensor_y + survey_shift_y;
            //sensor_z = ShortStereoHalfModule.sensor_z + survey_shift_z;//+ ShortModule.distance_between_stereo_axial_norm_dir;
            sensor_z = LongHalfModule.sensor_z + survey_shift_z + ShortModule.distance_between_stereo_axial_norm_dir;
            
            
            System.out.println("PF:: DEBUG "+getName()+ " SENSOR POSITION PRE-SURVEY");
            System.out.println(sensor_x+ " "+ sensor_y+" "+ sensor_z);
            init();
        }
        

    }

    public static class ShortStereoHoleHalfModuleL1Bot extends ShortStereoHalfModule {
                        
        //Constructor with survey constants on by default
        public ShortStereoHoleHalfModuleL1Bot(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, int layer, String half) {
            this(name, mother, alignmentCorrection, layer, half, true);
            
        }
        
        public ShortStereoHoleHalfModuleL1Bot(String name, SurveyVolume mother, AlignmentCorrection alignmentCorrection, int layer, String half, boolean applySurvey) {
            super(name, mother, alignmentCorrection, layer, half);
            
            double survey_shift_x = applySurvey ? 0.231 : 0.; //positive is away from beam (up)
            double survey_shift_y = 0.0; //positive is positive x shift in JLab coordinates (beam left)
            double survey_shift_z = 0.0; //positive is downstream shift
                        
            // reference is kind of random I guess
            sensor_x = ShortAxialHoleHalfModule.sensor_x + survey_shift_x;
            sensor_y = ShortAxialHoleHalfModule.sensor_y + survey_shift_y;
            //sensor_z = ShortStereoHalfModule.sensor_z + survey_shift_z;//+ ShortModule.distance_between_stereo_axial_norm_dir;
            sensor_z = LongHalfModule.sensor_z + survey_shift_z + ShortModule.distance_between_stereo_axial_norm_dir;
            init();
        }
    }        
}
