package org.hps.recon.tracking;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.lcsim.event.MCParticle;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.recon.tracking.seedtracker.Sector;
import org.lcsim.recon.tracking.seedtracker.SeedCandidate;
import org.lcsim.recon.tracking.seedtracker.SeedStrategy;
import org.lcsim.recon.tracking.seedtracker.TrackCheck;
import org.lcsim.recon.tracking.seedtracker.diagnostic.ISeedTrackerDiagnostics;

public class SeedTrackFinder {

    private HitManager _hitmanager;
    private HelixFitter _helixfitter;
    private ConfirmerExtender _confirmer;
    private List<SeedCandidate> _trackseeds;
    private ISeedTrackerDiagnostics _diag = null;
    private Set<MCParticle> _seededmcp;
    private Set<MCParticle> _confirmedmcp;
    TrackCheck _trackCheck; // set by SeedTracker
    private boolean _debug = false;
    private boolean _applySectorBinning = false;

    /**
     * Creates a new instance of SeedTrackFinder
     */
    public SeedTrackFinder(HitManager hitmanager, HelixFitter helixfitter) {

        //  Save the pointers to the hit manager and helix fitter classes
        _hitmanager = hitmanager;
        _helixfitter = helixfitter;

        //  Instantiate the Confirmer/Extender and Seed Candidate merging classes
        _confirmer = new ConfirmerExtender(_hitmanager, _helixfitter);

        //  Create a list of track seeds that have been found
        _trackseeds = new ArrayList<SeedCandidate>();

        //  Create a set of MC Particles that have been seeded, confirmed
        _seededmcp = new HashSet<MCParticle>();
        _confirmedmcp = new HashSet<MCParticle>();

    }

    public void setDiagnostic(ISeedTrackerDiagnostics d) {

        //  Setup the diagnostics for this class and the classes used by this class
        _diag = d;
        _confirmer.setDiagnostics(_diag);
    }

    public boolean FindTracks(SeedStrategy strategy, double bfield) {

        //  Instantiate the fast hit checker
        FastCheck checker = new FastCheck(strategy, bfield);
        if (_applySectorBinning)
            checker.setDoSectorBinCheck(_hitmanager.getSectorManager());

        //  Find the valid sector combinations
        SeedSectoring ss = new SeedSectoring(_hitmanager, strategy, bfield, _applySectorBinning);
        List<List<Sector>> sslist = ss.SeedSectors();

        //  Loop over the valid sector combinations
        for (List<Sector> slist : sslist) {

            //  Loop over the first seed layer
            for (HelicalTrackHit hit1 : slist.get(0).Hits()) {

                //  Loop over the second seed layer and check that we have a hit pair consistent with our strategy
                for (HelicalTrackHit hit2 : slist.get(1).Hits()) {

                    //  Call _trackCheck if set
                    if (_trackCheck != null) {
                        SeedCandidate tempseed = new SeedCandidate(strategy, bfield);
                        tempseed.addHit(hit1);
                        tempseed.addHit(hit2);
                        if (!_trackCheck.checkSeed(tempseed))
                            continue;
                    }

                    //  Check if the pair of hits is consistent with the current strategy
                    if (!checker.TwoPointCircleCheck(hit1, hit2, null)) {
                        if (_diag != null)
                            _diag.fireCheckHitPairFailed(hit1, hit2);
                        continue;
                    }

                    //  Loop over the third seed layer and check that we have a hit triplet consistent with our strategy
                    for (HelicalTrackHit hit3 : slist.get(2).Hits()) {

                        //  Call _trackCheck if set
                        if (_trackCheck != null) {
                            SeedCandidate tempseed2 = new SeedCandidate(strategy, bfield);
                            tempseed2.addHit(hit1);
                            tempseed2.addHit(hit3);
                            if (!_trackCheck.checkSeed(tempseed2))
                                continue;

                            SeedCandidate tempseed3 = new SeedCandidate(strategy, bfield);
                            tempseed3.addHit(hit2);
                            tempseed3.addHit(hit3);
                            if (!_trackCheck.checkSeed(tempseed3))
                                continue;
                        }

                        //  Form a seed candidate from the seed hits
                        SeedCandidate seed = new SeedCandidate(strategy, bfield);
                        seed.addHit(hit1);
                        seed.addHit(hit2);
                        seed.addHit(hit3);

                        //  Check if the triplet of hits is consistent with the current strategy
                        if (!checker.ThreePointHelixCheck(hit1, hit2, hit3)) {

                            if (_diag != null) {
                                if (seed.isTrueSeed())
                                    _diag.fireCheckHitTripletFailed(hit1, hit2, hit3);
                            }
                            continue;
                        }

                        //  Form a seed candidate from the seed hits

                        //  If it's a true seed, add the MC Particle to those that were seeded
                        if (_diag != null)
                            if (seed.isTrueSeed())
                                _seededmcp.addAll(seed.getMCParticles());

                        if (_debug)
                            System.out.println(this.getClass().getSimpleName() + ": fit the candidate");

                        //  See if we can fit a helix to this seed candidate
                        boolean success = _helixfitter.FitCandidate(seed, strategy);

                        if (!success)
                            continue;

                        if (_debug)
                            System.out.println(this.getClass().getSimpleName() + ": fit success");

                        //  Save the helix fit
                        seed.setHelix(_helixfitter.getHelix());

                        // Check the seed - hook for plugging in external constraint
                        if (_trackCheck != null) {
                            if (!_trackCheck.checkSeed(seed))
                                continue;
                        }

                        //  See if we can confirm this seed candidate
                        success = _confirmer.Confirm(seed, strategy, bfield);
                        if (!success)
                            continue;

                        if (_debug)
                            System.out.println(this.getClass().getSimpleName() + ": confirmed seed");

                        //  Confirmed a seed - if it's a true seed, add the MC Particle to those that were confirmed
                        if (_diag != null)
                            if (seed.isTrueSeed())
                                _confirmedmcp.addAll(seed.getMCParticles());

                        if (_debug)
                            System.out.println(this.getClass().getSimpleName() + ": try to extend");

                        //  Try to extend each confirmed seed candidates to make a track candidate
                        List<SeedCandidate> confirmedlist = _confirmer.getResult();
                        for (SeedCandidate confirmedseed : confirmedlist) {

                            //  See if we can extend this seed candidate
                            _confirmer.Extend(confirmedseed, strategy, bfield, _trackseeds);
                        }
                    }
                }
            }
        }

        //  Done with track finding for this strategy
        if (_diag != null)
            _diag.fireFinderDone(_trackseeds, _seededmcp);

        return _trackseeds.size() > 0;
    }

    /**
     * Return the list of track candidates.
     *
     * @return track candidates
     */
    public List<SeedCandidate> getTrackSeeds() {
        return _trackseeds;
    }

    /**
     * Clear the list of track candidates accumulated from previous calls to
     * SeedTrackFinder (typically done before starting a new event).
     */
    public void clearTrackSeedList() {
        _trackseeds.clear();
        _seededmcp.clear();
        _confirmedmcp.clear();
    }

    /**
     * Set the maximum number of fits for a given seed in a confirm or extend step.
     *  
     * @param maxfits maximum number of fits
     */
    public void setMaxFit(int maxfits) {
        _confirmer.setMaxFit(maxfits);
    }

    /**
    * Set to enable the sectoring to use the sector bins in checking for consistent hits.
    *
    * @param applySectorBinning apply sector binning switch
    */
    public void setApplySectorBinning(boolean applySectorBinning) {
        this._applySectorBinning = applySectorBinning;
    }

    /**
     * Return the list of MCParticles that formed valid 3-hit seeds.
     *
     * @return list of seeded MCParticles
     */
    public Set<MCParticle> getSeededMCParticles() {
        return _seededmcp;
    }

    /**
     * Return the list of confirmed MCParticles.
     *
     * @return confirmed MCParticles
     */
    public Set<MCParticle> getConfirmedMCParticles() {
        return _confirmedmcp;
    }

    /**
     * Return the ConfirmerExtender
     * 
     * @return confirmer/extender object
     * 
     */
    public ConfirmerExtender getConfirmer() {
        return _confirmer;
    }
}
