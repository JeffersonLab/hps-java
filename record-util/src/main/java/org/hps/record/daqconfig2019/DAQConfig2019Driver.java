package org.hps.record.daqconfig2019;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;

/**
 * Class <code>DAQConfig2019Driver</code> is responsible for accessing the 2019
 * DAQ configuration settings, and then passing them to the associated class
 * <code>ConfigurationManager2019</code> so that they can be accessed by other
 * classes.<br/>
 * <br/>
 * The driver may accomplish this by two means. By default, it will check each
 * event for an <code>EvioDAQParser2019</code> object which contains all of the
 * DAQ configuration information and pass this to the
 * <code>ConfigurationManager2019</code>. It will continue to update the
 * <code>ConfigurationManager2019</code> during the run if new parser objects
 * appear, and can thusly account for changing DAQ conditions. <br/>
 * <br/>
 * The driver may also be set to read a DAQ configuration from text files
 * containing the DAQ configuration bank information. The text files are located
 * in resources. To enable this mode, the parameter
 * <code>daqConfigurationAppliedintoReadout</code> need to be set
 * <code>true</code>, or the parameter <code>readDataFiles</code> need to be set
 * to <code>true</code>. <code>runNumber</code> defines the run number of the
 * configuration to be loaded. <br/>
 * <br/>
 * This driver must be included in the driver chain if any other drivers in the
 * chain rely on <code>ConfigurationManager2019</code>, as it can not be
 * initialized otherwise.
 * 
 * Code is developed referring to org.hps.record.daqconfig.DAQConfigDriver by
 * Kyle McCarty
 *
 * @see ConfigurationManager2019
 */
public class DAQConfig2019Driver extends Driver {
    private int runNumber = -1;
    private boolean readDataFiles = false;
    private boolean firstEvent = true;
    private InputStream[] dataFiles = new InputStream[3];
    private int[] crateNumber = { 37, 39, 11};
    private String daqVersion = null;
    
    /**
     * Indicates whether the DAQ configuration is applied in the readout system.
     */
    private boolean daqConfigurationAppliedintoReadout = false;

    /**
     * Verifies the parameter <code>filepath</code> for the data file repository and
     * checks that appropriate data files exist for the requested run number if the
     * driver is set to read from data files. Otherwise, this does nothing.
     */
    @Override
    public void startOfData() {
        if(runNumber == -1) runNumber = this.getConditionsManager().getRun(); 
        daqVersion = mapBetweenRunNumberDAQVersion(runNumber);
        
        // Check whether to apply the DAQ configuration into the readout system.
        if (daqConfigurationAppliedintoReadout) {
            // Define the data file objects.
            for (int i = 0; i < dataFiles.length; i++) {
                dataFiles[i] = DAQConfig2019Driver.class.getResourceAsStream(daqVersion + "_" + crateNumber[i] + ".txt");
                if (dataFiles[i] == null) {
                    if(runNumber == 1194550 || System.getProperties().containsKey("defaultDAQVersion2019") == true) 
                        dataFiles[i] = DAQConfig2019Driver.class.getResourceAsStream("hps_v12_1" + "_" + crateNumber[i] + ".txt");
                    else if(runNumber == 1193700 || System.getProperties().containsKey("defaultDAQVersion2021") == true) 
                        dataFiles[i] = DAQConfig2019Driver.class.getResourceAsStream("hps2021_v2_3" + "_" + crateNumber[i] + ".txt");
                    else if(runNumber == 1191920 || System.getProperties().containsKey("defaultDAQVersion20211920") == true) 
                        dataFiles[i] = DAQConfig2019Driver.class.getResourceAsStream("hps_1.9_v2_6" + "_" + crateNumber[i] + ".txt");
                    else throw new RuntimeException("No corresponding DAQ configuration file for run " + String.valueOf(runNumber) + " in hps-java/record-util/src/main/resources/org/hps/record/daqconfig2019.\n"
                            + " Please change run number or set the system property -DdefaultDAQVersion2019 to apply hps_v12_1, or -DdefaultDAQVersion2021 to apply hps2021_v2_3, or -DdefaultDAQVersion20211920 to apply hps_1.9_v2_6.");
                }
            }
            
            // If this is the first event and data files are to be read,
            // import the data files and generate the DAQ information.
            // Get the data files in the form of a data array.
            String[][] data;
            try {
                data = getDataFileArrays(dataFiles);
            } catch (IOException e) {
                throw new RuntimeException("An error occurred when processing the data files.");
            }

            // Instantiate an EvIO DAQ parser and feed it the data.
            EvioDAQParser2019 daqConfig = new EvioDAQParser2019();
            for (int i = 0; i < dataFiles.length; i++) {
                daqConfig.parse(crateNumber[i], runNumber, data[i]);
            }

            // Update the configuration manager.
            ConfigurationManager2019.updateConfiguration(daqConfig);
        }
        
        // Check whether to use data files stored in resources and apply the DAQ configuration into the trigger diagnostics 
        if(readDataFiles) {
            // Define the data file objects.
            for (int i = 0; i < dataFiles.length; i++) {
                dataFiles[i] = DAQConfig2019Driver.class.getResourceAsStream(daqVersion + "_" + crateNumber[i] + ".txt");                
                if (dataFiles[i] == null) {
                    if(runNumber == 1194550 || System.getProperties().containsKey("defaultDAQVersion2019") == true) 
                        dataFiles[i] = DAQConfig2019Driver.class.getResourceAsStream("hps_v12_1" + "_" + crateNumber[i] + ".txt");
                    else if(runNumber == 1193700 || System.getProperties().containsKey("defaultDAQVersion2021") == true) 
                        dataFiles[i] = DAQConfig2019Driver.class.getResourceAsStream("hps2021_v2_3" + "_" + crateNumber[i] + ".txt");
                    else if(runNumber == 1191920 || System.getProperties().containsKey("defaultDAQVersion20211920") == true) 
                        dataFiles[i] = DAQConfig2019Driver.class.getResourceAsStream("hps_1.9_v2_6" + "_" + crateNumber[i] + ".txt");
                    else throw new RuntimeException("No corresponding DAQ configuration file for run " + String.valueOf(runNumber) + " in hps-java/record-util/src/main/resources/org/hps/record/daqconfig2019.\n"
                            + " Please change run number or set the system property -DdefaultDAQVersion2019 to apply hps_v12_1, or -DdefaultDAQVersion2021 to apply hps2021_v2_3, or -DdefaultDAQVersion20211920 to apply hps_1.9_v2_6.");
                }
            }
        }
    }

    /**
     * Checks an event for the DAQ configuration banks and passes them to the
     * <code>ConfigurationManager2019</code> if the driver is set to read from the
     * EvIO data stream. Otherwise, this will parse the data files on the first
     * event and then do nothing.
     * 
     * @param event - The current LCIO event.
     */
    @Override
    public void process(EventHeader event) {       
        // If this is the first event and data files are to be read,
        // import the data files and generate the DAQ information.
        if(firstEvent && readDataFiles) {
            // Get the data files in the form of a data array.
            String[][] data;
            try { data = getDataFileArrays(dataFiles); }
            catch(IOException e) {
                throw new RuntimeException("An error occurred when processing the data files.");
            }
            
            // Instantiate an EvIO DAQ parser and feed it the data.
            EvioDAQParser2019 daqConfig = new EvioDAQParser2019();
            for(int i = 0; i < dataFiles.length; i++) {
                daqConfig.parse(crateNumber[i], runNumber, data[i]);
            }
            
            // Update the configuration manager.
            ConfigurationManager2019.updateConfiguration(daqConfig);
            
            // Note that it is no longer the first event.
            firstEvent = false;
        }
        
        // Check if a trigger configuration bank exists.
        if (!readDataFiles && event.hasCollection(EvioDAQParser2019.class, "TriggerConfig")) {
            // Get the trigger configuration bank. There should only be
            // one in the list.
            List<EvioDAQParser2019> configList = event.get(EvioDAQParser2019.class, "TriggerConfig");
            EvioDAQParser2019 daqConfig = configList.get(0);

            // Get the DAQ configuration and update it with the new
            // configuration object.
            ConfigurationManager2019.updateConfiguration(daqConfig);
        }        
    }

    /**
     * Converts DAQ configuration data files into an array of strings where each
     * array entry represents a line in the configuration file. The first array
     * index of the returned object corresponds to the file, and the second array
     * index corresponds to the line.
     * 
     * @param dataFiles - An array of <code>File</code> objects pointing to the data
     *                  files that are to be converted. These are expected to be
     *                  plain text files.
     * @return Returns a two-dimensional array of <code>String</code> objects where
     *         the first array index corresponds to the object of the same index in
     *         the <code>File</code> array and the second array index corresponds to
     *         the lines in the file referenced by the <code>File</code> object.
     * @throws IOException Occurs if there is an issue with accessing or reading the
     *                     objects in the objects referred to by the files pointed
     *                     to in the <code>dataFiles</code> array.
     */
    private static final String[][] getDataFileArrays(InputStream[] dataFiles) throws IOException {
        // Create file readers to process the data files.
        InputStreamReader[] fr = new InputStreamReader[dataFiles.length];
        BufferedReader[] reader = new BufferedReader[dataFiles.length];
        for (int i = 0; i < dataFiles.length; i++) {
            fr[i] = new InputStreamReader(dataFiles[i]);
            reader[i] = new BufferedReader(fr[i]);
        }

        // Convert the reader streams into line-delimited strings.
        String[][] data = getDataFileArrays(reader);

        // Close the readers.
        for (int i = 0; i < dataFiles.length; i++) {
            reader[i].close();
            fr[i].close();
        }

        // Return the data array.
        return data;
    }

    /**
     * Converts DAQ configuration data streams into an array of strings where each
     * array entry represents a line in the configuration file. The first array
     * index of the returned object corresponds to the stream, and the second array
     * index corresponds to the line.
     * 
     * @param reader - An array of <code>BufferedReader</code> objects containing
     *               DAQ trigger configuration crate data.
     * @return Returns a two-dimensional array of <code>String</code> objects where
     *         the first array index corresponds to the object of the same index in
     *         the <code>BufferedReader</code> array and the second array index
     *         corresponds to the lines in the stream referenced by the
     *         <code>BufferedReader</code> object.
     * @throws IOException Occurs if there is an issue with reading data stream.
     */
    protected static final String[][] getDataFileArrays(BufferedReader[] reader) throws IOException {
        // Generate String arrays where each entry in the array is
        // a line from the data file.
        String[][] data = new String[reader.length][0];
        for (int i = 0; i < reader.length; i++) {
            // Create a list to hold the raw strings.
            List<String> rawData = new ArrayList<String>();

            // Add each line from the current data file to the list
            // as a single entry.
            String curLine = null;
            while ((curLine = reader[i].readLine()) != null) {
                rawData.add(curLine);
            }

            // Convert the list into a String array.
            data[i] = rawData.toArray(new String[rawData.size()]);
        }

        // Return the data array.
        return data;
    }

    /**
     * Gets the run number that the DAQConfigDriver is set to use. This will be
     * <code>-1</code> in the event that the driver reads from an EvIO file.
     * 
     * @return Returns the run number as an <code>int</code> primitive. Will return
     *         <code>-1</code> if the driver is set to read from an EvIO file.
     */
    protected final int getRunNumber() {
        return runNumber;
    }

    /**
     * Sets the run number of the DAQ configuration being processed. This is only
     * used when reading from data files.
     * 
     * @param run - The run number of the data files to be used.
     */
    public void setRunNumber(int run) {
        runNumber = run;
    }    

    /**
     * Sets whether or not to read the DAQ configuration directly from the EvIO data
     * stream or whether to read the configuration from data files.
     * 
     * @param state - <code>true</code> indicates that the configuration should be
     * read from data files, and <code>false</code> that it should be
     * read from the EvIO stream.
     */
    public void setReadDataFiles(boolean state) {
        readDataFiles = state;
    }

    /**
     * Sets whether or not the DAQ configuration is applied into the readout system.
     * 
     * @param state - <code>true</code> indicates that the DAQ configuration is
     * applied into the readout system, and <code>false</code> that it
     * is not applied into the readout system.
     */
    public void setDaqConfigurationAppliedintoReadout(boolean state) {
        daqConfigurationAppliedintoReadout = state;
    }
    
    /**
     * According to run number, a specified DAQ version name is returned.
     * @param runNumber
     * @return name of a DAQ configuration version
     */
    private String mapBetweenRunNumberDAQVersion(int runNumber) {
        
        // 2019 experiment
        if(runNumber == 9920 || runNumber == 9921)
            return "hps_FEE";
        else if((runNumber >= 10010 && runNumber <= 10022) || (runNumber >= 10028 && runNumber <= 10038)
                || (runNumber >= 10045 && runNumber <= 10048)) 
            return "hps_v7";                
        else if(runNumber >= 10049 && runNumber <= 10080) 
            return "hps_v8";        
        else if((runNumber >= 10081 && runNumber <= 10093) || (runNumber >= 10105 && runNumber <= 10115)) 
            return "hps_v9";        
        else if(runNumber == 10097 || runNumber == 10103) 
            return "hps_v6_FEE";   
        else if(runNumber == 10104) 
            return "hps_v6_FEE_1";  
        else if((runNumber >= 10095 && runNumber <= 10096) || runNumber == 10102 || (runNumber >= 10118 && runNumber <= 10120)) 
            return "hps_v9_1";        
        else if(runNumber >= 10121 && runNumber <= 10149) 
            return "hps_v9_2";        
        else if((runNumber >= 10116 && runNumber <= 10117) || (runNumber >= 10153 && runNumber <= 10155)
                || (runNumber >= 10161 && runNumber <= 10172) || (runNumber >= 10185 && runNumber <= 10290)
                || (runNumber >= 10298 && runNumber <= 10331) || (runNumber >= 10341 && runNumber <= 10343)
                || (runNumber >= 10361 && runNumber <= 10398)) 
            return "hps_v10";     
        else if(runNumber == 10156) 
            return "hps_v10_random";  
        else if(runNumber >= 10400 && runNumber <= 10418) 
            return "hps_v11_1";            
        else if((runNumber >= 10419 && runNumber <= 10442) || (runNumber >= 10444 && runNumber <= 10495)
                || (runNumber >= 10653 && runNumber <= 10654) || (runNumber == 10530) || (runNumber == 10545)) 
            return "hps_v11_5";        
        else if((runNumber >= 9977 && runNumber <= 9979) || (runNumber >= 10023 && runNumber <= 10027)
                || (runNumber == 10291) || (runNumber == 10443))
            return "hps_v6_random";
        else if((runNumber >= 10496 && runNumber <= 10529) || (runNumber >= 10531 && runNumber <= 10543)
                || (runNumber >= 10547 && runNumber <= 10630))
            return "hps_v11_6";        
        else if(runNumber == 10645 || runNumber == 10646 || runNumber == 10700) 
            return "hps_v11_5_random";                
        else if((runNumber >= 10637 && runNumber <= 10644) || (runNumber <= 10647 && runNumber >= 10651) 
                || (runNumber >= 10655 && runNumber <= 10660) || (runNumber >= 10666 && runNumber <= 10667)
                || (runNumber >= 10676 && runNumber <= 10681) || (runNumber >= 10683 && runNumber <= 10699)
                || (runNumber >= 10702 && runNumber <= 10704) || (runNumber >= 10709 && runNumber <= 10714)
                || (runNumber >= 10719 && runNumber <= 10731) || (runNumber >= 10735 && runNumber <= 10740))
            return "hps_v12_1";   
        else if(runNumber >= 10716 && runNumber <= 10718)
            return "hps_v13_FEE";
        
        // 2021 experiment; 3.7 GeV
        else if(runNumber == 14161 || (runNumber >= 14166 && runNumber <= 14180))
            return "hps2021_v1_2";
        
        else if(runNumber == 14163)
            return "hps2021_v1_2_FEE";
        
        else if((runNumber >= 14184 && runNumber <= 14262))
            return "hps_v2021_v2_0";
        
        else if(runNumber == 14266)
            return "hps2021_NOSINGLES2_v2_2";
        
        else if((runNumber >= 14268 && runNumber <= 14272) || (runNumber >= 14277 && runNumber <= 14332))
            return "hps_v2021_v2_2";
        
        else if(runNumber == 14273)
            return "hps2021_v2_2_moller_only";
        
        else if(runNumber == 14275 || runNumber == 14370 || runNumber == 14371 || runNumber == 14503 || (runNumber >= 14586 && runNumber <= 14590))
            return "hps2021_v2_2_30kHz_random";
        
        else if(runNumber == 14362 || runNumber == 14364 || runNumber == 14502 || runNumber == 14591 || runNumber == 14592)
            return "hps2021_v2_2_moller_LowLumi";  
        
        else if(runNumber == 14753 || runNumber == 14754)
            return "hps2021_v2_3_SVT_WIRE_RUN";
        
        else if(runNumber >= 14762 && runNumber <= 14764)
            return "hps2021_FEE_straight_v2_2";
        
        else if(runNumber == 14767 || runNumber == 14768)
            return "hps2021_FEE_straight_v2_4";
        
        else if((runNumber >= 14334 && runNumber <= 14360) || (runNumber >= 14367 && runNumber <= 14369)
                || (runNumber >= 14372 && runNumber <= 14391) || (runNumber >= 14394 && runNumber <= 14501)
                || (runNumber >= 14504 && runNumber <= 14585) || (runNumber >= 14594 && runNumber <= 14621)
                || (runNumber >= 14684 && runNumber <= 14722) || (runNumber >= 14726 && runNumber <= 14751)
                || (runNumber >= 14756 && runNumber <= 14757) || (runNumber >= 14770 && runNumber <= 14772))
            return "hps2021_v2_3";
        
        // 2021 experiment; 1.92 GeV        
        else if(runNumber >= 14628 && runNumber <= 14630)
            return "hps_1.9_v2_4";
        
        else if(runNumber == 14633 || runNumber == 14634)
            return "hps_1.9_v2_5";
        
        else if((runNumber >= 14636 && runNumber <= 14650) || (runNumber >= 14654 && runNumber <= 14673) )
            return "hps_1.9_v2_6";
        
        else if(runNumber == 14652 || runNumber == 14653)
            return "hps_1.9_Moller_v2_6";
        
        
        else return "none";
        
    }
}
