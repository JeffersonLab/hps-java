package org.hps.recon.tracking.gbl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import static java.lang.Math.sqrt;

//import hep.physics.vec.VecOp;

import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

//Rounding
import java.math.BigDecimal;
import java.math.RoundingMode;

//import org.hps.recon.tracking.HpsHelicalTrackFit;

import hep.physics.vec.Hep3Vector;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Matrix;
//import hep.physics.vec.BasicHep3Matrix;
import org.hps.recon.tracking.gbl.matrix.SymMatrix;
//import org.hps.recon.tracking.gbl.matrix.Vector;
//import hep.physics.matrix.SymmetricMatrix;
import org.hps.recon.tracking.gbl.matrix.Matrix;

//import org.hps.recon.tracking.CoordinateTransformations;

//import org.apache.commons.math3.util.Pair;
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
import org.lcsim.constants.Constants;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.base.BaseTrack;

//Fiducial cuts on the calorimeter cluster
import org.hps.record.triggerbank.TriggerModule;

//import org.lcsim.event.base.BaseTrackState;
//import org.lcsim.fit.helicaltrack.HelixUtils;
import org.lcsim.geometry.FieldMap;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;

import org.lcsim.event.TrackerHit;
//import org.lcsim.event.base.BaseLCRelation;
import org.lcsim.geometry.Detector;
//import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;



import org.lcsim.geometry.compact.converter.MilleParameter;
import org.lcsim.detector.tracker.silicon.AlignableDetectorElement;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;


// Constrain file writer
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

//Derivatives plots
import org.lcsim.util.aida.AIDA;
//import hep.aida.IManagedObject;
//import hep.aida.IBaseHistogram;

//Reference
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.DoubleByReference;

//E/p method
import org.lcsim.event.Cluster;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.TrackState;

/**
 * A Driver which refits tracks using GBL. Does not require GBL collections to
 * be present in the event.
 */
public class SimpleGBLTrajAliDriver extends Driver {
    
    private AIDA aidaGBL; 
    String derFolder = "/gbl_derivatives/";
    String eopFolder = "/EoP/";
    
    private String inputCollectionName = "MatchedTracks";
    private String outputCollectionName = "GBLTracks";
    private String trackRelationCollectionName = "MatchedToGBLTrackRelations";
    private String helicalTrackHitRelationsCollectionName = "HelicalTrackHitRelations";
    private String rotatedHelicalTrackHitRelationsCollectionName = "RotatedHelicalTrackHitRelations";
    private String trackResidualsColName = "TrackResidualsGBL";
    private String trackResidualsRelColName = "TrackResidualsGBLRelations";
    private String rawHitCollectionName = "SVTRawTrackerHits";
    private String gblStripClusterDataRelations = "KFGBLStripClusterDataRelations";
        
    private double bfield;
    private FieldMap bFieldMap;
    private final MultipleScattering _scattering = new MultipleScattering(new MaterialSupervisor());
    private HpsGblTrajectoryCreator _hpsGblTrajCreator; 
    private boolean storeTrackStates = false;
    private StandardCuts cuts = new StandardCuts();
    
    private MilleBinaryJna mille;
    private String milleBinaryFileName = MilleBinary.DEFAULT_OUTPUT_FILE_NAME;
    private boolean writeMilleBinary = false;
    private double writeMilleChi2Cut = 99999;
    private boolean includeNoHitScatters = false;
    private boolean computeGBLResiduals  = true;
    private boolean enableStandardCuts = false;
    private boolean enableAlignmentCuts = false;
    private List<AlignableDetectorElement>  Alignabledes = new ArrayList<AlignableDetectorElement>();
    private List<SiSensor> sensors = new ArrayList<SiSensor>();
    private boolean debugAlignmentDs = false;
    private boolean compositeAlign = false;
    private boolean constrainedFit = false;
    private boolean constrainedBSFit = false;
    private double bsZ = -7.5;
    private double bsX = 0.0;
    private double bsY = 0.0;
    private boolean constrainedD0Fit = false;
    private boolean constrainedZ0Fit = false;
    private int trackSide = -1;
    private boolean doCOMAlignment = false;
    private double seed_precision = 10000; // the constraint on q/p
    private double momC     = 4.55;
    private double minMom   = 3;
    private double maxMom   = 6;
    private double maxtanL  = 0.025;
    private int    nHitsCut = 6;
    private boolean useParticles = false;
    private double posEoP = -1;
    private double eleEoP = -1;

    
    private GblTrajectoryMaker _gblTrajMaker;
    
    //This holds all the Alignment structures in the case one wants to use the COM alignment
    AlignmentStructuresBuilder asb;
    
    //Calculator for Frame to Frame derivatives
    private FrameToFrameDers f2fD = new FrameToFrameDers();
        
    //Setting 0 is a single refit, 1 refit twice and so on..
    private int gblRefitIterations = 5; 


    public void setPosEoP (double val) {
        posEoP = val;
    }

    public void setEleEoP (double val) {
        eleEoP = val;
    }

    //Switch the COM alignment 
    public void setDoCOMAlignment(boolean val) {
        doCOMAlignment = val;
    }
    
    //Set -1 for no selection, 0-slot side tracks 1-hole side tracks
    public void setTrackSide (int side) {
        trackSide = side;
    }
    
    public void setMomC (double val)  {
        momC = val;
    }
    public void setCompositeAlign (boolean val) {
        compositeAlign = val;
    }

    public void setConstrainedFit (boolean val) {
        constrainedFit = val;
    }

    public void setBsZ(double val) {
        bsZ = val;
    }
    public void setBsX(double val) {
        bsX = val;
    }
    public void setBsY(double val) {
        bsY = val;
    }

    public void setSeedPrecision(double val) {
        seed_precision = val;
    }

    public void setConstrainedBSFit (boolean val) {
        constrainedBSFit = val;
    } 

    public void setDebugAlignmentDs (boolean val) {
        debugAlignmentDs = val;
    }


    public void setEnableAlignmentCuts (boolean val) {
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

    public void setTrackResidualsColName (String val) {
        trackResidualsColName = val;
    }

    public void setTrackResidualsRelColName (String val) {
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
    
    public void setGblStripClusterDataRelations (String input) {
        this.gblStripClusterDataRelations = input;
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
        System.out.println("SimpleGBLTrajAliDriver::WARNING:Enabling standardCuts!");
        enableStandardCuts = val;
    }

    public void setMinMom(double val) {
        minMom = val;
    }

    public void setMaxtanL(double val) {
        maxtanL = val;
    }

    public void setMaxMom(double val) {
        maxMom = val;
    }

    public void setUseParticles(boolean val) {
        useParticles = val;
    }
    
    @Override
    protected void startOfData() {
        if (writeMilleBinary)
            mille = new MilleBinaryJna(milleBinaryFileName);
    }

    @Override
    protected void endOfData() {
        //Should be closed directly when destructor is called
        if (writeMilleBinary)
            mille.close();
        
        
        //Save the plots?
        
        //try {
        //    aidaGBL.saveAs("SimpleGBLTrajAliDriverplots.root");
        //}
        //catch (IOException ex) {
        //}
        
    }

    

    @Override
    protected void detectorChanged(Detector detector) {
        
        bFieldMap = detector.getFieldMap();
        
        if (aidaGBL == null)
            aidaGBL = AIDA.defaultInstance();

        aidaGBL.tree().cd("/");

        //setupPlots();
        setupEoPPlots();

        _hpsGblTrajCreator = new HpsGblTrajectoryCreator();

        bfield = Math.abs(TrackUtils.getBField(detector).magnitude());
        _scattering.getMaterialManager().buildModel(detector);
        _scattering.setBField(bfield); // only absolute of B is needed as it's used for momentum calculation only

        _gblTrajMaker = new GblTrajectoryMaker(_scattering, bfield);
        //_gblTrajMaker.setIncludeMS(includeNoHitScatters);
        _gblTrajMaker.setIncludeMS(false);
        

        //Alignment Manager  - Get the composite structures.
        IDetectorElement detectorElement = detector.getDetectorElement();
        Alignabledes = detectorElement.findDescendants(AlignableDetectorElement.class);
        
        for (AlignableDetectorElement ade : Alignabledes) {
            if (ade.getName().contains("alignable")) {
                System.out.printf("Alignable Detector Elements informations: %s \n", ade.getName());
                //System.out.printf(((AlignableDetectorElement)ade).getlocalToGlobal().toString()+"\n");
                if (ade.getParent() != null) {
                    System.out.printf("The parent is: %s\n", ade.getParent().getName());
                }
                else {
                    System.out.printf("No parent. \n");
                }
            }
        }
        
        // Get the sensors subcomponents // This should be only HpsSiSensors
        sensors = detectorElement.findDescendants(SiSensor.class);
        
        if (!doCOMAlignment) {

            //Assign the mothers to the sensors
            //TODO FIX this part. For the moment the mother of the sensors are chosen by string parsing. 
            MakeAlignmentTree("alignable_fullmodule");
            
            //Dump the constrain file
            MakeAlignmentConstraintFile();
        }
        else {
            asb = new AlignmentStructuresBuilder(sensors);
            asb.MakeAlignmentConstraintFile();
        }
    }
    
    @Override
    protected void process(EventHeader event) {

        //Track collection
       
        if (!useParticles && !event.hasCollection(Track.class, inputCollectionName)) {
            return;
        }

        //Particle collection 
        if (useParticles && !event.hasCollection(ReconstructedParticle.class, inputCollectionName)) {
            return;
        }
        
        setupSensors(event);
        List<Track> tracks = new ArrayList<Track>(); 
        List<ReconstructedParticle> particles = null;
        
        if (!useParticles)
            tracks = event.get(Track.class, inputCollectionName);
        
        else
            particles = event.get(ReconstructedParticle.class, inputCollectionName);
                
        RelationalTable hitToStrips  = null;  
        RelationalTable hitToRotated = null;
        
        List<Track> refittedTracks = new ArrayList<Track>();
        List<LCRelation> trackRelations = new ArrayList<LCRelation>();

        List<GBLKinkData> kinkDataCollection = new ArrayList<GBLKinkData>();
        List<LCRelation> kinkDataRelations = new ArrayList<LCRelation>();
        
        //Hit on Track Residuals
        List<TrackResidualsData> trackResidualsCollection =  new ArrayList<TrackResidualsData>();
        List<LCRelation> trackResidualsRelations          = new ArrayList<LCRelation>();

        int TrackType = 0;
        
        if (inputCollectionName.contains("Kalman") || inputCollectionName.contains("KF")) {
            TrackType = 1;
            //System.out.println("PF:: DEBUG :: Found Kalman Tracks in the event");
        }

        //If using Seed Tracker, get the hits from the event
        if (TrackType == 0) {
            hitToStrips = TrackUtils.getHitToStripsTable(event,helicalTrackHitRelationsCollectionName);
            hitToRotated =  TrackUtils.getHitToRotatedTable(event,rotatedHelicalTrackHitRelationsCollectionName);
        }
        
        
        // Create a mapping of matched Tracks to corresonding Clusters. 
        HashMap<Track,Cluster> TrackClusterPairs = null;
        
        //Get the tracks from the particles - redundant, just loop on the map

        if (useParticles) {
            for (ReconstructedParticle particle : particles) {
                if (particle.getTracks().isEmpty() || particle.getClusters().isEmpty())
                    continue;
                tracks.add(particle.getTracks().get(0));
            }
            
            TrackClusterPairs = GetClustersFromParticles(particles);
        }
        
        //Loop over the tracks
        for (Track track : tracks) {
                                    
            List<TrackerHit> temp = null;
            
            if (TrackType == 0) {
                if (hitToStrips == null || hitToRotated == null) {
                    System.out.println("no hits found");
                    continue;
                }
                
                temp = TrackUtils.getStripHits(track, hitToStrips, hitToRotated);
                
                if (temp.size() == 0)
                    //               System.out.println("GBLRefitterDriver::process  did not find any strip hits on this track???");
                    continue;
            }
            

            if (enableAlignmentCuts) {
                
                
                //Get the track parameters
                double[] trk_prms = track.getTrackParameters();
                double tanLambda = trk_prms[BaseTrack.TANLAMBDA];
                
                //Momentum cut: 3.8 - 5.2
                Hep3Vector momentum = new BasicHep3Vector(track.getTrackStates().get(0).getMomentum());
                //Kalman
                if (TrackType == 1)
                    nHitsCut = 2*nHitsCut;
                
                if (momentum.magnitude() < minMom || momentum.magnitude() > maxMom) {
                    continue;
                }
                
                if (Math.abs(tanLambda) < maxtanL) {

                    continue;
                }
                
                //Align with tracks with at least 6 hits
                if ((tanLambda > 0 && track.getTrackerHits().size() < nHitsCut) || (tanLambda < 0 && track.getTrackerHits().size() < nHitsCut))  {
                    continue;
                }
                
                // ask tracks only on a side
                if (trackSide >= 0) 
                {
                    
                    if (trackSide > 1) {
                        System.out.println("SimpleGBLTrajAliDriver:: wrong settings for track side selection");
                        continue;
                    }
                    
                    if (trackSide == 0 && TrackUtils.isHoleTrack(track) )
                        continue;
                    
                    else if (trackSide == 1 && !TrackUtils.isHoleTrack(track)) 
                        continue;
                }
                
            }
                
            
            //Get the E/p from the cluster
            if (useParticles) {
                
                //System.out.println("Getting cluster data");
                
                //Get the energy of the cluster
                Cluster em_cluster = TrackClusterPairs.get(track);
                
                //Should use the trackState at the last
                TrackState trackState = track.getTrackStates().get(0);
                double trackp = new BasicHep3Vector(trackState.getMomentum()).magnitude();
                double e_o_p = em_cluster.getEnergy() / trackp;
                
                //compute the correction
                momC = em_cluster.getEnergy();
                
                double[] trk_prms = track.getTrackParameters(); 
                
                if (trk_prms[BaseTrack.TANLAMBDA] > 0) {
                    aidaGBL.histogram1D(eopFolder+"Ecluster_top").fill(momC);
                    aidaGBL.histogram1D(eopFolder+"EoP_top").fill(momC/trackp);
                    aidaGBL.histogram2D(eopFolder+"EoP_vs_phi_top").fill(trk_prms[BaseTrack.PHI],momC/trackp);
                    aidaGBL.histogram2D(eopFolder+"EoP_vs_trackP_top").fill(trackp,momC/trackp);
                    aidaGBL.histogram2D(eopFolder+"EoP_vs_tanLambda_top").fill(trk_prms[BaseTrack.TANLAMBDA],momC/trackp);
                    
                    //Track sign is flipped
                    if (track.getCharge() > 0) {
                        aidaGBL.histogram2D(eopFolder+"EoP_vs_trackP_ele_top").fill(trackp,momC/trackp);
                        aidaGBL.histogram2D(eopFolder+"EoP_vs_tanLambda_ele_top").fill(trk_prms[BaseTrack.TANLAMBDA],momC/trackp);
                        aidaGBL.histogram2D(eopFolder+"EoP_vs_phi_ele_top").fill(trk_prms[BaseTrack.PHI],momC/trackp);
                    }
                    else {
                        aidaGBL.histogram2D(eopFolder+"EoP_vs_trackP_pos_top").fill(trackp,momC/trackp);
                        aidaGBL.histogram2D(eopFolder+"EoP_vs_tanLambda_pos_top").fill(trk_prms[BaseTrack.TANLAMBDA],momC/trackp);
                        aidaGBL.histogram2D(eopFolder+"EoP_vs_phi_pos_top").fill(trk_prms[BaseTrack.PHI],momC/trackp);
                    }
                }
                else {
                    aidaGBL.histogram1D(eopFolder+"Ecluster_bottom").fill(momC);
                    aidaGBL.histogram1D(eopFolder+"EoP_bottom").fill(momC/trackp);
                    aidaGBL.histogram2D(eopFolder+"EoP_vs_phi_bottom").fill(trk_prms[BaseTrack.PHI],momC/trackp);
                    aidaGBL.histogram2D(eopFolder+"EoP_vs_trackP_bottom").fill(trackp,momC/trackp);
                    aidaGBL.histogram2D(eopFolder+"EoP_vs_tanLambda_bottom").fill(trk_prms[BaseTrack.TANLAMBDA],momC/trackp);
                    
                    //Track sign is flipped
                    if (track.getCharge() > 0) {
                        aidaGBL.histogram2D(eopFolder+"EoP_vs_trackP_ele_bottom").fill(trackp,momC/trackp);
                        aidaGBL.histogram2D(eopFolder+"EoP_vs_tanLambda_ele_bottom").fill(trk_prms[BaseTrack.TANLAMBDA],momC/trackp);
                        aidaGBL.histogram2D(eopFolder+"EoP_vs_phi_ele_bottom").fill(trk_prms[BaseTrack.PHI],momC/trackp);
                    }
                    else {
                        aidaGBL.histogram2D(eopFolder+"EoP_vs_trackP_pos_bottom").fill(trackp,momC/trackp);
                        aidaGBL.histogram2D(eopFolder+"EoP_vs_tanLambda_pos_bottom").fill(trk_prms[BaseTrack.TANLAMBDA],momC/trackp);
                        aidaGBL.histogram2D(eopFolder+"EoP_vs_phi_pos_bottom").fill(trk_prms[BaseTrack.PHI],momC/trackp);
                    }
                                        
                }
                
                aidaGBL.histogram2D(eopFolder+"EoP_vs_tanLambda").fill(trk_prms[BaseTrack.TANLAMBDA],momC/trackp);
                aidaGBL.histogram2D(eopFolder+"EoP_vs_phi").fill(trk_prms[BaseTrack.PHI],momC/trackp);
                aidaGBL.histogram3D(eopFolder+"EoP_vs_tanLambda_phi").fill(trk_prms[BaseTrack.TANLAMBDA],
                                                                           trk_prms[BaseTrack.PHI],
                                                                           momC/trackp);
                
                
                
                
                if (TriggerModule.inFiducialRegion(em_cluster)) {
                    
                    if (trk_prms[BaseTrack.TANLAMBDA] > 0) {
                        aidaGBL.histogram1D(eopFolder+"Ecluster_top_fid").fill(momC);
                        aidaGBL.histogram1D(eopFolder+"EoP_top_fid").fill(momC/trackp);
                        aidaGBL.histogram2D(eopFolder+"EoP_vs_trackP_top_fid").fill(trackp,momC/trackp);
                        aidaGBL.histogram2D(eopFolder+"EoP_vs_phi_top_fid").fill(trk_prms[BaseTrack.PHI],momC/trackp);
                        aidaGBL.histogram2D(eopFolder+"EoP_vs_tanLambda_top_fid").fill(trk_prms[BaseTrack.TANLAMBDA],momC/trackp);
                        
                        //Track sign is flipped
                        if (track.getCharge() > 0) {
                            aidaGBL.histogram2D(eopFolder+"EoP_vs_trackP_ele_top_fid").fill(trackp,momC/trackp);
                            aidaGBL.histogram2D(eopFolder+"EoP_vs_tanLambda_ele_top_fid").fill(trk_prms[BaseTrack.TANLAMBDA],momC/trackp);
                            aidaGBL.histogram2D(eopFolder+"EoP_vs_phi_ele_top_fid").fill(trk_prms[BaseTrack.PHI],momC/trackp);
                        }
                        else {
                            aidaGBL.histogram2D(eopFolder+"EoP_vs_trackP_pos_top_fid").fill(trackp,momC/trackp);
                            aidaGBL.histogram2D(eopFolder+"EoP_vs_tanLambda_pos_top_fid").fill(trk_prms[BaseTrack.TANLAMBDA],momC/trackp);
                            aidaGBL.histogram2D(eopFolder+"EoP_vs_phi_pos_top_fid").fill(trk_prms[BaseTrack.PHI],momC/trackp);
                        }
                    }
                    else {
                        aidaGBL.histogram1D(eopFolder+"Ecluster_bottom_fid").fill(momC);
                        aidaGBL.histogram1D(eopFolder+"EoP_bottom_fid").fill(momC/trackp);
                        aidaGBL.histogram2D(eopFolder+"EoP_vs_trackP_bottom_fid").fill(trackp,momC/trackp);
                        aidaGBL.histogram2D(eopFolder+"EoP_vs_phi_bottom_fid").fill(trk_prms[BaseTrack.PHI],momC/trackp);
                        aidaGBL.histogram2D(eopFolder+"EoP_vs_tanLambda_bottom_fid").fill(trk_prms[BaseTrack.TANLAMBDA],momC/trackp);

                        
                        //Track sign is flipped
                        if (track.getCharge() > 0) {
                            aidaGBL.histogram2D(eopFolder+"EoP_vs_trackP_ele_bottom_fid").fill(trackp,momC/trackp);
                            aidaGBL.histogram2D(eopFolder+"EoP_vs_tanLambda_ele_bottom_fid").fill(trk_prms[BaseTrack.TANLAMBDA],momC/trackp);
                            aidaGBL.histogram2D(eopFolder+"EoP_vs_phi_ele_bottom_fid").fill(trk_prms[BaseTrack.PHI],momC/trackp);
                        }
                        else {
                            aidaGBL.histogram2D(eopFolder+"EoP_vs_trackP_pos_bottom_fid").fill(trackp,momC/trackp);
                            aidaGBL.histogram2D(eopFolder+"EoP_vs_tanLambda_pos_bottom_fid").fill(trk_prms[BaseTrack.TANLAMBDA],momC/trackp);
                            aidaGBL.histogram2D(eopFolder+"EoP_vs_phi_pos_bottom_fid").fill(trk_prms[BaseTrack.PHI],momC/trackp);
                        }
                    }
                    
                    aidaGBL.histogram2D(eopFolder+"EoP_vs_tanLambda_fid").fill(trk_prms[BaseTrack.TANLAMBDA],momC/trackp);
                    aidaGBL.histogram2D(eopFolder+"EoP_vs_phi_fid").fill(trk_prms[BaseTrack.PHI],momC/trackp);
                    aidaGBL.histogram3D(eopFolder+"EoP_vs_tanLambda_phi_fid").fill(trk_prms[BaseTrack.TANLAMBDA],
                                                                                   trk_prms[BaseTrack.PHI],
                                                                                   momC/trackp);
                    
                }
                
                
            }
            
            
            
            //Track biasing example 
            //Re-fit the track?
            //If momC < 0, only add a term in the covariance matrix to fix the momentum
            if (constrainedFit) {
                
                double momentum_param = 2.99792458e-04;
                //Get the track parameters
                double[] trk_prms = track.getTrackParameters();
                //Bias the track
                double pt = sqrt(track.getPX()*track.getPX() + track.getPY()*track.getPY());
                int sign = trk_prms[BaseTrack.OMEGA] > 0. ? 1 : -1;
                //Bias the FEEs to beam energy. Correct the curvature by projecting on  X / Y plane
                double tanLambda = trk_prms[BaseTrack.TANLAMBDA];
                double cosLambda = 1. / (Math.sqrt(1+tanLambda*tanLambda));
                
                
                if (momC > 0 ) {
                    double targetpT = momC * cosLambda;
                    //System.out.println("TargetpT: " + targetpT + " tanLambda = " + tanLambda);
                    double pt_bias = targetpT - pt;
                    //System.out.println("pT bias: " + pt_bias);
                    double corrected_pt = pt+pt_bias;
                    double corrected_c = sign*(bfield*momentum_param)/(corrected_pt);
                    trk_prms[BaseTrack.OMEGA] = corrected_c;
                    ((BaseTrack)track).setTrackParameters(trk_prms,bfield);
                }
                
                //Correct positrons. getCharge is flipped
                if (posEoP>0 && (track.getCharge() < 0)) {
                    
                    double p =  sqrt(track.getPX()*track.getPX() + 
                                     track.getPY()*track.getPY() + 
                                     track.getPZ()*track.getPZ());
                    
                    
                    //Provide the energy correction
                    double targetP  = p * posEoP;
                    double targetPt = targetP * cosLambda;
                    double corrected_c = sign*(bfield*momentum_param)/(targetPt);
                    trk_prms[BaseTrack.OMEGA] = corrected_c;
                    ((BaseTrack)track).setTrackParameters(trk_prms,bfield);
                }
                
                //Correct positrons
                if (eleEoP>0 && (track.getCharge() > 0)) {
                    
                    double p =  sqrt(track.getPX()*track.getPX() + 
                                     track.getPY()*track.getPY() + 
                                     track.getPZ()*track.getPZ());
                    
                    
                    //Provide the energy correction
                    double targetP  = p * posEoP;
                    double targetPt = targetP * cosLambda;
                    double corrected_c = sign*(bfield*momentum_param)/(targetPt);
                    trk_prms[BaseTrack.OMEGA] = corrected_c;
                    ((BaseTrack)track).setTrackParameters(trk_prms,bfield);
                }
                
            }//constrained fit

            if (constrainedD0Fit) {
                double [] trk_prms = track.getTrackParameters();
                //Bias the track 
                double d0 = trk_prms[BaseTrack.D0];
                double targetd0 = 0.;
                //double d0bias = targetd0 - d0;
               
                double d0bias = 0.;
                if (trk_prms[BaseTrack.TANLAMBDA] > 0)  {
                    d0bias = -0.887;
                }
                else {
                    d0bias = 1.58;
                }
                double corrected_d0 = d0+d0bias;
                trk_prms[BaseTrack.D0] = corrected_d0;
                //System.out.println("d0" + d0);
                //System.out.println("corrected_d0" + trk_prms[BaseTrack.D0);
                
                ((BaseTrack)track).setTrackParameters(trk_prms,bfield);
            }

            if (constrainedZ0Fit) {
                double [] trk_prms = track.getTrackParameters();
                //Bias the track 
                double z0 = trk_prms[BaseTrack.Z0];
                double targetz0 = 0.;
                double z0bias = targetz0 - z0;
                double corrected_z0 = z0+z0bias;
                trk_prms[BaseTrack.Z0] = corrected_z0;
                ((BaseTrack)track).setTrackParameters(trk_prms,bfield);
            }

            //if (enableStandardCuts && gblTrk.getChi2() > cuts.getMaxTrackChisq(gblTrk.getTrackerHits().size()))
            //    continue;

            //This should GBL Trajectories for both the ST+GBL and KF+GBL depending on the setup.
            
            //I think this should take the track->trackerHits and refit those with a GBL otherwise I think I miss the momentum constraint.. ?
            
            List<GBLStripClusterData> trackGblStripClusterData  = computeGBLStripClusterData(track,TrackType,
                                                                                             temp,gblStripClusterDataRelations,event);
            
            //Printout the Cluster Data: 
            
            
            if (debugAlignmentDs) {
                for (GBLStripClusterData strip : trackGblStripClusterData)   {
                    System.out.format("SimpleGBLTrajAliDriver: TrackType="+TrackType);
                    printGBLStripClusterData(strip);
                }
            }
            
            //Add the beamspot constraint

            HelicalTrackFit htf = TrackUtils.getHTF(track);
            double bfac = Constants.fieldConversion * bfield;
            
            GBLBeamSpotPoint bsPoint = FormBSPoint(htf, bsZ,bsX,bsY);

            DoubleByReference Chi2 = new DoubleByReference(0.);
            DoubleByReference lostWeight = new DoubleByReference(0.);
            IntByReference Ndf = new IntByReference(0);
            
            //Create a trajectory with the beamspot 
            List<GblPointJna> points_on_traj = new ArrayList<GblPointJna>();
            
            if (constrainedBSFit)  {
                points_on_traj = _hpsGblTrajCreator.MakeGblPointsList(trackGblStripClusterData, bsPoint, bfac);
            }
            else  {
                points_on_traj = _hpsGblTrajCreator.MakeGblPointsList(trackGblStripClusterData, null, bfac);
            }
            
            if (compositeAlign) {
                
                //For the moment I form the global derivatives here, but in principle I should create them when I run the trajectory creator.
                
                for ( GblPointJna gblpoint : points_on_traj) {
                    
                    
                    if (!doCOMAlignment)
                        ComputeStructureDerivatives(gblpoint);
                    else
                        ComputeCOMDerivatives(gblpoint);
                    
                }//point loop
                
                
                //Make a gblTrajectory with the points with all the composite derivatives + seed and write the record
                
                GblTrajectoryJna trajForMPII = null;
                GblTrajectoryJna trajForMPII_unconstrained = new GblTrajectoryJna(points_on_traj,1,1,1);
                
                if (!constrainedFit) {
                    trajForMPII =  new GblTrajectoryJna(points_on_traj,1,1,1);
                }
                
                else {
                    //Seed constrained fit 
                    SymMatrix seedPrecision = new SymMatrix(5);
                    //seed matrix q/p, yT', xT', xT, yT 
                    
                    //q/p constraint
                    seedPrecision.set(0,0,seed_precision);
                    
                    //d0 constraint
                    //seedPrecision.set(3,3,1000000);
                    trajForMPII = new GblTrajectoryJna(points_on_traj,1,seedPrecision,1,1,1);
                }
                
                if (debugAlignmentDs)
                    trajForMPII.printData();
                
                
                //Fit the trajectory to get the Chi2
                trajForMPII_unconstrained.fit(Chi2,Ndf, lostWeight,"");
                                
                //Avoid to use tracks with terrible Chi2
                if (Chi2.getValue() / Ndf.getValue() > writeMilleChi2Cut)
                    continue;
                
                trajForMPII.milleOut(mille);
                
                
            }// composite Alignment
        }//loop on tracks
        
    }
    
    
    //Get the derivatives
    
    private void addGlobalDerivatives(List<Double> derBuff, List<Integer> labBuff, 
                                      Hep3Matrix Rgtosc, Hep3Vector Tgtosc, Matrix g_ders,
                                      AlignableDetectorElement ade, List<Integer> baseLabels) {
        
        Hep3Matrix Rgtoc = ade.getlocalToGlobal().getRotation().getRotationMatrix();
        Hep3Vector Tgtoc = ade.getlocalToGlobal().getTranslation().getTranslationVector();
        Matrix C_matrix  = f2fD.getDerivative(Rgtosc, Rgtoc, Tgtosc, Tgtoc);
        
        //Compute the composite structure derivatives
        //dr/dac = dr/dai * dai/dac = dr/dai * Ci
        Matrix c_derGlobal = g_ders.times(C_matrix);
        
        //Change the sensor ID to the composite structure one                                                                           
        for (int ilabel = 0; ilabel < baseLabels.size(); ilabel++) {
            Integer c_label = (baseLabels.get(ilabel) / 100) * 100 + ade.getMillepedeId();
            labBuff.add(c_label);
            derBuff.add(c_derGlobal.get(0,ilabel));
        }
        
        if (debugAlignmentDs) {

            System.out.printf("PF::Print the Composite %s Transform:\n", ade.getName());
            System.out.println(Rgtoc.toString());
            System.out.println(Tgtoc.toString());
            C_matrix.print(6,6);
            
            System.out.printf("PF::addGlobalDerivatives::Labels\n");
            System.out.println(labBuff.toString());
            System.out.printf("PF::addGlobalDerivatives::Ders\n");
            System.out.println(derBuff.toString());
        }
    }
    
    //Custom constraint 
    private void getSensorConstraints(AlignableDetectorElement ade, List<SiSensor> hps_sensors, List<String> constraints, boolean MPIIFormat) {
        
        //Get the rotation and translation of the detectorElement                                                                                                                                                      
        Hep3Matrix Rgtoc = ade.getlocalToGlobal().getRotation().getRotationMatrix();
        Hep3Vector Tgtoc = ade.getlocalToGlobal().getTranslation().getTranslationVector();

        //Get the amount of constraints (should be 6)
        int nc = ade.getMPIILabels().size();
        
        //loop on the sensors list:
        
        //System.out.println("PF::CustomConstraint::"+ade.getName());
        
        for (SiSensor sensor : hps_sensors) {
            
            HpsSiSensor hps_sensor = (HpsSiSensor) sensor;
            
            //Check if the sensor belongs to the ade structure
            
            if (sensorBelongsToStructure(ade,hps_sensor)) {
                

                List<Integer> sc_labels = hps_sensor.getMPIILabels();
                
                if (debugAlignmentDs) {
                    System.out.println("PF::Sensor belongs::"+hps_sensor.getName());
                }
                
                //System.out.printf("Derivatives labels: \n%s\n", sc_labels.toString());
                //Get the rotation and translation of the child sub-component
                Hep3Matrix Rgtosc = hps_sensor.getlocalToGlobal().getRotation().getRotationMatrix();
                Hep3Vector Tgtosc = hps_sensor.getlocalToGlobal().getTranslation().getTranslationVector();
                
                //Compute the CMatrix of mother => child
                Matrix C_matrix = f2fD.getDerivative(Rgtosc, Rgtoc, Tgtosc, Tgtoc);
                
                Matrix C_matrixInv = C_matrix.inverse();
                
                if (debugAlignmentDs) {
                    System.out.println("CMATRIX::");
                    C_matrix.print(6,6);
                
                    System.out.println("CMATRIX-1::");
                    C_matrixInv.print(6,6);
                }
                
                FormatConstraint(C_matrixInv, nc, constraints, sc_labels, MPIIFormat);
                
            }//sensor belongs 
                        
        }//sensor loop
        
    }//custom constraint
    
    //Constraint is of the form 0 = sum_n Ciai
    private void getConstraints(AlignableDetectorElement ade, List<String> constraints, boolean MPIIFormat) {
        
        //System.out.printf("DEBUG::PF::getConstraints\n");
        //Get the rotation and translation of the detectorElement
        Hep3Matrix Rgtoc = ade.getlocalToGlobal().getRotation().getRotationMatrix();
        Hep3Vector Tgtoc = ade.getlocalToGlobal().getTranslation().getTranslationVector();
        
        //Get the amount of constraints (should be 6)
        int nc = ade.getMPIILabels().size();
                
        // loop on the children
        for (IDetectorElement i_de : ade.getChildren()) {
            
            //check if the child is an alignableDetectorElement
            if (i_de instanceof AlignableDetectorElement) {
                
                AlignableDetectorElement sc_de = (AlignableDetectorElement) i_de;
                //System.out.printf("Found alignable child: %s\n", sc_de.getName());
                //Get the labels of the child
                List<Integer> sc_labels = sc_de.getMPIILabels();
                
                //System.out.printf("Derivatives labels: \n%s\n", sc_labels.toString());
                //Get the rotation and translation of the child sub-component
                Hep3Matrix Rgtosc = sc_de.getlocalToGlobal().getRotation().getRotationMatrix();
                Hep3Vector Tgtosc = sc_de.getlocalToGlobal().getTranslation().getTranslationVector();
                
                //Compute the CMatrix of mother => child
                Matrix C_matrix = f2fD.getDerivative(Rgtosc, Rgtoc, Tgtosc, Tgtoc);
                Matrix C_matrixInv = C_matrix.inverse();
                
                if (debugAlignmentDs) {
                    System.out.printf("CMatrix\n");
                    C_matrix.print(6,6);
                
                    //Invert the C_Matrix ( PF::TODO::It is enough to compute the transpose of C11, C12, C22)
                    System.out.printf("CMatrix_inv\n");
                    
                    C_matrixInv.print(6,6);
                }

                FormatConstraint(C_matrixInv, nc, constraints, sc_labels, MPIIFormat);
                
            }//child is an ade
        }//children loop
        
    }//get constraints
    
    //Format the constraint
    
    private void FormatConstraint(Matrix C_matrixInv, int nc, List<String> constraints, List<Integer> sc_labels, boolean MPIIFormat) {
        
        for (int ic = 0; ic < nc; ic++) {
            String appendix   = ""; //decide if to keep summing or last entry in the constraint.
            
            //The constraint is of the form 0 = sum_i=0 ^{n} C^{-1}a_i
            
            //Cmatrix is a 6by6 (get the nCols) TODO
            for (int icol = 0 ; icol < 6; icol++) {
                
                //Get the current value
                String s_cnstr = constraints.get(ic);
                
                //Cmatrix coeff less than 1e-5 are ignored. Revisit? 
                
                if (Math.abs(C_matrixInv.get(ic,icol)) < 1e-6) 
                    continue;
                
                // get the rounded C matrix -1 entry rounded to 10e-4
                Double cnstr  = round(C_matrixInv.get(ic,icol),4);
                Integer sc_label = sc_labels.get(icol);
                
                int trans_type = (sc_label / 100) % 10;
                boolean isRot  = ((sc_label / 1000) % 10) == 1 ? false : true;
                
                
                //Remove translations along v of the sensors (They should always be fixed)
                //if (trans_type == 2)
                //  continue;
                
                //Remove rotations
                //if (isRot && false)
                //  continue;
                
                if (s_cnstr != "")
                    if (!MPIIFormat)
                        s_cnstr += " + ";
                    else
                        s_cnstr += "\n";
                else
                    if (MPIIFormat)
                        s_cnstr += "\n\n";
                
                //s_cnstr += String.valueOf(cnstr) + "* [" + String.valueOf(sc_label) + "]";
                if (!MPIIFormat)
                    s_cnstr +=  String.valueOf(sc_label) + " * " + String.valueOf(cnstr);
                else
                    s_cnstr +=  String.valueOf(sc_label) + " " + String.valueOf(cnstr);
                
                //Set this in the list
                constraints.set(ic, s_cnstr);
            }//loop on C^-1 columns
        }//contraint loop
    }
    
    //Make the alignment Constraint file
    //Should make a list for only alignable sensors
    
    private void MakeAlignmentConstraintFile() {
        
        //Map for mapping structure to constraints
        Map<String,List<String> > constraintMap =  new HashMap< String,List<String> >(); 
        
        try {
            
            FileWriter writer = new FileWriter("mille_constraint.txt",false);
            BufferedWriter bufferedWriter = new BufferedWriter(writer);
            
            //Get the base object
            for (AlignableDetectorElement ade : Alignabledes) {
                if (ade.getName().contains("Tracker_base"))
                    continue;
                if (!constraintMap.containsKey(ade.getName())) {
                    //Make the constraints:
                    List<String> constraints = new ArrayList<String>();
                    constraints.add("");
                    constraints.add("");
                    constraints.add("");
                    constraints.add("");
                    constraints.add("");
                    constraints.add("");
                    getConstraints(ade, constraints, true);
                    getSensorConstraints(ade, sensors, constraints,true);
                    
                    constraintMap.put(ade.getName(),constraints);
                }
            }
            
            //Print all the constraints
            System.out.printf("DEBUG::PF::CONSTRAINTS::\n");
            List<String> constr_labels = new ArrayList<String>();
            constr_labels.add("!tu");
            constr_labels.add("!tv");
            constr_labels.add("!tw");
            constr_labels.add("!ru");
            constr_labels.add("!rv");
            constr_labels.add("!rw");
            
            
            bufferedWriter.write("!Constraint file for HPS MPII Alignment\n\n\n");
            for (Map.Entry< String,List<String>> me : constraintMap.entrySet()) {
                System.out.printf(me.getKey()+":\n"); 
                
                int iconstr = -1;
                for ( String constr : me.getValue()) {
                    iconstr +=1;
                    bufferedWriter.write("Constraint 0.0    !"+me.getKey()+" " +  constr_labels.get(iconstr)+"\n");
                    bufferedWriter.write(constr);
                    bufferedWriter.write("\n\n");
                }
            }
            
            /*
              for (SiSensor sensor : sensors) {
                
              HpsSiSensor hpsSensor = (HpsSiSensor) sensor;
                
              if (hpsSensor.getMillepedeId() < 0)
              continue;
                
              bufferedWriter.write(hpsSensor.getName());
              bufferedWriter.newLine();
              bufferedWriter.write(hpsSensor.getMPIILabels().toString());
              bufferedWriter.newLine();
                
              }
            */
            
            bufferedWriter.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    
    
    //Assigns the mother to the sensors
    private void MakeAlignmentTree(String regEx) {
        
        //Loop on the sensors
        for (SiSensor sensor : sensors)  {
            
            String volname = "top";
            int volume = 1;
            int mpid = ((HpsSiSensor)sensor).getMillepedeId();
            
            //Remove the ecal scoring planes
            if (mpid < 0)
                continue;
                        
            if (((HpsSiSensor)sensor).isBottomLayer()) {
                volname = "bottom"; 
                volume = 2;
            }
            //System.out.printf("DEBUG::PF::MakeAlignmentTree Checking sensor %s \n", sensor.getName());
            
            //Loop on the alignable elements
            for (AlignableDetectorElement ade : Alignabledes) {
                
                if (ade.getName().contains(volname) && ade.getName().contains(String.valueOf(getFullModule(volume,((HpsSiSensor)sensor).getMillepedeId() ) ) ) )
                {
                    //Add the alignable mother to the sensor
                    ((HpsSiSensor)sensor).setAdeMother(ade);
                    
                    //Add the sensor to the children of the alignable detector element --  Redundant?!
                    //if (!ade.getName().contains("base"))
                    //  ade.getChildren().add(sensor);
                }
            }//loop on ades
        }//loop on sensors
        
        
        
        for (SiSensor sensor : sensors) {
            if (((HpsSiSensor)sensor).getAdeMother() != null)
                System.out.printf("DEBUG::PF::MakeAlignmentTree sensor %s has mother %s \n", sensor.getName(), ((HpsSiSensor)sensor).getAdeMother().getName());
        }
        
        for (AlignableDetectorElement ade : Alignabledes) {
            System.out.printf("DEBUG::PF::MakeAlignmentTree ade %s has children \n %s \n", ade.getName(), ade.getChildren().toString());
            
        }        
    }
    
    //Matching by name
    private boolean sensorBelongsToStructure(AlignableDetectorElement ade, HpsSiSensor sensor) {
        
        AlignableDetectorElement ade_to_check = sensor.getAdeMother();
        
        while (ade_to_check != null && ade_to_check.getName() != null && !ade_to_check.getName().contains("Tracker_base")) {
            
            if (ade_to_check.getName().equals(ade.getName()))
                return true;
            else {
                if (!(ade_to_check.getParent() instanceof AlignableDetectorElement))
                    break;
                ade_to_check = (AlignableDetectorElement)ade_to_check.getParent();
                if (ade_to_check == null)
                    break;
            }
        }
        
        return false;
    }


    private int getFullModule(int volume, int mpid) {
        
        //top
        if (volume == 1 || volume == 2) {
            if (mpid  == 1 || mpid == 2)
                return 1;
            else if (mpid == 3 || mpid == 4)
                return 2;
            else if (mpid == 5 || mpid == 6)
                return 3;
            else if (mpid == 7 || mpid == 8)
                return 4;
            else if (mpid == 9 || mpid == 10 || mpid == 11 || mpid == 12 )
                return 5;
            else if (mpid == 13 || mpid == 14 || mpid == 15 || mpid == 16 )
                return 6;
            else if (mpid == 17 || mpid == 18 || mpid == 19 || mpid == 20 )
                return 7;
            else {
                System.out.printf("ERROR::Module volume %d / mpid %d not found \n", volume, mpid);
                return -1;
            }
        }
        else {
            System.out.printf("ERROR::Module volume %d / mpid %d not found \n", volume, mpid);
            return -1;
        }
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
    
    public static double round(double value, int places) {
        if (places < 0) 
            return value;
        
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }


    private void setupEoPPlots() {
        
        List<String> volumes = new ArrayList<String>();
        volumes.add("_top");
        volumes.add("_bottom");

        List<String> charges = new ArrayList<String>();
        charges.add("");
        charges.add("_ele");
        charges.add("_pos");
        
        for (String vol : volumes) {
            
            aidaGBL.histogram1D(eopFolder+"Ecluster"+vol,200,0,5);
            aidaGBL.histogram1D(eopFolder+"EoP"+vol,200,0,2);
            
            double lmin = 0.;
            double lmax = 0.08;
            if (vol == "_bot") {
                lmin = -0.08;
                lmax = 0.;
            }
                        
            for (String charge : charges) {
                aidaGBL.histogram2D(eopFolder+"EoP_vs_trackP"+charge+vol,200,0,6,200,0,2);
                aidaGBL.histogram2D(eopFolder+"EoP_vs_tanLambda"+charge+vol,200,lmin,lmax,200,0,2);
                aidaGBL.histogram2D(eopFolder+"EoP_vs_phi"+charge+vol,200,-0.2,0.2,200,0,2);
            }
                        
            aidaGBL.histogram1D(eopFolder+"Ecluster"+vol+"_fid",200,0,5);
            aidaGBL.histogram1D(eopFolder+"EoP"+vol+"_fid",200,0,2);
            aidaGBL.histogram2D(eopFolder+"EoP_vs_trackP"+vol+"_fid",200,0,6,200,0,2);

            for (String charge : charges) {
                aidaGBL.histogram2D(eopFolder+"EoP_vs_trackP"+charge+vol+"_fid",200,0,6,200,0,2);
                aidaGBL.histogram2D(eopFolder+"EoP_vs_tanLambda"+charge+vol+"_fid",200,0.01,0.08,200,0,2);
                aidaGBL.histogram2D(eopFolder+"EoP_vs_phi"+charge+vol+"_fid",200,-0.2,0.2,200,0,2);
            }
        }
        
        aidaGBL.histogram2D(eopFolder+"EoP_vs_tanLambda",200,-0.1,0.1,200,0,2);
        aidaGBL.histogram2D(eopFolder+"EoP_vs_phi",200,-0.2,0.2,200,0,2);
        aidaGBL.histogram3D(eopFolder+"EoP_vs_tanLambda_phi",200,-0.08,0.08,200,-0.2,0.2,200,0,2);

        aidaGBL.histogram2D(eopFolder+"EoP_vs_tanLambda_fid",200,-0.1,0.1,200,0,2);
        aidaGBL.histogram2D(eopFolder+"EoP_vs_phi_fid",200,-0.2,0.2,200,0,2);
        aidaGBL.histogram3D(eopFolder+"EoP_vs_tanLambda_phi_fid",200,-0.08,0.08,200,-0.2,0.2,200,0,2);

        

        
        
    }

    private void setupPlots() {
                
        List<String> volumes = new ArrayList<String>();
        volumes.add("_top");
        volumes.add("_bottom");
        int nbins = 500;
        List<Integer> minAxis = new ArrayList<Integer>();
        minAxis.add(-5);
        minAxis.add(-2);
        minAxis.add(-50);

        List<Integer> maxAxis = new ArrayList<Integer>();
        maxAxis.add(5);
        maxAxis.add(2);
        maxAxis.add(50);
        
        //Only rotations around w
        for (int ivol = 1; ivol<=2; ivol++) {
            for (int itype = 2; itype<=2;itype++) {
                for (int iaxis = 1; iaxis<=3; iaxis++) {
                    for (int is=0; is<=20; is++){
                        String derTag = String.valueOf(ivol*10000 + itype*1000 + iaxis*100 + is);
                        aidaGBL.histogram1D(derFolder+derTag,nbins,minAxis.get(iaxis-1),maxAxis.get(iaxis-1));
                    }//isensor
                }//iaxis
            }//itype
        }//ivol

        //Local derivatives 
        //wrt q/p 
        aidaGBL.histogram1D(derFolder+"df_dqop",nbins, -5,5);
        
        
        //d0 and z0 wrt beamspot

        aidaGBL.histogram1D("d0_vs_bs",nbins,-2.,2.);
        aidaGBL.histogram1D("z0_vs_bs",nbins,-0.200,0.200);

        aidaGBL.histogram1D("d0_vs_bs_meas",nbins,-2.,2.);
        aidaGBL.histogram1D("z0_vs_bs_meas",nbins,-0.200,0.200);
        
        aidaGBL.histogram1D("d0_vs_bs_refit",nbins,-2.,2.);
        aidaGBL.histogram1D("z0_vs_bs_refit",nbins,-0.200,0.200);


        aidaGBL.histogram1D("d0_vs_bs_refit_lcsim",nbins,-2.,2.);
        aidaGBL.histogram1D("z0_vs_bs_refit_lcsim",nbins,-0.200,0.200);

        aidaGBL.histogram1D("d0_vs_bs_BSC_lcsim",nbins,-2.,2.);
        aidaGBL.histogram1D("z0_vs_bs_BSC_lcsim",nbins,-0.200,0.200);
        
        
    }//setupPlots
    
    public void printGBLStripClusterData(GBLStripClusterData clstr) {
        
        System.out.format("\nprintGBLStripClusterData: cluster ID=%d, scatterOnly=%d\n", clstr.getId(), clstr.getScatterOnly());
        System.out.format("  HPS tracking system U=%s\n", clstr.getU().toString());
        System.out.format("  HPS tracking system V=%s\n", clstr.getV().toString());
        System.out.format("  HPS tracking system W=%s\n", clstr.getW().toString());
        System.out.format("  HPS tracking system Track direction=%s\n", clstr.getTrackDirection().toString());
        System.out.format("  phi=%10.6f, lambda=%10.6f\n", clstr.getTrackPhi(), clstr.getTrackLambda());
        System.out.format("  Arc length 2D=%10.5f mm,  Arc length 3D=%10.5f mm\n", clstr.getPath(), clstr.getPath3D());
        System.out.format("  Measurement = %10.5f +- %8.5f mm\n", clstr.getMeas(), clstr.getMeasErr());
        System.out.format("  Track intercept in sensor frame = %s\n", clstr.getTrackPos().toString());
        System.out.format("  RMS projected scattering angle=%10.6f\n", clstr.getScatterAngle());
    }

    
    /* Returns the GBLStripClusterData for a particular track
       Params:
       - Track Type: [0] - SeedTracker; [1] - Kalman
       - gblSCDR: The name of the relations from track ot the GBLStripClusterData in the event (Only needed 
       for KalmanTracks)
       - seedTrackerStripHits -> list of strip hits for refitting the seedTracker tracks
       
    */
    List<GBLStripClusterData> computeGBLStripClusterData(Track track, int TrackType, 
                                                         List<TrackerHit> seedTrackerStripHits, 
                                                         String gblSCDR, EventHeader event) {
        
        if (TrackType == 0) {
            return _gblTrajMaker.makeStripData(TrackUtils.getHTF(track),seedTrackerStripHits);
        }
        
        else if (TrackType == 1) {
            
            //Get the GBLStripClusterData
            RelationalTable kfSCDsRT = null;
            List<LCRelation> kfSCDRelation = new ArrayList<LCRelation>();
            if (event.hasCollection(LCRelation.class,gblSCDR)) { 
                kfSCDsRT = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
                kfSCDRelation = event.get(LCRelation.class,gblSCDR);
                for (LCRelation relation : kfSCDRelation) {
                    if (relation != null && relation.getFrom() != null && relation.getTo() != null) { 
                        kfSCDsRT.add(relation.getFrom(), relation.getTo());
                    }
                }
            } else {
                System.out.println("null KFGBLStripCluster Data Relations.");
                return null; 
            }

            //Get the strip cluster data
            Set<GBLStripClusterData> kfSCDs = kfSCDsRT.allFrom(track);
            
            //Convert the set to a list for sorting it
            List<GBLStripClusterData> list_kfSCDs = new ArrayList<GBLStripClusterData>(kfSCDs);
            
            //Sort the list by cluster ID (that's the millepede index, which should correspond to a monotonous increase in arcLength of the points)
            Collections.sort(list_kfSCDs, arcLComparator);
            
            return list_kfSCDs;
        }//Track Type
        
        else  // Track Type unknown
            return null;
    }
    
    GBLBeamSpotPoint FormBSPoint(HelicalTrackFit htf, double bsZ, double bsX, double bsY) {
        //Form the BeamsSpotPoint
        double [] center = new double[3];
        double [] udir   = new double[3];
        double [] vdir   = new double[3];
        
        center[0] = bsZ; //Z
        center[1] = bsX;  //X
        center[2] = bsY;  //Y
        
        udir[0] = 0;    
        udir[1] = 0;
        udir[2] = 1;  //sensitive direction is along Y global, so along Z in tracking frame
        
        vdir[0] = -0.030429;  //insensitive direction should be along X in global, so along Y in tracking frame.
        vdir[1] = 0.999537; 
        vdir[2] = 0.;
        
        
        //Hard coded uncertainties
        double[] bserror = new double[2];
        bserror[0]=0.02;
        bserror[1]=0.2;
        return GblUtils.gblMakeBsPoint(htf, center, udir, vdir, bserror);
    }
    
    static Comparator<GBLStripClusterData>  arcLComparator = new Comparator<GBLStripClusterData>() {
        
        public int compare(GBLStripClusterData strip1, GBLStripClusterData strip2) {
            return strip1.getId() - strip2.getId();
        }
    };


    private void ComputeCOMDerivatives(GblPointJna gblpoint) {


        List<Integer> labels = gblpoint.getGlobalLabels();
        
        if (labels.size() < 1) 
            return;
        
        
        
        Matrix g_ders = gblpoint.getGlobalDerivatives();
        //The MPII ID + Volume is used to get the correct composite structure
        // 1 for top  2 for bottom
        int volume = labels.get(0) / MilleParameter.half_offset;
        
        String volname = "top";
        String volname_s = "t";
        
        if (volume == 2)  {
            volname= "bottom";
            volname_s = "b";
        }
        
        //sensor label % 100
        int mpid = labels.get(0) % 100;
        
        if (debugAlignmentDs) {
            System.out.println(labels.toString());
            g_ders.print(6,6);
        }
        
        
        HpsSiSensor mysensor = null;
        for (SiSensor sensor : sensors) {
            
            if (sensor.getName().contains(volname_s)) {
                if (((HpsSiSensor) sensor).getMillepedeId() == mpid) {
                    mysensor = (HpsSiSensor) sensor;
                }
            }
        }
        
        if (mysensor == null)
            throw new RuntimeException("Couldn't find HpsSensor for the volume/millepede ID " + volname_s+"/"+String.valueOf(mpid));

        //Holds the derivatives and labels buffers
        List<Double> derBuff = new ArrayList<Double>();
        List<Integer> labBuff = new ArrayList<Integer>();
        
        //Get the alignable sensor from the tree
                            
        AlignableVolume alignable_sensor = asb.getAlignableVolume(mysensor.getName()+"_AV");
        
        //Check against null
                            
        if (alignable_sensor == null)  {
            System.out.println("Alignable Sensor " + mysensor.getName()+"_AV   Not found in the alignment tree");
            return;
        }
        
        //if (debugAlignmentDs) {
        //    alignable_sensor.Print();
        //}
        
        //Get the alignable trxee hierarchy:
        AlignableVolume av_parent = alignable_sensor.getParent();
        String av_d_name = alignable_sensor.getName();
        Matrix c_derGlobal = g_ders;
        while (av_parent != null) {
            List <Integer> c_labels = new ArrayList<Integer>();
            if (debugAlignmentDs) {
                System.out.println("PARENT::" + av_parent.getName());
            }
            //Get the CMatrix of the parent and update the c_derGlobal
            c_derGlobal = c_derGlobal.times(av_parent.getCMatrix(av_d_name));
            //Check the derivatives
            if (debugAlignmentDs) {
                System.out.println("Derivatives for " + av_parent.getName());
            }
            //c_derGlobal.print(6,6);
            //Change to the composite structure ID
            for (int ilabel = 0; ilabel<labels.size(); ilabel++) {
                //Integer c_label = (labels.get(ilabel) / 100) * 100 + av_parent.getMillepedeId();
                Integer c_label = av_parent.getLabels().get(ilabel);
                derBuff.add(c_derGlobal.get(0,ilabel));                                              
                labBuff.add(c_label);
            }
            //System.out.println(c_labels.toString());
                                    
            //Update the daughter
            av_d_name = av_parent.getName();
            //Update the parent
            av_parent = av_parent.getParent();
        }
                            
        //Copy the labels for the composite structure
        List<Integer> c_labGlobal = new ArrayList<Integer>();
        c_labGlobal.addAll(labBuff);
                            
        //Add the labels and the derivatives to the points.
        Matrix allDer = new Matrix(1, labels.size() + c_labGlobal.size());
        for (int ider = 0; ider< labels.size(); ider++) {
            allDer.set(0,ider,g_ders.get(0,ider));
        }
                            
        for (int ider = 0; ider < c_labGlobal.size(); ider++) {
            allDer.set(0,ider+labels.size(),derBuff.get(ider));
        }
                            
        //Join the two lists
        List<Integer> all_labels = new ArrayList<Integer>();
        all_labels.addAll(labels);
        all_labels.addAll(c_labGlobal);
                            
        if (debugAlignmentDs) {
            System.out.println("PF:: Labels and derivatives");
                                
            //Print all data to be written to the binary:
            System.out.println(all_labels.toString());
            allDer.print(6,6);
            System.out.println("========================");
        }
                            
        gblpoint.addGlobals(all_labels,allDer);
                            
                            
    }
    
    private HashMap<Track,Cluster> GetClustersFromParticles(List<ReconstructedParticle> particles) {

        HashMap<Track,Cluster> tracksAndclusters = new HashMap<Track,Cluster>();
        
        for (ReconstructedParticle particle : particles) {
            if (particle.getTracks().isEmpty() || particle.getClusters().isEmpty())
                continue;
            Track track = particle.getTracks().get(0);
            Cluster cluster = particle.getClusters().get(0);
            tracksAndclusters.put(track,cluster);
        }
        
        return tracksAndclusters;
    }
    
    //Returns the E/p or sagitta correction given the input variable (trackP, tanL, phi..)
    private double getEoPCorrection(double xpar) {
        return -999;
    }


    //This will compute and add to the buffers the derivatives 
    private void ComputeStructureDerivatives(GblPointJna gblpoint) {
        
        List<Integer> labels = gblpoint.getGlobalLabels();
        
        if (labels.size() < 1) 
            return;
        
        Matrix g_ders = gblpoint.getGlobalDerivatives();
        //The MPII ID + Volume is used to get the correct composite structure
        // 1 for top  2 for bottom
        int volume = labels.get(0) / MilleParameter.half_offset;
        
        String volname = "top";
        String volname_s = "t";
        
        if (volume == 2)  {
            volname= "bottom";
            volname_s = "b";
        }
        
        //sensor label % 100
        int mpid = labels.get(0) % 100;
        
        if (debugAlignmentDs) {
            System.out.println(labels.toString());
            g_ders.print(6,6);
        }
        
        
        HpsSiSensor mysensor = null;
        for (SiSensor sensor : sensors) {
            
            if (sensor.getName().contains(volname_s)) {
                if (((HpsSiSensor) sensor).getMillepedeId() == mpid) {
                    mysensor = (HpsSiSensor) sensor;
                }
            }
        }
        
        if (mysensor == null)
            throw new RuntimeException("Couldn't find HpsSensor for the volume/millepede ID " + volname_s+"/"+String.valueOf(mpid));
        
        
        //Holds the derivatives and labels buffers
        List<Double> derBuff = new ArrayList<Double>();
        List<Integer> labBuff = new ArrayList<Integer>();
        


        //###############################################//
        //Get the sensor frame
        
        Hep3Matrix Rgtos = mysensor.getGeometry().getLocalToGlobal().getRotation().getRotationMatrix();
        Hep3Vector Tgtos = mysensor.getGeometry().getLocalToGlobal().getTranslation().getTranslationVector();
        
        if (debugAlignmentDs) {
            System.out.printf("PF::Print the Sensor %s Transform:\n", mysensor.getName());
            System.out.println(Rgtos.toString());
            System.out.println(Tgtos.toString());
        }
        
        
                        
        //Modules:
        
        AlignableDetectorElement myade_Mod =  (AlignableDetectorElement) mysensor.getAdeMother();
        
        if (debugAlignmentDs) 
            System.out.printf("PF:: The sensor vol %s and mpid %s corresponds to %s is contained in the structure %s\n\n\n", volname, mpid, mysensor.getName(),myade_Mod.getName());
        
        //Modules
        addGlobalDerivatives(derBuff, labBuff,
                             Rgtos,Tgtos, g_ders,
                             myade_Mod, labels);
        
        //UChannels:
        AlignableDetectorElement myade_UC = (AlignableDetectorElement) mysensor.getAdeMother().getParent();
        
        if (debugAlignmentDs)
            System.out.printf("PF:: The sensor vol %s and mpid %s corresponds to %s is contained in the structure %s\n\n\n", volname, mpid, mysensor.getName(),myade_UC.getName());
        
        
        //UChannels
        addGlobalDerivatives(derBuff, labBuff,
                             Rgtos,Tgtos, g_ders,
                             myade_UC, labels);
        
        
        
        //Copy the labels for the composite structure
        List<Integer> c_labGlobal = new ArrayList<Integer>();
        c_labGlobal.addAll(labBuff);
        
        //Add the labels and the derivatives to the points.
        Matrix allDer = new Matrix(1, labels.size() + c_labGlobal.size());
        for (int ider = 0; ider< labels.size(); ider++) {
            allDer.set(0,ider,g_ders.get(0,ider));
        }
        
        for (int ider = 0; ider < c_labGlobal.size(); ider++) {
            allDer.set(0,ider+labels.size(),derBuff.get(ider));
        }
        
        //Join the two lists
        List<Integer> all_labels = new ArrayList<Integer>();
        all_labels.addAll(labels);
        all_labels.addAll(c_labGlobal);
        
        
        
        if (debugAlignmentDs) {
            System.out.println("PF:: Labels and derivatives");
            
            //Print all data to be written to the binary:
            System.out.println(all_labels.toString());
            allDer.print(18,6);
            System.out.println("========================");
        }
        
        gblpoint.addGlobals(all_labels,allDer);
    }
    
}

