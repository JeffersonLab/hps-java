package org.hps.monitoring.svt;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;
import hep.aida.ref.plotter.PlotterRegion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.SiTrackerIdentifierHelper;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.hps.monitoring.deprecated.Resettable;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author mgraham
 */
public class SVTHitRecoCorrelations extends Driver implements Resettable {

	//private List<AIDAFrame> plotterFrame = new ArrayList<AIDAFrame>();
    private List<IPlotter> plotters = new ArrayList<IPlotter>();
    private AIDA aida = AIDA.defaultInstance();                  
    private String rawTrackerHitCollectionName = "SVTRawTrackerHits";
    private String fittedTrackerHitCollectionName = "SVTFittedRawTrackerHits";
    private String trackerHitCollectionName = "StripClusterer_SiTrackerHitStrip1D";
    private String hthOutputCollectionName = "HelicalTrackHits";
    private String trackerName = "Tracker";
    private int eventCount;
    private List<SiSensor> sensors;
    ArrayList< ArrayList<IPlotter> > plotter = new ArrayList< ArrayList<IPlotter> >();
    private Map<String, Integer> sensorRegionMap;
    private String outputPlots = null;
    String types[] = {"RawStrips","ClusterY","ClusterX"};
    String side[] = {"top","bottom"};
    
    boolean doStrips = true;
    
    
    protected void detectorChanged(Detector detector) {
        //plotterFrame.add(new AIDAFrame());
        //plotterFrame.get(0).setTitle("HPS Top SVT Hit Reconstruction Correlation Plots");
        //plotterFrame.add(new AIDAFrame());
        //plotterFrame.get(1).setTitle("HPS Bottom SVT Hit Reconstruction Correlation Plots");
        
        aida.tree().cd("/");


        sensors = detector.getSubdetector(trackerName).getDetectorElement().findDescendants(SiSensor.class);

        // Map a map of sensors to their region numbers in the plotter.
//        sensorRegionMap = new HashMap<String, Integer>();
//        for (SiSensor sensor : sensors) {
//            int region = computePlotterRegion(sensor);
//            sensorRegionMap.put(sensor.getName(), region);
//        }
        IAnalysisFactory fac = aida.analysisFactory();
        
        for(int i=0;i<2;++i) {
            plotter.add(new ArrayList<IPlotter>());
            for(int t=0;t<3;++t) {
                IPlotter bcorr = fac.createPlotterFactory().create("compact_" + types[t] + " " + side[i] + " hits");
                bcorr.setTitle("Cmpt "+ side[i] + " hits " + types[t]);
                bcorr.createRegion();
                plotter.get(i).add(bcorr);
                //plotterFrame.get(i).addPlotter(bcorr);
                plotters.add(bcorr);


                IPlotterStyle style = bcorr.style();
                style.setParameter("hist2DStyle", "colorMap");
                style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
                style.statisticsBoxStyle().setVisible(false);
                //style.dataStyle().fillStyle().setColor("yellow");
                //style.dataStyle().errorBarStyle().setVisible(false);
                IHistogram2D corPlot;
                //if(t==t) {
                        corPlot = aida.histogram2D("Cmpt_" + side[i] + "_" + types[t], 50*10, 0,10, 50*10, 0,10);
                //}
                
                
                plotter.get(i).get(t).region(0).plot(corPlot);
                
                
                
                ((PlotterRegion) plotter.get(i).get(t).region(0)).getPlot().setAllowUserInteraction(true);
                ((PlotterRegion) plotter.get(i).get(t).region(0)).getPlot().setAllowPopupMenus(true);
                
            }
        }

        /*
        for (int i=0;i<2;++i) {
            plotter.add(new ArrayList<IPlotter>());
            
            for (int t=0;t<8;++t) {
                IPlotter tmp = fac.createPlotterFactory().create(types[t] + " " + side[i] + " hits");
                tmp.setTitle(side[i] + " hits " + types[t]);
                if(i==2) tmp.createRegion();
                else tmp.createRegions(5, 5);
                plotter.get(i).add(tmp);
                plotterFrame.get(i).addPlotter(tmp);
                plotters.add(tmp);
                
                
                IPlotterStyle style = tmp.style();
                style.setParameter("hist2DStyle", "colorMap");
                style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
                style.statisticsBoxStyle().setVisible(false);
                //style.dataStyle().fillStyle().setColor("yellow");
                //style.dataStyle().errorBarStyle().setVisible(false);
                
                IHistogram2D corPlot = aida.histogram2D("Cmpt_" + side[i] + "_" + types[t], 100*10, 0,10, 100*10, 0,10);
                plotter.get(i).get(t).region(0).plot(corPlot);
                
            }
        }
        
        
     
        
        for (SiSensor ref_sensor : sensors) {
            int ref_l = getLayer(ref_sensor);
            int ref_s = getSide(ref_sensor);
            boolean ref_a = isAxial(ref_sensor);
            //ref_l = getPhysLayer(ref_l,ref_s,ref_a);
            
            System.out.println("Sensor " + ref_sensor.getName() + " -> ref_s " + ref_s + " layer " + getLayer(ref_sensor) + " phys layer " + ref_l + " axial " + ref_a);
            //if (!ref_a) continue;
            //if (ref_s == 1 ) continue; //only top for now

            if(ref_l>3) continue;
            
            for (SiSensor sensor : sensors) {
                int l = getLayer(sensor);
                int s = getSide(sensor);
                boolean a = isAxial(sensor);
                
                //l = getPhysLayer(l,s,a);

                if(l>3) continue;
                
                
                //correlation with same side and axial/stereo
                
                //if ( ref_a == a && ref_s == s ) {
                if ( ref_s == s ) {
                    int region = (ref_l-1) + (l-1)*5;
                    //int region = (ref_l-1) + (l-1)*5;
                    System.out.println("region " + region);
                    double ymin,ymax;
                    if(s==0) {
                        ymin=0;
                        ymax=60;
                    }else {
                        ymin=-60;
                        ymax=0;
                    }
                    if( a ) {
                        if(1==1) { 
                            IHistogram2D corPlot = aida.histogram2D(side[s] + "_" + types[0] + "_layer" + ref_l + "_layer" + l, 50, ymin,ymax, 50, ymin,ymax);
                            plotter.get(s).get(0).region(region).plot(corPlot);
                        }
                        if(doStrips) {
                            //IHistogram2D corPlot1 = aida.histogram2D(side[s] + "_" + types[4] + "_layer" + ref_l + "_layer" + l, 642, 0,641, 642, 0,641);
                            IHistogram2D corPlot1 = aida.histogram2D(side[s] + "_" + types[4] + "_layer" + ref_l + "_layer" + l, 100, 0,641, 100, 0,641);
                            plotter.get(s).get(4).region(region).plot(corPlot1);
                            IHistogram2D corPlot2 = aida.histogram2D(side[s] + "_" + types[6] + "_layer" + ref_l + "_layer" + l, 100, 0,641, 100, 0,641);
                            plotter.get(s).get(6).region(region).plot(corPlot2);
                        }
                    } else {
                        if(1==1) {
                            IHistogram2D corPlot = aida.histogram2D(side[s] + "_" + types[1] + "_layer" + ref_l + "_layer" + l, 50, ymin, ymax, 50, ymin, ymax);
                            plotter.get(s).get(1).region(region).plot(corPlot);
                        }
                        if(doStrips) {
                            //IHistogram2D corPlot1 = aida.histogram2D(side[s] + "_" + types[5] + "_layer" + ref_l + "_layer" + l, 642, 0,641, 642, 0,641);
                            IHistogram2D corPlot1 = aida.histogram2D(side[s] + "_" + types[5] + "_layer" + ref_l + "_layer" + l, 100, 0,641, 100, 0,641);
                            plotter.get(s).get(5).region(region).plot(corPlot1);
                            IHistogram2D corPlot2 = aida.histogram2D(side[s] + "_" + types[7] + "_layer" + ref_l + "_layer" + l, 100, 0,641, 100, 0,641);
                            plotter.get(s).get(7).region(region).plot(corPlot2);
                        
                        }
                    }

                }
               
            }
        }
        
        for (int i=1;i<6;++i) {
            for (int ii=1;ii<6;++ii) {
                for (int s=0;s<2;++s) {
                    System.out.println(" i " + i + " ii " + ii + " s " + side[s]);
                    double ymin,ymax;
                    if(s==0) {
                        ymin=0;
                        ymax=60;
                    }else {
                        ymin=-60;
                        ymax=0;
                    }
                    IHistogram2D corPlot = aida.histogram2D(side[s] + "_Y_HTH_layer" + i + "_layer" + ii, 50,ymin, ymax, 50, ymin, ymax);
                    IHistogram2D corPlot1 = aida.histogram2D(side[s] + "_X_HTH_layer" + i + "_layer" + ii, 60, ymin, ymax, 60, ymin,ymax);
                    int region = (i-1) + (ii-1)*5;
                    plotter.get(s).get(2).region(region).plot(corPlot);
                    plotter.get(s).get(3).region(region).plot(corPlot1);
                }
            }
        }
        */
        
        //for(int i=0;i<2;++i) {
       	//plotterFrame.get(i).pack();
        //    plotterFrame.get(i).setVisible(true);
        //}
    }

    public SVTHitRecoCorrelations() {
    }

    public void setOutputPlots(String output) {
        this.outputPlots = output;
    }

    public void setRawTrackerHitCollectionName(String rawTrackerHitCollectionName) {
        this.rawTrackerHitCollectionName = rawTrackerHitCollectionName;
    }

    public void setFittedTrackerHitCollectionName(String fittedTrackerHitCollectionName) {
        this.fittedTrackerHitCollectionName = fittedTrackerHitCollectionName;
    }

    public void setTrackerHitCollectionName(String trackerHitCollectionName) {
        this.trackerHitCollectionName = trackerHitCollectionName;
    }

    public void process(EventHeader event) {
        
    
        ++eventCount;

        if (event.hasCollection(RawTrackerHit.class, rawTrackerHitCollectionName)) {
            List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, rawTrackerHitCollectionName);
        
            System.out.println("The RawTrackerHit collection " + rawTrackerHitCollectionName + " has " + rawHits.size() + " hits.");
        
            for (RawTrackerHit ref_hit : rawHits) {
                
                IIdentifierHelper ref_helper = ref_hit.getDetectorIdentifierHelper();
                IIdentifier ref_id = ref_hit.getIdentifier();
                int ref_l = ref_helper.getValue(ref_id, "layer"); // 1-10; axial layers are odd layers; stereo layers are even
                int ref_s = ref_helper.getValue(ref_id, "module"); // 0-1; module number is top or bottom
                boolean ref_a = isAxial(ref_s,ref_l);
                //ref_l = getPhysLayer(ref_l,ref_s,ref_a);

                ///if(ref_l>3) continue;
        
                for (RawTrackerHit hit : rawHits) {
                       
                    IIdentifierHelper helper = hit.getDetectorIdentifierHelper();
                
                    IIdentifier id = hit.getIdentifier();
                    int l = helper.getValue(id, "layer"); // 1-10; axial layers are odd layers; stereo layers are even
                    int s = helper.getValue(id, "module"); // 0-1; module number is top or bottom
                    boolean a = isAxial(s,l);
                    //l = getPhysLayer(l,s,a);

                    //if(l>3) continue;

                    //if (ref_a==a && ref_s==s) {
                    if (ref_s==s) {
                        IExpandedIdentifier ref_eid = ref_helper.unpack(ref_id);
                        IIdentifierDictionary ref_dict = ref_helper.getIdentifierDictionary();
                        int ref_strip = ref_eid.getValue(ref_dict.getFieldIndex("strip"));
                        IExpandedIdentifier eid = helper.unpack(id);
                        IIdentifierDictionary dict = helper.getIdentifierDictionary();
                        int strip = eid.getValue(dict.getFieldIndex("strip"));
                        
                        
                        double c_strip = ((double)strip)/640.0 + (l-1);
                        double c_ref_strip = ((double)ref_strip)/640.0 + (ref_l-1);
                        
                        aida.histogram2D("Cmpt_" + side[s] + "_" + types[0]).fill(c_ref_strip,c_strip);
                        
            

                        
                        
                        
                        
                        
                        // Fill in the side and strip numbers.
                        //ref_eid.setValue(dict.getFieldIndex("side"), sideNumber);
                        //ref_eid.setValue(dict.getFieldIndex("strip"), stripNumber);

                        //int clusterSize = cluster.getRawHits().size();
                        //Move this to strip nr?
                        //System.out.println("side " + side[s]);
//                        if( a) {
//                            if(doStrips) aida.histogram2D(side[s] + "_" + types[6] + "_layer" + ref_l + "_layer" + l).fill(ref_strip,strip);
//                        } else {
//                            if(doStrips) aida.histogram2D(side[s] + "_" + types[7] + "_layer" + ref_l + "_layer" + l).fill(ref_strip,strip);
//
//                        }
                    }
                }
            }
        }
        else {
        
            System.out.println("No " + rawTrackerHitCollectionName + " was found in this event.");
        }
        
        
        
        
        
        
        
        if (event.hasCollection(SiTrackerHitStrip1D.class, trackerHitCollectionName)) {
            List<SiTrackerHitStrip1D> stripHits = event.get(SiTrackerHitStrip1D.class, trackerHitCollectionName);
        
            System.out.println("The SiTrackerHitStrip1D collection " + trackerHitCollectionName + " has " + stripHits.size() + " hits.");
        
            for (SiTrackerHitStrip1D ref_cluster : stripHits) {
                SiSensor ref_sensor = ref_cluster.getSensor();
                
                boolean ref_a = isAxial(ref_sensor);
                int ref_s = getSide(ref_sensor);
                int ref_l = getLayer(ref_sensor);
                //ref_l = getPhysLayer(ref_l,ref_s,ref_a);
                
                SiTrackerIdentifierHelper ref_helper = ref_cluster.getIdentifierHelper();
                IIdentifier ref_id = ref_cluster.getRawHits().get(0).getIdentifier();                    
                int ref_strip = ref_helper.getElectrodeValue(ref_id);

                

                for (SiTrackerHitStrip1D cluster : stripHits) {
                    SiSensor sensor = cluster.getSensor();
                    boolean a = isAxial(sensor);
                    int s = getSide(sensor);

                    if (ref_s==s) {
                    //if (ref_a==a && ref_s==s) {

                        int l = getLayer(sensor);
                        //l = getPhysLayer(l,s,a);
                        SiTrackerIdentifierHelper helper = cluster.getIdentifierHelper();
                        IIdentifier id = cluster.getRawHits().get(0).getIdentifier();                    
                        int strip = helper.getElectrodeValue(id);
                                //.hps_hit.getRawTrackerHit().getIdentifier();

                        //Remember measurement direction is tracking z
                        double p  = cluster.getPosition()[0]/10.0 + (l-1);
                        double ref_p  = ref_cluster.getPosition()[0]/10.0 + (ref_l-1);
                        //System.out.println("Y " + cluster.getPosition()[0]);
                        aida.histogram2D("Cmpt_" + side[s] + "_" + types[1]).fill(ref_p,p);

                        //Remember non-measurement direction is tracking y
                        p  = (cluster.getPosition()[1]+50.0)/100.0 + (l-1);
                        ref_p  = (ref_cluster.getPosition()[1]+50.0)/100.0 + (ref_l-1);
                        //System.out.println("X " + cluster.getPosition()[1]);
                        aida.histogram2D("Cmpt_" + side[s] + "_" + types[2]).fill(ref_p,p);
//                        
//                        
//                        
//                        //int clusterSize = cluster.getRawHits().size();
//                        //Move this to strip nr?
//                        //System.out.println("side " + side[s]);
//                        if( a) {
//                            if(1==1) aida.histogram2D(side[s] + "_" + types[0] + "_layer" + ref_l + "_layer" + l).fill(ref_cluster.getPosition()[1],cluster.getPosition()[1]);
//                            if(doStrips) aida.histogram2D(side[s] + "_" + types[4] + "_layer" + ref_l + "_layer" + l).fill(ref_strip,strip);
//                        } else {
//                            if(1==1) aida.histogram2D(side[s] + "_" + types[1] + "_layer" + ref_l + "_layer" + l).fill(ref_cluster.getPosition()[1],cluster.getPosition()[1]);
//                            if(doStrips) aida.histogram2D(side[s] + "_" + types[5] + "_layer" + ref_l + "_layer" + l).fill(ref_strip,strip);
//
//                        }
                    }
                }
            }
        }
        else {
        
            System.out.println("No " + trackerHitCollectionName + " was found in this event.");
        }
        
        
        
        /*
        if (event.hasCollection(TrackerHit.class, hthOutputCollectionName)) {
        
            List<TrackerHit> hth = event.get(TrackerHit.class, hthOutputCollectionName);
            System.out.println("The HelicalTrackHit collection " + hthOutputCollectionName + " has " + hth.size() + " hits.");
        
            for (TrackerHit h_ref : hth) {
                HelicalTrackHit hit_ref = (HelicalTrackHit)h_ref;
                int layer_ref = hit_ref.Layer();
                //HTH hits uses the axial layer nr i.e. odd numbers 1-10
                layer_ref = (layer_ref+1)/2;
                //How do I find the top or bottom side?
                int ref_s = 0;
                if (hit_ref.getPosition()[1] < 0 ) ref_s = 1;
                
                
                
                
                //if (ref_s ==0) continue;

                for (TrackerHit h : hth) {
                    HelicalTrackHit hit = (HelicalTrackHit)h; 
                    
                    int layer = hit.Layer();
                    layer = (layer+1)/2;
                    String name = hit.Detector();
                    int s = 0;
                    if (hit.getPosition()[1] < 0 ) s = 1;
                    //System.out.println("Hit name " + name + " layer " + layer + "  x,y , z" + hit.getPosition()[0] + "," + hit.getPosition()[1] + "," + hit.getPosition()[2] );
                    //System.out.println("Hit name " + name + " layer " + layer_ref + "  x,y , z" + hit_ref.getPosition()[0] + "," + hit_ref.getPosition()[1] + "," + hit_ref.getPosition()[2] );
                    
                    if ( s == ref_s) {               
                        aida.histogram2D(side[s] + "_Y_HTH_layer" + layer_ref + "_layer" + layer).fill(hit_ref.getPosition()[1],hit.getPosition()[1]);
                        aida.histogram2D(side[s] + "_X_HTH_layer" + layer_ref + "_layer" + layer).fill(hit_ref.getPosition()[0],hit.getPosition()[0]);
                    }
                }
                
            }
         
        } else {
        
            System.out.println("No " + hthOutputCollectionName + " was found in this event.");
        }
        
        */
        
        

    }

    public void endOfData() {
        if (outputPlots != null)
            try {
                aida.saveAs(outputPlots);
            } catch (IOException ex) {
                Logger.getLogger(SVTHitRecoCorrelations.class.getName()).log(Level.SEVERE, null, ex);
            }
        //for(int i=0;i<2;++i) {
        //    plotterFrame.get(i).dispose();
        //}
    }   

    @Override
    public void reset() {
        int ns = sensors.size();
        for (int i = 0; i < 5; i++) {
            for (int ii = 0; ii < 5; ii++) {
                aida.histogram2D("corr_TA_layer" + (i+1) + "_layer" + (ii+1)).reset();
                aida.histogram2D("corr_TS_layer" + (i+1) + "_layer" + (ii+1)).reset();
                aida.histogram2D("corrY_HTH_layer" + (i+1) + "_layer" + (ii+1)).reset();
                aida.histogram2D("corrX_HTH_layer" + (i+1) + "_layer" + (ii+1)).reset();
            }
        }
         
    }

    
    private int getPhysLayer(int l,int side, boolean axial) {
        if(side==0) {
            //top: odd are axial        
            if(axial) {
                l = (l+1)/2;
            } else {
                l = l/2;
            }
        } else {
            //bottom
            if(axial) {
                l = l/2;
            } else {
                l = (l+1)/2;
            }
        }
        return l;
    }

    private int[] getSideAndLayer(SiSensor sensor) {
        
        IIdentifierHelper helper = sensor.getIdentifierHelper();
        IIdentifier id = sensor.getIdentifier();
        int layer = helper.getValue(id, "layer"); // 1-10; axial layers are odd layers; stereo layers are even
        int module = helper.getValue(id, "module"); // 0-1; module number is top or bottom
        int v[] = {module,layer};
        return v;
    

    }

     private int getSide(SiSensor sensor) {
         int v[] = getSideAndLayer(sensor);
         return v[0];
         
    }

    private int getLayer(SiSensor sensor) {
         int v[] = getSideAndLayer(sensor);
         return v[1];
         
    }

    private boolean isAxial(SiSensor sensor) {
         int v[] = getSideAndLayer(sensor);
         int layer = v[1];
         if (v[0]==0) {
             //top
             if ( layer % 2 == 0 ) {
                 return false;
             }
         } else {
             //bottom
             if ( layer % 2 != 0 ) {
                 return false;
             }
         }
         return true;
    }
    
     private boolean isAxial(int ref_s,int ref_l) {
        boolean ref_a=true;
        if (ref_s==0) {
             //top
            if ( ref_l % 2 == 0 ) {
                 ref_a = false;
            }
        } else {
            //bottom
            if ( ref_l % 2 != 0 ) {
                ref_a = false;
            }
        }
        return ref_a;
            
    
     }
    
}
