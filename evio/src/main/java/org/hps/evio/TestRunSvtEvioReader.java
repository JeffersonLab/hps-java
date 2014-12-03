package org.hps.evio;

import java.util.List;

import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.HpsTestRunSiSensor;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.base.BaseRawTrackerHit;
import org.lcsim.geometry.Subdetector;

import org.hps.util.Pair;

import static org.hps.evio.EventConstants.TEST_RUN_SVT_BANK_TAG;

/**
 *	Test run SVT EVIO reader used to convert SVT bank integer data to LCIO
 *	objects.
 * 
 * 	@author Omar Moreno <omoreno1@ucsc.edu>
 * 	@date November 20, 2014
 *
 */
public class TestRunSvtEvioReader extends AbstractSvtEvioReader {

	//-----------------//
	//--- Constants ---//
	//-----------------//

	private static final int DATA_HEADER_LENGTH = 7;
	private static final int DATA_TAIL_LENGTH = 1; 
	private static final int MAX_FPGA_ID = 6;
	
	/** 
	 * Default Constructor
	 */
	public TestRunSvtEvioReader() { }; 
	
	/**
	 *	Get the maximum data bank tag in the event.  For the test run, this
	 *	corresponds to the maximum FPGA ID of 6.  The data contained an 
	 *	additional FPGA bank with tag = 7, but this wasn't used.
	 *
	 *	@return Maximum FPGA ID
	 */
	@Override
	protected int getMaxDataBankTag() {
		return MAX_FPGA_ID;
	}
	
	/**
	 *	Get the SVT bank tag.  For the test run, the bank tag was set to 0x3.
	 *
	 *	@return The SVT bank tag.
	 */
	@Override
	protected int getSvtBankTag() { 
		return TEST_RUN_SVT_BANK_TAG; 
	}
	
	/**
	 *	Get the number of 32 bit integers composing the data block header. For
	 * 	the test run, the header consisted of 7 32 bit integers.
	 *
	 *	@return The header length. 
	 */
	@Override
	protected int getDataHeaderLength() {
		return DATA_HEADER_LENGTH;
	}
	
	/**
	 * 	Get the number of 32 bit integers composing the data block tail.  For
	 * 	the test run, the tail consisted of a single 32 bit integer.
	 * 
	 *	@return The tail length
	 */
	@Override
	protected int getDataTailLength() {
		return DATA_TAIL_LENGTH;
	}

	/**
	 *	A method to setup a mapping between a DAQ pair (FPGA/Hybrid) and the 
	 *	corresponding sensor.
	 *
	 *	@param subdetector - The tracker {@link Subdetector} object
	 */
	// TODO: This can probably be done when the conditions are loaded.
	@Override
	protected void setupDaqMap(Subdetector subdetector) { 
	
        List<HpsTestRunSiSensor> sensors 
        	= subdetector.getDetectorElement().findDescendants(HpsTestRunSiSensor.class);
		for (HpsTestRunSiSensor sensor : sensors) { 
			Pair<Integer, Integer> daqPair 
				= new Pair<Integer, Integer>(sensor.getFpgaID(), sensor.getHybridID());
			daqPairToSensor.put(daqPair, sensor);
		}
		this.isDaqMapSetup = true;
	}

	/**
	 *	Get the sensor associated with a set of samples.  The sample block of
	 *	data is used to extract the FPGA ID and Hybrid ID corresponding to 
	 *	the samples. 
	 *
	 *	@param data - sample block of data
	 *	@return The sensor associated with a set of sample 
	 */
	@Override
	protected HpsSiSensor getSensor(int[] data) {
		//System.out.println("[ " + this.getClass().getSimpleName() + " ]: FPGA ID: " + SvtEvioUtils.getFpgaID(data));
		//System.out.println("[ " + this.getClass().getSimpleName() + " ]: Hybrid ID: " + SvtEvioUtils.getHybridID(data));
		
        Pair<Integer, Integer> daqPair 
        	= new Pair<Integer, Integer>(SvtEvioUtils.getFpgaID(data),
        								 SvtEvioUtils.getHybridID(data));
		return daqPairToSensor.get(daqPair);
	}


	/**
	 * 	Make a {@link RawTrackerHit} from a set of samples.
	 * 
	 *	@param data - sample block of data
	 * 	@return A raw hit
	 */
	@Override
	protected RawTrackerHit makeHit(int[] data) {
		
		HpsSiSensor sensor = this.getSensor(data); 
		int channel = SvtEvioUtils.getChannelNumber(data);
		//System.out.println("[ " + this.getClass().getSimpleName() + " ]: Channel ID: " + SvtEvioUtils.getChannelNumber(data));
		long cellID = sensor.makeChannelID(channel);
		int hitTime = 0;
		//short[] samples = SvtEvioUtils.getSamples(data);
		//for(int sampleN = 0; sampleN < 6; sampleN++) { 
		//	System.out.println("[ " + this.getClass().getSimpleName() + " ]: Sample " + sampleN + ": " + samples[sampleN]);
		//}
		
		return new BaseRawTrackerHit(hitTime, cellID, SvtEvioUtils.getSamples(data), null, sensor);
	}
}
