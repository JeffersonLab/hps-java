package org.hps.util;

/**
 *
 * @author mgraham borrowed liberally from hep.lcio.util.StdhepConverter
 * created on 7/27/2019
 */
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import hep.io.stdhep.StdhepEvent;
import hep.io.stdhep.StdhepReader;
import hep.io.stdhep.StdhepRecord;
import hep.physics.particle.properties.ParticlePropertyManager;
import hep.physics.particle.properties.ParticlePropertyProvider;
import hep.physics.particle.properties.ParticleType;
import hep.physics.particle.properties.UnknownParticleIDException;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.BasicHepLorentzVector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.HepLorentzVector;
import java.io.EOFException;
import java.util.Random;
import org.lcsim.event.MCParticle;
import org.lcsim.event.base.BaseMCParticle;

public class ReadStdHepFile {

    static boolean expDecay = true;
    static boolean flatDecay = false;
    static boolean trident = false;
    static double _declength = 0.0;//A' decay length (mm)
    static double _xoff = 0.03;
    static double _yoff = 0;
    static double _zoff = 0;
    static double sigx = 0.0;        // Luminous region size in x (mm)
    static double sigy = 0.01;        // Luminous region size in y (mm)
    static double sigz = 0.01;         // Luminous region size in z (mm)
    static double aMass = 0.05;  //Aprime mass (GeV)
    static double maxLen = 200; // maximum decay length (mm)
    static double _ecm = 5.5; //GeV
    static StdhepReader sr;
    static int nmax = 500000;
    private static final double c_light = 2.99792458e+8;
    private static ParticlePropertyProvider ppp = ParticlePropertyManager.getParticlePropertyProvider();

    /**
     * @param args the command line arguments
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        int nInFiles = 100;
        int iStart = 100;
        double apMass = 0.08;
        _ecm = 2.2;
        _declength = 0;//mm
        sigx = 0.02;
        sigy = 0.2;

        String fileLabel = "tritrigv2_MG5_ESum2GeV";
        String postfix = "_20u_beamspot_gammactau_5cm.stdhep";

        for (int i = iStart; i < iStart + nInFiles; i++) {
            int fnum = i + 1;
            String snum = "_" + fnum;
//            if (fnum < 1000) snum = "_0" + fnum;
//            if (fnum < 100) snum = "_00" + fnum;
//            if (fnum < 10) snum = "_000" + fnum;

            aMass = apMass;
            String file = "/Users/mgraham/hps/Data/PhysicsRun2019/stdhep/" + fileLabel + snum + ".stdhep";
            String infile = file;
            openStdHepFile(infile);
            int nread = process();
            closeStdHepFile();
        }
       
    }

    private static void openStdHepFile(String infile) throws IOException {
        sr = new StdhepReader(infile);
    }

    private static int process() throws IOException {
        Random generator = new Random();
        int nread = 0;

        try {
            // Loop over all records in the Stdhep file.
            for (;;) {
//            if (maxEvents != -1 && cntr >= maxEvents)
//               break;

                // Get the next Stdhep event.
                StdhepRecord record = sr.nextRecord();
                System.out.println("Got next record");

                // Only process StdhepEvent records.
                if (record instanceof StdhepEvent) {
                    // Convert to an LCCollection of MCParticle objects.
                    System.out.println("Got an event");
                    List<MCParticle> mcpcoll = convert((StdhepEvent) record);
                    for (MCParticle mcp : mcpcoll)
                        System.out.println("Four vector = " + mcp.asFourVector().toString());
                    nread++;
                }

            }

        } catch (EOFException e) {
            // End of Stdhep file.
        }

        return nread;
    }

    private static void closeStdHepFile() throws IOException {

        sr.close();
        System.out.println("Ok...done!");
    }

    /**
     * Convert a StdhepEvent to an LCCollection of MCParticle objects.
     *
     * @param hepevt The StdhepEvent to be converted.
     * @return An LCCollection of MCParticle objects converted from hepevt.
     */
    public static List<MCParticle> convert(StdhepEvent hepevt) {
        List<MCParticle> mcpcoll = new ArrayList<MCParticle>();

        int n = hepevt.getNHEP();

//        MCParticle particles[] = new MCParticle[n];

        for (int i = 0; i < n; i++) {

            // Add MCParticle to the temp array.
            // Set vertex from VHEP.
            double vertex[]
                    = {hepevt.getVHEP(i, 0), hepevt.getVHEP(i, 1), hepevt.getVHEP(i, 2)};
            Hep3Vector origin = new BasicHep3Vector(vertex);
            // Set momentum from PHEP.
            double momentum[]
                    = {hepevt.getPHEP(i, 0), hepevt.getPHEP(i, 1), hepevt.getPHEP(i, 2)};
            Hep3Vector mom = new BasicHep3Vector(momentum);
            // Lookup the particle by PDG using the Particle Property Provider.
            ParticleType type = null;
            double charge;
            try {
                // Get the particle type.
                type = ppp.get(hepevt.getIDHEP(i));

                charge = (float) type.getCharge();
            } catch (UnknownParticleIDException e) {
                // Flag the particle with NaN for unknown charge.
                charge = Float.NaN;
            }
            // get status from ISTEP
            int status = hepevt.getISTHEP(i);

            // Set mass from PHEP.
            double mass = hepevt.getPHEP(i, 4);

            // Set PDG from IDHEP.
            int pdgID = hepevt.getIDHEP(i);

            //           particle.setGeneratorStatus(hepevt.getISTHEP(i));
            // Set time from VHEP(4).
            // Convert to mm/c^2 from mm/c, as in slic/StdHepToLcioConvertor .
            double time = hepevt.getVHEP(i, 3) / c_light;
            //get 4-momentum from mass & mom
            HepLorentzVector fourMom = new BasicHepLorentzVector(mass, mom);
            // Create new MCParticle for this Stdhep record.
            MCParticle particle = new BaseMCParticle(origin, fourMom, type, status, time);
//            particles[i] = particle;
            mcpcoll.add(particle);

        }

//         int[] vec = new int[n];
//      //List<Set<BasicParticle>> ancestors = new ArrayList<Set<BasicParticle>>(n);
//      List ancestors = new ArrayList();
//      for (int i = 0; i < n; i++)
//         ancestors.add(new HashSet());
//      // Deal with parents
//      for (int i = 0; i < n; i++)
//      {
//         int idx1 = hepevt.getJMOHEP(i, 0) - 1;
//         int idx2 = hepevt.getJMOHEP(i, 1) - 1;
//         int l = fillIndexVec(vec, idx1, idx2);
//         //System.out.println("parent: "+i+" "+idx1+" "+idx2+" "+l);
//         for (int j = 0; j < l; j++)
//         {
//            checkAndAddDaughter(particles, ancestors, vec[j], i);
//         }
//      }
//      // Deal with daughters
//      for (int i = 0; i < n; i++)
//      {
//         int idx1 = hepevt.getJDAHEP(i, 0) % 10000 - 1;
//         int idx2 = hepevt.getJDAHEP(i, 1) % 10000 - 1;
//         int l = fillIndexVec(vec, idx1, idx2);
//         //System.out.println("child: "+i+" "+idx1+" "+idx2+" "+l);
//         for (int j = 0; j < l; j++)
//         {
//            checkAndAddDaughter(particles, ancestors, i, vec[j]);
//         }
//      }
//      
//      // Add particles to the collection.
//      for (int i=0; i<n; i++)
//      {
//         mcpcoll.add(particles[i]);
//      }
        return mcpcoll;
    }

//     private void checkAndAddDaughter(MCParticle[] particle, List ancestors, int parentID, int childID)
//   {
//      if (parentID == childID)
//         return; // Can't be parent of self
//      Set ancestor = (Set) ancestors.get(childID);
//      boolean added = ancestor.add(particle[parentID]);
//      if (added)
//         particle[parentID].addDaughter(particle[childID]);
//   }
}
