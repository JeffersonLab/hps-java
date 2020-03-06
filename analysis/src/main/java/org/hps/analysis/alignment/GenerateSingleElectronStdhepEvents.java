package org.hps.analysis.alignment;

import hep.io.stdhep.StdhepEndRun;
import hep.io.stdhep.StdhepEvent;
import hep.io.stdhep.StdhepWriter;
import java.io.IOException;
import static java.lang.Math.sqrt;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generate single electrons for alignment and calibration studies.
 *
 * @author Norman Graf
 */
public class GenerateSingleElectronStdhepEvents {

    public static void main(String[] args) throws IOException {
        int nEvents = 900000; // A start
        if (args.length > 2) {
            nEvents = Integer.parseInt(args[2]);
        }
        double eEnergy = 4.55;
        if (args.length > 1) {
            eEnergy = Double.parseDouble(args[1]);
        }
        double zTarget = 0.0;
        if (args.length > 0) {
            zTarget = Double.parseDouble(args[0]);
        }
        System.out.println("Generating "+nEvents+" at "+eEnergy+" GeV with z = "+zTarget);
        // but there is some efficiency and acceptance to account for, so allow for more to be generated.
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
        int idhep[] = {11}; // electron
        int jmohep[] = {0, 0};
        int jdahep[] = {0, 0};
        double phep[] = new double[5];
        // Interaction point
        // Start at (0,0,0) for now
        double vhep[] = {0., 0., 0., 0.};
        double emass = 0.000511;
        double mass2 = emass * emass;

        // eCal dimensions
        double xMax = 360.;
        double xMin = -280.;
        double yMin = 20;
        double yMax = 90.;
        double zEcal = 1390.;
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
//        double xMax = 25.;// -5.;
//        double xMin = -45.; //-72.+5.;
//        double yMin = 12.;
//        double yMax = 52.;
//        double zSVTLayer1 = 889.;

        double z = zEcal-zTarget;
        double p = sqrt(eEnergy * eEnergy - mass2);

        Random ran = new Random();
        StdhepWriter topEvents = new StdhepWriter("hpsForwardFullEnergyElectrons_z"+zTarget+"_"+eEnergy+"GeV_top.stdhep", "Generated Stdhep Events", "top", 250000);
        StdhepWriter bottomEvents = new StdhepWriter("hpsForwardFullEnergyElectrons_z"+zTarget+"_"+eEnergy+"GeV_bottom.stdhep", "Generated Stdhep Events", "bottom", 250000);

        topEvents.setCompatibilityMode(false);
        bottomEvents.setCompatibilityMode(false);
        // evenly populate face of ECal
        for (int i = 0; i < nEvents; ++i) {
            double x = ThreadLocalRandom.current().nextDouble(xMin, xMax) - vhep[0];
            double y = ThreadLocalRandom.current().nextDouble(yMin, yMax);
            double r = sqrt(x * x + y * y + z * z);
            phep[0] = p * x / r; //px
            phep[1] = p * y / r; //py
            phep[2] = p * z / r; //pz
            phep[3] = eEnergy;
            phep[4] = emass;

            StdhepEvent ev = new StdhepEvent(i, 1, isthep, idhep, jmohep, jdahep, phep, vhep);
            topEvents.writeRecord(ev);
            phep[1] = -phep[1];
            ev = new StdhepEvent(i, 1, isthep, idhep, jmohep, jdahep, phep, vhep);
            bottomEvents.writeRecord(ev);
        }

        int nreqe = (int) nEvents;
        int ngene = (int) nEvents;
        int nwrite = (int) nEvents;
        float ecme = (float) 2.3;
        float xsece = (float) 99999997952.;
        double rn1e = 12345321;
        double rn2e = 66666666;
        StdhepEndRun se = new StdhepEndRun(nreqe, ngene, nwrite, ecme, xsece, rn1e, rn2e);
        topEvents.writeRecord(se);
        topEvents.close();

        bottomEvents.writeRecord(se);
        bottomEvents.close();

    }
}
