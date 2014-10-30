/*
 * SeedTracker.java
 *
 * Created on August 16, 2005, 8:54 AM
 *
 */
package org.hps.recon.tracking.nobfield;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import java.util.*;
import org.hps.recon.tracking.MaterialManager;
import org.hps.recon.tracking.MaterialSupervisor;
import org.lcsim.detector.ITransform3D;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.seedtracker.DefaultStrategy;
import org.lcsim.recon.tracking.seedtracker.HitManager;
import org.lcsim.recon.tracking.seedtracker.MakeTracks;
import org.lcsim.recon.tracking.seedtracker.SeedCandidate;
import org.lcsim.recon.tracking.seedtracker.SeedStrategy;
import org.lcsim.recon.tracking.seedtracker.TrackCheck;
import org.lcsim.recon.tracking.seedtracker.diagnostic.ISeedTrackerDiagnostics;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Tracking algorithm based on forming straight track seeds from all 3-hit combinations,
 * confirming this tentantive track by requiring additional hits, and extending
 * the track to additional layers. The operation of the algorithm is controlled
 * by a list of SeedStrategy that define the tracker layers to be used and the
 * cuts on the tracking algorithm.
 *
 * @author Mathew Graham <mgraham.slac.stanford.edu>
 * Copied/Modified from org.lcsim.recon.tracking.SeedTracker
 * 
 */
public class StraightTracker extends Driver {

    protected List<SeedStrategy> _strategylist;
    protected ISeedTrackerDiagnostics _diag = null;
    protected MaterialManager _materialmanager = new MaterialManager();
    protected HitManager _hitmanager;
    protected StraightTrackFitter _helixfitter;
    protected StraightTrackFinder _finder;
    protected MakeTracks _maketracks;
    protected Hep3Vector _IP = new BasicHep3Vector(0., 0., 0.);
    protected double _bfield = 0.;
    protected boolean _forceBField = false;
    protected double _rtrk = 1000.;
    protected boolean _autosectoring = false;
    protected AIDA aida = AIDA.defaultInstance();
    protected boolean _timing = false;
    protected String _inputCol = "HelicalTrackHits";
    private int _iterativeConfirmedFits = 0;
    private boolean _debug = false;

    /**
     * Creates a new instance of SeedTracker
     */
    public StraightTracker() {
        this(new DefaultStrategy().getStrategyList(), true, true);
    }

    public StraightTracker(List<SeedStrategy> strategylist, boolean includeMS) {
        initialize(strategylist, true, includeMS);
    }

    public StraightTracker(List<SeedStrategy> strategylist, boolean useHPSMaterialManager, boolean includeMS) {
        initialize(strategylist, useHPSMaterialManager, includeMS);
    }

    private void initialize(List<SeedStrategy> strategylist, boolean useHPSMaterialManager, boolean includeMS) {
        _strategylist = strategylist;
        //  Instantiate the hit manager
        _hitmanager = new HitManager();
        //  Instantiate the material manager for HPS,  the helix fitter and seed track finder as tey depends on the material manager
        if (useHPSMaterialManager) {
            MaterialSupervisor materialSupervisor = new MaterialSupervisor(includeMS);
            _materialmanager = materialSupervisor;
            _helixfitter = new StraightTrackFitter(materialSupervisor);
        } else {
            MaterialManager materialmanager = new MaterialManager(includeMS);
            _materialmanager = materialmanager; //mess around with types here...
            _helixfitter = new StraightTrackFitter(materialmanager);
        }
        //  Instantiate the Seed Finder
        _finder = new StraightTrackFinder(_hitmanager, _helixfitter);
        //  Instantiate the Track Maker
        _maketracks = new MakeTracks();
    }

    /**
     * Set to enable debug output
     *
     * @param debug switch
     */
    public void setDebug(boolean debug) {
        _debug = true;
        _materialmanager.setDebug(debug);
        _helixfitter.setDebug(debug);
        _finder.setDebug(debug);
    }

    @Override
    protected void process(EventHeader event) {

//        System.out.println("New event");
        //  Pass the event to the diagnostics package
        if (_diag != null)
            _diag.setEvent(event);

        //  Initialize timing
        long last_time = System.currentTimeMillis();

        //  Get the hit collection from the event
        List<HelicalTrackHit> hitcol = event.get(HelicalTrackHit.class, _inputCol);
        if (_debug)
            System.out.println("In " + this.getClass().getSimpleName() + ":  Number of HelicalTrackHits=" + hitcol.size());
        //  Sort the hits for this event
        _hitmanager.OrganizeHits(hitcol);

        //  Make the timing plots if requested
        long start_time = System.currentTimeMillis();
        double dtime = ((double) (start_time - last_time)) / 1000.;
        last_time = start_time;
        if (_timing)
            aida.cloud1D("Organize Hits").fill(dtime);

        //  Make sure that we have cleared the list of track seeds in the finder
        _finder.clearTrackSeedList();

        //  Loop over strategies and perform track finding
        for (SeedStrategy strategy : _strategylist) {

            //  Set the strategy for the diagnostics
            if (_diag != null)
                _diag.fireStrategyChanged(strategy);

            //  Perform track finding under this strategy
            _finder.FindTracks(strategy, _bfield);
            if (_debug)
                System.out.println("In " + this.getClass().getSimpleName() + ":  Number of Tracks Found=" + _finder.getTrackSeeds().size());
            //  Make the timing plots if requested
            long time = System.currentTimeMillis();
            dtime = ((double) (time - last_time)) / 1000.;
            last_time = time;
            if (_timing)
                aida.cloud1D("Tracking time for strategy " + strategy.getName()).fill(dtime);
        }

        //  Get the list of final list of SeedCandidates
        List<SeedCandidate> trackseeds = _finder.getTrackSeeds();

        if (_iterativeConfirmedFits > 0) {
            // Iteratively re-fit tracks to take into account helix and hit position correlations
            if (_debug)
                System.out.printf("%s: Iteratively improve %d seeds\n", this.getClass().getSimpleName(), trackseeds.size());
            List<SeedCandidate> seedsToRemove = new ArrayList<SeedCandidate>();
            for (SeedCandidate seed : trackseeds) {
                SeedStrategy strategy = seed.getSeedStrategy();
                boolean success = false;
                for (int iterFit = 0; iterFit < _iterativeConfirmedFits; ++iterFit)
                    success = _helixfitter.FitCandidate(seed, strategy);
                if (!success)
                    seedsToRemove.add(seed);
//                else if (_debug)
//                    System.out.printf("%s: done iterating, this seed will be added to event:\n%s\n", this.getClass().getSimpleName(), seed.toString());
            }
            for (SeedCandidate badseed : seedsToRemove)
                trackseeds.remove(badseed);
        }

        //  Make tracks from the final list of track seeds
        _maketracks.Process(event, trackseeds, _bfield);

        //  Save the MC Particles that have been seeded / confirmed if diagnostics are enabled
        if (_diag != null) {
            Set<MCParticle> seededmcpset = _finder.getSeededMCParticles();
            List<MCParticle> seededmcp = new ArrayList<MCParticle>(seededmcpset);
            event.put("SeededMCParticles", seededmcp, MCParticle.class, 0);
            Set<MCParticle> confirmedmcpset = _finder.getConfirmedMCParticles();
            List<MCParticle> confirmedmcp = new ArrayList<MCParticle>(confirmedmcpset);
            event.put("ConfirmedMCParticles", confirmedmcp, MCParticle.class, 0);
        }

        //  Clear the list of track seeds accumulated in the track finder
        _finder.clearTrackSeedList();

        //  Make the total time plot if requested
        long end_time = System.currentTimeMillis();
        dtime = ((double) (end_time - start_time)) / 1000.;
        if (_timing)
            aida.cloud1D("Total tracking time").fill(dtime);

        return;
    }

    @Override
    protected void detectorChanged(Detector detector) {

        //  Only build the model when the detector is changed
        _materialmanager.buildModel(detector);

        //  Find the bfield and pass it to the helix fitter and diagnostic package
        if (!_forceBField)
            _bfield = detector.getFieldMap().getField(_IP).z();

        if (_diag != null)
            _diag.setBField(_bfield);
        _helixfitter.setBField(_bfield);

        //  Get the tracking radius
        _rtrk = _materialmanager.getRMax();

        //  Set the sectoring parameters
        if (_autosectoring)
            _hitmanager.setSectorParams(_strategylist, _bfield, _rtrk);
    }

    /**
     * Specifiy the strategies to be used by the SeedTracker algorithm. Invoking
     * this
     * method will override the default strategies defined by the
     * DefaultStrategy
     * class.
     *
     * @param strategylist List of strategies to be used
     */
    public void putStrategyList(List<SeedStrategy> strategylist) {

        //  Save the strategy list
        _strategylist = strategylist;

        //  Set the sectoring parameters
        if (_autosectoring)
            _hitmanager.setSectorParams(strategylist, _bfield, _rtrk);

        return;
    }

    public void setSectorParams(int nphi, double dz) {
        _hitmanager.setSectorParams(nphi, dz);
        _autosectoring = false;
        return;
    }

    public void setDiagnostics(ISeedTrackerDiagnostics d) {

        //  Set the diagnostic package
        _diag = d;
        _helixfitter.setDiagnostics(_diag);
        _finder.setDiagnostic(_diag);

        //  Pass the hit manager, material manager, and bfield to the diagnostic package
        if (_diag != null) {
            _diag.setMaterialManager(_materialmanager);
            _diag.setHitManager(_hitmanager);
            _diag.setBField(_bfield);
        }
    }

    public void setTimingPlots(boolean timing) {
        _timing = timing;
    }

    public void setTrkCollectionName(String name) {
        _maketracks.setTrkCollectionName(name);
    }

    public void setInputCollectionName(String name) {
        _inputCol = name;
    }

    public void setMaterialManagerTransform(ITransform3D _detToTrk) {
        _materialmanager.setTransform(_detToTrk);
    }

    /**
     * Set the maximum number of fits used to confirm or extend a seed.
     *
     * @param maxfit maximum number of fits
     */
    public void setMaxFit(int maxfit) {
        _finder.setMaxFit(maxfit);
    }

    public void setBField(double bfield) {
        _forceBField = true;
        _bfield = bfield;
    }

    public void setReferencePoint(double xref, double yref) {
        _helixfitter.setReferencePoint(xref, yref);
    }

    public void setSectorParams(boolean sector) {
        _hitmanager.setDoSectoring(sector);
    }

    /**
     * Set {@link TrackCheck} object to be used by the track finding algorithm.
     * If this method is never called, no external checking of seeds and tracks
     * is performed.
     */
    public void setTrackCheck(TrackCheck trackCheck) {
        _finder.setTrackCheck(trackCheck);
        _maketracks.setTrackCheck(trackCheck);
    }

    /**
     * Set the maximum number of iterative fits on a confirmed/extended
     * candidate.
     *
     * @param maxfits maximum number of fits
     */
    public void setIterativeConfirmed(int maxfits) {
        this._iterativeConfirmedFits = maxfits;
    }

    public void setUseDefaultXPlane(boolean useDefault) {
        _materialmanager.setDefaultXPlaneUsage(useDefault);

    }

}
