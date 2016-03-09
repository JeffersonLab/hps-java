package org.hps.users.celentan;

import java.util.HashMap;
import java.util.Map;

import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterStyle;
import hep.aida.IProfile1D;







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
import org.hps.conditions.ecal.EcalLedCalibration.LedColor;

import java.io.*;
import java.util.Scanner;

/**
 * This is a skeleton that can be used to create a user analysis Driver in LCSim.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class LedOnlineOfflineComparisonDriver extends Driver {    


    int runNumber;

    AIDA aida;

    DatabaseConditionsManager conditionsManager;

    private EcalChannelCollection ChannelCollection;    
    private EcalLedCollection LEDCollection;
    private EcalConditions ecalConditions;
    private EcalLedCalibrationCollection LEDCalibrations;


    IHistogram1D hChargeChannelsOnline;
    IHistogram1D hChargeChannelsOffline;
    IHistogram1D hChargeChannelsRatio;
    IHistogram1D hChargeChannelsRatioAll;
    
    IHistogram2D hChargeChannelsRatio2D;
    
    Map < Integer,Double > onlineResponse;
    Map < Integer,Double > offlineResponse;
    Map < Integer,Double > onlineRMS;
    Map < Integer,Double > offlineRMS;


    private static final int NUM_CHANNELS = 442;
    /**
     * Your Driver should have a public constructor.
     */
    public  LedOnlineOfflineComparisonDriver() {
        getLogger().info("Hello  LedOnlineOfflineComparisonDriver!");
    }


    /**
     * Process a single event.  
     * Your analysis code should go in here.
     * @param event The LCSim event to process.
     */
    public void process(EventHeader event) {

    }

    /**
     * Initialization code should go here that doesn't need the conditions system or Detector.
     */
    public void startOfData() {
        getLogger().info("start of data");
    }

    /**
     * Driver setup should go here that needs information from the conditions system or Detector.
     * @param detector The LCSim Detector object.
     */
    public void detectorChanged(Detector detector) {
        getLogger().info("detector changed");

        onlineResponse = new HashMap< Integer , Double >();
        offlineResponse= new HashMap< Integer , Double >();
        onlineRMS= new HashMap< Integer , Double >();
        offlineRMS= new HashMap< Integer , Double >();

        conditionsManager = DatabaseConditionsManager.getInstance();
        ChannelCollection = conditionsManager.getCachedConditions(EcalChannelCollection.class, "ecal_channels").getCachedData();    
        ecalConditions = conditionsManager.getEcalConditions();     
        LEDCollection = conditionsManager.getCachedConditions(EcalLedCollection.class, "ecal_leds").getCachedData();
        LEDCalibrations =  conditionsManager.getCachedConditions(EcalLedCalibrationCollection.class,"ecal_led_calibrations").getCachedData();


        // Setup plots
        aida = AIDA.defaultInstance();
        aida.tree().cd("/");
        hChargeChannelsOnline =  aida.histogram1D("LEDonline",NUM_CHANNELS,-0.5,NUM_CHANNELS-0.5);
        hChargeChannelsOffline = aida.histogram1D("LEDoffline",NUM_CHANNELS,-0.5,NUM_CHANNELS-0.5);
        hChargeChannelsRatio = aida.histogram1D("ratio",NUM_CHANNELS,-0.5,NUM_CHANNELS-0.5);
        hChargeChannelsRatioAll = aida.histogram1D("ratioAll",100,-0.03,0.03);
        
        hChargeChannelsRatio2D  = aida.histogram2D("ratio2D",47, -23.5, 23.5, 11, -5.5, 5.5);
    }


    public void endOfData(){

        int channel_id;
        int row,column;
        double led_response_online,led_rms_online;
        double led_response_offline,led_rms_offline;
        double diff,ratio;


        getLogger().info("end of data");
        runNumber= conditionsManager.getRun();
        System.out.println("runNumber is:"+runNumber);
        for (EcalLedCalibration LEDcalibration : LEDCalibrations){

            channel_id=LEDcalibration.getFieldValue("ecal_channel_id");
            led_response_online=LEDcalibration.getFieldValue("led_response");
            led_rms_online=LEDcalibration.getFieldValue("rms");


            onlineResponse.put(channel_id,led_response_online);
            onlineRMS.put(channel_id, led_rms_online);
        }
        String fileName = runNumber+".raw.txt";
        try {
            //Create object of FileReader
            FileReader inputFile = new FileReader(fileName);

            //Instantiate the BufferedReader Class
            BufferedReader bufferReader = new BufferedReader(inputFile);

            //Variable to hold the one line data
            String line;

            while ((line = bufferReader.readLine()) != null)   {
                Scanner s=new Scanner(line);
                channel_id=s.nextInt();
                column=s.nextInt();
                row=s.nextInt();
                led_response_offline=s.nextDouble();
                led_rms_offline=s.nextDouble();
                s.close();
                offlineResponse.put(channel_id,led_response_offline);
                offlineRMS.put(channel_id, led_rms_offline);


            }
            //Close the buffer reader
            bufferReader.close();
        } catch (IOException e) {
            System.err.println(e);
        }
        /*now some comparisons*/
        for (EcalLedCalibration LEDcalibration : LEDCalibrations){
            channel_id=LEDcalibration.getFieldValue("ecal_channel_id");

            EcalChannel channel = ChannelCollection.findChannel(channel_id);
            row=channel.getY();
            column=channel.getX();
            
            System.out.println(channel_id+" "+column+" "+row);
            led_response_online=onlineResponse.get(channel_id);
            led_response_offline=offlineResponse.get(channel_id);

            diff=led_response_online-led_response_offline;
            ratio=2*diff/(led_response_online+led_response_offline);
            
            ratio =  led_response_online/led_response_offline;
            
            hChargeChannelsOnline.fill(channel_id,led_response_online);
            hChargeChannelsOffline.fill(channel_id,led_response_offline);
            hChargeChannelsRatio.fill(channel_id,ratio);
            hChargeChannelsRatioAll.fill(ratio);
            if ((column==-14)&&(row==-2)) continue;
            hChargeChannelsRatio2D.fill(column,row,ratio);
        }
    }
}