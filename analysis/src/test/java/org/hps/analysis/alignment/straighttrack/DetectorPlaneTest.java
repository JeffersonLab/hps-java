package org.hps.analysis.alignment.straighttrack;

import Jama.Matrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import java.util.ArrayList;
import junit.framework.TestCase;
import static org.hps.analysis.alignment.straighttrack.FitTracks.PROD_ROT;
import static org.hps.analysis.alignment.straighttrack.FitTracks.GEN_ROTMAT;

/**
 *
 * @author ngraf
 */
public class DetectorPlaneTest extends TestCase {

    public void testIt() {
        String name = "module_L7t_halfmodule_stereo_slot_sensor0";
        double[] pos = {77.7196486644766, 29.98559999999985, 893.8027834140355};
        double[] angles = {-3.141592653589793, -0.030483299558163854, 1.6207963267948962};
        double[] normal = {0.030479, -2.2111e-16, 0.99954};
        double[] sigs = {0.0055, 0.0};
        double[] rot = {-0.04995594995825787, -0.9987502603949663, 0.001523294047485972, -0.99828626119962, 0.04997916927067786, 0.030440488483217336, -0.030478578770211602, 1.2240778529834114E-16, -0.9995354202008792};
        double[] SIGS = {0.0055, 0.00};     //  detector resolutions in mm

        Hep3Vector uDir = new BasicHep3Vector(-0.049956, -0.99875, 0.0015233);
        Hep3Vector vDir = new BasicHep3Vector(-0.99829, 0.049979, 0.030440);
        Hep3Vector origin = new BasicHep3Vector(pos);
        Hep3Vector normalVec = new BasicHep3Vector(normal);

        Matrix[] mats = new Matrix[3];
        double[][] RW = new double[3][9];
        for (int j = 0; j < 3; ++j) {
            double angl = angles[j];
            mats[j] = GEN_ROTMAT(angl, j);
            GEN_ROTMAT(angl, j, RW[j]);
            System.out.println("Rotation matrix for axis " + (j + 1) + " angle " + angl);
            mats[j].print(6, 4);
        }
        Matrix prodrot = PROD_ROT(mats[0], mats[1], mats[2]);
        // calculate zmin, zmax
        double width = 20.;
        double height = 10.;
        double[] bounds = DetectorBuilder.findZBounds(origin, VecOp.mult(width, vDir), VecOp.mult(height, uDir));
        double zmin = bounds[0];
        double zmax = bounds[1];
        System.out.println("zmin " + zmin + " zmax " + zmax);
        DetectorPlane dp = new DetectorPlane(14, prodrot, origin.v(), SIGS);
        dp.setName(name);
        dp.setUVWR(uDir, vDir, normalVec, origin);
        dp.setAngles(angles);

        System.out.println(dp);

        // test global to local and vice versa
        ArrayList<Hep3Vector> corners = new ArrayList<>();
        corners.add(new BasicHep3Vector(57.254, 20.998, 894.43));
        corners.add(new BasicHep3Vector(58.253, 40.973, 894.40));
        corners.add(new BasicHep3Vector(97.186, 18.999, 893.21));
        corners.add(new BasicHep3Vector(98.185, 38.974, 893.18));

        for (Hep3Vector global : corners) {
            Hep3Vector local = dp.globalToLocal(global);
        }

        ArrayList<Hep3Vector> lcorners = new ArrayList<>();
        lcorners.add(new BasicHep3Vector(-height, -width, 0.));
        lcorners.add(new BasicHep3Vector(-height, width, 0.));
        lcorners.add(new BasicHep3Vector(height, width, 0.));
        lcorners.add(new BasicHep3Vector(height, -width, 0.));
        Hep3Vector globalOrigin = dp.localToGlobal(new BasicHep3Vector(0., 0., 0.));
        for (Hep3Vector local : lcorners) {
            Hep3Vector global = dp.localToGlobal(local);
        }

    }
}
