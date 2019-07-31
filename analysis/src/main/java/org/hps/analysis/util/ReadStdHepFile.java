package org.hps.analysis.util;

/**
 *
 * @author mgraham borrowed liberally from hep.lcio.util.StdhepConverter
 * created on 7/27/2019
 */
import hep.aida.IHistogram1D;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import static java.lang.Math.abs;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
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
import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.TrackUtils;
import org.jdom.JDOMException;
import org.lcsim.detector.converter.compact.subdetector.HpsTracker2;
import org.lcsim.detector.converter.compact.subdetector.SvtStereoLayer;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.MCParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.base.BaseMCParticle;
import org.lcsim.event.base.BaseTrack;
import org.lcsim.fit.helicaltrack.HelixParamCalculator;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.FieldMap;
import org.lcsim.geometry.GeometryReader;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.xml.ElementFactory;

public class ReadStdHepFile {

    static private List<SiSensor> sensors = null;
    static private List<EcalCrystal> crystals = null;
    static private Map<Integer, List<SvtStereoLayer>> topStereoLayers = new HashMap<Integer, List<SvtStereoLayer>>();
    static private Map<Integer, List<SvtStereoLayer>> bottomStereoLayers = new HashMap<Integer, List<SvtStereoLayer>>();

    static private boolean debug = false;
    private static final String SUBDETECTOR_NAME = "Tracker";
    static int nLayers = 7;  //for L0 studies
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

    static StdhepReader sr;
    static int nmax = 500000;
    private static final double c_light = 2.99792458e+8;
    private static ParticlePropertyProvider ppp = ParticlePropertyManager.getParticlePropertyProvider();
    private static Detector detector = null;

    static String ECAL_POSITION_CONSTANT_NAME = "ecal_dface";
    static double ecalPosition; // mm...this get's read out from the detector

    // these numbers will get overwritten from the true geometry from compact.xml conversion!!!
    private static double halfCrystal = 13.3 / 2;//size at the front
    private static double x_edge_low = -262.74 - halfCrystal;
    private static double x_edge_high = 347.7 + halfCrystal;
    private static double y_edge_low = 33.54 - halfCrystal;
    private static double y_edge_high = 75.18 + halfCrystal;

    private static double x_gap_low = -106.66 - halfCrystal;
    private static double x_gap_high = 42.17 + halfCrystal;
    private static double y_gap_high = 47.18 - halfCrystal;

    private static double shiftECalinX = 0; //amount to shift ecal in (detector) X...settable via command line
    private static double bFieldMultiplier = 1;  //scaling factor for b-field...settable via command line
    private static double B_FIELD = 0.23;//Tesla

    static double maxMass = 500.0;
    static double _pCut = 0.1;
    static double _ecm = 4.4; //GeV

    private static int minLayerHits = 5;
    /**
     * The B field map
     */
    static FieldMap bFieldMap = null;
    /**
     * Z position to start extrapolation from
     */
    static double extStartPos = 700; // mm
    /**
     * The extrapolation step size
     */
    static double stepSize = 5.0; // mm

    static int nbinsH = 50;
    static protected AIDA aida = AIDA.defaultInstance();
    static IHistogram1D esumPassGen = aida.histogram1D("MadGraph Generated ESum (GeV)", nbinsH, 0.5 * _ecm, 1.1 * _ecm);
    static IHistogram1D esumPassECal = aida.histogram1D("MadGraph Pass ECal ESum (GeV)", nbinsH, 0.5 * _ecm, 1.1 * _ecm);
    static IHistogram1D pPosPassGen = aida.histogram1D("MadGraph Generated pPos (GeV)", nbinsH, 0, 1.1 * _ecm);
    static IHistogram1D pPosPassECal = aida.histogram1D("MadGraph Pass ECal pPos (GeV)", nbinsH, 0, 1.1 * _ecm);
    static IHistogram1D pPosPassECalAndMinLayers = aida.histogram1D("MadGraph Pass ECal+Layers pPos (GeV)", nbinsH, 0, 1.1 * _ecm);
    static IHistogram1D pPosPassECalAndPosMinLayersAndEleMinLayers = aida.histogram1D("MadGraph Pass ECal+Layers+EleLayers pPos (GeV)", nbinsH, 0, 1.1 * _ecm);

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
        } catch (ParseException e) {
            throw new RuntimeException("Problem parsing command line options.", e);
        }

//        int nInFiles = 99;
        int nInFiles = 1;
        int iStart = 100;
        double apMass = 0.08;
        _ecm = 2.2;
        _declength = 0;//mm
        sigx = 0.02;
        sigy = 0.2;
        String detName = "Foobar";

        if (cl.hasOption("d")) {
            detName = cl.getOptionValue("d");
            System.out.println("Using detector = " + detName);
        }

        //Read in the compact and get detector geometry information
        File compact = new File("/Users/mgraham/hps/Run2019Master/detector-data/detectors/" + detName + "/compact.xml");
        getDetector(compact);
        //

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
        aida.saveAs(fileLabel + "_Plots.root");
    }

    private static void openStdHepFile(String infile) throws IOException {
        sr = new StdhepReader(infile);
    }

    private static int process() throws IOException {
        Random generator = new Random();
        int nread = 0;

        List<Boolean> passEleFull = new ArrayList<Boolean>();
        List<Boolean> passPosFull = new ArrayList<Boolean>();
        List<Boolean> passRecFull = new ArrayList<Boolean>();
        Boolean passEleEcal = false;
        Boolean passPosEcal = false;
        Boolean passRecEcal = false;
        Boolean passEleEcalInHalf = false;
        Boolean passPosEcalInHalf = false;
        Boolean passRecEcalInHalf = false;

        HepLorentzVector pEle = new BasicHepLorentzVector();
        HepLorentzVector pPos = new BasicHepLorentzVector();
        HepLorentzVector pRecoil = new BasicHepLorentzVector();
        double[] atEcalEle = {-999, -999, -999};
        double[] atEcalPos = {-999, -999, -999};
        double[] atEcalRec = {-999, -999, -999};

        try {
            // Loop over all records in the Stdhep file.
            for (;;) {
                boolean foundRecoil = false;
                boolean foundElectron = false;

                // Get the next Stdhep event.
                StdhepRecord record = sr.nextRecord();
                // Only process StdhepEvent records.

                if (record instanceof StdhepEvent) {
                    // Convert to an LCCollection of MCParticle objects.
                    List<MCParticle> mcpcoll = convert((StdhepEvent) record);
                    for (MCParticle mcp : mcpcoll) {
                        int pdgid = mcp.getPDGID();
                        int status = mcp.getGeneratorStatus();
                        System.out.println("PDG ID = " + pdgid);
                        if ((abs(pdgid) == 611 || abs(pdgid) == 11) && status == 1) {
                            double ene = mcp.getEnergy();
                            Hep3Vector p = mcp.getMomentum();
                            Hep3Vector o = mcp.getOrigin();
                            if (debug)
                                System.out.println("P(x/y/z) = (" + p.x() + "," + p.y() + ","
                                        + "" + p.z() + ")");
                            if (debug)
                                System.out.println("o(x/y/z) = (" + o.x() + "," + o.y() + ","
                                        + "" + o.z() + ")");
                            int charge = 1;
                            if (pdgid == 611)
                                charge = -1;
                            HepLorentzVector pl = new BasicHepLorentzVector(ene, p);
                            Hep3Vector pRotated = CoordinateTransformations.transformVectorToTracking(p);
                            Hep3Vector oRotated = CoordinateTransformations.transformVectorToTracking(o);
                            HelixParamCalculator hpc = new HelixParamCalculator(pRotated, oRotated, -charge, -B_FIELD);
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
                            double[] atEcal = {-999, -999, -999};
                            if (p.z() > _pCut) {
                                TrackState state = TrackUtils.extrapolateTrackUsingFieldMap(trkFromMCP, extStartPos, ecalPosition, stepSize, bFieldMap);
                                atEcal = state.getReferencePoint();
                                trkFromMCP.getTrackStates().add(state);

                                passEcal = inFiducialRegion(atEcal[1], atEcal[2], 0);
                                if (debug)
                                    System.out.println("X at ECal = " + atEcal[1] + "; Y at ECal = " + atEcal[2] + "; PassEcal = " + passEcal);
                                if (Double.isNaN(atEcal[1]))
                                    System.out.println("Projection to ECal is NaN...momentum = " + p.z());
                            }

                            List<Boolean> passLayerFull = new ArrayList<Boolean>();
                            for (int ii = 0; ii < nLayers; ii++) {
                                boolean inAcceptance = isWithinAcceptance(trkFromMCP, ii + 1);
                                passLayerFull.add(inAcceptance);
                            }

                            if (pdgid == 611) {  //electron from A'
                                passEleFull = passLayerFull;
                                passEleEcal = passEcal;
                                pEle = pl;
                                atEcalEle = atEcal;
                                foundElectron = true;
                            } else if (pdgid == -611 || pdgid == -11) {//positron
                                pPos = pl;
                                passPosEcal = passEcal;
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
                                atEcalRec = atEcal;
                            }
                        }
                    }
                    boolean passEleMinLayers = false;
                    boolean passPosMinLayers = false;
                    boolean passRecMinLayers = false;
                    int nlayPass = 0;
                    for (boolean pass : passEleFull)
                        if (pass)
                            nlayPass++;
                    if (nlayPass >= minLayerHits)
                        passEleMinLayers = true;

                    nlayPass = 0;
                    for (boolean pass : passPosFull)
                        if (pass)
                            nlayPass++;
                    if (nlayPass >= minLayerHits)
                        passPosMinLayers = true;
                    System.out.println("Number of layers hit: positron = " + nlayPass);
                    nlayPass = 0;
                    for (boolean pass : passRecFull)
                        if (pass)
                            nlayPass++;
                    if (nlayPass >= minLayerHits)
                        passRecMinLayers = true;

                    pPosPassGen.fill(pPos.v3().magnitude());
                    if (passPosEcal)
                        pPosPassECal.fill(pPos.v3().magnitude());
                    if (passPosEcal && passPosMinLayers)
                        pPosPassECalAndMinLayers.fill(pPos.v3().magnitude());
                    if((passEleMinLayers||passRecMinLayers)&&passPosEcal && passPosMinLayers)
                         pPosPassECalAndPosMinLayersAndEleMinLayers.fill(pPos.v3().magnitude());
                    if (passPosEcal && !passPosMinLayers)
                        System.out.println("Passed ECal but not passPosMinLayers");
                    if (!passPosEcal && passPosMinLayers)
                        System.out.println("Passed passPosMinLayers but not ECal");

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
    public static void getDetector(File compact) throws FileNotFoundException, IOException {
        FileInputStream fis = new FileInputStream(compact);
        GeometryReader reader = new GeometryReader();
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
            boolean inAxial = TrackUtils.sensorContainsTrack(axialTrackPos, stereoLayer.getAxialSensor());
            boolean inStereo = TrackUtils.sensorContainsTrack(stereoTrackPos, stereoLayer.getStereoSensor());
//            LOGGER.info("in Axial = " + inAxial + "; in Stereo = " + inStereo);
            if (inAxial && inStereo) {
                System.out.println("Track lies within layer acceptance.");
                return true;
            }
        }

        return false;

        /*int layerNumber = (layer - 1)/2 + 1;
         String title = "Track Position - Layer " + layerNumber + " - Tracks Within Acceptance";
         //aida.histogram2D(title).fill(trackPos.y(), trackPos.z());
         //aida.cloud2D(title).fill(frontTrackPos.y(), frontTrackPos.z()); */
    }

    private static boolean inFiducialRegion(double x, double y, double offsetY) {
        boolean in_fid = false;

        y = Math.abs(y);

        if (x > x_edge_low + shiftECalinX && x < x_edge_high + shiftECalinX && y > y_edge_low + offsetY && y < y_edge_high + offsetY)
            if ((x > x_gap_low + shiftECalinX && x < x_gap_high + shiftECalinX && y > y_edge_low + offsetY && y < y_gap_high + offsetY) != true)
                in_fid = true;
        return in_fid;
    }

}
