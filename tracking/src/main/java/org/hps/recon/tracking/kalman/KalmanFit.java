package kalman;

import java.util.ArrayList;
import java.util.Iterator;

//Driver program for executing a Kalman fit.  At first it takes just the simplest case of starting
// from one end, filtering to the other end, and then smoothing.  No pattern recognition is done (yet).
public class KalmanFit {  

	ArrayList<MeasurementSite> sites;
	int initialSite;
	int finalSite;
	double chi2f, chi2s;   // Filtered and smoothed chi squared values (just summed over the N measurement sites)
	
	public KalmanFit(
			ArrayList<Measurement> data,   // List of data points to be included in the fit
			Vec pivot,                     // Pivot point for the starting "guess" helix (normally the origin)
			Vec helixParams,               // 5 helix parameters for the starting "guess" helix
			SquareMatrix C,                // Full covariance matrix for the starting "guess" helix
			double B,                      // Magnetic field strength at helix beginning
			Vec t,                         // Magnetic field direction at helix beginning; defines the helix coordinate system
			boolean verbose) {
		
		// Create an state vector from the input seed to initialize the Kalman filter
		Vec origin = new Vec(0.,0.,0.);
		StateVector sI = new StateVector(-1, helixParams, C, pivot, B, t, origin, verbose);

		if (verbose) System.out.format(">>>Begin Kalman fit.\n");

		Iterator<Measurement> itr;
		//System.out.format("Extrapolate this initial helix to all measurement sites for a quick sanity test\n");
		//itr = data.iterator();
		/*
		while (itr.hasNext()) {
			Measurement m = itr.next();
			MeasurementSite newSite = new MeasurementSite (-1, -1, m, B);
			if (newSite.makePrediction(sI)) {
				newSite.print();
			}
		}
		*/
		
		// If the starting guess for the helix isn't great, then two iterations are needed in order to get a good 
		// covariance matrix for the smoothed helix.
		for (int iteration=0; iteration<2; iteration++) {
			if (verbose) {
				System.out.format("KalmanFit: iteration %d; initial guess for the state vector:\n", iteration);
				sI.print("initial state");
			}
			
			sites = new ArrayList<MeasurementSite>();
			
			initialSite = 0;
			chi2f = 0.;
			int siteIndex = -1;
			int prevSite = -1;
			itr = data.iterator();
			while (itr.hasNext()) {     // Filter step through all measurement sites
				Measurement m = itr.next();
				siteIndex++;
	
				MeasurementSite newSite = new MeasurementSite (siteIndex, prevSite, m);
				if (siteIndex == 0) {
					if (!newSite.makePrediction(sI)) {
						System.out.format("Failed to make initial prediction at site %d.  Abort\n", siteIndex);
						break;
					}
				} else {
					if (!newSite.makePrediction(sites.get(prevSite).aF)) {
						System.out.format("Failed to make prediction at site %d.  Abort\n", siteIndex);
						break;
					}
				}
				
				if (!newSite.filter()) {
					System.out.format("Failed to filter at site %d.  Ignore remaining sites\n",  siteIndex);
					break;
				};
				
				if (verbose) newSite.print();
				chi2f += newSite.chi2inc;
				
				sites.add(newSite);			
				
				prevSite = siteIndex;
			}
			Vec afF = sites.get(prevSite).aF.a.copy();
			Vec afC = sites.get(prevSite).aF.helixErrors();
			if (verbose) System.out.format("Fit chi^2 after filtering = %12.4e\n", chi2f);
			finalSite = sites.size() - 1;
			
			// Now go back through all the sites in the reverse order to do the smoothing
			chi2s = 0.;
			MeasurementSite nextSite = null;
			for (int site = sites.size()-1; site>=0; site--) {
				MeasurementSite thisSite= sites.get(site);
				if (nextSite == null) {
					thisSite.aS = thisSite.aF.copy();
					thisSite.smoothed = true;
				} else {
					thisSite.smooth(nextSite);
				}
				chi2s += thisSite.chi2inc;
				if (site == 0) {
					sI = thisSite.aS.copy();   // Initial state vector for the next iteration.
					sI.C.scale(10000.);        // Blow up the covariance matrix so that measurements don't double count.
				}
				if (verbose) thisSite.print();
				nextSite = thisSite;
			}
			if (verbose) {
				System.out.format("Fit chi^2 after smoothing = %12.4e\n", chi2s);
				afF.print("KalmanFit helix parameters at final filtered site");
				afC.print("KalmanFit helix parameter errors");
				if (nextSite != null) {
					nextSite.aS.a.print("KalmanFit helix parameters at the final smoothed site");
					nextSite.aS.helixErrors().print("KalmanFit helix parameter errors:");
				}
			}
		}
		
	}
	
	public StateVector fittedStateBegin() {
		return sites.get(initialSite).aS;
	}
	
	public StateVector fittedStateEnd() {
		return sites.get(finalSite).aS;
	}
	
}
