package org.hps.recon.filtering;

import org.hps.recon.ecal.FADCGenericHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class CosmicPMTFilter extends Driver {
	
	// class storing raw PMT readout:
	static final String CLASSNAME="FADCGenericHits";

	// hardware location of PMTs:
	static final int CRATE=2; // this is hps2, crate #37 in EVIO
	static final int SLOT=20;
	static final int CHANNELS[]={13,14};
	
	// cuts on pulse integrals for cosmic signals (units ADC):
	static final float CUTS[]={2000,4000};

	// number of samples to use at beginning of window for pedestal:
	static final int NPEDSAMP=20;

	AIDA aida = AIDA.defaultInstance();
    
	public void detectorChanged(Detector detector) {
    	aida.tree().cd("/");
    	aida.histogram2D("CosmicPMTs",400,0,10000,400,0,10000);
    }

	public void process(EventHeader event) {
	
		// find PMT data:
		FADCGenericHit pmt1=null,pmt2=null;
		if (!event.hasCollection(GenericObject.class,CLASSNAME))
		      throw new Driver.NextEventException();
		for (GenericObject gob : event.get(GenericObject.class,CLASSNAME)) {
			FADCGenericHit hit=(FADCGenericHit)gob;
			if (hit.getCrate()==CRATE && hit.getSlot()==SLOT) {
				if      (hit.getChannel()==CHANNELS[0]) pmt1=hit;
				else if (hit.getChannel()==CHANNELS[1]) pmt2=hit;
			}
		}
		if (pmt1==null || pmt2==null) throw new Driver.NextEventException();
	
		// calculate and histogram pulse integrals:
		float pulse1=getPulseIntegral(pmt1);
		float pulse2=getPulseIntegral(pmt2);
        aida.histogram2D("CosmicPMTs").fill(pulse1,pulse2);
		
        // cut on pulse integrals:
		if (pulse1<CUTS[0] || pulse2<CUTS[1]) throw new Driver.NextEventException();
	}
	
	public float getPulseIntegral(FADCGenericHit hh) {
		float sum=0;
		for (int samp : hh.getData()) sum+=samp;
		return sum-getPedestal(hh)*hh.getData().length;
	}
	
	public float getPedestal(FADCGenericHit hh) {
		if (hh.getData().length<NPEDSAMP)
			throw new java.lang.RuntimeException("Not enough samples for pedestal.");
		float sum=0;
		for (int isamp=0; isamp<NPEDSAMP; isamp++) sum+=hh.getData()[isamp];
		return sum/NPEDSAMP;
	}
}
