/**
 * 
 */
package org.lcsim.geometry.compact.converter;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.logging.Logger;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.jdom.Element;
import org.lcsim.geometry.compact.converter.HPSTestRunTracker2014GeometryDefinition.BaseModule;
import org.lcsim.geometry.compact.converter.HPSTestRunTracker2014GeometryDefinition.CarbonFiber;
import org.lcsim.geometry.compact.converter.HPSTestRunTracker2014GeometryDefinition.Sensor;
import org.lcsim.geometry.compact.converter.HPSTestRunTracker2014GeometryDefinition.TestRunHalfModule;

/**
 * 
 * Geometry information for the HPS tracker 2014
 * 
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class HPSTracker2014GeometryDefinition extends
        HPSTrackerGeometryDefinition {

    private static final Logger LOGGER = Logger
            .getLogger(HPSTracker2014GeometryDefinition.class.getPackage()
                    .getName());

    public HPSTracker2014GeometryDefinition(boolean debug, Element node) {
        super(debug, node);
        doAxial = true;
        doStereo = true;
        doColdBlock = false;
        doBottom = true;
        doTop = true;
        layerBitMask = 0x3F;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.lcsim.geometry.compact.converter.HPSTrackerBuilder#build()
     */
    public void build() {

        if (isDebug())
            System.out.printf("%s: constructing the geometry objects\n", this
                    .getClass().getSimpleName());

        // Create alignment correction objects
        // THis is really a ugly approach with MP corrections initialized before
        // and
        // the survey corrections based on the XML node
        // FIX THIS! //TODO
        AlignmentCorrection alignmentCorrections = new AlignmentCorrection();
        alignmentCorrections.setNode(node);
        AlignmentCorrection supBotCorr = getL13UChannelAlignmentCorrection(false);
        supBotCorr.setNode(node);
        AlignmentCorrection supTopCorr = this
                .getL13UChannelAlignmentCorrection(true);
        supTopCorr.setNode(node);

        // Build the geometry from the basic building blocks in the geometry
        // definition class
        // Keep the order correct.
        // Each item has knowledge of its mother but not its daughters
        HPSTrackerGeometryDefinition.TrackingVolume tracking = new HPSTrackerGeometryDefinition.TrackingVolume(
                "trackingVolume", null);
        surveyVolumes.add(tracking);

        PSVacuumChamber chamber = new PSVacuumChamber("chamber", tracking, null);
        surveyVolumes.add(chamber);

        SvtBox svtBox = new SvtBox("base", chamber, null);
        surveyVolumes.add(svtBox);

        SvtBoxBasePlate svtBoxBasePlate = new SvtBoxBasePlate("base_plate",
                svtBox, null);
        surveyVolumes.add(svtBoxBasePlate);

        SupportRingL13BottomKinMount supportRingKinL13Bottom = new SupportRingL13BottomKinMount(
                "c_support_kin_L13b", svtBox, supBotCorr);
        surveyVolumes.add(supportRingKinL13Bottom);

        UChannelL13 uChannelL13Bottom = new UChannelL13Bottom(
                "support_bottom_L13", svtBox, alignmentCorrections,
                supportRingKinL13Bottom);
        surveyVolumes.add(uChannelL13Bottom);

        UChannelL13Plate uChannelL13BottomPlate = new UChannelL13BottomPlate(
                "support_plate_bottom_L13", svtBox, null, uChannelL13Bottom);
        surveyVolumes.add(uChannelL13BottomPlate);

        SupportRingL13TopKinMount supportRingKinL13Top = new SupportRingL13TopKinMount(
                "c_support_kin_L13t", svtBox, supTopCorr);
        surveyVolumes.add(supportRingKinL13Top);

        UChannelL13Top uChannelL13Top = new UChannelL13Top("support_top_L13",
                svtBox, alignmentCorrections, supportRingKinL13Top);
        surveyVolumes.add(uChannelL13Top);

        UChannelL13Plate uChannelL13TopPlate = new UChannelL13TopPlate(
                "support_plate_top_L13", svtBox, null, uChannelL13Top);
        surveyVolumes.add(uChannelL13TopPlate);

        UChannelL46 uChannelL46Bottom = new UChannelL46Bottom(
                "support_bottom_L46", svtBox, alignmentCorrections);
        surveyVolumes.add(uChannelL46Bottom);

        UChannelL46Plate uChannelL46BottomPlate = new UChannelL46BottomPlate(
                "support_plate_bottom_L46", svtBox, null, uChannelL46Bottom);
        surveyVolumes.add(uChannelL46BottomPlate);

        UChannelL46 uChannelL46Top = new UChannelL46Top("support_top_L46",
                svtBox, alignmentCorrections);
        surveyVolumes.add(uChannelL46Top);

        UChannelL46Plate uChannelL46TopPlate = new UChannelL46TopPlate(
                "support_plate_top_L46", svtBox, null, uChannelL46Top);
        surveyVolumes.add(uChannelL46TopPlate);

        for (int l = 1; l <= 6; ++l) {
            if (doLayer(l)) {
                if (doBottom)
                    makeModuleBundle(l, "bottom");
                if (doTop)
                    makeModuleBundle(l, "top");
            }
        }

        LOGGER.info(String.format("%s: Constructed %d geometry objects", this
                .getClass().getSimpleName(), surveyVolumes.size()));
        LOGGER.info(String.format("%s: Constructed %d module bundles", this
                .getClass().getSimpleName(), modules.size()));

        if (isDebug()) {
            System.out.printf("%s: DONE constructing the geometry objects\n",
                    this.getClass().getSimpleName());
            System.out.printf("%s: List of the survey volumes built\n", this
                    .getClass().getSimpleName());
            for (SurveyVolume bg : surveyVolumes) {
                System.out.printf("-------\n%s\n", bg.toString());
            }
        }
        if (isDebug()) {
            System.out.printf("%s: List of the module bundles built\n", this
                    .getClass().getSimpleName());
            for (BaseModuleBundle bundle : this.modules) {
                bundle.print();
            }
        }

    }

    /**
     * {@link SurveyVolume} volume defining the pair spectrometer (PS) vacuum
     * chamber Reference: tracking volume coordinate system Origin: same as
     * reference Orientation: u - points in x direction (towards positron side),
     * v - points upstream
     * 
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     *
     */
    public static class PSVacuumChamber extends SurveyVolume {
        public static final double height = PS_vac_box_inner_height;
        public static final double width = PS_vac_box_inner_width;
        public static final double length = PS_vac_box_inner_length;

        public PSVacuumChamber(String name, SurveyVolume mother,
                AlignmentCorrection alignmentCorrection) {
            super(name, mother, alignmentCorrection);
            init();
        }

        protected void setCenter() {
            setCenter(0, 0, 0);
        }

        protected void setBoxDim() {
            setBoxDim(width, length, height);
        }

        protected void setPos() {
            ballPos = new BasicHep3Vector(SvtBox.center_to_target_x,
                    SvtBox.center_to_target_y, SvtBox.center_to_target_z);
            veePos = new BasicHep3Vector(ballPos.x() + 1, ballPos.y(),
                    ballPos.z());
            flatPos = new BasicHep3Vector(ballPos.x(), ballPos.y(),
                    ballPos.z() - 1);

        }
    }

    /**
     * {@link SurveyVolume} volume defining the SVT box envelope Reference: PS
     * vacuum chamber coordinate system. Note that the PS vacuum chamber box is
     * placed w.r.t. this box and the target positions. Origin: intersection of
     * midplanes vertically and horizontally Orientation: same as reference
     * 
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     *
     */
    public static class SvtBox extends SurveyVolume {
        public static final double height = 6.740 * inch;
        public static final double width = SvtBoxBasePlate.width;
        public static final double length = SvtBoxBasePlate.length;

        // position of the target w.r.t. center of this box.
        // the coordinate frame is the JLab coordinates..confusing.
        public static final double center_to_target_z = 13.777 * inch;
        public static final double center_to_target_x = 0.84 * inch;
        public static final double center_to_target_y = 0.0;

        public SvtBox(String name, SurveyVolume mother,
                AlignmentCorrection alignmentCorrection) {
            super(name, mother, alignmentCorrection);
            init();
        }

        protected void setCenter() {
            setCenter(0, 0, 0);
        }

        protected void setBoxDim() {
            setBoxDim(width, length, height);
        }

        protected void setPos() {

            ballPos = new BasicHep3Vector(0, 0, 0);
            veePos = new BasicHep3Vector(ballPos.x() + 1, ballPos.y(),
                    ballPos.z());
            flatPos = new BasicHep3Vector(ballPos.x(), ballPos.y() + 1,
                    ballPos.z());

        }
    }

    /**
     * {@link SurveyVolume} volume defining the base plate of the SVT box.
     * Reference: {@link SvtBox} coordinate system. Origin: surface of base
     * plate intersection with center of hole for adjustment screw on positron
     * side Orientation: same as reference
     * 
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     *
     */
    public static class SvtBoxBasePlate extends SurveyVolume {
        public static final double length = 50.5 * inch;
        public static final double width = 16.0 * inch;
        public static final double height = 0.25 * inch;
        public static final double kin_mount_to_edge_of_plate_x = (8.0 - 5.0)
                * inch;
        public static final double kin_mount_to_edge_of_plate_y = 0.375 * inch;
        public static final double adj_screw_height = 0.13 * inch; // amount
                                                                   // screw
                                                                   // sticks out
                                                                   // from plate
        public static final double adj_screw_width = 0.13 * inch; // amount
                                                                  // screw
                                                                  // sticks out
                                                                  // on side

        public SvtBoxBasePlate(String name, SurveyVolume mother,
                AlignmentCorrection alignmentCorrection) {
            super(name, mother, alignmentCorrection);
            setMaterial("Aluminum");
            init();
        }

        protected void setCenter() {
            final double x = -kin_mount_to_edge_of_plate_x
                    + SvtBoxBasePlate.width / 2.0;
            final double y = -kin_mount_to_edge_of_plate_y
                    + SvtBoxBasePlate.length / 2.0;
            final double z = -SvtBoxBasePlate.height / 2.0;
            setCenter(new BasicHep3Vector(x, y, z));
        }

        protected void setBoxDim() {
            setBoxDim(width, length, height);
        }

        protected void setPos() {
            final double x = -width / 2.0 + kin_mount_to_edge_of_plate_x;
            final double y = -length / 2.0 + kin_mount_to_edge_of_plate_y;
            final double z = -SvtBox.height / 2.0 + height;
            ballPos = new BasicHep3Vector(x, y, z);
            veePos = new BasicHep3Vector(ballPos.x() + 1, ballPos.y(),
                    ballPos.z());
            flatPos = new BasicHep3Vector(ballPos.x(), ballPos.y() + 1,
                    ballPos.z());
        }

    }

    /**
     * {@link SurveyVolume} volume defining the coordinate system of the support
     * ring Reference: @SvtBoxBasePlate Origin: pin position of support ring
     * (electron side) Orientation: slot position is vee position (positron
     * side) i.e u points towards the positron side and v in the upstream beam
     * direction
     * 
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     *
     */
    public static class SupportRing extends SurveyVolume {
        private static final double plateThickness = 0.35 * inch;

        public SupportRing(String name, SurveyVolume mother,
                AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, mother, alignmentCorrection, ref);
            init();
        }

        protected void setCenter() {
            setCenter(null); // dummy
        }

        protected void setBoxDim() {
            // do nothing
        }

        protected void setPos() {

            final double ball_pos_x = -SvtBoxBasePlate.kin_mount_to_edge_of_plate_x
                    + SvtBoxBasePlate.width / 2.0 - 6.997 * inch;
            final double ball_pos_y = -SvtBoxBasePlate.kin_mount_to_edge_of_plate_y
                    + SvtBoxBasePlate.length - 28.543 * inch;
            final double ball_pos_z = 0.0;

            final double vee_pos_x = -SvtBoxBasePlate.kin_mount_to_edge_of_plate_x
                    + SvtBoxBasePlate.width / 2.0 + 6.622 * inch;
            final double vee_pos_y = -SvtBoxBasePlate.kin_mount_to_edge_of_plate_y
                    + SvtBoxBasePlate.length - 28.116 * inch;
            final double vee_pos_z = 0.0;

            ballPos = new BasicHep3Vector(ball_pos_x, ball_pos_y, ball_pos_z);
            veePos = new BasicHep3Vector(vee_pos_x, vee_pos_y, vee_pos_z);
            flatPos = new BasicHep3Vector(0, 0, 0);

            Hep3Vector uPrime = VecOp.unit(VecOp.sub(veePos, ballPos));
            Rotation r = new Rotation(new Vector3D(1, 0, 0), new Vector3D(0, 0,
                    1), new Vector3D(uPrime.v()), new Vector3D(0, 0, 1));
            Hep3Vector vPrime = new BasicHep3Vector(r.applyTo(
                    new Vector3D(0, 1, 0)).toArray());
            flatPos = VecOp.add(ballPos, vPrime);
        }
    }

    /**
     * Abstract {@link SurveyVolume} volume defining a coordinate system from
     * the kinematic mount positions for support channels
     * 
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     *
     */
    public abstract static class SupportRingL13KinMount extends SurveyVolume {

        public static final double kin_mount_offset_vertically = 0.093 * inch;
        protected static final double kin_mount_pos_x = -138.665;
        protected static final double kin_mount_pos_y = -67.855;

        public SupportRingL13KinMount(String name, SurveyVolume mother,
                AlignmentCorrection alignmentCorrection) {
            super(name, mother, alignmentCorrection);
        }

        protected void setCenter() {
            setCenter(null); // dummy
        }

        protected void setBoxDim() {
            // do nothing
        }

        protected void setPos() {
            ballPos = new BasicHep3Vector(kin_mount_pos_x, kin_mount_pos_y,
                    getKinMountVerticalPos());

            final double vee_pos_x = ballPos.x() + 1; // random positive offset
            final double vee_pos_y = ballPos.y();
            final double vee_pos_z = ballPos.z();
            veePos = new BasicHep3Vector(vee_pos_x, vee_pos_y, vee_pos_z);

            final double flat_pos_x = ballPos.x();
            final double flat_pos_y = ballPos.y() + 1.0; // random positive
                                                         // offset
            final double flat_pos_z = ballPos.z();
            flatPos = new BasicHep3Vector(flat_pos_x, flat_pos_y, flat_pos_z);
        }

        abstract protected double getKinMountVerticalPos();
    }

    /**
     * {@link SurveyVolume} volume defining a coordinate system from the
     * kinematic mount positions for support channels Reference: {@link SvtBox}
     * coordinate system Origin: cone mount (it's on the electron side)
     * Orientation: ball is cone mount, slot mount is vee position and flat is
     * along beam line pointing upstream
     * 
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     *
     */
    public static class SupportRingL13BottomKinMount extends
            SupportRingL13KinMount {

        protected static final double kin_mount_pos_z = -67.996;

        public SupportRingL13BottomKinMount(String name, SurveyVolume mother,
                AlignmentCorrection alignmentCorrection) {
            super(name, mother, alignmentCorrection);
            init();
        }

        @Override
        protected double getKinMountVerticalPos() {
            return kin_mount_pos_z;
        }

    }

    /**
     * {@link SurveyVolume} volume defining a coordinate system from the
     * kinematic mount positions for support channels Reference: @SupportRing
     * coordinate system Origin: cone mount (it's on the electron side)
     * Orientation: ball is cone mount, slot mount is vee position and flat is
     * along beamline pointing upstream
     * 
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     *
     */
    public static class SupportRingL13TopKinMount extends
            SupportRingL13KinMount {
        // public static final double mount_surface_wrt_baseplate_vertically =
        // 5.388*inch;
        protected static final double kin_mount_pos_z = 56.857;

        public SupportRingL13TopKinMount(String name, SurveyVolume mother,
                AlignmentCorrection alignmentCorrection) {
            super(name, mother, alignmentCorrection);
            init();
        }

        @Override
        protected double getKinMountVerticalPos() {
            return kin_mount_pos_z;
        }

        // protected void setPos() {
        // final double ball_pos_x = (7.0 - 5.444) *inch;
        // final double ball_pos_y = 0.574*inch;
        // final double ball_pos_z = mount_surface_wrt_baseplate_vertically +
        // kin_mount_offset_vertically;
        // ballPos = new BasicHep3Vector(ball_pos_x, ball_pos_y, ball_pos_z);
        //
        // final double vee_pos_x = (2*7.0)*inch;
        // final double vee_pos_y = ball_pos_y;
        // final double vee_pos_z = ball_pos_z;
        // veePos = new BasicHep3Vector(vee_pos_x, vee_pos_y, vee_pos_z);
        //
        // final double flat_pos_x = ball_pos_x;
        // final double flat_pos_y = ball_pos_y + 1.0; // random distance
        // final double flat_pos_z = ball_pos_z;
        // flatPos = new BasicHep3Vector(flat_pos_x,flat_pos_y,flat_pos_z);
        // }
    }

    /**
     * Abstract {@link SurveyVolume} volume defining the coordinate system of
     * the L1-3 u-channels
     * 
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     */
    public abstract static class UChannelL13 extends SurveyVolume {
        protected final static double length = UChannelL13Plate.length;
        private static final double width = UChannelL13Plate.width;
        public static final double height = 2.575 * inch;
        // private static final double kin_mount_to_edge_of_plate_x =
        // width/2.0-4.0*inch;
        public static final double side_plate_cone_y = 2.0 * inch;

        public UChannelL13(String name, SurveyVolume m,
                AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, m, alignmentCorrection, ref);
        }

        protected void setBoxDim() {
            setBoxDim(getWidth(), getLength(), getHeight());
        }

        protected double getLength() {
            //System.out.println("UChannelL13 getLength");
            return length;
        }

        protected double getWidth() {
            return width;
        }

        protected double getHeight() {
            return height;
        }
    }

    /**
     * {@link SurveyVolume} volume defining the coordinate system of the bottom
     * L1-3 u-channel Reference: {@link SupportRingL13BottomKinMount} coordinate
     * system Origin: midpoint between upstream survey cones Orientation: u -
     * width pointing towards electron side, v - pointing along the U-channel in
     * the beam direction
     * 
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     *
     */
    public static class UChannelL13Bottom extends UChannelL13 {
        protected final static double cone_to_edge_of_plate_y = 12.25 * inch;
        private final static Hep3Vector ball_kinMount = new BasicHep3Vector(
                SupportRingL13BottomKinMount.kin_mount_pos_x,
                SupportRingL13BottomKinMount.kin_mount_pos_y,
                SupportRingL13BottomKinMount.kin_mount_pos_z);

        public UChannelL13Bottom(String name, SurveyVolume m,
                AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, m, alignmentCorrection, ref);
            init();
        }

        protected void setCenter() {
            //System.out.println("UChannelL13Bottom setCenter");
            final double x = 0.0;
            final double y = cone_to_edge_of_plate_y - length / 2.0;
            final double z = -side_plate_cone_y - UChannelL13Plate.height
                    + height / 2.0;
            setCenter(x, y, z);
        }

        protected void setPos() {            
            //System.out.println("UChannelL13Bottom setPos");
            ballPos = VecOp.sub(UChannelL13BottomSurveyBalls.ball_pos,
                    ball_kinMount);
            Hep3Vector veeOffset = UChannelL13BottomSurveyBalls.getVeeOffset();
            veePos = VecOp.add(ballPos, veeOffset);
            Hep3Vector flatOffset = UChannelL13BottomSurveyBalls
                    .getFlatOffset();
            flatPos = VecOp.add(ballPos, flatOffset);

            // ballPos = VecOp.sub(UChannelL13BottomSurveyBalls.ball_pos,
            // ball_kinMount);
            // veePos = new BasicHep3Vector(ballPos.x()-1, ballPos.y(),
            // ballPos.z());
            // flatPos = new BasicHep3Vector(ballPos.x(), ballPos.y()-1,
            // ballPos.z());
        }
    }

    /**
     * Position of the center of the survey balls when engaging the cones in the
     * side plates of the U-channel. This is at nominal position.
     * 
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     *
     */
    private static class UChannelL13BottomSurveyBalls {

        // Shawn's calculated point at midpoint between two forward survey balls
        protected final static Hep3Vector ball_pos = new BasicHep3Vector(
                -46.446, 241.184, -8.423);

        // Nominal Design FWD Right (x,y,z) BWD Right (x,y,z) FWD Left (x,y,z,)
        // BWD Left (x,y,z)
        //
        // Layer 1-3, Lower -6.493, -.332, 9.353 -6.253, -.332, 1.483 2.836,
        // -.332, 9.638 3.076, -.332, 1.767
        //
        // Layer 1-3, Upper -6.512, .332, 9.978 -6.272, .332, 2.107 2.817, .332,
        // 10.262 3.057, .332, 2.392

        protected static final Hep3Vector fwd_right = new BasicHep3Vector(
                -6.493, 9.353, -.332);
        protected static final Hep3Vector bwd_right = new BasicHep3Vector(
                -6.253, 1.483, -.332);
        protected static final Hep3Vector fwd_left = new BasicHep3Vector(2.836,
                9.638, -.332);

        // protected static final Hep3Vector bwd_left = new
        // BasicHep3Vector(3.076, 1.767, -.332);

        protected static Hep3Vector getVeeOffset() {
            return VecOp.mult(0.5, VecOp.sub(fwd_right, fwd_left));
        }

        protected static Hep3Vector getFlatOffset() {
            return VecOp.sub(bwd_right, fwd_right);
        }
    }

    /**
     * {@link SurveyVolume} volume defining the coordinate system of the top
     * L1-3 u-channel Reference: SupportRingL13TopKinMount coordinate system
     * Origin: midpoint between upstream survey cones Orientation: u - width
     * pointing towards positron side, v - pointing along the U-channel in the
     * beam direction Note that this is flipped w.r.t. bottom support.
     * 
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     *
     */
    public static class UChannelL13Top extends UChannelL13 {
        private final static Hep3Vector ball_kinMount = new BasicHep3Vector(
                SupportRingL13TopKinMount.kin_mount_pos_x,
                SupportRingL13TopKinMount.kin_mount_pos_y,
                SupportRingL13TopKinMount.kin_mount_pos_z);

        private final static double cone_to_side_plate_pin_y = (14.5 - 3.125)
                * inch;
        private final static double side_plate_pin_to_edge_of_plate_y = (16.0 - 14.5)
                * inch;
        protected final static double cone_to_edge_of_plate_y = cone_to_side_plate_pin_y
                + side_plate_pin_to_edge_of_plate_y;

        public UChannelL13Top(String name, SurveyVolume m,
                AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, m, alignmentCorrection, ref);
            init();
        }

        protected void setCenter() {
            final double x = 0.0;
            final double y = cone_to_edge_of_plate_y - length / 2.0;
            final double z = -side_plate_cone_y - UChannelL13Plate.height
                    + height / 2.0;
            setCenter(x, y, z);
        }

        protected void setPos() {
            ballPos = VecOp.sub(UChannelL13TopSurveyBalls.ball_pos,
                    ball_kinMount);
            Hep3Vector veeOffset = UChannelL13TopSurveyBalls.getVeeOffset();
            veePos = VecOp.add(ballPos, veeOffset);
            Hep3Vector flatOffset = UChannelL13TopSurveyBalls.getFlatOffset();
            flatPos = VecOp.add(ballPos, flatOffset);

        }

        protected double getLength() {
            return length;
        }
    }

    /**
     * Position of the center of the survey balls when engaging the cones in the
     * side plates of the U-channel. This is at nominal position.
     * 
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     *
     */
    private static class UChannelL13TopSurveyBalls {

        // Shawn's calculated point at midpoint between two forward survey balls
        protected final static Hep3Vector ball_pos = new BasicHep3Vector(
                -46.930, 257.052, 8.423);

        // Nominal Design FWD Right (x,y,z) BWD Right (x,y,z) FWD Left (x,y,z,)
        // BWD Left (x,y,z)
        //
        // Layer 1-3, Lower -6.493, -.332, 9.353 -6.253, -.332, 1.483 2.836,
        // -.332, 9.638 3.076, -.332, 1.767
        //
        // Layer 1-3, Upper -6.512, .332, 9.978 -6.272, .332, 2.107 2.817, .332,
        // 10.262 3.057, .332, 2.392

        protected static final Hep3Vector fwd_right = new BasicHep3Vector(
                -6.512, 9.978, .332);
        // protected static final Hep3Vector bwd_right = new
        // BasicHep3Vector(-6.272, 2.107, .332);
        protected static final Hep3Vector fwd_left = new BasicHep3Vector(2.817,
                10.262, .332);
        protected static final Hep3Vector bwd_left = new BasicHep3Vector(3.057,
                2.392, .332);

        protected static Hep3Vector getVeeOffset() {
            return VecOp.mult(0.5, VecOp.sub(fwd_left, fwd_right));
        }

        protected static Hep3Vector getFlatOffset() {
            return VecOp.sub(bwd_left, fwd_left);
        }
    }

    /**
     * Abstract {@link SurveyVolume} volume defining the coordinate system of
     * the u-channel plate
     * 
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     *
     */
    public abstract static class UChannelPlate extends SurveyVolume {
        public UChannelPlate(String name, SurveyVolume m,
                AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, m, alignmentCorrection, ref);
        }

        // the kin mount cone is recessed into the plate meaning that the
        // effective pivot axis is
        // also recessed into the plate from the surface
        public static final double dist_from_plate_surface_to_pivot_point = 0.0295 * inch;
    }

    /**
     * Abstract {@link SurveyVolume} volume defining the coordinate system of
     * the u-channel plate
     * 
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     *
     */
    public abstract static class UChannelL13Plate extends UChannelPlate {
        private final static double pocket_depth_L1 = 0.025;
        private final static double pocket_depth_L2 = pocket_depth_L1 + 0.059;
        private final static double pocket_depth_L3 = pocket_depth_L2 + 0.059;
        // private final static double module_mounting_hole_to_hole_x
        // =3.937*inch;
        protected static final double width = 9.25 * inch;
        protected static final double height = 0.375 * inch;
        protected final static double length = 16.0 * inch;

        public UChannelL13Plate(String name, SurveyVolume m,
                AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, m, alignmentCorrection, ref);
            setMaterial("Aluminum");
        }

        /**
         * Get pocket depth for this plate
         * 
         * @param layer
         * @return pocket depth
         */
        public static double getPocketDepth(int layer) {
            if (layer == 1)
                return pocket_depth_L1;
            else if (layer == 2)
                return pocket_depth_L2;
            else if (layer == 3)
                return pocket_depth_L3;
            else {
                throw new RuntimeException(
                        "Trying to create a L1-3 module with invalid layer nr: "
                                + layer);
            }
        }

        protected void setBoxDim() {
            setBoxDim(getWidth(), getLength(), getHeight());
        }

        protected void setPos() {
            ballPos = new BasicHep3Vector(0, 0, 0);
            veePos = new BasicHep3Vector(1, 0, 0);
            flatPos = new BasicHep3Vector(0, 1, 0);
        }

        public double getWidth() {
            return width;
        }

        public double getLength() {
            return length;
        }

        public double getHeight() {
            return height;
        }

    }

    /**
     * {@link SurveyVolume} volume defining the coordinate system of the bottom
     * u-channel plate Reference: @UChannelL13Bottom coordinate system Origin:
     * same as reference Orientation: same as reference
     *
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     *
     */
    public static class UChannelL13BottomPlate extends UChannelL13Plate {
        protected final static double L1_module_pin_to_edge_of_plate = (16.0 - 4.126)
                * inch;

        public UChannelL13BottomPlate(String name, SurveyVolume m,
                AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, m, alignmentCorrection, ref);
            init();
        }

        protected void setCenter() {
            final double x = 0.0;
            final double y = UChannelL13Bottom.cone_to_edge_of_plate_y
                    - getLength() / 2.0;
            final double z = -UChannelL13.side_plate_cone_y - getHeight() / 2.0;
            setCenter(x, y, z);
        }

    }

    /**
     * {@link SurveyVolume} volume defining the coordinate system of the bottom
     * u-channel plate Reference: @UChannelL13Bottom coordinate system Origin:
     * same as reference Orientation: same as reference
     *
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     *
     */
    public static class UChannelL13TopPlate extends UChannelL13Plate {
        protected final static double L1_module_pin_to_edge_of_plate = (16.0 - 2.75)
                * inch;

        public UChannelL13TopPlate(String name, SurveyVolume m,
                AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, m, alignmentCorrection, ref);
            init();
        }

        protected void setCenter() {
            final double x = 0.0;
            final double y = UChannelL13Top.cone_to_edge_of_plate_y
                    - getLength() / 2.0;
            final double z = -UChannelL13.side_plate_cone_y - getHeight() / 2.0;
            setCenter(x, y, z);
        }

    }

    /**
     * Abstract {@link SurveyVolume} volume defining the L4-6 u-channel volume
     * 
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     */
    public abstract static class UChannelL46 extends SurveyVolume {

        protected static final double width = UChannelL46Plate.width;
        protected static final double length = UChannelL46Plate.length;
        protected static final double height = 2.575 * inch;
        // private static final double kin_mount_to_edge_of_plate_x =
        // width/2.0-5.75*inch;
        // private static final double kin_mount_to_edge_of_plate_y = 0.2*inch;
        protected static final double side_plate_cone_y = 2.0 * inch;

        public UChannelL46(String name, SurveyVolume m,
                AlignmentCorrection alignmentCorrection) {
            super(name, m, alignmentCorrection);
        }

        protected void setBoxDim() {
            setBoxDim(width, length, height);
        }

    }

    /**
     * {@link SurveyVolume} volume defining the coordinate system of the
     * u-channel Reference: SVTBox coordinate system Origin: midpoint between
     * upstream survey cones Orientation: u - width pointing towards electron
     * side, v - pointing along the U-channel in the beam direction
     * 
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     *
     */
    public static class UChannelL46Bottom extends UChannelL46 {

        protected static final double cone_to_edge_of_plate_y = 2.75 * inch;

        public UChannelL46Bottom(String name, SurveyVolume m,
                AlignmentCorrection alignmentCorrection) {
            super(name, m, alignmentCorrection);
            init();
        }

        protected void setCenter() {
            final double x = 0.0;
            final double y = -cone_to_edge_of_plate_y + length / 2.0;
            final double z = -side_plate_cone_y - UChannelL46Plate.height
                    + height / 2.0;
            setCenter(x, y, z);
        }

        protected void setPos() {
            ballPos = UChannelL46BottomSurveyBalls.ball_pos;
            Hep3Vector veeOffset = UChannelL46BottomSurveyBalls.getVeeOffset();
            veePos = VecOp.add(ballPos, veeOffset);
            Hep3Vector flatOffset = UChannelL46BottomSurveyBalls
                    .getFlatOffset();
            flatPos = VecOp.add(ballPos, flatOffset);
        }
    }

    /**
     * Position of the center of the survey balls when engaging the cones in the
     * side plates of the U-channel.
     * 
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     *
     */
    private static class UChannelL46BottomSurveyBalls {

        // Shawn's calculated point at midpoint between two forward survey balls
        protected final static Hep3Vector ball_pos = new BasicHep3Vector(
                -5.857, -157.776, -8.423);

        private static final double cone_fwd_right_x = -7.019 * inch;
        private static final double cone_fwd_right_y = -6.419 * inch;
        private static final double cone_fwd_right_z = -0.332 * inch;

        // private static final double cone_bwd_right_x = -6.539*inch;
        // private static final double cone_bwd_right_y = -22.159*inch;
        // private static final double cone_bwd_right_z = -0.332*inch;

        private static final double cone_fwd_left_x = 6.558 * inch;
        private static final double cone_fwd_left_y = -6.005 * inch;
        private static final double cone_fwd_left_z = -0.332 * inch;

        private static final double cone_bwd_left_x = 7.038 * inch;
        private static final double cone_bwd_left_y = -21.745 * inch;
        private static final double cone_bwd_left_z = -0.332 * inch;

        protected static final Hep3Vector fwd_right = new BasicHep3Vector(
                cone_fwd_right_x, cone_fwd_right_y, cone_fwd_right_z);
        protected static final Hep3Vector fwd_left = new BasicHep3Vector(
                cone_fwd_left_x, cone_fwd_left_y, cone_fwd_left_z);
        // protected static final Hep3Vector bwd_right = new
        // BasicHep3Vector(cone_bwd_right_x, cone_bwd_right_y,
        // cone_bwd_right_z);
        protected static final Hep3Vector bwd_left = new BasicHep3Vector(
                cone_bwd_left_x, cone_bwd_left_y, cone_bwd_left_z);

        protected static Hep3Vector getVeeOffset() {
            return VecOp.mult(0.5, VecOp.sub(fwd_right, fwd_left));
        }

        protected static Hep3Vector getFlatOffset() {
            return VecOp.sub(bwd_left, fwd_left);
        }
    }

    /**
     * {@link SurveyVolume} volume defining the coordinate system of the
     * u-channel Reference: {@link SVTBox} coordinate system Origin: midpoint
     * between upstream survey cones Orientation: u - width pointing towards
     * electron side, v - pointing along the U-channel in the beam direction
     * 
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     *
     */
    public static class UChannelL46Top extends UChannelL46 {

        private static final double cone_to_side_plate_pin_y = (0.875 - 0.25)
                * inch;
        private static final double side_plate_pin_to_edge_of_plate_y = 1.5 * inch;

        protected static final double cone_to_edge_of_plate_y = cone_to_side_plate_pin_y
                + side_plate_pin_to_edge_of_plate_y;

        public UChannelL46Top(String name, SurveyVolume m,
                AlignmentCorrection alignmentCorrection) {
            super(name, m, alignmentCorrection);
            init();
        }

        protected void setCenter() {
            final double x = 0.0;
            final double y = -cone_to_edge_of_plate_y + length / 2.0;
            final double z = -side_plate_cone_y - UChannelL46Plate.height
                    + height / 2.0;
            setCenter(x, y, z);
        }

        protected void setPos() {

            ballPos = UChannelL46TopSurveyBalls.ball_pos;
            Hep3Vector veeOffset = UChannelL46TopSurveyBalls.getVeeOffset();
            veePos = VecOp.add(ballPos, veeOffset);
            Hep3Vector flatOffset = UChannelL46TopSurveyBalls.getFlatOffset();
            flatPos = VecOp.add(ballPos, flatOffset);

            // ballPos = ball_pos;
            // veePos = new BasicHep3Vector(ballPos.x()+1, ballPos.y(),
            // ballPos.z()); // note sign change on random offset w.r.t. bottom
            // flatPos = new BasicHep3Vector(ballPos.x(), ballPos.y()-1,
            // ballPos.z()); // random offset
        }
    }

    /**
     * Position of the center of the survey balls when engaging the cones in the
     * side plates of the U-channel.
     * 
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     *
     */
    private static class UChannelL46TopSurveyBalls {

        // Shawn's calculated point at midpoint between two forward survey balls
        protected final static Hep3Vector ball_pos = new BasicHep3Vector(
                -6.341, -141.909, 8.423);

        protected static final double cone_fwd_right_x = -7.038 * inch;
        protected static final double cone_fwd_right_y = -5.794 * inch;
        protected static final double cone_fwd_right_z = 0.332 * inch;

        protected static final double cone_bwd_right_x = -6.558 * inch;
        protected static final double cone_bwd_right_y = -21.535 * inch;
        protected static final double cone_bwd_right_z = 0.332 * inch;

        protected static final double cone_fwd_left_x = 6.539 * inch;
        protected static final double cone_fwd_left_y = -5.380 * inch;
        protected static final double cone_fwd_left_z = 0.332 * inch;

        protected static final double cone_bwd_left_x = 7.019 * inch;
        protected static final double cone_bwd_left_y = -21.121 * inch;
        protected static final double cone_bwd_left_z = 0.332 * inch;

        protected static final Hep3Vector fwd_right = new BasicHep3Vector(
                cone_fwd_right_x, cone_fwd_right_y, cone_fwd_right_z);
        protected static final Hep3Vector fwd_left = new BasicHep3Vector(
                cone_fwd_left_x, cone_fwd_left_y, cone_fwd_left_z);
        protected static final Hep3Vector bwd_right = new BasicHep3Vector(
                cone_bwd_right_x, cone_bwd_right_y, cone_bwd_right_z);

        // protected static final Hep3Vector bwd_left = new
        // BasicHep3Vector(cone_bwd_left_x, cone_bwd_left_y, cone_bwd_left_z);

        protected static Hep3Vector getVeeOffset() {
            return VecOp.mult(0.5, VecOp.sub(fwd_left, fwd_right));
        }

        protected static Hep3Vector getFlatOffset() {
            return VecOp.sub(bwd_right, fwd_right);
        }
    }

    /**
     * Abstract {@link SurveyVolume} defining the coordinate system of the
     * u-channel plates
     * 
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     *
     */
    public abstract static class UChannelL46Plate extends UChannelPlate {
        public final static double pocket_depth_L4 = 0.1;
        public final static double pocket_depth_L5 = pocket_depth_L4 + 0.118;
        public final static double pocket_depth_L6 = pocket_depth_L5 + 0.118;
        public final static double module_mounting_hole_to_hole_x = 7.874 * inch;
        public static final double width = 13.5 * inch;
        public static final double length = 21.0 * inch;
        public static final double height = 0.5 * inch;

        public UChannelL46Plate(String name, SurveyVolume m,
                AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, m, alignmentCorrection, ref);
            setMaterial("Aluminum");
        }

        /**
         * Get pocket depth for this plate
         * 
         * @param layer
         * @return pocket depth
         */
        public static double getPocketDepth(int layer) {
            if (layer == 4)
                return pocket_depth_L4;
            else if (layer == 5)
                return pocket_depth_L5;
            else if (layer == 6)
                return pocket_depth_L6;
            else {
                throw new RuntimeException(
                        "Trying to create a L4-6 module with invalid layer nr: "
                                + layer);
            }
        }

        protected void setBoxDim() {
            setBoxDim(getWidth(), getLength(), getHeight());
        }

        protected void setPos() {
            ballPos = new BasicHep3Vector(0, 0, 0);
            veePos = new BasicHep3Vector(1, 0, 0);
            flatPos = new BasicHep3Vector(0, 1, 0);
        }

        public double getWidth() {
            return width;
        }

        public double getLength() {
            return length;
        }

        public double getHeight() {
            return height;
        }
    }

    /**
     * {@link SurveyVolume} defining the coordinate system of the bottom
     * u-channel plate Reference: @UChannelL13Bottom coordinate system Origin:
     * same as reference Orientation: same as reference
     *
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     *
     */
    public static class UChannelL46BottomPlate extends UChannelL46Plate {
        protected final static double L4_module_pin_to_edge_of_plate = 3.125 * inch;

        public UChannelL46BottomPlate(String name, SurveyVolume m,
                AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, m, alignmentCorrection, ref);
            init();
        }

        protected void setCenter() {
            final double x = 0.0;
            final double y = -UChannelL46Bottom.cone_to_edge_of_plate_y
                    + length / 2.0;
            final double z = -UChannelL46.side_plate_cone_y - height / 2.0;
            setCenter(x, y, z);
        }

    }

    /**
     * {@link SurveyVolume} defining the coordinate system of the top u-channel
     * plate Reference: @UChannelL13Top coordinate system Origin: same as
     * reference Orientation: same as reference
     *
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     *
     */
    public static class UChannelL46TopPlate extends UChannelL46Plate {
        protected final static double L4_module_pin_to_edge_of_plate = 1.75 * inch;

        public UChannelL46TopPlate(String name, SurveyVolume m,
                AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, m, alignmentCorrection, ref);
            init();
        }

        protected void setCenter() {
            final double x = 0.0;
            final double y = -UChannelL46Top.cone_to_edge_of_plate_y + length
                    / 2.0;
            final double z = -UChannelL46.side_plate_cone_y - height / 2.0;
            setCenter(x, y, z);
        }

    }

    /**
     * {@link SurveyVolume} volume defining the coordinate system of module L1-3
     * Reference: @UChannelL13Bottom coordinate system Origin: hole position on
     * mounting surface (on electron side) Orientation: u - is normal to the
     * surface pointing vertically down, v - points along module away from
     * hybrid side (i.e. positron direction).
     * 
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     *
     */
    public abstract static class ModuleL13 extends BaseModule {
        private final static double box_extra_length = 10.0;// random at this
                                                            // point
        private final static double box_extra_height = -0.45 * inch;// random at
                                                                    // this
                                                                    // point
        private final static double box_extra_width = 0.5 * inch;// random at
                                                                 // this point

        private static final double tension_lever_y = 2.5 * inch;
        // TODO the dimension of this volume is padded manually. Check if this
        // can cause overlap problems
        public static final double length = 8.0 * inch + box_extra_length;
        public static final double height = 1.0 * inch + box_extra_height;
        private static final double width = tension_lever_y + 0.04 * inch
                + box_extra_width;
        // private static final double hole_to_end_of_module_x = 7.750*inch;
        // private static final double hole_to_module_edge_height_dir = height -
        // 0.875*inch;
        protected static final double hole_to_center_of_plate_width_dir = 3.75 * inch;
        private static final double hole_to_module_edge_length_dir = 0.25 * inch;

        public ModuleL13(String name, SurveyVolume mother,
                AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, mother, alignmentCorrection, ref,
                    getLayerFromVolumeName(name), getHalfFromName(name));

        }

        protected void setCenter() {
            final double x = -width / 2.0;
            final double y = -hole_to_module_edge_length_dir + length / 2.0;
            // center this volume around the center of the module which is the
            // same as the cone for L1
            // final double z = hole_to_module_edge_height_dir - height/2.0;
            final double z = -Math.abs(getHoleModuleCenterOffset());
            setCenter(x, y, z);
        }

        protected void setBoxDim() {
            setBoxDim(width, length, height);
        }

        protected abstract void setPos();

        protected abstract double getHoleModuleCenterOffset();

        protected abstract Hep3Vector getHolePosition();
    }

    public abstract static class ModuleL13Top extends ModuleL13 {
        protected static final double cone_to_hole_across_uchannel = -95.25;

        public ModuleL13Top(String name, SurveyVolume mother,
                AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, mother, alignmentCorrection, ref);
        }

        protected void setPos() {
            ballPos = getHolePosition();
            veePos = new BasicHep3Vector(ballPos.x(), ballPos.y(),
                    ballPos.z() - 1.0);
            flatPos = new BasicHep3Vector(ballPos.x() + 1, ballPos.y(),
                    ballPos.z());
        }

        protected double getHoleModuleCenterOffset() {
            return UChannelL13Top.cone_to_edge_of_plate_y
                    - UChannelL13TopPlate.L1_module_pin_to_edge_of_plate;
        }

    }

    public abstract static class ModuleL13Bot extends ModuleL13 {
        protected static final double cone_to_hole_across_uchannel = 95.25;

        public ModuleL13Bot(String name, SurveyVolume mother,
                AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, mother, alignmentCorrection, ref);
        }

        protected void setPos() {
            ballPos = getHolePosition();
            veePos = new BasicHep3Vector(ballPos.x(), ballPos.y(),
                    ballPos.z() - 1.0);
            flatPos = new BasicHep3Vector(ballPos.x() - 1, ballPos.y(),
                    ballPos.z());
        }

        protected double getHoleModuleCenterOffset() {
            return UChannelL13Bottom.cone_to_edge_of_plate_y
                    - UChannelL13BottomPlate.L1_module_pin_to_edge_of_plate;
        }
    }

    public static class ModuleL1Bot extends ModuleL13Bot {
        protected final static double cone_to_hole_along_uchannel = 9.525;
        protected final static double cone_to_hole_vertical_from_uchannel = -51.435;

        public ModuleL1Bot(String name, SurveyVolume mother,
                AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, mother, alignmentCorrection, ref);
            init();
        }

        protected Hep3Vector getHolePosition() {
            double x = cone_to_hole_across_uchannel;
            double y = cone_to_hole_along_uchannel;
            double z = cone_to_hole_vertical_from_uchannel;
            return new BasicHep3Vector(x, y, z);
        }

    }

    public static class ModuleL1Top extends ModuleL13Top {

        protected final static double cone_to_hole_along_uchannel = -9.525;
        protected final static double cone_to_hole_vertical_from_uchannel = -51.435;

        public ModuleL1Top(String name, SurveyVolume mother,
                AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, mother, alignmentCorrection, ref);
            init();
        }

        protected Hep3Vector getHolePosition() {
            double x = cone_to_hole_across_uchannel; // note minus sign compared
                                                     // to bottom
            double y = cone_to_hole_along_uchannel;
            double z = cone_to_hole_vertical_from_uchannel;
            return new BasicHep3Vector(x, y, z);
        }

    }

    public static class ModuleL2Bot extends ModuleL13Bot {

        protected final static double cone_to_hole_along_uchannel = 109.525;
        protected final static double cone_to_hole_vertical_from_uchannel = ModuleL1Bot.cone_to_hole_vertical_from_uchannel - 1.5;

        public ModuleL2Bot(String name, SurveyVolume mother,
                AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, mother, alignmentCorrection, ref);
            init();
        }

        protected Hep3Vector getHolePosition() {
            double x = cone_to_hole_across_uchannel;
            double y = cone_to_hole_along_uchannel;
            double z = cone_to_hole_vertical_from_uchannel;
            return new BasicHep3Vector(x, y, z);
        }

    }

    public static class ModuleL2Top extends ModuleL13Top {
        protected final static double cone_to_hole_along_uchannel = 90.475;
        protected final static double cone_to_hole_vertical_from_uchannel = ModuleL1Top.cone_to_hole_vertical_from_uchannel - 1.5;

        public ModuleL2Top(String name, SurveyVolume mother,
                AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, mother, alignmentCorrection, ref);
            init();
        }

        protected Hep3Vector getHolePosition() {
            double x = cone_to_hole_across_uchannel; // note minus sign compared
                                                     // to bottom
            double y = cone_to_hole_along_uchannel;
            double z = cone_to_hole_vertical_from_uchannel;
            return new BasicHep3Vector(x, y, z);
        }

    }

    public static class ModuleL3Bot extends ModuleL13Bot {
        protected final static double cone_to_hole_along_uchannel = 209.525;
        protected final static double cone_to_hole_vertical_from_uchannel = ModuleL1Bot.cone_to_hole_vertical_from_uchannel - 2 * 1.5;

        public ModuleL3Bot(String name, SurveyVolume mother,
                AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, mother, alignmentCorrection, ref);
            init();
        }

        protected Hep3Vector getHolePosition() {
            double x = cone_to_hole_across_uchannel;
            double y = cone_to_hole_along_uchannel;
            double z = cone_to_hole_vertical_from_uchannel;
            return new BasicHep3Vector(x, y, z);
        }

    }

    public static class ModuleL3Top extends ModuleL13Top {
        protected final static double cone_to_hole_along_uchannel = 190.475;
        protected final static double cone_to_hole_vertical_from_uchannel = ModuleL1Top.cone_to_hole_vertical_from_uchannel - 2 * 1.5;

        public ModuleL3Top(String name, SurveyVolume mother,
                AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, mother, alignmentCorrection, ref);
            init();
        }

        protected Hep3Vector getHolePosition() {
            double x = cone_to_hole_across_uchannel; // note minus sign compared
                                                     // to bottom
            double y = cone_to_hole_along_uchannel;
            double z = cone_to_hole_vertical_from_uchannel;
            return new BasicHep3Vector(x, y, z);
        }

    }

    /**
     * Abstract {@link SurveyVolume} volume defining the coordinate system of
     * module L4-6
     * 
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     *
     */
    public abstract static class ModuleL46 extends BaseModule {
        protected final static double hole_to_center_of_plate_width_dir = 5.875 * inch;
        protected final static double hole_to_module_edge_height_dir = 0.875 * inch;
        protected static final double hole_to_module_edge_length_dir = 0.25 * inch;
        private final static double box_extra_length = 0.0;// random at this
                                                           // point
        private final static double box_extra_height = -0.45 * inch;// random at
                                                                    // this
                                                                    // point
        private final static double box_extra_width = 0.5 * inch;// random at
                                                                 // this point

        private static final double tension_lever_y = 2.5 * inch;
        // TODO the dimension of the L4-6 module is completely made up
        public static final double length = 12.25 * inch + box_extra_length;
        public static final double height = 1.0 * inch + box_extra_height;
        public static final double width = tension_lever_y + 0.04 * inch
                + box_extra_width;

        public ModuleL46(String name, SurveyVolume mother,
                AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, mother, alignmentCorrection, ref,
                    getLayerFromVolumeName(name), getHalfFromName(name));

        }

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

        protected abstract Hep3Vector getHole();
    }

    /**
     * Abstract {@link SurveyVolume} volume defining the coordinate system of
     * bottom modules for L4-6 Reference: @UChannelL46Bottom coordinate system
     * Origin: hole position on mounting surface (electron side) Orientation: u
     * - is normal to the mounting surface pointing vertically down, v - points
     * along module towards positron side.
     * 
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     *
     */
    public static abstract class ModuleL46Bot extends ModuleL46 {
        // positions are in the mother (U-channel) coord. systtem as usual
        protected final static double x = 149.225; // distance from survey ball
                                                   // to hole mounting surface
        protected final static double y = 9.525; // distance along U-channel
        protected final static double z = -53.34; // distance normal to the
                                                  // U-channel plate

        public ModuleL46Bot(String name, SurveyVolume mother,
                AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, mother, alignmentCorrection, ref);
        }

        protected void setPos() {
            ballPos = getHole();
            veePos = new BasicHep3Vector(ballPos.x(), ballPos.y(),
                    ballPos.z() - 1.0);
            flatPos = new BasicHep3Vector(ballPos.x() - 1.0, ballPos.y(),
                    ballPos.z());
        }

        protected double getHoleModuleCenterOffset() {
            return UChannelL46Bottom.cone_to_edge_of_plate_y
                    - UChannelL46BottomPlate.L4_module_pin_to_edge_of_plate;
        }
    }

    /**
     * Abstract {@link SurveyVolume} volume defining the coordinate system of
     * top modules for L4-6 Reference: @UChannelL46Top coordinate system Origin:
     * hole position on mounting surface (electron side when installed)
     * Orientation: u - is normal to the mounting surface pointing vertically
     * down, v - points along module towards electron side when installed.
     * 
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     *
     */
    public static abstract class ModuleL46Top extends ModuleL46 {
        // positions are in the mother (U-channel) coord. systtem as usual
        protected final static double x = -149.225; // distance from survey ball
                                                    // to hole mounting surface
        protected final static double y = -9.525; // distance along U-channel
        protected final static double z = -53.34; // distance normal to the
                                                  // U-channel plate

        public ModuleL46Top(String name, SurveyVolume mother,
                AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, mother, alignmentCorrection, ref);
        }

        protected void setPos() {
            ballPos = getHole();
            veePos = new BasicHep3Vector(ballPos.x(), ballPos.y(),
                    ballPos.z() - 1.0);
            flatPos = new BasicHep3Vector(ballPos.x() + 1.0, ballPos.y(),
                    ballPos.z());
        }

        protected double getHoleModuleCenterOffset() {
            return UChannelL46Top.cone_to_edge_of_plate_y
                    - UChannelL46TopPlate.L4_module_pin_to_edge_of_plate;
        }

    }

    public static class ModuleL4Bot extends ModuleL46Bot {

        public ModuleL4Bot(String name, SurveyVolume mother,
                AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, mother, alignmentCorrection, ref);
            init();
        }

        protected Hep3Vector getHole() {
            return new BasicHep3Vector(x, y, z);
        }

    }

    public static class ModuleL4Top extends ModuleL46Top {

        public ModuleL4Top(String name, SurveyVolume mother,
                AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, mother, alignmentCorrection, ref);
            init();
        }

        protected Hep3Vector getHole() {
            return new BasicHep3Vector(x, y, z);
        }

    }

    public static class ModuleL5Bot extends ModuleL46Bot {

        public ModuleL5Bot(String name, SurveyVolume mother,
                AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, mother, alignmentCorrection, ref);
            init();
        }

        protected Hep3Vector getHole() {
            double y_local = y + 200.0;
            double z_local = z - 3.0;
            return new BasicHep3Vector(x, y_local, z_local);
        }

    }

    public static class ModuleL5Top extends ModuleL46Top {

        public ModuleL5Top(String name, SurveyVolume mother,
                AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, mother, alignmentCorrection, ref);
            init();
        }

        protected Hep3Vector getHole() {
            double y_local = y + 200.0;
            double z_local = z - 3.0;
            return new BasicHep3Vector(x, y_local, z_local);
        }

    }

    public static class ModuleL6Bot extends ModuleL46Bot {

        public ModuleL6Bot(String name, SurveyVolume mother,
                AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, mother, alignmentCorrection, ref);
            init();
        }

        protected Hep3Vector getHole() {
            double y_local = y + 200.0 * 2;
            double z_local = z - 2 * 3.0;
            return new BasicHep3Vector(x, y_local, z_local);
        }

    }

    public static class ModuleL6Top extends ModuleL46Top {

        public ModuleL6Top(String name, SurveyVolume mother,
                AlignmentCorrection alignmentCorrection, SurveyVolume ref) {
            super(name, mother, alignmentCorrection, ref);
            init();
        }

        protected Hep3Vector getHole() {
            double y_local = y + 2 * 200.0;
            double z_local = z - 2 * 3.0;
            return new BasicHep3Vector(x, y_local, z_local);
        }

    }

    public abstract static class LongHalfModule extends BaseModule {

        // private static final double randomoffset = 5.0;
        public static final double width = Sensor.width; // + randomoffset;
        public static final double length = Sensor.length;// +
                                                          // randomoffset/10.0;
        public static final double height = Sensor.height
                + HalfLongModuleLamination.height;
        protected final static double sensor_z = 0.23 * inch;

        public LongHalfModule(String name, SurveyVolume mother,
                AlignmentCorrection alignmentCorrection, int layer, String half) {
            super(name, mother, alignmentCorrection, layer, half);
        }

        protected abstract Hep3Vector getSensorPosition();

        protected void setBoxDim() {
            setBoxDim(width, length, height);
        }

        protected void setCenter() {
            double x = 0.0;
            double y = 0.0;
            double z = +0.5 * Sensor.height - height / 2.0;
            ;
            setCenter(x, y, z);
        }

        protected void setPos() {
            ballPos = getSensorPosition(); // TODO make this get each coordinate
                                           // instead.
            veePos = new BasicHep3Vector(ballPos.x() + 1, ballPos.y(),
                    ballPos.z());
            flatPos = new BasicHep3Vector(ballPos.x(), ballPos.y() + 1,
                    ballPos.z());
        }

    }

    public static class LongAxialHoleHalfModule extends LongHalfModule {

        private final static double sensor_x = 1.382 * inch;
        private final static double sensor_y = 3.887 * inch;

        public LongAxialHoleHalfModule(String name, SurveyVolume mother,
                AlignmentCorrection alignmentCorrection, int layer, String half) {
            super(name, mother, alignmentCorrection, layer, half);
            init();
        }

        protected Hep3Vector getSensorPosition() {
            return new BasicHep3Vector(-sensor_x, sensor_y, -sensor_z);
        }

    }

    public abstract static class LongAxialSlotHalfModuleBase extends
            LongHalfModule {

        private final static double sensor_x = 1.382 * inch;
        private final static double sensor_y = 7.863 * inch;

        public LongAxialSlotHalfModuleBase(String name, SurveyVolume mother,
                AlignmentCorrection alignmentCorrection, int layer, String half) {
            super(name, mother, alignmentCorrection, layer, half);
        }

        protected Hep3Vector getSensorPosition() {
            return new BasicHep3Vector(-sensor_x, sensor_y, -sensor_z);
        }

    }

    public static class LongAxialSlotHalfModule extends
            LongAxialSlotHalfModuleBase {

        private final static double sensor_x = 1.382 * inch;
        private final static double sensor_y = 7.863 * inch;

        public LongAxialSlotHalfModule(String name, SurveyVolume mother,
                AlignmentCorrection alignmentCorrection, int layer, String half) {
            super(name, mother, alignmentCorrection, layer, half);
            init();
        }

        protected Hep3Vector getSensorPosition() {
            return new BasicHep3Vector(-sensor_x, sensor_y, -sensor_z);
        }
    }

    public abstract static class LongStereoHalfModule extends LongHalfModule {

        protected final static double sensor_z = 0.52 * inch;
        protected final static double stereo_angle = 0.05;

        public LongStereoHalfModule(String name, SurveyVolume mother,
                AlignmentCorrection alignmentCorrection, int layer, String half) {
            super(name, mother, alignmentCorrection, layer, half);
        }

        protected void applyGenericCoordinateSystemCorrections() {

            // Rotate these into the right place for the stereo
            // My rotations here are active rotations in the mother coordinate
            // system frame

            // flip around v ~ along the strips
            Rotation r1 = new Rotation(new Vector3D(0, 1, 0), Math.PI);
            // apply stereo angle around w ~ normal to the sensor plane
            Rotation r2 = new Rotation(new Vector3D(0, 0, 1), stereo_angle);
            // Build full rotation
            Rotation r = r2.applyTo(r1);
            // Rotation r = r1;
            if (debug) {
                System.out.printf(
                        "%s: LongStereoHalfModule Generic Corrections\n",
                        getClass().getSimpleName());
                System.out.printf("%s: Coord before corrections\n%s\n",
                        getClass().getSimpleName(), getCoord().toString());
                System.out.printf("%s: box center before corrections\n%s\n",
                        getClass().getSimpleName(), getBoxDim().toString());
            }

            getCoord().rotateApache(r);

            if (debug) {
                System.out.printf("%s: Coord after corrections\n%s\n",
                        getClass().getSimpleName(), getCoord().toString());
                System.out.printf("%s: box center after corrections\n%s\n",
                        getClass().getSimpleName(), getBoxDim().toString());
            }

        }

    }

    public static class LongStereoHoleHalfModule extends LongStereoHalfModule {

        private final static double sensor_x = 1.282 * inch;
        private final static double sensor_y = 3.889 * inch;

        public LongStereoHoleHalfModule(String name, SurveyVolume mother,
                AlignmentCorrection alignmentCorrection, int layer, String half) {
            super(name, mother, alignmentCorrection, layer, half);
            init();
        }

        protected Hep3Vector getSensorPosition() {
            return new BasicHep3Vector(-sensor_x, sensor_y, -sensor_z);
        }

    }

    public static abstract class LongStereoSlotHalfModuleBase extends
            LongStereoHalfModule {

        private final static double sensor_x = 1.481 * inch;
        private final static double sensor_y = 7.861 * inch;

        public LongStereoSlotHalfModuleBase(String name, SurveyVolume mother,
                AlignmentCorrection alignmentCorrection, int layer, String half) {
            super(name, mother, alignmentCorrection, layer, half);
        }

        protected Hep3Vector getSensorPosition() {
            return new BasicHep3Vector(-sensor_x, sensor_y, -sensor_z);
        }
    }

    public static class LongStereoSlotHalfModule extends
            LongStereoSlotHalfModuleBase {
        public LongStereoSlotHalfModule(String name, SurveyVolume mother,
                AlignmentCorrection alignmentCorrection, int layer, String half) {
            super(name, mother, alignmentCorrection, layer, half);
            init();
        }
    }

    /**
     * {@link SurveyVolume} volume defining the coordinate system of the axial
     * half-module in module L1-3 Reference: @ModuleL13Bot coordinate system
     * Origin: sensor center Orientation: w - is normal to the surface pointing
     * from p-side to n-side, v - points along strips away from signal bond pads
     * 
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     *
     */
    public static class HalfModuleAxial extends
            HPSTestRunTracker2014GeometryDefinition.TestRunHalfModule {
        // Sensor positions from Shawn's 3D model
        public static final double sensor_x = -1.543 * inch;
        public static final double sensor_y = 4.868 * inch;
        public static final double sensor_z = -0.23 * inch;

        public HalfModuleAxial(String name, SurveyVolume mother,
                AlignmentCorrection alignmentCorrection, int layer, String half) {
            super(name, mother, alignmentCorrection, layer, half);
            init();
        }

        protected void setPos() {
            ballPos = getSensorPosition();
            veePos = new BasicHep3Vector(ballPos.x() + 1, ballPos.y(),
                    ballPos.z());
            flatPos = new BasicHep3Vector(ballPos.x(), ballPos.y() + 1,
                    ballPos.z());
        }

        protected Hep3Vector getSensorPosition() {
            return new BasicHep3Vector(sensor_x, sensor_y, sensor_z);
        }

        protected void setCenter() {
            double x = -1.0
                    * (TestRunHalfModule.getWidth() / 2.0 - (12.66 - (8.83 - 3.00) + Sensor.width / 2.0));
            double y = TestRunHalfModule.getLength() / 2.0
                    - ((170.00 + 10.00) - Sensor.length / 2.0);
            double z = -Sensor.getSensorThickness()
                    / 2.0
                    - HPSTestRunTracker2014GeometryDefinition.HalfModuleLamination.thickness
                    - CarbonFiber.thickness + half_module_thickness / 2.0;
            setCenter(x, y, z);
        }

        @Override
        protected void setStereoAngle() {
            // do nothing here
        }
    }

    /**
     * {@link SurveyVolume} volume defining the coordinate system of the stereo
     * half-module in module L1-3 Reference: @ModuleL13Bot coordinate system
     * Origin: sensor center Orientation: same as axial - the module is rotated
     * later.
     * 
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     *
     */
    public static class HalfModuleStereo extends
            HPSTestRunTracker2014GeometryDefinition.TestRunHalfModule {

        public static final double sensor_z = -0.52 * inch;
        protected static final double stereo_angle_value = 0.1;

        public HalfModuleStereo(String name, SurveyVolume mother,
                AlignmentCorrection alignmentCorrection, int layer, String half) {
            super(name, mother, alignmentCorrection, layer, half);
            setStereoAngle();
            init();
        }

        protected void setPos() {
            // Sensor positions from Shawn's 3D model
            final double x = HalfModuleAxial.sensor_x;
            final double y = HalfModuleAxial.sensor_y;
            final double z = sensor_z;
            ballPos = new BasicHep3Vector(x, y, z);
            veePos = new BasicHep3Vector(ballPos.x() + 1, ballPos.y(),
                    ballPos.z());
            flatPos = new BasicHep3Vector(ballPos.x(), ballPos.y() + 1,
                    ballPos.z());
        }

        protected void setCenter() {
            double x = -1.0
                    * (TestRunHalfModule.getWidth() / 2.0 - (12.66 - (8.83 - 3.00) + Sensor.width / 2.0));
            double y = TestRunHalfModule.getLength() / 2.0
                    - ((170.00 + 10.00) - Sensor.length / 2.0);
            double z = -Sensor.getSensorThickness()
                    / 2.0
                    - HPSTestRunTracker2014GeometryDefinition.HalfModuleLamination.thickness
                    - CarbonFiber.thickness + half_module_thickness / 2.0;
            setCenter(x, y, z);
        }

        protected void applyGenericCoordinateSystemCorrections() {

            // Rotate into the right place for the stereo - just offset compared
            // to axial before this.
            // My rotations here are active rotations in the mother coordinate
            // system frame
            // Sloppy description of the frame:
            // x: direction along strips towards the readout bonds/apv25's
            // v: normal to sensor plane pointing from the back-plane (n-side)
            // to strip side (p-side)
            // w: measurement direction with direction from right hand-rule

            // flip around strip direction - sign doesn't matter
            Rotation r1 = new Rotation(new Vector3D(0, 1, 0), Math.PI);
            // apply stereo angle around v
            Rotation r2 = new Rotation(new Vector3D(0, 0, 1), stereo_angle);
            // Build full rotation
            Rotation r = r2.applyTo(r1);
            // Rotation r = r1;
            if (debug)
                System.out.printf("%s: Coord before corrections\n%s\n",
                        getClass().getSimpleName(), getCoord().toString());
            if (debug)
                System.out.printf("%s: box center before corrections\n%s\n",
                        getClass().getSimpleName(), getBoxDim().toString());
            getCoord().rotateApache(r);
            if (debug)
                System.out.printf("%s: Coord after corrections\n%s\n",
                        getClass().getSimpleName(), getCoord().toString());
            if (debug)
                System.out.printf("%s: box center after corrections\n%s\n",
                        getClass().getSimpleName(), getBoxDim().toString());

        }

        protected void setStereoAngle() {
            stereo_angle = stereo_angle_value;
        }

    }

    public static class HalfLongModuleLamination extends
            HPSTestRunTracker2014GeometryDefinition.HalfModuleComponent {
        protected static final double width = Sensor.width;
        protected static final double length = Sensor.length;
        protected static final double height = 0.050;

        public HalfLongModuleLamination(String name, SurveyVolume mother, int id) {
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
            ballPos = new BasicHep3Vector(0, 0,
                    -(Sensor.getSensorThickness() / 2.0 + height / 2.0));
            veePos = new BasicHep3Vector(ballPos.x() + 1, ballPos.y(),
                    ballPos.z());
            flatPos = new BasicHep3Vector(ballPos.x(), ballPos.y() + 1,
                    ballPos.z());
        }

        protected void setCenter() {
            setCenter(0, 0, 0);
        }

        protected void setBoxDim() {
            setBoxDim(width, length, height);
        }
    }

    /**
     * Create the module.
     * 
     * @param layer
     *            - of the module
     * @param half
     *            - top or bottom half of the tracker
     */
    protected void makeModuleBundle(int layer, String half) {

        final SurveyVolume mother = getSurveyVolume(SvtBox.class);
        final SurveyVolume ref;
        AlignmentCorrection alignmentCorrection = new AlignmentCorrection();
        alignmentCorrection.setNode(node);
        if (half == "bottom") {
            if (layer < 4) {
                ref = getSurveyVolume(UChannelL13Bottom.class);
            } else {
                ref = getSurveyVolume(UChannelL46Bottom.class);
            }
        } else {
            if (layer < 4) {
                ref = getSurveyVolume(UChannelL13Top.class);
            } else {
                ref = getSurveyVolume(UChannelL46Top.class);
            }
        }

        makeModuleBundle(layer, half, mother, ref);

    }

    /**
     * Create the module.
     * 
     * @param layer
     *            - of the module
     * @param half
     *            - top or bottom half of the tracker
     * @param mother
     *            - mother volume
     * @param ref
     *            - reference volume
     */
    protected void makeModuleBundle(int layer, String half,
            SurveyVolume mother, SurveyVolume ref) {

        if (isDebug())
            System.out.printf("%s: makeModule for layer %d %s \n", this
                    .getClass().getSimpleName(), layer, half);

        // Create the module
        BaseModule module = createModule(half, layer, mother, ref);

        // create the bundle for this module
        // need to create it and add to list before half-module is created
        // as it uses the list to find the bundle. Ugly. TODO fix this.
        BaseModuleBundle bundle;

        if (layer <= 3) {
            bundle = new TestRunModuleBundle(module);
            addModuleBundle(bundle);
            if (doAxial)
                makeHalfModule("axial", module);
            // if(doColdBlock) makeColdBlock(module);
            if (doStereo)
                makeHalfModule("stereo", module);
        } else {
            bundle = new LongModuleBundle(module);
            addModuleBundle(bundle);
            if (doAxial) {
                makeLongHalfModule("axial", "hole", module);
                makeLongHalfModule("axial", "slot", module);
            }
            // if(doColdBlock) makeColdBlock(module);
            if (doStereo) {
                makeLongHalfModule("stereo", "hole", module);
                makeLongHalfModule("stereo", "slot", module);
            }
        }

        if (isDebug()) {
            System.out.printf("%s: created module bundle:\n", this.getClass()
                    .getSimpleName());
            bundle.print();
            System.out.printf("%s: Now there are %d  modules\n", this
                    .getClass().getSimpleName(), modules.size());
        }

    }

    /**
     * Create a {@link BaseModule} object.
     * 
     * @param half
     *            - top or bottom string
     * @param layer
     *            - layer integer
     * @param mother
     *            - mother {@link SurveyVolume}
     * @param ref
     *            - reference {@link SurveyVolume}
     * @return the created {@link BaseModule}
     */
    protected BaseModule createModule(String half, int layer,
            SurveyVolume mother, SurveyVolume ref) {

        // build the module name
        String volName = "module_L" + layer + (half == "bottom" ? "b" : "t");

        // find alignment corrections
        AlignmentCorrection alignmentCorrection = new AlignmentCorrection();
        alignmentCorrection.setNode(node);

        BaseModule module;
        if (half == "bottom") {
            if (layer == 1) {
                module = new ModuleL1Bot(volName, mother, alignmentCorrection,
                        ref);
            } else if (layer == 2) {
                module = new ModuleL2Bot(volName, mother, alignmentCorrection,
                        ref);
            } else if (layer == 3) {
                module = new ModuleL3Bot(volName, mother, alignmentCorrection,
                        ref);
            } else if (layer == 4) {
                module = new ModuleL4Bot(volName, mother, alignmentCorrection,
                        ref);
            } else if (layer == 5) {
                module = new ModuleL5Bot(volName, mother, alignmentCorrection,
                        ref);
            } else if (layer == 6) {
                module = new ModuleL6Bot(volName, mother, alignmentCorrection,
                        ref);
            } else {
                throw new UnsupportedOperationException("Layer " + layer
                        + " not implemented yet for bottom");
            }
        } else {
            if (layer == 1) {
                module = new ModuleL1Top(volName, mother, alignmentCorrection,
                        ref);
            } else if (layer == 2) {
                module = new ModuleL2Top(volName, mother, alignmentCorrection,
                        ref);
            } else if (layer == 3) {
                module = new ModuleL3Top(volName, mother, alignmentCorrection,
                        ref);
            } else if (layer == 4) {
                module = new ModuleL4Top(volName, mother, alignmentCorrection,
                        ref);
            } else if (layer == 5) {
                module = new ModuleL5Top(volName, mother, alignmentCorrection,
                        ref);
            } else if (layer == 6) {
                module = new ModuleL6Top(volName, mother, alignmentCorrection,
                        ref);
            } else {
                throw new UnsupportedOperationException("Layer " + layer
                        + " not implemented yet for top");
            }
        }
        return module;
    }

    /**
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     *
     */
    public static class LongModuleBundle extends BaseModuleBundle {
        public HalfModuleBundle halfModuleAxialHole = null;
        public HalfModuleBundle halfModuleStereoHole = null;
        public HalfModuleBundle halfModuleAxialSlot = null;
        public HalfModuleBundle halfModuleStereoSlot = null;
        protected SurveyVolume coldBlock = null;

        public LongModuleBundle(BaseModule m) {
            super(m);
        }

        public void print() {
            if (module != null)
                System.out.printf("%s: %s\n", this.getClass().getSimpleName(),
                        module.toString());
            if (halfModuleAxialHole != null)
                halfModuleAxialHole.print();
            if (halfModuleAxialSlot != null)
                halfModuleAxialSlot.print();
            if (coldBlock != null)
                System.out.printf("%s: %s\n", this.getClass().getSimpleName(),
                        coldBlock.getName());
            if (halfModuleStereoHole != null)
                halfModuleStereoHole.print();
            if (halfModuleStereoSlot != null)
                halfModuleStereoSlot.print();
        }
    }

    /**
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     *
     */
    public static class LongHalfModuleBundle extends HalfModuleBundle {
        public LongHalfModuleBundle() {
            super();
        }

        public LongHalfModuleBundle(SurveyVolume hm) {
            super(hm);
        }
    }

    /**
     * Create the half-module.
     * 
     * @param side
     *            - stereo or axial
     * @param type
     *            - hole or slot
     * @param mother
     *            to the half-module
     */
    protected void makeLongHalfModule(String side, String type,
            BaseModule mother) {

        String moduleName = mother.getName();

        if (isDebug())
            System.out.printf("%s: makeHalfModule for %s %s %s \n", this
                    .getClass().getSimpleName(), moduleName, side, type);

        String volName = moduleName + "_halfmodule_" + side + "_" + type;

        // top or bottom?
        String half = mother.getHalf();
        boolean isTopLayer = !mother.isBottom();

        // find layer
        int layer = mother.getLayer();

        // axial or stereo
        boolean isAxial = isAxialFromName(volName);

        // hole or slot
        boolean isHole = isHoleFromName(volName);

        // find layer according to Millepede layer definition
        int millepedeLayer = getMillepedeLayer(isTopLayer, layer, isAxial,
                isHole);

        // find alignment correction to this volume
        AlignmentCorrection alignmentCorrection = getHalfModuleAlignmentCorrection(
                isTopLayer, millepedeLayer);
        alignmentCorrection.setNode(node);

        // find the module bundle that it will be added to
        // TestRunModuleBundle bundle =
        // (TestRunModuleBundle)getModuleBundle(mother);
        // TestRunHalfModuleBundle halfModuleBundle;
        LongModuleBundle bundle = (LongModuleBundle) getModuleBundle(mother);

        // Build the half-module bundle and half-module
        // TODO clean this up to a separate method
        LongHalfModule halfModule;
        HalfModuleBundle halfModuleBundle;
        if (isAxial) {
            halfModuleBundle = new LongHalfModuleBundle();
            if (isHole) {
                halfModule = new LongAxialHoleHalfModule(volName, mother,
                        alignmentCorrection, layer, half);
                bundle.halfModuleAxialHole = halfModuleBundle;
            } else {
                halfModule = createLongAxialSlotHalfModule(volName, mother,
                        alignmentCorrection, layer, half);
                bundle.halfModuleAxialSlot = halfModuleBundle;
            }
        } else {
            halfModuleBundle = new LongHalfModuleBundle();
            if (isHole) {
                halfModule = new LongStereoHoleHalfModule(volName, mother,
                        alignmentCorrection, layer, half);
                bundle.halfModuleStereoHole = halfModuleBundle;
            } else {
                halfModule = createLongStereoSlotHalfModule(volName, mother,
                        alignmentCorrection, layer, half);
                bundle.halfModuleStereoSlot = halfModuleBundle;
            }
        }
        halfModuleBundle.halfModule = halfModule;

        // create the half module components
        makeHalfModuleComponentSensor(halfModule);

        makeLongHalfModuleComponentKapton(halfModule);

        // makeHalfModuleComponentCF(halfModule);

        // makeHalfModuleComponentHybrid(halfModule);

    }

    protected void makeLongHalfModuleComponentKapton(BaseModule mother) {

        if (isDebug())
            System.out.printf("%s: makeHalfModuleComponentKapton for %s \n",
                    this.getClass().getSimpleName(), mother.getName());

        String volName = mother.getName() + "_lamination";

        // Build the half-module

        // id is hard coded
        int component_number = 2;

        HalfLongModuleLamination lamination = new HalfLongModuleLamination(
                volName, mother, component_number);
        lamination.setMaterial("Kapton");

        HalfModuleBundle hm = getHalfModuleBundle(
                (BaseModule) mother.getMother(), mother.getName());
        hm.lamination = lamination;

    }

    protected HPSTestRunTracker2014GeometryDefinition.TestRunHalfModule createTestRunHalfModuleAxial(
            String volName, BaseModule mother,
            AlignmentCorrection alignmentCorrection, int layer, String half) {
        return new HalfModuleAxial(volName, mother, alignmentCorrection, layer,
                half);

    }

    protected HPSTestRunTracker2014GeometryDefinition.TestRunHalfModule createTestRunHalfModuleStereo(
            String volName, BaseModule mother,
            AlignmentCorrection alignmentCorrection, int layer, String half) {
        return new HalfModuleStereo(volName, mother, alignmentCorrection,
                layer, half);

    }

    /**
     * Create {@link LongAxialSlotHalfModule} {@link SurveyVolume}.
     * 
     * @param name
     * @param mother
     * @param alignmentCorrection
     * @param layer
     * @param half
     * @return
     */
    protected LongHalfModule createLongAxialSlotHalfModule(String name,
            SurveyVolume mother, AlignmentCorrection alignmentCorrection,
            int layer, String half) {
        return new LongAxialSlotHalfModule(name, mother, alignmentCorrection,
                layer, half);
    }

    /**
     * Create {@link LongStereoSlotHalfModule} {@link SurveyVolume}.
     * 
     * @param name
     * @param mother
     * @param alignmentCorrection
     * @param layer
     * @param half
     * @return
     */
    protected LongHalfModule createLongStereoSlotHalfModule(String name,
            SurveyVolume mother, AlignmentCorrection alignmentCorrection,
            int layer, String half) {
        return new LongStereoSlotHalfModule(name, mother, alignmentCorrection,
                layer, half);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.lcsim.geometry.compact.converter.HPSTrackerGeometryDefinition#
     * getHalfModuleBundle
     * (org.lcsim.geometry.compact.converter.HPSTestRunTracker2014GeometryDefinition
     * .BaseModule, java.lang.String)
     */
    protected HalfModuleBundle getHalfModuleBundle(BaseModule module,
            String halfModuleName) {
        BaseModuleBundle moduleBundle = getModuleBundle(module);
        HalfModuleBundle hm = null;
        if (moduleBundle != null) {
            if (moduleBundle instanceof TestRunModuleBundle) {
                TestRunModuleBundle mtr = (TestRunModuleBundle) moduleBundle;
                if (halfModuleName.contains("axial")) {
                    hm = mtr.halfModuleAxial;
                } else if (halfModuleName.contains("stereo")) {
                    hm = mtr.halfModuleStereo;
                } else {
                    throw new RuntimeException(
                            "NO axial or stereo string found in half module bundle name "
                                    + halfModuleName);
                }
            } else if (moduleBundle instanceof LongModuleBundle) {
                LongModuleBundle longModuleBundle = (LongModuleBundle) moduleBundle;
                if (halfModuleName.contains("axial")) {
                    if (halfModuleName.contains("hole")) {
                        hm = longModuleBundle.halfModuleAxialHole;
                    } else if (halfModuleName.contains("slot")) {
                        hm = longModuleBundle.halfModuleAxialSlot;
                    } else {
                        throw new RuntimeException(
                                "This half-module name \""
                                        + halfModuleName
                                        + " \" is invalid. Need to contain hole or slot for this type.");
                    }
                } else if (halfModuleName.contains("stereo")) {
                    if (halfModuleName.contains("hole")) {
                        hm = longModuleBundle.halfModuleStereoHole;
                    } else if (halfModuleName.contains("slot")) {
                        hm = longModuleBundle.halfModuleStereoSlot;
                    } else {
                        throw new RuntimeException(
                                "This half-module name \""
                                        + halfModuleName
                                        + " \" is invalid. Need to contain hole or slot for this type.");
                    }
                } else {
                    throw new RuntimeException(
                            "This half-module name \""
                                    + halfModuleName
                                    + " \" is invalid. Need to contain axial or stereo.");
                }
            } else {
                throw new NotImplementedException(
                        "This type of module bundle is not implemented!?");
            }
        } else {
            throw new RuntimeException("Couldn't find module "
                    + module.getName() + " and layer " + module.getLayer()
                    + " and half " + module.getHalf());
        }
        return hm;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.lcsim.geometry.compact.converter.HPSTrackerBuilder#getMillepedeLayer
     * (java.lang.String)
     */
    public int getMillepedeLayer(String name) {

        boolean isTopLayer = getHalfFromName(name).equals("top") ? true : false;

        // find layer
        int layer = getLayerFromVolumeName(name);

        // axial or stereo
        boolean isAxial = isAxialFromName(name);

        // use default layer numbering for L1-3
        if (layer < 4) {
            return getOldLayerDefinition(isTopLayer, layer, isAxial);
        }

        // hole or slot
        boolean isHole = isHoleFromName(name);

        return getMillepedeLayer(isTopLayer, layer, isAxial, isHole);

    }

    /**
     * Definition relating the sensors and layer number used in millepede for
     * this detector.
     * 
     * @param isTopLayer
     * @param layer
     * @param isAxial
     * @param isHole
     * @return
     */
    public int getMillepedeLayer(boolean isTopLayer, int layer,
            boolean isAxial, boolean isHole) {
        int l = -1;
        // use default layer numbering for L1-3
        if (layer < 4) {
            l = getOldLayerDefinition(isTopLayer, layer, isAxial);
        } else {
            // Scheme:
            // For top modules axial layer is odd and stereo is even.
            // Hole vs slot given by example below:
            // e.g. top layer 4:
            // axial - hole: 7
            // axial - slot: 9
            // stereo - hole: 8
            // axial - slot: 10

            l = 7 + (layer - 4) * 4;
            int s = -1;
            if (isTopLayer) {
                s = 0;
                if (isAxial) {
                    s += 0;
                } else {
                    s += 1;
                }
                if (isHole) {
                    s += 0;
                } else {
                    s += 2;
                }
            } else {
                s = 0;
                if (!isAxial) {
                    s += 0;
                } else {
                    s += 1;
                }
                if (isHole) {
                    s += 0;
                } else {
                    s += 2;
                }
            }
            l = l + s;
        }

        if (l < 0)
            throw new RuntimeException("Error getting the millepede layer.");

        if (isDebug())
            System.out.printf("%s: %s %d %s %s -> MP layer %d\n", getClass()
                    .getSimpleName(), isTopLayer ? "top" : "bottom", layer,
                    isAxial ? "axial" : "stereo", isHole ? "hole" : "slot", l);

        return l;
    }

}
