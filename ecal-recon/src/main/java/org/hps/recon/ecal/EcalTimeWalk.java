package org.hps.recon.ecal;

public final class EcalTimeWalk {

	// TODO: 
	// Put parameters in the conditions database once functional form is fixed.
	// Also, there is some higher-order dependence on gains that maybe worth considering.
	
	// From a fit of Pass0 Run 3261 (Mode-3 FADC):
	// ([0]+e*[1]+e*e*[2]) * exp(-([3]*e+[4]*e*e+[5]*e*e*e*e))
	final double[] pars = { 
			3.64218e+01, 
		   -4.60756e+02, 
		    9.18743e+03,
			3.73873e+01, 
		   -6.57130e+01, 
		    1.07182e+02 
    };

	private EcalTimeWalk() {
	}

	/**
	 * Perform Time Walk Correction
	 * @param time - FADC Mode-3 Hit time (ns)
	 * @param energy - Pulse energy (GeV)
	 * @return corrected time (ns)
	 */
	public double correctTimeWalk(double time, double energy) {
		final double poly1 = pars[0] + 
				             pars[1] * energy +
				             pars[2] * energy * energy;
		final double poly2 = pars[3] * energy +
				             pars[4] * energy * energy +
				             pars[5] * Math.pow(energy, 4);
		return time - poly1*Math.exp(-poly2);
	}

}
