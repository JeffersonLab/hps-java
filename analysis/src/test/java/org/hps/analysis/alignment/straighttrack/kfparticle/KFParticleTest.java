/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.analysis.alignment.straighttrack.kfparticle;

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;
import junit.framework.TestCase;

/**
 *
 * @author ngraf
 */
public class KFParticleTest extends TestCase {

    public void testKFParticleMethods() {
        double x = 0.;
        double y = 2.;
        double z = 10.;
        double xprime = 0.; //dx/dz
        double yprime = 0.2; //dy/dz
        int q = 1;
        double mom = 10.;

        double zOffset = 0.;
        //                   x   y   x'   y' q/p  z
        double[] t1params = {x, y, xprime, yprime, q / mom, z - zOffset};
        double[] t1cov = new double[15];
        for (int i = 0; i < 15; i++) {
            t1cov[i] = 0.;
        }
        t1cov[0] = 1.;
        t1cov[2] = 1.;
        t1cov[5] = 1.;
        t1cov[9] = 1.;
        t1cov[14] = 1.;
        double mass = 0.511;
        double chi2 = 1.;
        boolean isElectron = true;
        int NDF = 5;
        KFTrack t1 = new KFTrack(t1params, t1cov, mass, chi2, isElectron, NDF);

        KFParticle p1 = new KFParticle(t1);
        //note that chi2 and ndf for p1 are NOT those of the track. They are from the fits to tracks
        assertEquals(x, p1.GetX());
        assertEquals(y, p1.GetY());
        assertEquals(z - zOffset, p1.GetZ());
        assertEquals(p1.GetMomentum(), mom, 1E-10);

        double[] origin = new double[3];
        double straightLineDistance = sqrt(x * x + y * y + (z - zOffset) * (z - zOffset));
        double ds1 = p1.GetDStoPoint(origin);
        //note that S is signed path/momentum!
        System.out.println("distance to origin: " + ds1 * mom + " expected: " + straightLineDistance);
        // in zero field these should be equal
        assertEquals(abs(ds1 * mom), straightLineDistance, 1E-10);
        // in zero B field these should be equal
        assertEquals(p1.GetDStoPoint(origin), p1.GetDStoPointLine(origin));
    }

    public void testKFParticleTwoBodyVertexingZeroBField() {
        double x = 0.;
        double y = 2.;
        double z = 10.;
        double xprime = 0.; //dx/dz
        double yprime = 0.2; //dy/dz
        int q = 1;
        double mom = 1.;

        double zOffset = -1.;
        //                   x   y   x'   y' q/p  z
        double[] t1params = {x, y, xprime, yprime, q / mom, z - zOffset};
        double[] t1cov = new double[15];
        for (int i = 0; i < 15; i++) {
            t1cov[i] = 0.;
        }
        t1cov[0] = 1.;
        t1cov[2] = 1.;
        t1cov[5] = 1.;
        t1cov[9] = 1.;
        t1cov[14] = 1.;
        double mass = 0.511;
        double chi2 = 1.;
        boolean isElectron = true;
        int NDF = 5;
        KFTrack t1 = new KFTrack(t1params, t1cov, mass, chi2, isElectron, NDF);

        KFParticle p1 = new KFParticle(t1);
        //note that chi2 and ndf for p1 are NOT those of the track. They are from the fits to tracks
        assertEquals(x, p1.GetX());
        assertEquals(y, p1.GetY());
        assertEquals(z - zOffset, p1.GetZ());

        // TODO exercise particle methods...
        // let's create another particle
        // track 2
        //                   x   y   x'   y' q/p  z
        double[] t2params = {0., -5., 0., -0.5, -1., 10. - zOffset};
        double[] t2cov = new double[15];

        t2cov[0] = 1.;
        t2cov[2] = 1.;
        t2cov[5] = 1.;
        t2cov[9] = 1.;
        t2cov[14] = 1.;
        KFTrack t2 = new KFTrack(t2params, t2cov, mass, chi2, isElectron, NDF);
        KFParticle p2 = new KFParticle(t2);
        // OK, can we create a composite particle?
        KFParticle p = new KFParticle(p1, p2);

        System.out.println(p.GetX() + " " + p.GetY() + " " + p.GetZ());
        //damn!!! that's impressive! It just works!

//	p.TransportToProductionVertex();
        System.out.println("pos: " + p.GetX() + " " + p.GetY() + " " + p.GetZ());
        System.out.println("mom: " + p.GetPx() + " " + p.GetPy() + " " + p.GetPz());
        System.out.println("mass: " + p.GetMass() + " charge: " + p.GetQ());
        // note that there is a method SetProductionVertex which should
        // presumably be used to set the primary vertex, or target
        // position

    }

    public void testFieldOffData() {
        double zWire = -2338;
        double mass = 0.511;
        double chi2 = 1.;
        boolean isElectron = true;
        int NDF = 5;
        double[] t1 = {-66.48626693163084, -0.6808786175420553, 0.02428397066518905, -0.014866312254313186, 1., zWire};
        double[] t2 = {-69.67450024009771, -1.6247051626064148, 0.011960239026825992, -0.008852486371903726, 1., zWire};
        double[] t3 = {-58.155936100013484, -1.479480927314696, 0.008032417140393737, -0.006655462674078146, 1., zWire};
        double[] t4 = {-67.09573395865574, -1.2969624141736695, 0.020097938627936057, -0.010900971110678843, 1., zWire};
        double[] t5 = {-65.23775636088055, -0.661979044559828, 0.017216099311644997, -0.00462952620851782, 1., zWire};
        double[] t6 = {-61.0113158434742, 1.9869596271738161, 0.022605786916267752, -0.009287203223089369, 1., zWire};
        double[] t7 = {-62.06319935457753, -0.6635976546556667, 0.02642688773533541, -0.004951455067623932, 1., zWire};
        double[] t8 = {-60.812406432515395, 0.6989434207239655, 0.008916609861558955, -0.007596079762517236, 1., zWire};
        double[] t9 = {-62.898284743742636, 1.2894560421891894, 0.022569242965493227, -0.01411610508148105, 1., zWire};

        double[] cov1 = {0.36605541487791204, 0.013485455975794473, 8.989288135593836E-4, -1.3786173521217502E-4, -5.011322286118674E-6, 5.226591436420421E-8, -4.974576834678909E-6, -3.235987983373748E-7, 1.8618112727794733E-9, 1.1751066674852724E-10, 0., 0., 0., 0., 1.};
        double[] cov2 = {0.3449942734518463, 0.009651547148419004, 7.821081859777398E-4, -1.302021404188756E-4, -3.4460358939967424E-6, 4.9526324422869394E-8, -3.468732873010859E-6, -2.7208126092106194E-7, 1.251613463670531E-9, 9.5473443716933E-11, 0., 0., 0., 0., 1.};
        double[] cov3 = {0.5555380608255467, 0.018666779180462754, 0.001196872848193346, -2.0592402430092804E-4, -6.864215462144115E-6, 7.677353766185759E-8, -6.848675188534643E-6, -4.287069224220117E-7, 2.5342566798540725E-9, 1.5476195910832544E-10, 0., 0., 0., 0., 1.};
        double[] cov4 = {0.41837978218920946, 0.013088740792325266, 9.214361789541082E-4, -1.5969146025130904E-4, -4.8641460227430936E-6, 6.129764837735481E-8, -4.902146323433325E-6, -3.336172366894938E-7, 1.8381212931224923E-9, 1.220153119077029E-10, 0., 0., 0., 0., 1.};
        double[] cov5 = {0.328855451311414, 0.00986776205029398, 6.571673059427029E-4, -1.2550706220846515E-4, -3.6743759775965152E-6, 4.826623130490362E-8, -3.6574949416852347E-6, -2.35937998135741E-7, 1.3754697140442899E-9, 8.574171157364473E-11, 0., 0., 0., 0., 1.};
        double[] cov6 = {0.6788981287450124, 0.021786713969135847, 0.0012951364052968715, -2.5903223959227215E-4, -8.284897204306374E-6, 9.920345242625789E-8, -8.352298283936582E-6, -4.845154021832067E-7, 3.1915280572768524E-9, 1.825724073191702E-10, 0., 0., 0., 0., 1.};
        double[] cov7 = {0.40503009337444856, 0.01271589848345388, 9.111220960599615E-4, -1.540657066985349E-4, -4.703909275033619E-6, 5.892384047855435E-8, -4.7168477509621906E-6, -3.2824322630657996E-7, 1.758719145989803E-9, 1.192735185756266E-10, 0., 0., 0., 0., 1.};
        double[] cov8 = {0.37938740013727407, 0.012271121995073611, 8.492330613918448E-4, -1.4085468439126105E-4, -4.470616085743114E-6, 5.274996164542447E-8, -4.468454640776244E-6, -2.987860148973707E-7, 1.6445126302521586E-9, 1.0630717489198393E-10, 0., 0., 0., 0., 1.};
        double[] cov9 = {0.5426627062771504, 0.023260811587628195, 0.0015202886747852252, -2.052226944669244E-4, -8.705676854984887E-6, 7.799023360552354E-8, -8.605933412048287E-6, -5.529765365567892E-7, 3.23665056329879E-9, 2.02362979244542E-10, 0., 0., 0., 0., 1.};

        KFTrack trk1 = new KFTrack(t1, cov1, mass, chi2, isElectron, NDF);
        KFTrack trk2 = new KFTrack(t2, cov2, mass, chi2, isElectron, NDF);
        KFTrack trk3 = new KFTrack(t3, cov3, mass, chi2, isElectron, NDF);
        KFTrack trk4 = new KFTrack(t4, cov4, mass, chi2, isElectron, NDF);
        KFTrack trk5 = new KFTrack(t5, cov5, mass, chi2, isElectron, NDF);
        KFTrack trk6 = new KFTrack(t6, cov6, mass, chi2, isElectron, NDF);
        KFTrack trk7 = new KFTrack(t7, cov7, mass, chi2, isElectron, NDF);
        KFTrack trk8 = new KFTrack(t8, cov8, mass, chi2, isElectron, NDF);
        KFTrack trk9 = new KFTrack(t9, cov9, mass, chi2, isElectron, NDF);

        KFParticle p1 = new KFParticle(trk1);
        KFParticle p2 = new KFParticle(trk2);
        KFParticle p3 = new KFParticle(trk3);
        KFParticle p4 = new KFParticle(trk4);
        KFParticle p5 = new KFParticle(trk5);
        KFParticle p6 = new KFParticle(trk6);
        KFParticle p7 = new KFParticle(trk7);
        KFParticle p8 = new KFParticle(trk8);
        KFParticle p9 = new KFParticle(trk9);

        KFParticle p = new KFParticle(p1, p2);
        System.out.println("pos: " + p.GetX() + " " + p.GetY() + " " + p.GetZ());
        p.AddDaughter(p3);
        p.AddDaughter(p4);
        p.AddDaughter(p5);
        p.AddDaughter(p6);
        p.AddDaughter(p7);
        p.AddDaughter(p8);
        p.AddDaughter(p9);
        System.out.println("pos: " + p.GetX() + " " + p.GetY() + " " + p.GetZ());
    }

}
