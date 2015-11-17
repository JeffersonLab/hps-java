package org.hps.users.baltzell;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import hep.aida.IAnalysisFactory;
import hep.aida.IDataPointSet;
import hep.aida.IFitFactory;
import hep.aida.IFitResult;
import hep.aida.IFitter;
import hep.aida.IFunction;
import hep.aida.IFunctionFactory;

import org.hps.recon.ecal.FADCGenericHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/*
 * Extract RF time from waveform and put into lcsim event.
 */
public class RfFitterDriver extends Driver {

	static final double NOISE=2.0; // units = FADC
	static final int CRATE=46;
	static final int SLOT=13;
	static final int CHANNELS[]={0,1};
	static final double NSPERSAMPLE=4;

	// boilerplate:
    AIDA aida = AIDA.defaultInstance();
    IAnalysisFactory analysisFactory = aida.analysisFactory();
    IFunctionFactory functionFactory = analysisFactory.createFunctionFactory(null);
    IFitFactory fitFactory = analysisFactory.createFitFactory();
    IFitter fitter=fitFactory.createFitter();
    IDataPointSet fitData=aida.analysisFactory().createDataPointSetFactory(null).create("RF ADC DataPointSet", 2);
    
    // the function used to fit the RF pulse:
    IFunction fitFunction=new RfFitFunction();

    /*
     * Check the event for an RF pulse, and, if found, fit it to get
     * RF time and then dump it in the lcsim event.
     */
	public void process(EventHeader event) {
		if (!event.hasCollection(GenericObject.class,"FADCGenericHits")) return;
		
		boolean foundRf=false;
    	double times[]={-9999,-9999};
    	
    	for (GenericObject gob : event.get(GenericObject.class,"FADCGenericHits")) {
			FADCGenericHit hit=(FADCGenericHit)gob;
    		
			// ignore hits not from proper RF signals based on crate/slot/channel:
			if (hit.getCrate()!=CRATE || hit.getSlot()!=SLOT) continue;
    		for (int ii=0; ii<CHANNELS.length; ii++) {
				if (hit.getChannel()==CHANNELS[ii]) {
					
					// we found a RF readout, fit it:
					foundRf=true;
					IFitResult fit=fitPulse(hit);
  				    times[ii]=NSPERSAMPLE*fit.fittedParameter("time");
					break;
				}
			}
		}
		
    	// if we found an RF readout, dump the fit result in the event:  
    	if (foundRf) {
    		List <RfHit> rfHits=new ArrayList<RfHit>();
    		rfHits.add(new RfHit(times));
	    	event.put("RFHits", rfHits, RfHit.class, 1);
		}
	}

	/*
	 * Perform the fit to the RF pulse:
	 */
	public IFitResult fitPulse(FADCGenericHit hit) {

		fitData.clear();
		final int adcSamples[]=hit.getData();
		
		// TODO: only add those ADC values which are to be fitted:
		for (int ii=0; ii<adcSamples.length; ii++) {
			final int jj=fitData.size();
			fitData.addPoint();
			fitData.point(jj).coordinate(0).setValue(ii);
			fitData.point(jj).coordinate(1).setValue(adcSamples[ii]);
			fitData.point(jj).coordinate(1).setErrorMinus(NOISE);
			fitData.point(jj).coordinate(1).setErrorPlus(NOISE);
		}
		
		// TODO: properly initialize fit parameters:
		fitFunction.setParameter("time",0.0);
		fitFunction.setParameter("pedestal",0.0);
		fitFunction.setParameter("slope",100.0);
	
		// this used to be turned on somewhere else on every event, dunno if it still is:
		//Logger.getLogger("org.freehep.math.minuit").setLevel(Level.OFF);
		
		return fitter.fit(fitData,fitFunction);
	}
	
}
