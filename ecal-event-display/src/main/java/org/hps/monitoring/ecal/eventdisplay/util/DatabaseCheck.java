package org.hps.monitoring.ecal.eventdisplay.util;

import java.awt.Point;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.conditions.ecal.EcalLed;
import org.hps.conditions.ecal.EcalLed.EcalLedCollection;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;

public class DatabaseCheck {
    private static final Set<Integer> idFailSet = new HashSet<Integer>();
    private static final Set<Point> pointFailSet = new HashSet<Point>();
    
    public static void main(String[] args) throws ConditionsNotFoundException, IOException {
        // Check that an appropriate file has been given.
        String filepath = null;
        if(args.length == 1) {
            filepath = args[0];
        }
        
        // If no file path was defined, throw an error.
        if(filepath == null) {
            throw new FileNotFoundException("No CSV mapping file defined.");
        }
        
        // Initialize the local database.
        EcalWiringManager manager = new EcalWiringManager(filepath);
        
        // Initialize the database.
        int runNumber = 2000;
        String detectorName = "HPS-Proposal2014-v7-2pt2";
        DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();
        conditionsManager.setDetector(detectorName, runNumber);
        
        // Get ECAL conditions.
        EcalConditions ecalConditions = DatabaseConditionsManager.getInstance().getEcalConditions(); 
        
        // Get the list of EcalChannel objects.
        EcalChannelCollection channels = ecalConditions.getChannelCollection();
        EcalLedCollection leds = conditionsManager.getCachedConditions(EcalLedCollection.class, "ecal_leds").getCachedData();
        
        // Map the LED objects to their channels.
        Map<Integer, EcalLed> ledMap = new HashMap<Integer, EcalLed>();
        for (EcalLed led : leds) {
            ledMap.put(led.getEcalChannelId(), led);
        }
        
        // Perform the comparison test.
        for(EcalChannel channel : channels) {
            // Get the crystal point information.
            Point crystal = new Point(channel.getX(), channel.getY());
            
            // Get the data from manager.
            CrystalDataSet data = manager.getCrystalData(crystal);
            
            // Get the appropriate LED collection.
            EcalLed led = ledMap.get(channel.getChannelId());
            
            // Perform the comparison.
            System.out.printf("Checking Mappings for Crystal (%3d, %3d):%n", crystal.x, crystal.y);
            System.out.printf("\tChannel ID      :: %d%n", channel.getChannelId());
            
            System.out.printf("\tChannel     [ %3d ] vs [ %3d ] ... ", channel.getChannel(), data.getFADCChannel());
            if(channel.getChannel() == data.getFADCChannel()) {
                System.out.printf("[ Success ]%n");
            } else {
                System.out.printf("[ Failure ]%n");
                idFailSet.add(channel.getChannelId());
                pointFailSet.add(crystal);
            }
            
            int crate = data.getMotherboard().isTop() ? 1 : 2;
            System.out.printf("\tCrate       [ %3d ] vs [ %3d ] ... ", channel.getCrate(), crate);
            if(channel.getCrate() == crate) {
                System.out.printf("[ Success ]%n");
            } else {
                System.out.printf("[ Failure ]%n");
                idFailSet.add(channel.getChannelId());
                pointFailSet.add(crystal);
            }
            
            System.out.printf("\tSlot        [ %3d ] vs [ %3d ] ... ", channel.getSlot(), data.getFADCSlot());
            if(channel.getSlot() == data.getFADCSlot()) {
                System.out.printf("[ Success ]%n");
            } else {
                System.out.printf("[ Failure ]%n");
                idFailSet.add(channel.getChannelId());
                pointFailSet.add(crystal);
            }
            
            System.out.printf("\tLED Channel [ %3d ] vs [ %3d ] ... ", led.getLedNumber(), data.getLEDChannel());
            if(led.getLedNumber() == data.getLEDChannel()) {
                System.out.printf("[ Success ]%n");
            } else {
                System.out.printf("[ Failure ]%n");
                idFailSet.add(channel.getChannelId());
                pointFailSet.add(crystal);
            }
            
            System.out.printf("\tLED Crate   [ %3d ] vs [ %3d ] ... ", led.getCrateNumber(), crate);
            if(led.getCrateNumber() == crate) {
                System.out.printf("[ Success ]%n");
            } else {
                System.out.printf("[ Failure ]%n");
                idFailSet.add(channel.getChannelId());
                pointFailSet.add(crystal);
            }
            
            System.out.println();
            System.out.println();
        }
        
        // Print out the failing crystals.
        System.out.println("Crystals that Failed:");
        for(Point fail : pointFailSet) {
            System.out.printf("\tCrystal (%3d, %3d)%n", fail.x, fail.y);
        }
        if(pointFailSet.isEmpty()) {
            System.out.println("\tNone!");
        }
        
        // Indicate the database connection settings.
        System.out.println("\n");
        System.out.printf("Detector           :: %s%n", detectorName);
        System.out.printf("Run Number         :: %d%n", runNumber);
        System.out.printf("Channel Collection :: %d%n", channels.getCollectionId());
        System.out.printf("LED Collection     :: %d%n", leds.getCollectionId());
    }
}
