/*
 * FindableTrack.java
 *
 * Created on October 24, 2008, 9:50 PM
 *
 */
package org.lcsim.hps.recon.tracking;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.IDetectorElementContainer;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.fit.helicaltrack.HelixParamCalculator;
import org.lcsim.fit.helicaltrack.HitIdentifier;
import org.lcsim.geometry.subdetector.BarrelEndcapFlag;
import org.lcsim.recon.tracking.seedtracker.SeedLayer;
import org.lcsim.recon.tracking.seedtracker.SeedLayer.SeedType;
import org.lcsim.recon.tracking.seedtracker.SeedStrategy;

/**
 *
 * @author Richard Partridge
 * @version $Id: FindableTrack.java,v 1.4 2012/11/08 01:22:41 omoreno Exp $
 */
public class FindableTrack {

    public enum Ignore {
        NoPTCut, NoDCACut, NoZ0Cut, NoSeedCheck, NoConfirmCheck, NoMinHitCut
    };
    
    private double _bfield;
    private RelationalTable<SimTrackerHit, MCParticle> _hittomc;
    private HitIdentifier _ID;
    private int _nlayersTot=10;
    
    public FindableTrack(EventHeader event, List<SimTrackerHit> simTrackerHits){
        
        // Get the magnetic field
        Hep3Vector IP = new BasicHep3Vector(0., 0., 1.);
        _bfield = event.getDetector().getFieldMap().getField(IP).y();
        
        //  Instantiate the hit identifier class
        _ID = new HitIdentifier();
        
        //  Create a relational table that maps SimTrackerHits to MCParticles
        _hittomc = new BaseRelationalTable<SimTrackerHit, MCParticle>(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        
        List<List<SimTrackerHit>> simTrackerHitCollections = new ArrayList<List<SimTrackerHit>>();
        
        // If the collection of SimTrackerHits is not specified get the collection from the event.
        // Otherwise, add the collection to the list of collections to be processed.
        if(simTrackerHits == null) simTrackerHitCollections.addAll(event.get(SimTrackerHit.class));
        else simTrackerHitCollections.add(simTrackerHits);
        
        //  Loop over the SimTrackerHits and fill in the relational table
        for (List<SimTrackerHit> simlist : simTrackerHitCollections){
            for (SimTrackerHit simhit : simlist)
                if (simhit.getMCParticle() != null)
                    _hittomc.add(simhit, simhit.getMCParticle());
        }
    }
    
    public FindableTrack(EventHeader event, List<SimTrackerHit> simTrackerHits, int nLayersTot){
        this(event, simTrackerHits);
        this._nlayersTot = nLayersTot;
    }
    
    public FindableTrack(EventHeader event){
        this(event, null);
    }

    public FindableTrack(EventHeader event, int nLayersTot){
        this(event, null, nLayersTot);
    }
    
    public boolean isFindable(MCParticle mcp, List<SeedStrategy> slist, Ignore ignore) {
        List<Ignore> ignores = new ArrayList<Ignore>();
        ignores.add(ignore);
        return isFindable(mcp, slist, ignores);
    }

    public boolean isFindable(MCParticle mcp, List<SeedStrategy> slist) {
        return isFindable(mcp, slist, new ArrayList<Ignore>());
    }

    public boolean isFindable(MCParticle mcp, List<SeedStrategy> slist, List<Ignore> ignores) {

        //  We can't find neutral particles'
        if (mcp.getCharge() == 0)
            return false;

        //  Find the helix parameters in the L3 convention used by org.lcsim
        HelixParamCalculator helix = new HelixParamCalculator(mcp, _bfield);

        //  We haven't yet determined the track is findable
        boolean findable = false;

        //  Loop over strategies and check if the track is findable
        for (SeedStrategy strat : slist) {

            //  Check the MC Particle's pT
            if (!CheckPT(helix, ignores, strat))
                continue;

            //  Check the MC Particle's DCA
            if (!CheckDCA(helix, ignores, strat))
                continue;

            //  Check the MC Particle's Z0
            if (!CheckZ0(helix, ignores, strat))
                continue;

            //  Check that we have hits on the seed layers
            if (!CheckSeed(mcp, ignores, strat))
                continue;

            //  Check that we have the required confirmation hits
            if (!CheckConfirm(mcp, ignores, strat))
                continue;

            //  Check for the minimum number of hits
            if (!CheckMinHits(mcp, ignores, strat))
                continue;

            //  Passed all the checks - track is findable
            findable = true;
            break;
        }

        return findable;
    }

    public int LayersHit(MCParticle mcp) {

        //  Get the list of hits associated with the MCParticle
        Set<SimTrackerHit> hitlist = _hittomc.allTo(mcp);

        //  Create a set of the identifiers for the hit layers
        Set<String> idset = new HashSet<String>();

        //  Create the set of identifiers
        for (SimTrackerHit simhit : hitlist) {

            String identifier_old = _ID.Identifier(getDetectorElement(simhit));
            String identifier = _ID.Identifier(simhit);
            if (!idset.contains(identifier))
                idset.add(identifier);
        }

        return idset.size();
    }
    
    public boolean isTrackFindable(MCParticle mcParticle, int nLayers){
        
        if(nLayers%2 == 1) throw new RuntimeException(this.getClass().getSimpleName() + ": The required number of layers hit must be even");
        
        // A neutral particle can't be found
        if(mcParticle.getCharge() == 0) return false;
        
        // Get the list of SimTrackerHits associated with the MC particle
        Set<SimTrackerHit> simHits = _hittomc.allTo(mcParticle);
        
        // Find the layers hit
        boolean[] layerHit = new boolean[_nlayersTot];
        for(SimTrackerHit simHit : simHits){
            layerHit[simHit.getLayer()-1] = true;
        }
        
        int nLayersHit = 0;
        // Check how many pairs of layers were hit
        for(int index = 0; index < _nlayersTot; index += 2){
            if(layerHit[index] && layerHit[index+1]) nLayersHit += 2; 
        }
        
        return nLayersHit >= nLayers;
    }
    
    public Set<SimTrackerHit> getSimTrackerHits(MCParticle mcParticle){
        return _hittomc.allTo(mcParticle);
    }
    
    public boolean InnerTrackerIsFindable(MCParticle mcp, int nlayers, boolean printout) {
        Set<SimTrackerHit> hitlist = _hittomc.allTo(mcp);
        boolean[] layerHit={false,false,false,false,false,false,false,false,false,false,false,false};
        for (SimTrackerHit simhit : hitlist) {
            layerHit[simhit.getLayer()-1]=true;
        }
        for(int i=0;i<nlayers;i++){
            System.out.println(layerHit[i]);
            if(layerHit[i]==false)return false;
        }
        return true;
    }

       public boolean InnerTrackerIsFindable(MCParticle mcp, int nlayers) {
        Set<SimTrackerHit> hitlist = _hittomc.allTo(mcp);
        boolean[] layerHit={false,false,false,false,false,false,false,false,false,false,false,false};
        for (SimTrackerHit simhit : hitlist) {
            layerHit[simhit.getLayer()-1]=true;
        }
        for(int i=0;i<nlayers;i++){
            if(layerHit[i]==false)return false;
        }
        return true;
    }

      public boolean OuterTrackerIsFindable(MCParticle mcp, int start) {
        Set<SimTrackerHit> hitlist = _hittomc.allTo(mcp);
        boolean[] layerHit={false,false,false,false,false,false,false,false,false,false,false,false};
        for (SimTrackerHit simhit : hitlist) {
            layerHit[simhit.getLayer()-1]=true;
        }
        for(int i=start;i<_nlayersTot;i++){
            if(layerHit[i]==false)return false;
        }
        return true;
    }

    private boolean CheckPT(HelixParamCalculator helix, List<Ignore> ignores, SeedStrategy strat) {

        //  First see if we are skipping this check
        if (ignores.contains(Ignore.NoPTCut))
            return true;

        return helix.getMCTransverseMomentum() >= strat.getMinPT();
    }

    private boolean CheckDCA(HelixParamCalculator helix, List<Ignore> ignores, SeedStrategy strat) {

        //  First see if we are skipping this check
        if (ignores.contains(Ignore.NoDCACut))
            return true;

        return Math.abs(helix.getDCA()) <= strat.getMaxDCA();
    }

    private boolean CheckZ0(HelixParamCalculator helix, List<Ignore> ignores, SeedStrategy strat) {

        //  First see if we are skipping this check
        if (ignores.contains(Ignore.NoZ0Cut))
            return true;

        return Math.abs(helix.getZ0()) <= strat.getMaxZ0();
    }

    private boolean CheckSeed(MCParticle mcp, List<Ignore> ignores, SeedStrategy strat) {

        //  First see if we are skipping this check
        if (ignores.contains(Ignore.NoSeedCheck))
            return true;

        return HitCount(mcp, strat.getLayers(SeedType.Seed)) == 3;
    }

    private boolean CheckConfirm(MCParticle mcp, List<Ignore> ignores, SeedStrategy strat) {

        //  First see if we are skipping this check
        if (ignores.contains(Ignore.NoConfirmCheck))
            return true;

        return HitCount(mcp, strat.getLayers(SeedType.Confirm)) >= strat.getMinConfirm();
    }

    private boolean CheckMinHits(MCParticle mcp, List<Ignore> ignores, SeedStrategy strat) {

        //  First see if we are skipping this check
        if (ignores.contains(Ignore.NoMinHitCut))
            return true;

        return HitCount(mcp, strat.getLayerList()) >= strat.getMinHits();
    }

    private int HitCount(MCParticle mcp, List<SeedLayer> lyrlist) {

        //  Get the list of hits associated with the MCParticle
        Set<SimTrackerHit> hitlist = _hittomc.allTo(mcp);

        //  Count the number of layers with hits in them
        int hitcount = 0;
        for (SeedLayer lyr : lyrlist)
            //  Loop over the hits for this MCParticle
            for (SimTrackerHit simhit : hitlist) {

                //  Get the detector element for this hit
//                IDetectorElement de = getDetectorElement(simhit);

                //  Check names
//                String detname_old = _ID.getName(de);
                String detname_new = simhit.getSubdetector().getName();
                //               if (!detname_old.equals(detname_new)) {
                //                   System.out.println("Detector name mismatch - old: "+detname_old+ " new: "+detname_new);
                //               }
                //               int layer_old = _ID.getLayer(de);
                int layer_new = simhit.getLayer();
                //               if (layer_old != layer_new) {
                //                   System.out.println("Layer number mismatch - old: "+layer_old+" new: "+layer_new);
                //               }
//                BarrelEndcapFlag be_old = _ID.getBarrelEndcapFlag(de);
                BarrelEndcapFlag be_new = simhit.getBarrelEndcapFlag();
                //               if (!be_old.equals(be_new)) {
                //                   System.out.println("BarrelEndcapFlag mismatch - old: "+be_old+" new: "+be_new);
                //               }

                //  See if this hit is on the layer we are checking
//               if (!lyr.getDetName().equals(_ID.getName(de))) continue;
//                if (lyr.getLayer() != _ID.getLayer(de)) continue;
//                if (!lyr.getBarrelEndcapFlag().equals(_ID.getBarrelEndcapFlag(de)))
//                    continue;
                if (!lyr.getDetName().equals(detname_new))
                    continue;
                if (lyr.getLayer() != layer_new)
                    continue;
                if (!lyr.getBarrelEndcapFlag().equals(be_new))
                    continue;
                hitcount++;
                break;
            }

        return hitcount;
    }

    private IDetectorElement getDetectorElement(SimTrackerHit hit) {
        IDetectorElementContainer cont = DetectorElementStore.getInstance().find(hit.getIdentifier());
        IDetectorElement de;
        if (cont.isEmpty())
            throw new RuntimeException("Detector Container is empty!");
        else
            de = cont.get(0);
        return de;
    }
}