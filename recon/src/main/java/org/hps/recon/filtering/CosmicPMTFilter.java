package org.hps.recon.filtering;

import org.hps.recon.ecal.FADCGenericHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.util.Driver;

public class CosmicPMTFilter extends Driver {
	
	// class storing raw PMT readout:
	static final String CLASSNAME="FADCGenericHits";

	// hardware location of PMTs:
	static final int CRATE=39;
	static final int SLOT=20;
	static final int CHANNELS[]={13,14};
	
	// cuts on pulse integrals for cosmic signals (units ADC):
	static final float CUTS[]={1534,3242};

	// number of samples to use at beginning of window for pedestal:
	static final int NPEDSAMP=20;
	
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
		
		// put cuts on pulse integrals:
		float pulse1=getPulseIntegral(pmt1);
		float pulse2=getPulseIntegral(pmt2);
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
