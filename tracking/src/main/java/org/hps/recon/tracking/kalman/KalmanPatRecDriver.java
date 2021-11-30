package org.hps.recon.tracking.kalman;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import hep.physics.vec.Hep3Vector;
import hep.physics.vec.BasicHep3Vector;

import org.hps.conditions.beam.BeamPosition;
import org.hps.conditions.beam.BeamPosition.BeamPositionCollection;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.MaterialSupervisor;
import org.hps.recon.tracking.TrackData;
import org.hps.recon.tracking.TrackResidualsData;
import org.hps.recon.tracking.MaterialSupervisor.ScatteringDetectorVolume;
import org.hps.recon.tracking.MaterialSupervisor.SiStripPlane;
import org.hps.recon.tracking.gbl.GBLStripClusterData;
import org.hps.util.Pair;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseLCRelation;
import org.lcsim.geometry.Detector;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

import org.lcsim.geometry.IDDecoder;

/**
 * Driver for pattern recognition and fitting of HPS tracks using the Kalman
 * Filter
 */
public class KalmanPatRecDriver extends Driver {

    IDDecoder decoder;
    private ArrayList<SiStripPlane> detPlanes;
    private MaterialSupervisor _materialManager;
    private org.lcsim.geometry.FieldMap fm;
    private KalmanInterface KI;
    private boolean verbose = false;
    private boolean uniformB = false;
    private String outputFullTrackCollectionName = "KalmanFullTracks";
    public AIDA aida;
    private int nTracks;
    private int nEvents;
    private double executionTime;
    private double maxTime;
    private double interfaceTime;
    private double plottingTime;
    private KalmanParams kPar;
    private KalmanPatRecPlots kPlot;
    private static Logger logger;

    // Parameters for the Kalman pattern recognition that can be set by the user in the steering file:
    private ArrayList<String> strategies;     // List of seed strategies for both top and bottom trackers, from steering
    private ArrayList<String> strategiesTop;  // List of all the top tracker seed strategies from the steering file
    private ArrayList<String> strategiesBot;  // List of all the bottom tracker seed strategies from the steering file
    private int numPatRecIteration;    // Number of global iterations of the pattern recognition
    private int numKalmanIteration;    // Number of Kalman filter iterations per track in the final fit
    private double maxPtInverse;       // Maximum value of 1/pt for the seed and the final track
    private double maxD0;              // Maximum dRho (or D0) at the target plane for a seed and the final track
    private double maxZ0;              // Maximum dz (or Z0) at the target plane for a seed and the final track
    private double maxChi2;            // Maximum Kalman chi^2 per hit for a track candidate
    private int minHits;               // Minimum number of hits on a track
    private int minStereo;             // Minimum number of stereo hits on a track
    private int maxSharedHits;         // Maximum number of hits on a track that are shared with another track
    private double maxTimeRange;       // Maximum time range in ns spanned by all the hits on a track
    private double maxTanLambda;       // Maximum tan(lambda) for a track seed
    private double maxResidual;        // Maximum residual in units of SSD resolution to add a hit to a track candidate
    private double maxChi2Inc;         // Maximum increment in chi^2 to add a hit to an already completed track
    private double minChi2IncBad;      // Minimum increment in chi^2 to remove a hit from an already completed track
    private double maxResidShare;      // Maximum residual in units of detector resolution for a shared hit
    private double maxChi2IncShare;    // Maximum increment in chi^2 for a hit shared with another track
    private double mxChi2Vtx;          // Maximum chi^2 for 5-hit tracks with a vertex constraint
    private int numEvtPlots;           // Number of event displays to plot (gnuplot files)
    private boolean doDebugPlots;      // Whether to make all the debugging histograms 
    private int siHitsLimit;           // Maximum number of SiClusters in one event allowed for KF pattern reco 
    // (protection against monster events) 
    private double seedCompThr;        // Threshold for seedTrack helix parameters compatibility
    private int numStrategyIter1;      // Number of seed strategies to use in the first iteration of pattern recognition
    private double beamPositionZ;      // Beam spot location along the beam axis
    private double beamSigmaZ;         // Beam spot size along the beam axis
    private double beamPositionX;
    private double beamSigmaX;
    private double beamPositionY;
    private double beamSigmaY;
    private double lowPhThresh;                 // Threshold in residual ratio to prefer a low-ph hit over a high-ph hit
    private double minSeedEnergy = -1.;           // Minimum energy of a hit for it to be used in a pattern recognition seed
    private boolean useBeamPositionConditions;  // True to use beam position from database
    private boolean useFixedVertexZPosition;    // True to override the database just for the z beam position
    private Level logLevel = Level.WARNING;     // Set log level from steering
    private boolean addResiduals;               // If true add the hit-on-track residuals to the LCIO event
    private List<HpsSiSensor> sensors = null;   // List of tracker sensors
    private String kalTrackCollectionName = "KalTracks";
    private String  kalTrackRelationName="KalTrackRelations";
    private boolean addKalTracks = false;
    

    public String getOutputFullTrackCollectionName() {
        return outputFullTrackCollectionName;
    }

    public void setOutputFullTrackCollectionName(String input) {
        outputFullTrackCollectionName = input;
    }

    public void setVerbose(boolean input) {
        verbose = input;
    }

    public void setUniformB(boolean input) {
        uniformB = input;
        logger.config("KalmanPatRecDriver: the B field will be assumed uniform.\n");
    }

    public void setMaterialManager(MaterialSupervisor mm) {
        _materialManager = mm;
    }

    public void setTrackCollectionName(String input) {
    }

    public void setSiHitsLimit(int input) {
        siHitsLimit = input;
    }

    public void setAddResiduals(boolean input) {
        addResiduals = input;
    }

    @Override
    public void detectorChanged(Detector det) {
        logger = Logger.getLogger(KalmanPatRecDriver.class.getName());
        if (logLevel != null) {
            logger.setLevel(logLevel);
            //LogManager.getLogManager().getLogger(Logger.GLOBAL_LOGGER_NAME).setLevel(logLevel);
        }
        verbose = (logger.getLevel() == Level.FINE);
        System.out.format("KalmanPatRecDriver: entering detectorChanged, logger level = %s\n", logger.getLevel().getName());
        executionTime = 0.;
        interfaceTime = 0.;
        plottingTime = 0.;
        maxTime = 0.;

        _materialManager = new MaterialSupervisor();
        _materialManager.buildModel(det);

        fm = det.getFieldMap();
        /*
        System.out.format("B field map vs y:\n");
        double eCalLoc = 1394.;
        for (double y=0.; y<1500.; y+=5.) {
            double z1=-50.; 
            double z2= 50.;
            Vec B1 = new Vec(3, KalmanInterface.getFielD(new Vec(0., y, z1), fm));
            Vec B2 = new Vec(3, KalmanInterface.getFielD(new Vec(0., y, 0.), fm));
            Vec B3 = new Vec(3, KalmanInterface.getFielD(new Vec(0., y, z2), fm));
            System.out.format("y=%6.1f z=%6.1f: %s z=0: %s z=%6.1f: %s\n", y, z1, B1.toString(), B2.toString(), z2, B3.toString());
        }
        System.out.format("B field map vs z at ECAL:\n");
        for (double z=-200.; z<200.; z+=5.) {
            double y=eCalLoc; 
            Vec B = new Vec(3, KalmanInterface.getFielD(new Vec(0., y, z), fm));
            System.out.format("x=0 y=%6.1f z=%6.1f: %s\n", y, z, B.toString());
        }
        System.out.format("B field map vs x at ECAL:\n");
        for (double x=-200.; x<200.; x+=5.) {
            double y=eCalLoc;
            double z=20.;
            Vec B = new Vec(3, KalmanInterface.getFielD(new Vec(x, y, z), fm));
            System.out.format("x=%6.1f y=%6.1f z=%6.1f: %s\n", x, y, z, B.toString());
        }
         */

        detPlanes = new ArrayList<SiStripPlane>();
        List<ScatteringDetectorVolume> materialVols = ((MaterialSupervisor) (_materialManager)).getMaterialVolumes();
        for (ScatteringDetectorVolume vol : materialVols) {
            detPlanes.add((SiStripPlane) (vol));
        }

        sensors = det.getSubdetector("Tracker").getDetectorElement().findDescendants(HpsSiSensor.class);

        // Instantiate the interface to the Kalman-Filter code and set up the geometry
        KalmanParams kPar = new KalmanParams();

        // Change Kalman parameters per settings supplied by the steering file
        // We assume that if not set by the steering file, then the parameters will have the Java default values for the primitives
        // Note that all of the parameters have defaults hard coded in KalmanParams.java
        if (numPatRecIteration != 0) {
            kPar.setGlbIterations(numPatRecIteration);
        }
        if (numKalmanIteration != 0) {
            kPar.setIterations(numKalmanIteration);
        }
        if (maxPtInverse != 0.0) {
            kPar.setMaxK(maxPtInverse);
        }
        if (maxD0 != 0.0) {
            kPar.setMaxdRho(maxD0);
        }
        if (maxZ0 != 0.0) {
            kPar.setMaxdZ(maxZ0);
        }
        if (maxChi2 != 0.0) {
            kPar.setMaxChi2(maxChi2);
        }
        if (minHits != 0) {
            kPar.setMinHits(minHits);
        }
        if (minStereo != 0) {
            kPar.setMinStereo(minStereo);
        }
        if (maxSharedHits != 0) {
            kPar.setMaxShared(maxSharedHits);
        }
        if (maxTimeRange != 0.0) {
            kPar.setMaxTimeRange(maxTimeRange);
        }
        if (maxTanLambda != 0.0) {
            kPar.setMaxTanL(maxTanLambda);
        }
        if (maxResidual != 0.0) {
            kPar.setMxResid(maxResidual);
        }
        if (maxChi2Inc != 0.0) {
            kPar.setMxChi2Inc(maxChi2Inc);
        }
        if (minChi2IncBad != 0.0) {
            kPar.setMinChi2IncBad(minChi2IncBad);
        }
        if (maxResidShare != 0.0) {
            kPar.setMxResidShare(maxResidShare);
        }
        if (maxChi2IncShare != 0.0) {
            kPar.setMxChi2double(maxChi2IncShare);
        }
        if (seedCompThr != 0.0) {
            kPar.setSeedCompThr(seedCompThr);
        }
        if (beamPositionZ != 0.0) {
            kPar.setBeamSpotY(beamPositionZ);
        }
        if (beamSigmaZ != 0.0) {
            kPar.setBeamSizeY(beamSigmaZ);
        }
        if (beamPositionX != 0.0) {
            kPar.setBeamSpotX(beamPositionX);
        }
        if (beamSigmaX != 0.0) {
            kPar.setBeamSizeX(beamSigmaX);
        }
        if (beamPositionY != 0.0) {
            kPar.setBeamSpotZ(-beamPositionY);
        }
        if (beamSigmaY != 0.0) {
            kPar.setBeamSizeZ(beamSigmaY);
        }
        if (mxChi2Vtx != 0.0) {
            kPar.setMaxChi2Vtx(mxChi2Vtx);
        }
        if (minSeedEnergy >= 0.) {
            kPar.setMinSeedEnergy(minSeedEnergy);
        }

        // Here we set the seed strategies for the pattern recognition
        if (strategies != null || (strategiesTop != null && strategiesBot != null)) {
            logger.config("The Kalman pattern recognition seed strategies are being set from the steering file");
            kPar.clrStrategies();
            int nB = 0;
            int nT = 0;
            int nA = 0;
            if (strategies != null) {
                nA = strategies.size();
                for (String strategy : strategies) {
                    kPar.addStrategy(strategy, "top");
                    kPar.addStrategy(strategy, "bottom");
                }
            }
            if (strategiesTop != null) {
                nT = strategiesTop.size();
                for (String strategy : strategiesTop) {
                    kPar.addStrategy(strategy, "top");
                }
            }
            if (strategiesBot != null) {
                nB = strategiesBot.size();
                for (String strategy : strategiesBot) {
                    kPar.addStrategy(strategy, "bottom");
                }
            }
            kPar.setNumSeedIter1(nA + nT);
            kPar.setNumSeedIter1(nA + nB);
        }
        if (numStrategyIter1 != 0) {
            kPar.setNumSeedIter1(numStrategyIter1);
        }

        // Setup optional usage of beam positions from database.
        final DatabaseConditionsManager mgr = DatabaseConditionsManager.getInstance();
        if (useBeamPositionConditions && mgr.hasConditionsRecord("beam_positions")) {
            logger.config("Using Kalman beam position from the conditions database");
            BeamPositionCollection beamPositions
                    = mgr.getCachedConditions(BeamPositionCollection.class, "beam_positions").getCachedData();
            BeamPosition beamPositionCond = beamPositions.get(0);
            if (!useFixedVertexZPosition) {
                kPar.setBeamSpotY(beamPositionCond.getPositionZ());
            } else {
                logger.config("Using fixed Kalman beam Z position: " + kPar.beamSpot[1]);
            }
            kPar.setBeamSpotX(beamPositionCond.getPositionX());   // Includes a transformation to Kalman coordinates
            kPar.setBeamSpotZ(-beamPositionCond.getPositionY());
        } else {
            logger.config("Using Kalman beam position from the steering file or default");
        }
        logger.config("Using Kalman beam position [ Z, X, Y ]: " + String.format("[ %f, %f, %f ]",
                kPar.beamSpot[0], -kPar.beamSpot[2], kPar.beamSpot[1]) + " in HPS coordinates.");

        logger.config(String.format("KalmanPatRecDriver: the B field is assumed uniform? %b\n", uniformB));
        logger.config("KalmanPatRecDriver: done with configuration changes.");
        kPar.print();

        KI = new KalmanInterface(uniformB, kPar, fm);
        KI.setSiHitsLimit(siHitsLimit);
        KI.createSiModules(detPlanes);
        decoder = det.getSubdetector("Tracker").getIDDecoder();
        if (doDebugPlots) {
            kPlot = new KalmanPatRecPlots(verbose, KI, decoder, numEvtPlots, fm);
        }
    }

    @Override
    public void process(EventHeader event) {

        List<Track> outputFullTracks = new ArrayList<Track>();
        List<KalTrack> kalTracks = new ArrayList<KalTrack>();
        //For additional track information
        List<TrackData> trackDataCollection = new ArrayList<TrackData>();
        List<LCRelation> trackDataRelations = new ArrayList<LCRelation>();
        List<LCRelation> kalTracksRelations = new ArrayList<LCRelation>();
        //For GBL Refitting
        List<GBLStripClusterData> allClstrs = new ArrayList<GBLStripClusterData>();
        List<LCRelation> gblStripClusterDataRelations = new ArrayList<LCRelation>();

        //For hit-on-track residuals information
        List<TrackResidualsData> trackResiduals = new ArrayList<TrackResidualsData>();
        List<LCRelation> trackResidualsRelations = new ArrayList<LCRelation>();

        ArrayList<KalTrack>[] kPatList = prepareTrackCollections(event, outputFullTracks, trackDataCollection, trackDataRelations, allClstrs, gblStripClusterDataRelations, trackResiduals, trackResidualsRelations, kalTracksRelations);

        int flag = 1 << LCIOConstants.TRBIT_HITS;
        event.put(outputFullTrackCollectionName, outputFullTracks, Track.class, flag);
        event.put("KFGBLStripClusterData", allClstrs, GBLStripClusterData.class, flag);
        event.put("KFGBLStripClusterDataRelations", gblStripClusterDataRelations, LCRelation.class, flag);
        event.put("KFTrackData", trackDataCollection, TrackData.class, 0);
        event.put("KFTrackDataRelations", trackDataRelations, LCRelation.class, 0);

        if (addResiduals) {
            event.put("KFUnbiasRes", trackResiduals, TrackResidualsData.class, 0);
            event.put("KFUnbiasResRelations", trackResidualsRelations, LCRelation.class, 0);
        }

        if (addKalTracks) {
            kalTracks = kPatList[0];
            kalTracks.addAll(kPatList[1]);
            event.put(kalTrackCollectionName, kalTracks, KalTrack.class, flag);
            event.put(kalTrackRelationName, kalTracksRelations, LCRelation.class, 0);
        }

        if (kPlot != null) {
            long startTime = System.nanoTime();

            kPlot.process(event, sensors, kPatList, outputFullTracks);
            long endPlottingTime = System.nanoTime();
            double runTime = (double) (endPlottingTime - startTime) / 1000000.;
            plottingTime += runTime;
        }

        KI.clearInterface();
        logger.log(Level.FINE, String.format("\n KalmanPatRecDriver.process: Done with event %d", event.getEventNumber()));
    }

    class SortByZ implements Comparator<Pair<double[], double[]>> {

        @Override
        public int compare(Pair<double[], double[]> o1, Pair<double[], double[]> o2) {
            return (int) (o1.getSecondElement()[2] - o2.getSecondElement()[2]);
        }
    }

    class SortByZ2 implements Comparator<TrackerHit> {

        @Override
        public int compare(TrackerHit o1, TrackerHit o2) {
            return (int) (o1.getPosition()[2] - o2.getPosition()[2]);
        }
    }

    private ArrayList<KalTrack>[] prepareTrackCollections(EventHeader event, List<Track> outputFullTracks, List<TrackData> trackDataCollection, List<LCRelation> trackDataRelations, List<GBLStripClusterData> allClstrs, List<LCRelation> gblStripClusterDataRelations, List<TrackResidualsData> trackResiduals, List<LCRelation> trackResidualsRelations, List<LCRelation> kalTracksRelations) {

        int evtNumb = event.getEventNumber();
        String stripHitInputCollectionName = "StripClusterer_SiTrackerHitStrip1D";
        if (!event.hasCollection(TrackerHit.class, stripHitInputCollectionName)) {
            System.out.format("KalmanPatRecDriver.process:" + stripHitInputCollectionName + " does not exist; skipping event %d\n", evtNumb);
            return null;
        }

        long startTime = System.nanoTime();
        ArrayList<KalTrack>[] kPatList = KI.KalmanPatRec(event, decoder);
        long endTime = System.nanoTime();
        double runTime = (double) (endTime - startTime) / 1000000.;
        executionTime += runTime;
        if (verbose) {
            if (runTime > 200.) {
                System.out.format("KalmanPatRecDriver.process: run time for pattern recognition at event %d is %9.1f ms\n", evtNumb, runTime);
                List<TrackerHit> striphits = event.get(TrackerHit.class, "StripClusterer_SiTrackerHitStrip1D");
                System.out.format("                            Number of strip hits = %d\n", striphits.size());
            }
        }
        if (runTime > maxTime) {
            maxTime = runTime;
        }
        nEvents++;
        logger.log(Level.FINE, "KalmanPatRecDriver.process: run time for pattern recognition at event " + evtNumb + " is " + runTime + " milliseconds");

        //List<RawTrackerHit> rawhits = event.get(RawTrackerHit.class, "SVTRawTrackerHits");
        //if (rawhits == null) {
        //    logger.log(Level.FINE, String.format("KalmanPatRecDriver.process: the raw hits collection is missing"));
        //    return null;
        //}        
        int nKalTracks = 0;
        for (int topBottom = 0; topBottom < 2; ++topBottom) {
            ArrayList<KalTrack> kPat = kPatList[topBottom];
            if (kPat.size() == 0) {
                logger.log(Level.FINE, String.format("KalmanPatRecDriver.process: pattern recognition failed to find tracks in tracker %d for event %d.", topBottom, evtNumb));
            }
            for (KalTrack kTk : kPat) {
                if (verbose) {
                    kTk.print(String.format(" PatRec for topBot=%d ", topBottom));
                }
                double[][] covar = kTk.originCovariance();
                for (int ix = 0; ix < 5; ++ix) {
                    for (int iy = 0; iy < 5; ++iy) {
                        if (Double.isNaN(covar[ix][iy])) {
                            logger.log(Level.FINE, String.format("KalmanPatRecDriver.process event %d: NaN at %d %d in covariance for track %d", evtNumb, ix, iy, kTk.ID));
                        }
                    }
                }
                nKalTracks++;

                //Here is where the tracks to be persisted are formed
                Track KalmanTrackHPS = KI.createTrack(kTk, true);
                if (KalmanTrackHPS == null) {
                    continue;
                }

                //pT cut 
                //double [] hParams_check = kTk.originHelixParms();
                //double ptInv_check = hParams_check[2];
                //double pt = Math.abs(1./ptInv_check);
                outputFullTracks.add(KalmanTrackHPS);
                List<GBLStripClusterData> clstrs = KI.createGBLStripClusterData(kTk);
                if (verbose) {
                    for (GBLStripClusterData clstr : clstrs) {
                        KI.printGBLStripClusterData(clstr);
                    }
                }

                //Ecal extrapolation - For the moment done here, but should be moved inside the KalmanInterface (DONE)
                //BaseTrackState ts_ecal = TrackUtils.getTrackExtrapAtEcalRK(KalmanTrackHPS,fm);
                //KalmanTrackHPS.getTrackStates().add(ts_ecal);
                allClstrs.addAll(clstrs);
                for (GBLStripClusterData clstr : clstrs) {
                    gblStripClusterDataRelations.add(new BaseLCRelation(KalmanTrackHPS, clstr));
                }

                //Set top by default
                int trackerVolume = 0;
                //if tanLamda<0 set bottom
                if (KalmanTrackHPS.getTrackStates().get(0).getTanLambda() < 0) {
                    trackerVolume = 1;
                }

                //TODO: compute isolations
                double qualityArray[] = new double[1];
                qualityArray[0] = -1;

                //Get the track momentum and convert it into detector frame and float values
                Hep3Vector momentum = new BasicHep3Vector(KalmanTrackHPS.getTrackStates().get(0).getMomentum());
                momentum = CoordinateTransformations.transformVectorToDetector(momentum);

                float[] momentum_f = new float[3];
                momentum_f[0] = (float) momentum.x();
                momentum_f[1] = (float) momentum.y();
                momentum_f[2] = (float) momentum.z();

                //Add the Track Data 
                TrackData KFtrackData = new TrackData(trackerVolume, (float) kTk.getTime(), qualityArray, momentum_f);
                trackDataCollection.add(KFtrackData);
                trackDataRelations.add(new BaseLCRelation(KFtrackData, KalmanTrackHPS));
                kalTracksRelations.add(new BaseLCRelation(KalmanTrackHPS,kTk));

                //Add the TrackResiduas
                List<Integer> layers = new ArrayList<Integer>();
                List<Double> residuals = new ArrayList<Double>();
                List<Float> sigmas = new ArrayList<Float>();

                for (int ilay = 0; ilay < 14; ilay++) {
                    Pair<Double, Double> res_and_sigma = kTk.unbiasedResidual(ilay);
                    if (res_and_sigma.getSecondElement() > -1.) {
                        layers.add(ilay);
                        residuals.add(res_and_sigma.getFirstElement());
                        sigmas.add(res_and_sigma.getSecondElement().floatValue());
                    }
                }//Loop on layers

                TrackResidualsData resData = new TrackResidualsData(trackerVolume, layers, residuals, sigmas);
                trackResiduals.add(resData);
                trackResidualsRelations.add(new BaseLCRelation(resData, KalmanTrackHPS));
                /*
                if (KalmanTrackHPS.getTrackerHits().size() != residuals.size()) {
                    System.out.println("KalmanPatRecDriver::Residuals consistency check failed.");
                    System.out.printf("Track has %d hits while I have %d residuals \n", KalmanTrackHPS.getTrackerHits().size(), residuals.size());
                }
                 */

            } // end of loop on tracks
        } // end of loop on trackers

        nTracks += nKalTracks;

        long endInterfaceTime = System.nanoTime();
        runTime = (double) (endInterfaceTime - endTime) / 1000000.;
        interfaceTime += runTime;

        return kPatList;
    }

    @Override
    public void endOfData() {
        System.out.format("KalmanPatRecDriver.endOfData: total pattern recognition execution time=%12.4f ms for %d events and %d tracks.\n",
                executionTime, nEvents, nTracks);
        double evtTime = executionTime / (double) nEvents;
        double tkrTime = executionTime / (double) nTracks;
        System.out.format("                              Kalman Patrec Time per event = %9.4f ms; Time per track = %9.4f ms\n", evtTime, tkrTime);
        System.out.format("                              Kalman Patrec maximum time for one event = %10.4f ms\n", maxTime);
        evtTime = interfaceTime / (double) nEvents;
        System.out.format("                              Kalman Interface Time per event = %9.4f ms\n", evtTime);
        if (kPlot != null) {
            kPlot.output();
            evtTime = plottingTime / (double) nEvents;
            System.out.format("                              Kalman Plotting Time per event = %9.4f ms\n", evtTime);
        }
        KI.summary();
    }

    // Methods to set Kalman parameters from within the steering file
    public void setNumPatRecIteration(int numPatRecIteration) {
        this.numPatRecIteration = numPatRecIteration;
    }

    public void setNumKalmanIteration(int numKalmanIteration) {
        this.numKalmanIteration = numKalmanIteration;
    }

    public void setMaxPtInverse(double maxPtInverse) {
        this.maxPtInverse = maxPtInverse;
    }

    public void setMaxD0(double maxD0) {
        this.maxD0 = maxD0;
    }

    public void setMaxZ0(double maxZ0) {
        this.maxZ0 = maxZ0;
    }

    public void setMaxChi2(double maxChi2) {
        this.maxChi2 = maxChi2;
    }

    public void setMinHits(int minHits) {
        this.minHits = minHits;
    }

    public void setMinStereo(int minStereo) {
        this.minStereo = minStereo;
    }

    public void setMaxSharedHits(int maxSharedHits) {
        this.maxSharedHits = maxSharedHits;
    }

    public void setMaxTimeRange(double maxTimeRange) {
        this.maxTimeRange = maxTimeRange;
    }

    public void setMaxTanLambda(double maxTanLambda) {
        this.maxTanLambda = maxTanLambda;
    }

    public void setMaxResidual(double maxResidual) {
        this.maxResidual = maxResidual;
    }

    public void setMaxChi2Inc(double maxChi2Inc) {
        this.maxChi2Inc = maxChi2Inc;
    }

    public void setMinChi2IncBad(double minChi2IncBad) {
        this.minChi2IncBad = minChi2IncBad;
    }

    public void setMxChi2Vtx(double mxChi2Vtx) {
        this.mxChi2Vtx = mxChi2Vtx;
    }

    public void setMaxResidShare(double maxResidShare) {
        this.maxResidShare = maxResidShare;
    }

    public void setMaxChi2IncShare(double maxChi2IncShare) {
        this.maxChi2IncShare = maxChi2IncShare;
    }

    public void setNumEvtPlots(int numEvtPlots) {
        this.numEvtPlots = numEvtPlots;
    }

    public void setDoDebugPlots(boolean doDebugPlots) {
        this.doDebugPlots = doDebugPlots;
    }

    public void setSeedCompThr(double seedCompThr) {
        this.seedCompThr = seedCompThr;
    }

    public void setBeamPositionZ(double beamPositionZ) {
        this.beamPositionZ = beamPositionZ;
    }

    public void setBeamSigmaZ(double beamSigmaZ) {
        this.beamSigmaZ = beamSigmaZ;
    }

    public void setBeamPositionX(double beamPositionX) {
        this.beamPositionX = beamPositionX;
    }

    public void setBeamSigmaX(double beamSigmaX) {
        this.beamSigmaX = beamSigmaX;
    }

    public void setBeamPositionY(double beamPositionY) {
        this.beamPositionY = beamPositionY;
    }

    public void setBeamSigmaY(double beamSigmaY) {
        this.beamSigmaY = beamSigmaY;
    }

    public void setUseBeamPositionConditions(boolean useBeamPositionConditions) {
        this.useBeamPositionConditions = useBeamPositionConditions;
    }

    public void setUseFixedVertexZPosition(boolean useFixedVertexZPosition) {
        this.useFixedVertexZPosition = useFixedVertexZPosition;
    }

    public void setNumStrategyIter1(int numStrategyIter1) {
        this.numStrategyIter1 = numStrategyIter1;
    }

    public void setMinSeedEnergy(double minSeedEnergy) {
        this.minSeedEnergy = minSeedEnergy;
        System.out.format("KalmanPatRecDriver: minimum seed energy from steering = %8.4f\n", minSeedEnergy);
    }

    public void setLowPhThresh(double lowPhThresh) {
        this.lowPhThresh = lowPhThresh;
        System.out.format("KalmanPatRecDriver: setting lowPhThresh from steering : %12.4f\n", lowPhThresh);
    }

    public void setSeedStrategy(String seedStrategy) {
        if (strategies == null) {
            strategies = new ArrayList<String>();
        }
        strategies.add(seedStrategy);
        System.out.format("KalmanPatRecDriver: top and bottom strategy %s specified by steering.\n", seedStrategy);
    }

    public void setSeedStrategyTop(String seedStrategy) {
        if (strategiesTop == null) {
            strategiesTop = new ArrayList<String>();
        }
        strategiesTop.add(seedStrategy);
        System.out.format("KalmanPatRecDriver: top strategy %s specified by steering.\n", seedStrategy);
    }

    public void setSeedStrategyBottom(String seedStrategy) {
        if (strategiesBot == null) {
            strategiesBot = new ArrayList<String>();
        }
        strategiesBot.add(seedStrategy);
        System.out.format("KalmanPatRecDriver: bottom strategy %s specified by steering.\n", seedStrategy);
    }

    public void setLogLevel(String logLevel) {
        System.out.format("KalmanPatRecDriver: setting the logger level to %s\n", logLevel);
        this.logLevel = Level.parse(logLevel);
        System.out.format("                    logger level = %s\n", this.logLevel.getName());
    }
}
