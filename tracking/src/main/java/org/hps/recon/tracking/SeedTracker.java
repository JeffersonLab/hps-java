/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.recon.tracking;

import java.util.List;

import org.lcsim.recon.tracking.seedtracker.SeedStrategy;
import org.lcsim.recon.tracking.seedtracker.SeedTrackFinder;

/**
 * Class extending lcsim version to allow extra flexibility
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 */
public class SeedTracker extends org.lcsim.recon.tracking.seedtracker.SeedTracker  {

    public SeedTracker(List<SeedStrategy> strategylist) {
        // use base class only if this constructor is called!
        super(strategylist);
    }

    public SeedTracker(List<SeedStrategy> strategylist,boolean includeMS) {
        super(strategylist);
        initialize(strategylist, true, includeMS);
    }

    public SeedTracker(List<SeedStrategy> strategylist,boolean useHPSMaterialManager, boolean includeMS) {
        super(strategylist);
        initialize(strategylist, useHPSMaterialManager, includeMS);
    }
    
    private void initialize(List<SeedStrategy> strategylist,boolean useHPSMaterialManager, boolean includeMS) {
            
            // Explicitly only replace the objects that might change to avoid getting the lcsim versions
            
            //  Instantiate the material manager for HPS,  the helix fitter and seed track finder as tey depends on the material manager
            if(useHPSMaterialManager) {
                MaterialSupervisor materialSupervisor = new MaterialSupervisor(includeMS);
                _materialmanager = materialSupervisor;
                _helixfitter = new HelixFitter(materialSupervisor);
            } else {
                MaterialManager materialmanager = new MaterialManager(includeMS);
                _materialmanager = materialmanager; //mess around with types here...
                _helixfitter = new HelixFitter(materialmanager);
            }
            //  Instantiate the helix finder since it depends on the material manager
            _finder = new SeedTrackFinder(_hitmanager, _helixfitter);
    } 

     /**
     * Set to enable debug output
     * 
     * @param debug switch
     */
    @Override
    public void setDebug(boolean debug) {
        super.setDebug(debug);
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
}
