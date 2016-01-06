package org.hps.users.phansson.gbl;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import java.util.List;

import org.hps.recon.tracking.MaterialSupervisor;
import org.hps.recon.tracking.MultipleScattering;
import org.hps.recon.tracking.gbl.MilleBinary;
import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;


/**
 * Run GBL track fit on existing seed tracks in the event.
 * 
 * @author phansson
 *
 */
public class GBLDriver extends Driver {

	private boolean _debug = false;
	private boolean _isMC = false;
	MaterialSupervisor _materialManager = null;
	MultipleScattering _scattering = null;
	private HpsGblFitter _gbl_fitter = null; 
	private String milleBinaryName = "";
	
	public GBLDriver() {
	}
	
	public void setDebug(boolean debug) {
		_debug = debug;
	}
	
	public void setIsMC(boolean mcflag) {
		_isMC = mcflag;
	}
	
	protected void detectorChanged(Detector det) {
	    System.out.printf("%s: detectorChanged\n",getClass().getSimpleName());
        Hep3Vector bfieldvec = det.getFieldMap().getField(new BasicHep3Vector(0., 1., 0.));
        double bfield = bfieldvec.y();
        System.out.printf("%s: b-field %s\n",getClass().getSimpleName(),bfieldvec.toString());
		 _materialManager = new MaterialSupervisor();
		 _scattering = new MultipleScattering(_materialManager);
		 _materialManager.buildModel(det);
		 _scattering.setBField(Math.abs(bfield)); // only absolute of B is needed as it's used for momentum calculation only
		 _gbl_fitter = new HpsGblFitter(bfield, _scattering, _isMC);
		 if(!milleBinaryName.equalsIgnoreCase("")) {
			 _gbl_fitter.setMilleBinary(new MilleBinary());
		 }
		 System.out.printf("%s: detectorChanged end\n",getClass().getSimpleName());
	}

	protected void process(EventHeader event) {

		List<Track> seedTracks = null;
        if(event.hasCollection(Track.class,"MatchedTracks")) {        
            seedTracks = event.get(Track.class, "MatchedTracks");
             if(_debug) {
                System.out.printf("%s: Event %d has %d tracks\n", this.getClass().getSimpleName(),event.getEventNumber(),seedTracks.size());
             }
        } else {
        	 if(_debug) {
                 System.out.printf("%s: No tracks in Event %d \n", this.getClass().getSimpleName(),event.getEventNumber());
              }
        	 return;
        }
	    
        
        for(int itrack = 0; itrack < seedTracks.size(); ++itrack) {

        	if(_debug) {
        		System.out.printf("%s: do the fit for track  %d \n", this.getClass().getSimpleName(),itrack);
        	}

        	// Reset
        	_gbl_fitter.clear();
        	
        	// Run the GBL fit on this track
        	int status = _gbl_fitter.Fit(seedTracks.get(itrack));
        
        	if(_debug) {
        		System.out.printf("%s: fit status %d \n", this.getClass().getSimpleName(),status);
        	}
        
        }
        
        
        
	}

	protected void endOfData() {
	}


	
}
