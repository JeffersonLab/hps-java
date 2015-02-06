package org.hps.users.celentan;


import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.lcsim.geometry.Detector;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.base.BaseRawCalorimeterHit;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;
import hep.aida.IPlotterFactory;
import hep.aida.IHistogramFactory;
import hep.aida.ICloud1D;
import hep.aida.ICloud2D;


import org.hps.recon.ecal.ECalUtils;
import org.hps.monitoring.ecal.plots.EcalMonitoringUtilities;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.database.TableConstants;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.conditions.ecal.EcalLed;
import org.hps.conditions.ecal.EcalLed.EcalLedCollection;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;

public class LedAnalysis extends Driver{
	
	private static final int NUM_CHANNELS = 11 * 47;
	
	String inputCollectionRaw = "EcalReadoutHits";
	String inputCollection = "EcalCalHits";	
	AIDA aida;
		
	DatabaseConditionsManager conditionsManager;
	
	EcalChannelCollection ChannelCollection;	
    EcalLedCollection LedCollection;
    Map < Integer,Integer > LedTopMap;
    Map < Integer,Integer > LedBotMap;
    
    private int id,row,column,chid,ledid,driverid;
    private int eventN = 0;
	private int nDrivers = 8;
	private int nSteps = 56;
	private  int[][] LEDStep = new int[][]{
			//first 4 are the flasher1 sequence, TOP controller 
			{2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,434,44,45,46,47,48,49,50,51,52,53,54,55,56,-1},
			{57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,90,91,92,93,94,95,96,97,98,99,100,101,102,103,104,105,106,107,108,109,110,111,-1},
			{112,113,114,115,116,117,118,119,120,121,122,123,124,125,126,127,128,129,130,131,132,133,134,136,137,138,130,140,141,142,143,144,145,146,147,148,149,150,151,152,153,154,155,156,157,158,159,160,161,162,163,164,165,166,167,168}, //missing 135 is ok
			{169,170,171,172,173,174,175,176,177,178,179,180,181,182,183,184,185,186,187,188,189,190,191,192,193,194,195,196,197,198,199,200,201,202,203,204,205,206,207,208,209,210,211,212,213,214,215,216,217,218,219,220,221,222,223,-1},
			//second 4 are the flasher2 sequence, BOTTOM controller 
			{2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,434,44,45,46,47,48,49,50,51,52,53,54,55,56,-1},	
			{57,58,59,60,61,62,63,64,65,66,67,68,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,90,91,92,93,94,95,96,97,98,99,100,101,102,103,104,105,106,107,108,109,110,111,112,113}, //missing 69 is OK
			{114,115,116,117,118,119,120,121,122,123,124,125,126,127,128,129,130,131,132,133,134,135,136,137,138,130,140,141,142,143,144,145,146,147,148,149,150,151,152,153,154,155,156,157,158,159,160,161,162,163,164,165,166,167,168,-1},	
			{169,170,171,172,173,174,175,176,177,178,179,180,181,182,183,184,185,186,187,188,189,190,191,192,193,194,195,196,197,198,199,200,201,202,203,204,205,206,207,208,209,210,211,212,213,214,215,216,217,218,219,220,221,222,223,-1},
			};
			
	private int[] iStep = new int[nDrivers];
	private int[] nEvents = new int[NUM_CHANNELS];
	//Histograms
	private ArrayList<IHistogram1D> hRaw;
	private ArrayList<ICloud1D> hStrip;
	
	@Override
	protected void detectorChanged(Detector detector) {
		System.out.println("LedAnalysis::Detector changed was called");

		// Setup conditions
		
		conditionsManager = DatabaseConditionsManager.getInstance();

	    LedTopMap = new HashMap< Integer , Integer >(); //key: ecal channel ID. Value: 
	    LedBotMap = new HashMap< Integer , Integer >();	
		
        ChannelCollection = conditionsManager.getCachedConditions(EcalChannel.EcalChannelCollection.class, "ecal_channels").getCachedData();	
		LedCollection = conditionsManager.getConditionsData(EcalLedCollection.class, TableConstants.ECAL_LEDS);
		
		for (EcalChannel channel : ChannelCollection){
			chid = channel.getChannelId();
			for (EcalLed Led : LedCollection) {    	
				if (Led.getEcalChannelId()==chid){
					if (channel.getY()>0){
						LedTopMap.put( chid , Led.getLedNumber() );
					}
					else if (channel.getY()<0){
						LedBotMap.put( chid , Led.getLedNumber() );
					}
				}
			}
		}
		
		
		// Setup plots
		aida = AIDA.defaultInstance();
		aida.tree().cd("/");
		
	  //  IPlotterFactory factory= aida.analysisFactory().createPlotterFactory("ECAL DAQ Plots");
		
		hRaw = new ArrayList<IHistogram1D>(NUM_CHANNELS);
		hStrip = new ArrayList<ICloud1D>(NUM_CHANNELS);
		
		
		
		for (int ii=0;ii<NUM_CHANNELS;ii++){
			int row = EcalMonitoringUtilities.getRowFromHistoID(ii);
			int column = EcalMonitoringUtilities.getColumnFromHistoID(ii);
			
			// Initialize the histograms for the current crystal channel.
			hRaw.add(aida.histogram1D("h1_"+ii, 1000, -1,30));
			hStrip.add(aida.cloud1D("strip_"+ii,100000));
		}
		
	}		
	     
	@Override
	public void process(EventHeader event) {
		eventN++;
		if (event.hasCollection(CalorimeterHit.class, inputCollection)) {
			//List<BaseRawCalorimeterHit> hits = event.get(BaseRawCalorimeterHit.class, inputCollectionRaw);
			List<CalorimeterHit> hits = event.get(CalorimeterHit.class, inputCollection);
			for (CalorimeterHit hit : hits) {
			
				column = hit.getIdentifierFieldValue("ix");
				row = hit.getIdentifierFieldValue("iy");
				id = EcalMonitoringUtilities.getHistoIDFromRowColumn(row, column);
							
				//find the LED
				chid = ChannelCollection.findGeometric(hit.getCellID()).getChannelId();
				if (row>0){
					ledid=LedTopMap.get(chid);
				}
				else if (row<0){
					ledid=LedBotMap.get(chid);
				}
				driverid=getDriver(ledid);
				if (row<0) driverid+=4;
				
				/*First, check if this led is the one in the NEXT step. Therefore, increment by 1 the step*/
				if (iStep[driverid]<(nSteps-1)){
					if (ledid==LEDStep[driverid][iStep[driverid]+1]){
						iStep[driverid]++;
						System.out.println("LedAnalysis:: increment step "+driverid+" "+ledid+" "+column+" "+row+" "+id);
					}	
				}
				/*Case 1: this led is the one in the corresponding step*/;
				if (ledid==LEDStep[driverid][iStep[driverid]]){
					hRaw.get(id).fill(hit.getCorrectedEnergy());
					hStrip.get(id).fill(nEvents[id],hit.getCorrectedEnergy());
					nEvents[id]++;
				}
				else{	/*Case 2: this led is not one in the corresponding step (but maybe is the neighborhood??Ctalk??)*/;
					
				}
				
		   }		
	   }		
	}
	@Override
	public void endOfData() {
		System.out.println("LedAnalysis::end of data");
	}	
	
	/**
	 * This function returns the driver number (from 0 to 3) given the LED id.
	 * @param led
	 * @return
	 */
	public int getDriver(int led){
		int ret=-1;	
		if ((led>=2)&&(led<56)) ret=0;
		else if ((led>=56)&&(led<112)) ret=1;
		else if ((led>=112)&&(led<168)) ret=2;
		else if ((led>=168)&&(led<224)) ret=3;
		return ret;
	}
}	