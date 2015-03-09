package org.hps.monitoring.ecal.plots;


import hep.aida.ICloud1D;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IProfile1D;
import hep.aida.ITuple;
import hep.aida.IFunction;
import hep.aida.IPlotter;
import hep.aida.IFitter;
import hep.aida.IFitResult;
import hep.aida.IFunctionFactory;
import hep.aida.IPlotterStyle;




import javax.swing.JOptionPane; 

import java.io.Console;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.conditions.api.ConditionsObjectException;
import org.hps.conditions.api.ConditionsRecord;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.database.TableMetaData;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalCalibration.EcalCalibrationCollection;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalChannel.GeometryId;
import org.hps.conditions.ecal.EcalLed;
import org.hps.conditions.ecal.EcalLed.EcalLedCollection;
import org.hps.conditions.ecal.EcalLedCalibration;
import org.hps.conditions.ecal.EcalLedCalibration.EcalLedCalibrationCollection;
import org.hps.conditions.ecal.EcalCalibration;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.conditions.ecal.EcalChannelConstants;
import org.hps.recon.ecal.ECalUtils;
import org.hps.monitoring.ecal.plots.EcalMonitoringUtilities;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/* This is the driver used to determine the response of each calorimeter channel after a LED run
 * @author Andrea Celentano  <andrea.celentano@ge.infn.it>
 */
public class EcalLedSequenceMonitor extends Driver{

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

    private static final String dbTag = "led";
    private static final String dbTableName = "ecal_led_calibrations";
    private static final int runNumberMax = 9999;
    private static final int nDrivers = 8;
    private static final int nSteps = 56;

    private int runNumber = 0;	
    private int eventN    = 0;
    private int id,row,column,chid,ledid,driverid;
    private  int[][] LEDStep = new int[][]{
            //first 4 are the flasher1 sequence, TOP controller 
            {2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,-1,-1},
            {56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,90,91,92,93,94,95,96,97,98,99,100,101,102,103,104,105,106,107,108,109,110,111},
            {112,113,114,115,116,117,118,119,120,121,122,123,124,125,126,127,128,129,130,131,132,133,134,136,137,138,139,140,141,142,143,144,145,146,147,148,149,150,151,152,153,154,155,156,157,158,159,160,161,162,163,164,165,166,167,-1}, //missing 135 is ok
            {168,169,170,171,172,173,174,175,176,177,178,179,180,181,182,183,184,185,186,187,188,189,190,191,192,193,194,195,196,197,198,199,200,201,202,203,204,205,206,207,208,209,210,211,212,213,214,215,216,217,218,219,220,221,222,223},
            //second 4 are the flasher2 sequence, BOTTOM controller 
            {2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,-1,-1},	
            {56,57,58,59,60,61,62,63,64,65,66,67,68,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,90,91,92,93,94,95,96,97,98,99,100,101,102,103,104,105,106,107,108,109,110,111,-1}, //missing 69 is OK
            {112,113,114,115,116,117,118,119,120,121,122,123,124,125,126,127,128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143,144,145,146,147,148,149,150,151,152,153,154,155,156,157,158,159,160,161,162,163,164,165,166,167},	
            {168,169,170,171,172,173,174,175,176,177,178,179,180,181,182,183,184,185,186,187,188,189,190,191,192,193,194,195,196,197,198,199,200,201,202,203,204,205,206,207,208,209,210,211,212,213,214,215,216,217,218,219,220,221,222,223},
    };

    private int[] iStep = new int[nDrivers];
    private int[] nEvents = new int[NUM_CHANNELS];

    private double energy,fillEnergy,fillTime;
    private long cellID;
    //Histograms-functions-ntuples
    private ArrayList<ITuple> iTuple;
    private ArrayList<IProfile1D> cProfile;
    private ArrayList<IFunction> fFunction;
    private ArrayList<IFunction> fFunction1;
    private ArrayList<IHistogram1D> hCharge;
    private ArrayList<IHistogram2D> hChargeVsTime;
    private ArrayList<IHistogram1D> hChargeALL;
    private ArrayList<IHistogram2D> hChargeVsTimeALL;
    IHistogram2D					hMeanCharge2D;

    public void setUseRawEnergy(boolean useRawEnergy) {
        this.useRawEnergy=useRawEnergy;
    }


    private double skipInitial=0.1;

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

        ChannelCollection = conditionsManager.getCachedConditions(EcalChannelCollection.class, "ecal_channels").getCachedData();	
        LedCollection = conditionsManager.getCachedConditions(EcalLedCollection.class, "ecal_leds").getCachedData();
        ecalConditions = conditionsManager.getEcalConditions();		

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
        fFunction1= new ArrayList<IFunction>(NUM_CHANNELS);		
        hCharge = new ArrayList<IHistogram1D>(NUM_CHANNELS);
        // hChargeVsTime = new ArrayList<IHistogram2D>(NUM_CHANNELS);
        hChargeALL = new ArrayList<IHistogram1D>(NUM_CHANNELS);
        hChargeVsTimeALL = new ArrayList<IHistogram2D>(NUM_CHANNELS);
        hMeanCharge2D = aida.histogram2D("Average LED response", 47, -23.5, 23.5, 11, -5.5, 5.5);

        for (int ii=0;ii<NUM_CHANNELS;ii++){
            int row = EcalMonitoringUtilities.getRowFromHistoID(ii);
            int column = EcalMonitoringUtilities.getColumnFromHistoID(ii);	    
            iTuple.add(aida.analysisFactory().createTupleFactory(aida.tree()).create("nTuple"+ii,"nTuple"+ii,"int fEvn=0 , double fCharge=0.,double fTime=0.",""));

            hChargeALL.add(aida.histogram1D("ChargeAllEvents_"+ii,400,0.,100.));
            hChargeVsTimeALL.add(aida.histogram2D("ChargeVsTimeAllEvents_"+ii,100,0.,400.,100,0.,100.));
        }

    }		

    @Override
    public void process(EventHeader event) {
        runNumber = event.getRunNumber();
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
                    fillEnergy = getRawADCSum(energy,cellID);
                }
                else {
                    fillEnergy = energy;
                }
                fillTime = hit.getTime();

                //fill "all" histograms
                hChargeALL.get(id).fill(fillEnergy);
                hChargeVsTimeALL.get(id).fill(fillTime,fillEnergy);

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
                        System.out.println("LedAnalysis:: increment step ("+iStep[driverid]+") for driver "+driverid+" . Led ID: "+ledid+" Column: "+column+" Row: "+row);
                    }	
                }


                if (iStep[driverid]==-1) continue;

                /*Case 1: this led is the one in the corresponding step*/;
                //if (ledid==LEDStep[driverid][iStep[driverid]]){
                if (true){

                    iTuple.get(id).fill(0,nEvents[id]);
                    iTuple.get(id).fill(1,fillEnergy);
                    iTuple.get(id).fill(2,fillTime);
                    iTuple.get(id).addRow();


                    nEvents[id]++;
                }
                else{	/*Case 2: this led is not one in the corresponding step (but maybe is the neighborhood??Ctalk??)*/;

                }

                /*Add a debug print */
                if (eventN % 10000==0){
                    System.out.println("Debug. LED ID: "+ledid+" DRIVER ID: "+driverid+" ECAL ID: "+id+" ROW: "+row+" COLUMN: "+column+ "HISTO ID: "+id);
                }
            }
            if (eventN % 10000==0){
                System.out.println("\n");
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
    }
    public void MYendOfData() {
        System.out.println("LedAnalysis::end of data");

     
        double e,eMin,eMax;
        double t;
        int n,nBins,nFits,nSkip;

        double[] fPars;	
        double[] fParErrs;
        String[] fParNames;	
        IFunctionFactory fFactory=aida.analysisFactory().createFunctionFactory(aida.tree());
        IPlotter pPlotter= aida.analysisFactory().createPlotterFactory().create();
        IFitResult fResult;
        IFitter	   fFitter;

        for (int id = 0; id < 11 * 47; id++) {

            eMin=9999;
            eMax=-9999;
            row = EcalMonitoringUtilities.getRowFromHistoID(id);
            column = EcalMonitoringUtilities.getColumnFromHistoID(id);
            System.out.println("Doing channel: X= "+column+" Y= "+row);
            /*Create the profile. Create it for all the channels, to keep sync.*/
            nBins=nEvents[id]/100;
            if (nBins<=0) nBins=1;
            cProfile.add(aida.profile1D("strip_"+id,nBins,-0.5,nEvents[id]*(1-skipInitial)+0.5));

            /*Create the function for the profile fit and the gaus fit*/
            /* Create it for all the channels, to keep sync.*/
            fFunction.add(fFactory.createFunctionFromScript("fun0_"+id,1,"A*exp(-x[0]/tau)+B","A,tau,B","",null));
            fFunction1.add(fFactory.createFunctionByName("fun1_"+id,"G"));

            if (EcalMonitoringUtilities.isInHole(row,column)==true){
                hCharge.add(aida.histogram1D("charge_"+id,200,0.,1.)); //create here the histogram to keep sync
                //   hChargeVsTime.add(aida.histogram2D("chargeVsTime_"+id,200,0.,400.,200,0.,1.));
                continue;
            }
            if (nEvents[id]==0) {
                hCharge.add(aida.histogram1D("charge_"+id,200,0.,1.)); //create here the histogram to keep sync
                //  hChargeVsTime.add(aida.histogram2D("chargeVsTime_"+id,200,0.,400.,200,0.,1.));            
                //System.out.println("LedAnalysis: channel x= "+column+" y= "+row+" not found");
                continue;
            }			  

            /*Fill the profile*/
            nSkip=(int)(nEvents[id]*skipInitial);
            if (nSkip>iTuple.get(id).rows()){
                System.out.println("Can't skip initial events?");
                nSkip=0;
            }
            iTuple.get(id).start();
            iTuple.get(id).skip(nSkip); /*This is the work-around for those channels with charge starting from 0 and rapidly growing*/
            n=0;
            iTuple.get(id).next(); e=iTuple.get(id).getDouble(1); eMax=e; n++; /*eMax is the first sample*/
            while ( iTuple.get(id).next() ){
                e=iTuple.get(id).getDouble(1);
                eMin=e;           			  /*eMin is the last sample*/
                cProfile.get(id).fill(1.*n,e);
                n++;
            }			


            /*Init function parameters*/
            double[] initialPars={eMax-eMin,nEvents[id]/10.,eMin};
            fFunction.get(id).setParameters(initialPars);

            /*Do the fit*/
            fFitter=aida.analysisFactory().createFitFactory().createFitter("chi2","","v");
            System.out.println("LedAnalysis:: do profile fit "+id+" "+fFitter.engineName()+" "+fFitter.fitMethodName());
            System.out.println("LedAnalysis:: initial parameters "+initialPars[0]+" "+initialPars[1]+" "+initialPars[2]);
            fResult=fFitter.fit(cProfile.get(id),fFunction.get(id));
            fPars     = fResult.fittedParameters();
            fParErrs  = fResult.errors();
            fParNames = fResult.fittedParameterNames();			
            System.out.println("LedAnalysis:: Status= "+fResult.fitStatus()+" "+fResult.isValid()+" Chi2 = "+fResult.quality()+" NDF: "+fResult.ndf());
            for(int i=0; i< fResult.fittedFunction().numberOfParameters(); i++ ){
                System.out.println(fParNames[i]+" : "+fPars[i]+" +- "+fParErrs[i]);
            }  
            fFunction.get(id).setParameters(fPars);


            /*Do again the fit: it is a terrible work-around*/
            nFits=0;
            while (Double.isNaN(fParErrs[1])){
                System.out.println("LedAnalysis:: redo fit");
                fFunction.get(id).setParameters(fPars);
                fResult=fFitter.fit(cProfile.get(id),fFunction.get(id));
                fPars     = fResult.fittedParameters();
                fParErrs  = fResult.errors();
                System.out.println("LedAnalysis:: Status= "+fResult.fitStatus()+" "+fResult.isValid()+" Chi2 = "+fResult.quality()+" NDF: "+fResult.ndf());
                for(int i=0; i< fResult.fittedFunction().numberOfParameters(); i++ ){
                    System.out.println(fParNames[i]+" : "+fPars[i]+" +- "+fParErrs[i]);
                }  
                fFunction.get(id).setParameters(fPars);
                nFits++;
                if (nFits>=10){
                    System.out.println("LedAnalysis:: Error, too many fits without convergence");
                    break;
                }
            }

            System.out.println("LedAnalysis:: fit "+id+" done");  

            /*Now we have the tau parameter. Take ONLY the events that are with N>5*tau/
				As a cross-check, also verify that tau > Nevents/10, otherwise skip the first Nevents/2
				and emit warning
             */
            hCharge.add(aida.histogram1D("charge_"+id,200,eMin*0.9,eMax*1.1));
            // hChargeVsTime.add(aida.histogram2D("chargeVsTime_"+id,200,0.,400.,200,eMin*0.9,eMax*1.1));     
            nSkip=(int)( fPars[1]*5);
            if (nSkip < (nEvents[id]/2)){
                System.out.println("LedAnalysis:: Skip number too low: "+nSkip+" Increment it to "+nEvents[id]/2);
                nSkip=nEvents[id]/2;
            }
            if (nSkip > nEvents[id]){
                System.out.println("LedAnalysis:: Skip number too high, reduce it");
                nSkip=nEvents[id]/2;
            }
            iTuple.get(id).start();
            iTuple.get(id).skip(nSkip); /*This is the work-around for those channels with charge starting from 0 and rapidly growing*/
            n=0;
            while ( iTuple.get(id).next() ){
                e=iTuple.get(id).getDouble(1);
                t=iTuple.get(id).getDouble(2);
                hCharge.get(id).fill(e);
                //   hChargeVsTime.get(id).fill(t,e);
                n++;
            }			

            /*Finally do the fit with the gaussian*/
            double[] initialPars1={hCharge.get(id).maxBinHeight(),hCharge.get(id).mean(),hCharge.get(id).rms()};

            System.out.println("LedAnalysis:: Gaus fit");
            System.out.println("LedAnalysis:: initial parameters "+initialPars1[0]+" "+initialPars1[1]+" "+initialPars1[2]);

            fFunction1.get(id).setParameters(initialPars1);
            fResult=fFitter.fit(hCharge.get(id),fFunction1.get(id));
            fPars     = fResult.fittedParameters();
            fParErrs  = fResult.errors();
            fParNames = fResult.fittedParameterNames();			
            System.out.println("Status= "+fResult.fitStatus()+" "+fResult.isValid()+" Chi2 = "+fResult.quality()+" NDF: "+fResult.ndf());
            for(int i=0; i< fResult.fittedFunction().numberOfParameters(); i++ ){
                System.out.println(fParNames[i]+" : "+fPars[i]+" +- "+fParErrs[i]);
            }  
            fFunction1.get(id).setParameters(fPars);

            hMeanCharge2D.fill(column,row,fPars[1]);
            System.out.println("\n");
        }/*End loop on channels*/


        pPlotter.createRegions(1,1);
        IPlotterStyle style = pPlotter.region(0).style();
        style.setParameter("hist2DStyle", "colorMap");
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        style.dataStyle().fillStyle().setParameter("showZeroHeightBins", Boolean.FALSE.toString());

        pPlotter.region(0).plot(hMeanCharge2D);
        pPlotter.show();

        int ret=JOptionPane.showConfirmDialog(null, "Do you want to load these conditions to the database\n"+
                "for Runs: "+runNumber+" "+runNumberMax+" ? ", "Message", 
                JOptionPane.YES_NO_OPTION);
        if (ret == JOptionPane.YES_OPTION){
            ret=JOptionPane.showConfirmDialog(null, "Confirm?", "Message", 
                    JOptionPane.YES_NO_OPTION);
            if (ret == JOptionPane.YES_OPTION){
                System.out.println("You confirmed \n");
                System.out.println("Load DB condtions \n");
                //               uploadToDB();
            }   
        }
        /* System.err.println("\n\n\n***************************************************************\n");
        String userInput="";
        String outputFilePrefix="";
        userInput=cc.readLine("Enter filename prefix, or just press RETURN ...");
        if (userInput==null || userInput.length()==0 || userInput=="") {
            String home=System.getenv().get("HOME");
            outputFilePrefix = home+"/LedAnalysis_"+runNumber+"_";
        } else {
            outputFilePrefix = userInput;
        }*/






    }/*End endOfData*/


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

    private void uploadToDB() {
        int x,y,id;
        double mean,rms;
        System.out.println(String.format("Uploading new led data to the database, runMin=%d, runMax=%d, tag=%s ....",
                runNumber,runNumberMax,dbTag));

        EcalLedCalibrationCollection led_calibrations =  new EcalLedCalibrationCollection();

        TableMetaData tableMetaData = conditionsManager.findTableMetaData(dbTableName);
        led_calibrations.setTableMetaData(tableMetaData);

        for (int cid = 1; cid <= 442; cid++) {/*This is a loop over the channel ID, as in the conditions system*/
            EcalChannel cc = findChannel(cid);
            x = cc.getX(); //This is the column
            y = cc.getY(); //This is the row
            id=EcalMonitoringUtilities.getHistoIDFromRowColumn(y,x);
            mean=fFunction1.get(id).parameters()[1];
            rms=fFunction1.get(id).parameters()[2];
            led_calibrations.add(new EcalLedCalibration(cid,mean,rms));
        }

        int collectionId = conditionsManager.getNextCollectionID(dbTableName);
        try {
            led_calibrations.setCollectionId(collectionId);
            System.err.println("CollectionID:  "+collectionId);
            led_calibrations.insert();
            ConditionsRecord conditionsRecord = new ConditionsRecord(
                    led_calibrations.getCollectionId(), runNumber, runNumberMax, dbTableName, dbTableName, 
                    "Generated by LedAnalysis from Run #"+runNumber, dbTag);
            conditionsRecord.insert();

        } catch (ConditionsObjectException | SQLException e) {
            throw new RuntimeException(e);
        }


    }



    public EcalChannel findChannel(int channel_id) {
        return ecalConditions.getChannelCollection().findChannel(channel_id);
    }




}
