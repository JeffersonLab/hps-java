package org.hps.recon.tracking.kalman;

//import java.io.File;
//import java.io.FileNotFoundException;
import java.io.IOException;
//import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.recon.tracking.MaterialSupervisor;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.TrackingReconstructionPlots;
import org.hps.recon.tracking.MaterialSupervisor.ScatteringDetectorVolume;
import org.hps.recon.tracking.MaterialSupervisor.SiStripPlane;
import org.hps.recon.tracking.gbl.GBLStripClusterData;
import org.hps.util.Pair;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseLCRelation;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.geometry.Detector;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.recon.tracking.digitization.sisim.TrackerHitType;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

import hep.physics.vec.Hep3Vector;

import org.lcsim.geometry.IDDecoder;

// $ java -jar ./distribution/target/hps-distribution-4.0-SNAPSHOT-bin.jar -b -DoutputFile=output -d HPS-EngRun2015-Nominal-v4-4-fieldmap -i tracking/tst_4-1.slcio -n 1 -R 5772 steering-files/src/main/resources/org/hps/steering/recon/KalmanTest.lcsim

public class KalmanPatRecDriver extends Driver {

    IDDecoder decoder;
    private ArrayList<SiStripPlane> detPlanes;
    private MaterialSupervisor _materialManager;
    private org.lcsim.geometry.FieldMap fm;
    private KalmanInterface KI;
    private boolean verbose = false;
    private boolean uniformB = false;
    private String trackCollectionName = "GBLTracks";
    private String outputFullTrackCollectionName = "KalmanFullTracks";
    public AIDA aida;
    private String outputPlots = "KalmanTestPlots.root";
    private int nPlotted;
    private int nTracks;
    private int[] nHitsOnTracks;
    private RelationalTable hitToStrips;
    private RelationalTable hitToRotated;

    public void setOutputPlotsFilename(String input) {
        outputPlots = input;
    }

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
        System.out.format("KalmanPatRecDriver: the B field will be assumed uniform.\n");
    }

    public void setMaterialManager(MaterialSupervisor mm) {
        _materialManager = mm;
    }

    public void setTrackCollectionName(String input) {
    }

    private void setupPlots() {
        if (aida == null) aida = AIDA.defaultInstance();
        aida.tree().cd("/");
        nPlotted = 0;

        // arguments to histogram1D: name, nbins, min, max
        aida.histogram1D("Kalman number of tracks", 10, 0., 10.);
        aida.histogram1D("Kalman Track Chi2", 50, 0., 100.);
        aida.histogram1D("Kalman Track Chi2, 12 hits", 50, 0., 100.);
        aida.histogram1D("Kalman Track simple Chi2, 12 hits", 50, 0., 100.);
        aida.histogram1D("Kalman Track Chi2 with 12 good hits", 50, 0., 100.);
        aida.histogram2D("number tracks Kalman vs GBL", 20, 0., 5., 20, 0., 5.);
        aida.histogram1D("helix chi-squared at origin", 100, 0., 25.);
        aida.histogram1D("GBL track chi^2", 50, 0., 100.);
        aida.histogram1D("Kalman Track Number Hits", 20, 0., 20.);
        aida.histogram1D("GBL number tracks", 10, 0., 10.);
        aida.histogram1D("Kalman missed hit residual", 100, -1.0, 1.0);
        aida.histogram1D("Kalman track hit residual, sigmas", 100, -5., 5.);
        aida.histogram1D("Kalman track hit residual", 100, -0.1, 0.1);
        aida.histogram1D("Kalman hit true error", 100, -0.2, 0.2);
        aida.histogram1D("Kalman hit true error over uncertainty", 100, -5., 5.);
        aida.histogram1D("Kalman track Momentum", 100, 0., 5.);
        aida.histogram1D("dRho", 100, -5., 5.);
        aida.histogram1D("dRho error, sigmas", 100, -5., 5.);
        aida.histogram1D("z0", 100, -2., 2.);
        aida.histogram1D("z0 error, sigmas", 100, -5., 5.);
        aida.histogram1D("pt inverse", 100, -1.2, 1.2);
        aida.histogram1D("pt inverse error, percent", 100, -50., 50.);
        aida.histogram1D("pt inverse error, sigmas", 100, -5., 5.);
        aida.histogram1D("tanLambda", 100, -0.3, 0.3);
        aida.histogram1D("tanLambda true", 100, -0.3, 0.3);
        aida.histogram1D("tanLambda error, sigmas", 100, -5., 5.);
        aida.histogram1D("phi0 true", 100, -0.3, 0.3);
        aida.histogram1D("phi0", 100, -0.3, 0.3);
        aida.histogram1D("phi0 error, sigmas", 100, -5., 5.);
        aida.histogram1D("Kalman track drho",100,-5.,5.);
        aida.histogram1D("Kalman track dz",100,-2.,2.);
        aida.histogram1D("Kalman track number MC particles",10,0.,10.);
        aida.histogram1D("Kalman number of wrong hits on track",12,0.,12.);
        aida.histogram1D("GBL track number MC particles",10,0.,10.);
        aida.histogram1D("GBL number of wrong hits on track",12,0.,12.);
        aida.histogram1D("MC hit z in local system (should be zero)", 50, -2., 2.);
        aida.histogram1D("GBL d0", 100, -5., 5.);
        aida.histogram1D("GBL z0", 100, -2., 2.);
        aida.histogram1D("GBL pt inverse", 100, -1.2, 1.1);
        aida.histogram1D("GBL pt inverse, sigmas", 100, -5., 5.);
        aida.histogram1D("Kalman good track time range (ns)", 100, 0., 100.);
        for (int lyr=2; lyr<14; ++lyr) {
            aida.histogram1D(String.format("Layers/Kalman track hit residual in layer %d",lyr), 100, -0.1, 0.1);
            aida.histogram1D(String.format("Layers/Kalman track hit residual in layer %d, sigmas",lyr), 100, -5., 5.);
            aida.histogram1D(String.format("Layers/Kalman true error in layer %d",lyr), 100, -0.2, 0.2);
            aida.histogram1D(String.format("Layers/Kalman layer %d chi^2 contribution", lyr), 100, 0., 20.);
            if (lyr<13) {
                aida.histogram1D(String.format("Layers/Kalman kink in xy, layer %d", lyr),100, -0.001, .001);
                aida.histogram1D(String.format("Layers/Kalman kink in zy, layer %d", lyr),100, -0.0025, .0025);
            }
        }
        nTracks = 0;
        nHitsOnTracks = new int[14];
    }

    @Override
    public void detectorChanged(Detector det) {
        _materialManager = new MaterialSupervisor();
        _materialManager.buildModel(det);

        fm = det.getFieldMap();

        setupPlots();

        detPlanes = new ArrayList<SiStripPlane>();
        List<ScatteringDetectorVolume> materialVols = ((MaterialSupervisor) (_materialManager)).getMaterialVolumes();
        for (ScatteringDetectorVolume vol : materialVols) {
            detPlanes.add((SiStripPlane) (vol));
        }

        TrackUtils.getBField(det).magnitude();
        det.getSubdetector("Tracker").getDetectorElement().findDescendants(HpsSiSensor.class);

        KI = new KalmanInterface(verbose, uniformB);
        KI.createSiModules(detPlanes, fm);
        
        decoder = det.getSubdetector("Tracker").getIDDecoder();
        
        System.out.format("KalmanPatRecDriver: the B field is assumed uniform? %b\n", uniformB);
    }

    @Override
    public void process(EventHeader event) {
        List<Track> outputFullTracks = new ArrayList<Track>();
        String stripHitInputCollectionName = "StripClusterer_SiTrackerHitStrip1D";
        if (!event.hasCollection(TrackerHit.class, stripHitInputCollectionName)) {
            System.out.println("KalmanPatRecDriver.process:" + stripHitInputCollectionName + " does not exist; skipping event");
            return;
        }
        int evtNumb = event.getEventNumber();

        ArrayList<KalmanPatRecHPS> kPatList = KI.KalmanPatRec(event, decoder);
        if (kPatList == null) {
            System.out.println("KalmanPatRecDriver.process: null returned by KalmanPatRec.");
            return;
        }
        
        hitToStrips = TrackUtils.getHitToStripsTable(event);
        hitToRotated = TrackUtils.getHitToRotatedTable(event);
        
        RelationalTable rawtomc = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        if (event.hasCollection(LCRelation.class, "SVTTrueHitRelations")) {
            List<LCRelation> trueHitRelations = event.get(LCRelation.class, "SVTTrueHitRelations");
            for (LCRelation relation : trueHitRelations)
                if (relation != null && relation.getFrom() != null && relation.getTo() != null)
                    rawtomc.add(relation.getFrom(), relation.getTo());
        }

        List<RawTrackerHit> rawhits = event.get(RawTrackerHit.class, "SVTRawTrackerHits");
        if (rawhits == null) {
            System.out.format("KalmanPatRecDriver.process: the raw hits collection is missing\n");
            return;
        }
        
        List<GBLStripClusterData> allClstrs = new ArrayList<GBLStripClusterData>();
        List<LCRelation> gblStripClusterDataRelations  =  new ArrayList<LCRelation>();
        nTracks = 0;
        for (KalmanPatRecHPS kPat : kPatList) {
            if (kPat == null) {
                System.out.println("KalmanPatRecDriver.process: pattern recognition failed in the top or bottom tracker.\n");
                return;
            }
            for (KalTrack kTk : kPat.TkrList) {
                if (verbose) kTk.print(String.format(" PatRec for topBot=%d ",kPat.topBottom));
                nTracks++;
                aida.histogram1D("Kalman Track Number Hits").fill(kTk.nHits);
                if (kTk.nHits == 12) {
                    aida.histogram1D("Kalman Track Chi2, 12 hits").fill(kTk.chi2);
                    aida.histogram1D("Kalman Track simple Chi2, 12 hits").fill(kTk.chi2prime());
                }
                
                aida.histogram1D("Kalman Track Chi2").fill(kTk.chi2);
                double[] momentum = kTk.originP();
                double pMag = Math.sqrt(momentum[0]*momentum[0]+momentum[1]*momentum[1]+momentum[2]*momentum[2]);
                aida.histogram1D("Kalman track Momentum").fill(pMag);
                aida.histogram1D("Kalman track drho").fill(kTk.originHelixParms()[0]);
                aida.histogram1D("Kalman track dz").fill(kTk.originHelixParms()[3]);
                Track KalmanTrackHPS = KI.createTrack(kTk, true);
                outputFullTracks.add(KalmanTrackHPS);
                List<GBLStripClusterData> clstrs = KI.createGBLStripClusterData(kTk);
                if (verbose) {
                    for (GBLStripClusterData clstr : clstrs) {
                        KI.printGBLStripClusterData(clstr);
                    }
                }
                allClstrs.addAll(clstrs);
                for (GBLStripClusterData clstr : clstrs) {
                    gblStripClusterDataRelations.add(new BaseLCRelation(KalmanTrackHPS, clstr));
                }
                // Histogram residuals of hits in layers with no hits on the track and with hits
                ArrayList<MCParticle> mcParts = new ArrayList<MCParticle>();
                ArrayList<Integer> mcCnt= new ArrayList<Integer>();
                for (MeasurementSite site : kTk.SiteList) {
                    if (verbose) site.print(String.format("track %d", kTk.ID));
                    StateVector aS = site.aS;
                    SiModule mod = site.m;
                    if (aS != null && mod != null) {
                        double phiS = aS.planeIntersect(mod.p);
                        if (!Double.isNaN(phiS)) {
                            Vec rLocal = aS.atPhi(phiS);        // Position in the Bfield frame
                            Vec rGlobal = aS.toGlobal(rLocal);  // Position in the global frame                 
                            Vec rLoc = mod.toLocal(rGlobal);    // Position in the detector frame
                            if (site.hitID < 0) {
                                for (Measurement m : mod.hits) {
                                    double resid = m.v - rLoc.v[1];
                                    aida.histogram1D("Kalman missed hit residual").fill(resid);
                                }                       
                            } else {
                                nHitsOnTracks[mod.Layer]++;
                                double resid = mod.hits.get(site.hitID).v - rLoc.v[1];
                                aida.histogram1D("Kalman track hit residual").fill(resid);
                                aida.histogram1D("Kalman track hit residual, sigmas").fill(resid/Math.sqrt(site.aS.R));
                                aida.histogram1D(String.format("Layers/Kalman track hit residual in layer %d",mod.Layer)).fill(resid);
                                aida.histogram1D(String.format("Layers/Kalman track hit residual in layer %d, sigmas",mod.Layer)).fill(resid/Math.sqrt(site.aS.R));
                                aida.histogram1D(String.format("Layers/Kalman layer %d chi^2 contribution", mod.Layer)).fill(site.chi2inc);
                                if (mod.Layer<13) {
                                    aida.histogram1D(String.format("Layers/Kalman kink in xy, layer %d", mod.Layer)).fill(kTk.scatX(mod.Layer));
                                    aida.histogram1D(String.format("Layers/Kalman kink in zy, layer %d", mod.Layer)).fill(kTk.scatZ(mod.Layer));
                                }  
                                if (kTk.nHits > 9 && kTk.chi2 < 30.) {
                                    aida.histogram1D("Kalman good track time range (ns)").fill(kTk.tMax - kTk.tMin);
                                }
                                TrackerHit hpsHit = KI.getHpsHit(mod.hits.get(site.hitID));
                                List<RawTrackerHit> rawHits = hpsHit.getRawHits();
                                for (RawTrackerHit rawHit : rawHits) {
                                    Set<SimTrackerHit> simHits = rawtomc.allFrom(rawHit);
                                    for (SimTrackerHit simHit : simHits) {
                                        MCParticle mcp = simHit.getMCParticle();
                                        if (mcParts.contains(mcp)) {
                                            int id = mcParts.indexOf(mcp);
                                            mcCnt.set(id, mcCnt.get(id)+1);
                                        } else {
                                            mcParts.add(mcp);
                                            mcCnt.add(1);
                                        }
                                    }
                                }                               
                            }
                        }
                    }
                }
                aida.histogram1D("Kalman track number MC particles").fill(mcParts.size());
                // Which MC particle is the best match?
                int idBest = -1;
                int nMatch = 0;
                for (int id=0; id<mcCnt.size(); ++id) {
                    if (mcCnt.get(id) > nMatch) {
                        nMatch = mcCnt.get(id);
                        idBest = id;
                    }
                }
                int nBad = 0;
                for (MeasurementSite site : kTk.SiteList) {
                    SiModule mod = site.m;
                    TrackerHit hpsHit = KI.getHpsHit(mod.hits.get(site.hitID));
                    List<RawTrackerHit> rawHits = hpsHit.getRawHits();
                    for (RawTrackerHit rawHit : rawHits) {
                        Set<SimTrackerHit> simHits = rawtomc.allFrom(rawHit);
                        boolean goodHit = false;
                        for (SimTrackerHit simHit : simHits) {
                            MCParticle mcp = simHit.getMCParticle();
                            int id = mcParts.indexOf(mcp);
                            if (id == idBest) {
                                goodHit = true;
                                break;
                            }                          
                        }
                        if (!goodHit) nBad++;
                    }                               
                }
                aida.histogram1D("Kalman number of wrong hits on track").fill(nBad);
                MCParticle mcBest = null;
                if (idBest > -1) {
                    mcBest = mcParts.get(idBest); 
                    Hep3Vector pVec = mcBest.getMomentum();
                    Hep3Vector rVec = mcBest.getOrigin();
                    double ptTrue = Math.sqrt(pVec.x()*pVec.x() + pVec.z()*pVec.z());
                    double ptInvTrue = mcBest.getCharge()/ptTrue;
                    //double [] pKal = kTk.originP();
                    double [] hParams = kTk.originHelixParms();
                    double ptInv = hParams[2];
                    double tanLambda = -hParams[4];
                    double tanLambdaTrue = pVec.y()/ptTrue;
                    double phi0True = Math.atan2(pVec.x(), pVec.z());
                    double phi0 = -hParams[1];
                    double phi0Err = kTk.helixErr(1);
                    double ptInvErr = kTk.helixErr(2);
                    double tanLambdaErr = kTk.helixErr(4);
                    double z0 = -hParams[3];
                    double z0True = rVec.y();
                    double z0Err = kTk.helixErr(3);
                    double dRho = hParams[0];
                    double dRhoErr = kTk.helixErr(0);
                    Vec apTrue = new Vec(0.,-phi0True,ptInvTrue,-z0True,-tanLambdaTrue);
                    Vec ap = new Vec(5,hParams);
                    SquareMatrix Cov = new SquareMatrix(5,kTk.originCovariance());
                    SquareMatrix CovInv = Cov.invert();
                    Vec helixDiff = ap.dif(apTrue);
                    double chi2Helix = helixDiff.dot(helixDiff.leftMultiply(CovInv));
                    aida.histogram1D("helix chi-squared at origin").fill(chi2Helix);
                    aida.histogram1D("dRho").fill(dRho);
                    aida.histogram1D("dRho error, sigmas").fill(dRho/dRhoErr);
                    aida.histogram1D("z0").fill(z0);
                    aida.histogram1D("z0 error, sigmas").fill((z0-z0True)/z0Err);
                    aida.histogram1D("phi0 true").fill(phi0True);
                    aida.histogram1D("phi0").fill(phi0);
                    aida.histogram1D("phi0 error, sigmas").fill((phi0-phi0True)/phi0Err);
                    aida.histogram1D("pt inverse").fill(ptInv);
                    aida.histogram1D("pt inverse error, percent").fill(100.*(ptInv-ptInvTrue)/ptInvTrue);
                    aida.histogram1D("pt inverse error, sigmas").fill((ptInv-ptInvTrue)/ptInvErr);
                    aida.histogram1D("tanLambda").fill(tanLambda);
                    aida.histogram1D("tanLambda true").fill(tanLambdaTrue);
                    aida.histogram1D("tanLambda error, sigmas").fill((tanLambda - tanLambdaTrue)/tanLambdaErr);
                }
            }
        }
        aida.histogram1D("Kalman number of tracks").fill(nTracks);
        
        if (event.hasCollection(Track.class, trackCollectionName)) {
            List<Track> tracksGBL = event.get(Track.class, trackCollectionName);
            int nGBL = tracksGBL.size();
            aida.histogram2D("number tracks Kalman vs GBL").fill(nTracks, nGBL);
            aida.histogram1D("GBL number tracks").fill(nGBL);
            double c = 2.99793e8; // Speed of light in m/s
            double conFac = 1.0e12 / c;
            Vec Bfield = KalmanInterface.getField(new Vec(0.,505.57,0.), fm); // Field at the instrument center
            double B = Bfield.mag();
            double alpha = conFac / B; // Convert from pt in GeV to curvature in mm
            for (Track tkrGBL : tracksGBL) {
                aida.histogram1D("GBL track chi^2").fill(tkrGBL.getChi2());
                List<TrackState> stLst = tkrGBL.getTrackStates();
                for (TrackState st : stLst) {
                    if (st.getLocation() == TrackState.AtIP) {
                        double d0 = st.getParameter(0);
                        aida.histogram1D("GBL d0").fill(d0);
                        double z0 = st.getParameter(3);
                        aida.histogram1D("GBL z0").fill(z0);
                        double Omega = st.getOmega();
                        double ptInvGBL = -alpha * Omega;
                        aida.histogram1D("GBL pt inverse").fill(ptInvGBL);
                        double [] covGBL = st.getCovMatrix();
                        double omegaErr = Math.sqrt(covGBL[5]);
                        aida.histogram1D("GBL pt inverse, sigmas").fill(Omega/omegaErr);
                        //System.out.format("d0=%10.5f +- %10.5f\n", d0, Math.sqrt(covGBL[0]));
                        //System.out.format("phi0=%10.5f +- %10.5f\n", st.getParameter(1), Math.sqrt(covGBL[2]));
                        //System.out.format("omega=%10.5f +- %10.5f\n", Omega, omegaErr);
                        //System.out.format("z0=%10.5f +- %10.5f\n", z0, Math.sqrt(covGBL[9]));
                        //System.out.format("tanL=%10.5f +- %10.5f\n", st.getParameter(4), Math.sqrt(covGBL[14]));
                        break;
                    }
                }
                ArrayList<MCParticle> mcParts = new ArrayList<MCParticle>();
                ArrayList<Integer> mcCnt= new ArrayList<Integer>();
                List<TrackerHit> hitsOnTrack = TrackUtils.getStripHits(tkrGBL, hitToStrips, hitToRotated);
                for (TrackerHit hit1D : hitsOnTrack) {
                    List<RawTrackerHit> rawHits = hit1D.getRawHits();
                    for (RawTrackerHit rawHit : rawHits) {
                        Set<SimTrackerHit> simHits = rawtomc.allFrom(rawHit);
                        for (SimTrackerHit simHit : simHits) {
                            MCParticle mcp = simHit.getMCParticle();
                            if (mcParts.contains(mcp)) {
                                int id = mcParts.indexOf(mcp);
                                mcCnt.set(id, mcCnt.get(id)+1);
                            } else {
                                mcParts.add(mcp);
                                mcCnt.add(1);
                            }
                        }
                    }               
                }
                aida.histogram1D("GBL track number MC particles").fill(mcParts.size());
                // Which MC particle is the best match?
                int idBest = -1;
                int nMatch = 0;
                for (int id=0; id<mcCnt.size(); ++id) {
                    if (mcCnt.get(id) > nMatch) {
                        nMatch = mcCnt.get(id);
                        idBest = id;
                    }
                }
                int nBad = 0;
                for (TrackerHit hit1D : hitsOnTrack) {
                    List<RawTrackerHit> rawHits = hit1D.getRawHits();
                    for (RawTrackerHit rawHit : rawHits) {
                        Set<SimTrackerHit> simHits = rawtomc.allFrom(rawHit);
                        boolean goodHit = false;
                        for (SimTrackerHit simHit : simHits) {
                            MCParticle mcp = simHit.getMCParticle();
                            int id = mcParts.indexOf(mcp);
                            if (id == idBest) {
                                goodHit = true;
                                break;
                            }                          
                        }
                        if (!goodHit) nBad++;
                    }                               
                }
                aida.histogram1D("GBL number of wrong hits on track").fill(nBad);
            }
        }
        
        String path = "C:\\Users\\Robert\\Desktop\\Kalman\\";
        if (nPlotted < 40) {
            KI.plotKalmanEvent(path, event, kPatList);
            nPlotted++;
        }
        
        //simScatters(event);
        double maxTruErr = simHitRes(event);
        if (maxTruErr < 0.02) {
            for (KalmanPatRecHPS kPat : kPatList) {
                for (KalTrack kTk : kPat.TkrList) {
                    if (kTk.nHits < 12) continue;                    
                    aida.histogram1D("Kalman Track Chi2 with 12 good hits").fill(kTk.chi2);                    
                }
            }
        }
        
        KI.clearInterface();
        if (verbose) System.out.format("\n KalmanPatRecDriver.process: Done with event %d\n", evtNumb);
        
        int flag = 1 << LCIOConstants.TRBIT_HITS;
        event.put(outputFullTrackCollectionName, outputFullTracks, Track.class, flag);
        event.put("GBLStripClusterData", allClstrs, GBLStripClusterData.class, flag);
        event.put("GBLStripClusterDataRelations", gblStripClusterDataRelations,  LCRelation.class, flag);
    }

    // Make histograms of the MC hit resolution
    private double simHitRes(EventHeader event) {
        // Get the collection of 1D hits
        String stripHitInputCollectionName = "StripClusterer_SiTrackerHitStrip1D";
        List<TrackerHit> stripHits = event.get(TrackerHit.class, stripHitInputCollectionName);
        
        if (stripHits == null) return 999.;

        // Make a mapping from sensor to hits
        Map<HpsSiSensor, ArrayList<TrackerHit>> hitSensorMap = new HashMap<HpsSiSensor, ArrayList<TrackerHit>>();
        for (TrackerHit hit1D : stripHits) {
            HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) hit1D.getRawHits().get(0)).getDetectorElement();

            ArrayList<TrackerHit> hitsInSensor = null;
            if (hitSensorMap.containsKey(sensor)) {
                hitsInSensor = hitSensorMap.get(sensor);
            } else {
                hitsInSensor = new ArrayList<TrackerHit>();
            }
            hitsInSensor.add(hit1D);
            hitSensorMap.put(sensor, hitsInSensor);
        }
        
        String MCHitInputCollectionName = "TrackerHits";
        List<SimTrackerHit> MChits = event.get(SimTrackerHit.class, MCHitInputCollectionName);
        
        if (MChits == null) return 999.;

        // Make a map from MC particle to the hit in the layer
        Map<Integer, ArrayList<SimTrackerHit>> hitMCparticleMap = new HashMap<Integer, ArrayList<SimTrackerHit>>();
        for (SimTrackerHit hit1D : MChits) {
            decoder.setID(hit1D.getCellID());
            int Layer = decoder.getValue("layer") + 1;  // Kalman layers go from 0 to 13, with 0 and 1 for new 2019 modules
            //int Module = decoder.getValue("module");
            //MCParticle MCpart = hit1D.getMCParticle();
            ArrayList<SimTrackerHit> partInLayer = null;
            if (hitMCparticleMap.containsKey(Layer)) {
                partInLayer = hitMCparticleMap.get(Layer);
            } else {
                partInLayer = new ArrayList<SimTrackerHit>();
            }
            partInLayer.add(hit1D);
            hitMCparticleMap.put(Layer,partInLayer);
        }
        //System.out.format("KalmanPatRecDriver.simHitRes: found both hit collections in event %d\n", event.getEventNumber());
        
        double maxErr = 0.;
        ArrayList<SiModule> SiMlist = KI.getSiModuleList();
        Map<SiModule, SiStripPlane> moduleMap = KI.getModuleMap();
        for (SiModule siMod : SiMlist) {
            int layer = siMod.Layer;
            if (hitMCparticleMap.get(layer) == null) continue;
            SiStripPlane plane = moduleMap.get(siMod);
            if (!hitSensorMap.containsKey(plane.getSensor())) continue;
            ArrayList<TrackerHit> hitsInSensor = hitSensorMap.get(plane.getSensor());
            if (hitsInSensor == null) continue;
            for (TrackerHit hit : hitsInSensor) {
                //System.out.format("simHitRes: Hit in sensor of layer %d, detector %d\n",layer,siMod.detector);
                //Vec tkrHit = new Vec(3,hit.getPosition());
                //tkrHit.print("Tracker hit position");
                //SiTrackerHitStrip1D global = (new SiTrackerHitStrip1D(hit)).getTransformedHit(TrackerHitType.CoordinateSystem.GLOBAL);
                SiTrackerHitStrip1D localHit = (new SiTrackerHitStrip1D(hit)).getTransformedHit(TrackerHitType.CoordinateSystem.SENSOR);
                double du = Math.sqrt(localHit.getCovarianceAsMatrix().diagonal(0));
                //Vec globalHPS = new Vec(3,global.getPosition());
                Vec localHPS = new Vec(3,localHit.getPosition());
                //globalHPS.print("simulated hit in HPS global system");
                //localHPS.print("simulated hit in HPS local system");
                
                //Vec hitGlobal = KalmanInterface.vectorGlbToKalman(global.getPosition());
                Vec hitLocal = new Vec(-localHit.getPosition()[1], localHit.getPosition()[0], localHit.getPosition()[2]);
                
                //hitGlobal.print("simulated hit Kal global position");
                //hitLocal.print("simulated hit Kal local position");
                //Vec kalGlobal = siMod.toGlobal(hitLocal);
                //kalGlobal.print("simulated hit global position from Kalman transformation");
                for (SimTrackerHit hitMC : hitMCparticleMap.get(layer)) {
                    Vec hitMCglobal = KalmanInterface.vectorGlbToKalman(hitMC.getPosition());
                    //hitMCglobal.print("true hit Kal global position");
                    Vec kalMClocal = siMod.toLocal(hitMCglobal);
                    //kalMClocal.print("true hit Kal local position");
                    //hitLocal.print("sim hit Kal local position");
                    double hitError = hitLocal.v[1] - kalMClocal.v[1];
                    if (Math.abs(hitError) > maxErr) maxErr = Math.abs(hitError);
                    aida.histogram1D("Kalman hit true error").fill(hitError);
                    aida.histogram1D("Kalman hit true error over uncertainty").fill(hitError/du);
                    aida.histogram1D(String.format("Layers/Kalman true error in layer %d",layer)).fill(hitError);
                    aida.histogram1D("MC hit z in local system (should be zero)").fill(kalMClocal.v[2]);
                }
            }
        }
        return maxErr;
    }
    // Find the MC true scattering angles at tracker layers
    // Note, this doesn't work, because the MC true momentum saved is neither the incoming nor outgoing momentum but rather
    // some average of the two.
    private void simScatters(EventHeader event) {
        String stripHitInputCollectionName = "TrackerHits";
        List<SimTrackerHit> striphits = event.get(SimTrackerHit.class, stripHitInputCollectionName);
        if (striphits == null) return;
        
        // Make a map from MC particle to the hit in the layer
        Map<Integer, ArrayList<SimTrackerHit>> hitMCparticleMap = new HashMap<Integer, ArrayList<SimTrackerHit>>();
        for (SimTrackerHit hit1D : striphits) {
            decoder.setID(hit1D.getCellID());
            int Layer = decoder.getValue("layer") + 1;
            //int Module = decoder.getValue("module");
            //MCParticle MCpart = hit1D.getMCParticle();
            ArrayList<SimTrackerHit> partInLayer = null;
            if (hitMCparticleMap.containsKey(Layer)) {
                partInLayer = hitMCparticleMap.get(Layer);
            } else {
                partInLayer = new ArrayList<SimTrackerHit>();
            }
            partInLayer.add(hit1D);
            hitMCparticleMap.put(Layer,partInLayer);
        }
        
        // Now analyze each MC hit, except those in the first and last layers
        for (int Layer=2; Layer<11; ++Layer) {
            if (hitMCparticleMap.containsKey(Layer) && hitMCparticleMap.containsKey(Layer-1)) {
                hit2Loop :
                for (SimTrackerHit hit2 : hitMCparticleMap.get(Layer)) {
                    MCParticle MCP2 = hit2.getMCParticle();
                    double Q2 = MCP2.getCharge();
                    Vec p2 = KalmanInterface.vectorGlbToKalman(hit2.getMomentum());
                    for (SimTrackerHit hit1 : hitMCparticleMap.get(Layer-1)) {
                        Vec p1 = KalmanInterface.vectorGlbToKalman(hit2.getMomentum());  // reverse direction
                        MCParticle MCP1 = hit1.getMCParticle();
                        double Q1 = MCP1.getCharge();
                        double ratio = p2.mag()/p1.mag();
                        if (ratio > 0.98 && ratio <= 1.02 && Q1*Q2 > 0. && MCP1.equals(MCP2)) {
                            // Integrate through the B field from the previous layer to this layer to get the momentum
                            // before scattering. I'm assuming that the MC hit gives the momentum following the scatter.
                            // The integration distance is approximated to be the straight line distance, which should get
                            // pretty close for momenta of interest.
                            double dx = 1.0;
                            RungeKutta4 rk4 = new RungeKutta4(Q1, dx, fm);
                            Vec xStart = KalmanInterface.vectorGlbToKalman(hit1.getPosition());
                            Vec xEnd = KalmanInterface.vectorGlbToKalman(hit2.getPosition());
                            double straightDistance = xEnd.dif(xStart).mag();
                            double[] r= rk4.integrate(xStart, p1, straightDistance-dx);
                            dx = dx/100.0;
                            rk4 = new RungeKutta4(Q1, dx, fm);
                            straightDistance = xEnd.dif(new Vec(r[0],r[1],r[2])).mag();
                            r = rk4.integrate(new Vec(r[0],r[1],r[2]),  new Vec(r[3],r[4],r[5]), straightDistance-dx);
                            
                            Vec newPos = new Vec(r[0], r[1], r[2]);
                            Vec newMom = new Vec(r[3], r[4], r[5]);
                            System.out.format("KalmanPatRecDriver.simScatters: trial match at layer %d\n", Layer);
                            xEnd.print("MC point on layer");
                            newPos.print("integrated point near layer");
                            p2.print("MC momentum at layer");
                            newMom.print("integrated momentum near layer");
                            double ctScat = p2.dot(newMom)/p2.mag()/newMom.mag();
                            double angle = Math.acos(ctScat);
                            System.out.format("  Angle between incoming and outgoing momenta=%10.6f\n", angle);
                            continue hit2Loop;
                        }
                    }
                }
            }
        }    
    }
    
    void printKalmanKinks(KalTrack tkr) {
        //for (MeasurementSite site : tkr.SiteList) {
        //    if (verbose) {
        //        System.out.format("Layer %d Kalman filter lambda-kink= %10.8f  phi-kink=  %10.8f\n", site.m.Layer, site.scatZ(), site.scatX());
        //    }
        //}
        for (int layer = 1; layer < 12; layer++) {
            aida.histogram1D(String.format("Kalman lambda kinks for layer %d", layer)).fill(tkr.scatZ(layer));
            aida.histogram1D(String.format("Kalman phi kinks for layer %d", layer)).fill(tkr.scatX(layer));
            if (verbose) {
                System.out.format("Layer %d Kalman smoother lambda-kink= %10.8f  phi-kink= %10.8f\n", layer, tkr.scatZ(layer),
                        tkr.scatX(layer));
            }
        }
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

    @Override
    public void endOfData() {
        System.out.println("Individual layer efficiencies:");
        for (int lyr=0; lyr<14; lyr++) {
            double effic = (double)nHitsOnTracks[lyr]/(double)nTracks;
            System.out.format("   Layer %d, hit efficiency = %9.3f\n", lyr, effic);
        }
        if (outputPlots != null) {
            try {
                System.out.println("Outputting the aida histograms now.");
                aida.saveAs(outputPlots);
            } catch (IOException ex) {
                Logger.getLogger(TrackingReconstructionPlots.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
