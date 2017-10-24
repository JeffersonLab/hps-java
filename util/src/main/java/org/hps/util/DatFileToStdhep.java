/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package org.hps.util;

/**
 * @author richp modified by mbussonn to take arguments from command line
 */
import java.io.FileReader;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.List;

import hep.io.stdhep.StdhepBeginRun;
import hep.io.stdhep.StdhepEndRun;
import hep.io.stdhep.StdhepEvent;
import hep.io.stdhep.StdhepWriter;
import java.util.Random;

public class DatFileToStdhep {

    static boolean expDecay = false;
    static boolean flatDecay = false;
    static boolean trident = false;
    // static double _declength = 0.0;//A' decay length (mm)
    static double _declength = 0.0;// A' decay length (mm)
    static double _xoff = 0.03;
    static double _yoff = 0;
    static double _zoff = 0;
    static double sigx = 0.0; // Luminous region size in x (mm)
    static double sigy = 0.01; // Luminous region size in y (mm)
    static double sigz = 0.01; // Luminous region size in z (mm)
    static double aMass = 0.05; // Aprime mass (GeV)
    static double maxLen = 200; // maximum decay length (mm)
    static double _ecm = 5.5; // GeV

    /**
     * @param args the command line arguments
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        // int nfiles = 6;
        // String[] files = {"ap5.5gev050mevsel.dat", "ap5.5gev100mevsel.dat", "ap5.5gev150mevsel.dat",
        // "ap5.5gev200mevsel.dat", "ap5.5gev250mevsel.dat", "ap5.5gev300mevsel.dat"};
        // double[] mass = {0.05, 0.1, 0.15, 0.2, 0.25, 0.3};

        // int nfiles = 10;
        // String[] files = {"ap5.5gev050mevall.dat", "ap5.5gev100mevall.dat", "ap5.5gev150mevall.dat",
        // "ap5.5gev200mevall.dat", "ap5.5gev250mevall.dat", "ap5.5gev300mevall.dat", "ap5.5gev400mevall.dat",
        // "ap5.5gev500mevall.dat", "ap5.5gev600mevall.dat", "ap5.5gev700mevall.dat"};
        // double[] mass = {0.05, 0.1, 0.15, 0.2, 0.25, 0.3, 0.4, 0.5, 0.6, 0.7};

        // int nfiles=10;
        // String fileLabel="ap5.5gev100mevsel";
        // double apMass=0.1;
        // _ecm=5.5;

        int nfiles = 40;
        String fileLabel = "ap2.2gev80mevsel";
        double apMass = 0.08;
        _ecm = 2.2;

        // int nfiles=7;
        // String fileLabel="ap3.3gev075mevsel";
        // double apMass=0.075;

        // int nfiles=1;
        // String fileLabel="ap5.5gev200mevall";
        // double apMass=0.2;
        // _ecm=5.5;

        for (int i = 0; i < nfiles; i++) {

            // String file = "/a/surrey10/vol/vol0/g.hps/mgraham/DarkPhoton/SignalEvents/" + files[i];
            // aMass = mass[i];
            int fnum = i + 1;
            String file = "/a/surrey10/vol/vol0/g.hps/mgraham/DarkPhoton/SignalEvents/" + fileLabel + "_" + fnum
                    + ".dat";
            // String file = "/a/surrey10/vol/vol0/g.hps/mgraham/DarkPhoton/SignalEvents/" +fileLabel+".dat";
            aMass = apMass;

            String infile = file;
            // String outfile = file.substring(0, file.length() - 4) + "_100u_beamspot.stdhep";//replace .txt by .stdhep
            // String outfile = file.substring(0, file.length() - 4) + "_100u_beamspot_5cm.stdhep";//replace .txt by
            // .stdhep
            // String outfile = file.substring(0, file.length() - 4) + "_10u_beamspot_0cm.stdhep";//replace .txt by
            // .stdhep
            // String outfile = file.substring(0, file.length() - 4) + "_10u_beamspot_FlatDecay.stdhep";//replace .txt
            // by .stdhep
            String outfile = file.substring(0, file.length() - 4) + "_10u_beamspot_gammactau_0cm.stdhep";// replace .txt
                                                                                                         // by .stdhep
            System.out.println("==== processing " + infile + " into " + outfile + "  aP mass = " + aMass + "====");
            process(infile, outfile);
        }
    }

    @SuppressWarnings("static-access")
    private static int lineCounter(StreamTokenizer tok) throws IOException {
        int lines = 0;
        while (tok.nextToken() != tok.TT_EOF) {
            if (tok.ttype == tok.TT_EOL)
                lines++;
            if (tok.ttype == tok.TT_WORD && tok.sval.startsWith("nev"))
                return lines;
        }
        // shouldn't get here...but maybe
        return lines;
    }

    private static void process(String infile, String outfile) throws IOException {
        Random generator = new Random();

        FileReader lc = new FileReader(infile);
        StreamTokenizer lctok = new StreamTokenizer(lc);
        lctok.resetSyntax();
        lctok.wordChars(33, 255);
        lctok.parseNumbers();

        lctok.whitespaceChars(0, ' ');
        lctok.eolIsSignificant(true);
        int nevts = lineCounter(lctok);
        lc.close();
        FileReader fr = new FileReader(infile);

        StreamTokenizer tok = new StreamTokenizer(fr);

        tok.resetSyntax();
        tok.wordChars(33, 255);
        tok.parseNumbers();

        tok.whitespaceChars(0, ' ');
        tok.eolIsSignificant(true);

        System.out.println("Found " + nevts + "  events");
        int nreq = (int) nevts;
        int ngen = (int) nevts;
        int nwrit = (int) nevts;
        float ecm = (float) _ecm;
        // float ecm = (float) 3.3;
        float xsec = (float) 99999997952.;
        double rn1 = 12345321;
        double rn2 = 66666666;
        StdhepWriter sw = new StdhepWriter(outfile, "Imported Stdhep Events", "From file " + infile, nwrit);
        sw.setCompatibilityMode(false);
        StdhepBeginRun sb = new StdhepBeginRun(nreq, ngen, nwrit, ecm, xsec, rn1, rn2);
        sw.writeRecord(sb);

        tok.resetSyntax();
        tok.wordChars(33, 255);
        tok.wordChars('0', '9'); // java.io.StreamTokenizer fails to parse
        tok.wordChars('e', 'e'); // scientific notation like "1.09E-008".
        tok.wordChars('E', 'E'); // The solution is to read and parse
        tok.wordChars('.', '.'); // coordinates as "words".
        tok.wordChars('+', '+'); // You run into trouble if the input file
        tok.wordChars('-', '-'); // contains text with "e" or "E" which is
        tok.whitespaceChars(0, ' ');
        tok.eolIsSignificant(true);

        double maxWght = 0;
        if (expDecay)
            maxWght = findMaxWeight();

        double[] beam = {0, 0, 0, 0};
        int nevhep = 0;
        for (int icross = 0; icross < nwrit; icross++) {
            beam[0] = sigx * generator.nextGaussian() + _xoff;
            beam[1] = sigy * generator.nextGaussian() + _yoff;
            beam[2] = sigz * generator.nextGaussian() + _zoff;

            // getToNextEvent(tok);
            List<Double> vals = getNumbersInLine(tok);
            // System.out.println(nums.toString());

            if (vals.size() != 16)
                throw new RuntimeException("Unexpected entry for event:  size = " + vals.size());

            nevhep++;
            // int nhep = nums.get(0).intValue();
            int nhep = 4; // two daughters of the A' and the recoil e-

            // System.out.println("Number of particles for event " + nevhep + ": " + nhep);

            int isthep[] = new int[nhep];
            int idhep[] = new int[nhep];
            int jmohep[] = new int[2 * nhep];
            int jdahep[] = new int[2 * nhep];
            double phep[] = new double[5 * nhep];
            double vhep[] = new double[4 * nhep];
            int i = 0;
            double tmpDecLen = 0;
            double[] ApMom = {0, 0, 0};
            if (expDecay)
                tmpDecLen = getDecayLength(maxWght);
            if (flatDecay)
                tmpDecLen = generator.nextDouble() * maxLen;
            for (int npart = 0; npart < nhep; npart++) {

                isthep[i] = 1;
                if (npart == 0)
                    isthep[i] = 2;
                // if (npart == 0) isthep[i] = 0;
                // if (npart == 0) isthep[i] = 1;
                idhep[i] = 622;
                if (npart == 1)
                    idhep[i] = -11;
                if (npart == 2)
                    idhep[i] = 11;
                if (npart == 3)
                    idhep[i] = 11;
                jmohep[2 * i] = 0;
                jmohep[2 * i + 1] = 0;
                jdahep[2 * i] = 2;
                jdahep[2 * i + 1] = 3;
                if (npart == 1 || npart == 2) {
                    jmohep[2 * i] = 1;
                    jmohep[2 * i + 1] = 1;
                    jdahep[2 * i] = 0;
                    jdahep[2 * i + 1] = 0;
                }
                if (npart == 3) {
                    jmohep[2 * i] = 0;
                    jmohep[2 * i + 1] = 0;
                    jdahep[2 * i] = 0;
                    jdahep[2 * i + 1] = 0;
                }
                for (int j = 0; j < 4; j++)
                    phep[5 * i + j] = vals.get(4 * i + j);
                phep[5 * i + 4] = 0.51109989000E-03;
                if (npart == 0 && !trident) {
                    phep[5 * i + 4] = aMass;
                    ApMom[0] = phep[5 * i + 2];
                    ApMom[1] = phep[5 * i + 1];
                    ApMom[2] = phep[5 * i + 0];
                }
                for (int j = 0; j < 4; j++)
                    vhep[4 * i + j] = beam[j];
                // decay the A' and daughters daughters at _declength
                if (!trident && (npart == 1 || npart == 2 || npart == 0))
                    if (!expDecay && !flatDecay)
                        vhep[4 * i + 0] = beam[0] + _declength;
                    else {
                        double totApMom = Math.sqrt(ApMom[0] * ApMom[0] + ApMom[1] * ApMom[1] + ApMom[2] * ApMom[2]);
                        // System.out.println("Decay at : " + tmpDecLen);
                        vhep[4 * i + 0] = beam[0] + tmpDecLen * ApMom[0] / totApMom;
                        vhep[4 * i + 1] = beam[1] + tmpDecLen * ApMom[1] / totApMom;
                        vhep[4 * i + 2] = beam[2] + tmpDecLen * ApMom[2] / totApMom;
                    }
                double px = phep[5 * i + 0];
                double pz = phep[5 * i + 2];
                phep[5 * i + 0] = pz;
                phep[5 * i + 2] = px;
                // if (i == 0 || i == nhep - 1) {
                /*
                 * System.out.println(i + " st: " + isthep[i] + " id: " + idhep[i] + " jmo: " + jmohep[2 * i] + " " +
                 * jmohep[2 * i + 1] + " jda: " + jdahep[2 * i] + " " + jdahep[2 * i + 1]); System.out.println("p: " +
                 * phep[5 * i] + " " + phep[5 * i + 1] + " " + phep[5 * i + 2] + " " + phep[5 * i + 3] + " " + phep[5 *
                 * i + 4]); System.out.println("v: " + vhep[4 * i] + " " + vhep[4 * i + 1] + " " + vhep[4 * i + 2] + " "
                 * + vhep[4 * i + 3]);
                 */
                // }
                i++;
                // }
            }
            StdhepEvent ev = new StdhepEvent(nevhep, nhep, isthep, idhep, jmohep, jdahep, phep, vhep);
            sw.writeRecord(ev);
        }

        int nreqe = (int) nevts;
        int ngene = (int) nevts;
        int nwrite = (int) nevts;
        float ecme = (float) 6.0;
        float xsece = (float) 99999997952.;
        double rn1e = 12345321;
        double rn2e = 66666666;
        StdhepEndRun se = new StdhepEndRun(nreqe, ngene, nwrite, ecme, xsece, rn1e, rn2e);
        sw.writeRecord(se);

        sw.close();
        fr.close();
        System.out.println("Ok...done!");
    }

    @SuppressWarnings("static-access")
    private static List<Double> getNumbersInLine(StreamTokenizer tok) throws IOException {
        List<Double> nums = new ArrayList<Double>();
        while (tok.nextToken() != tok.TT_EOF) {
            if (tok.ttype == tok.TT_EOL)
                break;
            String tokVal = tok.sval;
            // System.out.println(tokVal);
            nums.add(Double.valueOf(tokVal).doubleValue());
        }

        return nums;
    }

    @SuppressWarnings("static-access")
    private static double getNumberOfEvents(StreamTokenizer tok) throws IOException {
        boolean fndNumber = false;
        boolean fndOf = false;
        boolean fndEvents = false;
        double evts = -999;
        while (tok.nextToken() != tok.TT_EOF) {
            // System.out.println(tok.toString());
            if (tok.ttype == tok.TT_EOL)
                break;
            if (tok.ttype == tok.TT_WORD && tok.sval.contentEquals("Number"))
                fndNumber = true;
            if (tok.ttype == tok.TT_WORD && tok.sval.contentEquals("of"))
                fndOf = true;
            if (tok.ttype == tok.TT_WORD && tok.sval.contentEquals("Events"))
                fndEvents = true;
            if (tok.ttype == tok.TT_NUMBER && fndEvents && fndOf && fndNumber)
                evts = tok.nval;
        }
        return evts;
    }

    @SuppressWarnings("static-access")
    private static void getToNextEvent(StreamTokenizer tok) throws IOException {
        while (tok.nextToken() != tok.TT_EOF)
            // System.out.println(tok.toString());
            if (tok.ttype == tok.TT_WORD && tok.sval.contentEquals("<event>")) {
                tok.nextToken();// get to the EOL
                return;
            }
    }

    static private double expWeight(double x) {
        return Math.exp(-x / _declength);
    }

    static private double findMaxWeight() {
        Random generator = new Random();
        int ntrials = 100000;
        double maxlength = maxLen;
        double maxWeight = 0;
        for (int i = 0; i < ntrials; i++) {
            double x = generator.nextDouble() * maxlength;
            double wght = expWeight(x);
            if (wght > maxWeight)
                maxWeight = wght;
        }

        return maxWeight;
    }

    static private double getDecayLength(double MaxWeight) {
        Random generator = new Random();
        double maxlength = maxLen;
        double dl = 0;
        double draw = generator.nextDouble();
        double tmpwght = 0;
        while (tmpwght < draw) {
            dl = generator.nextDouble() * maxlength;
            tmpwght = expWeight(dl) / MaxWeight;
        }
        return dl;
    }
}
