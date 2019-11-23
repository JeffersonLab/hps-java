/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.analysis.util;

/**
 *
 * @author mgraham
 */
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;

import hep.physics.vec.BasicHep3Matrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.BasicHepLorentzVector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.HepLorentzVector;
import org.lcsim.detector.tracker.silicon.SiSensor;

import org.lcsim.detector.converter.compact.subdetector.HpsTracker2;
import org.lcsim.detector.converter.compact.subdetector.SvtStereoLayer;
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
import org.hps.detector.ecal.EcalCrystal;
import org.hps.detector.ecal.HPSEcalDetectorElement;
import org.hps.recon.tracking.TrackUtils;
import org.jdom.JDOMException;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.Rotation3D;
import org.lcsim.detector.Transform3D;
import org.lcsim.detector.solids.Box;
import org.lcsim.detector.solids.Point3D;
import org.lcsim.detector.solids.Polygon3D;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.base.BaseTrack;
import org.lcsim.fit.helicaltrack.HelixParamCalculator;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.FieldMap;
import org.lcsim.geometry.GeometryReader;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.xml.ElementFactory;

public class CalcAccFromMadGraphWithDetector {

    static private List<SiSensor> sensors = null;
    static private List<EcalCrystal> crystals = null;
    static private Map<Integer, List<SvtStereoLayer>> topStereoLayers = new HashMap<Integer, List<SvtStereoLayer>>();
    static private Map<Integer, List<SvtStereoLayer>> bottomStereoLayers = new HashMap<Integer, List<SvtStereoLayer>>();

    static private boolean debug = false;
    private static final String SUBDETECTOR_NAME = "Tracker";
    static boolean expDecay = false;  //set a ctau decay length
    static boolean flatDecay = false; //decay uniformily in some range
    static boolean trident = false;  //are these trident events or A' signal events0.
    static boolean reqEcal = true;
    static double _declength = 0.0;   //A' decay length  (mm)
    static double _xoff = 0.0;   //set the x,y,z origin offsets...
    static double _yoff = 0.0;
    static double _zoff = 0.0;
    static double aMass = 99;  //Aprime mass (MeV)
    static double sigx = 0.2;        // Luminous region size in x (mm)
    static double sigy = 0.02;        // Luminous region size in y (mm)
    static double sigz = 0.0001;         // Luminous region size in z (mm)
    static double maxLen = 200; // maximum decay length (mm)
    static int nInFiles = 1000;  //number of input files
    static int nmax = 500000;  //maximum number of events to write to 1 stdhep file (new one opens if n>nmax)
    static String fileType = "lhe";
    static int nread = -1;  // a running total of number of events read/written to stdhep files
    static boolean _eventFilter = false;
    static boolean _isMuon = false;
    static int _nEleRequired = 2;
    static int nLayers = 7;  //for L0 studies
//    static int nLayers = 6;  //nominal number
    static double beamAngle = 0.03; //30 mrad about the y-axis in lab frame

    static int nGenerated = 0;
    static int nPassNoMax = 0;

    static int nPassFull = 0;
    static int nPassPosTrig = 0;
    static int nPassHalf = 0;
    static int nPassPosTrigHalf = 0;
    static int nPassLyr50 = 0;
    /*
     * static double maxMass = 1000.0;    
     * static double _pCut = 0.25;
     * static double _ecm = 6.6; //GeV
     */
    static String _custom = "";

    /*  1.1 GeV  */
//    static double maxMass = 250.0;
//    static double _pCut = 0.05;
//    static double _ecm = 1.1; //GeV

    /*  2.2 GeV  */
//    static double maxMass = 500.0;
//    static double _pCut = 0.1;
//    static double _ecm = 2.2; //GeV
//
//           /*  4.4 GeV  */
//    static double maxMass = 500.0;
//    static double _pCut = 0.2;
//    static double _ecm = 4.4; //GeV
    //           /*  6.6 GeV  */   
    static double maxMass = 1000.0;
    static double _pCut = 0.33;
    static double _ecm = 6.6; //GeV

    static double binSize = 1.0;
    static int nbins = (int) (maxMass / binSize);
    static int[] genMass = new int[nbins];
    //static int[] recoMassLyr1 = new int[nbins];
    static int[] recoMassGen = new int[nbins];
    static int[] recoMassLyr1Lyr1 = new int[nbins];
    static int[] recoMassLyr1Lyr2 = new int[nbins];
    static int[] recoMassLyr2Lyr2 = new int[nbins];
    static int[] recoMassLyr2Lyr3 = new int[nbins];
    static int[] recoMassLyr3Lyr3 = new int[nbins];
    static int[] recoMassLyr1Lyr3 = new int[nbins];

    static int[] recoMassPosTrigLyr1Lyr1 = new int[nbins];
    static int[] recoMassPosTrigLyr1Lyr2 = new int[nbins];
    static int[] recoMassPosTrigLyr2Lyr2 = new int[nbins];
    static int[] recoMassPosTrigLyr2Lyr3 = new int[nbins];
    static int[] recoMassPosTrigLyr3Lyr3 = new int[nbins];
    static int[] recoMassPosTrigLyr1Lyr3 = new int[nbins];

    static int[] recoMassInHalfLyr1Lyr1 = new int[nbins];
    static int[] recoMassInHalfLyr1Lyr2 = new int[nbins];
    static int[] recoMassInHalfLyr2Lyr2 = new int[nbins];
    static int[] recoMassInHalfLyr2Lyr3 = new int[nbins];
    static int[] recoMassInHalfLyr3Lyr3 = new int[nbins];
    static int[] recoMassInHalfLyr1Lyr3 = new int[nbins];

    static int[] recoMassPosTrigInHalfLyr1Lyr1 = new int[nbins];
    static int[] recoMassPosTrigInHalfLyr1Lyr2 = new int[nbins];
    static int[] recoMassPosTrigInHalfLyr2Lyr2 = new int[nbins];
    static int[] recoMassPosTrigInHalfLyr2Lyr3 = new int[nbins];
    static int[] recoMassPosTrigInHalfLyr3Lyr3 = new int[nbins];
    static int[] recoMassPosTrigInHalfLyr1Lyr3 = new int[nbins];

    /**
     * Z position to start extrapolation from
     */
    static double extStartPos = 700; // mm

    /**
     * The extrapolation step size
     */
    static double stepSize = 5.0; // mm
    /**
     * Name of the constant denoting the position of the Ecal face in the
     * compact description.
     */
    static String ECAL_POSITION_CONSTANT_NAME = "ecal_dface";

    // these numbers will get overwritten from the true geometry from compact.xml conversion!!!
    static double halfCrystal = 13.3 / 2;//size at the front
    static double x_edge_low = -262.74 - halfCrystal;
    static double x_edge_high = 347.7 + halfCrystal;
    static double y_edge_low = 33.54 - halfCrystal;
    static double y_edge_high = 75.18 + halfCrystal;

    static double x_gap_low = -106.66 - halfCrystal;
    static double x_gap_high = 42.17 + halfCrystal;
    static double y_gap_high = 47.18 - halfCrystal;

    static double shiftECalinX = 0; //amount to shift ecal in (detector) X...settable via command line
    static double bFieldMultiplier = 1;  //scaling factor for b-field...settable via command line
    static double B_FIELD = 0.23;//Tesla
    /**
     * The B field map
     */
    static FieldMap bFieldMap = null;
    /**
     * Position of the Ecal face
     */
    static double ecalPosition; // mm...this get's read out from the detector
    static int nbinsH = 50;
    static protected AIDA aida = AIDA.defaultInstance();
    static IHistogram1D esumPassGen = aida.histogram1D("MadGraph Generated ESum (GeV)", nbinsH, 0.5 * _ecm, 1.1 * _ecm);
    static IHistogram1D esumPassL1L1 = aida.histogram1D("MadGraph Pass L1L1 ESum (GeV)", nbinsH, 0.5 * _ecm, 1.1 * _ecm);
    static IHistogram1D esumPassL1L2 = aida.histogram1D("MadGraph Pass L1L2 ESum (GeV)", nbinsH, 0.5 * _ecm, 1.1 * _ecm);
    static IHistogram1D esumPassL2L2 = aida.histogram1D("MadGraph Pass L2L2 ESum (GeV)", nbinsH, 0.5 * _ecm, 1.1 * _ecm);

    static IHistogram1D massPassGen = aida.histogram1D("MadGraph Generated Mass (GeV)", nbinsH, 0, maxMass);
    static IHistogram1D massPassL1L1 = aida.histogram1D("MadGraph Pass L1L1 Mass (GeV)", nbinsH, 0, maxMass);
    static IHistogram1D massPassL1L2 = aida.histogram1D("MadGraph Pass L1L2 Mass (GeV)", nbinsH, 0, maxMass);
    static IHistogram1D massPassL2L2 = aida.histogram1D("MadGraph Pass L2L2 Mass (GeV)", nbinsH, 0, maxMass);

    static IHistogram1D pElePassGen = aida.histogram1D("MadGraph Generated pEle (GeV)", nbinsH, 0, 1.1 * _ecm);
    static IHistogram1D pElePassL1L1 = aida.histogram1D("MadGraph Pass L1L1  pEle (GeV)", nbinsH, 0, 1.1 * _ecm);
    static IHistogram1D pElePassL1L2 = aida.histogram1D("MadGraph Pass L1L2  pEle (GeV)", nbinsH, 0, 1.1 * _ecm);
    static IHistogram1D pElePassL2L2 = aida.histogram1D("MadGraph Pass L2L2  pEle (GeV)", nbinsH, 0, 1.1 * _ecm);

    static IHistogram1D pPosPassGen = aida.histogram1D("MadGraph Generated pPos (GeV)", nbinsH, 0, 1.1 * _ecm);
    static IHistogram1D pPosPassL1L1 = aida.histogram1D("MadGraph Pass L1L1  pPos (GeV)", nbinsH, 0, 1.1 * _ecm);
    static IHistogram1D pPosPassL1L2 = aida.histogram1D("MadGraph Pass L1L2  pPos (GeV)", nbinsH, 0, 1.1 * _ecm);
    static IHistogram1D pPosPassL2L2 = aida.histogram1D("MadGraph Pass L2L2  pPos (GeV)", nbinsH, 0, 1.1 * _ecm);

    static IHistogram1D slopeElePassGen = aida.histogram1D("MadGraph Generated slopeEle (GeV)", nbinsH, -0.1, 0.1);
    static IHistogram1D slopeElePassL1L1 = aida.histogram1D("MadGraph Pass L1L1  slopeEle (GeV)", nbinsH, -0.1, 0.1);
    static IHistogram1D slopeElePassL1L2 = aida.histogram1D("MadGraph Pass L1L2  slopeEle (GeV)", nbinsH, -0.1, 0.1);
    static IHistogram1D slopeElePassL2L2 = aida.histogram1D("MadGraph Pass L2L2  slopeEle (GeV)", nbinsH, -0.1, 0.1);

    static IHistogram1D slopePosPassGen = aida.histogram1D("MadGraph Generated slopePos (GeV)", nbinsH, -0.1, 0.1);
    static IHistogram1D slopePosPassL1L1 = aida.histogram1D("MadGraph Pass L1L1  slopePos (GeV)", nbinsH, -0.1, 0.1);
    static IHistogram1D slopePosPassL1L2 = aida.histogram1D("MadGraph Pass L1L2  slopePos (GeV)", nbinsH, -0.1, 0.1);
    static IHistogram1D slopePosPassL2L2 = aida.histogram1D("MadGraph Pass L2L2  slopePos (GeV)", nbinsH, -0.1, 0.1);

    static IHistogram2D atEcalElePassGen = aida.histogram2D("MadGraph Generated atEcalEle (mm)", nbinsH, -300, 400, nbinsH, -100, 100);
    static IHistogram2D atEcalElePassL1L1 = aida.histogram2D("MadGraph Pass L1L1  atEcalEle (mm)", nbinsH, -300, 400, nbinsH, -100, 100);
    static IHistogram2D atEcalElePassL1L2 = aida.histogram2D("MadGraph Pass L1L2  atEcalEle (mm)", nbinsH, -300, 400, nbinsH, -100, 100);
    static IHistogram2D atEcalElePassL2L2 = aida.histogram2D("MadGraph Pass L2L2  atEcalEle (mm)", nbinsH, -300, 400, nbinsH, -100, 100);

    static IHistogram2D atEcalPosPassGen = aida.histogram2D("MadGraph Generated atEcalPos (mm)", nbinsH, -300, 400, nbinsH, -100, 100);
    static IHistogram2D atEcalPosPassL1L1 = aida.histogram2D("MadGraph Pass L1L1  atEcalPos (mm)", nbinsH, -300, 400, nbinsH, -100, 100);
    static IHistogram2D atEcalPosPassL1L2 = aida.histogram2D("MadGraph Pass L1L2  atEcalPos (mm)", nbinsH, -300, 400, nbinsH, -100, 100);
    static IHistogram2D atEcalPosPassL2L2 = aida.histogram2D("MadGraph Pass L2L2  atEcalPos (mm)", nbinsH, -300, 400, nbinsH, -100, 100);

    public static void doAccounting(List<Boolean> passEleFull, List<Boolean> passPosFull, boolean passEvt, boolean passEleEcal, boolean passPosEcal, boolean passEleEcalInHalf, boolean passPosEcalInHalf, HepLorentzVector pEle, HepLorentzVector pPos, double[] atEcalEle, double[] atEcalPos) {

        ///////   FAKE THE ECAL PASSING!
        if (!reqEcal) {
            passEleEcal = true;
            passPosEcal = true;
            passEleEcalInHalf = true;
            passPosEcalInHalf = true;
        }
        boolean passL1L1 = false;
        boolean passL1L2 = false;
        boolean passL2L2 = false;
        boolean passL2L3 = false;
        boolean passL1L3 = false;
        boolean passL3L3 = false;

        int minHits = 5;
        int eleHits = 0;
        int posHits = 0;

        for (Boolean isHit : passEleFull)
            if (isHit)
                eleHits++;

        for (Boolean isHit : passPosFull)
            if (isHit)
                posHits++;

        //see if they were in layer 1
        if (passEleFull.get(0) && passPosFull.get(0))
            passL1L1 = true;
        else if ((passEleFull.get(0) && passPosFull.get(1)) || (passEleFull.get(1) && passPosFull.get(0)))
            passL1L2 = true;
        else if (passEleFull.get(1) && passPosFull.get(1))
            passL2L2 = true;
        else if ((passEleFull.get(0) && passPosFull.get(2)) || (passEleFull.get(2) && passPosFull.get(0)))
            passL1L3 = true;
        else if ((passEleFull.get(1) && passPosFull.get(2)) || (passEleFull.get(2) && passPosFull.get(1))) {
            if (debug)
                System.out.println("Found an L2L3 Event!!!  Pass Event = " + passEvt + "; # of ele hits = " + eleHits + "; # of pos hits = " + posHits + "; passEleECal=" + passEleEcal + "; passPosECal=" + passPosEcal);
            passL2L3 = true;
        } else if (passEleFull.get(2) && passPosFull.get(2))
            passL3L3 = true;

        double invMass = 1000 * getInvMass(pEle.v3(), pPos.v3());
        //find the bin...
        int bin = (int) (invMass / maxMass * nbins);
//        System.out.println("invMass = " + invMass + "...goes in bin #" + bin + "; electron hits = " + eleHits + "; pass ecal = " + passEleEcal + "; positron hits = " + posHits + "; pass ecal = " + passPosEcal + "; pass event? " + passEvt);

        if (debug) {
            System.out.println("Event Summary:");
            System.out.println("\t\tElectron:  #hits = " + eleHits + "; passEleEcal = " + passEleEcal + "; passEleEcalInHalf = " + passEleEcalInHalf + "; EcalPosition(x,y)=(" + atEcalEle[1] + "," + atEcalEle[2] + ")");
            System.out.println("\t\tPositron:  #hits = " + posHits + "; passPosEcal = " + passPosEcal + "; passPosEcalInHalf = " + passPosEcalInHalf + "; EcalPosition(x,y)=(" + atEcalPos[1] + "," + atEcalPos[2] + ")");
        }
        double esum = pEle.v3().magnitude() + pPos.v3().magnitude();
        double pele = pEle.v3().magnitude();
        double ppos = pPos.v3().magnitude();
        double slopeele = pEle.v3().z() / Math.sqrt(pEle.v3().x() * pEle.v3().x() + pEle.v3().y() * pEle.v3().y());
        double slopepos = pPos.v3().z() / Math.sqrt(pPos.v3().x() * pPos.v3().x() + pPos.v3().y() * pPos.v3().y());
        esumPassGen.fill(esum);
        massPassGen.fill(invMass);
        pElePassGen.fill(pele);
        pPosPassGen.fill(ppos);
        slopeElePassGen.fill(slopeele);
        slopePosPassGen.fill(slopepos);
        atEcalElePassGen.fill(atEcalEle[1], atEcalEle[2]);
        atEcalPosPassGen.fill(atEcalPos[1], atEcalPos[2]);
        if (bin < nbins) {
            genMass[bin]++;
            nGenerated++;

            //ok...fill the histograms
            if (passL1L1 && passEvt && eleHits >= minHits && posHits >= minHits && passEleEcal && passPosEcal) {
                recoMassLyr1Lyr1[bin]++;
                nPassFull++;
                esumPassL1L1.fill(esum);
                massPassL1L1.fill(invMass);
                pElePassL1L1.fill(pele);
                pPosPassL1L1.fill(ppos);
                slopeElePassL1L1.fill(slopeele);
                slopePosPassL1L1.fill(slopepos);
                atEcalElePassL1L1.fill(atEcalEle[1], atEcalEle[2]);
                atEcalPosPassL1L1.fill(atEcalPos[1], atEcalPos[2]);
            }
            if (passL1L2 && passEvt && eleHits >= minHits && posHits >= minHits && passEleEcal && passPosEcal) {
                recoMassLyr1Lyr2[bin]++;
                nPassFull++;
                esumPassL1L2.fill(esum);
                massPassL1L2.fill(invMass);
                pElePassL1L2.fill(pele);
                pPosPassL1L2.fill(ppos);
                slopeElePassL1L2.fill(slopeele);
                slopePosPassL1L2.fill(slopepos);
                atEcalElePassL1L2.fill(atEcalEle[1], atEcalEle[2]);
                atEcalPosPassL1L2.fill(atEcalPos[1], atEcalPos[2]);
            }
            if (passL2L2 && passEvt && eleHits >= minHits && posHits >= minHits && passEleEcal && passPosEcal) {
                recoMassLyr2Lyr2[bin]++;
                nPassFull++;
                esumPassL2L2.fill(esum);
                massPassL2L2.fill(invMass);
                pElePassL2L2.fill(pele);
                pPosPassL2L2.fill(ppos);
                slopeElePassL2L2.fill(slopeele);
                slopePosPassL2L2.fill(slopepos);
                atEcalElePassL2L2.fill(atEcalEle[1], atEcalEle[2]);
                atEcalPosPassL2L2.fill(atEcalPos[1], atEcalPos[2]);
            }
            if (passL1L3 && passEvt && eleHits >= minHits && posHits >= minHits && passEleEcal && passPosEcal) {
                recoMassLyr1Lyr3[bin]++;
                nPassFull++;
            }
            if (passL2L3 && passEvt && eleHits >= minHits && posHits >= minHits && passEleEcal && passPosEcal) {
                if (debug)
                    System.out.println("Incrementing L2L3!!!!         " + nPassFull);
                recoMassLyr2Lyr3[bin]++;
                nPassFull++;
            }
            if (passL3L3 && passEvt && eleHits >= minHits && posHits >= minHits && passEleEcal && passPosEcal) {
                recoMassLyr3Lyr3[bin]++;
                nPassFull++;
            }

            if (passL1L1 && passEvt && eleHits >= minHits && posHits >= minHits && passEleEcalInHalf && passPosEcalInHalf) {
                recoMassInHalfLyr1Lyr1[bin]++;
                nPassHalf++;
            }
            if (passL1L2 && passEvt && eleHits >= minHits && posHits >= minHits && passEleEcalInHalf && passPosEcalInHalf) {
                recoMassInHalfLyr1Lyr2[bin]++;
                nPassHalf++;
            }
            if (passL2L2 && passEvt && eleHits >= minHits && posHits >= minHits && passEleEcalInHalf && passPosEcalInHalf) {
                recoMassInHalfLyr2Lyr2[bin]++;
                nPassHalf++;
            }
            if (passL1L3 && passEvt && eleHits >= minHits && posHits >= minHits && passEleEcalInHalf && passPosEcalInHalf) {
                recoMassInHalfLyr1Lyr3[bin]++;
                nPassHalf++;
            }
            if (passL2L3 && passEvt && eleHits >= minHits && posHits >= minHits && passEleEcalInHalf && passPosEcalInHalf) {
                recoMassInHalfLyr2Lyr3[bin]++;
                nPassHalf++;
            }
            if (passL3L3 && passEvt && eleHits >= minHits && posHits >= minHits && passEleEcalInHalf && passPosEcalInHalf) {
                recoMassInHalfLyr3Lyr3[bin]++;
                nPassHalf++;
            }

            if (passL1L1 && passEvt && eleHits >= minHits && posHits >= minHits && passPosEcal && atEcalPos[1] > 100) {
                recoMassPosTrigLyr1Lyr1[bin]++;
                nPassPosTrig++;
            }
            if (passL1L2 && passEvt && eleHits >= minHits && posHits >= minHits && passPosEcal && atEcalPos[1] > 100) {
                recoMassPosTrigLyr1Lyr2[bin]++;
                nPassPosTrig++;
            }
            if (passL2L2 && passEvt && eleHits >= minHits && posHits >= minHits && passPosEcal && atEcalPos[1] > 100) {
                recoMassPosTrigLyr2Lyr2[bin]++;
                nPassPosTrig++;
            }
            if (passL1L3 && passEvt && eleHits >= minHits && posHits >= minHits && passPosEcal && atEcalPos[1] > 100) {
                recoMassPosTrigLyr1Lyr3[bin]++;
                nPassPosTrig++;
            }
            if (passL2L3 && passEvt && eleHits >= minHits && posHits >= minHits && passPosEcal && atEcalPos[1] > 100) {
                recoMassPosTrigLyr2Lyr3[bin]++;
                nPassPosTrig++;
            }
            if (passL3L3 && passEvt && eleHits >= minHits && posHits >= minHits && passPosEcal && atEcalPos[1] > 100) {
                recoMassPosTrigLyr3Lyr3[bin]++;
                nPassPosTrig++;
            }

            if (passL1L1 && passEvt && eleHits >= minHits && posHits >= minHits && passPosEcalInHalf && atEcalPos[1] > 100) {
                recoMassPosTrigInHalfLyr1Lyr1[bin]++;
                nPassPosTrigHalf++;
            }
            if (passL1L2 && passEvt && eleHits >= minHits && posHits >= minHits && passPosEcalInHalf && atEcalPos[1] > 100) {
                recoMassPosTrigInHalfLyr1Lyr2[bin]++;
                nPassPosTrigHalf++;
            }
            if (passL2L2 && passEvt && eleHits >= minHits && posHits >= minHits && passPosEcalInHalf && atEcalPos[1] > 100) {
                recoMassPosTrigInHalfLyr2Lyr2[bin]++;
                nPassPosTrigHalf++;
            }
            if (passL1L3 && passEvt && eleHits >= minHits && posHits >= minHits && passPosEcalInHalf && atEcalPos[1] > 100) {
                recoMassPosTrigInHalfLyr1Lyr3[bin]++;
                nPassPosTrigHalf++;
            }
            if (passL2L3 && passEvt && eleHits >= minHits && posHits >= minHits && passPosEcalInHalf && atEcalPos[1] > 100) {
                recoMassPosTrigInHalfLyr2Lyr3[bin]++;
                nPassPosTrigHalf++;
            }
            if (passL3L3 && passEvt && eleHits >= minHits && posHits >= minHits && passPosEcalInHalf && atEcalPos[1] > 100) {
                recoMassPosTrigInHalfLyr3Lyr3[bin]++;
                nPassPosTrigHalf++;
            }

        } else {
//            System.out.println("Mass out of range!  "+invMass);
        }

    }

    private static Options createCommandLineOptions() {
        Options options = new Options();

        options.addOption(new Option("e", true, "Beam Energy (GeV)"));
        options.addOption(new Option("n", true, "Number of files to run."));
        options.addOption(new Option("b", true, "B-Field Multiplier"));
        options.addOption(new Option("x", true, "ECal X Shift"));
        options.addOption(new Option("z", true, "z-offset"));
        options.addOption(new Option("t", true, "Rad, BH, or FullRadBH"));
        options.addOption(new Option("u", false, "Is muon decay?"));
        options.addOption(new Option("c", true, "Custom String"));
        options.addOption(new Option("d", true, "Detector Name"));
        options.addOption(new Option("q", false, "Require ECal Hit?  Defaults to true...include -q to turn off"));
        return options;
    }

    /**
     * @param args the command line arguments
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

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
        String intypeString = "Rad";
        String zString = "0";
        String xString = "0";
        String detName = "Foobar";
        eptString = convertDecimal(eptString);
//  if (!reqEcal)
//            typeString += "NoEcal";
        // don't put any option catches about this...makes the type the first label in the file
        if (cl.hasOption("t")) {
            typeString = cl.getOptionValue("t");
            System.out.println(typeString);
            if (typeString.contains("Tri"))
                trident = true;
            if (typeString.contains("BH"))
                trident = true;
            intypeString = typeString;

        }

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
            bFieldMultiplier = Double.valueOf(bString);
            System.out.println(bString);
            bString = "BField" + convertDecimal(bString) + "x";
            typeString += "_" + bString + "_";
        }

        if (cl.hasOption("x")) {
            xString = cl.getOptionValue("x");
            shiftECalinX = Double.valueOf(xString);
            System.out.println(xString);
            xString = "ECalXSHift" + convertDecimal(xString) + "mm";
            typeString += "_" + xString + "_";
        }

        if (cl.hasOption("z")) {
            zString = cl.getOptionValue("z");
            _zoff = Double.valueOf(zString);
            System.out.println(zString);
            zString = convertDecimal(zString) + "mm";
        }

        if (cl.hasOption("u"))
            _isMuon = true;
        boolean _hasCustomString = false;
        if (cl.hasOption("c")) {
            _custom = cl.getOptionValue("c");
            _hasCustomString = true;
            System.out.println("Using custom string = " + _custom);
        }

        if (cl.hasOption("d")) {
            detName = cl.getOptionValue("d");
            System.out.println("Using detector = " + detName);
        }

        if (cl.hasOption("q")) {
            reqEcal = false;
            typeString += "_NoEcal_";
        }
///////////////////////////////////   Get Geoemetry    ////////////////////////////////////        
        GeometryReader reader = new GeometryReader();
//      File compact = new File("/Users/mgraham/hps/hps-layer0/detector-data/detectors/HPS-Proposal2017-Nominal-v2-2pt3-fieldmap/compact.xml");
//        File compact = new File("/Users/mgraham/hps/hps-layer0/detector-data/detectors/" + detName + "/compact.xml");
        File compact = new File("/Users/mgraham/hps/hps-java/detector-data/detectors/" + detName + "/compact.xml");
        FileInputStream fis = new FileInputStream(compact);
        Detector detector = null;
        try {
            detector = reader.read(fis);
        } catch (JDOMException ex) {
            Logger.getLogger(CalcAccFromMadGraphWithDetector.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ElementFactory.ElementCreationException ex) {
            Logger.getLogger(CalcAccFromMadGraphWithDetector.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Get the HpsSiSensor objects from the tracker detector element
        if (detector != null) {
            sensors = detector.getSubdetector(SUBDETECTOR_NAME)
                    .getDetectorElement().findDescendants(SiSensor.class);
            B_FIELD = detector.getFieldMap().getField(new BasicHep3Vector(0, 0, 500)).y();
            // Get the field map from the detector object
            bFieldMap = detector.getFieldMap();
            // Get the position of the Ecal from the compact description
            ecalPosition = detector.getConstants().get(ECAL_POSITION_CONSTANT_NAME).getValue();
            HPSEcalDetectorElement ecal = (HPSEcalDetectorElement) detector.getSubdetector("Ecal").getDetectorElement();
            System.out.println("Upper Half ECal Boundries");
            x_edge_low = ecal.getCrystal(-23, 5).getPositionFront().x();
            x_edge_high = ecal.getCrystal(23, 5).getPositionFront().x();
            y_edge_low = ecal.getCrystal(-23, 1).getPositionFront().y();
            y_edge_high = ecal.getCrystal(-23, 5).getPositionFront().y();
            x_gap_low = ecal.getCrystal(-11, 2).getPositionFront().x();
            x_gap_high = ecal.getCrystal(-1, 2).getPositionFront().x();
            y_gap_high = ecal.getCrystal(-1, 2).getPositionFront().y();
            System.out.println("x_edge_low = " + x_edge_low);
            System.out.println("x_edge_high = " + x_edge_high);
            System.out.println("y_edge_low = " + y_edge_low);
            System.out.println("y_edge_high = " + y_edge_high);
            System.out.println("x_gap_low = " + x_gap_low);
            System.out.println("x_gap_high = " + x_gap_high);
            System.out.println("y_gap_high = " + y_gap_high);
        } else
            System.out.println("DAAAAAHHHHHHHHHH   My Detector is NULL!!!!!!!");
        System.out.println("Ok...done getting detector...ECal position == " + ecalPosition);
        List<SvtStereoLayer> stereoLayers
                = ((HpsTracker2) detector.getSubdetector(SUBDETECTOR_NAME).getDetectorElement()).getStereoPairs();
        for (SvtStereoLayer stereoLayer : stereoLayers)
            if (stereoLayer.getAxialSensor().isTopLayer()) {
                System.out.println("Adding stereo layer " + stereoLayer.getLayerNumber());
                if (!topStereoLayers.containsKey(stereoLayer.getLayerNumber()))
                    topStereoLayers.put(stereoLayer.getLayerNumber(), new ArrayList<SvtStereoLayer>());
                topStereoLayers.get(stereoLayer.getLayerNumber()).add(stereoLayer);
            } else {
                if (!bottomStereoLayers.containsKey(stereoLayer.getLayerNumber()))
                    bottomStereoLayers.put(stereoLayer.getLayerNumber(), new ArrayList<SvtStereoLayer>());
                bottomStereoLayers.get(stereoLayer.getLayerNumber()).add(stereoLayer);
            }
        System.out.println("Ok...done with getting layers...this many:  " + stereoLayers.size());
        System.out.println("Number of bottom layers = " + bottomStereoLayers.size());

//        String fDir = "/nfs/slac/g/hps/mgraham/DarkPhoton/MadGraph/Events" + eptString + typeString + "/";
        String fDir = "/Users/mgraham/hps/MadGraph/Events" + eptString + intypeString + "/";
//        String fDir = "./";
//        String inLabel = "W" + eptString + "GeV_Ap" + massString + "MeV_";
        String inLabel = "W" + eptString + "GeV_" + intypeString + "_";
//        String inLabel =  intypeString + "_";

        String inPost = "_unweighted_events.lhe";
//        String inPost = ".lhe";

//        String outDir = "/nfs/slac/g/hps/mgraham/DarkPhoton/SignalEvents/";
//        String outDir = "./Acceptance/";
        String outDir = "./AcceptanceZDependence/";
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
                    Logger.getLogger(CalcAccFromMadGraphWithDetector.class.getName()).log(Level.SEVERE, null, ex);
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
                    Logger.getLogger(CalcAccFromMadGraphWithDetector.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        //ok ... now spit out histograms
        System.out.println("nGenerated = " + nGenerated);
        System.out.println("nPass(Full) = " + nPassFull);
        System.out.println("nPass(ECalInHalf) = " + nPassHalf);
        System.out.println("nPass(Positron Trigger) = " + nPassPosTrig);
        System.out.println("*********    Histograms:  Nominal Positions     ***********   ");
        System.out.println("bin     mass   Gen    L1L1     L1L2    L2L2    L1L3    L2L3    L3L3 ");
        for (int kk = 0; kk < nbins; kk++) {
            double mass = ((double) kk / nbins) * maxMass;
//            System.out.println(kk+"\t"+mass+"\t"+genMass[kk]+"\t"+recoMassLyr1[kk]+"\t"+recoMassLyr50[kk]);
//            System.out.printf("%d\t%4.1f\t%d\t%d\t%d\t%d\n", kk, mass, genMass[kk], recoMassLyr1[kk], recoMassLyr50[kk], recoMassLyr1Full[kk]);
//            System.out.printf("%d\t%4.1f\t%d\t%d\t%d\t%d\n", kk, mass, genMass[kk], recoMassLyr1Full[kk], recoMassLyr50Full[kk], recoMassLyr1NoMax[kk]);
            System.out.printf("%d\t%4.1f\t%d\t%d\t%d\t%d\t%d\t%d\t%d\n", kk, mass, genMass[kk], recoMassLyr1Lyr1[kk], recoMassLyr1Lyr2[kk], recoMassLyr2Lyr2[kk], recoMassLyr1Lyr3[kk], recoMassLyr2Lyr3[kk], recoMassLyr3Lyr3[kk]);
        }

        System.out.println("*********    Histograms:  ECal In 1/2 Crystal      ***********   ");
//        System.out.println("bin     mass   Gen    Lyr1     Target50   NoMax ");
        System.out.println("bin     mass   Gen    L1L1     L1L2    L2L2    L1L3    L2L3    L3L3 ");
        for (int kk = 0; kk < nbins; kk++) {
            double mass = ((double) kk / nbins) * maxMass;
            System.out.printf("%d\t%4.1f\t%d\t%d\t%d\t%d\t%d\t%d\t%d\n", kk, mass, genMass[kk], recoMassInHalfLyr1Lyr1[kk], recoMassInHalfLyr1Lyr2[kk], recoMassInHalfLyr2Lyr2[kk], recoMassInHalfLyr1Lyr3[kk], recoMassInHalfLyr2Lyr3[kk], recoMassInHalfLyr3Lyr3[kk]);
        }

        System.out.println("*********    Histograms:  Positron Trigger    ***********   ");
//        System.out.println("bin     mass   Gen    Lyr1     Target50   NoMax ");
        System.out.println("bin     mass   Gen    L1L1     L1L2    L2L2    L1L3    L2L3    L3L3 ");
        for (int kk = 0; kk < nbins; kk++) {
            double mass = ((double) kk / nbins) * maxMass;
            System.out.printf("%d\t%4.1f\t%d\t%d\t%d\t%d\t%d\t%d\t%d\n", kk, mass, genMass[kk], recoMassPosTrigLyr1Lyr1[kk], recoMassPosTrigLyr1Lyr2[kk], recoMassPosTrigLyr2Lyr2[kk], recoMassPosTrigLyr1Lyr3[kk], recoMassPosTrigLyr2Lyr3[kk], recoMassPosTrigLyr3Lyr3[kk]);
        }

        System.out.println("*********    Histograms:  Positron Trigger  ECal In 1/2 Crystal  ***********   ");
//        System.out.println("bin     mass   Gen    Lyr1     Target50   NoMax ");
        System.out.println("bin     mass   Gen    L1L1     L1L2    L2L2    L1L3    L2L3    L3L3 ");
        for (int kk = 0; kk < nbins; kk++) {
            double mass = ((double) kk / nbins) * maxMass;
            System.out.printf("%d\t%4.1f\t%d\t%d\t%d\t%d\t%d\t%d\t%d\n", kk, mass, genMass[kk], recoMassPosTrigInHalfLyr1Lyr1[kk], recoMassPosTrigInHalfLyr1Lyr2[kk], recoMassPosTrigLyr2Lyr2[kk], recoMassPosTrigLyr1Lyr3[kk], recoMassPosTrigLyr2Lyr3[kk], recoMassPosTrigLyr3Lyr3[kk]);
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
        outputFile(outDir + typeString + eptString + "_" + detName + "_" + zString + "_Gen.dat", genMass);
        outputFile(outDir + typeString + eptString + "_" + detName + "_" + zString + "_L1L1.dat", recoMassLyr1Lyr1);
        outputFile(outDir + typeString + eptString + "_" + detName + "_" + zString + "_L1L2.dat", recoMassLyr1Lyr2);
        outputFile(outDir + typeString + eptString + "_" + detName + "_" + zString + "_L2L2.dat", recoMassLyr2Lyr2);
        outputFile(outDir + typeString + eptString + "_" + detName + "_" + zString + "_L1L3.dat", recoMassLyr1Lyr3);
        outputFile(outDir + typeString + eptString + "_" + detName + "_" + zString + "_L2L3.dat", recoMassLyr2Lyr3);
        outputFile(outDir + typeString + eptString + "_" + detName + "_" + zString + "_L3L3.dat", recoMassLyr3Lyr3);

        outputFile(outDir + typeString + eptString + "_" + detName + "_" + zString + "_InHalf_L1L1.dat", recoMassInHalfLyr1Lyr1);
        outputFile(outDir + typeString + eptString + "_" + detName + "_" + zString + "_InHalf_L1L2.dat", recoMassInHalfLyr1Lyr2);
        outputFile(outDir + typeString + eptString + "_" + detName + "_" + zString + "_InHalf_L2L2.dat", recoMassInHalfLyr2Lyr2);
        outputFile(outDir + typeString + eptString + "_" + detName + "_" + zString + "_InHalf_L1L3.dat", recoMassInHalfLyr1Lyr3);
        outputFile(outDir + typeString + eptString + "_" + detName + "_" + zString + "_InHalf_L2L3.dat", recoMassInHalfLyr2Lyr3);
        outputFile(outDir + typeString + eptString + "_" + detName + "_" + zString + "_InHalf_L3L3.dat", recoMassInHalfLyr3Lyr3);

        outputFile(outDir + typeString + eptString + "_" + detName + "_" + zString + "_PosTrig_L1L1.dat", recoMassPosTrigLyr1Lyr1);
        outputFile(outDir + typeString + eptString + "_" + detName + "_" + zString + "_PosTrig_L1L2.dat", recoMassPosTrigLyr1Lyr2);
        outputFile(outDir + typeString + eptString + "_" + detName + "_" + zString + "_PosTrig_L2L2.dat", recoMassPosTrigLyr2Lyr2);
        outputFile(outDir + typeString + eptString + "_" + detName + "_" + zString + "_PosTrig_L1L3.dat", recoMassPosTrigLyr1Lyr3);
        outputFile(outDir + typeString + eptString + "_" + detName + "_" + zString + "_PosTrig_L2L3.dat", recoMassPosTrigLyr2Lyr3);
        outputFile(outDir + typeString + eptString + "_" + detName + "_" + zString + "_PosTrig_L3L3.dat", recoMassPosTrigLyr3Lyr3);

        outputFile(outDir + typeString + eptString + "_" + detName + "_" + zString + "_PosTrigInHalf_L1L1.dat", recoMassPosTrigInHalfLyr1Lyr1);
        outputFile(outDir + typeString + eptString + "_" + detName + "_" + zString + "_PosTrigInHalf_L1L2.dat", recoMassPosTrigInHalfLyr1Lyr2);
        outputFile(outDir + typeString + eptString + "_" + detName + "_" + zString + "_PosTrigInHalf_L2L2.dat", recoMassPosTrigInHalfLyr2Lyr2);
        outputFile(outDir + typeString + eptString + "_" + detName + "_" + zString + "_PosTrigInHalf_L1L3.dat", recoMassPosTrigInHalfLyr1Lyr3);
        outputFile(outDir + typeString + eptString + "_" + detName + "_" + zString + "_PosTrigInHalf_L2L3.dat", recoMassPosTrigInHalfLyr2Lyr3);
        outputFile(outDir + typeString + eptString + "_" + detName + "_" + zString + "_PosTrigInHalf_L3L3.dat", recoMassPosTrigInHalfLyr3Lyr3);

//        outputFile(outDir + typeString + eptString + zString+ "_NoMax.dat", recoMassLyr1NoMax);     
//        outputFile(outDir + typeString + eptString + zString+ "_lyr50.dat", recoMassLyr50Full);
//        outputFile(outDir + typeString + eptString + zString+ "_generated.dat", genMass);
        aida.saveAs(outDir + typeString + eptString + "_" + detName + "_" + zString + "_Plots.root");
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
            if (icross % 1000 == 0)
                System.out.println("On event number " + icross);
            Hep3Vector beamVec
                    = new BasicHep3Vector(sigx * generator.nextGaussian() + _xoff,
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

    private static void readLHEEvent(StreamTokenizer tok, double[] beam, int nevhep) throws IOException {
        Random generator = new Random();
        getToNextEvent(tok);
        List<Double> nums = getNumbersInLine(tok);

        if (nums.size() != 6)
            throw new RuntimeException("Unexpected entry for number of particles");
        int nhep = nums.get(0).intValue();
//        System.out.println("Number of particles for event " + nevhep + ": " + nhep);

//        List<Boolean> passEleTarget50 = new ArrayList<Boolean>();
//        List<Boolean> passPosTarget50 = new ArrayList<Boolean>();
//        List<Boolean> passEleNoMax = new ArrayList<Boolean>();
//        List<Boolean> passPosNoMax = new ArrayList<Boolean>();
        List<Boolean> passEleFull = new ArrayList<Boolean>();
        List<Boolean> passPosFull = new ArrayList<Boolean>();
        List<Boolean> passRecFull = new ArrayList<Boolean>();
        Boolean passEleEcal = false;
        Boolean passPosEcal = false;
        Boolean passRecEcal = false;
        Boolean passEleEcalInHalf = false;
        Boolean passPosEcalInHalf = false;
        Boolean passRecEcalInHalf = false;

        //      List<Boolean> passRecoilTarget50 = new ArrayList<Boolean>();
        //      List<Boolean> passRecoilNoMax = new ArrayList<Boolean>();
        //      List<Boolean> passRecoilFull = new ArrayList<Boolean>();
        //Hep3Vector pEle = new BasicHep3Vector();
        //Hep3Vector pPos = new BasicHep3Vector();
        //Hep3Vector pRecoil = new BasicHep3Vector();
        HepLorentzVector pEle = new BasicHepLorentzVector();
        HepLorentzVector pPos = new BasicHepLorentzVector();
        HepLorentzVector pRecoil = new BasicHepLorentzVector();
        double[] atEcalEle = {-999, -999, -999};
        double[] atEcalPos = {-999, -999, -999};
        double[] atEcalRec = {-999, -999, -999};

        int i = 0;
        int pdgid = 0;
        double[] ApMom = {0, 0, 0};
        double mom[] = {0, 0, 0};
        double ene = 0;
        int charge;
        boolean foundRecoil = false;
        boolean foundElectron = false;
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
                if ((Math.abs(pdgid) == 611 || (Math.abs(pdgid) == 11) && vals.get(1).intValue() == 1)) {
//                if (Math.abs(pdgid) == 611) {
                    //                  System.out.println("Ok...getting info for this particle");
                    for (int j = 0; j < 3; j++)
                        mom[j] = vals.get(j + 6);
//                    System.out.println("Mom(x/y/z) = ("+mom[0]+","+mom[1]+","
//                            + ""+mom[2]+")");

                    ene = vals.get(10);
                    Hep3Vector p = rotate(rotateToBeam(new BasicHep3Vector(mom[0], mom[1], mom[2])));
                    //                   Hep3Vector p = rotate(mom[1], mom[0], mom[2]); //flip x,y because my trident files have cut on thetaX that may bias things
                    Hep3Vector o = rotate(rotateToBeam(new BasicHep3Vector(beam[0], beam[1], beam[2])));
                    if (debug)
                        System.out.println("P(x/y/z) = (" + p.x() + "," + p.y() + ","
                                + "" + p.z() + ")");
                    if (debug)
                        System.out.println("o(x/y/z) = (" + o.x() + "," + o.y() + ","
                                + "" + o.z() + ")");

                    charge = 1;
                    if (pdgid == 611)
                        charge = -1;
                    HepLorentzVector pl = new BasicHepLorentzVector(ene, p);
                    HelixParamCalculator hpc = new HelixParamCalculator(p, o, -charge, -B_FIELD * bFieldMultiplier);

                    double[] trackParameters = new double[5];
                    Track trkFromMCP = new BaseTrack();
                    trackParameters[BaseTrack.D0] = hpc.getDCA();
                    trackParameters[BaseTrack.OMEGA] = hpc.getMCOmega();
                    trackParameters[BaseTrack.PHI] = hpc.getPhi0();
                    trackParameters[BaseTrack.TANLAMBDA] = hpc.getSlopeSZPlane();
                    trackParameters[BaseTrack.Z0] = hpc.getZ0();

                    if (debug)
                        System.out.println("d0=" + trackParameters[BaseTrack.D0]
                                + "; omega=" + trackParameters[BaseTrack.OMEGA]
                                + "; phi=" + trackParameters[BaseTrack.PHI]
                                + "; TanLambda=" + trackParameters[BaseTrack.TANLAMBDA]
                                + "; z0=" + trackParameters[BaseTrack.Z0]);
                    ((BaseTrack) trkFromMCP).setTrackParameters(trackParameters, B_FIELD);

                    Boolean passEcal = false;
                    Boolean passEcalInHalf = false;
                    double[] atEcal = {-999, -999, -999};
                    if (mom[2] > _pCut) {
                        TrackState state = TrackUtils.extrapolateTrackUsingFieldMap(trkFromMCP, extStartPos, ecalPosition, stepSize, bFieldMap); 
                        atEcal = state.getReferencePoint();
                        trkFromMCP.getTrackStates().add(state);

                        passEcal = inFiducialRegion(atEcal[1], atEcal[2], 0);
                        passEcalInHalf = inFiducialRegion(atEcal[1], atEcal[2], -halfCrystal);
                        if (debug)
                            System.out.println("X at ECal = " + atEcal[1] + "; Y at ECal = " + atEcal[2] + "; PassEcal = " + passEcal + ";  PassEcalInHalf = " + passEcalInHalf);
                        if (Double.isNaN(atEcal[1]))
                            System.out.println("Projection to ECal is NaN...momentum = " + mom[2]);
                    }
                    List<Boolean> passLayerFull = new ArrayList<Boolean>();
                    for (int ii = 0; ii < nLayers; ii++) {
                        boolean inAcceptance = isWithinAcceptance(trkFromMCP, ii + 1);
                        passLayerFull.add(inAcceptance);
                    }

                    if (pdgid == 611) {  //electron from A'
                        passEleFull = passLayerFull;
                        passEleEcal = passEcal;
                        passEleEcalInHalf = passEcalInHalf;
                        pEle = pl;
                        atEcalEle = atEcal;
                        foundElectron = true;
                    } else if (pdgid == -611 || pdgid == -11) {//positron
                        pPos = pl;
                        passPosEcal = passEcal;
                        passPosEcalInHalf = passEcalInHalf;
                        passPosFull = passLayerFull;
                        atEcalPos = atEcal;
                    } else if (pdgid == 11)
                        if (!foundElectron && trident) {
//                            System.out.println("This is a trident????");
                            foundElectron = true;
                            passEleFull = passLayerFull;
                            passEleEcal = passEcal;
                            pEle = pl;
                            atEcalEle = atEcal;
                        }
                    if (!foundRecoil) {
                        foundRecoil = true;
                        pRecoil = pl;
                        passRecFull = passLayerFull;
                        passRecEcal = passEcal;
                        passRecEcalInHalf = passEcalInHalf;
                        atEcalRec = atEcal;
                    }
                }

                i++;
            }
        }

//        if (!passEleFull.get(0) && passEleFull.get(1))
//            System.out.println("Electron Missed Layer 1 but hit layer 2");
//         if (!passPosFull.get(0) && passPosFull.get(1))
//            System.out.println("Positron Missed Layer 1 but hit layer 2");
        double invMass = getInvMass(pEle.v3(), pPos.v3());
        //       doAccounting(passEleTarget50, passPosTarget50, passEleNoMax, passPosNoMax, passEleFull, passPosFull, eventPass(pEle.v3(), pPos.v3()), 1000.0 * invMass);
        doAccounting(passEleFull, passPosFull, eventPass(pEle.v3(), pPos.v3()), passEleEcal, passPosEcal, passEleEcalInHalf, passPosEcalInHalf, pEle, pPos, atEcalEle, atEcalPos);
        if (trident)
            doAccounting(passRecFull, passPosFull, eventPass(pRecoil.v3(), pPos.v3()), passRecEcal, passPosEcal, passRecEcalInHalf, passPosEcalInHalf, pRecoil, pPos, atEcalRec, atEcalPos);
        //       doAccounting(passRecoil, passPos, passRecoilFull, passPosFull, eventPass(pRecoil, pPos), 1000.0 * getInvMass(pRecoil, pPos));
    }

    public static Hep3Vector rotate(double x, double y, double z) {
        return new BasicHep3Vector(z, x, y);
    }

    public static Hep3Vector rotate(Hep3Vector vec) {
        return new BasicHep3Vector(vec.z(), vec.x(), vec.y());
    }

    public static String convertDecimal(String num) {
        if (num.contains("."))
            num = num.replace(".", "pt");
        return num;
    }
//
//    public static boolean inAcceptance(Hep3Vector position, double yExt, double zExt, double zGap) {
//        double ypos = position.y();
//        if (Math.abs(ypos) > yExt)
//            return false;
//        double zpos = position.z();
//        if (Math.abs(zpos) < zGap)
//            return false;
//        if (Math.abs(zpos) > zGap + zExt)
//            return false;
//
//        return true;
//    }

    public static boolean eventPass(Hep3Vector p1, Hep3Vector p2) {
        if (debug)
            System.out.println("p1.magnitude = " + p1.magnitude() + "; p2.magnitude = " + p2.magnitude() + "; 0.8*_ecm = " + 0.8 * _ecm + "; p1.z = " + p1.z() + "; p2.z = " + p2.z());

        if (p1.magnitude() + p2.magnitude() < 0.8 * _ecm)//trigger requires 80% of beam energy...WAIT, this isn't true anymore!  Anyway, for this we want stuff with raditive cut so this is ok

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

    /**
     * Extrapolate a track to a layer and check that it lies within its
     * acceptance.
     *
     * @param track The track that will be extrapolated to the layer of interest
     * @param layer The layer number to extrapolate to
     * @return true if the track lies within the sensor acceptance, false
     * otherwise
     */
    private static boolean isWithinAcceptance(Track track, int layer) {

        // TODO: Move this to a utility class 
        //System.out.println("Retrieving sensors for layer: " + layer);
        // Since TrackUtils.isTop/BottomTrack does not work when running off 
        // a recon file, get the detector volume that a track is associated 
        // with by using the sensor.  This assumes that a track is always
        // composed by stereo hits that lie within a single detector volume
        //HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit)track.getTrackerHits().get(0).getRawHits().get(0)).getDetectorElement();
        boolean isTop = true;
        if (track.getTrackStates().get(0).getTanLambda() < 0)
            isTop = false;
        // Get the sensors associated with the layer that the track
        // will be extrapolated to
        List<SvtStereoLayer> stereoLayers = null;

        // if (TrackUtils.isTopTrack(track, track.getTrackerHits().size())) {
//        System.out.println("layer = "+layer+" isTop?"+isTop);
        if (isTop)
            //System.out.println("Top track found.");
            stereoLayers = topStereoLayers.get(layer); //} else if (TrackUtils.isBottomTrack(track, track.getTrackerHits().size())) {
        else
            //System.out.println("Bottom track found.");
            stereoLayers = bottomStereoLayers.get(layer);

        for (SvtStereoLayer stereoLayer : stereoLayers) {
            Hep3Vector axialSensorPosition = stereoLayer.getAxialSensor().getGeometry().getPosition();
            Hep3Vector stereoSensorPosition = stereoLayer.getStereoSensor().getGeometry().getPosition();

            //System.out.println("Axial sensor position: " + axialSensorPosition.toString());
            //System.out.println("Stereo sensor position: " + stereoSensorPosition.toString());
            Hep3Vector axialTrackPos = TrackUtils.extrapolateTrack(track, axialSensorPosition.z());
            Hep3Vector stereoTrackPos = TrackUtils.extrapolateTrack(track, stereoSensorPosition.z());
//            LOGGER.info("track position on axial sensor:  " + axialTrackPos.x() + ", " + axialTrackPos.y() + ", " + axialTrackPos.z());
//            LOGGER.info("track position on stero sensor:  " + stereoTrackPos.x() + ", " + stereoTrackPos.y() + ", " + stereoTrackPos.z());
            //System.out.println("Track position at axial sensor: " + axialTrackPos.toString());
            //System.out.println("Track position at stereo sensor: " + stereoTrackPos.toString());
            boolean inAxial = sensorContainsTrack(axialTrackPos, stereoLayer.getAxialSensor());
            boolean inStereo = sensorContainsTrack(stereoTrackPos, stereoLayer.getStereoSensor());
//            LOGGER.info("in Axial = " + inAxial + "; in Stereo = " + inStereo);
            if (inAxial
                    && inStereo)
                //System.out.println("Track lies within layer acceptance.");
                return true;
        }

        return false;

        /*int layerNumber = (layer - 1)/2 + 1;
         String title = "Track Position - Layer " + layerNumber + " - Tracks Within Acceptance";
         //aida.histogram2D(title).fill(trackPos.y(), trackPos.z());
         //aida.cloud2D(title).fill(frontTrackPos.y(), frontTrackPos.z()); */
    }

    public static boolean sensorContainsTrack(Hep3Vector trackPosition, SiSensor sensor) {

//        if(maskBadChannels){
//            int intersectingChannel = this.findIntersectingChannel(trackPosition, sensor);
//            if(intersectingChannel == 0 || intersectingChannel == 638) return false;
//
//            if(sensor.isBadChannel(intersectingChannel) 
//                    || sensor.isBadChannel(intersectingChannel+1) 
//                    || sensor.isBadChannel(intersectingChannel-1)){
//                //this.printDebug("Track intersects a bad channel!");
//                return false;
//                    }
//        }
        ITransform3D localToGlobal = sensor.getGeometry().getLocalToGlobal();

        Hep3Vector sensorPos = sensor.getGeometry().getPosition();
        Box sensorSolid = (Box) sensor.getGeometry().getLogicalVolume().getSolid();
        Polygon3D sensorFace = sensorSolid.getFacesNormalTo(new BasicHep3Vector(0, 0, 1)).get(0);

        List<Point3D> vertices = new ArrayList<Point3D>();
        for (int index = 0; index < 4; index++)
            vertices.add(new Point3D());
        for (Point3D vertex : sensorFace.getVertices())
            if (vertex.y() < 0 && vertex.x() > 0) {
                localToGlobal.transform(vertex);
                //vertices.set(0, new Point3D(vertex.y() + sensorPos.x(), vertex.x() + sensorPos.y(), vertex.z() + sensorPos.z()));
                vertices.set(0, new Point3D(vertex.x(), vertex.y(), vertex.z()));
                //System.out.println(this.getClass().getSimpleName() + ": Vertex 1 Position: " + vertices.get(0).toString());
                //System.out.println(this.getClass().getSimpleName() + ": Transformed Vertex 1 Position: " + localToGlobal.transformed(vertex).toString());
            } else if (vertex.y() > 0 && vertex.x() > 0) {
                localToGlobal.transform(vertex);
                //vertices.set(1, new Point3D(vertex.y() + sensorPos.x(), vertex.x() + sensorPos.y(), vertex.z() + sensorPos.z()));
                vertices.set(1, new Point3D(vertex.x(), vertex.y(), vertex.z()));
                //System.out.println(this.getClass().getSimpleName() + ": Vertex 2 Position: " + vertices.get(1).toString());
                //System.out.println(this.getClass().getSimpleName() + ": Transformed Vertex 2 Position: " + localToGlobal.transformed(vertex).toString());
            } else if (vertex.y() > 0 && vertex.x() < 0) {
                localToGlobal.transform(vertex);
                //vertices.set(2, new Point3D(vertex.y() + sensorPos.x(), vertex.x() + sensorPos.y(), vertex.z() + sensorPos.z()));
                vertices.set(2, new Point3D(vertex.x(), vertex.y(), vertex.z()));
                //System.out.println(this.getClass().getSimpleName() + ": Vertex 3 Position: " + vertices.get(2).toString());
                //System.out.println(this.getClass().getSimpleName() + ": Transformed Vertex 3 Position: " + localToGlobal.transformed(vertex).toString());
            } else if (vertex.y() < 0 && vertex.x() < 0) {
                localToGlobal.transform(vertex);
                //vertices.set(3, new Point3D(vertex.y() + sensorPos.x(), vertex.x() + sensorPos.y(), vertex.z() + sensorPos.z()));
                vertices.set(3, new Point3D(vertex.x(), vertex.y(), vertex.z()));
                //System.out.println(this.getClass().getSimpleName() + ": Vertex 4 Position: " + vertices.get(3).toString());
                //System.out.println(this.getClass().getSimpleName() + ": Transformed Vertex 4 Position: " + localToGlobal.transformed(vertex).toString());
            }
        /*
         double area1 = this.findTriangleArea(vertices.get(0).x(), vertices.get(0).y(), vertices.get(1).x(), vertices.get(1).y(), trackPosition.y(), trackPosition.z()); 
         double area2 = this.findTriangleArea(vertices.get(1).x(), vertices.get(1).y(), vertices.get(2).x(), vertices.get(2).y(), trackPosition.y(), trackPosition.z()); 
         double area3 = this.findTriangleArea(vertices.get(2).x(), vertices.get(2).y(), vertices.get(3).x(), vertices.get(3).y(), trackPosition.y(), trackPosition.z()); 
         double area4 = this.findTriangleArea(vertices.get(3).x(), vertices.get(3).y(), vertices.get(0).x(), vertices.get(0).y(), trackPosition.y(), trackPosition.z()); 
         */

        double area1 = findTriangleArea(vertices.get(0).x(), vertices.get(0).y(), vertices.get(1).x(), vertices.get(1).y(), trackPosition.x(), trackPosition.y());
        double area2 = findTriangleArea(vertices.get(1).x(), vertices.get(1).y(), vertices.get(2).x(), vertices.get(2).y(), trackPosition.x(), trackPosition.y());
        double area3 = findTriangleArea(vertices.get(2).x(), vertices.get(2).y(), vertices.get(3).x(), vertices.get(3).y(), trackPosition.x(), trackPosition.y());
        double area4 = findTriangleArea(vertices.get(3).x(), vertices.get(3).y(), vertices.get(0).x(), vertices.get(0).y(), trackPosition.x(), trackPosition.y());

        if ((area1 > 0 && area2 > 0 && area3 > 0 && area4 > 0) || (area1 < 0 && area2 < 0 && area3 < 0 && area4 < 0))
            return true;
        return false;

    }

    /**
     *
     */
    public static double findTriangleArea(double x0, double y0, double x1, double y1, double x2, double y2) {
        return .5 * (x1 * y2 - y1 * x2 - x0 * y2 + y0 * x2 + x0 * y1 - y0 * x1);
    }

//    public int getLayerFromPosition(double posZ) {
//        if (posZ < 150)
//            return 1;
//        if (posZ < 250)
//            return 2;
//        if (posZ < 350)
//            return 3;
//        if (posZ < 550)
//            return 4;
//        if (posZ < 750)
//            return 5;
//        if (posZ < 950)
//            return 6;
//        return 666;
//    }
    private static boolean inFiducialRegion(double x, double y, double offsetY) {
        boolean in_fid = false;
//        double x_edge_low = -262.74;
//        double x_edge_high = 347.7;
//        double y_edge_low = 33.54;
//        double y_edge_high = 75.18;
//
//        double x_gap_low = -106.66;
//        double x_gap_high = 42.17;
//        double y_gap_high = 47.18;

//        y_edge_low += offsetY;
//        y_edge_high += offsetY;
//        y_gap_high += offsetY;
        y = Math.abs(y);

        if (x > x_edge_low + shiftECalinX && x < x_edge_high + shiftECalinX && y > y_edge_low + offsetY && y < y_edge_high + offsetY)
            if ((x > x_gap_low + shiftECalinX && x < x_gap_high + shiftECalinX && y > y_edge_low + offsetY && y < y_gap_high + offsetY) != true)
                in_fid = true;
        return in_fid;
    }

    private static Hep3Vector rotateToBeam(Hep3Vector vec) {
        BasicHep3Matrix tmp = new BasicHep3Matrix();
        tmp.setElement(0, 0, Math.cos(beamAngle));
        tmp.setElement(0, 2, Math.sin(beamAngle));
        tmp.setElement(1, 1, 1);
        tmp.setElement(2, 2, Math.cos(beamAngle));
        tmp.setElement(2, 0, -Math.sin(beamAngle));
        Transform3D trans = new Transform3D(new Rotation3D(tmp));
        return trans.transformed(vec);
    }

}
