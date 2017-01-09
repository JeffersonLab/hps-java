package org.hps.plugin;

import org.freehep.application.studio.Plugin;
import org.freehep.application.studio.Studio;
import org.freehep.util.FreeHEPLookup;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.util.heprep.HepRepCollectionConverter;
import org.lcsim.util.heprep.LCSimHepRepConverter;

/**
 * JAS3 plugin for HPS.
 * This install custom converter for the Track and ReconstructedParticle classes. 
 * 
 * @author Jeremy McCormick
 * @version $Id: HPSPlugin.java,v 1.1 2013/06/03 16:23:47 jeremy Exp $
 */

public class HPSPlugin extends Plugin {
    
    /**
     * Install a custom converter for HPS Track and ReconstructedParticle objects
     * so that they are swum correctly in the B-field. 
     */
    protected void postInit() {
        
        Studio app = getApplication();
        FreeHEPLookup lookup = app.getLookup();
         
        // Get the HepRep converter registered by lcsim.
        Class<?> converterClass = null;
        try {
            converterClass = Class.forName("org.lcsim.util.heprep.LCSimHepRepConverter");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }                       
        LCSimHepRepConverter converter = (LCSimHepRepConverter) lookup.lookup(converterClass);
        if (converter == null) {
            throw new RuntimeException("LCSimHepRepConverter was not found.");
        }
        
        // Remove the existing track converter from the lcsim plugin.
        HepRepCollectionConverter trackConverter = converter.findConverter(Track.class);
        if (trackConverter != null) {
            converter.deregister(trackConverter);            
        } else {
            throw new RuntimeException("The TrackConverter was not found.");
        }
        
        // Register the HPS track converter in the heprep converter.
        converter.register(new HPSTrackConverter());
        
        // Remove the existing particle converter.
        HepRepCollectionConverter particleConverter = converter.findConverter(ReconstructedParticle.class);
        if (particleConverter != null) {
            converter.deregister(particleConverter);
        } else {
            throw new RuntimeException("The ReconstructedParticleConverter was not found.");
        }
        
        // Register the HPS particle converter.
        converter.register(new HPSParticleConverter());
    }
    
}
