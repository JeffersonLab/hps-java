package org.hps.monitoring.ecal.plots;


import hep.aida.IEvaluator;
import hep.aida.IFitResult;
import hep.aida.IFitter;
import hep.aida.IFunction;
import hep.aida.IFunctionFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterStyle;
import hep.aida.IProfile1D;
import hep.aida.ITuple;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.hps.conditions.api.ConditionsObjectException;
import org.hps.conditions.api.ConditionsRecord;
import org.hps.conditions.api.DatabaseObjectException;
import org.hps.conditions.api.TableMetaData;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalChannelConstants;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.conditions.ecal.EcalLed;
import org.hps.conditions.ecal.EcalLed.EcalLedCollection;
import org.hps.conditions.ecal.EcalLedCalibration;
import org.hps.conditions.ecal.EcalLedCalibration.EcalLedCalibrationCollection;
import org.hps.recon.ecal.EcalUtils;
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
    private static final String dbTag = "led";
    private static final String dbTableName = "ecal_led_calibrations";
    private static final int runNumberMax = 9999;
    private static final int nDrivers = 8;
    private static final int nSteps = 56;

    String inputCollectionRaw = "EcalReadoutHits";
    String inputCollection = "EcalCalHits";	
    AIDA aida;

    DatabaseConditionsManager conditionsManager;

    private EcalChannelCollection ChannelCollection;	
    private EcalLedCollection LedCollection;
    private EcalConditions ecalConditions;

    Map < Integer,Integer > LedTopMap; //chid (conditions) --> LED id
    Map < Integer,Integer > LedBotMap; //chid (conditions) --> LED id 

    Map < Integer,Integer > LedTopMapInverted; //LED id --> chid (conditions)
    Map < Integer,Integer > LedBotMapInverted; //LED id  --> chid (conditions)

    private boolean useRawEnergy=false;



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
    private double[] mMean = new double[NUM_CHANNELS];
    private double[] mRMS = new double[NUM_CHANNELS];

    private int nEventsMin=200;
    private int nMinChannelsWithEvents=350; 
    private double energy,fillEnergy,fillTime;
    private double energyCut=2; //we expect very high energy from the LEDs..
    private double skipInitial=0.05;
    private double skipMin=0.3;
    private long cellID;
    
    
    
    
    //Histograms-functions-ntuples
    private ArrayList<ITuple> iTuple;

    private IFunction  fFunction,fFunction1;
    private IProfile1D   cProfile;
    private IHistogram2D			hMeanCharge2D;
    private ArrayList<IHistogram1D> hCharge;
    private ArrayList<IHistogram2D> hChargeVsEvn;
    private IPlotterFactory factory;
    private IPlotter pPlotter=null;
    private IPlotter pPlotter2=null;
    private IPlotterStyle style ;
    private int[] fitStatus = new int[NUM_CHANNELS];
    
    private boolean doFullAnalysis=false;
    private boolean isMonitoringApp=false; 
    
    private double[] fPars;    
    private double[] fPrevPars;
    private double[] fParErrs;
    private String[] fParNames; 

    
    private double fEvnMinDraw=0.;
    private double fEvnMaxDraw=80000.;
    private double fChargeMinDraw=0.;
    private double fChargeMaxDraw=100.;

    /*Components for user interaction*/
    private JDialog dialog;
    private JLabel  label;
    private JFrame frame;
    private JPanel panel;
    String  labelString;
    private JButton okButton,cancelButton;
    private int m_iteration=0;
    private int m_ret=0;
    static Object modalMonitor = new Object();
    
    public void setUseRawEnergy(boolean useRawEnergy) {
        this.useRawEnergy=useRawEnergy;
    }

    public void setEnergyCut(double energyCut) {
        this.energyCut=energyCut;
    }
    public void setSkipInitial(double skipInitial) {
        this.skipInitial=skipInitial;
    }
    public void setSkipMin(double skipMin) {
        this.skipMin=skipMin;
    }

    public void setEvnMinDraw(double evnMinDraw){
        this.fEvnMinDraw=evnMinDraw;
    }
    public void setEvnMaxDraw(double evnMaxDraw){
        this.fEvnMaxDraw=evnMaxDraw;
    }
    public void setChargeMinDraw(double chargeMinDraw){
        this.fChargeMinDraw=chargeMinDraw;
    }
    public void setChargeMaxDraw(double chargeMaxDraw){
        this.fChargeMaxDraw=chargeMaxDraw;
    }
    
    public void setNEventsMin(int nEeventsMin){
        this.nEventsMin=nEventsMin;
    }
    
    public void setIsMonitoringApp(boolean app){
        this.isMonitoringApp=app;
    }

    public void setDoFullAnalysis(boolean fullAnalysis){
        this.doFullAnalysis=fullAnalysis;
    }
    
    @Override
    protected void detectorChanged(Detector detector) {
        System.out.println("LedAnalysis::Detector changed was called");
        System.out.println(fEvnMinDraw+" "+fEvnMaxDraw);
        for (int ii=0;ii<nDrivers;ii++){
            iStep[ii]=-1;
        }

        // Setup conditions

        conditionsManager = DatabaseConditionsManager.getInstance();

        LedTopMap = new HashMap< Integer , Integer >(); //key: ecal channel ID. Value:  led id
        LedBotMap = new HashMap< Integer , Integer >();	

        LedTopMapInverted = new HashMap< Integer , Integer >(); //key: led id. Value: ecal channel id
        LedBotMapInverted = new HashMap< Integer , Integer >(); 


        ChannelCollection = conditionsManager.getCachedConditions(EcalChannelCollection.class, "ecal_channels").getCachedData();	
        LedCollection = conditionsManager.getCachedConditions(EcalLedCollection.class, "ecal_leds").getCachedData();
        ecalConditions = conditionsManager.getEcalConditions();		

        for (EcalChannel channel : ChannelCollection){
            chid = channel.getChannelId();
            for (EcalLed Led : LedCollection) {    	
                if (Led.getEcalChannelId()==chid){
                    if (channel.getY()>0){
                        LedTopMap.put( chid , Led.getLedNumber() );
                        LedTopMapInverted.put(  Led.getLedNumber(), chid  );
                    }
                    else if (channel.getY()<0){
                        LedBotMap.put( chid , Led.getLedNumber() );
                        LedBotMapInverted.put( Led.getLedNumber(), chid );                    
                    }
                }
            }
        }

    
        
        // Setup plots
        aida = AIDA.defaultInstance();
        aida.tree().cd("/");
        hMeanCharge2D = aida.histogram2D("Average LED response", 47, -23.5, 23.5, 11, -5.5, 5.5);
        
        factory= aida.analysisFactory().createPlotterFactory("Ecal Led Sequence");
        pPlotter= factory.create("Drivers");
        pPlotter.createRegions(4,2);
        if (isMonitoringApp){
            pPlotter2=factory.create("Sequence Map");
            pPlotter2.createRegions(1,1);
            pPlotter2.region(0).plot(hMeanCharge2D);
        }   
        iTuple = new ArrayList<ITuple>(NUM_CHANNELS);   
        hCharge = new ArrayList<IHistogram1D>(NUM_CHANNELS);
        hChargeVsEvn = new ArrayList<IHistogram2D>(nDrivers);



        //pPlotter2.region(0).plot(hMeanCharge2D);
        
        for (int ii=0;ii<NUM_CHANNELS;ii++){
            int row = EcalMonitoringUtilities.getRowFromHistoID(ii);
            int column = EcalMonitoringUtilities.getColumnFromHistoID(ii);	    
            iTuple.add(aida.analysisFactory().createTupleFactory(aida.tree()).create("nTuple"+ii,"nTuple"+ii,"int fEvn=0 , double fCharge=0.,double fTime=0.",""));
        }

        for (int ii=0;ii<nDrivers;ii++){
            hChargeVsEvn.add(aida.histogram2D("Driver"+ii,100,fEvnMinDraw,fEvnMaxDraw,100,fChargeMinDraw,fChargeMaxDraw));
            pPlotter.region(ii).plot( hChargeVsEvn.get(ii));
        }

        pPlotter.show();
        if (isMonitoringApp) pPlotter2.show();

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
                chid = ChannelCollection.findGeometric(cellID).getChannelId();

                energy = hit.getCorrectedEnergy();
                if (useRawEnergy){
                    fillEnergy = getRawADCSum(energy,cellID);
                }
                else {
                    fillEnergy = energy;
                }
                fillTime = hit.getTime();


                //find the LED
                if (row>0){
                    ledid=LedTopMap.get(chid);
                }
                else if (row<0){
                    ledid=LedBotMap.get(chid);
                }
                driverid=getDriver(ledid);
                if (row<0) driverid+=4;


                                
                /*Skip the events under thr*/
                if (energy<energyCut) continue;

                /*First, check if this led is the one in the NEXT step. Therefore, increment by 1 the step*/
                if (iStep[driverid]<(nSteps-1)){
                    if (ledid==LEDStep[driverid][iStep[driverid]+1]){   
                        iStep[driverid]++;
                        System.out.println("LedAnalysis:: increment step ("+iStep[driverid]+") for driver "+driverid+" . Led ID: "+ledid+" Column: "+column+" Row: "+row);
                        if (iStep[driverid]>0) drawProfiles(LEDStep[driverid][iStep[driverid]-1],driverid);      
                    }	
                }


                if (iStep[driverid]==-1) continue;
                
                /*Put this code here, since we want to always fill the ntuple*/
                iTuple.get(id).fill(0,nEvents[id]);
                iTuple.get(id).fill(1,fillEnergy);
                iTuple.get(id).fill(2,fillTime);
                iTuple.get(id).addRow();
                nEvents[id]++;
                
                /*Case 1: this led is the one in the corresponding step*/;
                if (ledid==LEDStep[driverid][iStep[driverid]]){
                    
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
        System.out.println("LedAnalysis::end of data");
        System.out.println("LedAnalysis:: nEventsMin is: "+nEventsMin);

        double e,eMin,eMax;
        double t;
        int n,nBins,nFits,nSkip;
        
       
        IFunctionFactory fFactory=aida.analysisFactory().createFunctionFactory(aida.tree());

        IFitResult fResult;
        IFitter	   fFitter;

        for (int id = 0; id < 11 * 47; id++) {

            eMin=9999;
            eMax=-9999;
            row = EcalMonitoringUtilities.getRowFromHistoID(id);
            column = EcalMonitoringUtilities.getColumnFromHistoID(id);
            System.out.println("");
            System.out.println("Doing channel: X= "+column+" Y= "+row);
            System.out.println("Number of entries in analysis ntuple: "+iTuple.get(id).rows());
            System.out.println("Number of recognized events: "+nEvents[id]);
            /*Create the profile. Create it for all the channels, to keep sync.*/
            nBins=nEvents[id]/100;
            if (nBins<=0) nBins=1;

            /*Clear previous*/

            if (id>0){
                aida.tree().rm("strip");
                aida.tree().rm("fun0");
                aida.tree().rm("fun1");
            }
            /*Create the profile.*/
            cProfile=aida.profile1D("strip",nBins,-0.5,nEvents[id]*(1-skipInitial)+0.5);
            cProfile.reset();
            /*Create the function for the profile fit and the gaus fit*/
            fFunction=fFactory.createFunctionFromScript("fun0",1,"A*exp(-x[0]/tau)+B","A,tau,B","",null);
            fFunction1=fFactory.createFunctionByName("fun1","G");

            if (EcalMonitoringUtilities.isInHole(row,column)==true){
            	System.out.println("Channel X= "+column+" Y= "+row+" is in hole. Skip");
                hCharge.add(aida.histogram1D("charge_"+id,200,0.,1.)); //create here the histogram to keep sync
                System.out.println("In hole, skip");
                continue;
            }
            else if (nEvents[id]<nEventsMin) {
                hCharge.add(aida.histogram1D("charge_"+id,200,0.,1.)); //create here the histogram to keep sync
                System.err.println("LedAnalysis:: the channel X= "+column+" Y= "+row+" has not enough events "+nEvents[id]+" "+nEventsMin);
                
                continue;
            }			  

            //Fill the profile*/
            nSkip=(int)(nEvents[id]*skipInitial);
            if (nSkip>iTuple.get(id).rows()){
                System.out.println("Can't skip initial events?");
                nSkip=0;
            }
            iTuple.get(id).start();
            iTuple.get(id).skip(nSkip); //This is the work-around for those channels with charge starting from 0 and rapidly growing//
            n=0;
            iTuple.get(id).next(); 
            while ( iTuple.get(id).next() ){
                e=iTuple.get(id).getDouble(1);
                if (e<eMin) eMin=e;           			  
                if (e>eMax) eMax=e;
                cProfile.fill(1.*n,e);
                n++;
            }			
            fFitter=aida.analysisFactory().createFitFactory().createFitter("chi2","","v");
            
            if (doFullAnalysis){ 
                //Init function parameters
                double[] initialPars={eMax-eMin,nEvents[id]/10.,eMin};
                if (initialPars[0]<0) initialPars[0]=0;
                fFunction.setParameters(initialPars);
                
                //Do the fit      
                System.out.println("LedAnalysis:: do profile fit "+id+" "+fFitter.engineName()+" "+fFitter.fitMethodName());
                System.out.println("LedAnalysis:: initial parameters "+initialPars[0]+" "+initialPars[1]+" "+initialPars[2]);
                fResult=fFitter.fit(cProfile,fFunction);
                fPars     = fResult.fittedParameters();
                fParErrs  = fResult.errors();
                fParNames = fResult.fittedParameterNames();			
                System.out.println("LedAnalysis:: Status= "+fResult.fitStatus()+" "+fResult.isValid()+" Chi2 = "+fResult.quality()+" NDF: "+fResult.ndf());
                for(int i=0; i< fResult.fittedFunction().numberOfParameters(); i++ ){
                    System.out.println(fParNames[i]+" : "+fPars[i]+" +- "+fParErrs[i]);
                }  
                fFunction.setParameters(fPars);
                
                
                //Do again the fit: it is a terrible work-around
                nFits=0;
                if (Double.isNaN(fParErrs[1])){
                    fPars=fPrevPars;
                }
                while (Double.isNaN(fParErrs[1])){
                    System.out.println("LedAnalysis:: redo fit");
                    fFunction.setParameters(fPars);
                    fResult=fFitter.fit(cProfile,fFunction);
                    fPars     = fResult.fittedParameters();
                    fParErrs  = fResult.errors();
                    System.out.println("LedAnalysis:: Status= "+fResult.fitStatus()+" "+fResult.isValid()+" Chi2 = "+fResult.quality()+" NDF: "+fResult.ndf());
                    for(int i=0; i< fResult.fittedFunction().numberOfParameters(); i++ ){
                        System.out.println(fParNames[i]+" : "+fPars[i]+" +- "+fParErrs[i]);
                    }  
                    fFunction.setParameters(fPars);
                    nFits++;
                    if (nFits>=10){
                        System.out.println("LedAnalysis:: Error, too many fits without convergence");
                        break;
                    }
                }
                fPrevPars=Arrays.copyOf(fPars,fPars.length);
                System.out.println("LedAnalysis:: fit "+id+" done");  
                
                //Now we have the tau parameter. Take ONLY the events that are with N>5*tau/
                //As a cross-check, also verify that tau > Nevents/10, otherwise skip the first Nevents/2
                //and emit warning
                nSkip=(int)( fPars[1]*5);
                if (nSkip < (nEvents[id]*skipMin)){
                    System.out.println("LedAnalysis:: Skip number too low: "+nSkip+" Increment it to "+nEvents[id]/2);
                    nSkip=(int)(nEvents[id]*skipMin);
                }
                if (nSkip > nEvents[id]){
                    System.out.println("LedAnalysis:: Skip number too high, reduce it");
                    nSkip=(int)(nEvents[id]*skipMin);
                }
        
            }
            else{
                nSkip=(int)(nEvents[id]*(skipMin+skipInitial));
            }
            
            System.out.println("LedAnalysis:: gaus fit :: Going to skip "+nSkip+" out of "+nEvents[id]);
            System.out.println("eMin is: "+eMin+" eMax is: "+eMax);
            hCharge.add(aida.histogram1D("charge_"+id,200,eMin*0.9,eMax*1.1));
       
         
            iTuple.get(id).start();
            iTuple.get(id).skip(nSkip); 
            n=0;
            while (iTuple.get(id).next()){
                e=iTuple.get(id).getDouble(1);
                t=iTuple.get(id).getDouble(2);
                hCharge.get(id).fill(e);
                n++;
            }			

            /*Finally do the fit with the gaussian*/
            double[] initialPars1={hCharge.get(id).maxBinHeight(),hCharge.get(id).mean(),hCharge.get(id).rms()};

            System.out.println("LedAnalysis:: Gaus fit");
            System.out.println("LedAnalysis:: initial parameters "+initialPars1[0]+" "+initialPars1[1]+" "+initialPars1[2]);

            fFunction1.setParameters(initialPars1);
            fResult=fFitter.fit(hCharge.get(id),fFunction1);
            fPars     = fResult.fittedParameters();
            fParErrs  = fResult.errors();
            fParNames = fResult.fittedParameterNames();			
            System.out.println("Status= "+fResult.fitStatus()+" "+fResult.isValid()+" Chi2 = "+fResult.quality()+" NDF: "+fResult.ndf());
            for(int i=0; i< fResult.fittedFunction().numberOfParameters(); i++ ){
                System.out.println(fParNames[i]+" : "+fPars[i]+" +- "+fParErrs[i]);
            }  
            fFunction1.setParameters(fPars);
            mMean[id]=fPars[1];
            mRMS[id]=fPars[2];

            hMeanCharge2D.fill(column,row,mMean[id]);
            System.out.println("\n");
        }//End loop on channels



        if ((pPlotter2!=null)&&(isMonitoringApp)){
            style = pPlotter2.region(0).style();
            style.setParameter("hist2DStyle", "colorMap");
            style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
            style.dataStyle().fillStyle().setParameter("showZeroHeightBins", Boolean.FALSE.toString()); 
            pPlotter2.region(0).plot(hMeanCharge2D);
            pPlotter2.region(0).refresh();
        }
        else{
            IPlotterStyle pstyle =  aida.analysisFactory().createPlotterFactory().createPlotterStyle();
            pPlotter2 = null;
            pPlotter2 =  aida.analysisFactory().createPlotterFactory().create();
            pstyle.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
            pstyle.dataStyle().fillStyle().setParameter("showZeroHeightBins", Boolean.FALSE.toString());
            pstyle.setParameter("hist2DStyle", "colorMap");
            if (pPlotter2!=null){
                pPlotter2.createRegion().plot(hMeanCharge2D,pstyle);
                pPlotter2.show();
            }
        }



        
        /*
        Console cc = System.console();
        if (cc == null) {
            System.err.println("No console.");
        }
        else{
            String userInput="";
            System.out.println("Enter 'YES' - case sensitive - to write conditions database for run range "+runNumber+" - "+runNumberMax);
            System.out.println("Use the monitoring app (Led sequence - Sequence Map) to look at the current sequence");
            userInput=cc.readLine(String.format("Your choice: YES or NO?"));
            System.out.println("***********"+userInput+"********");
            if (userInput!=null && userInput.equals("YES")) {
                userInput=cc.readLine("Really?");
                if (userInput!=null && userInput.equals("YES")) {
                   m_ret=1;
                }
            }
            
        }*/
        askUploadToDBDialog();
        synchronized (modalMonitor) {
            try{
            modalMonitor.wait(120000); //wait 2 minutes for user interaction.
            }
            catch(InterruptedException excp){
                System.out.println("Got exception: "+excp);
            }
        }
        if (m_ret==1){
            System.out.println("OK, upload to DB");
            try {
            	uploadToDB();
            } catch (SQLException | DatabaseObjectException | ConditionsObjectException error) {
            	throw new RuntimeException("Error uploading to the database.", error);
            }
            if (isMonitoringApp){
                System.out.println("Save an Elog too");
                uploadToElog();
            }
        }
       System.out.println("endOfData end");
       System.out.println("The program is not stucked. It is writing the output AIDA file, this takes time!");
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
        double RawSum=energy / EcalUtils.GeV;
        double gain=channelData.getGain().getGain();
        double ret=RawSum/gain;
        //  System.out.println("A:C "+RawSum+" "+ret);

        return ret;

    }

    private void uploadToDB() throws DatabaseObjectException, ConditionsObjectException, SQLException {
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
            mean=mMean[id];
            rms=mRMS[id];
            led_calibrations.add(new EcalLedCalibration(cid,mean,rms));
        }

        int collectionId = -1;

        try {
            collectionId = conditionsManager.getCollectionId(led_calibrations, "EcalLedSequenceMonitor generated by " + System.getProperty("user.name"), null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.err.println("CollectionID:  "+collectionId);
        led_calibrations.insert();
        ConditionsRecord conditionsRecord = new ConditionsRecord(
        		led_calibrations.getCollectionId(), runNumber, runNumberMax, dbTableName, dbTableName, 
        		"Generated by LedAnalysis from Run #"+runNumber, dbTag);
        conditionsRecord.insert();

        System.out.println("Upload to DB done");
    }
    
    private void uploadToElog(){
        String path,exe,command,imgpath;
        path="/home/hpsrun/LedSequenceData";
        exe=path+"/doElog.csh";
        imgpath=path+"/screenshots/"+runNumber+".png";
        
        File f=new File(path);
        if (!f.exists()){
            System.err.println("LedMonitoringSequence:: wrong path");
            return;
        }
        if (pPlotter2==null){
            System.err.println("LedMonitoringSquence:: no plotter");
            return;
        }
        try{
            pPlotter2.writeToFile(imgpath);
        }
        catch(Exception e){
            System.err.println("Exception "+e);
        }
        File f1=new File(exe);
        if (!f1.exists()){
            System.err.println("LedMonitoringSequence:: no script!");
            return;
        }   
        command=exe+" "+imgpath;
        try{
            System.out.println("LedMonitoringSequence:: try this command: "+command);
            Runtime.getRuntime().exec(command);
        }
        catch(Exception e){
            System.err.println("Exception "+e);
        }
    }
    
    
    private void drawProfiles(int ledID,int driverID){

        int m_column,m_row,m_ledID,m_chID,m_ID,m_driverID;

        m_ledID = ledID;
        m_driverID = driverID;
        m_chID = 0;


        if (m_driverID<=3) m_chID = LedTopMapInverted.get(ledID);
        else m_chID = LedBotMapInverted.get(ledID);

        m_column=findChannel(m_chID).getX();
        m_row=findChannel(m_chID).getY();
        m_ID=EcalMonitoringUtilities.getHistoIDFromRowColumn(m_row, m_column);
        /* 
        System.out.println("Going to draw LED id "+m_ledID+" X= "+m_column+" Y= "+m_row+" driver: "+m_driverID);
        System.out.println("Ch_ID: "+m_chID);
        System.out.println("Histo ID:"+m_ID);
        System.out.println("Events: "+iTuple.get(m_ID).rows());
         */      
        hChargeVsEvn.get(m_driverID).reset();
        hChargeVsEvn.get(m_driverID).setTitle("Driver_"+m_driverID+" Led_"+ledID);
        IEvaluator evaluatorX = aida.analysisFactory().createTupleFactory(aida.analysisFactory().createTreeFactory().create()).createEvaluator("fEvn");
        IEvaluator evaluatorY = aida.analysisFactory().createTupleFactory(aida.analysisFactory().createTreeFactory().create()).createEvaluator("fCharge");  

        iTuple.get(m_ID).project(hChargeVsEvn.get(m_driverID),evaluatorX,evaluatorY);

        pPlotter.region(m_driverID).clear();  
        pPlotter.region(m_driverID).plot(hChargeVsEvn.get(m_driverID));
        pPlotter.region(m_driverID).refresh();

    }

    private EcalChannel findChannel(int channel_id) {
        return ecalConditions.getChannelCollection().findChannel(channel_id);
    }


    private void askUploadToDBDialog(){
        m_ret=0;

        okButton = new JButton("OK");
        cancelButton = new JButton("Cancel");
        labelString = "<html> Update conditions to DB <br> for run: <br> "+runNumber+" - "+runNumberMax+" <br> ???? <br> "
                + "Use the monitoring app to look at the map<br>" 
                + "(Tab LED sequence)<br"
                +"Reply in 60 seconds<br>"+"</html>";   
        label = new JLabel( labelString);
            
        frame  = new JFrame("Upload to DB?");
        frame.setSize(500,250);
        panel = new JPanel();
        frame.add(panel);
        
        
       // dialog = new JDialog((JFrame)null, "User selection");
       // dialog.setSize(200,200);
       // dialog.setLayout(new FlowLayout());
       // dialog.add(label);
       // dialog.add(cancelButton);
       // dialog.add(okButton);
       // dialog.setVisible(true);
        //dialog.pack();
        panel.add(label);
        panel.add(cancelButton);
        panel.add(okButton);
        
          frame.setVisible(true);
        okButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent event)
            {
                m_ret=1;
                frame.dispose();    
                synchronized(modalMonitor)
                {
                    System.out.println("Ok pressed");
                    modalMonitor.notify();
                }
            }
        }
                );

        cancelButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent event)
            {
                m_ret=0;
                frame.dispose();   
                synchronized(modalMonitor)
                {
                    System.out.println("Cancel pressed");
                    modalMonitor.notify();
                }
            }
        }
        );
        
        System.out.println("askUploadDB done");
    }
    
}
