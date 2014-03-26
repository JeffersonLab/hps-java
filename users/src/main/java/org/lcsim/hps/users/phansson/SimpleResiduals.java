/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lcsim.hps.users.phansson;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotter;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.recon.ecal.HPSEcalCluster;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.SiSensorElectrodes;
import org.lcsim.detector.tracker.silicon.SiStrips;
import org.lcsim.detector.tracker.silicon.SiTrackerIdentifierHelper;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;
import org.lcsim.fit.helicaltrack.HelixUtils;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.IDDecoder;
import org.lcsim.geometry.Subdetector;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.recon.tracking.seedtracker.SeedCandidate;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author phansson
 */
public class SimpleResiduals extends Driver {
    
    int nevents = 0;
    private boolean debug = false;
    private boolean doFastTracking = false;
    private boolean doDefaultResiduals = false;
    protected IDDecoder dec = null;
    protected Subdetector ecal;
    private String ecalName = "Ecal";
    double crystalX = (13.3 + 16.0) / 2;
    double crystalY = (13.3 + 16.0) / 2;
    double beamGap = 20.0;
    private double EcalZPosition = 1370.0;
    
    
    
    private String outputPlotFileName;
    
    private AIDA aida = AIDA.defaultInstance();
    private IAnalysisFactory af = aida.analysisFactory();
    private IHistogram1D phi0;
    private IHistogram1D z0;
    private IHistogram1D slope;
    private IHistogram1D theta;
    private IHistogram1D resx_simple_TrackHit;
    private IHistogram1D resx_simple_Strip0;
    private IHistogram1D resx_simple_Strip1;
    private IHistogram1D resy_simple_TrackHit;
    private IHistogram1D resy_simple_Strip0;
    private IHistogram1D resy_simple_Strip1;
    private IHistogram1D resz_simple_TrackHit;
    private IHistogram1D resz_simple_Strip0;
    private IHistogram1D resz_simple_Strip1;
    
    //FastTrack residuals
    private List< List<IHistogram1D> > resy_org = new ArrayList<List<IHistogram1D> >();
    private List< List<IHistogram1D> > resy_org_layallhit = new ArrayList<List<IHistogram1D> >();
    private List< List<IHistogram1D> > resy_org_lay1hit = new ArrayList<List<IHistogram1D> >();
    
    private List< IHistogram1D> nhits_tracker = new ArrayList<IHistogram1D>();
    
    
    public void startOfData() {
        System.out.println("startOfData called");
        if( debug ) 
            System.out.println("Debug ON");
        else 
            System.err.println("Debug OFF");
    }
    
    public void detectorChanged(Detector detector) {
	// Get the Subdetector.
	ecal = detector.getSubdetector(ecalName);

	// Cache ref to decoder.
	dec = ecal.getIDDecoder();
    }
    
    public SimpleResiduals() {
        System.out.println("Empty HPSRunAlignment constructor called");
        IHistogramFactory hf = aida.histogramFactory();
        //Histograms
        phi0 = hf.createHistogram1D("phi0", 50, 0, 2*Math.PI);
        z0 = hf.createHistogram1D("z0", 50, -10, 10);
        slope = hf.createHistogram1D("slope", 50, -0.1, 0.1);
        theta = hf.createHistogram1D("theta", 50, -1*Math.PI, Math.PI);
        resx_simple_TrackHit = hf.createHistogram1D("resx_simple_TrackHit", 50, -10, 10);
        resy_simple_TrackHit = hf.createHistogram1D("resy_simple_TrackHit", 50, -20, 20);
        resz_simple_TrackHit = hf.createHistogram1D("resz_simple_TrackHit", 50, -10, 10);
        
        resx_simple_Strip0 = hf.createHistogram1D("resx_simple_Strip0", 50, -10, 10);
        resy_simple_Strip0 = hf.createHistogram1D("resy_simple_Strip0", 50, -100, 100);
        resz_simple_Strip0 = hf.createHistogram1D("resz_simple_Strip0", 50, -10, 10);

        resx_simple_Strip1 = hf.createHistogram1D("resx_simple_Strip1", 50, -10, 10);
        resy_simple_Strip1 = hf.createHistogram1D("resy_simple_Strip1", 50, -100, 100);
        resz_simple_Strip1 = hf.createHistogram1D("resz_simple_Strip1", 50, -10, 10);
        
        System.out.println("Creating resy histos");
        for (int iSide=0;iSide<2;++iSide) {
            String side;
            if(iSide==0) side="up";
            else side="down";
            System.out.println("Creating resy histos for side  " + side + "(size: " + resy_org.size() + ")");
            List<IHistogram1D> list  = new ArrayList<IHistogram1D>();
            resy_org.add(list);
            List<IHistogram1D> listLayAllHit  = new ArrayList<IHistogram1D>();
            resy_org_layallhit.add(listLayAllHit);
            List<IHistogram1D> listLay1Hit  = new ArrayList<IHistogram1D>();
            resy_org_lay1hit.add(listLay1Hit);
            
            nhits_tracker.add(hf.createHistogram1D("FT_nhits_tracker_" + side , 20, 0, 20));
            
            System.out.println("Creating resy histos for side " + iSide + "(size: " + resy_org.size() + ")");
            for (int iLayer=1;iLayer<6;++iLayer) {
                System.out.println("add iSide " + iSide + " iLayer " + iLayer);
                
                IHistogram1D h = hf.createHistogram1D("FT_resy_org_" + side + "_l"+iLayer, 50, -25, 25);
                list.add(h);

                IHistogram1D hAll = hf.createHistogram1D("FT_resy_org_LayAllHit_" + side + "_l"+iLayer, 50, -25, 25);
                listLayAllHit.add(hAll);
                
                IHistogram1D hLay1 = hf.createHistogram1D("FT_resy_org_lay1hit_" + side + "_l"+iLayer, 50, -25, 25);
                listLay1Hit.add(hLay1);
                
            }
        }
    
        
    }
    
    List<SiTrackerHitStrip1D> getAllHits(EventHeader event) {
        List<SiTrackerHitStrip1D> stripHits = event.get(SiTrackerHitStrip1D.class, "StripClusterer_SiTrackerHitStrip1D");
        if(debug) System.out.println("Got " + stripHits.size() + " SiTrackerHitStrip1D in this event");
        return stripHits;
    }
    
    
    List<TrackerHit> getAllHitsFromTracks(EventHeader event) {
        List<Track> trackList = event.get(Track.class, "MatchedTracks");
        
        if( debug )
            System.out.println("There are " +  trackList.size() + " tracks in \"MatchedTrack\"");
        
        
        //Loop over all tracks and find the hits
        List<TrackerHit> hits = new ArrayList<TrackerHit>();
        int ntracks = 0;
        for ( Track track : trackList ) {
            List<TrackerHit> trackerHits = track.getTrackerHits();
            for ( TrackerHit h : trackerHits ) {
                
                hits.add(h);
            }
            if ( debug ) { 
                System.out.println("Track " + ntracks + " has " + trackerHits.size() + " hits -> added " + hits.size() + " so far.");
            }
        }
        return hits;
    }
    
    List<SiTrackerHitStrip1D> getAllHitsInEvent(EventHeader event) {
        //First a workaround - use hits from track.. 
        //List<TrackerHit> hits = getAllHitsFromTracks(event);
        List<SiTrackerHitStrip1D> hits = getAllHits(event);
        if( debug ) {
            System.err.println("Found " + hits.size() + " hits");
        }
        return hits;
    }
    
    private Hep3Vector getFastTrackOrigin(EventHeader event) {
        double tx = 0.0;
        double ty = 0.0;
        double tz = 0.0;
        Hep3Vector pos = new BasicHep3Vector(tx,ty,tz);
                
        return pos;
    }
        
    private List<HPSEcalCluster> getAllEcalClusters(EventHeader event) {
        
        List<HPSEcalCluster> clusters = event.get(HPSEcalCluster.class, "EcalClusters"); 
        if ( debug) {
            System.out.println("Found " + clusters.size() + " clusters");
        }
        return clusters;
    }

    private double[] getClusterPosition(HPSEcalCluster cluster) {
        CalorimeterHit hit = cluster.getSeedHit();

        //IDDecoder dec = dec.getSubdetector("Ecal").getIDDecoder();
        dec.setID(hit.getCellID());
        int ix = dec.getValue("ix");
        int iy = dec.getValue("iy");
        double position[] = new double[2];
        position[0] = crystalX * ix;
        position[1] = crystalY * iy + beamGap * Math.signum(iy);
        return position; 
    }

    
    private List<Hep3Vector> getEcalClustersForFastTracking(List<HPSEcalCluster> clusters, String side) {
        if(!side.equalsIgnoreCase("up") && !side.equalsIgnoreCase("down")) {
            throw new RuntimeException("This ecal side" + side + " do not exist!!");
        }
        List<Hep3Vector> cls = new ArrayList<Hep3Vector>();
        boolean save;
        double [] pos_xy;
        for ( HPSEcalCluster cl : clusters) {
            save=false;
            pos_xy = getClusterPosition(cl);
            if(pos_xy[1]>=0 && side.equalsIgnoreCase("up")) {
                save=true;
            } else if(pos_xy[1]<0 && side.equalsIgnoreCase("down")) {
                save=true;
            } 
            if(save) cls.add(new BasicHep3Vector(pos_xy[0],pos_xy[1],EcalZPosition));
        }
        return cls;
    }
     
    private Hep3Vector selectCluster(List<Hep3Vector> clusters) {
        //need to decide which cluster to take
        if (clusters.size()>0) {
            Hep3Vector pos = clusters.get(0);
            return pos;
        } else {
            throw new RuntimeException("No cluser positions to choose from!");
        }
        
    }

    private List<HPSEcalCluster> getAllEcalClustersForFastTracking(EventHeader event) {
        
        List<HPSEcalCluster> clusters = event.get(HPSEcalCluster.class, "EcalClusters"); 
        if ( debug) {
            System.out.println("Found " + clusters.size() + " clusters");
        }
        
     
        return clusters;
    }

    
    public void fastTracking(EventHeader event) {
        
        if ( debug) {
            System.out.println("Running fast tracking on this event");
        }
        
        //Get list of all Hits in the event
        List<SiTrackerHitStrip1D> trackerHits = getAllHitsInEvent(event);
        
        //Get the calorimeter cluster object used to construct the track
        List<HPSEcalCluster> ecal_all_clusters = getAllEcalClustersForFastTracking(event);
        //Exit if no clusters found
        if (ecal_all_clusters.size()==0) return;

        String sides[] = {"up","down"};
        int nhits;
        int nhitsInTracker;
        int nhitsInLayer1;
        Hep3Vector ecal_cl;
        Hep3Vector origin_pos;
        List<Hep3Vector> ecal_cls;
        FastTrack fastTrack = new FastTrack(debug);
        List<SiTrackerHitStrip1D> stripList;
        boolean isaxial;
        String name;
        SiSensor siSensor;
        int layer;
        String si_side;
        double res;
        for (int iSide=0;iSide<2;++iSide) {
            
            if(debug) System.out.println("Side: " + sides[iSide]);
            
            ecal_cls = getEcalClustersForFastTracking(ecal_all_clusters, sides[iSide]);
            if (debug) System.out.println("This side has " + ecal_cls.size() + " clusters");
            if( ecal_cls.size() ==0 ) {
                System.out.println("No clusters...");
                continue;
            }
            ecal_cl = selectCluster(ecal_cls);  
                        
            //Get "target" position i.e. the origin of the radiation
            origin_pos = getFastTrackOrigin(event);
            
            //Create the fast track 
            fastTrack.setTrack(origin_pos, ecal_cl);
            
            if ( debug ) System.out.println(fastTrack.toString());
           
        
            nhits = 0;
            nhitsInTracker = getNLayersWithAxialHit(trackerHits,sides[iSide]);
            //most upstream layer nr is different for top and bottom
            int firstLayer = 1;
            if (sides[iSide] == "down") firstLayer=2;
            nhitsInLayer1 = getNAxialHitsInLayers(trackerHits,sides[iSide],firstLayer);
            
            nhits_tracker.get(iSide).fill(nhitsInTracker);
            
            stripList = new ArrayList<SiTrackerHitStrip1D>();
            for ( SiTrackerHitStrip1D stripCluster : trackerHits ) {
                
                isaxial = isAxialHit(stripCluster);
                
                if( isaxial == false) continue;
                
                siSensor = stripCluster.getSensor();
                name = siSensor.getName();
                if ( name.length() < 14) {
                    System.err.println("This name is too short!!");
                    throw new RuntimeException("SiSensor name " + name + " is invalid?");
                }
                
                
                layer = getLayerFromSensorName(name);
                
                si_side = getSideFromSiCluster(stripCluster);
                
                if ( debug ) {
                    System.out.println("hit " + nhits + " on " + si_side + " side of detector");
                    System.out.println("SiSensor layer " + layer + "(" + name + ")");
                    System.out.println("isAxial? " + isaxial);
                }

                
                if( sides[iSide] == si_side) {
                    System.out.println("This hit is same side as Ecal cluster side");
                } else {
                     System.out.println("This hit is opposite side side as Ecal cluster side -> skip!");
                     continue;
                } 
                
                res = fastTrack.getFastTrackResidual(stripCluster);
            
//                1 2 3 4 5 6 7 8 9 10
//                0   1   2   3   4
//                  0   1   2   3   4
               
                // Fix the layer for the list index
                int layerIndex = layer;
                if ( layer % 2 ==0 ) layerIndex=layer/2-1; 
                else layerIndex = (layer-1)/2;
                resy_org.get(iSide).get(layerIndex).fill(res);
                
                if (nhitsInTracker==5) {
                    resy_org_layallhit.get(iSide).get(layerIndex).fill(res);
                }
                
                if (nhitsInLayer1>0) {
                    resy_org_lay1hit.get(iSide).get(layerIndex).fill(res);
                }
            
                ++nhits;
            }
        
            if ( debug ) System.out.println("Processed " + nhits + " for this side");
        }
       return;
    }
    
     
    private boolean isAxialHit(SiTrackerHitStrip1D strip) {
        boolean axial = false;
        Hep3Vector m = strip.getMeasuredCoordinate();
        System.out.println("y " + m.y() + " -> abs(y) " + Math.abs(m.y()) );
        if ( Math.abs((Math.abs(m.y())-1.0))<0.0000000001 ) {
            axial = true;
            System.out.println( " ===> " + axial );
        }
        return axial;
    }
        
    private int getNLayersWithAxialHit(List<SiTrackerHitStrip1D> trackerHits, String side) {
        int nhits=0;
        for ( SiTrackerHitStrip1D stripCluster : trackerHits ) {
            
            if(isAxialHit(stripCluster)) {
                String si_side = getSideFromSiCluster(stripCluster);
                
                if( side == si_side) {
                    ++nhits;
                }
            }
        }
                          
        return nhits;
    }   
    
     private int getNAxialHitsInLayers(List<SiTrackerHitStrip1D> trackerHits, String side, int layer) {
        int nhits=0;
        String si_side;
        SiSensor siSensor;
        String name;
        int l;
        for ( SiTrackerHitStrip1D stripCluster : trackerHits ) {
            
            if(isAxialHit(stripCluster)==false) continue;
           
            si_side = getSideFromSiCluster(stripCluster);

            if( side == si_side) {
                
                siSensor = stripCluster.getSensor();
                name = siSensor.getName();
                if ( name.length() < 14) {
                    System.err.println("This name is too short!!");
                    throw new RuntimeException("SiSensor name " + name + " is invalid?");
                }
            
                l = getLayerFromSensorName(name);
                if ( l == layer) ++nhits;
            
            } 
        }
                          
        return nhits;
    }   
    
     private String getSideFromSiCluster(SiTrackerHitStrip1D stripCluster) {       
        Hep3Vector posVec = stripCluster.getPositionAsVector();
        double yHit = posVec.y();
        String side;
        if (yHit>=0.0) side = "up";
        else side = "down";
        return side;
    }

    
    private int getLayerFromSensorName(String name) {
            int ilayer = -1;// = Integer.parseInt(layer.substring(layer.length()-1));
            if (name.contains("layer")) {
                
                String l = name.substring(13);
                l = l.substring(0, l.indexOf("_"));
                System.out.println("layer: " + l + " (length: " + l.length() + ")");
                //String ll = l.subs
                //System.out.println("ll: " + ll);
                ilayer = Integer.parseInt(l);
                System.out.println("il: " + ilayer);
                if ( ilayer < 1 || ilayer > 10 ) {
                    System.err.println("This layer doesn't exist?");
                    throw new RuntimeException("SiSensor name " + name + " is invalid?");
                }
                
            } else {
                throw new RuntimeException("This sensor name do not have a layer!");
            }
            
            
            return ilayer;
    }
    
    public void process(EventHeader event) {
        ++nevents;
        if( debug ) {
            System.out.println("Processing event " + nevents);
            
        }
        
        if ( doFastTracking ) {
            System.out.println("doFastTracking ");
            fastTracking(event);
        } 
        
        //if ( doDefaultResiduals ) {
        if ( 1==0 ) {
            if( debug )     
                System.out.println("Do normal residuals");
            
            //Get list of tracks
            List<Track> trackList = event.get(Track.class, "MatchedTracks");
            
            if( debug )
                System.out.println(trackList.size() + " tracks in \"MatchedTrack\"");
        
            //Loop over all tracks and calculate Millipede input
            int ntracks = 0;
            for ( Track track : trackList ) {
                if ( debug ) { 
                    System.out.println("Track " + ntracks);
                    printTrackBasicInfo(track);
                }
                
                fillTrackInfo(track);
                //Calculate residuals
                calcResiduals(track);
            
            
                ++ntracks;
            }
        
            if ( debug )
                System.out.println("Processed " + ntracks + " in this event");
        }
    }
    
    
    public void getHitInfo( SiTrackerHitStrip1D stripCluster) {
        SiSensor sensor = stripCluster.getSensor();
        SiTrackerIdentifierHelper sid_helper = stripCluster.getIdentifierHelper();
        List<RawTrackerHit> raw_hits = stripCluster.getRawHits();
        if (debug) {
            System.out.println("This stripCluster has " + raw_hits.size() + ":");
            System.out.println(raw_hits.toString());
            System.out.println("Loop and print info on all raw hits");
        }
        for (RawTrackerHit raw_hit : raw_hits) {
            SiSensorElectrodes electrodes = sensor.getReadoutElectrodes(ChargeCarrier.HOLE);
            IIdentifier id = raw_hit.getIdentifier();
            Integer strip_id = sid_helper.getElectrodeValue(id);
            Hep3Vector local_pos = ((SiStrips) electrodes).getStripCenter(strip_id);
            electrodes.getParentToLocal().inverse().transform(local_pos);
            Hep3Vector global_pos = ((SiSensor) electrodes.getDetectorElement()).getGeometry().getLocalToGlobal().transformed(local_pos);
            double ypos = global_pos.y();
            double zpos = global_pos.z();
            System.out.println("id  " + id + " strip_id " + strip_id + " local_pos " + local_pos.toString() + " global_pos " + global_pos.toString() + " ypos " + ypos + " zpos " + zpos);
        }
    }
    
    
    public void fillTrackInfo( Track track) {
        aida.histogram1D("phi0").fill(track.getTrackParameter(HelicalTrackFit.phi0Index));    
        aida.histogram1D("z0").fill(track.getTrackParameter(HelicalTrackFit.z0Index));
        aida.histogram1D("slope").fill(track.getTrackParameter(HelicalTrackFit.slopeIndex));
        double theta = Math.atan(1.0/track.getTrackParameter(HelicalTrackFit.slopeIndex));
        aida.histogram1D("theta").fill(theta);
    }
    
    public void displayTrackPlots() {
        IPlotter plotter = af.createPlotterFactory().create();
        //IPlotter plotter = af.createPlotterFactory(af.createTreeFactory().create());

        plotter.createRegions(2,2,0);
        plotter.region(0).plot(phi0);
        plotter.region(1).plot(z0);
        plotter.region(2).plot(slope);
        plotter.region(3).plot(theta);
        plotter.show();
    }
    
    public void calcResiduals(Track track) {
        //Calculates residuals of the hits on the fitted track
        List<TrackerHit> trackerHits = track.getTrackerHits();
        int nhits = 0;
        for ( TrackerHit hit : trackerHits ) {
            //Get the strips
            HelicalTrackHit helicalTrackHit = (HelicalTrackHit) hit;
            HelicalTrackCross helicalTrackCross = (HelicalTrackCross) helicalTrackHit;
            List<HelicalTrackStrip> helicalStrips = helicalTrackCross.getStrips();
            HelicalTrackStrip s0 = helicalStrips.get(0);
            HelicalTrackStrip s1 = helicalStrips.get(1);
            //Get the position
            Hep3Vector pos = helicalTrackHit.getCorrectedPosition();
            Hep3Vector pos0 = getStripGlobalPos(s0);
            Hep3Vector pos1 = getStripGlobalPos(s1);
            
            if ( debug ) {
                System.out.println("hit " + nhits);
                System.out.println("HelicalTrackHit corr pos:\n" + pos.toString());
                System.out.println("Strip 0 global pos:" + pos0.toString());
                System.out.println("Strip s0:");
                System.out.println(s0.toString());
               
                System.out.println("Strip 1 global pos:" + pos1.toString());
                System.out.println("Strip s1:");
                System.out.println(s1.toString());
            }
            
            //Simple residual calculation
            fillSimpleResiduals(track,pos,"TrackHit");
            fillSimpleResiduals(track,pos0,"Strip0");
            fillSimpleResiduals(track,pos1,"Strip1");
            
            ++nhits;
        }
    }
    
    Hep3Vector getTrackPosAtHit(Track track, Hep3Vector hit) {
        //Find the position of the track at the hit
        double x_origin = hit.x();
        double smax = 1e3; //not used
        int mxint = 5; // not used (five track parameters )
        SeedTrack st = (SeedTrack) track;
        SeedCandidate seed = st.getSeedCandidate();
        HelicalTrackFit hel_track = seed.getHelix();
        List<Double> pathlength_xyplane = HelixUtils.PathToXPlane(hel_track, x_origin,smax, mxint);
        Hep3Vector track_pos = HelixUtils.PointOnHelix(hel_track, pathlength_xyplane.get(0));
        return track_pos;
    }
    
    public void fillSimpleResiduals(Track track, Hep3Vector hit, String name) {
        Hep3Vector trackPosAtHit = getTrackPosAtHit(track,hit);
        double res[] = new double[3];
        res[0]=trackPosAtHit.x()-hit.x();
        res[1]=trackPosAtHit.y()-hit.y();
        res[2]=trackPosAtHit.z()-hit.z();
        if (name == "TrackHit") { 
            aida.histogram1D("resx_simple_TrackHit").fill(res[0]);
            aida.histogram1D("resy_simple_TrackHit").fill(res[1]);
            aida.histogram1D("resz_simple_TrackHit").fill(res[2]);
        } else if(name == "Strip0") {
            aida.histogram1D("resx_simple_Strip0").fill(res[0]);
            aida.histogram1D("resy_simple_Strip0").fill(res[1]);
            aida.histogram1D("resz_simple_Strip0").fill(res[2]);
        } else if(name == "Strip1") {
            aida.histogram1D("resx_simple_Strip1").fill(res[0]);
            aida.histogram1D("resy_simple_Strip1").fill(res[1]);
            aida.histogram1D("resz_simple_Strip1").fill(res[2]);
        } else {
            System.out.println("Histograms not defined: " + name);
        }
   
    }
        
    public void displaySimpleResPlots() {
        IPlotter plotter = af.createPlotterFactory().create();
        //IPlotter plotter = af.createPlotterFactory(af.createTreeFactory().create());
        plotter.createRegions(3,3,0);
        plotter.region(0).plot(resx_simple_TrackHit);
        plotter.region(1).plot(resy_simple_TrackHit);
        plotter.region(2).plot(resz_simple_TrackHit);
        plotter.region(3).plot(resx_simple_Strip0);
        plotter.region(4).plot(resy_simple_Strip0);
        plotter.region(5).plot(resz_simple_Strip0);
        plotter.region(6).plot(resx_simple_Strip1);
        plotter.region(7).plot(resy_simple_Strip1);
        plotter.region(8).plot(resz_simple_Strip1);
        plotter.show();
    }
    
   public void displayFastTrackingPlots() {
        //IPlotter plotter = af.createPlotterFactory(af.createTreeFactory().create());
        IPlotter plotter_org = af.createPlotterFactory().create();
        plotter_org.createRegions(5,2,0);
        IPlotter plotter_org_LayAllHit = af.createPlotterFactory().create();
        plotter_org_LayAllHit.createRegions(5,2,0);
        IPlotter plotter_org_Lay1Hit = af.createPlotterFactory().create();
        plotter_org_Lay1Hit.createRegions(5,2,0);
        
        for ( int iSide=0;iSide<2;++iSide) {
            int nLayers = resy_org.get(iSide).size();
            for (int iLayer=0;iLayer<nLayers;++iLayer) {
                int idx = 2*iLayer+iSide;
                
                //0 1 2 3 4
                //1 3 5 7 9
                
                plotter_org.region(idx).plot(resy_org.get(iSide).get(iLayer));
                plotter_org_LayAllHit.region(idx).plot(resy_org_layallhit.get(iSide).get(iLayer));
                plotter_org_Lay1Hit.region(idx).plot(resy_org_lay1hit.get(iSide).get(iLayer));
            }
        }
        plotter_org.show();
        plotter_org_LayAllHit.show();
        plotter_org_Lay1Hit.show();
        
        IPlotter plotter_hitmult = af.createPlotterFactory().create();
        plotter_hitmult.createRegions(2,1,0);
        plotter_hitmult.region(0).plot(nhits_tracker.get(0));
        plotter_hitmult.region(1).plot(nhits_tracker.get(1));
        plotter_hitmult.show();
        
        
    }
    
    
    
    Hep3Vector getStripGlobalPos(HelicalTrackStrip strip) {
        Hep3Vector origin0 = strip.origin();       
        Hep3Vector u = strip.u();
        Hep3Vector v = strip.v();
        double umeas = strip.umeas();    
        Hep3Vector pos = new BasicHep3Vector(origin0.x()+u.x()*umeas+v.x(),origin0.y()+u.y()*umeas+v.y(),origin0.z()+u.z()*umeas+v.z());
        //pos = origin + umeas * uhat + v * vhat
        return pos;
    }
    
    public void printTrackBasicInfo(Track track) {
        System.out.println("--Basic track info--");
        System.out.println(track.toString());
    }
    
    public void endOfData() {
        System.out.println("endOfData called");
        try {
            aida.saveAs(outputPlotFileName);
        } catch (IOException ex) {
            Logger.getLogger(FastTrackResidualDriver.class.getName()).log(Level.SEVERE, "Couldn't save aida plots to file " + outputPlotFileName, ex);
        }
        if (doFastTracking) {
            displayFastTrackingPlots();
        }
        if (0==1) {
        //if (doDefaultResiduals) {
            displayTrackPlots();
            displaySimpleResPlots();
        }
    }

    public void setDebug(boolean debug) {
        this.debug = true;
    }

    public void setdoFastTracking(boolean debug) {
        this.doFastTracking = true;
    }
    
    public void setdoDefaultResiduals(boolean debug) {
        this.doDefaultResiduals = true;
    }
    public void setOutputPlotFileName( String name ) {
        this.outputPlotFileName = name;
    }

    public void setEcalZPosition(double val) {
        this.EcalZPosition = val;
    }
    
}
