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
 */
public class LedOnlineDataDumpDriver extends Driver {    


	int runNumber;

	
	DatabaseConditionsManager conditionsManager;

	private EcalChannelCollection ChannelCollection;	
	private EcalLedCollection LEDCollection;
	private EcalConditions ecalConditions;
	private EcalLedCalibrationCollection LEDCalibrations;


	
	
	Map < Integer,Double > onlineResponse;
	Map < Integer,Double > offlineResponse;
	Map < Integer,Double > onlineRMS;
	Map < Integer,Double > offlineRMS;


	private static final int NUM_CHANNELS = 442;
	/**
	 * Your Driver should have a public constructor.
	 */
	public  LedOnlineDataDumpDriver(){
		getLogger().info("Hello LedOnlineDataDumpDriver!");
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
		

		
		}
		String fileName = runNumber+".raw.txt";
		try {
			//Create object of FileWriter
			FileWriter outputFile = new FileWriter(fileName);

			//Instantiate the BufferedWriter Class
			BufferedWriter bufferWriter = new BufferedWriter(outputFile);

			//Variable to hold the one line data
			String line;
		    for (EcalLedCalibration LEDcalibration : LEDCalibrations){
		        
		        channel_id=LEDcalibration.getFieldValue("ecal_channel_id");
	            led_response_online=LEDcalibration.getFieldValue("led_response");
	            led_rms_online=LEDcalibration.getFieldValue("rms");
	            EcalChannel ch=ChannelCollection.findChannel(channel_id);
	            row=ch.getY();
	            column=ch.getX();
	            
	            line=channel_id+" "+column+" "+row+" "+led_response_online+" "+led_rms_online+"\n";
	            bufferWriter.write(line);
	           


			}
			//Close the buffer reader
			bufferWriter.close();
		} catch (IOException e) {
			System.err.println(e);
		}
	}
}