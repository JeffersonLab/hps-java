/*
 * TrackAnalysis.java
 *
 * Created on October 16, 2008, 6:09 PM
 *
 */
package org.lcsim.hps.recon.tracking;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lcsim.event.MCParticle;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrack2DHit;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;

/**
 *
 * @author Richard Partridge & Matt Graham
 */
public class TrackAnalysis {

    private enum HelixPar {
        Curvature, Phi0, DCA, Z0, Slope
    };
    private MCParticle _mcp = null;
    private int _nhits;
    private int _nbadhits;
    private double _purity;
    private MCParticle _mcpNew = null;
    private int _nhitsNew;
    private int _nbadhitsNew;
    private double _purityNew;
    private int _nAxialhits;
    private int _nZhits;
    private int _nbadAxialhits;
    private int _nbadZhits;
    private boolean _hasLayerOne;
    List<Integer> badHitList = new ArrayList();
    List<Integer> sharedHitList = new ArrayList();
    List<Integer> trackLayerList = new ArrayList();
    Map<MCParticle, HelicalTrackCross> badhits = new HashMap<MCParticle, HelicalTrackCross>();
    private int[] _nMCHitsPerLayer={0,0,0,0,0,0,0,0,0,0,0,0};
    private int[] _nStripHitsPerLayer={0,0,0,0,0,0,0,0,0,0,0,0};
     Map<Integer, Hep3Vector> _hitLocationPerLayer = new HashMap<Integer,Hep3Vector>();

    /** Creates a new instance of TrackAnalysis */
    public TrackAnalysis(Track trk, RelationalTable hittomc) {

        //  Get the number of hits on the track
        _nhits = trk.getTrackerHits().size();

        //  Create a map containing the number of hits for each MCParticle associated with the track
        Map<MCParticle, Integer> mcmap = new HashMap<MCParticle, Integer>();
        Map<MCParticle, Integer> mcmapAll = new HashMap<MCParticle, Integer>();
        Map<MCParticle, Integer> mcmapAxial = new HashMap<MCParticle, Integer>();
        Map<MCParticle, Integer> mcmapZ = new HashMap<MCParticle, Integer>();
        _hasLayerOne = false;
        //  Loop over the hits on the track and make sure we have HelicalTrackHits (which contain the MC particle)
        for (TrackerHit hit : trk.getTrackerHits()) {
            //  get the set of MCParticles associated with this hit and update the hit count for each MCParticle
            Set<MCParticle> mclist = hittomc.allFrom(hit);
            for (MCParticle mcp : mclist) {
                Integer mchits = 0;
                if (mcmap.containsKey(mcp))
                    mchits = mcmap.get(mcp);
                mchits++;
                mcmap.put(mcp, mchits);
            }

            BasicHep3Vector axial = new BasicHep3Vector();
            axial.setV(0, 1, 0);
            HelicalTrackHit htc = (HelicalTrackHit) hit;
            if (hit instanceof HelicalTrackCross) {
                HelicalTrackCross cross = (HelicalTrackCross) hit;
                List<HelicalTrackStrip> clusterlist = cross.getStrips();

                for (HelicalTrackStrip cl : clusterlist) {
                    int layer = cl.layer();
                    if (layer == 1) _hasLayerOne = true;

                    _nStripHitsPerLayer[layer - 1] = cl.rawhits().size();
                    _hitLocationPerLayer.put(layer,clusterPosition(cl));
                    _nhitsNew++;
                    double axdotu = VecOp.dot(cl.u(), axial);
                    boolean isAxial = false;
                    if (axdotu > 0.5) {
                        isAxial = true;
                        _nAxialhits++;
                    } else _nZhits++;
                    List<MCParticle> mcPartList = cl.MCParticles();
                    _nMCHitsPerLayer[layer-1] = mcPartList.size();
                    for (MCParticle mcp : mcPartList) {
                        Integer mchits = 0;
                        if (mcmapAll.containsKey(mcp))
                            mchits = mcmapAll.get(mcp);
                        mchits++;
                        mcmapAll.put(mcp, mchits);
                        if (isAxial) {
                            Integer mchitsAxial = 0;
                            if (mcmapAxial.containsKey(mcp))
                                mchitsAxial = mcmapAxial.get(mcp);
                            mchitsAxial++;
                            mcmapAxial.put(mcp, mchitsAxial);
                        } else {
                            Integer mchitsZ = 0;
                            if (mcmapZ.containsKey(mcp))
                                mchitsZ = mcmapZ.get(mcp);
                            mchitsZ++;
                            mcmapZ.put(mcp, mchitsZ);
                        }
                    }
                }
            } else {
                _nhitsNew++;
                _nAxialhits++;
                HelicalTrack2DHit hit2d = (HelicalTrack2DHit) hit;
                List<MCParticle> mcPartList = hit2d.getMCParticles();
                //assume that lone hits are all axial
                boolean isAxial = true;
                for (MCParticle mcp : mcPartList) {
                    Integer mchits = 0;
                    if (mcmapAll.containsKey(mcp))
                        mchits = mcmapAll.get(mcp);
                    mchits++;
                    mcmapAll.put(mcp, mchits);
                    Integer mchitsAxial = 0;
                    if (mcmapAxial.containsKey(mcp))
                        mchitsAxial = mcmapAxial.get(mcp);
                    mchitsAxial++;
                    mcmapAxial.put(mcp, mchitsAxial);
                }
            }
        }

        //  Find the MCParticle that has the most hits on the track

        int nbest = 0;
        MCParticle mcbest = null;
        for (MCParticle mcp : mcmap.keySet()) {
            int count = mcmap.get(mcp);
            if (count > nbest) {
                nbest = count;
                mcbest = mcp;
            }
        }

        if (nbest > 0)
            _mcp = mcbest;
        _purity = (double) nbest / (double) _nhits;
        _nbadhits = _nhits - nbest;


//single strip layer accounting.
        int nbestAll = 0;
        MCParticle mcbestAll = null;
        for (MCParticle mcp : mcmapAll.keySet()) {
            int count = mcmapAll.get(mcp);
            if (count > nbestAll) {
                nbestAll = count;
                mcbestAll = mcp;
            }
        }

        if (nbestAll > 0)
            _mcpNew = mcbestAll;
        _purityNew = (double) nbestAll / (double) _nhitsNew;
        _nbadhitsNew = _nhitsNew - nbestAll;

        for (TrackerHit hit : trk.getTrackerHits()) {
            HelicalTrackHit htc = (HelicalTrackHit) hit;
            if (hit instanceof HelicalTrackCross) {
                HelicalTrackCross cross = (HelicalTrackCross) hit;
                List<HelicalTrackStrip> clusterlist = cross.getStrips();
                for (HelicalTrackStrip cl : clusterlist){
                    trackLayerList.add(cl.layer());
                    if (!(cl.MCParticles().contains(_mcpNew))) {
                        badHitList.add(cl.layer());
                        badhits.put(_mcpNew, cross);
                    }
                    if(cl.MCParticles().size()>1)
                        sharedHitList.add(cl.layer());
                }
            }
        }



        if (_nAxialhits > 0)
            if (mcmapAxial.containsKey(_mcpNew))
                _nbadAxialhits = _nAxialhits - mcmapAxial.get(_mcpNew);
            else _nbadAxialhits = _nAxialhits;
        if (_nZhits > 0)
            if (mcmapZ.containsKey(_mcpNew))
                _nbadZhits = _nZhits - mcmapZ.get(_mcpNew);
            else _nbadZhits = _nZhits;

    }

    public Hep3Vector clusterPosition(HelicalTrackStrip cl) {
        Hep3Vector corigin = cl.origin();
        Hep3Vector u = cl.u();
        double umeas = cl.umeas();
        Hep3Vector uvec = VecOp.mult(umeas, u);
        return VecOp.add(corigin, uvec);

    }

    public MCParticle getMCParticle() {
        return _mcp;
    }

    public int getNHits() {
        return _nhits;
    }

    public int getNBadHits() {
        return _nbadhits;
    }

    public double getPurity() {
        return _purity;
    }

    public MCParticle getMCParticleNew() {
        return _mcpNew;
    }

    public int getNHitsNew() {
        return _nhitsNew;
    }

    public int getNAxialHits() {
        return _nAxialhits;
    }

    public int getNZHits() {
        return _nZhits;
    }

    public int getNBadHitsNew() {
        return _nbadhitsNew;
    }

    public double getPurityNew() {
        return _purityNew;
    }

    public int getNBadAxialHits() {
        return _nbadAxialhits;
    }

    public int getNBadZHits() {
        return _nbadZhits;
    }

    public boolean hasLayerOne() {
        return _hasLayerOne;
    }

    public Hep3Vector getClusterPosition(Integer layer) {
        return _hitLocationPerLayer.get(layer);
    }

    public int getNumberOfMCParticles(int layer) {
        return _nMCHitsPerLayer[layer - 1];
    }

    public int getNumberOfStripHits(int layer) {
        return _nStripHitsPerLayer[layer - 1];
    }

    public List<Integer> getBadHitList() {
        return badHitList;
    }
     public List<Integer> getSharedHitList() {
        return sharedHitList;
    }
     
       public List<Integer> getTrackLayerList() {
        return trackLayerList;
    }

    public Map<MCParticle, HelicalTrackCross> getBadHits() {
        return badhits;
    }
}
