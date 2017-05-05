/*
 * Simple java program to generate single particles of fixed momentum uniformly
 * distributed in pseudorapidity.
 */
package org.hps.util;

import hep.io.stdhep.StdhepEvent;
import hep.io.stdhep.StdhepWriter;
import hep.physics.particle.properties.ParticlePropertyManager;
import hep.physics.particle.properties.ParticleType;

import java.io.IOException;
import java.util.Random;

public class GenerateBunches {

    //  Edit the following declarations to set desired paramters
    static String dir = "/a/surrey10/vol/vol0/g.hps/mgraham/DarkPhoton/SingleParticleSamples/5.5GeV-100u/";        // Output directory
// static String dir = "/a/surrey10/vol/vol0/g.hps/mgraham/DarkPhoton/SingleParticleSamples/";
    static String file = "electron_5.5GeV_7.5ns_100na_400bunches_";  // File name
//    static String file = "electron_6GeV_25ns_10bunches_";  // File name

    static int nevt = 400;            // Number of events/file to generate
    static int nfiles = 400;             //number of files to generate
    static int start=400;

    static double current = 100;  //nanoamps
    static double bunchtime=7.5;  //nanoseconds

    static double convert=6.25;   //electrons/ns in a nanoamp
    static String ext = "stdhep";      // File extension

//    static int nele = 15000;            // Number of elecrons in a bunch (25ns)
    static int nele = (int) Math.round(current*bunchtime*convert);            // Number of elecrons in a bunch (7.5ns)
    
    
    static int pdgid = 11;             // PDG code for particle to generate (11=electrons, 13 = muons, 211 = pions)
    static boolean flipsign = false;    // Set to false if you only want a specific charge
    static double pmin = 5.5;         // Minimum particle momentum at 90 degrees (GeV)
    static double pmax = 5.5;         // Maximum particle momentum at 90 degrees (GeV)
    static boolean pfixed = true;      // Set to false for fixed pt
    static double sigx = 0.1;        // Luminous region size in x (mm)
    static double sigy = 0.1;        // Luminous region size in y (mm)
    static double sigz = 0.001;         // Luminous region size in z (mm)
    static double sigpx = 0.001;        // momentum spread
    static double sigpy = 0.001;        // 
    static double sigpz = 0.001;         //
    static double xoff = -1.0;         // offset of the beam in the xdir (so that it goes through the target)

    public static void main(String[] args) throws IOException {

        //  Instantiate the random number generators
        Random generator = new Random();
        generator.setSeed(start);
        //  Decode the pdgid
        ParticleType pid = ParticlePropertyManager.getParticlePropertyProvider().get(pdgid);
        String pname = pid.getName();


        for (int n = start; n < start+nfiles; n++) {
            //  Open the output file
            String fname = dir + file + n + "." + ext;
            StdhepWriter sw = new StdhepWriter(fname, "Bunch",
                    "Bunch", nevt);
            sw.setCompatibilityMode(false);

            //  Loop over the events
            for (int icross = 0; icross < nevt; icross++) {

                //  Generate the momentum at 90 degrees


                //  Fill the event record variables
                //  Set the event number
                int nevhep = icross;

                //  Set the number of particles in the event
                int nhep = nele;
                int isthep[] = new int[nhep];
                int idhep[] = new int[nhep];
                int jmohep[] = new int[2 * nhep];
                int jdahep[] = new int[2 * nhep];
                double phep[] = new double[5 * nhep];
                double vhep[] = new double[4 * nhep];
                for (int jj = 0; jj < nhep; jj++) {

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
                    //  Set the status code of the particle

                    isthep[jj] = 1;

                    //  Set the particle ID of the particle

                    idhep[jj] = pdgid;
                    if (flipsign && generator.nextDouble() > 0.5)
                        idhep[jj] = -pdgid;

                    //  Set the mother and daughter pointers

                    for (int i = 0; i < 2; i++) {
                        jmohep[2 * jj + i] = 0;
                        jdahep[2 * jj + i] = 0;
                    }

                    //  Set the momentum/energy/mass

//            phep[0] = pt * Math.cos(phi);
//            phep[1] = pt * Math.sin(phi);
//            phep[2] = pt / Math.tan(theta);
// swap x and z
                    phep[5 * jj + 2] = pt * Math.cos(phi);
                    phep[5 * jj + 1] = pt * Math.sin(phi);
                    phep[5 * jj + 0] = pt / Math.tan(theta);
                    double p2 = pt * pt + phep[2] * phep[2];
                    phep[5 * jj + 3] = Math.sqrt(p2 + m * m);
                    phep[5 * jj + 4] = m;

                    //  Set the particle origin

//            vhep[0] = x0;
//            vhep[1] = y0;
//           vhep[2] = z0;
                    vhep[4 * jj + 2] = x0;
                    vhep[4 * jj + 1] = y0;
                    vhep[4 * jj + 0] = z0 + xoff;
                    vhep[4 * jj + 3] = t0;
                }
                //  Create an event record
                StdhepEvent ev = new StdhepEvent(nevhep, nhep, isthep, idhep, jmohep, jdahep, phep, vhep);

                //  Write out the event record
                sw.writeRecord(ev);
            }

            //  Done with generating particles - close the file
            sw.close();
        }
    }
}