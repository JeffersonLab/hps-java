package org.hps.analysis.MC;

import hep.io.stdhep.StdhepEndRun;
import hep.io.stdhep.StdhepEvent;
import hep.io.stdhep.StdhepWriter;
import hep.physics.particle.properties.ParticlePropertyManager;
import hep.physics.particle.properties.ParticlePropertyProvider;
import hep.physics.particle.properties.ParticleType;
import java.io.IOException;
import static java.lang.Math.sqrt;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * @author Norman A Graf
 */
public class GenerateSingleParticleStdhepEvents {

    public static void main(String[] args) throws IOException {

        // remind user of correct usage
        if (args.length < 5) {
            usage();
        }
        if (args.length == 1 && args[0].equals("-h")) {
            usage();
        }

        int pdgId = Integer.parseInt(args[0]);
        boolean chargeConjugate = Boolean.parseBoolean(args[1]);
        double energy = Double.parseDouble(args[2]);
        double zPos = Double.parseDouble(args[3]);
        int nEvents = Integer.parseInt(args[4]);

        ParticlePropertyProvider pinfo = ParticlePropertyManager.getParticlePropertyProvider();
        ParticleType ptype = pinfo.get(pdgId);
        double mass = ptype.getMass();
        double charge = ptype.getCharge();
        String pname = ptype.getName();
        int sign = 1;
        if (chargeConjugate) {
            sign = -1;
        }
        System.out.println("mass= " + mass);
        System.out.println("Generating " + nEvents + " " + pname + (chargeConjugate ? " of both charges " : " ") + "with " + energy + " GeV at z= " + zPos + " mm.");

        /*--------------------------------------------------------*/
 /* NEVHEP          - event number (or some special meaning*/
 /*                    (see documentation for details)     */
 /* NHEP            - actual number of entries in current  */
 /*                    event.                              */
 /* ISTHEP[IHEP]    - status code for IHEP'th entry - see  */
 /*                    documentation for details           */
 /* IDHEP [IHEP]    - IHEP'th particle identifier according*/
 /*                    to PDG.                             */
 /* JMOHEP[IHEP][0] - pointer to position of 1st mother    */
 /* JMOHEP[IHEP][1] - pointer to position of 2nd mother    */
 /* JDAHEP[IHEP][0] - pointer to position of 1st daughter  */
 /* JDAHEP[IHEP][1] - pointer to position of 2nd daughter  */
 /* PHEP  [IHEP][0] - X momentum [Gev/c]                   */
 /* PHEP  [IHEP][1] - Y momentum [Gev/c]                   */
 /* PHEP  [IHEP][2] - Z momentum [Gev/c]                   */
 /* PHEP  [IHEP][3] - Energy [Gev]                         */
 /* PHEP  [IHEP][4] - Mass[Gev/c^2]                        */
 /* VHEP  [IHEP][0] - X vertex [mm]                        */
 /* VHEP  [IHEP][1] - Y vertex [mm]                        */
 /* VHEP  [IHEP][2] - Z vertex [mm]                        */
 /* VHEP  [IHEP][3] - production time [mm/c]               */
 /*========================================================*/
//IstHep convention:
//                            0      - final state particle if JdaHEP=0
//                                     intermediate particle if JdaHEP>0
//                                     (NEUGEN extension; was "null")
//                            1      - final state particle
//                            2      - intermediate state
//                            3      - documentation line
//                            4-10   - reserved for future
//                            11-200 - reserved for specific model use
//                            201+   - reserved for users
        int isthep[] = {1};
        int pdg = pdgId;
        int idhep[] = {pdg};
        int jmohep[] = {0, 0};
        int jdahep[] = {0, 0};
        double phep[] = new double[5];
        double wireZ2019 = -2267.;
        double xAtWire2019 = -65.;
//        double vhep[] = {-63., 0., -2338., 0.};
//        double emass = 0.000511;
        double mass2 = mass * mass;
//        double eEnergy = 4.556;

        // eCal dimensions
        // can't use ECal for straight-throughs!
        double xMaxECal = 360.;
        double xMinECal = -280.;
        double yMinECal = 20;
        double yMaxECal = 93.;
//        double zEcal = 1390.;
// use SVT axial sensor rough dimensions and layout
// can't use layer 1!
//        double xMax = 45.;
//        double xMin = -45.;
//        double yMin = 1.3;
//        double yMax = 39.;
//        double zSVTLayer1 = 86.;
//// use SVT axial sensor rough dimensions and layout for last layer
//        double xMax = 25. -5.;
//        double xMin = -72.+5.;
//        double yMin = 12.;
//        double yMax = 52.;
//        double zSVTLayer1 = 889.;
// with x offset at HARP, need to make some adjustments...
// use SVT axial sensor rough dimensions and layout for last layer
        double xMaxSVT = 25.;// -5.;
        double xMinSVT = -45.; //-72.+5.;
        double yMinSVT = 12.;
        double yMaxSVT = 52.;

        double xMax = xMaxECal;
        double xMin = xMinECal;
        double yMin = yMinECal;
        double yMax = yMaxECal;

        double zSVTLayer1 = 889.;
        double eCalFace2019 = 1439.;
        double ip2019 = -7.5;

        double zPlane = eCalFace2019;
        double targetX = 0.; //xAtWire;
        double zTarget = zPos; //wireZ2019

        double z = zPlane - zTarget; // last SVT layer + HARP wire location
        double p = sqrt(energy * energy - mass2);

        Random ran = new Random();
        StdhepWriter eventWriter = new StdhepWriter("hpsForward_" + pname + "_" + energy + "GeV_z" + zTarget + ".stdhep", "Generated Stdhep Events", "top", 250000);
//        StdhepWriter bottomEvents = new StdhepWriter("hpsForwardFullEnergyElectrons_z" + zTarget + "_bottom.stdhep", "Generated Stdhep Events", "bottom", 250000);

        eventWriter.setCompatibilityMode(false);
//        bottomEvents.setCompatibilityMode(false);

        double vhep[] = {targetX, 0., zTarget, 0.};
        // evenly populate face of ECal
        for (int i = 0; i < nEvents; ++i) {
            pdg *= sign;
            idhep[0] = pdg;
            double x = ThreadLocalRandom.current().nextDouble(xMin, xMax) - vhep[0];
            double y = ThreadLocalRandom.current().nextDouble(yMin, yMax);
            double r = sqrt(x * x + y * y + z * z);
            phep[0] = p * x / r; //px
            // randomize top and bottom
            int top = Math.random() < 0.5 ? 1 : -1;
            phep[1] = top * p * y / r; //py
            phep[2] = p * z / r; //pz
            phep[3] = energy;
            phep[4] = mass;

            StdhepEvent ev = new StdhepEvent(i, 1, isthep, idhep, jmohep, jdahep, phep, vhep);
            eventWriter.writeRecord(ev);
//            phep[1] = -phep[1];
//            ev = new StdhepEvent(i, 1, isthep, idhep, jmohep, jdahep, phep, vhep);
//            bottomEvents.writeRecord(ev);
        }

        int nreqe = (int) nEvents;
        int ngene = (int) nEvents;
        int nwrite = (int) nEvents;
        float ecme = (float) energy;
        float xsece = (float) 99999997952.;
        double rn1e = 12345321;
        double rn2e = 66666666;
        StdhepEndRun se = new StdhepEndRun(nreqe, ngene, nwrite, ecme, xsece, rn1e, rn2e);
        eventWriter.writeRecord(se);
        eventWriter.close();

//        bottomEvents.writeRecord(se);
//        bottomEvents.close();
    }

    public static void usage() {
        System.out.println("GenerateSingleParticleStdhepEvents: \n  an application to generate single particle events in stdhep format.\n");
        System.out.println("Usage: \n\n >> java GenerateSingleParticleStdhepEvents particleType conjugate energy(GeV) targetZposition(mm) nEvents\n");
        System.out.println("\n e.g. >> java GenerateSingleParticleStdhepEvents 11 false 4.55 -7.5 10000\n");
        System.out.println("  will generate 10k single full energy (4.55GeV) electrons evenly spread over the face of the ECal starting at z=-7.5mm");
        System.exit(0);
    }
}
