/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.users.phansson;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;
import hep.aida.ref.plotter.PlotterRegion;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.recon.ecal.HPSEcalCluster;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.IDDecoder;
import org.lcsim.geometry.Subdetector;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author phansson+
 */
public class FastTrackResidualDriver extends Driver {
    
    int nevents = 0;
    boolean debug = false;
    boolean saveFile = false;
    protected IDDecoder dec = null;
    protected Subdetector ecal;
    private String ecalName = "Ecal";
    String sides[] = {"up","down"};
    double crystalX;
    double crystalY;
    double beamGap;
    double EcalZPosition;
    double conversionZ;
    private static int crystalCols;
    private static int crystalRows;
    double ecalBeamgapCorr;
    int ecalClusterSel;

    private String outputPlotFileName = "";
    
    private AIDA aida = AIDA.defaultInstance();
    private IAnalysisFactory af = aida.analysisFactory();
    private List< List<IHistogram1D> > resy_org = new ArrayList<List<IHistogram1D> >();
    private List< List<IHistogram1D> > resy_org_layallhit = new ArrayList<List<IHistogram1D> >();
    private List< List<IHistogram1D> > resy_org_layallsinglehit = new ArrayList<List<IHistogram1D> >();
    private List< List<IHistogram1D> > resy_org_lay1hit = new ArrayList<List<IHistogram1D> >();
    private List< IHistogram1D > nhits_tracker = new ArrayList<IHistogram1D>();
    private List< List<IHistogram1D> > nhits_tracker_layer = new ArrayList<List<IHistogram1D> >();
    private List< IHistogram1D > ncl_ecal = new ArrayList<IHistogram1D>();
    private List< IHistogram1D > selcl_ecal_e = new ArrayList<IHistogram1D>(); 
    private List< IHistogram1D > cl_ecal_e = new ArrayList<IHistogram1D>();
    private List< IHistogram2D > ncl_ecal_map = new ArrayList<IHistogram2D>(); 
    private List< IHistogram2D > nselcl_ecal_map = new ArrayList<IHistogram2D>(); 
    private List< IHistogram2D> resy_2d_org_layallhit = new ArrayList<IHistogram2D>();
    private List< IHistogram2D> resy_2d_org_layallsinglehit = new ArrayList<IHistogram2D>();
    
    
    public void startOfData() {
    }
    
    public void detectorChanged(Detector detector) {
	// Get the Subdetector.
	ecal = detector.getSubdetector(ecalName);

	// Cache ref to decoder.
	dec = ecal.getIDDecoder();
        
        //Ecal geometry
        crystalX = (13.3 + 16.0) / 2;
        crystalY = (13.3 + 16.0) / 2;
        beamGap = 20.0;
        EcalZPosition = 1370.0;
        crystalCols = 46;
        crystalRows = 5;
        
        ecalBeamgapCorr = 0.0;
        
        // Position of the conversion
        conversionZ = -1500.0; 
        
        ecalClusterSel = 0;
        
        IHistogramFactory hf = aida.histogramFactory();
        String side;
        for (int iSide=0;iSide<2;++iSide) {
            if(iSide==0) side="up";
            else side="down";
            
            ncl_ecal.add(hf.createHistogram1D("FT_ncl_ecal_" + side , 20, 0, 20));
            selcl_ecal_e.add(hf.createHistogram1D("FT_selcl_ecal_e" + side , 100, 0, 4000));
            cl_ecal_e.add(hf.createHistogram1D("FT_cl_ecal_e" + side , 100, 0, 4000));
           

            nhits_tracker.add(hf.createHistogram1D("FT_nhits_tracker_" + side , 15, 0, 15));

            List<IHistogram1D> nhl_list  = new ArrayList<IHistogram1D>();
            nhits_tracker_layer.add(nhl_list);
            for (int i=0;i<5;++i) {
                nhl_list.add(hf.createHistogram1D("FT_nhits_tracker_" + side + "_layer" + i , 15, 0, 15));
            }

                        
            double res_axis[] = {-40,40};

            
            resy_2d_org_layallhit.add(hf.createHistogram2D("FT_resy_2d_org_layallhit_" + side, 5, 0.5, 5.5, 60, res_axis[0], res_axis[1]));
            resy_2d_org_layallsinglehit.add(hf.createHistogram2D("FT_resy_2d_org_layallsinglehit_" + side, 5, 0.5, 5.5, 60, res_axis[0], res_axis[1]));
            
                   
            List<IHistogram1D> list  = new ArrayList<IHistogram1D>();
            resy_org.add(list);
            List<IHistogram1D> listLayAllHit  = new ArrayList<IHistogram1D>();
            resy_org_layallhit.add(listLayAllHit);
            List<IHistogram1D> listLay1Hit  = new ArrayList<IHistogram1D>();
            resy_org_lay1hit.add(listLay1Hit);  
            List<IHistogram1D> listLayAllSingleHit  = new ArrayList<IHistogram1D>();
            resy_org_layallsinglehit.add(listLayAllSingleHit);

                
            //Setup the ecal 2D plot
                        
            if (side == "up") {
                ncl_ecal_map.add(hf.createHistogram2D("FT_ecal_hitmap_" + side, 92, -46.0, 46.0, 5, 0.0, 5.0));
                nselcl_ecal_map.add(hf.createHistogram2D("FT_sel_ecal_hitmap_" + side, 92, -46.0, 46.0, 5, 0.0, 5.0));
            } else {
                ncl_ecal_map.add(hf.createHistogram2D("FT_ecal_hitmap_" + side, 92, -46.0, 46.0, 5, -5.0, 0.0));
                nselcl_ecal_map.add(hf.createHistogram2D("FT_sel_ecal_hitmap_" + side, 92, -46.0, 46.0, 5, -5.0, 0.0));
            }

            for (int iLayer=1;iLayer<6;++iLayer) {
                
                IHistogram1D h = hf.createHistogram1D("FT_res_" + side + "_l"+iLayer, 50, res_axis[0], res_axis[1]);
                list.add(h);

                IHistogram1D hAll = hf.createHistogram1D("FT_res_LayAllHit_" + side + "_l"+iLayer, 50,res_axis[0], res_axis[1]);
                listLayAllHit.add(hAll);
                
                IHistogram1D hLay1 = hf.createHistogram1D("FT_res_Lay1Hit_" + side + "_l"+iLayer, 50,res_axis[0], res_axis[1]);
                listLay1Hit.add(hLay1);
                
                IHistogram1D hAllSingle = hf.createHistogram1D("FT_res_LayAllSingleHit_" + side + "_l"+iLayer, 50,res_axis[0], res_axis[1]);
                listLayAllSingleHit.add(hAllSingle);
                
            }
        }
        
        
        displayFastTrackingPlots();
        
        
        
    }
    
    public FastTrackResidualDriver() {}
    

    public void setConversionZ(double z) {
        this.conversionZ = z;
    }
    
    public void setecalBeamgapCorr(double val) {
        this.ecalBeamgapCorr = val;
        System.out.println("beamgap corr set: "  + ecalBeamgapCorr);
    }

    public void setDebug(boolean flag) {
        this.debug = flag;
    }
    
    public void setOutputPlotFileName( String name ) {
        this.outputPlotFileName = name;
    }

    public void setEcalZPosition(double val) {
        this.EcalZPosition = val;
        System.out.println("EcalZPosition set: "  + EcalZPosition);
    }
    
    public void setEcalClusterSel(int id) {
        this.ecalClusterSel = id;
        System.out.println("ecalClusterSel set: "  + ecalClusterSel);
    }
    
    
    public void process(EventHeader event) {
        ++nevents;
        if( debug ) {
            System.out.println("Processing event " + nevents);    
        }
        
        fastTracking(event);
        
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

        //if ( 1==1 ) return;
        
        
        int nhits;
        int nhitsInTracker;
        int nhitsInLayer1;
        Hep3Vector origin_pos;
        List<Integer> ecal_cls;
        FastTrack fastTrack = new FastTrack(debug);
        fastTrack.setEcalBeamgapCorr(ecalBeamgapCorr); //Correct for geometry

        //List<SiTrackerHitStrip1D> stripList;
        boolean isaxial;
        String name;
        SiSensor siSensor;
        int layer;
        String si_side;
        double res;
        int selclids[];
        int sel_ecal_idx;
        int layerIndex = -1;
        for (int iSide=0;iSide<2;++iSide) {
            
        
            //if ( 1==1 ) return;
            
            ecal_cls = getEcalClustersForFastTracking(ecal_all_clusters, sides[iSide]);
            
            if (debug) {
                    System.out.println("Found  " + ecal_cls.size() +" Ecal clusters on the " + sides[iSide] + ": " + ecal_cls.toString());
            }
            
            
            //if ( 1==1 ) return;
            
            ncl_ecal.get(iSide).fill(ecal_cls.size());
            
            if( ecal_cls.size() ==0 ) {
                if(debug) System.out.println("No clusters on this side...");
                continue;
            }
            
            
            //Fill map of Ecal hits
            for( int icl=0; icl<ecal_cls.size(); ++icl) {
                if(debug) System.out.println("icl " + icl);
                int clid = ecal_cls.get(icl); 
                if(debug) System.out.println("clid " + clid + " all clusters size " + ecal_all_clusters.size());
                int clpos[] = getCrystalPair(ecal_all_clusters.get(clid));
                if(debug) System.out.println("clpos " + clpos[0] + "," + clpos[1]);
                ncl_ecal_map.get(iSide).fill(clpos[0], clpos[1]);
                if(debug) System.out.println("clpos " + clpos[0] + "," + clpos[1]);
                cl_ecal_e.get(iSide).fill(ecal_all_clusters.get(icl).getEnergy());
            }
            
            sel_ecal_idx = selectCluster(ecal_cls,ecal_all_clusters); 
            
            
            if (sel_ecal_idx < 0) {
                if (debug) System.out.println("No selected cluster!");
                continue;
            }
            
            if (debug) System.out.println("Selected clid " + sel_ecal_idx + " is cluster " + ecal_all_clusters.get(sel_ecal_idx).toString() + " and will be used as pointer of fast track ");
            
            
             
            selclids = getCrystalPair(ecal_all_clusters.get(sel_ecal_idx));
            nselcl_ecal_map.get(iSide).fill(selclids[0], selclids[1]);
            
            selcl_ecal_e.get(iSide).fill(ecal_all_clusters.get(sel_ecal_idx).getEnergy());
             
            //Get "target" position i.e. the origin of the radiation
            origin_pos = getFastTrackOrigin(event);
            
            if (debug) System.out.println("Conversion started at " + origin_pos.toString());
            
            //Create the fast track 
            fastTrack.setTrack(origin_pos,ecal_all_clusters.get(sel_ecal_idx).getPosition());
            

            if ( debug ) System.out.println(fastTrack.toString());
           
            
        
            nhits = 0;
            nhitsInTracker = getNAxialHits(trackerHits,sides[iSide]);
            
            if( debug ) System.out.println("There are " + nhitsInTracker + " hits on this side of the tracker");
            
            //most upstream layer nr is different for top and bottom
//            int firstLayer = 1;
//            if (sides[iSide] == "down") firstLayer=2;
//            nhitsInLayer1 = getNAxialHitsInLayers(trackerHits,sides[iSide],firstLayer);

            int nhitsInLayers[] = getNAxialHitsPerLayer(trackerHits,sides[iSide]);

            boolean allLayersHasHits = true;
            boolean allLayersHasSingleHit = true;
            for (int i=0;i<5;++i){
                if(nhitsInLayers[i]==0) allLayersHasHits = false;
                if(nhitsInLayers[i]!=1) allLayersHasSingleHit = false;
            }
            
            if( debug ) {
                System.out.println("Number of hits per layer:");
                for (int i=0;i<5;++i){
                    System.out.println("Layer " + (i+1) + " has " + nhitsInLayers[i] + " axial hits");                    
                }
            }
            
            nhits_tracker.get(iSide).fill(nhitsInTracker);
            for (int i=0;i<5;++i){
                nhits_tracker_layer.get(iSide).get(i).fill(nhitsInLayers[i]);
            }
            //stripList = new ArrayList<SiTrackerHitStrip1D>();
            for ( SiTrackerHitStrip1D stripCluster : trackerHits ) {
                System.out.println("cluster at " + stripCluster.getPositionAsVector().toString() + " " + stripCluster.getSensor().toString());
                
                isaxial = isAxialHit(stripCluster);
                
                if( isaxial == false) continue;
                
                siSensor = stripCluster.getSensor();
                name = siSensor.getName();
                
                if ( name.length() < 14) {
                    System.err.println("This name is too short!!");
                    throw new RuntimeException("SiSensor name " + name + " is invalid?");
                }
                
                
                layer = getLayerFromSiCluster(stripCluster);
                
                si_side = getSideFromSiCluster(stripCluster);
                
                if ( debug ) {
                    System.out.println("hit " + nhits + " in layer " + layer + " on " + si_side + " side of detector");
                }

                
                if( sides[iSide] != si_side) {
                     if (debug) System.out.println("This hit is opposite side side as Ecal cluster side -> skip!");
                     continue;
                } 
               
                if (debug) System.out.println("Hit and cluster both on " + sides[iSide] + " side -> use it!");
               
                
                res = fastTrack.getFastTrackResidual(stripCluster);
            
                if (debug) System.out.println("-->res " + res);
                
//                1 2 3 4 5 6 7 8 9 10
//                0   1   2   3   4
//                  0   1   2   3   4
               
                // Fix the layer for the list index -> ugly
                layerIndex = layer;
                if ( layer % 2 == 0 ) layerIndex=layer/2-1; 
                else layerIndex = (layer-1)/2;
                resy_org.get(iSide).get(layerIndex).fill(res);
                
               
                
                if (allLayersHasHits) {
                    resy_org_layallhit.get(iSide).get(layerIndex).fill(res);
                    resy_2d_org_layallhit.get(iSide).fill((double)layerIndex+1, res, 1);
                }
                if (allLayersHasSingleHit) {
                    resy_org_layallsinglehit.get(iSide).get(layerIndex).fill(res);
                    resy_2d_org_layallsinglehit.get(iSide).fill((double)layerIndex+1, res, 1);
                }
                
                if (nhitsInLayers[0]>0) {
                    resy_org_lay1hit.get(iSide).get(layerIndex).fill(res);
                }
            
                ++nhits;
            }
        
            if ( debug ) System.out.println("Processed " + nhits + " for this side");
        }
       return;
    }
    
    
    
    List<SiTrackerHitStrip1D> getAllHits(EventHeader event) {
        List<SiTrackerHitStrip1D> stripHits = event.get(SiTrackerHitStrip1D.class, "StripClusterer_SiTrackerHitStrip1D");
        if(debug) System.out.println("Got " + stripHits.size() + " SiTrackerHitStrip1D in this event");
        return stripHits;
    }
    
    
    List<SiTrackerHitStrip1D> getAllHitsInEvent(EventHeader event) {
        //First a workaround - use hits from track.. 
        //List<TrackerHit> hits = getAllHitsFromTracks(event);
        List<SiTrackerHitStrip1D> hits = getAllHits(event);
        return hits;
    }
    
    private Hep3Vector getFastTrackOrigin(EventHeader event) {
        double tx = 0.0;
        double ty = 0.0;
        Hep3Vector pos = new BasicHep3Vector(tx,ty,conversionZ);
                
        return pos;
    }
        
    
    
    
//    private List<HPSEcalCluster> getAllEcalClusters(EventHeader event) {
//        
//        List<HPSEcalCluster> clusters = event.get(HPSEcalCluster.class, "EcalReadoutHits"); 
//        //List<HPSEcalCluster> clusters = event.get(HPSEcalCluster.class, "EcalClusters"); 
//
//        if ( debug) {
//            System.out.println("Found " + clusters.size() + " clusters");
//        }
//        return clusters;
//    }

 
    public int[] getCrystalPair(HPSEcalCluster cluster) {
        int[] pos = new int[2];
        pos[0] = cluster.getSeedHit().getIdentifierFieldValue("ix");
        pos[1] = cluster.getSeedHit().getIdentifierFieldValue("iy");
        
        //System.out.println("cluster ix,iy " + pos[0] + "," + pos[1] + "    from pos  " + cluster.getSeedHit().getPositionVec().toString());
        return pos;
        //getCrystalPair(cluster.getPosition());
    }
    
    
    private List<Integer> getEcalClustersForFastTracking(List<HPSEcalCluster> clusters, String side) {
        if(side!="up" && side!="down") {
            throw new RuntimeException("This ecal side" + side + " do not exist!!");
        }
        List<Integer> cls = new ArrayList<Integer>();
        boolean save;
        int [] pos;
        
        for (int i=0;i<clusters.size();++i) {
            save=false;
            
            pos = getCrystalPair(clusters.get(i));
            
            if(pos[1]>=0 && side=="up") {
                save=true;
            } 
            if(pos[1]<0 && side=="down") {
                save=true;
            } 
            if(save) {
                //cls.add(new BasicHep3Vector(pos[0],pos[1],EcalZPosition));
                cls.add(i);
                //new BasicHep3Vector(pos[0],pos[1],pos[2]));
            }
            
        }
        return cls;
    }
     
    private int selectCluster(List<Integer> ids, List<HPSEcalCluster> clusters) {
        //need to decide which cluster to take

        
        if (clusters.size()==0) {
            throw new RuntimeException("No clusters to select from!!!");
        }

        if (ids.size()==0) {
            throw new RuntimeException("No idx to clusters to select from!!!");
        }
        
        if(debug) {
            System.out.println("Select among " + clusters.size() + " clusters restricted to indexes" + ids.toString());
        }
        
        int sel_id = -1;
        
        switch (ecalClusterSel) {
            case 1: 
                //Require at least 1000MeV cluster
                double E = -1.0;
                for (int i=0;i<ids.size();++i) {
                    if (clusters.get(ids.get(i)).getEnergy()>1000.0 && clusters.get(ids.get(i)).getEnergy()>E ) {
                        sel_id = ids.get(i);
                        E = clusters.get(ids.get(i)).getEnergy();
                    }
                }
                break;
            default:
                //Use the first in list
                if (ids.size()>0 && ids.get(0)<clusters.size()) {
                    sel_id = ids.get(0);
                }
                break;
        }
        if (debug) System.out.println("Selected ecal cluster id  " + sel_id);
        
        return sel_id;        
           
    }

    private List<HPSEcalCluster> getAllEcalClustersForFastTracking(EventHeader event) {
        
        //List<HPSEcalCluster> clusters = new ArrayList<HPSEcalCluster>();
        List<HPSEcalCluster> clusters = event.get(HPSEcalCluster.class, "EcalClusters"); 
        //List<HPSEcalCluster> clusters = event.get(HPSEcalCluster.class, "EcalReadoutHits"); 
        //List<HPSEcalCluster> clusters = event.get(HPSEcalCluster.class, "EcalCalHits"); 
        if ( debug) {
            System.out.println("Found " + clusters.size() + " EcalClusters");
        }
        
     
        return clusters;
    }


     
    private boolean isAxialHit(SiTrackerHitStrip1D strip) {
        String side = this.getSideFromSiCluster(strip);
        int layer = this.getLayerFromSiCluster(strip);
        return layer%2==0 ? false : true;
        
        //int ref_l = ref_helper.getValue(ref_id, "layer"); // 1-10; axial layers are odd layers; stereo layers are even
        //int ref_s = ref_helper.getValue(ref_id, "module"); // 0-1; module number is top or bottom
//        boolean axial = false;
//        Hep3Vector m = strip.getMeasuredCoordinate();
//        //System.out.println("y " + m.y() + " -> abs(y) " + Math.abs(m.y()) );
//        if ( Math.abs((Math.abs(m.y())-1.0))<0.0000000001 ) {
//            axial = true;
//            //System.out.println( " ===> " + axial );
//        }
//        return axial;
    }
        
    private int getNAxialHits(List<SiTrackerHitStrip1D> trackerHits, String side) {
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
            
                l = this.getLayerFromSiCluster(stripCluster);
                if ( l == layer) ++nhits;
            
            } 
        }
                          
        return nhits;
    }   
     
      public int[] getNAxialHitsPerLayer(List<SiTrackerHitStrip1D> trackerHits, String side) {
        int nhits=0;
        String si_side;
        SiSensor siSensor;
        String name;
        int l;
        int i;
        int n[] = {0,0,0,0,0};
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
                l = this.getLayerFromSiCluster(stripCluster);
                
                
                //Turn this into the layer scheme I use
                    i = ((l+1)/2) - 1;
                    n[i] = n[i] + 1;                    
//                if(side=="down") {
//                    i = ((l+1)/2) - 1;
//                    n[i] = n[i] + 1;                    
//                } else {
//                    //even numbers are used
//                    i = (l/2) - 1;
//                    n[i] = n[i] + 1;
//                }
            
            } 

        }
                          
        return n;
    }   
    
     private String getSideFromSiCluster(SiTrackerHitStrip1D stripCluster) {       
         IIdentifierHelper ref_helper = stripCluster.getIdentifierHelper();
         IIdentifier ref_id = stripCluster.getSensor().getIdentifier();
                int ref_l = ref_helper.getValue(ref_id, "layer"); // 1-10; axial layers are odd layers; stereo layers are even
                int ref_s = ref_helper.getValue(ref_id, "module"); // 0-1; module number is top or bottom
                return ref_s==0 ? "up" : "down";
////        Hep3Vector posVec = stripCluster.getPositionAsVector();
////        double yHit = posVec.y();
////        String side;
////        if (yHit>=0.0) side = "up";
////        else side = "down";
////        return side;
    }

     private int getLayerFromSiCluster(SiTrackerHitStrip1D stripCluster) {       
         IIdentifierHelper ref_helper = stripCluster.getIdentifierHelper();
         IIdentifier ref_id = stripCluster.getSensor().getIdentifier();
                int ref_l = ref_helper.getValue(ref_id, "layer"); // 1-10; axial layers are odd layers; stereo layers are even
                int ref_s = ref_helper.getValue(ref_id, "module"); // 0-1; module number is top or bottom
                return ref_l;
////        Hep3Vector posVec = stripCluster.getPositionAsVector();
////        double yHit = posVec.y();
////        String side;
////        if (yHit>=0.0) side = "up";
////        else side = "down";
////        return side;
    }
    
    private int getLayerFromSensorName(String name) {
            int ilayer = -1;// = Integer.parseInt(layer.substring(layer.length()-1));
            if (name.contains("layer")) {
                
                //String str_l = name.substring(13);
            String str_l = name.substring(name.indexOf("layer")+5, name.indexOf("_module"));
            ilayer = Integer.parseInt(str_l);
               
                if ( ilayer < 1 || ilayer > 10 ) {
                    System.out.println("This layer doesn't exist?");
                    throw new RuntimeException("SiSensor name " + name + " is invalid?");
                }
                
            } else {
                throw new RuntimeException("This sensor name do not have a layer!");
            }
            System.out.println("Name " + name + " -> layer " + ilayer);
            return ilayer;
    }
    
   
    

   
    
   public void displayFastTrackingPlots() {
        //IPlotter plotter = af.createPlotterFactory(af.createTreeFactory().create());
        IPlotter plotter_org = af.createPlotterFactory().create("HPS SVT Fast Track Residuals (All tracks)");
        plotter_org.createRegions(5,2,0);
        IPlotter plotter_org_LayAllHit = af.createPlotterFactory().create("HPS SVT Fast Track Residuals (All layers has hits)");
        plotter_org_LayAllHit.createRegions(6,2,0);
        IPlotter plotter_org_LayAllSingleHit = af.createPlotterFactory().create("HPS SVT Fast Track Residuals (All layers has single hit)");
        plotter_org_LayAllSingleHit.createRegions(6,2,0);
        IPlotter plotter_org_Lay1Hit = af.createPlotterFactory().create("HPS SVT Fast Track Residuals (1st layer has hits)");
        plotter_org_Lay1Hit.createRegions(5,2,0);
        
        for ( int iSide=0;iSide<2;++iSide) {
            int nLayers = resy_org.get(iSide).size();
            for (int iLayer=0;iLayer<nLayers;++iLayer) {
                int idx = 2*iLayer+iSide;
                
                //0 1 2 3 4
                //1 3 5 7 9
                
                plotter_org.region(idx).plot(resy_org.get(iSide).get(iLayer));
                plotter_org_LayAllHit.region(idx).plot(resy_org_layallhit.get(iSide).get(iLayer));
                plotter_org_LayAllSingleHit.region(idx).plot(resy_org_layallsinglehit.get(iSide).get(iLayer));
                plotter_org_Lay1Hit.region(idx).plot(resy_org_lay1hit.get(iSide).get(iLayer));


            }
            //IPlotterStyle style = plotter_org_LayAllHit.style();
            //style.regionBoxStyle().
                    
            plotter_org_LayAllHit.region(10+iSide).style().setParameter("hist2DStyle", "colorMap");
            plotter_org_LayAllHit.region(10+iSide).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
            plotter_org_LayAllHit.region(10+iSide).plot(resy_2d_org_layallhit.get(iSide));
            plotter_org_LayAllSingleHit.region(10+iSide).style().setParameter("hist2DStyle", "colorMap");
            plotter_org_LayAllSingleHit.region(10+iSide).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
            plotter_org_LayAllSingleHit.region(10+iSide).plot(resy_2d_org_layallsinglehit.get(iSide));
        }
        plotter_org.show();
        plotter_org_LayAllHit.show();
        plotter_org_LayAllSingleHit.show();
        plotter_org_Lay1Hit.show();
        
        
        
        //Hit multiplicity
        IPlotter plotter_hitmult = af.createPlotterFactory().create();
        plotter_hitmult.createRegions(1,2,0);
        plotter_hitmult.region(0).plot(nhits_tracker.get(0));
        plotter_hitmult.region(1).plot(nhits_tracker.get(1));
        plotter_hitmult.show();
        
        IPlotter plotter_hitmult_layer = af.createPlotterFactory().create();
        plotter_hitmult_layer.createRegions(5,2,0);
        for ( int iSide=0;iSide<2;++iSide) {
            int nLayers = resy_org.get(iSide).size();
            for (int iLayer=0;iLayer<nLayers;++iLayer) {
                int idx = 2*iLayer+iSide;
                
                //0 1 2 3 4
                //1 3 5 7 9
                plotter_hitmult_layer.region(idx).plot(nhits_tracker_layer.get(iSide).get(iLayer));
              
            }
        }
        plotter_hitmult_layer.show();
        
        
        //ECal plots
        IPlotter plotter_ecalhitmult = af.createPlotterFactory().create();
        plotter_ecalhitmult.createRegions(5,2,0);
        plotter_ecalhitmult.region(0).plot(ncl_ecal.get(0));
        plotter_ecalhitmult.region(1).plot(ncl_ecal.get(1));
        plotter_ecalhitmult.region(2).plot(ncl_ecal_map.get(0));
        plotter_ecalhitmult.region(3).plot(ncl_ecal_map.get(1));
        plotter_ecalhitmult.region(4).plot(nselcl_ecal_map.get(0));
        plotter_ecalhitmult.region(5).plot(nselcl_ecal_map.get(1));
        plotter_ecalhitmult.region(6).plot(cl_ecal_e.get(0));
        plotter_ecalhitmult.region(7).plot(cl_ecal_e.get(1));
        plotter_ecalhitmult.region(8).plot(selcl_ecal_e.get(0));
        plotter_ecalhitmult.region(9).plot(selcl_ecal_e.get(1));
        plotter_ecalhitmult.show();
        
        
          for (int idx=2;idx<6;++idx) {
                IPlotterStyle style = plotter_ecalhitmult.region(idx).style();
                style.setParameter("hist2DStyle", "colorMap");
                style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
                ((PlotterRegion) plotter_ecalhitmult.region(idx)).getPlot().setAllowUserInteraction(false);
		((PlotterRegion) plotter_ecalhitmult.region(idx)).getPlot().setAllowPopupMenus(false);
          }
               
        
        
    }
    
    
    
    
    public void endOfData() {
        
        if (outputPlotFileName != "")
        try {
            aida.saveAs(outputPlotFileName);
        } catch (IOException ex) {
            Logger.getLogger(FastTrackResidualDriver.class.getName()).log(Level.SEVERE, "Couldn't save aida plots to file " + outputPlotFileName, ex);
        }
        displayFastTrackingPlots();
        
    }

    
}
