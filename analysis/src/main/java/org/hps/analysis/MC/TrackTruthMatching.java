package org.hps.analysis.MC;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lcsim.event.MCParticle;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;

/**
 *
 * @author Matt Solt
 * based off of "TrackAnalysis" Driver
 */
public class TrackTruthMatching {

    private MCParticle _mcp = null;
    private int _nhits;
    private int _nbadhits;
    private double _purity;
    private Map<MCParticle, Integer> mcmap = new HashMap<MCParticle, Integer>();
    private Map<MCParticle,boolean[]> mapGoodHitList = new HashMap<MCParticle,boolean[]>();
    private boolean[] _goodHitList = {false,false,false,false,false,false,false,false,false,false,false,false};
    private int[] _nMCHitsPerLayer = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private boolean[] dummy = {false,false,false,false,false,false,false,false,false,false,false,false};

    /**
     * Creates a new instance of TrackAnalysis
     * @return 
     */
    
    public TrackTruthMatching(Track trk, RelationalTable rawtomc) {
        doAnalysis(trk, rawtomc);
    }

    private void doAnalysis(Track trk, RelationalTable rawtomc){
        //  Get the number of hits on the track
        _nhits = trk.getTrackerHits().size()*2;

        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        //System.out.println("New Particle");
        for (TrackerHit hit : trk.getTrackerHits()) {
            //  get the set of MCParticles associated with this hit and update the hit count for each MCParticle
            System.out.println("New TrackerHit " + hit.getPosition()[0]);
            List<RawTrackerHit> rawhits = hit.getRawHits();
            Set<MCParticle> mcPartList = new HashSet<MCParticle>();
            for(RawTrackerHit rawhit : rawhits){
                System.out.println("New Raw Hit " + rawhit.getLayerNumber());
                Set<SimTrackerHit> simhits = (Set<SimTrackerHit>) rawtomc.allFrom(rawhit);
                int layer = rawhit.getLayerNumber();
                for (SimTrackerHit simhit : simhits){
                    //System.out.println("New Sim Hit");
                    if (simhit != null && simhit.getMCParticle() != null){
                        MCParticle simhitpart = simhit.getMCParticle();
                        int simlay = simhit.getLayer();
                        mcPartList.add(simhitpart);
                        System.out.println("Sim Hits " + simhit.getLayer() + " " + simhit.getMCParticle());
                        if(mapGoodHitList.get(simhitpart) == null)
                            mapGoodHitList.put(simhitpart, dummy);
                        if(mcmap.get(simhitpart) == null)
                            mcmap.put(simhitpart, 0);
                        if(mapGoodHitList.get(simhitpart)[simlay - 1] == false)
                            mcmap.put(simhitpart,mcmap.get(simhitpart) + 1);
                        mapGoodHitList.get(simhitpart)[simlay - 1] = true;
                        System.out.println("Is hit?" + mapGoodHitList.get(simhitpart)[simlay - 1]);
                        System.out.println("Nhits " + mcmap.get(simhitpart));
                    }
                }
                if(mcPartList.size() > 1){
                    for(MCParticle p : mapGoodHitList.keySet()){
                        for(int i = 0; i < 12; i++){
                            System.out.println("HitList " + p + "  " + mapGoodHitList.get(p)[i]);
                        }
                        System.out.println("mcmap " + mcmap.get(p));
                    }
                }
                _nMCHitsPerLayer[layer - 1] = mcPartList.size();
            }
        }


        int nbest = 0;
        MCParticle mcbest = null;
        
        boolean has2 = false;
        for(int i = 0; i < 12; i++){
            //System.out.println(_nMCHitsPerLayer[i]);
            if(_nMCHitsPerLayer[i] > 1){
                System.out.println("Hits per layer = " + _nMCHitsPerLayer[i]);
                has2 = true;
                break;
            }
        }
        for (MCParticle mcp : mcmap.keySet()) {
            if(has2){
                System.out.println("Loop " + mcp + "  MCmap " + mcmap.get(mcp));
                if(mapGoodHitList.get(mcp) != null){
                    boolean[] test = mapGoodHitList.get(mcp);
                    for(int i = 0; i < test.length; i++){
                        System.out.println(test[i]);
                    }
                }
            }
            int count = mcmap.get(mcp);
            if (count > nbest) {
                nbest = count;
                mcbest = mcp;
            }
        }
        
        if(has2) System.out.println("MCbest " + mcbest + "  nbest " + nbest);

        if (nbest > 0)
            _mcp = mcbest;
        _purity = (double) nbest / (double) _nhits;
        _nbadhits = _nhits - nbest;
        _goodHitList = mapGoodHitList.get(mcbest);
    }
    
    //Return MCParticle matched to track
    public MCParticle getMCParticle() {
        return _mcp;
    }

    //Returns number of tracker hits (10 or 12)
    public int getNHits() {
        return _nhits;
    }

    //Returns the number of missing hits
    //_nhits - number of MCParticle hits on track
    public int getNBadHits() {
        return _nbadhits;
    }

    //Returns purity of track truth match
    //(_nhits - _nbadhits)/ _nhits
    public double getPurity() {
        return _purity;
    }

    //Returns the number of MCParticles that contribute to the
    //tracker hit at a layer
    public int getNumberOfMCParticles(int layer) {
        return _nMCHitsPerLayer[layer - 1];
    }

    //Returns a boolean array of which hits of MCParticle contribute
    //to the track
    public boolean[] getHitList() {
        return _goodHitList;
    }
}
