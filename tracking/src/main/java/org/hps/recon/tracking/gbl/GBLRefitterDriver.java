package org.hps.recon.tracking.gbl;

import java.util.ArrayList;
import java.util.List;

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
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseLCRelation;
import org.lcsim.geometry.Detector;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;
import org.hps.recon.tracking.gbl.matrix.Matrix;
import org.lcsim.geometry.compact.converter.MilleParameter;
import org.lcsim.detector.tracker.silicon.AlignableDetectorElement;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;


/**
 * A Driver which refits tracks using GBL. Does not require GBL collections to
 * be present in the event.
 */
public class GBLRefitterDriver extends Driver {

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
        bfield = Math.abs(TrackUtils.getBField(detector).magnitude());
        _scattering.getMaterialManager().buildModel(detector);
        _scattering.setBField(bfield); // only absolute of B is needed as it's used for momentum calculation only
        
        //TESTING PURPOSES
        //GBLexample1 example1 = new GBLexample1();
        //example1.runExample(1,10,false);        
        //GBLexampleJna examplejna1 = new GBLexampleJna();
        //examplejna1.runExample();

        //Alignment Manager  - Get the composite structures.
        IDetectorElement detectorElement = detector.getDetectorElement();
        Alignabledes = detectorElement.findDescendants(AlignableDetectorElement.class);
        
        /*
        for (AlignableDetectorElement ade : Alignabledes) {
            if (ade.getName().contains("alignable")) {
                System.out.printf("Detector element informations: %s \n", ade.getName());
                System.out.printf(((AlignableDetectorElement)ade).getlocalToGlobal().toString()+"\n");
            }
        }
        */
        // Get the sensors subcomponents
        sensors = detectorElement.findDescendants(SiSensor.class);

    }
    
    @Override
    protected void process(EventHeader event) {
        if (!event.hasCollection(Track.class, inputCollectionName))
            return;

        setupSensors(event);
        List<Track> tracks = event.get(Track.class, inputCollectionName);
        //       System.out.println("GBLRefitterDriver::process number of tracks = "+tracks.size());
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
            
            if (compositeAlign) {
                
                //Calculator for Frame to Frame derivatives
                
                FrameToFrameDers f2fD = new FrameToFrameDers();
                
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
                            for (AlignableDetectorElement ade : Alignabledes) {
                                
                                AlignableDetectorElement myade = null;
                                
                                if (ade.getName().contains(volname) && ade.getName().contains(String.valueOf(getFullModule(volume,mpid)))) {
                                    if (debugAlignmentDs)
                                        System.out.printf("PF:: The sensor vol %s and mpid %s corresponds to %s is contained in the structure %s\n\n\n", volname, mpid, mysensor.getName(),ade.getName());
                                    myade = ade;
                                    
                                    
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
                                    
                                    //if (writeMilleBinary) {
                                    //  mille.addData(floats[0],floats[1],indLocal,derLocal,all_labels,all_ders);
                                    //  mille.writeRecord();
                                    //}
                                                                        
                                }//Found the alignable composite structure
                            }//loop on alignable structures
                        }//labels > 0
                    }//point loop
                    
                    //Make a gblTrajectory and write the record
                    
                    GblTrajectory trajForMPII = new GblTrajectory(points_on_traj);
                    trajForMPII.milleOut(mille);
                    
                }//usePoints
                
                else {
                    //Get the labels and the derivatives
                    for ( GblData gbldata : data_on_traj) {
                        
                        //Skip the kinks or other points. Only take the internal measurements.
                        if (gbldata.getType() != GblData.dataBlockType.InternalMeasurement)
                            continue;
                        
                        //Get all Data out of the GblData (needed to form the millepede output)
                        float[] floats = new float[2]; // fValue , fErr
                        List<Integer> indLocal = new ArrayList<Integer>();
                        List<Double> derLocal = new ArrayList<Double>();
                        
                        //I will just need to modify and add these two
                        List<Integer> labels = new ArrayList<Integer>();
                        List<Double> derGlobal = new ArrayList<Double>();
                        gbldata.getAllData(floats, indLocal, derLocal, labels, derGlobal);
                        
                        if (labels.size() > 0) {
                            
                            //Matrix g_ders =  new MAtrixgblpoint.getGlobalDerivatives();
                            Matrix g_ders = new Matrix(1,6);
                            for (int ider = 0; ider < derGlobal.size(); ider++) {
                                g_ders.set(0,ider,derGlobal.get(ider));
                            }
                            
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
                            for (AlignableDetectorElement ade : Alignabledes) {
                                
                                AlignableDetectorElement myade = null;
                                
                                if (ade.getName().contains(volname) && ade.getName().contains(String.valueOf(getFullModule(volume,mpid)))) {
                                    if (debugAlignmentDs)
                                        System.out.printf("PF:: The sensor vol %s and mpid %s corresponds to %s is contained in the structure %s\n\n\n", volname, mpid, mysensor.getName(),ade.getName());
                                    myade = ade;
                                    
                                    
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
                                    
                                    //Create the mille data
                                    List<Double> c_derGlobals = new ArrayList<Double>();
                                    
                                    //Convert the derivatives in a list
                                    for (int ilabel = 0 ; ilabel < c_labGlobal.size(); ilabel++) {
                                        Double c_der = (c_derGlobal.get(0,ilabel));
                                        c_derGlobals.add(c_der);
                                    }
                                    
                                    //Join the two lists
                                    List<Integer> all_labels = new ArrayList<Integer>();
                                    all_labels.addAll(labels);
                                    all_labels.addAll(c_labGlobal);
                                    
                                    List<Double> all_ders = new ArrayList<Double>();
                                    all_ders.addAll(derGlobal);
                                    all_ders.addAll(c_derGlobals);
                                    
                                    if (debugAlignmentDs) {
                                        System.out.printf("PF::Print the Composite %s Transform:\n", myade.getName());
                                        System.out.println(Rgtoc.toString());
                                        System.out.println(Tgtoc.toString());
                                        C_matrix.print(6,6);
                                        System.out.println("PF:: Composite labels and derivatives");
                                        System.out.println(c_labGlobal.toString());
                                        c_derGlobal.print(6,6);
                                        
                                        System.out.println(c_derGlobals.toString());
                                        
                                        //Print all data to be written to the binary:
                                        System.out.println(floats[0] + " " + floats[1] + " "+indLocal.toString());
                                        System.out.println(derLocal.toString());
                                        System.out.println(all_labels.toString());
                                        System.out.println(all_ders.toString());
                                        System.out.println("========================");
                                    }
                                    
                                    
                                    //if (writeMilleBinary) {
                                    //  mille.addData(floats[0],floats[1],indLocal,derLocal,all_labels,all_ders);
                                    //  mille.writeRecord();
                                    //}
                                                                        
                                }//Found the alignable composite structure
                            }//loop on alignable structures
                        }//labels size > 0 ; removing scatter only and the reference point
                    }//gblpointsloop
                }//decide if gblpoints or gbldata
            }//alitest
            
            
            if (writeMilleBinary)
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
                System.out.printf("ERROR::Module volume %d / mpid %d not found", volume, mpid);
                return -1;
            }
        }
        else {
            System.out.printf("ERROR::Module volume %d / mpid %d not found", volume, mpid);
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
}
