package org.hps.recon.tracking.gbl;

import java.util.ArrayList;
import java.util.List;

import hep.physics.vec.Hep3Vector;
import hep.physics.vec.BasicHep3Vector;

import org.apache.commons.math3.util.Pair;
import org.hps.recon.tracking.MaterialSupervisor;
import org.hps.recon.tracking.MultipleScattering;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.TrackResidualsData;
import org.hps.record.StandardCuts;
import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.base.BaseTrack;

import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseLCRelation;
import org.lcsim.geometry.Detector;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;

import org.lcsim.detector.tracker.silicon.AlignableDetectorElement;

// Derivatives plots
import org.lcsim.util.aida.AIDA;
// import hep.aida.IManagedObject;
// import hep.aida.IBaseHistogram;

/**
 * A Driver which refits tracks using GBL. Does not require GBL collections to
 * be present in the event.
 */
public class GBLRefitterDriver extends Driver {

    private AIDA aidaGBL;
    String derFolder = "/gbl_derivatives/";
    private String inputCollectionName = "MatchedTracks";
    private String outputCollectionName = "GBLTracks";
    private String trackRelationCollectionName = "MatchedToGBLTrackRelations";
    private String helicalTrackHitRelationsCollectionName = "HelicalTrackHitRelations";
    private String rotatedHelicalTrackHitRelationsCollectionName = "RotatedHelicalTrackHitRelations";
    private String trackResidualsColName = "TrackResidualsGBL";
    private String trackResidualsRelColName = "TrackResidualsGBLRelations";
    private String rawHitCollectionName = "SVTRawTrackerHits";

    private double bfield;
    private final MultipleScattering _scattering = new MultipleScattering(new MaterialSupervisor());
    private boolean storeTrackStates = false;
    private StandardCuts cuts = new StandardCuts();

    private MilleBinary mille;
    private String milleBinaryFileName = MilleBinary.DEFAULT_OUTPUT_FILE_NAME;
    private boolean writeMilleBinary = false;
    private double writeMilleChi2Cut = 99999;
    private boolean includeNoHitScatters = false;
    private boolean computeGBLResiduals = false;
    private boolean enableStandardCuts = false;
    private boolean enableAlignmentCuts = false;
    private List<AlignableDetectorElement> Alignabledes = new ArrayList<AlignableDetectorElement>();
    private List<SiSensor> sensors = new ArrayList<SiSensor>();
    private boolean usePoints = true;

    // Calculator for Frame to Frame derivatives
    private FrameToFrameDers f2fD = new FrameToFrameDers();

    // Setting 0 is a single refit, 1 refit twice and so on..
    private int gblRefitIterations = 5;

    public void setUsePoints(boolean val) {
        usePoints = val;
    }

    public void setEnableAlignmentCuts(boolean val) {
        enableAlignmentCuts = val;
    }

    public void setIncludeNoHitScatters(boolean val) {
        includeNoHitScatters = val;
    }

    public void setComputeGBLResiduals(boolean val) {
        computeGBLResiduals = val;
    }

    public void setGblRefitIterations(int val) {
        gblRefitIterations = val;
    }

    public void setWriteMilleChi2Cut(int input) {
        writeMilleChi2Cut = input;
    }

    public void setMilleBinaryFileName(String filename) {
        milleBinaryFileName = filename;
    }

    public void setWriteMilleBinary(boolean writeMillepedeFile) {
        writeMilleBinary = writeMillepedeFile;
    }

    public void setStoreTrackStates(boolean input) {
        storeTrackStates = input;
    }

    public boolean getStoreTrackStates() {
        return storeTrackStates;
    }

    public void setInputCollectionName(String inputCollectionName) {
        this.inputCollectionName = inputCollectionName;
    }

    public void setOutputCollectionName(String outputCollectionName) {
        this.outputCollectionName = outputCollectionName;
    }

    public void setTrackResidualsColName(String val) {
        trackResidualsColName = val;
    }

    public void setTrackResidualsRelColName(String val) {
        trackResidualsRelColName = val;
    }

    public void setTrackRelationCollectionName(String trackRelationCollectionName) {
        this.trackRelationCollectionName = trackRelationCollectionName;
    }

    public void setHelicalTrackHitRelationsCollectionName(String helicalTrackHitRelationsCollectionName) {
        this.helicalTrackHitRelationsCollectionName = helicalTrackHitRelationsCollectionName;
    }

    public void setRotatedHelicalTrackHitRelationsCollectionName(String rotatedHelicalTrackHitRelationsCollectionName) {
        this.rotatedHelicalTrackHitRelationsCollectionName = rotatedHelicalTrackHitRelationsCollectionName;
    }

    public void setRawHitCollectionName(String rawHitCollectionName) {
        this.rawHitCollectionName = rawHitCollectionName;
    }

    public void setMaxTrackChisq(int nhits, double input) {
        cuts.setMaxTrackChisq(nhits, input);
    }

    public void setMaxTrackChisq4hits(double input) {
        cuts.setMaxTrackChisq(4, input);
    }

    public void setMaxTrackChisq5hits(double input) {
        cuts.setMaxTrackChisq(5, input);
    }

    public void setMaxTrackChisq6hits(double input) {
        cuts.setMaxTrackChisq(6, input);
    }

    public void setMaxTrackChisqProb(double input) {
        cuts.changeChisqTrackProb(input);
    }

    public void setEnableStandardCuts(boolean val) {
        System.out.println("GBLRefitterDriver::WARNING:Enabling standardCuts!");
        enableStandardCuts = val;
    }

    @Override
    protected void startOfData() {
        if (writeMilleBinary)
            mille = new MilleBinary(milleBinaryFileName);
    }

    @Override
    protected void endOfData() {
        if (writeMilleBinary)
            mille.close();
    }

    @Override
    protected void detectorChanged(Detector detector) {

        bfield = Math.abs(TrackUtils.getBField(detector).magnitude());
        _scattering.getMaterialManager().buildModel(detector);
        _scattering.setBField(bfield); // only absolute of B is needed as it's used for momentum calculation only

        IDetectorElement detectorElement = detector.getDetectorElement();
        sensors = detectorElement.findDescendants(SiSensor.class);

    }

    @Override
    protected void process(EventHeader event) {
        if (!event.hasCollection(Track.class, inputCollectionName))
            return;

        setupSensors(event);
        List<Track> tracks = event.get(Track.class, inputCollectionName);
        // System.out.println("GBLRefitterDriver::process number of tracks = "+tracks.size());
        // RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
        // RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);
        RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event, helicalTrackHitRelationsCollectionName);
        RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event, rotatedHelicalTrackHitRelationsCollectionName);

        List<Track> refittedTracks = new ArrayList<Track>();
        List<LCRelation> trackRelations = new ArrayList<LCRelation>();

        List<GBLKinkData> kinkDataCollection = new ArrayList<GBLKinkData>();
        List<LCRelation> kinkDataRelations = new ArrayList<LCRelation>();

        // Hit on Track Residuals
        List<TrackResidualsData> trackResidualsCollection = new ArrayList<TrackResidualsData>();
        List<LCRelation> trackResidualsRelations = new ArrayList<LCRelation>();

        // Map<Track, Track> inputToRefitted = new HashMap<Track, Track>();
        for (Track track : tracks) {
            List<TrackerHit> temp = TrackUtils.getStripHits(track, hitToStrips, hitToRotated);
            if (temp.size() == 0)
                // System.out.println("GBLRefitterDriver::process did not find any strip hits on this track???");
                continue;

            Pair<Pair<Track, GBLKinkData>, FittedGblTrajectory> newTrackTraj = MakeGblTracks.refitTrackWithTraj(TrackUtils.getHTF(track), temp,
                    track.getTrackerHits(), gblRefitIterations, track.getType(), _scattering, bfield, storeTrackStates, includeNoHitScatters);
            if (newTrackTraj == null) {
                getLogger().warning("Null returned from MakeGblTracks.refitTrackWithTraj - aborting refit");
                continue;
            }
            Pair<Track, GBLKinkData> newTrack = newTrackTraj.getFirst();
            if (newTrack == null)
                continue;
            Track gblTrk = newTrack.getFirst();

            if (enableStandardCuts && gblTrk.getChi2() > cuts.getMaxTrackChisq(gblTrk.getTrackerHits().size()))
                continue;

            // Include trackSelector to decide which tracks to use for alignment.
            // Propose to use tracks with at least 6 hits in the detector.

            if (enableAlignmentCuts) {

                // At least 1 GeV
                Hep3Vector momentum = new BasicHep3Vector(gblTrk.getTrackStates().get(0).getMomentum());

                if (momentum.magnitude() < 1)
                    continue;

                //// At least 6 hits
                // if (gblTrk.getTrackerHits().size() < 6)
                // continue;

                double[] trk_prms = track.getTrackParameters();
                double tanLambda = trk_prms[BaseTrack.TANLAMBDA];

                // Align with tracks without hole on track
                if ((tanLambda > 0 && track.getTrackerHits().size() < 5) || (tanLambda < 0 && track.getTrackerHits().size() < 5))
                    continue;

            }

            // System.out.printf("gblTrkNDF %d gblTrkChi2 %f getMaxTrackChisq5 %f getMaxTrackChisq6 %f \n", gblTrk.getNDF(), gblTrk.getChi2(),
            // cuts.getMaxTrackChisq(5), cuts.getMaxTrackChisq(6));
            if (enableStandardCuts && (gblTrk.getChi2() > cuts.getMaxTrackChisq(gblTrk.getTrackerHits().size())))
                continue;

            refittedTracks.add(gblTrk);
            trackRelations.add(new BaseLCRelation(track, gblTrk));
            // PF :: unused
            // inputToRefitted.put(track, gblTrk);
            kinkDataCollection.add(newTrack.getSecond());
            kinkDataRelations.add(new BaseLCRelation(newTrack.getSecond(), gblTrk));

            if (computeGBLResiduals) {

                GblTrajectory gbl_fit_trajectory = newTrackTraj.getSecond().get_traj();

                List<Double> b_residuals = new ArrayList<Double>();
                List<Float> b_sigmas = new ArrayList<Float>();
                // List<Double> u_residuals = new ArrayList<Double>();
                // List<Float> u_sigmas = new ArrayList<Float>();
                List<Integer> r_sensors = new ArrayList<Integer>();

                int numData[] = new int[1];
                // System.out.printf("Getting the residuals. Points on trajectory: %d \n",gbl_fit_trajectory.getNpointsOnTraj());
                // The fitted trajectory has a mapping between the MPID and the ilabel. Use that to get the MPID of the residual.
                Integer[] sensorsFromMapArray = newTrackTraj.getSecond().getSensorMap().keySet().toArray(new Integer[0]);
                // System.out.printf("Getting the residuals. Sensors on trajectory: %d \n",sensorsFromMapArray.length);

                // System.out.println("Check residuals of the original fit");
                // Looping on all the sensors on track - to get the biased residuals.
                for (int i_s = 0; i_s < sensorsFromMapArray.length; i_s++) {
                    // Get the point label
                    int ilabel = sensorsFromMapArray[i_s];
                    // Get the millepede ID
                    int mpid = newTrackTraj.getSecond().getSensorMap().get(ilabel);
                    List<Double> aResiduals = new ArrayList<Double>();
                    List<Double> aMeasErrors = new ArrayList<Double>();
                    List<Double> aResErrors = new ArrayList<Double>();
                    List<Double> aDownWeights = new ArrayList<Double>();
                    gbl_fit_trajectory.getMeasResults(ilabel, numData, aResiduals, aMeasErrors, aResErrors, aDownWeights);
                    if (numData[0] > 1) {
                        System.out.printf("GBLRefitterDriver::WARNING::We have SCT sensors. Residuals dimensions should be <=1\n");
                    }
                    for (int i = 0; i < numData[0]; i++) {
                        // System.out.printf("Example1::ilabel numDataIDX MPID aResidual aMeasError aResError\n");
                        // System.out.printf("Example1::measResults %d %d %d %f %f %f \n",ilabel, i, mpid,
                        // aResiduals.get(i),aMeasErrors.get(i),aResErrors.get(i));

                        r_sensors.add(mpid);
                        b_residuals.add(aResiduals.get(i));
                        b_sigmas.add(aResErrors.get(i).floatValue());
                    }
                    // Perform an unbiasing fit for each traj

                    // System.out.println("Run the unbiased residuals!!!\n");
                    // For each sensor create a trajectory
                    GblTrajectory gbl_fit_traj_u = new GblTrajectory(gbl_fit_trajectory.getSingleTrajPoints());
                    double[] u_dVals = new double[2];
                    int[] u_iVals = new int[1];
                    int[] u_numData = new int[1];
                    // Fit it once to have exactly the same starting point of gbl_fit_trajectory.
                    gbl_fit_traj_u.fit(u_dVals, u_iVals, "");
                    List<Double> u_aResiduals = new ArrayList<Double>();
                    List<Double> u_aMeasErrors = new ArrayList<Double>();
                    List<Double> u_aResErrors = new ArrayList<Double>();
                    List<Double> u_aDownWeights = new ArrayList<Double>();

                    try {
                        // Fit removing the measurement
                        gbl_fit_traj_u.fit(u_dVals, u_iVals, "", ilabel);
                        gbl_fit_traj_u.getMeasResults(ilabel, numData, u_aResiduals, u_aMeasErrors, u_aResErrors, u_aDownWeights);
                        for (int i = 0; i < numData[0]; i++) {
                            // System.out.printf("Example1::ilabel numDataIDX MPID aResidual aMeasError aResError\n");
                            // System.out.printf("Example1::UmeasResults %d %d %d %f %f %f \n",ilabel, i, mpid,
                            // u_aResiduals.get(i),u_aMeasErrors.get(i),u_aResErrors.get(i));

                            r_sensors.add(mpid);
                            b_residuals.add(u_aResiduals.get(i));
                            b_sigmas.add(u_aResErrors.get(i).floatValue());
                        }
                    } catch (RuntimeException e) {
                        // e.printStackTrack();
                        r_sensors.add(-999);
                        b_residuals.add(-9999.);
                        b_sigmas.add((float) -9999.);
                        // System.out.printf("Unbiasing fit fails! For label::%d\n",ilabel);
                    }

                }// loop on sensors on track

                // Set top by default
                int trackerVolume = 0;
                // if tanLamda<0 set bottom
                // System.out.printf("Residuals size %d \n", r_sensors.size());
                if (gblTrk.getTrackStates().get(0).getTanLambda() < 0)
                    trackerVolume = 1;
                TrackResidualsData resData = new TrackResidualsData(trackerVolume, r_sensors, b_residuals, b_sigmas);
                trackResidualsCollection.add(resData);
                trackResidualsRelations.add(new BaseLCRelation(resData, gblTrk));
            }// computeGBLResiduals
        }// loop on tracks

        // Put the tracks back into the event and exit
        int flag = 1 << LCIOConstants.TRBIT_HITS;
        event.put(outputCollectionName, refittedTracks, Track.class, flag);
        event.put(trackRelationCollectionName, trackRelations, LCRelation.class, 0);

        if (computeGBLResiduals) {
            event.put(trackResidualsColName, trackResidualsCollection, TrackResidualsData.class, 0);
            event.put(trackResidualsRelColName, trackResidualsRelations, LCRelation.class, 0);
        }

        event.put(GBLKinkData.DATA_COLLECTION, kinkDataCollection, GBLKinkData.class, 0);
        event.put(GBLKinkData.DATA_RELATION_COLLECTION, kinkDataRelations, LCRelation.class, 0);
    }

    private void setupSensors(EventHeader event) {
        List<RawTrackerHit> rawTrackerHits = null;
        if (event.hasCollection(RawTrackerHit.class, rawHitCollectionName))
            rawTrackerHits = event.get(RawTrackerHit.class, rawHitCollectionName);
        if (event.hasCollection(RawTrackerHit.class, "RawTrackerHitMaker_RawTrackerHits"))
            rawTrackerHits = event.get(RawTrackerHit.class, "RawTrackerHitMaker_RawTrackerHits");

        EventHeader.LCMetaData meta = event.getMetaData(rawTrackerHits);
        // Get the ID dictionary and field information.
        IIdentifierDictionary dict = meta.getIDDecoder().getSubdetector().getDetectorElement().getIdentifierHelper().getIdentifierDictionary();
        int fieldIdx = dict.getFieldIndex("side");
        int sideIdx = dict.getFieldIndex("strip");
        for (RawTrackerHit hit : rawTrackerHits) {
            // if sensor already has a DetectorElement, skip it
            if (hit.getDetectorElement() != null)
                continue;

            // The "side" and "strip" fields needs to be stripped from the ID for sensor lookup.
            IExpandedIdentifier expId = dict.unpack(hit.getIdentifier());
            expId.setValue(fieldIdx, 0);
            expId.setValue(sideIdx, 0);
            IIdentifier strippedId = dict.pack(expId);
            // Find the sensor DetectorElement.
            List<IDetectorElement> des = DetectorElementStore.getInstance().find(strippedId);
            if (des == null || des.size() == 0)
                throw new RuntimeException("Failed to find any DetectorElements with stripped ID <0x" + Long.toHexString(strippedId.getValue()) + ">.");
            else if (des.size() == 1)
                hit.setDetectorElement((SiSensor) des.get(0));
            else
                // Use first sensor found, which should work unless there are sensors with duplicate IDs.
                for (IDetectorElement de : des)
                    if (de instanceof SiSensor) {
                        hit.setDetectorElement((SiSensor) de);
                        break;
                    }
            // No sensor was found.
            if (hit.getDetectorElement() == null)
                throw new RuntimeException("No sensor was found for hit with stripped ID <0x" + Long.toHexString(strippedId.getValue()) + ">.");
        }
    }

}
