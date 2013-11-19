/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lcsim.hps.util;

/**
 *
 * @author mgraham
 */
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.BasicHepLorentzVector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.HepLorentzVector;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.lcsim.fit.helicaltrack.HelixParamCalculator;

public class CalculateAcceptanceFromMadGraph {

    static boolean expDecay = false;  //set a ctau decay length
    static boolean flatDecay = false; //decay uniformily in some range
    static boolean trident = false;  //are these trident events or A' signal events
    static double _declength = 0.0;   //A' decay length  (mm)
    static double _xoff = 0.0;   //set the x,y,z origin offsets...
    static double _yoff = 0.0;
    static double _zoff = 0.03;
    static double aMass = 99;  //Aprime mass (MeV)
    static double sigx = 0.00001;        // Luminous region size in x (mm)
    static double sigy = 0.02;        // Luminous region size in y (mm)
    static double sigz = 0.02;         // Luminous region size in z (mm)
    static double maxLen = 200; // maximum decay length (mm)
    static int nInFiles = 1000;  //number of input files
    static int nmax = 500000;  //maximum number of events to write to 1 stdhep file (new one opens if n>nmax)
    static String fileType = "lhe";
    static int nread = -1;  // a running total of number of events read/written to stdhep files
    static boolean _eventFilter = false;
    static boolean _isMuon = false;
    static int _nEleRequired = 2;
    static double sensorWidth = 40;
    static double sensorLength = 100;
    static double gap = Math.sin(0.015);
    static double gapBig = Math.sin(0.030);
    static int nLayers = 6;
    static double[] x = {100, 200, 300, 500, 700, 900};
    static double[] ySize = { sensorLength / 2, sensorLength / 2, sensorLength / 2, sensorLength / 2, sensorLength, sensorLength};
    static double[] zSize = { sensorWidth, sensorWidth, sensorWidth, sensorWidth, sensorWidth, sensorWidth};
    static double[] zGap = {x[0] * gap, x[1] * gap, x[2] * gap, x[3] * gap, x[4] * gap, x[5] * gap};
    
//    static double[] ySizeFull = {sensorWidth, sensorWidth, sensorWidth, 3 * sensorWidth / 2, 2 * sensorWidth, 7 * sensorWidth / 2, 4 * sensorLength,};
//    static double[] zSizeFull = {sensorWidth, sensorWidth, 2 * sensorWidth, 2 * sensorWidth, sensorLength, sensorLength, sensorLength};
//    static double[] zGapFull = {x[0] * gap, x[1] * gap, x[2] * gap, x[3] * gap, x[4] * gap, x[5] * gap, x[6] * gap};
    static int nGenerated = 0;
    static int nPassNoMax = 0;
    
    static int nPassFull = 0;
    static int nPassLyr50 = 0;
    /*
     * static double maxMass = 1000.0;
     * static double bField = 1.0;
     * static double _pCut = 0.25;
     * static double _ecm = 6.6; //GeV
     */
    static String _custom = "";
    static double maxMass = 1000.0;
    static double bField = 1.0;
    static double _pCut = 0.05;
    static double _ecm = 4.4; //GeV
    static double binSize = 1.0;
    static int nbins = (int) (maxMass / binSize);
    static int[] genMass = new int[nbins];
    //static int[] recoMassLyr1 = new int[nbins];
    static int[] recoMassLyr1Full = new int[nbins];
    static int[] recoMassLyr1NoMax = new int[nbins];

    static int[] recoMassLyr50Full = new int[nbins];

    public static void doAccounting(List<Boolean> passEle50, List<Boolean> passPos50, List<Boolean> passEleNoMax, List<Boolean> passPosNoMax, List<Boolean> passEleFull, List<Boolean> passPosFull, boolean passEvt, double invMass) {

        boolean passLyr700Full = false;
        boolean passLyr700Full50 = false;
        boolean passLyr100Full = false;
        boolean passLyr50Full = false;
        boolean passLyr700NoMax = false;
        boolean passLyr100NoMax = false;
       
        //find the bin...
        int bin = (int) (invMass / maxMass * nbins);
//        System.out.println("invMass = "+invMass + "...goes in bin #"+bin);
        if (bin < nbins) {
            genMass[bin]++;
            nGenerated++;

            if (passEleFull.get(4) && passPosFull.get(4))
                passLyr700Full = true;
            if (passEleNoMax.get(4) && passPosNoMax.get(4))
                passLyr700NoMax = true;
            if (passEle50.get(4) && passPos50.get(4))
                passLyr700Full50 = true;
           

            //see if they were in layer 1
            if (passEleFull.get(0) && passPosFull.get(0))
                passLyr100Full = true;

            if (passEleNoMax.get(0) && passPosNoMax.get(0))
                passLyr100NoMax = true;

            if (passEle50.get(0) && passPos50.get(0))
                passLyr50Full = true;

            //ok...fill the histograms
            if (passLyr700Full && passLyr100Full && passEvt) {
                recoMassLyr1Full[bin]++;
                nPassFull++;
            }

            if (passLyr700Full50 && passLyr50Full && passEvt) {
                recoMassLyr50Full[bin]++;
                nPassLyr50++;
            }

            if (passLyr700NoMax && passLyr100NoMax && passEvt) {
                recoMassLyr1NoMax[bin]++;
                nPassNoMax++;
            }

        



        } else {
//            System.out.println("Mass out of range!  "+invMass);
        }

    }

    private static Options createCommandLineOptions() {
        Options options = new Options();

        options.addOption(new Option("e", true, "Beam Energy (GeV)"));
        options.addOption(new Option("n", true, "Number of files to run."));
        options.addOption(new Option("b", true, "B-Field"));
        options.addOption(new Option("t", true, "Rad, BH, or FullRadBH"));
        options.addOption(new Option("u", false, "Is muon decay?"));
        options.addOption(new Option("c", true, "Custom String"));
        return options;
    }

    /**
     * @param args the command line arguments
     * @throws IOException
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

        String ninString = String.valueOf(nInFiles);
        String ecmString = String.valueOf(_ecm);
        String bString = "0.5";
        String eptString = String.valueOf(_ecm);
        String typeString = "Rad";
        eptString = convertDecimal(eptString);


        if (cl.hasOption("n")) {
            ninString = cl.getOptionValue("n");
            nInFiles = Integer.valueOf(ninString);
            System.out.println(ninString);
        }



        if (cl.hasOption("e")) {
            ecmString = cl.getOptionValue("e");
            _ecm = Double.valueOf(ecmString);
            System.out.println(ecmString);
            eptString = convertDecimal(ecmString);
        }
        if (cl.hasOption("b")) {
            bString = cl.getOptionValue("b");
            bField = Double.valueOf(bString);
            System.out.println(bString);
        }

        if (cl.hasOption("t")) {
            typeString = cl.getOptionValue("t");
            System.out.println(typeString);
        }

        if (cl.hasOption("u"))
            _isMuon = true;
        boolean _hasCustomString = false;
        if (cl.hasOption("c")) {
            _custom = cl.getOptionValue("c");
            _hasCustomString = true;
            System.out.println("Using custom string = " + _custom);
        }
        
        
        String fDir = "/nfs/slac/g/hps/mgraham/DarkPhoton/MadGraph/Events" + eptString + typeString + "/";
//        String fDir = "./";
//        String inLabel = "W" + eptString + "GeV_Ap" + massString + "MeV_";
        String inLabel = "W" + eptString + "GeV_" + typeString + "_";

        String inPost = "_unweighted_events.lhe";


//        String outDir = "/nfs/slac/g/hps/mgraham/DarkPhoton/SignalEvents/";
        String outDir = "./Acceptance/";
        for (int i = 0; i < nInFiles; i++) {
            int fnum = i + 1;
            String snum = "_" + fnum;
            if (fnum < 10000)
                snum = "_0" + fnum;
            if (fnum < 1000)
                snum = "_00" + fnum;
            if (fnum < 100)
                snum = "_000" + fnum;
            if (fnum < 10)
                snum = "_0000" + fnum;

            String infile = "";
            if (fileType.contains("dat")) {
//                infile = fDir + fileLabel + snum + ".dat";
//                System.out.println(infile);
            } else if (fileType.contains("lhe")) {
                infile = fDir + inLabel + i + inPost;
                System.out.println("Unzipping " + infile);
                String cmd = "gunzip " + infile + ".gz";
                Process p = Runtime.getRuntime().exec(cmd);
                try {
                    p.waitFor();
                } catch (InterruptedException ex) {
                    Logger.getLogger(CalculateAcceptanceFromMadGraph.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            File f = new File(infile);
            if (nread == -1 && f.exists())
                System.out.println("==== processing " + infile + " ====");
            if (f.exists())
                nread += process(infile);
            if (fileType.contains("lhe")) {
                String cmd = "gzip " + infile;
                Process p = Runtime.getRuntime().exec(cmd);
                try {
                    p.waitFor();
                } catch (InterruptedException ex) {
                    Logger.getLogger(CalculateAcceptanceFromMadGraph.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        }
        //ok ... now spit out histograms
        System.out.println("nGenerated = " + nGenerated);
//        System.out.println("nPass(Test) = " + nPassTest);

        System.out.println("nPass(Full) = " + nPassFull);
        System.out.println("nPass(Lyr50)= " + nPassLyr50);
        System.out.println("nPass(NoMax) = " + nPassNoMax);
        System.out.println("*********    Histograms      ***********   ");
        System.out.println("bin     mass   Gen    Lyr1     Target50   NoMax ");
        for (int kk = 0; kk < nbins; kk++) {
            double mass = ((double) kk / nbins) * maxMass;
//            System.out.println(kk+"\t"+mass+"\t"+genMass[kk]+"\t"+recoMassLyr1[kk]+"\t"+recoMassLyr50[kk]);
//            System.out.printf("%d\t%4.1f\t%d\t%d\t%d\t%d\n", kk, mass, genMass[kk], recoMassLyr1[kk], recoMassLyr50[kk], recoMassLyr1Full[kk]);
            System.out.printf("%d\t%4.1f\t%d\t%d\t%d\t%d\n", kk, mass, genMass[kk], recoMassLyr1Full[kk], recoMassLyr50Full[kk], recoMassLyr1NoMax[kk]);
        }
        /*
         * outputFile(outDir + typeString + eptString + "_Test_1T.dat",
         * recoMassLyr1);
         * outputFile(outDir + typeString + eptString + "_Full_1T.dat",
         * recoMassLyr1Full);
         * outputFile(outDir + typeString + eptString + "_lyr50_1T.dat",
         * recoMassLyr50);
         * outputFile(outDir + typeString + eptString + "_generated_1T.dat",
         * genMass);
         */

//        outputFile(outDir + typeString + eptString + "_Test.dat", recoMassLyr1);
        outputFile(outDir + typeString + eptString + "_Full.dat", recoMassLyr1Full);
        outputFile(outDir + typeString + eptString + "_NoMax.dat", recoMassLyr1NoMax);     
        outputFile(outDir + typeString + eptString + "_lyr50.dat", recoMassLyr50Full);
        outputFile(outDir + typeString + eptString + "_generated.dat", genMass);


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
        //shouldn't get here...but maybe
        return lines;
    }

    private static void outputFile(String fName, int[] hist) throws IOException {
        FileOutputStream fos = new FileOutputStream(fName);
        PrintWriter osw = new PrintWriter(fos);
        for (int kk = 0; kk < nbins; kk++) //            double mass = ((double)kk/nbins)*maxMass;
        //            System.out.println(kk+"\t"+mass+"\t"+genMass[kk]+"\t"+recoMassLyr1[kk]+"\t"+recoMassLyr50[kk]);
        //            System.out.printf("%d\t%4.1f\t%d\t%d\t%d\n",kk,mass,genMass[kk],recoMassLyr1[kk],recoMassLyr50[kk]);
        
            osw.println(hist[kk]);
        osw.close();
        fos.close();

    }

    private static int getnevts(StreamTokenizer lctok) throws IOException {
        int nevts = -1;
        if (fileType.contains("dat"))
            return lineCounter(lctok);
        else if (fileType.contains("lhe")) {
            while (nevts == -1)
                nevts = getNumberOfEvents(lctok);
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
            Hep3Vector beamVec =
                    new BasicHep3Vector(sigx * generator.nextGaussian() + _xoff,
                    sigy * generator.nextGaussian() + _yoff,
                    sigz * generator.nextGaussian() + _zoff);

            beam[0] = beamVec.x();
            beam[1] = beamVec.y();
            beam[2] = beamVec.z();

            double tmpDecLen = 0;


            readLHEEvent(tok, beam, icross);


        }
        fr.close();


        return nwrit;


    }

    @SuppressWarnings("static-access")
    private static List<Double> getNumbersInLine(StreamTokenizer tok) throws IOException {
        List<Double> nums = new ArrayList<Double>();
        while (tok.nextToken() != tok.TT_EOF) {
            if (tok.ttype == tok.TT_EOL)
                break;
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
            if (tok.ttype == tok.TT_EOL)
                break;
            if (tok.ttype == tok.TT_WORD && tok.sval.contentEquals("Number")) //              System.out.println(tok.toString());
            
                fndNumber = true;
            if (tok.ttype == tok.TT_WORD && tok.sval.contentEquals("of"))
                fndOf = true;
            if (tok.ttype == tok.TT_WORD && tok.sval.contentEquals("Events"))
                fndEvents = true;
            if (tok.ttype == tok.TT_NUMBER && fndEvents && fndOf && fndNumber)
                evts = (int) tok.nval;
        }
        return evts;
    }

    @SuppressWarnings("static-access")
    private static void getToNextEvent(StreamTokenizer tok) throws IOException {
        while (tok.nextToken() != tok.TT_EOF) //            System.out.println(tok.toString());
        
            if (tok.ttype == tok.TT_WORD && tok.sval.contentEquals("<event>")) {
                tok.nextToken();//get to the EOL
                return;
            }
    }

    static private double expWeight(double x) {
        return Math.exp(-x / _declength);
    }

    static private double expWeight(double x, double gamma) {
        return Math.exp(-x / (gamma * _declength));
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

    static private double getDecayLength(double MaxWeight, double gamma) {
        Random generator = new Random();
        double maxlength = maxLen;
        double dl = 0;
        double draw = generator.nextDouble();
        double tmpwght = 0;
        while (tmpwght < draw) {
            dl = generator.nextDouble() * maxlength;
            tmpwght = expWeight(dl, gamma) / MaxWeight;
        }
        return dl;
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

    static private void readLHEEvent(StreamTokenizer tok, double[] beam, int nevhep) throws IOException {
        Random generator = new Random();
        getToNextEvent(tok);
        List<Double> nums = getNumbersInLine(tok);

        if (nums.size() != 6)
            throw new RuntimeException("Unexpected entry for number of particles");
        int nhep = nums.get(0).intValue();
//        System.out.println("Number of particles for event " + nevhep + ": " + nhep);

        List<Boolean> passEleTarget50 = new ArrayList<Boolean>();
        List<Boolean> passPosTarget50 = new ArrayList<Boolean>();
        List<Boolean> passEleNoMax = new ArrayList<Boolean>();
        List<Boolean> passPosNoMax = new ArrayList<Boolean>();
        List<Boolean> passEleFull = new ArrayList<Boolean>();
        List<Boolean> passPosFull = new ArrayList<Boolean>();
        List<Boolean> passRecoilTarget50 = new ArrayList<Boolean>();
        List<Boolean> passRecoilNoMax = new ArrayList<Boolean>();
        List<Boolean> passRecoilFull = new ArrayList<Boolean>();
        //Hep3Vector pEle = new BasicHep3Vector();
        //Hep3Vector pPos = new BasicHep3Vector();
        //Hep3Vector pRecoil = new BasicHep3Vector();
        HepLorentzVector pEle = new BasicHepLorentzVector();
        HepLorentzVector pPos = new BasicHepLorentzVector();
        HepLorentzVector pRecoil = new BasicHepLorentzVector();
        int i = 0;
        int pdgid = 0;
        double[] ApMom = {0, 0, 0};
        double mom[] = {0, 0, 0};
        double ene = 0;
        int charge;
        boolean foundRecoil = false;
        for (int npart = 0; npart < nhep; npart++) {
            List<Double> vals = getNumbersInLine(tok);
            if (vals.size() != 13)
                throw new RuntimeException("Unexpected entry for a particle");
            if (vals.get(1).intValue() != 9) {//ignore the vertex for now
//                int ip = vals.get(0).intValue();
//                if (ip != i + 1) {
//                    throw new RuntimeException("Particle numbering mismatch");
//                }

                pdgid = vals.get(0).intValue();
//                System.out.println(idhepTmp);
//                System.out.println(pdgid+"   "+vals.get(1).intValue());
                if (Math.abs(pdgid) == 611 || (Math.abs(pdgid) == 11 && vals.get(1).intValue() == 1)) {
//                if (Math.abs(pdgid) == 611) {
                    //                  System.out.println("Ok...getting info for this particle");
                    for (int j = 0; j < 3; j++)
                        mom[j] = vals.get(j + 6);
                    ene = vals.get(10);
                    Hep3Vector p = rotate(mom[0], mom[1], mom[2]);
                    //                   Hep3Vector p = rotate(mom[1], mom[0], mom[2]); //flip x,y because my trident files have cut on thetaX that may bias things
                    Hep3Vector o = rotate(beam[0], beam[1], beam[2]);
                    charge = 1;
                    if (pdgid == 611)
                        charge = -1;
                    HepLorentzVector pl = new BasicHepLorentzVector(ene, p);
                    HelixParamCalculator hpc = new HelixParamCalculator(p, o, charge, bField);
                    double d0 = hpc.getDCA();
                    double phi0 = hpc.getPhi0();
                    double z0 = hpc.getZ0();
                    double slope = hpc.getSlopeSZPlane();
                    double R = hpc.getRadius();
                    double x0 = hpc.getX0();
                    double y0 = hpc.getY0();
                    double xc = getxc(R, d0, phi0);
                    double yc = getyc(R, d0, phi0);;

//                     System.out.println(p.toString());
//                     System.out.println("d0 = "+d0+"; phi0 = "+phi0+"; z0 = "+z0+"; slope = "+slope+"; R = "+R);
//                       System.out.println("x0 = "+x0+"; y0 = "+y0+"; xc = "+xc+"; yc = "+yc);
                    List<Boolean> passLayerTarget50 = new ArrayList<Boolean>();
                    List<Boolean> passLayerNoMax = new ArrayList<Boolean>();
                    List<Boolean> passLayerFull = new ArrayList<Boolean>();
                    for (int ii = 0; ii < nLayers; ii++) {
                        double pathL = PathToXPlane(x0, y0, xc, yc, R, x[ii]);
//                        System.out.println("path length "+pathL);
                        Hep3Vector posL = PointOnHelix(xc, yc, R, phi0, z0, slope, pathL);
//                        System.out.println("Position "+posL.toString());
                        //                       passLayer.add(inAcceptance(posL, ySize[ii], zSize[ii], zGap[ii]));
                        passLayerFull.add(inAcceptance(posL, ySize[ii], zSize[ii], zGap[ii]));
                        passLayerNoMax.add(inAcceptance(posL, 9999999, 9999999, zGap[ii]));                      
                        
                          double pathLTg50 = PathToXPlane(x0, y0, xc, yc, R, x[ii]-50.0);
//                        System.out.println("path length "+pathL);
                        Hep3Vector posLTg50 = PointOnHelix(xc, yc, R, phi0, z0, slope, pathLTg50);
//                        System.out.println("Position "+posL.toString());
                         passLayerTarget50.add(inAcceptance(posLTg50, ySize[ii], zSize[ii], zGap[ii]));
                    }

                    if (pdgid == 611) {  //electron from A'
                        passEleTarget50 = passLayerTarget50;
                        passEleNoMax = passLayerNoMax;
                        passEleFull = passLayerFull;
                        pEle = pl;
                    } else if (pdgid == -611 || pdgid == -11) {//positron
                        pPos = pl;
                        passPosTarget50 = passLayerTarget50;
                        passPosNoMax = passLayerNoMax;
                        passPosFull = passLayerFull;
                    } else if (pdgid == 11)
                        if (!foundRecoil) {
                            foundRecoil = true;
                            passRecoilTarget50 = passLayerTarget50;
                            passRecoilNoMax = passLayerNoMax;
                            passRecoilFull = passLayerFull;
                            pRecoil = pl;
                        } //                        else{                    //                             passEle = passLayer;
                    //                            passEleFull = passLayerFull;
                    //                            pEle = p;
                    //                        }                    

                }

                i++;
            }
        }
        double invMass = getInvMass(pEle.v3(), pPos.v3());
        doAccounting(passEleTarget50, passPosTarget50, passEleNoMax, passPosNoMax, passEleFull, passPosFull, eventPass(pEle.v3(), pPos.v3()), 1000.0 * invMass);
        //       doAccounting(passRecoil, passPos, passRecoilFull, passPosFull, eventPass(pRecoil, pPos), 1000.0 * getInvMass(pRecoil, pPos));
    }

    public static Hep3Vector rotate(double x, double y, double z) {
        return new BasicHep3Vector(z, x, y);
    }

    public static String convertDecimal(String num) {
        if (num.contains("."))
            num = num.replace(".", "pt");
        return num;
    }

    public static boolean inAcceptance(Hep3Vector position, double yExt, double zExt, double zGap) {
        double ypos = position.y();
        if (Math.abs(ypos) > yExt)
            return false;
        double zpos = position.z();
        if (Math.abs(zpos) < zGap)
            return false;
        if (Math.abs(zpos) > zGap + zExt)
            return false;

        return true;
    }

    public static boolean eventPass(Hep3Vector p1, Hep3Vector p2) {
        //       System.out.println("p1.magnitude = "+p1.magnitude()+"; p2.magnitude = "+p2.magnitude()+"; 0.8*_ecm = "+0.8*_ecm);

        if (p1.magnitude() + p2.magnitude() < 0.8 * _ecm)//trigger requires 80% of beam energy
        
            return false;
//        System.out.println("Passed totenergy");
        if (p1.magnitude() < _pCut)
            return false;
        if (p2.magnitude() < _pCut)
            return false;
        if (p2.z() * p1.z() > 0) // this is basically the opposite quadrant cut in the trigger (B-field makes them opposite in y)
        
            return false;
        //      System.out.println("Event is good!!!!!");
        return true;

    }

    public static Double PathToXPlane(double x0, double y0, double xc, double yc, double RC, double x) {
        //  Create a list to hold the path lengths
        Double path;

        double y = yc + Math.signum(RC) * Math.sqrt(RC * RC - Math.pow(x - xc, 2));
//        System.out.println("x = "+x+"; y = "+y);
        double s = PathCalc(xc, yc, RC, x0, y0, x, y);

//        System.out.println("PathToXPlane :  s = "+s+"; sFromClass = "+sFromClass);

        path = s;

        return path;
    }

    private static double PathCalc(double xc, double yc, double RC, double x1, double y1, double x2, double y2) {
        //  Find the angle between these points measured wrt the circle center
        double phi1 = Math.atan2(y1 - yc, x1 - xc);
        double phi2 = Math.atan2(y2 - yc, x2 - xc);
        double dphi = phi2 - phi1;
        //  Make sure dphi is in the valid range (-pi, pi)
        if (dphi > Math.PI)
            dphi -= 2. * Math.PI;
        if (dphi < -Math.PI)
            dphi += 2. * Math.PI;
        //  Return the arc length
        return -RC * dphi;
    }

    public static Hep3Vector PointOnHelix(double xc, double yc, double RC, double phi0, double z0, double slope, double s) {
        //  Find the azimuthal direction at this path length      
        double phi = phi0 - s / RC;
        //  Calculate the position on the helix at this path length
        double x = xc - RC * Math.sin(phi);
        double y = yc + RC * Math.cos(phi);
        double z = z0 + s * slope;
        //  Return the position as a Hep3Vector
        return new BasicHep3Vector(x, y, z);
    }

    private static double getxc(double R, double d0, double phi0) {
        return (R - d0) * Math.sin(phi0);
    }

    private static double getyc(double R, double d0, double phi0) {
        return -(R - d0) * Math.cos(phi0);
    }

    public static double getInvMass(Hep3Vector p1, Hep3Vector p2) {
        double esum = 0.;
        double pxsum = 0.;
        double pysum = 0.;
        double pzsum = 0.;
        double me = 0.000511;
        if (_isMuon)
            me = 0.1057;
        // Loop over tracks


        double p1x = p1.x();
        double p1y = p1.y();
        double p1z = p1.z();
        double p1mag2 = p1x * p1x + p1y * p1y + p1z * p1z;
        double e1 = Math.sqrt(p1mag2 + me * me);
        pxsum += p1x;
        pysum += p1y;
        pzsum += p1z;
        esum += e1;

        double p2x = p2.x();
        double p2y = p2.y();
        double p2z = p2.z();
        double p2mag2 = p2x * p2x + p2y * p2y + p2z * p2z;
        double e2 = Math.sqrt(p2mag2 + me * me);
        pxsum += p2x;
        pysum += p2y;
        pzsum += p2z;
        esum += e2;
        double psum = Math.sqrt(pxsum * pxsum + pysum * pysum + pzsum * pzsum);
        double evtmass = esum * esum - psum * psum;

        if (evtmass > 0)
            return Math.sqrt(evtmass);
        else
            return -99;
    }
}
