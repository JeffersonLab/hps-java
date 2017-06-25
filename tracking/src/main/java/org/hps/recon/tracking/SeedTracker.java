/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.recon.tracking;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.lcsim.detector.ITransform3D;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.seedtracker.DefaultStrategy;
import org.lcsim.recon.tracking.seedtracker.MakeTracks;
import org.lcsim.recon.tracking.seedtracker.SeedCandidate;
import org.lcsim.recon.tracking.seedtracker.SeedStrategy;
import org.lcsim.recon.tracking.seedtracker.TrackCheck;
import org.lcsim.recon.tracking.seedtracker.diagnostic.ISeedTrackerDiagnostics;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Class extending lcsim version to allow extra flexibility
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 * @author Miriam Diamond <mdiamond@slac.stanford.edu>
 */
public class SeedTracker extends Driver {

    protected List<SeedStrategy> _strategylist;
    protected ISeedTrackerDiagnostics _diag = null;
    protected MaterialManager _materialmanager = new MaterialManager();
    protected HitManager _hitmanager;
    protected HelixFitter _helixfitter;
    protected SeedTrackFinder _finder;
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

    /** Creates a new instance of SeedTracker */
    public SeedTracker() {
        this(new DefaultStrategy().getStrategyList());
    }

    public SeedTracker(List<SeedStrategy> strategylist) {
        _strategylist = strategylist;

        //  Instantiate the material manager
        // _materialmanager = new MaterialManager();

        //  Instantiate the hit manager
        _hitmanager = new HitManager();

        //  Instantiate the helix finder
        _helixfitter = new HelixFitter(_materialmanager);

        //  Instantiate the Seed Finder
        _finder = new SeedTrackFinder(_hitmanager, _helixfitter);

        //  Instantiate the Track Maker
        _maketracks = new MakeTracks();
    }

    public SeedTracker(List<SeedStrategy> strategylist, boolean useHPSMaterialManager, boolean includeMS) {
        this(strategylist);
        initialize(strategylist, useHPSMaterialManager, includeMS);
    }

    private void initialize(List<SeedStrategy> strategylist, boolean useHPSMaterialManager, boolean includeMS) {

        // Explicitly only replace the objects that might change to avoid getting the lcsim versions

        //  Instantiate the material manager for HPS,  the helix fitter and seed track finder as tey depends on the material manager
        if (useHPSMaterialManager) {
            MaterialSupervisor materialSupervisor = new MaterialSupervisor(includeMS);
            materialSupervisor.setDebug(true);
            _materialmanager = materialSupervisor;
            _helixfitter = new HelixFitter(materialSupervisor);
        } else {
            MaterialManager materialmanager = new MaterialManager(includeMS);
            _materialmanager = materialmanager; //mess around with types here...
            _helixfitter = new HelixFitter(materialmanager);
        }

        //  Instantiate the helix finder since it depends on the material manager
        _finder = new SeedTrackFinder(_hitmanager, (HelixFitter) _helixfitter);

    }

    public void setIterativeConfirmed(int maxfits) {
        _iterativeConfirmedFits = maxfits;
    }

    /**
     * Set to enable debug output
     * 
     * @param debug switch
     */
    public void setDebug(boolean debug) {
        _debug = debug;
        _materialmanager.setDebug(debug);
        _helixfitter.setDebug(debug);
    }

    /**
     * Set to enable the sectoring to use the sector bins in checking for consistent hits.
     *
     * @param applySectorBinning apply sector binning switch
     */
    public void setApplySectorBinning(boolean applySectorBinning) {
        _finder.setApplySectorBinning(applySectorBinning);
        _finder.getConfirmer().setApplySectorBinning(applySectorBinning);
    }

    public void setSubdetectorName(String subdetectorName) {
        ((MaterialSupervisor) this._materialmanager).setSubdetectorName(subdetectorName);
    }

    @Override
    protected void process(EventHeader event) {

        // System.out.println("New event");
        // Pass the event to the diagnostics package
        if (_diag != null)
            _diag.setEvent(event);

        // Initialize timing
        long last_time = System.currentTimeMillis();

        // Get the hit collection from the event
        List<HelicalTrackHit> hitcol = event.get(HelicalTrackHit.class, _inputCol);

        // Sort the hits for this event
        _hitmanager.OrganizeHits(hitcol);

        // Make the timing plots if requested
        long start_time = System.currentTimeMillis();
        double dtime = ((double) (start_time - last_time)) / 1000.;
        last_time = start_time;
        if (_timing)
            aida.cloud1D("Organize Hits").fill(dtime);

        // Make sure that we have cleared the list of track seeds in the finder
        _finder.clearTrackSeedList();

        // Loop over strategies and perform track finding
        for (SeedStrategy strategy : _strategylist) {

            // Set the strategy for the diagnostics
            if (_diag != null)
                _diag.fireStrategyChanged(strategy);

            // Perform track finding under this strategy
            _finder.FindTracks(strategy, _bfield);

            // Make the timing plots if requested
            long time = System.currentTimeMillis();
            dtime = ((double) (time - last_time)) / 1000.;
            last_time = time;
            if (_timing)
                aida.cloud1D("Tracking time for strategy " + strategy.getName()).fill(dtime);
        }

        // Get the list of final list of SeedCandidates
        List<SeedCandidate> trackseeds = _finder.getTrackSeeds();

        if (_iterativeConfirmedFits > 0) {
            // Iteratively re-fit tracks to take into account helix and hit position correlations

            List<SeedCandidate> seedsToRemove = new ArrayList<SeedCandidate>();
            for (SeedCandidate seed : trackseeds) {
                SeedStrategy strategy = seed.getSeedStrategy();
                boolean success = false;
                for (int iterFit = 0; iterFit < _iterativeConfirmedFits; ++iterFit) {
                    success = _helixfitter.FitCandidate(seed, strategy);
                }
                if (!success) {
                    seedsToRemove.add(seed);
                }
            }
            for (SeedCandidate badseed : seedsToRemove) {
                trackseeds.remove(badseed);
            }
        }

        // Make tracks from the final list of track seeds
        _maketracks.Process(event, trackseeds, _bfield);

        // Save the MC Particles that have been seeded / confirmed if diagnostics are enabled
        if (_diag != null) {
            Set<MCParticle> seededmcpset = _finder.getSeededMCParticles();
            List<MCParticle> seededmcp = new ArrayList<MCParticle>(seededmcpset);
            event.put("SeededMCParticles", seededmcp, MCParticle.class, 0);
            Set<MCParticle> confirmedmcpset = _finder.getConfirmedMCParticles();
            List<MCParticle> confirmedmcp = new ArrayList<MCParticle>(confirmedmcpset);
            event.put("ConfirmedMCParticles", confirmedmcp, MCParticle.class, 0);
        }

        // Clear the list of track seeds accumulated in the track finder
        _finder.clearTrackSeedList();

        // Make the total time plot if requested
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
        _rtrk = org.lcsim.recon.tracking.seedtracker.MaterialManager.getRMax();

        //  Set the sectoring parameters
        if (_autosectoring)
            _hitmanager.setSectorParams(_strategylist, _bfield, _rtrk);
    }

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
            //_diag.setHitManager(_hitmanager);
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
     * If this method is never called, no external checking of seeds and tracks is performed.
     */
    public void setTrackCheck(TrackCheck trackCheck) {
        _finder._trackCheck = trackCheck;
        _maketracks.setTrackCheck(trackCheck);
    }

    public void setUseDefaultXPlane(boolean useDefault) {
        _materialmanager.setDefaultXPlaneUsage(useDefault);

    }
}
