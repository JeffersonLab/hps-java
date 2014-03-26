package org.hps.users.meeg;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hps.conditions.deprecated.SvtUtils;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.Identifier;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;

/**
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: LCIOTrackAnalysis.java,v 1.3 2013/10/24 18:11:43 meeg Exp $
 */
public class LCIOTrackAnalysis {

    protected Track track;
    protected MCParticle _mcp = null;
    protected double _purity;
    protected int _nhits;
    protected int _nbadhits;
    private int _nAxialhits;
    private int _nZhits;
    protected boolean _hasLayerOne;
    private int[] _nStripHitsPerLayer = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    protected Map<Integer, Hep3Vector> _hitLocationPerLayer = new HashMap<Integer, Hep3Vector>();
    protected int _nhitsNew;

    public Track getTrack() {
        return track;
    }

    public LCIOTrackAnalysis(Track trk, RelationalTable hittomc, RelationalTable hittostrip, RelationalTable hittorotated) {
        track = trk;

        //  Get the number of hits on the track
        _nhits = trk.getTrackerHits().size();

        //  Create a map containing the number of hits for each MCParticle associated with the track
        Map<MCParticle, Integer> mcmap = new HashMap<MCParticle, Integer>();
        _hasLayerOne = false;
        //  Loop over the hits on the track (HelicalTrackHits)
        for (TrackerHit rotatedHit : trk.getTrackerHits()) {
            TrackerHit hit = (TrackerHit) hittorotated.from(rotatedHit);
            //  get the set of MCParticles associated with this hit and update the hit count for each MCParticle
            Set<MCParticle> mclist = hittomc.allFrom(hit);
//            System.out.println("MCParticle count: " + mclist.size());
            for (MCParticle mcp : mclist) {
                if (mcp != null) {
//                System.out.println(mcp.getOrigin());
                    Integer mchits = 0;
                    if (mcmap.containsKey(mcp)) {
                        mchits = mcmap.get(mcp);
                    }
                    mchits++;
                    mcmap.put(mcp, mchits);
                }
            }

            Set<TrackerHit> hitlist = hittostrip.allFrom(hit);
            for (TrackerHit cl : hitlist) {
                int layer = -1;
                int module = -1;
                List<RawTrackerHit> rawHits = cl.getRawHits();
//                System.out.println("RawHits: " + rawHits.size());
                for (RawTrackerHit rawHit : rawHits) {
//                    System.out.println(rawHit.getCellID());
                    IIdentifier id = new Identifier(rawHit.getCellID());
                    int newLayer = SvtUtils.getInstance().getHelper().getValue(id, "layer");
                    if (layer != -1 && layer != newLayer) {
                        System.out.format("TrackerHit has hits from multiple layers: %d and %d\n", layer, newLayer);
                    }
                    layer = newLayer;
                    int newModule = SvtUtils.getInstance().getHelper().getValue(id, "module");
                    if (module != -1 && module != newModule) {
                        System.out.format("TrackerHit has hits from multiple modules: %d and %d\n", module, newModule);
                    }
                    module = newModule;
//                    System.out.println(SvtUtils.getInstance().getHelper().getValue(id, "strip"));
                }
//                System.out.format("layer %d, module %d\n", layer, module);
                if (layer == 1) {
                    _hasLayerOne = true;
                }


                _nStripHitsPerLayer[layer - 1] = rawHits.size();
                _hitLocationPerLayer.put(layer, new BasicHep3Vector(cl.getPosition()));
                _nhitsNew++;

                boolean isAxial = SvtUtils.getInstance().isAxial(SvtUtils.getInstance().getSensor(module, layer - 1));
                if (isAxial) {
                    _nAxialhits++;
                } else {
                    _nZhits++;

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

        if (nbest > 0) {
            _mcp = mcbest;
        }
        _purity = (double) nbest / (double) _nhits;
        _nbadhits = _nhits - nbest;
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

    public int getNHitsNew() {
        return _nhitsNew;
    }

    public int getNAxialHits() {
        return _nAxialhits;
    }

    public int getNZHits() {
        return _nZhits;
    }

    public boolean hasLayerOne() {
        return _hasLayerOne;
    }

    public Hep3Vector getClusterPosition(Integer layer) {
        return _hitLocationPerLayer.get(layer);
    }

    public int getNumberOfStripHits(int layer) {
        return _nStripHitsPerLayer[layer - 1];
    }
}
