package org.hps.recon.tracking.gbl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
//import static java.lang.Math.sqrt;

//Rounding
import java.math.BigDecimal;
import java.math.RoundingMode;


import hep.physics.vec.Hep3Vector;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Matrix;
//import hep.physics.vec.BasicHep3Matrix;

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
//import org.lcsim.event.base.BaseTrack;

import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseLCRelation;
import org.lcsim.geometry.Detector;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;
import org.hps.recon.tracking.gbl.matrix.Matrix;
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
    private boolean computeGBLResiduals  = true;
    private boolean enableStandardCuts = false;
    private boolean enableAlignmentCuts = false;
    private List<AlignableDetectorElement>  Alignabledes = new ArrayList<AlignableDetectorElement>();
    private List<SiSensor> sensors = new ArrayList<SiSensor>();
    private boolean debugAlignmentDs = false;
    private boolean compositeAlign = false;
    private boolean usePoints = true;
    
    //Calculator for Frame to Frame derivatives
    private FrameToFrameDers f2fD = new FrameToFrameDers();
        
    //Setting 0 is a single refit, 1 refit twice and so on..
    private int gblRefitIterations = 5; 
    
    public void setCompositeAlign (boolean val) {
        compositeAlign = val;
    }

    public void setUsePoints (boolean val) {
        usePoints = val;
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

        if (aidaGBL == null)
            aidaGBL = AIDA.defaultInstance();

        aidaGBL.tree().cd("/");

        setupPlots();

        bfield = Math.abs(TrackUtils.getBField(detector).magnitude());
        _scattering.getMaterialManager().buildModel(detector);
        _scattering.setBField(bfield); // only absolute of B is needed as it's used for momentum calculation only

        //GBLexample1 example1 = new GBLexample1();  
        //example1.runExample(1,10,false); 
        
        //GBLexampleJna1 examplejna1 = new GBLexampleJna1();
        //examplejna1.runExample();

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

        //Assign the mothers to the sensors
        //TODO FIX this part. For the moment the mother of the sensors are chosen by string parsing. 
        MakeAlignmentTree("alignable_fullmodule");
        
        //Dump the constrain file
        MakeAlignmentConstraintFile();

        //setupPlots
        
    }
    
    @Override
    protected void process(EventHeader event) {

        if (!event.hasCollection(Track.class, inputCollectionName))
            return;

        setupSensors(event);
        List<Track> tracks = event.get(Track.class, inputCollectionName);
        // System.out.println("GBLRefitterDriver::process number of tracks = "+tracks.size());
        //RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
        //RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);
        RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event,helicalTrackHitRelationsCollectionName);
        RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event,rotatedHelicalTrackHitRelationsCollectionName);

        List<Track> refittedTracks = new ArrayList<Track>();
        List<LCRelation> trackRelations = new ArrayList<LCRelation>();

        List<GBLKinkData> kinkDataCollection = new ArrayList<GBLKinkData>();
        List<LCRelation> kinkDataRelations = new ArrayList<LCRelation>();

        //Hit on Track Residuals
        List<TrackResidualsData> trackResidualsCollection =  new ArrayList<TrackResidualsData>();
        List<LCRelation> trackResidualsRelations          = new ArrayList<LCRelation>();
        
        //Map<Track, Track> inputToRefitted = new HashMap<Track, Track>();
        for (Track track : tracks) {
            List<TrackerHit> temp = TrackUtils.getStripHits(track, hitToStrips, hitToRotated);
            if (temp.size() == 0)
                //               System.out.println("GBLRefitterDriver::process  did not find any strip hits on this track???");
                continue;
            
            //Track biasing example
            /*
            double momentum_param = 2.99792458e-04;
            //Get the track parameters
            double[] trk_prms = track.getTrackParameters();
            //Bias the track
            double pt = sqrt(track.getPX()*track.getPX() + track.getPY()*track.getPY());
            int sign = trk_prms[BaseTrack.OMEGA] > 0. ? 1 : -1;
            double pt_bias = 0.5; 
            double corrected_pt = pt+pt_bias;
            double corrected_c = sign*(bfield*momentum_param)/(corrected_pt);
            trk_prms[BaseTrack.OMEGA] = corrected_c;
            
            ((BaseTrack)track).setTrackParameters(trk_prms,bfield);
            */

            Pair<Pair<Track, GBLKinkData>, FittedGblTrajectory> newTrackTraj = MakeGblTracks.refitTrackWithTraj(TrackUtils.getHTF(track), temp, track.getTrackerHits(), gblRefitIterations, track.getType(), _scattering, bfield, storeTrackStates,includeNoHitScatters);
            if (newTrackTraj == null) {
                System.out.println("GBLRefitterDriver::process() -- Aborted refit of track -- null pointer for newTrackTraj returned from MakeGblTracks.refitTrackWithTraj .");
                continue;
            }
            Pair<Track, GBLKinkData> newTrack = newTrackTraj.getFirst();
            if (newTrack == null)
                continue;
            Track gblTrk = newTrack.getFirst();

            if (enableStandardCuts && gblTrk.getChi2() > cuts.getMaxTrackChisq(gblTrk.getTrackerHits().size()))
                continue;
            
            //Include trackSelector to decide which tracks to use for alignment. 
            //Propose to use tracks with at least 6 hits in the detector. 
            
            
            if (enableAlignmentCuts) {
                
                //At least 1 GeV
                Hep3Vector momentum = new BasicHep3Vector(gblTrk.getTrackStates().get(0).getMomentum());
                
                
                if (momentum.magnitude() < 1)
                    continue;
                //At least 6 hits
                if (gblTrk.getTrackerHits().size() < 6) 
                    continue;
                
            }

            //Check the rw derivatives
            GblTrajectory gbltraj = newTrackTraj.getSecond().get_traj();
            
            for (GblData gbldata : gbltraj.getTrajData()) {
                
                float vals[] = new float[2];
                List<Integer> indLocal = new ArrayList<Integer>();
                List<Double> derLocal = new ArrayList<Double>();
                List<Integer> labGlobal = new ArrayList<Integer>();
                List<Double> derGlobal = new ArrayList<Double>();
                
                gbldata.getAllData(vals, indLocal, derLocal, labGlobal, derGlobal);
                
                //Measurement
                if  (labGlobal.size() >=6 ) {
                    for (int itag = 3; itag<=5; itag++) {
                        String derTag = String.valueOf(labGlobal.get(itag));
                        aidaGBL.histogram1D(derFolder+derTag).fill(derGlobal.get(itag));
                    }
                }
                
            }
            
            
            if (compositeAlign) {
                
                //Retrieve the GBLPoints from the fit trajectory.
                GblTrajectory gbl_fit_trajectory = newTrackTraj.getSecond().get_traj();
                List<GblPoint> points_on_traj = gbl_fit_trajectory.getSingleTrajPoints();
                
                //Retrieve the GBLData from the fit trajectory.
                List<GblData> data_on_traj = gbl_fit_trajectory.getTrajData();

                if (debugAlignmentDs) {
                    System.out.printf("Size of GBL Points %d Size of GBL Data %d\n",points_on_traj.size(), data_on_traj.size());
                    
                    for (int idata =0; idata< data_on_traj.size(); idata++) {
                        if (data_on_traj.get(idata).getType() == GblData.dataBlockType.InternalMeasurement) {
                            System.out.printf("GblData %d\n",idata);
                            data_on_traj.get(idata).printData();
                        }
                    }
                }
                
                if (usePoints) {
                    
                    for ( GblPoint gblpoint : points_on_traj) {
                        
                        List<Integer> labels = gblpoint.getGlobalLabels();
                        
                        if (labels.size() >  0) {
                            
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
                            //Get the sensor frame
                            
                            Hep3Matrix Rgtos = mysensor.getGeometry().getLocalToGlobal().getRotation().getRotationMatrix();
                            Hep3Vector Tgtos = mysensor.getGeometry().getLocalToGlobal().getTranslation().getTranslationVector();
                            
                            if (debugAlignmentDs) {
                                System.out.printf("PF::Print the Sensor %s Transform:\n", mysensor.getName());
                                System.out.println(Rgtos.toString());
                                System.out.println(Tgtos.toString());
                            }
                            
                            //The same sensor will be contained in multiple Composite structures. 
                            //For the moment I'll try to align the front support and back UChannels. HARDCODED!
                            
                            AlignableDetectorElement myade = (AlignableDetectorElement) mysensor.getAdeMother().getParent();
                            
                            if (debugAlignmentDs)
                                System.out.printf("PF:: The sensor vol %s and mpid %s corresponds to %s is contained in the structure %s\n\n\n", volname, mpid, mysensor.getName(),myade.getName());
                            
                            Hep3Matrix Rgtoc = myade.getlocalToGlobal().getRotation().getRotationMatrix();
                            Hep3Vector Tgtoc = myade.getlocalToGlobal().getTranslation().getTranslationVector();
                            Matrix C_matrix = f2fD.getDerivative(Rgtos, Rgtoc, Tgtos, Tgtoc);
                            
                            //Compute the composite structure derivatives
                            //dr/dac = dr/dai * dai/dac = dr/dai * Ci
                            
                            Matrix c_derGlobal = g_ders.times(C_matrix);
                            
                            //Copy the labels for the composite structure
                            List<Integer> c_labGlobal = new ArrayList<Integer>(labels);
                            
                            //Change the sensor ID to the composite structure one
                            for (int ilabel = 0; ilabel < c_labGlobal.size(); ilabel++) {
                                Integer c_label = (c_labGlobal.get(ilabel) / 100) * 100 + myade.getMillepedeId();
                                c_labGlobal.set(ilabel , c_label);
                            }
                            
                            //Add the labels and the derivatives to the points.
                            Matrix allDer = new Matrix(1, labels.size() + c_labGlobal.size());
                            for (int ider = 0; ider< labels.size(); ider++) {
                                allDer.set(0,ider,g_ders.get(0,ider));
                            }
                            
                            for (int ider = 0; ider < c_labGlobal.size(); ider++) {
                                allDer.set(0,ider+labels.size(),c_derGlobal.get(0,ider));
                            }
                            
                            //Check the derivatives print out. 

                            List<Double> derBuff = new ArrayList<Double>();
                            List<Integer> labBuff = new ArrayList<Integer>();
                            
                            addGlobalDerivatives(derBuff, labBuff,
                                                 Rgtos,Tgtos, g_ders,
                                                 myade, labels);
                            
                            if (debugAlignmentDs) {
                                System.out.printf("PF::addGlobalDerivatives::Labels\n");
                                System.out.println(labBuff.toString());
                                System.out.printf("PF::addGlobalDerivatives::Ders\n");
                                System.out.println(derBuff.toString());
                            }
                            
                            //Join the two lists
                            List<Integer> all_labels = new ArrayList<Integer>();
                            all_labels.addAll(labels);
                            all_labels.addAll(c_labGlobal);
                            
                            gblpoint.addGlobals(all_labels,allDer);
                            
                            if (debugAlignmentDs) {
                                System.out.printf("PF::Print the Composite %s Transform:\n", myade.getName());
                                System.out.println(Rgtoc.toString());
                                System.out.println(Tgtoc.toString());
                                C_matrix.print(6,6);
                                System.out.println("PF:: Composite labels and derivatives");
                                System.out.println(c_labGlobal.toString());
                                c_derGlobal.print(6,6);
                                
                                //Print all data to be written to the binary:
                                System.out.println(all_labels.toString());
                                allDer.print(12,6);
                                System.out.println("========================");
                            }
                            
                        }//labels > 0
                    }//point loop
                    
                    //Make a gblTrajectory and write the record
                    
                    GblTrajectory trajForMPII = new GblTrajectory(points_on_traj);
                    trajForMPII.milleOut(mille);
                    
                }//usePoints
                
            }//alitest
            
            
            if (writeMilleBinary && !compositeAlign)
                if (gblTrk.getChi2() < writeMilleChi2Cut)
                    newTrackTraj.getSecond().get_traj().milleOut(mille);
            

            
            //System.out.printf("gblTrkNDF %d  gblTrkChi2 %f  getMaxTrackChisq5 %f getMaxTrackChisq6 %f \n", gblTrk.getNDF(), gblTrk.getChi2(), cuts.getMaxTrackChisq(5), cuts.getMaxTrackChisq(6));
            if (enableStandardCuts && (gblTrk.getChi2() > cuts.getMaxTrackChisq(gblTrk.getTrackerHits().size())))
                continue;

            refittedTracks.add(gblTrk);
            trackRelations.add(new BaseLCRelation(track, gblTrk));
            //PF :: unused
            //inputToRefitted.put(track, gblTrk);
            kinkDataCollection.add(newTrack.getSecond());
            kinkDataRelations.add(new BaseLCRelation(newTrack.getSecond(), gblTrk));
            

            if (computeGBLResiduals) { 
                
                GblTrajectory gbl_fit_trajectory =  newTrackTraj.getSecond().get_traj();
                
                List<Double>  b_residuals = new ArrayList<Double>();
                List<Float>   b_sigmas    = new ArrayList<Float>();
                //List<Double>  u_residuals = new ArrayList<Double>();
                //List<Float>   u_sigmas    = new ArrayList<Float>();
                List<Integer> r_sensors   = new ArrayList<Integer>();
                
                int numData[] = new int[1];
                //System.out.printf("Getting the residuals. Points  on trajectory: %d \n",gbl_fit_trajectory.getNpointsOnTraj());
                //The fitted trajectory has a mapping between the MPID and the ilabel. Use that to get the MPID of the residual.
                Integer[] sensorsFromMapArray = newTrackTraj.getSecond().getSensorMap().keySet().toArray(new Integer[0]);
                //System.out.printf("Getting the residuals. Sensors on trajectory: %d \n",sensorsFromMapArray.length);
                
                //System.out.println("Check residuals of the original fit");
                //Looping on all the sensors on track -  to get the biased residuals.
                for (int i_s = 0; i_s < sensorsFromMapArray.length; i_s++) {       
                    //Get the point label
                    int ilabel = sensorsFromMapArray[i_s];
                    //Get the millepede ID
                    int mpid = newTrackTraj.getSecond().getSensorMap().get(ilabel);
                    List<Double> aResiduals   = new ArrayList<Double>();   
                    List<Double> aMeasErrors  = new ArrayList<Double>();
                    List<Double> aResErrors   = new ArrayList<Double>();  
                    List<Double> aDownWeights = new ArrayList<Double>();
                    gbl_fit_trajectory.getMeasResults(ilabel,numData,aResiduals,aMeasErrors,aResErrors,aDownWeights); 
                    if (numData[0]>1) { 
                        System.out.printf("GBLRefitterDriver::WARNING::We have SCT sensors. Residuals dimensions should be <=1\n");
                    }
                    for (int i=0; i<numData[0];i++) {
                        //System.out.printf("Example1::ilabel numDataIDX MPID aResidual aMeasError aResError\n");
                        //System.out.printf("Example1::measResults %d %d %d %f %f %f \n",ilabel, i, mpid, aResiduals.get(i),aMeasErrors.get(i),aResErrors.get(i));
                        
                        r_sensors.add(mpid);
                        b_residuals.add(aResiduals.get(i));
                        b_sigmas.add(aResErrors.get(i).floatValue());
                    }
                    //Perform an unbiasing fit for each traj
                    
                    //System.out.println("Run the unbiased residuals!!!\n");
                    //For each sensor create a trajectory 
                    GblTrajectory gbl_fit_traj_u = new GblTrajectory(gbl_fit_trajectory.getSingleTrajPoints());
                    double[] u_dVals = new double[2];
                    int[] u_iVals    = new int[1];
                    int[] u_numData  = new int[1]; 
                    //Fit it once to have exactly the same starting point of gbl_fit_trajectory.
                    gbl_fit_traj_u.fit(u_dVals,u_iVals,"");
                    List<Double> u_aResiduals   = new ArrayList<Double>();   
                    List<Double> u_aMeasErrors  = new ArrayList<Double>();
                    List<Double> u_aResErrors   = new ArrayList<Double>();  
                    List<Double> u_aDownWeights = new ArrayList<Double>();
                    
                    try {
                        //Fit removing the measurement
                        gbl_fit_traj_u.fit(u_dVals,u_iVals,"",ilabel);
                        gbl_fit_traj_u.getMeasResults(ilabel,numData,u_aResiduals,u_aMeasErrors,u_aResErrors,u_aDownWeights); 
                        for (int i=0; i<numData[0];i++) {
                            //System.out.printf("Example1::ilabel numDataIDX MPID aResidual aMeasError aResError\n");
                            //System.out.printf("Example1::UmeasResults %d %d %d %f %f %f \n",ilabel, i, mpid, u_aResiduals.get(i),u_aMeasErrors.get(i),u_aResErrors.get(i));
                            
                            r_sensors.add(mpid);
                            b_residuals.add(u_aResiduals.get(i));
                            b_sigmas.add(u_aResErrors.get(i).floatValue());
                        }
                    }
                    catch (RuntimeException e){
                    //  e.printStackTrack();
                        r_sensors.add(-999);
                        b_residuals.add(-9999.);
                        b_sigmas.add((float)-9999.);
                        //System.out.println("Unbiasing fit fails!");
                    }
                    
                }//loop on sensors on track
                
                //Set top by default
                int trackerVolume = 0;
                //if tanLamda<0 set bottom
                //System.out.printf("Residuals size %d \n", r_sensors.size());
                if (gblTrk.getTrackStates().get(0).getTanLambda() < 0) trackerVolume = 1;
                TrackResidualsData resData  = new TrackResidualsData(trackerVolume,r_sensors,b_residuals,b_sigmas);
                trackResidualsCollection.add(resData);
                trackResidualsRelations.add(new BaseLCRelation(resData,gblTrk));
            }//computeGBLResiduals
        }//loop on tracks
        
        // Put the tracks back into the event and exit
        int flag = 1 << LCIOConstants.TRBIT_HITS;
        event.put(outputCollectionName, refittedTracks, Track.class, flag);
        event.put(trackRelationCollectionName, trackRelations, LCRelation.class, 0);
        
        if (computeGBLResiduals) {
            event.put(trackResidualsColName,    trackResidualsCollection,  TrackResidualsData.class, 0);
            event.put(trackResidualsRelColName, trackResidualsRelations, LCRelation.class, 0);
        }
        
        event.put(GBLKinkData.DATA_COLLECTION, kinkDataCollection, GBLKinkData.class, 0);
        event.put(GBLKinkData.DATA_RELATION_COLLECTION, kinkDataRelations, LCRelation.class, 0);
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
            
            //check if the child is and alignableDetectorElement
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
                
                
                //Remove translations along v and w of the sensors
                if (trans_type == 2 || trans_type == 3)
                    continue;
                //Remove rotations  - don't remove rotations
                if (isRot && false)
                    continue;
                
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
                    //getConstraints(ade, constraints,true);
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
                    
                    //Add the sensor to the children of the alignable detector element
                    ade.getChildren().add(sensor);
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
        
    }//setupPlots
}



