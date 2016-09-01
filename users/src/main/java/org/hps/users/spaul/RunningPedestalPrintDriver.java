package org.hps.users.spaul;

import org.hps.recon.ecal.EcalRawConverter;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class RunningPedestalPrintDriver extends Driver{
	private static int NCHANNELS = 422;
	AIDA aida = AIDA.defaultInstance();
	private int skipEvents = 10000;
	private int prescale = 1000;
	public void setPrescale(int prescale){
		this.prescale = prescale;
	}
	public void setSkipEvents(int skipEvents){
		this.skipEvents = skipEvents;
	}
	
	@Override
	public void process(EventHeader event){
		int eventNum = event.getEventNumber();
		if(eventNum < skipEvents)
			return;
		if(eventNum % prescale != 0)
			return;
		long time = event.getTimeStamp();
		EcalRawConverter erc = new EcalRawConverter();
		double[] pedestals = new double[NCHANNELS];
		for(int cellID = 1; cellID<= NCHANNELS; cellID++){
			pedestals[cellID-1] = erc.getSingleSamplePedestal(event, cellID);
		}
		String line = time + "\t" + eventNum;
		for(int i = 0; i< NCHANNELS; i++)
			line += "\t" + prescale;
		System.out.println(line);
	}
}
