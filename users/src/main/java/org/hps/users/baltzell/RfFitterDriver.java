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
                    if (ii==1){
                        
                        System.out.println(times[1]-times[0]);
                    }
                                        
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
        //stores the number of peaks
        int iz=0;
        int peakBin[]={-999,-999};
        final int threshold = 300;  
        double fitThresh[]={-999,-999};
        double pedVal[]={-999,-999};
        
        // Look for bins containing the peaks (2-3 peaks)
        for (int ii=4; ii<adcSamples.length; ii++) {
            // After 2 peaks, stop looking for more
            if (iz==2){break;}
            if ((adcSamples[ii+1]>0) && (adcSamples[ii-1]>0) && (adcSamples[ii]>threshold) && ii>8){
                if ((adcSamples[ii]>adcSamples[ii+1]) && (adcSamples[ii]>=adcSamples[ii-1]) ){
                    
                    peakBin[iz]=ii;
                    iz++;
                }
            }
        }
        
        
        int jj=0;
        // Choose peak closest to center of window (second peak, ik=1)
        final int ik=1;
        pedVal[ik] = (adcSamples[peakBin[ik]-6]+adcSamples[peakBin[ik]-7]+adcSamples[peakBin[ik]-8]+adcSamples[peakBin[ik]-9])/4.0;
        fitThresh[ik]= (adcSamples[peakBin[ik]]+pedVal[ik])/3.0;
    
        // Initial values: we find/fit 3 points:
        double itime[] = {-999,-999,-999};
        double ifadc[] = {-999,-999,-999};
        
        // Find the points of the peak bin to peak bin-5 
        for (int ll=0; ll<5; ll++){ 
            if ((adcSamples[peakBin[ik]-5+ll]) > fitThresh[ik]){
                // One point is below fit threshold and two points are above    
                if(jj==0 && (adcSamples[peakBin[ik]-6+ll] > pedVal[ik])){
                    final int zz=fitData.size();    
                    fitData.addPoint();
                    itime[zz] = peakBin[ik]-6+ll;
                    ifadc[zz] = adcSamples[peakBin[ik]-6+ll];
                    fitData.point(zz).coordinate(0).setValue(peakBin[ik]-6+ll);
                    fitData.point(zz).coordinate(1).setValue(adcSamples[peakBin[ik]-6+ll]);
                    fitData.point(zz).coordinate(1).setErrorMinus(NOISE);
                    fitData.point(zz).coordinate(1).setErrorPlus(NOISE);        
                    jj++;   
                }
                final int zz=fitData.size();    
                fitData.addPoint();
                itime[zz] = peakBin[ik]-5+ll;
                ifadc[zz] = adcSamples[peakBin[ik]-5+ll];
                fitData.point(zz).coordinate(0).setValue(peakBin[ik]-5+ll);
                fitData.point(zz).coordinate(1).setValue(adcSamples[peakBin[ik]-5+ll]);
                fitData.point(zz).coordinate(1).setErrorMinus(NOISE);
                fitData.point(zz).coordinate(1).setErrorPlus(NOISE);
                    
                jj++;
                if (jj==3) {break;}                 
            }
        }
        
        double islope = ((double)(ifadc[2]-ifadc[0]))/(itime[2]-itime[0]);
        double icept = ifadc[1] - islope*itime[1];
        // Initialize fit parameters:
        fitFunction.setParameter("intercept",icept);
        fitFunction.setParameter("slope",islope);

        // this used to be turned on somewhere else on every event, dunno if it still is:
        //Logger.getLogger("org.freehep.math.minuit").setLevel(Level.OFF);
    
        IFitResult fitResults = fitter.fit(fitData,fitFunction);
        
        // Read the time value at this location on the fit:
        double halfVal = (adcSamples[peakBin[1]]+pedVal[1])/2.0;    
    
        return NSPERSAMPLE*(halfVal-fitResults.fittedParameter("intercept"))/fitResults.fittedParameter("slope");
            
    }
        
}
