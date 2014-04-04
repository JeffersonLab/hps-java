/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.users.mgraham;

import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.recon.tracking.CoordinateTransformations;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.tracker.silicon.SiTrackerModule;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.base.MyLCRelation;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.BarrelEndcapFlag;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHit;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.recon.tracking.digitization.sisim.TrackerHitType;

/**
 *
 * @author phansson, mgraham
 */
public class SingleSensorHelicalTrackHitDriver extends org.lcsim.fit.helicaltrack.HelicalTrackHitDriver {
    private boolean _debug = false;
    private String subdetectorName = "Tracker";
    private Map<String,String> _stereomap = new HashMap<String,String>();
    private List<String> _colnames = new ArrayList<String>();
    private boolean _doTransformToTracking = true;
    public enum LayerGeometryType { 
        /*
         * Each Layer in the geometry is a separate sensor
         */
        Split, 
        /*
         * Each layer in the geometry comprises top and bottom sensor
         */
        Common
    }
    private LayerGeometryType _layerGeometryType;

    public SingleSensorHelicalTrackHitDriver() {
        this.setLayerGeometryType("Common");
        this.addCollection("StripClusterer_SiTrackerHitStrip1D");
    }
    public void setLayerGeometryType(String geomType) {
        this._layerGeometryType = LayerGeometryType.valueOf(geomType);
    }
    
    public void setSubdetectorName(String subdetectorName) {
        this.subdetectorName = subdetectorName;
    }
    
    public void setDebug(boolean debug) {
        this._debug = debug;
    }
    
    public void setTransformToTracking(boolean trans) {
        this._doTransformToTracking = trans;
    }
    
    public void setStripHitsCollectionName(String stripHitsCollectionName) {
        HitRelationName(stripHitsCollectionName);
    }

    public void setHelicalTrackHitRelationsCollectionName(String helicalTrackHitRelationsCollectionName) {
        HitRelationName(helicalTrackHitRelationsCollectionName);
    }

    public void setHelicalTrackMCRelationsCollectionName(String helicalTrackMCRelationsCollectionName) {
        MCRelationName(helicalTrackMCRelationsCollectionName);
    }

    public void setOutputHitCollectionName(String outputHitCollectionName) {
        OutputCollection(outputHitCollectionName);
    }
   
    
    @Override
    public void process(EventHeader event) {
        //super.process(event);

        //  Initialize the list of HelicalTrackHits
        List<HelicalTrackCross> stereoCrosses = new ArrayList<HelicalTrackCross>();
        List<HelicalTrackHit> helhits = new ArrayList<HelicalTrackHit>();
        
        //  Create a List of LCRelations to relate HelicalTrackHits to the original hits
        List<LCRelation> hitrelations = new ArrayList<LCRelation>();
        //  Create a List of LCRelations to relate HelicalTrackHits to the MC particle
        List<LCRelation> mcrelations = new ArrayList<LCRelation>();

        
        
        for(String _colname : this._colnames) {
            
            if (!event.hasCollection(SiTrackerHit.class, _colname)) {
                continue;
            }

            //  Get the list of SiTrackerHits for this collection
            List<SiTrackerHit> hitlist = (List<SiTrackerHit>) event.get(_colname);
            if (_debug) System.out.printf("%s: found %d SiTrackerHits = ",this.getClass().getSimpleName(),hitlist.size());
            
            //  Create collections for strip hits by layer and hit cross references
            Map<String, List<HelicalTrackStrip>> striplistmap = new HashMap<String, List<HelicalTrackStrip>>();
            Map<HelicalTrackStrip, SiTrackerHitStrip1D> stripmap = new HashMap<HelicalTrackStrip, SiTrackerHitStrip1D>();
            
            for(SiTrackerHit hit : hitlist) {
                
                if( hit instanceof SiTrackerHitStrip1D) {
                    
                    //  Cast the hit as a 1D strip hit and find the identifier for the detector/layer combo
                    SiTrackerHitStrip1D h = (SiTrackerHitStrip1D) hit;
                    IDetectorElement de = h.getSensor();
                    String id = this.makeID(_ID.getName(de), _ID.getLayer(de));

                    //  This hit should be a on a stereo pair!
                    if (!_stereomap.containsKey(id) &&! _stereomap.containsValue(id)) {
                        throw new RuntimeException(this.getClass().getSimpleName() + ": this " + id + " was not among the stereo modules!");
                    }
                    
                    //  Create a HelicalTrackStrip for this hit
                    HelicalTrackStrip strip = makeDigiStrip(h);

                    //  Get the list of strips for this layer - create a new list if one doesn't already exist
                    List<HelicalTrackStrip> lyrhits = striplistmap.get(id);
                    if (lyrhits == null) {
                        lyrhits = new ArrayList<HelicalTrackStrip>();
                        striplistmap.put(id, lyrhits);
                    }

                    //  Add the strip to the list of strips on this sensor
                    lyrhits.add(strip);

                    //  Map a reference back to the hit needed to create the stereo hit LC relations
                    stripmap.put(strip, h);

                    if(_debug) System.out.printf("%s: added strip (org=%s,umeas=%.3f) at layer %d ",this.getClass().getSimpleName(),strip.origin().toString(),strip.umeas(),strip.layer());
                    
                } else {

                    //  If not a 1D strip hit, make a pixel hit
                    HelicalTrackHit hit3d = this.makeDigi3DHit(hit);
                    helhits.add(hit3d);
                    hitrelations.add(new MyLCRelation(hit3d, hit));
                }
                
            } // Loop over SiTrackerHits
            
            
            //  Create a list of stereo hits
            //List<HelicalTrackCross> stereohits = new ArrayList<HelicalTrackCross>();
            
            if (_debug) System.out.println(this.getClass().getSimpleName() + ": Create stereo hits from " + striplistmap.size() + " strips (map size)");
                        
            //  Loop over the stereo layer pairs
            for (String id1 : _stereomap.keySet()) {

                //  Get the second layer
                String id2 = _stereomap.get(id1);

                if (_debug) {
                    if (striplistmap.get(id1) != null && striplistmap.get(id2) != null) {
                        System.out.println(this.getClass().getSimpleName() + ": Form stereo hits from " + id1 + " and " + id2);
                        //for(HelicalTrackStrip strip: striplistmap.get(id1)) System.out.printf("%s: stripid1 at origin %s\n",this.getClass().getSimpleName(),strip.origin().toString());
                        //for(HelicalTrackStrip strip: striplistmap.get(id2)) System.out.printf("%s: stripid2 at origin %s\n",this.getClass().getSimpleName(),strip.origin().toString());
                    }
                }
                
                /*
                 * Form the stereo hits and add them to our hit list
                 * Add LC relations for stereo hit to SiTrackHitStrip1D object 
                 * Add LC relation between MC particle and stereo hit
                 */
                
                List<HelicalTrackCross> cross_list =  _crosser.MakeHits(striplistmap.get(id1), striplistmap.get(id2));
                
                for(HelicalTrackCross cross : cross_list) {
                    stereoCrosses.add(cross);
                    if(cross.getMCParticles()!=null) {
                        for(MCParticle mcp : cross.getMCParticles()) {
                            mcrelations.add(new MyLCRelation((HelicalTrackHit)cross,mcp));
                        }
                    }
                    for(HelicalTrackStrip strip : cross.getStrips()) {
                        hitrelations.add(new MyLCRelation(cross,stripmap.get(strip)));
                    }
                    
                }
                
            } // Loop over stereo pairs

            if (_debug) {
                System.out.printf("%s: added %d stereo hits from %s collection ",this.getClass().getSimpleName(),stereoCrosses.size(),_colname);
            }
            
            
        } // Loop over collection names
        
        
        
        // Add things to the event
        // Cast crosses to HTH
        helhits.addAll(stereoCrosses);
        event.put(_outname, helhits, HelicalTrackHit.class, 0);
        event.put(_hitrelname,hitrelations,LCRelation.class,0);
        event.put(_mcrelname,mcrelations,LCRelation.class,0);       
        if(_doTransformToTracking) addRotatedHitsToEvent(event, stereoCrosses);
        
        
        
    } //Process()
    

    public void addCollection(String colname) {
        _colnames.add(colname);
    }
    public void setCollection(String colname) {
        _colnames.clear();
        this.addCollection(colname);
    }
    private String makeID(String detname, int lyr) {
        return detname + lyr;
    }
    
    public void setStereoPair(String detname, int lyr1, int lyr2) {
        this._stereomap.put(this.makeID(detname, lyr1), this.makeID(detname, lyr2));
    }
    
    @Override
    protected void detectorChanged(Detector detector) {
        /*
         * Setup default pairing
         */
        if (_debug) System.out.printf("%s: Setup stereo hit pair modules ",this.getClass().getSimpleName());
        
        List<SiTrackerModule> modules = detector.getSubdetector(this.subdetectorName).getDetectorElement().findDescendants(SiTrackerModule.class);
        if (modules.isEmpty()) {
            throw new RuntimeException(this.getClass().getName() + ": No SiTrackerModules found in detector.");
        }
        int nLayersTotal = detector.getSubdetector(subdetectorName).getLayering().getLayers().getNumberOfLayers();
        if (_debug) System.out.printf("%s: %d layers ",this.getClass().getSimpleName(),nLayersTotal);
        if (nLayersTotal % 2 != 0) {
            throw new RuntimeException(this.getClass().getName() + ": Don't know how to do stereo pairing for odd number of modules.");
        }
        if(this._layerGeometryType==LayerGeometryType.Split) {
            int nLayers = nLayersTotal/2;
            for(int i=1;i<=nLayers;++i) {
                int ly1 = i;
                int ly2 = i+10;
                if (_debug) System.out.printf("%s: adding stereo pair from layer %d and %d ",this.getClass().getSimpleName(),ly1,ly2);
                this.setStereoPair(subdetectorName, ly1, ly2);
            }            
        } else if(this._layerGeometryType==LayerGeometryType.Common) {
            List<int[]> pairs = new ArrayList<int[]>();
            for (int i = 1; i <= (nLayersTotal) - 1; i += 2) {
                int[] pair = {i, i + 1};
                if (_debug)
                    System.out.println("Adding stereo pair: " + pair[0] + ", " + pair[1]);
                pairs.add(pair);
            }
            for (int[] pair : pairs) {
                if (_debug) System.out.printf("%s: adding stereo pair from layer %d and %d ",this.getClass().getSimpleName(),pair[0],pair[1]);
                setStereoPair(subdetectorName, pair[0], pair[1]);
            }
        } else {
            throw new RuntimeException(this.getClass().getSimpleName() + ": this layer geometry is not implemented!");
        }

        if (_debug) System.out.printf("%s: %d stereo modules added",this.getClass().getSimpleName(),this._stereomap.size());
        
    }
    
     private HelicalTrackStrip makeDigiStrip(SiTrackerHitStrip1D h) {
        
        if(_debug) System.out.println(this.getClass().getSimpleName() + ": makeDigiStrip--");
        
        SiTrackerHitStrip1D local = h.getTransformedHit(TrackerHitType.CoordinateSystem.SENSOR);
        SiTrackerHitStrip1D global = h.getTransformedHit(TrackerHitType.CoordinateSystem.GLOBAL);
    
        ITransform3D trans = local.getLocalToGlobal();
        Hep3Vector org = trans.transformed(_orgloc);
        Hep3Vector u = global.getMeasuredCoordinate();
        Hep3Vector v = global.getUnmeasuredCoordinate();
         
       
        
        double umeas = local.getPosition()[0];
        double vmin = VecOp.dot(local.getUnmeasuredCoordinate(), local.getHitSegment().getStartPoint());
        double vmax = VecOp.dot(local.getUnmeasuredCoordinate(), local.getHitSegment().getEndPoint());
        double du = Math.sqrt(local.getCovarianceAsMatrix().diagonal(0));

       
        
        IDetectorElement de = h.getSensor();
        String det = _ID.getName(de);
        int lyr = _ID.getLayer(de);
        BarrelEndcapFlag be = _ID.getBarrelEndcapFlag(de);

        double dEdx = h.getdEdx();
        double time = h.getTime();
        List<RawTrackerHit> rawhits = h.getRawHits();
        HelicalTrackStrip strip = new HelicalTrackStrip(org, u, v, umeas, du,
                vmin, vmax, dEdx, time, rawhits, det, lyr, be);

        try {
            if (h.getMCParticles() != null) {
                for (MCParticle p : h.getMCParticles()) {
                    strip.addMCParticle(p);
                }
            }
        } catch (RuntimeException e) {
            // Okay when MC info not present.
        }
       
        if(_debug) {
            System.out.println(this.getClass().getSimpleName() + ": produced HelicalTrackStrip with origin " + strip.origin().toString());
        }

        return strip;
    }

     private void addRotatedHitsToEvent(EventHeader event, List<HelicalTrackCross> stereohits) {

        List<HelicalTrackHit> rotatedhits = new ArrayList<HelicalTrackHit>();
        List<LCRelation> hthrelations = new ArrayList<LCRelation>();
        List<LCRelation> mcrelations = new ArrayList<LCRelation>();
        for (HelicalTrackCross cross : stereohits) {
            List<HelicalTrackStrip> rotatedstriphits = new ArrayList<HelicalTrackStrip>();
            for (HelicalTrackStrip strip : cross.getStrips()) {

                Hep3Vector origin = strip.origin();
                Hep3Vector u = strip.u();
                Hep3Vector v = strip.v();
                double umeas = strip.umeas();
                double du = strip.du();
                double vmin = strip.vmin();
                double vmax = strip.vmax();
                double dedx = strip.dEdx();
                double time = strip.time();
                List<RawTrackerHit> rthList = strip.rawhits();
                String detname = strip.detector();
                int layer = strip.layer();
                BarrelEndcapFlag bec = strip.BarrelEndcapFlag();
                Hep3Vector neworigin = CoordinateTransformations.transformVectorToTracking(origin);
                Hep3Vector newu = CoordinateTransformations.transformVectorToTracking(u);
                Hep3Vector newv = CoordinateTransformations.transformVectorToTracking(v);
                HelicalTrackStrip newstrip = new HelicalTrackStrip(neworigin, newu, newv, umeas, du, vmin, vmax, dedx, time, rthList, detname, layer, bec);
                for (MCParticle p : strip.MCParticles()) {
                    newstrip.addMCParticle(p);
                }
                rotatedstriphits.add(newstrip);
            }
            HelicalTrackCross newhit = new HelicalTrackCross(rotatedstriphits.get(0), rotatedstriphits.get(1));
            rotatedhits.add(newhit);
            hthrelations.add(new MyLCRelation(cross, newhit));
            for (MCParticle mcp : newhit.getMCParticles()) {
                mcrelations.add(new MyLCRelation(newhit, mcp));
            }
        }
        
        event.put("Rotated"+_outname, rotatedhits, HelicalTrackHit.class, 0);
        event.put("Rotated"+_hitrelname, hthrelations, LCRelation.class, 0);
        event.put("Rotated"+_mcrelname, mcrelations, LCRelation.class, 0);
        
        
        //  Create the LCRelations between HelicalTrackHits and MC particles

    }
    
}
