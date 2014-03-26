/*
 * Simple java program to generate single particles of fixed momentum uniformly
 * distributed in pseudorapidity.
 */
package org.hps.util;

import hep.io.stdhep.StdhepEvent;
import hep.io.stdhep.StdhepWriter;
import hep.physics.particle.properties.ParticleType;
import hep.physics.particle.properties.ParticlePropertyManager;
import java.io.IOException;
import java.util.Random;

/**
 *
 * @author partridge
 */
public class GenerateSingleParticles {

    //  Edit the following declarations to set desired paramters
    static String dir = "/nfs/sulky21/g.ec.u12/users/mgraham/DarkPhoton/SingleParticleSamples/";        // Output directory
//    static String file = "electron_6GeV_10spills";  // File name
    static String file = "electron_6GeV_10000_XDir";  // File name
    static String ext = "stdhep";      // File extension
//    static int nevt = 15000*10;            // Number of events to generate
    static int nevt = 10000;            // Number of events to generate
    static int pdgid = 11;             // PDG code for particle to generate (11=electrons, 13 = muons, 211 = pions)
    static boolean flipsign = false;    // Set to false if you only want a specific charge
    static double pmin = 6.;         // Minimum particle momentum at 90 degrees (GeV)
    static double pmax = 1.;         // Maximum particle momentum at 90 degrees (GeV)
    static boolean pfixed = true;      // Set to false for fixed pt
    static double sigx = 0.001;        // Luminous region size in x (mm)
    static double sigy = 0.001;        // Luminous region size in y (mm)
    static double sigz = 0.001;         // Luminous region size in z (mm)

    /*
    static double sigpx = 0.001;        // momentum spread
    static double sigpy = 0.001;        // 
    static double sigpz = 0.001;         //
*/
    static double sigpx = 0.6;        // momentum spread
    static double sigpy = 0.6;        //
    static double sigpz = 0.001;         //
    public static void main(String[] args) throws IOException {

        //  Instantiate the random number generators
        Random generator = new Random();

        //  Decode the pdgid
        ParticleType pid = ParticlePropertyManager.getParticlePropertyProvider().get(pdgid);
        String pname = pid.getName();

        //  Open the output file
        String fname = dir + file + "." + ext;
        StdhepWriter sw = new StdhepWriter(fname, "Single particles",
                "Single particles", nevt);
        sw.setCompatibilityMode(false);

        //  Loop over the events
        for (int icross = 0; icross < nevt; icross++) {

            //  Generate the momentum at 90 degrees
            double ptot = pmin + (pmax - pmin) * generator.nextDouble();
            double px = sigpx * generator.nextGaussian();
            double py = sigpy * generator.nextGaussian();
            //  Generate the pseudorapidity and calculate the polar angle

            //  Figure out the transverse momentum and mass

            double pt = Math.sqrt(px * px + py * py);
            double theta = Math.asin(pt / ptot);
            double m = pid.getMass();

            //  Generate the aximutha angle
            double phi = 2. * Math.PI * generator.nextDouble();

            //  Generate the IP
            double x0 = sigx * generator.nextGaussian();
            double y0 = sigy * generator.nextGaussian();
            double z0 = sigz * generator.nextGaussian();
            double t0 = 0.;

            //  Fill the event record variables
            //  Set the event number
            int nevhep = icross;

            //  Set the number of particles in the event
            int nhep = 1;

            //  Set the status code of the particle
            int isthep[] = new int[2];
            isthep[0] = 1;

            //  Set the particle ID of the particle
            int idhep[] = new int[2];
            idhep[0] = pdgid;
            if (flipsign && generator.nextDouble() > 0.5) {
                idhep[0] = -pdgid;
            }

            //  Set the mother and daughter pointers
            int jmohep[] = new int[2];
            int jdahep[] = new int[2];
            for (int i = 0; i < 2; i++) {
                jmohep[i] = 0;
                jdahep[i] = 0;
            }

            //  Set the momentum/energy/mass
            double phep[] = new double[5];
//            phep[0] = pt * Math.cos(phi);
//            phep[1] = pt * Math.sin(phi);
//            phep[2] = pt / Math.tan(theta);
// swap x and z
            phep[2] = pt * Math.cos(phi);
            phep[1] = pt * Math.sin(phi);
            phep[0] = pt / Math.tan(theta);
            double p2 = pt * pt + phep[2] * phep[2];
            phep[3] = Math.sqrt(p2 + m * m);
            phep[4] = m;

            //  Set the particle origin
            double vhep[] = new double[4];
//            vhep[0] = x0;
//            vhep[1] = y0;
//           vhep[2] = z0;
            vhep[2] = x0;
            vhep[1] = y0;
            vhep[0] = z0;
            vhep[3] = t0;

            //  Create an event record
            StdhepEvent ev = new StdhepEvent(nevhep, nhep, isthep, idhep, jmohep, jdahep, phep, vhep);

            //  Write out the event record
            sw.writeRecord(ev);
        }

        //  Done with generating particles - close the file
        sw.close();
    }
}