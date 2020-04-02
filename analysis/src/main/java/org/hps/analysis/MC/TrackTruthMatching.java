package org.hps.analysis.MC;

import java.util.ArrayList;
import java.util.Collection;
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
 * This class matches a track to an MCParticle as well as info about each truth hit
 * The MC Particle with the most hits on a track wins.
 * First tie breaker is the innermost hits, second tie breaker is highest energy particle.
 */
public class TrackTruthMatching {

    private MCParticle _mcp = null;
    private int _nhits;
    private int _ngoodhits;
    private int _nbadhits;
    private double _purity;
    private boolean _isTop;
    private Map<MCParticle, Integer> mcmap = new HashMap<MCParticle, Integer>();
    private Map<MCParticle,Map<Integer,Boolean>> mapGoodHitList = new HashMap<MCParticle,Map<Integer,Boolean>>();
    private Map<Integer,Boolean> _goodHitList = new HashMap<Integer,Boolean>();
    private Map<Integer,Integer> _nMCHitsPerLayer = new HashMap<Integer,Integer>();
    private Set<SimTrackerHit> _hitListNotMatched = new HashSet<SimTrackerHit>();
    private Map<Integer,Set<MCParticle>> _hitMCPartList = new HashMap<Integer,Set<MCParticle>>();
    private Map<MCParticle,Map<Integer,Boolean>> mcbestlist = new HashMap<MCParticle,Map<Integer,Boolean>>();
    private Set<SimTrackerHit> simhitsontrack = new HashSet<SimTrackerHit>();
    private Set<SimTrackerHit> mcbesthits = new HashSet<SimTrackerHit>();
    private List<Integer> trackerlayerhitlist = new ArrayList<Integer>();
    
    /**
     * Creates a new instance of TrackAnalysis. If the track is Kalman do not multiply the hits by 2
     * @return 
     */
    
    public TrackTruthMatching(Track trk, RelationalTable rawtomc, List<SimTrackerHit> allsimhits) {
        doAnalysis(trk,rawtomc,allsimhits,false);
    }
    
    public TrackTruthMatching(Track trk, RelationalTable rawtomc, List<SimTrackerHit> allsimhits, boolean isKalman) {
        doAnalysis(trk, rawtomc, allsimhits, isKalman);
    }
    
    private void doAnalysis(Track trk, RelationalTable rawtomc, List<SimTrackerHit> allsimhits, boolean isKalman){
        //  Get the number of hits on the track
        _nhits = trk.getTrackerHits().size();
        
        if (!isKalman)
            _nhits = _nhits*2;
        
        _isTop = trk.getTrackStates().get(0).getTanLambda() > 0;

        for (TrackerHit hit : trk.getTrackerHits()) {
            //  get the set of MCParticles associated with this hit and update the hit count for each MCParticle
            List<RawTrackerHit> rawhits = hit.getRawHits();
            int trackhitlayer = ((RawTrackerHit) hit.getRawHits().get(0)).getLayerNumber();
            Set<MCParticle> mcPartList1 = new HashSet<MCParticle>();
            Set<MCParticle> mcPartList2 = new HashSet<MCParticle>();
            for(RawTrackerHit rawhit : rawhits){
                //Get all the SimTrackerHits associated with the raw hits
                Set<SimTrackerHit> simhits = rawtomc.allFrom(rawhit);
                for (SimTrackerHit simhit : simhits){
                    if (simhit != null && simhit.getMCParticle() != null){
                        simhitsontrack.add(simhit);
                        MCParticle simhitpart = simhit.getMCParticle();
                        int simlay = simhit.getLayer();
                        if(simlay % 2 == 1)
                            mcPartList1.add(simhitpart);
                        if(simlay % 2 == 0)
                            mcPartList2.add(simhitpart);
                        if(mapGoodHitList.get(simhitpart) == null){
                            Map<Integer,Boolean> dummy = new HashMap<Integer,Boolean>();
                            dummy.put(simlay, true);
                            mapGoodHitList.put(simhitpart, dummy);
                        }
                        if(mapGoodHitList.get(simhitpart).get(simlay) == null)
                            mapGoodHitList.get(simhitpart).put(simlay,false);
                        if(mcmap.get(simhitpart) == null)
                            mcmap.put(simhitpart, 1);
                        if(mapGoodHitList.get(simhitpart).get(simlay) == false)
                            mcmap.put(simhitpart,mcmap.get(simhitpart) + 1);
                        mapGoodHitList.get(simhitpart).put(simlay,true);
                    }
                    else{
                        System.out.println("SIM HIT IS NULL!!!!!!!!");
                    }
                }
            }
            _nMCHitsPerLayer.put(trackhitlayer,mcPartList1.size());
            _nMCHitsPerLayer.put(trackhitlayer + 1,mcPartList2.size());
            _hitMCPartList.put(trackhitlayer,mcPartList1);
            _hitMCPartList.put(trackhitlayer + 1,mcPartList2);
            trackerlayerhitlist.add(trackhitlayer);
            trackerlayerhitlist.add(trackhitlayer + 1);
        }
        
        //Create good hit map list
        for (TrackerHit hit : trk.getTrackerHits()){
            List<RawTrackerHit> rawhits = hit.getRawHits();
            for(RawTrackerHit rawhit : rawhits){
                int layer = rawhit.getLayerNumber();
                for(MCParticle p : mapGoodHitList.keySet()){
                    if(mapGoodHitList.get(p).get(layer) == null)
                        mapGoodHitList.get(p).put(layer,false);
                }
            }
        }
        

        _ngoodhits = GetMax(mcmap.values());
        MCParticle mcbest = null;
        
        boolean has2 = false;
        for(int i = 0; i < 12; i++){
            if(_nMCHitsPerLayer.get(i+1) == null) continue;
            if(_nMCHitsPerLayer.get(i+1) > 1){
                has2 = true;
                break;
            }
        }
        
        has2 = false;
        
        //Choose a list of the best MC Particles
        //If more than one, choose the one with the innermost hits
        for (MCParticle mcp : mcmap.keySet()) {
            if(has2){
                if(mapGoodHitList.get(mcp) != null){
                    for(int i = 0; i < mapGoodHitList.get(mcp).size(); i++){
                        System.out.println(mapGoodHitList.get(mcp).get(i+1));
                    }
                }
            }
            if (mcmap.get(mcp) == _ngoodhits) {
                mcbestlist.put(mcp, mapGoodHitList.get(mcp));
            }
        }
        
        //If there is more than 1 best MC, chose the one with the higher energy
        if(mcbestlist.size() > 1)
            mcbest = ChooseBest(mcbestlist,trackerlayerhitlist);
        else if(mcbestlist.size() == 1){
            List<MCParticle> plist = new ArrayList<MCParticle>();
            plist.addAll(mcbestlist.keySet());
            mcbest = plist.get(0);
        }

        if (_ngoodhits > 0)
            _mcp = mcbest;
        _purity = (double) _ngoodhits / (double) _nhits;
        _nbadhits = _nhits - _ngoodhits;
        _goodHitList = mapGoodHitList.get(mcbest);
        
        
        
        for(SimTrackerHit hit : allsimhits){
            if(hit.getMCParticle().equals(mcbest))
                mcbesthits.add(hit);
        }
        
        for(SimTrackerHit besthit : mcbesthits){
            boolean hastrackerhit = false;
            for(SimTrackerHit hit : simhitsontrack){
                if(hit.equals(besthit)){
                    hastrackerhit = true;
                    break;
                }
            }
            if(!hastrackerhit)
                _hitListNotMatched.add(besthit);
        }
    }
    
    //Get the maximum integer in the collection
    private int GetMax(Collection<Integer> ilist){
        int imax = 0;
        for(Integer i : ilist){
            if(i.intValue() > imax){
                imax = i.intValue();
            }
        }
        return imax;
    }
    
    //This function chooses the particle with the highest energy
    //This function needs to be improved
    private MCParticle ChooseBest(Map<MCParticle,Map<Integer,Boolean>> pmap,List<Integer> trackerlayerhitlist){
        List<MCParticle> plist = new ArrayList<MCParticle>();
        plist.addAll(pmap.keySet());
        MCParticle Pbest = plist.get(0);
        double maxenergy = 0;
        for(Map.Entry<MCParticle,Map<Integer,Boolean>> map : pmap.entrySet()){
            MCParticle p = map.getKey();
            double energy = p.getEnergy();
            if(energy > maxenergy){
                energy = maxenergy;
                Pbest = p;
            }
            
        }
        return Pbest;
    }
    
    //Return MCParticle matched to track
    public MCParticle getMCParticle() {
        return _mcp;
    }

    //Returns number of tracker hits (10 or 12)
    public int getNHits() {
        return _nhits;
    }

    //Returns the number of MCParticle hits on track
    public int getNGoodHits() {
        return _ngoodhits;
    }
    
    //Returns the number of missing hits
    //_nhits - number of MCParticle hits on track
    public int getNBadHits() {
        return _nbadhits;
    }

    //Returns purity of track truth match
    //_ngoodhits / _nhits
    public double getPurity() {
        return _purity;
    }
    
    //Returns true if the track is in the top volume
    //false otherwise
    public boolean isTop() {
        return _isTop;
    }

    //Returns the number of MCParticles that contribute to the
    //tracker hit at a layer
    public int getNumberOfMCParticles(int layer) {
        if(_nMCHitsPerLayer.get(layer) == null)
            return 0;
        else
            return _nMCHitsPerLayer.get(layer);
    }
    
    //Returns list of all MCParticles associated with a tracker hit
    //on a layer
    public Set<MCParticle> getHitMCParticleList(int layer) {
        return _hitMCPartList.get(layer);
    }

    
    //Returns a boolean of which hits of MCParticle contribute
    //to the track
    public Boolean getHitList(int layer) {
        return _goodHitList.get(layer);
    }
    
    //Returns a list of SimTrackerHits for the matched MCParticle
    //that does not contribute to the tracker hits on the tracks
    public Set<SimTrackerHit> getHitListNotMatched() {
        return _hitListNotMatched;
    }
    
}
