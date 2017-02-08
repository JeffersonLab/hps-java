package org.hps.util;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.lcsim.detector.IRotation3D;
import org.lcsim.detector.RotationGeant;

public class DumpLHEEventsToASCII {

    static FileWriter fw;
    static PrintWriter pw;
    static boolean expDecay = false;  //set a ctau decay length
    static boolean flatDecay = false; //decay uniformily in some range
    static boolean trident = false;  //are these trident events or A' signal events
    static double _declength = 0.0;   //A' decay length  (mm)
    static double _xoff = 0.0;   //set the x,y,z origin offsets...
    static double _yoff = 0.0;
    static double _zoff = 0.03;
    static double aMass = 99;  //Aprime mass (MeV)
    static double sigx = 0.2;        // Luminous region size in x (mm)
    static double sigy = 0.02;        // Luminous region size in y (mm)
    static double sigz = 0.0;         // Luminous region size in z (mm)
//beam is positioned so that at first beam direction is in z, then rotated to correct orientation    
    static double rotx = 0.00;        // Rotation of beam about X
    static double roty = 0.03;        // Rotation of beam about Y
    static double rotz = 0.00;         // Rotation of beam about Z
    static double maxLen = 200; // maximum decay length (mm)
    static double _ecm = 6.6; //GeV
    static int nInFiles = 10000;  //number of input files
    static int nBegin = 0;
    static int nmax = 500000;  //maximum number of events to write to 1 stdhep file (new one opens if n>nmax)
    static String fileType = "lhe";
    static IRotation3D rot = new RotationGeant(rotx, roty, rotz);
//        static String fileType="dat";
    static int nread = -1;  // a running total of number of events read/written to stdhep files
    static boolean _eventFilter = false;
    static boolean _isMuon = false;
//    static int _nEleRequired = 2;
    static int _nEleRequired = 0;

    private static Options createCommandLineOptions() {
        Options options = new Options();

        options.addOption(new Option("m", true, "A' Mass (MeV)"));
        options.addOption(new Option("e", true, "Beam Energy (GeV)"));
        options.addOption(new Option("n", true, "Number of files to run."));
        options.addOption(new Option("b", true, "First file number."));
        options.addOption(new Option("x", true, "Beam sigma in x"));
        options.addOption(new Option("y", true, "Beam sigma in y"));
        options.addOption(new Option("s", false, "Filter Events"));
        options.addOption(new Option("u", false, "Is muonic decay?"));
        options.addOption(new Option("t", false, "Is Trident?"));

        return options;
    }

    /**
     @param args the command line arguments
     @throws IOException
     */
    public static void main(String[] args) throws IOException {


        // Set up command line parsing.
        Options options = createCommandLineOptions();

        CommandLineParser parser = new PosixParser();

        // Parse command line arguments.
        CommandLine cl = null;
        try {
            cl = parser.parse(options, args);
            System.out.println("Trying parser");
        } catch (ParseException e) {
            throw new RuntimeException("Problem parsing command line options.", e);
        }
        String massString = String.valueOf(aMass);
        String ninString = String.valueOf(nInFiles);
        String nbegString = String.valueOf(nBegin);
        String ecmString = String.valueOf(_ecm);
        String eptString = String.valueOf(_ecm);
        String sigxString = String.valueOf(sigx);
        String sigyString = String.valueOf(sigy);
        eptString = convertDecimal(eptString);
        if (cl.hasOption("t")) {
        trident=true;
        }
        if (cl.hasOption("m")) {
            massString = cl.getOptionValue("m");
            if (!trident)
                aMass = Integer.valueOf(massString);
            System.out.println(massString);
        }

        if (cl.hasOption("n")) {
            ninString = cl.getOptionValue("n");
            nInFiles = Integer.valueOf(ninString);
            System.out.println(ninString);
        }
        if (cl.hasOption("b")) {
            nbegString = cl.getOptionValue("b");
            nBegin = Integer.valueOf(nbegString);
            System.out.println(nbegString);
        }

        if (cl.hasOption("x")) {
            sigxString = cl.getOptionValue("x");
            sigx = Double.valueOf(sigxString);
            System.out.println(sigxString);
        }
        if (cl.hasOption("y")) {
            sigyString = cl.getOptionValue("y");
            sigy = Double.valueOf(sigyString);
            System.out.println(sigyString);
        }

        if (cl.hasOption("e")) {
            ecmString = cl.getOptionValue("e");
            _ecm = Double.valueOf(ninString);
            System.out.println(ecmString);
            eptString = convertDecimal(ecmString);
        }

        String filter = "all";
        if (cl.hasOption("s")) {
            _eventFilter = true;
            filter = "selected";
        }
        if (cl.hasOption("u")) {
            _isMuon = true;
        }
        sigxString = convertMicron(sigx);
        sigyString = convertMicron(sigy);

//        String postfix = "_20ux200u_beamspot_gammactau_0cm.stdhep";
//        String postfix = "_"+sigxString+"x"+sigyString+"_beamspot_gammactau_0cm.stdhep";
        String postfix = ".dat";
//        String fDir="/nfs/slac/g/hps/mgraham/DarkPhoton/tvm/testrun/";
//        String fileLabel = "ap2.2gev40mevsel";

        //            String fDir="/nfs/slac/g/hps/mgraham/DarkPhoton/tvm/trident/full/";
        //      String fileLabel = "full6.6gev";

        //String fDir = "/nfs/slac/g/hps/mgraham/DarkPhoton/MadGraph/aMassEvents2pt2Ap100MeV/";
        //String fileLabel = "ap2.2gev100mevall";
        //String inLabel = "W2pt2GeV_Ap100MeV_";

        String fDir = "/nfs/slac/g/hps/mgraham/DarkPhoton/MadGraph/Events" + eptString + "Ap" + massString + "MeV/";
        String fileLabel = "ap" + ecmString + "gev" + massString + "mev" + filter;
        String inLabel = "W" + eptString + "GeV_Ap" + massString + "MeV_";
        if (trident) {
            fDir = "/nfs/slac/g/hps/mgraham/DarkPhoton/MadGraph/Events" + eptString + massString + "/";
            fileLabel = "ap" + ecmString + massString + filter;
            inLabel = "W" + eptString + "GeV_" + massString + "_";
        }
        if (_isMuon) {
            fDir = "/nfs/slac/g/hps/mgraham/DarkPhoton/MadGraph/Events" + eptString + "Ap" + massString + "MeVMuon/";
            fileLabel = "ap" + ecmString + "gev" + massString + "mevMuon" + filter;
            inLabel = "W" + eptString + "GeV_Ap" + massString + "MeVMuon_";
        }
        String inPost = "_unweighted_events.lhe";


        String outDir = "/nfs/slac/g/hps/mgraham/DarkPhoton/GeneratedEvents/";

        int nOutFile = nBegin;
        System.out.println(fDir + fileLabel);
        for (int i = nBegin; i < nBegin + nInFiles; i++) {
            int fnum = i + 1;
            String snum = "_" + fnum;
            if (fnum < 10000) {
                snum = "_0" + fnum;
            }
            if (fnum < 1000) {
                snum = "_00" + fnum;
            }
            if (fnum < 100) {
                snum = "_000" + fnum;
            }
            if (fnum < 10) {
                snum = "_0000" + fnum;
            }

            String infile = "";
            if (fileType.contains("dat")) {
                infile = fDir + fileLabel + snum + ".dat";
//                System.out.println(infile);
            } else if (fileType.contains("lhe")) {
                infile = fDir + inLabel + i + inPost;
                System.out.println("Unzipping " + infile);
                String cmd = "gunzip " + infile + ".gz";
                Process p = Runtime.getRuntime().exec(cmd);
                try {
                    p.waitFor();
                } catch (InterruptedException ex) {
                    Logger.getLogger(DumpLHEEventsToASCII.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            File f = new File(infile);
            if (nread == -1 && f.exists()) {
                nOutFile++;
                String outfile = outDir + fileLabel + "_" + nOutFile + postfix;//replace .txt by .stdhep
                System.out.println("==== processing " + infile + " into " + outfile + "  aP mass = " + aMass + "====");
                openASCIIFile(outfile);
            }
            if (f.exists()) {
                nread += process(infile);
            }
            if (fileType.contains("lhe")) {
                String cmd = "gzip " + infile;
                Process p = Runtime.getRuntime().exec(cmd);
                try {
                    p.waitFor();
                } catch (InterruptedException ex) {
                    Logger.getLogger(DumpLHEEventsToASCII.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (nread > nmax) {
                nread = -1;
                closeASCIIFile();
            }
        }
        closeASCIIFile();
    }

    @SuppressWarnings("static-access")
    private static int lineCounter(StreamTokenizer tok) throws IOException {
        int lines = 0;
        while (tok.nextToken() != tok.TT_EOF) {
            if (tok.ttype == tok.TT_EOL) {
                lines++;
            }
            if (tok.ttype == tok.TT_WORD && tok.sval.startsWith("nev")) {
                return lines;
            }
        }
        //shouldn't get here...but maybe
        return lines;
    }

    private static int getnevts(StreamTokenizer lctok) throws IOException {
        int nevts = -1;
        if (fileType.contains("dat")) {
            return lineCounter(lctok);
        } else if (fileType.contains("lhe")) {
            while (nevts == -1) {
                nevts = getNumberOfEvents(lctok);
            }
            return nevts;
        }
        return nevts;
    }

    private static int process(String infile) throws IOException {
        Random generator = new Random();

        FileReader lc = new FileReader(infile);
        StreamTokenizer lctok = new StreamTokenizer(lc);
        lctok.resetSyntax();
        lctok.wordChars(33, 255);
        lctok.parseNumbers();

        lctok.whitespaceChars(0, ' ');
        lctok.eolIsSignificant(true);
        int nevts = getnevts(lctok);
        lc.close();
        System.out.println("Found " + nevts + " events");

        FileReader fr = new FileReader(infile);

        StreamTokenizer tok = new StreamTokenizer(fr);

        tok.resetSyntax();
        tok.wordChars(33, 255);
        tok.parseNumbers();

        tok.whitespaceChars(0, ' ');
        tok.eolIsSignificant(true);

//        System.out.println("Found " + nevts + "  events");
        int nreq = (int) nevts;
        int ngen = (int) nevts;
        int nwrit = (int) nevts;
        float ecm = (float) _ecm;
        float xsec = (float) 99999997952.;
        double rn1 = 12345321;
        double rn2 = 66666666;
//        StdhepBeginRun sb = new StdhepBeginRun(nreq, ngen, nwrit, ecm, xsec, rn1, rn2);
//        sw.writeRecord(sb);


        tok.resetSyntax();
        tok.wordChars(33, 255);
        tok.wordChars('0', '9');        // java.io.StreamTokenizer fails to parse
        tok.wordChars('e', 'e');        // scientific notation like "1.09E-008".
        tok.wordChars('E', 'E');        // The solution is to read and parse
        tok.wordChars('.', '.');        // coordinates as "words".
        tok.wordChars('+', '+');        // You run into trouble if the input file
        tok.wordChars('-', '-');        // contains text with "e" or "E" which is
        tok.whitespaceChars(0, ' ');
        tok.eolIsSignificant(true);



        double[] beam = {0, 0, 0, 0};
        int nevhep = 0;
        for (int icross = 0; icross < nwrit; icross++) {
            Hep3Vector beamVecOrig =
                    new BasicHep3Vector(sigx * generator.nextGaussian() + _xoff,
                    sigy * generator.nextGaussian() + _yoff,
                    sigz * generator.nextGaussian() + _zoff);
            Hep3Vector beamVec = rot.rotated(beamVecOrig);
            beam[0] = beamVec.x();
            beam[1] = beamVec.y();
            beam[2] = beamVec.z();

            double tmpDecLen = 0;

            if (fileType.contains("lhe")) {
                writeLHEEvent(tok, beam, icross);
            }

        }
        fr.close();


        return nwrit;


    }

    private static void openASCIIFile(String outfile) throws IOException {
        try {
            fw = new FileWriter(outfile);
            pw = new PrintWriter(fw);
        } catch (IOException ex) {
            Logger.getLogger(DumpLHEEventsToASCII.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.out.println("Ok...opened " + outfile);
    }

    private static void closeASCIIFile() throws IOException {
        pw.close();
        try {
            fw.close();
        } catch (IOException ex) {
            Logger.getLogger(DumpLHEEventsToASCII.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Ok...done with closing!");
    }

    @SuppressWarnings("static-access")
    private static List<Double> getNumbersInLine(StreamTokenizer tok) throws IOException {
        List<Double> nums = new ArrayList<Double>();
        while (tok.nextToken() != tok.TT_EOF) {
            if (tok.ttype == tok.TT_EOL) {
                break;
            }
            String tokVal = tok.sval;
//            System.out.println(tokVal);
            nums.add(Double.valueOf(tokVal).doubleValue());
        }

        return nums;
    }

    @SuppressWarnings("static-access")
    private static int getNumberOfEvents(StreamTokenizer tok) throws IOException {
        boolean fndNumber = false;
        boolean fndOf = false;
        boolean fndEvents = false;
        int evts = -1;
        while (tok.nextToken() != tok.TT_EOF) {
//            System.out.println(tok.toString());
            if (tok.ttype == tok.TT_EOL) {
                break;
            }
            if (tok.ttype == tok.TT_WORD && tok.sval.contentEquals("Number")) {
                //              System.out.println(tok.toString());
                fndNumber = true;
            }
            if (tok.ttype == tok.TT_WORD && tok.sval.contentEquals("of")) {
                fndOf = true;
            }
            if (tok.ttype == tok.TT_WORD && tok.sval.contentEquals("Events")) {
                fndEvents = true;
            }
            if (tok.ttype == tok.TT_NUMBER && fndEvents && fndOf && fndNumber) {
                evts = (int) tok.nval;
            }
        }
        return evts;
    }

    @SuppressWarnings("static-access")
    private static void getToNextEvent(StreamTokenizer tok) throws IOException {
        while (tok.nextToken() != tok.TT_EOF) //            System.out.println(tok.toString());
        {
            if (tok.ttype == tok.TT_WORD && tok.sval.contentEquals("<event>")) {
                tok.nextToken();//get to the EOL
                return;
            }
        }
    }

    static private double expWeight(double x) {
        return Math.exp(-x / _declength);
    }

    static private double expWeight(double x, double gamma) {
        return Math.exp(-x / (gamma * _declength));
    }

    /*
     Old code written by Matt static private double findMaxWeight() { Random
     generator = new Random(); int ntrials = 100000; double maxlength = maxLen;
     double maxWeight = 0; for (int i = 0; i < ntrials; i++) { double x =
     generator.nextDouble() * maxlength; double wght = expWeight(x); if (wght >
     maxWeight) { maxWeight = wght; } }

     return maxWeight; }

     static private double getDecayLength(double MaxWeight, double gamma) {
     Random generator = new Random(); double maxlength = maxLen; double dl = 0;
     double draw = generator.nextDouble(); double tmpwght = 0; while (tmpwght <
     draw) { dl = generator.nextDouble() * maxlength; tmpwght = expWeight(dl,
     gamma) / MaxWeight; } return dl; }
     */
    static private double getDecayLength(double gamma) {
        Random generator = new Random();
        double a = generator.nextDouble();
        double l = -gamma * _declength * Math.log(1 - a);
        return l;
    }

    /*
     Old code written by Matt static private double getDecayLength(double
     MaxWeight) { Random generator = new Random(); double maxlength = maxLen;
     double dl = 0; double draw = generator.nextDouble(); double tmpwght = 0;
     while (tmpwght < draw) { dl = generator.nextDouble() * maxlength; tmpwght =
     expWeight(dl) / MaxWeight; } return dl; }
     */
    static private void writeLHEEvent(StreamTokenizer tok, double[] beam, int nevhep) throws IOException {
        Random generator = new Random();
        getToNextEvent(tok);
        List<Double> nums = getNumbersInLine(tok);

        if (nums.size() != 6) {
            throw new RuntimeException("Unexpected entry for number of particles");
        }
        int nhep = nums.get(0).intValue();
    //        System.out.println("Number of particles for event " + nevhep + ": " + nhep);

        double decLen = 0;
        double maxWght = 0;

        /*
         No longer needed

         if (expDecay) { maxWght = findMaxWeight(); }
         */

        double phepEle[] = new double[5];
        double phepPos[] = new double[5];
        double phepRec[] = new double[5];
        double phepNuc[] = new double[5];


        int idhepTmp = 0;
        double[] ApMom = {0, 0, 0};
        double ApMass = 0;
        double ApEnergy = 0;
        boolean acceptEvent = false;
        int nElePass = 0;
        for (int npart = 0; npart < nhep; npart++) {
            List<Double> vals = getNumbersInLine(tok);
            if (vals.size() != 13) {
                throw new RuntimeException("Unexpected entry for a particle");
            }
        idhepTmp = vals.get(0).intValue();
//      System.out.println(idhepTmp);
            if (vals.get(1).intValue() == 9) {//apparently, vertices aren't counted in nhep
        nhep++;
        }

            if (vals.get(1).intValue() == 1) {//ignore initial  & intermediate state particles
        //      System.out.println("Ok...good"+idhepTmp);



                for (int j = 0; j < 5; j++) {
                    if (idhepTmp == 611)
                        phepEle[j] = vals.get(j + 6);                    
                    if (idhepTmp == -611)
                        phepPos[j] = vals.get(j + 6);
                    if (idhepTmp == 11)
                        phepRec[j] = vals.get(j + 6);
                    if (idhepTmp == -623){
                        phepNuc[j] = vals.get(j + 6);
            //          System.out.println("Found the recoil nucleus");
            }
                }





            }



        }
        //StdhepEvent ev = new StdhepEvent(nevhep, nhep, isthep, idhep, jmohep, jdahep, phep, vhep);

        pw.format("%d ", nevhep);
        pw.format("%5.5f %5.5f %5.5f %5.5f %5.5f ", phepEle[0], phepEle[1], phepEle[2], phepEle[3], phepEle[4]);
        pw.format("%5.5f %5.5f %5.5f %5.5f %5.5f ", phepPos[0], phepPos[1], phepPos[2], phepPos[3], phepPos[4]);
        pw.format("%5.5f %5.5f %5.5f %5.5f %5.5f ", phepRec[0], phepRec[1], phepRec[2], phepRec[3], phepRec[4]);
        pw.format("%5.5f %5.5f %5.5f %5.5f %5.5f ", phepNuc[0], phepNuc[1], phepNuc[2], phepNuc[3], phepNuc[4]);

        pw.println();

    }

    public static Hep3Vector rotateToDetector(double x, double y, double z) {
        Hep3Vector vecOrig =
                new BasicHep3Vector(x, y, z);
        return rot.rotated(vecOrig);
    }

    public static String convertDecimal(String num) {
        if (num.contains(".")) {
            num = num.replace(".", "pt");
        }
        return num;
    }

    public static String convertMicron(double num) {
        double mic = num * 1000.0;
        String out = Integer.toString((int) mic);
        return out + "u";
    }

    public static boolean inAcceptance(Hep3Vector ph) {
        boolean ok = false;
        double[] p = {ph.x(), ph.y(), ph.z()};
        double ptot = Math.sqrt(p[0] * p[0] + p[1] * p[1] + p[2] * p[2]);

        double sinThx = p[0] / Math.sqrt(p[0] * p[0] + p[2] * p[2]);
        double sinThy = p[1] / Math.sqrt(p[1] * p[1] + p[2] * p[2]);
        // for now, just use thetay (non-bend direction)
//         System.out.println("px = "+p[0]+"; py = "+p[1]+"; pz = "+p[2]);
//        System.out.println(sinThy+" "+sinThx+" "+ptot);

        if (Math.abs(sinThy) > 0.012 && ptot > 0.1) {
            ok = true;
        }
        return ok;
    }
}
