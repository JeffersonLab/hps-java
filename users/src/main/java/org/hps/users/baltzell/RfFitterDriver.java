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
					times[ii] = fitPulse(hit);
					//System.out.println("rf times:\t"+times[ii]);
  				    
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
	public double fitPulse(FADCGenericHit hit) {
		fitData.clear();
		final int adcSamples[]=hit.getData();
		//stores the location of the peak bins
		int iz=0;
		int peakBin[]={-999,-999,-999};
		final int threshold = 300;	
		double fitThresh[]={-999,-999,-999};
		double pedVal[]={-999,-999,-999};
		for (int ii=4; ii<(adcSamples.length-1); ii++) {
			//looks for peak bins in time spectra (not more than 3)
			//System.out.println("Samp:\t"+ii+"\t"+adcSamples[ii]);
			if (iz==3){break;}
			if (adcSamples[ii+1]>0 && adcSamples[ii-1]>0 && adcSamples[ii]>threshold && ii>12){
				if ((adcSamples[ii]>adcSamples[ii+1] && adcSamples[ii]>adcSamples[ii-1])
						||((adcSamples[ii]>adcSamples[ii+1] && adcSamples[ii]==adcSamples[ii-1])
								||(adcSamples[ii]==adcSamples[ii+1] && adcSamples[ii]>adcSamples[ii-1]))){
					//System.out.println("peak:\t"+iz);
					peakBin[iz]=ii;
					iz++;
				}
			}
		}
		
		int jj=0;
		//each signal will always have 2-3 pulses in the window. ik=1 selects the second pulse (closest to middle of window)
		int ik=1;
		pedVal[ik] = (adcSamples[peakBin[ik]-6]+adcSamples[peakBin[ik]-7]+adcSamples[peakBin[ik]-8]+adcSamples[peakBin[ik]-9])/4.0;
		fitThresh[ik]= (adcSamples[peakBin[ik]]-pedVal[ik])/3.0;
		
		//calc initial values along the way:
		double itime = -999;
		double islope = -999; 
			
		//find the points of the peak bin to peak bin-5
		for (int ll=0; ll<5; ll++){	
			if ((adcSamples[peakBin[ik]-5+ll]) > fitThresh[ik]){
				//get one below fit threshold and two points above	
				if(jj==0 && (adcSamples[peakBin[ik]-6+ll] > pedVal[ik])){
					final int zz=fitData.size();	
					fitData.addPoint();
					//System.out.println("fit points:\t"+zz+"\t"+(peakBin[ik]-6+ll));
					fitData.point(zz).coordinate(0).setValue(peakBin[ik]-6+ll);
					fitData.point(zz).coordinate(1).setValue(adcSamples[peakBin[ik]-6+ll]);
					fitData.point(zz).coordinate(1).setErrorMinus(0.0);
					fitData.point(zz).coordinate(1).setErrorPlus(0.0);		
					jj++;	
				}
				final int zz=fitData.size();	
				fitData.addPoint();
				//System.out.println("fit points:\t"+zz+"\t"+(peakBin[ik]-5+ll));
				if (zz==1){
					itime = peakBin[ik]-5+ll;
					islope =((double) (adcSamples[peakBin[ik]-5+ll]-adcSamples[peakBin[ik]-6+ll]))/(peakBin[ik]-5+ll-(peakBin[ik]-6+ll));	
				}
				fitData.point(zz).coordinate(0).setValue(peakBin[ik]-5+ll);
				fitData.point(zz).coordinate(1).setValue(adcSamples[peakBin[ik]-5+ll]);
				fitData.point(zz).coordinate(1).setErrorMinus(0.0);
				fitData.point(zz).coordinate(1).setErrorPlus(0.0);
						
				jj++;
				if (jj==3) {break;}					
			}
		}
			
		double icept = itime*(1-islope);
		//System.out.println("initial parameters, icept:\t"+icept+"\t islope:\t"+islope+"\t itime:\t"+itime);
		// properly initialize fit parameters:
		fitFunction.setParameter("time",itime);
		fitFunction.setParameter("intercept",icept);
		fitFunction.setParameter("slope",islope);
	
		// this used to be turned on somewhere else on every event, dunno if it still is:
		//Logger.getLogger("org.freehep.math.minuit").setLevel(Level.OFF);
		
		IFitResult fitResults = fitter.fit(fitData,fitFunction);
		
		//choose to get the time value at this location on the fit:
		double halfVal = (adcSamples[peakBin[1]]+pedVal[1])/2.0;	
		System.out.println("Fit results:\t"+fitResults.fittedParameter("intercept")+"\t"+fitResults.fittedParameter("slope"));
		System.out.println("Half height:\t"+halfVal);
		return NSPERSAMPLE*(halfVal-fitResults.fittedParameter("intercept"))/fitResults.fittedParameter("slope");
		
		
	}
	
	
	
	
}
