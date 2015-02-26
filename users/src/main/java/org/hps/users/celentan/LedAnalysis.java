package org.hps.users.celentan;


import hep.aida.ICloud1D;
import hep.aida.IHistogram1D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalLed;
import org.hps.conditions.ecal.EcalLed.EcalLedCollection;
import org.hps.monitoring.ecal.plots.EcalMonitoringUtilities;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/* This is the driver used to determine the response of each calorimeter channel after a LED run
 * @author Andrea Celentano  <andrea.celentano@ge.infn.it>
 */
public class LedAnalysis extends Driver{
	
	private static final int NUM_CHANNELS = 11 * 47;
	
	String inputCollectionRaw = "EcalReadoutHits";
	String inputCollection = "EcalCalHits";	
	AIDA aida;
		
	DatabaseConditionsManager conditionsManager;
	
	private EcalChannelCollection ChannelCollection;	
    private EcalLedCollection LedCollection;
    private EcalConditions ecalConditions;
    
    Map < Integer,Integer > LedTopMap;
    Map < Integer,Integer > LedBotMap;
    
    private boolean useRawEnergy=false;
    
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
	
	private double energy,rawEnergy;
	private long cellID;
	//Histograms
	private ArrayList<ITuple> iTuple;
	private ArrayList<IProfile1D> cProfile;
	private ArrayList<IFunction> fFunction;
	
	private ArrayList<IHistogram1D> hRaw;
	private ArrayList<IHistogram1D> hStrip;

    public void setUseRawEnergy(boolean useRawEnergy) {
        this.useRawEnergy=useRawEnergy;
    }
	
	
	
	@Override
	protected void detectorChanged(Detector detector) {
		System.out.println("LedAnalysis::Detector changed was called");

		for (int ii=0;ii<nDrivers;ii++){
			iStep[ii]=-1;
		}
		
		// Setup conditions
		
		conditionsManager = DatabaseConditionsManager.getInstance();

	    LedTopMap = new HashMap< Integer , Integer >(); //key: ecal channel ID. Value: 
	    LedBotMap = new HashMap< Integer , Integer >();	
		
        ChannelCollection = conditionsManager.getCollection(EcalChannelCollection.class);	
		LedCollection = conditionsManager.getCollection(EcalLedCollection.class);
		ecalConditions = conditionsManager.getCachedConditions(EcalConditions.class, TableConstants.ECAL_CONDITIONS).getCachedData();
		
		
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
		iTuple = new ArrayList<ITuple>(NUM_CHANNELS);
		cProfile= new ArrayList<IProfile1D>(NUM_CHANNELS);
		fFunction= new ArrayList<IFunction>(NUM_CHANNELS);
		
		hRaw = new ArrayList<IHistogram1D>(NUM_CHANNELS);
		hStrip = new ArrayList<IHistogram1D>(NUM_CHANNELS);
		
		
		
		for (int ii=0;ii<NUM_CHANNELS;ii++){
			int row = EcalMonitoringUtilities.getRowFromHistoID(ii);
			int column = EcalMonitoringUtilities.getColumnFromHistoID(ii);	    
	    	iTuple.add(aida.analysisFactory().createTupleFactory(aida.tree()).create("nTuple"+ii,"nTuple"+ii,"int fEvn=0 , double fCharge=0.",""));
	    	
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
				cellID=hit.getCellID();
				energy = hit.getCorrectedEnergy();
				
				if (useRawEnergy){
					rawEnergy = getRawADCSum(energy,cellID);
				}
				else {
					rawEnergy = energy;
				}
				
				
				//find the LED
				chid = ChannelCollection.findGeometric(cellID).getChannelId();
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
						System.out.println("LedAnalysis:: increment step for driver "+driverid+" "+ledid+" "+column+" "+row+" "+id);
					}	
				}
			
				if (iStep[driverid]==-1) continue;
				
				/*Case 1: this led is the one in the corresponding step*/;
				if (ledid==LEDStep[driverid][iStep[driverid]]){
					//hRaw.get(id).fill(rawEnergy);
					//cStrip.get(id).fill(nEvents[id],rawEnergy);
					
					iTuple.get(id).fill(0,nEvents[id]);
					iTuple.get(id).fill(1,rawEnergy);
					iTuple.get(id).addRow();
					
				
					nEvents[id]++;
				}
				else{	/*Case 2: this led is not one in the corresponding step (but maybe is the neighborhood??Ctalk??)*/;
					
				}
				
		   }		
	   }		
	}
	
	/*
	 * The endOfData() method analises each ntuple to find the LED response.
	 * We cannot simply fit a gaussian to the energy distribution, since there is a high-energy tail due to the LED being turned on:
	 * When the LED turns on, it is "cold", and emits more light. Immediately, it starts to heat, and due to temperature effects the
	 * emitted light is less. This is clearly visible if one plots the charge VS the event number: the trend is decreasing, toward a
	 * plateau, that corresponds to the value at thermal equilibrium.
	 * 
	 * For (few) channels, the first charge values are close to 0, then charge grows rapidly, then it returns back to the plateau.
	 * To handle these, I always cut the first 10% events
	 * To properly handle this:
	 * 
	 * 1) First create a profile histogram, charge VS event number.
	 * 2) Fit it with something like "A*exp(-event_number/N0)+C. The function does not need to be extra-accurate at this stage
	 * 3) Cut the events with event_number < 5*N0.
	 * 4) Fit the remaining events with a gaussian.
	 */
	@Override
	public void endOfData() {
		System.out.println("LedAnalysis::end of data");
		
		
		double e,eMin,eMax;
		int n,nBins;
		
		double skip=0.1;
		
		IFunctionFactory fFactory=aida.analysisFactory().createFunctionFactory(aida.tree());
		IPlotter pPlotter= aida.analysisFactory().createPlotterFactory().create();
	    IFitResult fResult;
		IFitter	   fFitter;
	    
		for (int id = 0; id < 11 * 47; id++) {
	           
			  eMin=9999;
			  eMax=-9999;
		      row = EcalMonitoringUtilities.getRowFromHistoID(id);
              column = EcalMonitoringUtilities.getColumnFromHistoID(id);
              
			  /*Create the profile. Create it for all the channels, to keep sync.*/
              nBins=nEvents[id]/100;
              if (nBins<=0) nBins=1;
              cProfile.add(aida.profile1D("strip_"+id,nBins,-0.5,nEvents[id]*(1-skip)+0.5));

			  /*Create the function for the profile fit*/
			  /* Create it for all the channels, to keep sync.*/
			  fFunction.add(fFactory.createFunctionFromScript("fun0_"+id,1,"A*exp(-x[0]/tau)+B","A,tau,B","",null));

              
			  if (EcalMonitoringUtilities.isInHole(row,column)==true) continue;
			  if (nEvents[id]==0) {
				  //System.out.println("LedAnalysis: channel x= "+column+" y= "+row+" not found");
				  continue;
			  }			  
			
			  /*Fill the profile*/
			  iTuple.get(id).start();
			  iTuple.get(id).skip((int)(nEvents[id]*skip)); /*This is the work-around for those channels with charge starting from 0 and rapidly growing*/
			  n=0;
			  while ( iTuple.get(id).next() ){
				  e=iTuple.get(id).getDouble(1);
				  if (e>eMax) eMax=e;
				  if (e<eMin) eMin=e;
				  cProfile.get(id).fill(1.*n,e);
				  n++;
			  }			
			  

			  /*Init function parameters*/
			  double[] initialPars= {eMax-eMin,1.*(nEvents[id]/10),eMin};
			  fFunction.get(id).setParameters(initialPars);
			  
			  /*Do the fit*/
			  fFitter=aida.analysisFactory().createFitFactory().createFitter("chi2","","V");
			  System.out.println("LedAnalysis:: do fit "+id+" "+fFitter.engineName()+" "+fFitter.fitMethodName());
			  fResult=fFitter.fit(cProfile.get(id),fFunction.get(id));
			  double[] fPars     = fResult.fittedParameters();
			  double[] fParErrs  = fResult.errors();
			  String[] fParNames = fResult.fittedParameterNames();			
			  System.out.println("Chi2 = "+fResult.quality());
			  for(int i=0; i< fResult.fittedFunction().numberOfParameters(); i++ ){
				  System.out.println(fParNames[i]+" : "+fPars[i]+" +- "+fParErrs[i]);
			  }
			  System.out.println("LedAnalysis:: fit "+id+" done \n");  
			  /*plot*/
			  pPlotter.region(0).clear();
			  pPlotter.region(0).plot(cProfile.get(id));
			  pPlotter.region(0).plot(fFunction.get(id));
			//  plotter.show();
			  
			  if (useRawEnergy){
				    	eMin=eMin/.2; //@TODO do this better
				    	eMax=eMax/.2; //@TODO do this better
			  }
			  
	 	}
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
	
/**
 * Very simple method to retreive the pedestal-subtracted raw Energy.
 * If the gain changes (because we do a re-calibration), I do not want to include this in the LED analysis
 * @param energy
 * @param cellID
 * @return
 */
	public double getRawADCSum(double energy,long cellID){
		  EcalChannelConstants channelData = ecalConditions.getChannelConstants(ecalConditions.getChannelCollection().findGeometric(cellID));
		  double RawSum=energy / ECalUtils.GeV;
		  double gain=channelData.getGain().getGain();
		  double ret=RawSum/gain;
		//  System.out.println("A:C "+RawSum+" "+ret);
		  
		  return ret;
	
	}
}	