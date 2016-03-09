package org.hps.record.daqconfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;

/**
 * Class <code>DAQConfigDriver</code> is responsible for accessing the
 * DAQ configuration settings, and then passing them to the associated
 * class <code>ConfigurationManager</code> so that they can be accessed
 * by other classes.<br/>
 * <br/>
 * The driver may accomplish this by two means. By default, it will
 * check each event for an <code>EvioDAQParser</code> object which
 * contains all of the DAQ configuration information and pass this to
 * the <code>ConfigurationManager</code>. It will continue to update
 * the <code>ConfigurationManager</code> during the run if new parser
 * objects appear, and can thusly account for changing DAQ conditions.
 * <br/><br/>
 * The driver may also be set to read a DAQ configuration from text
 * files containing the DAQ configuration bank information. To enable
 * this mode, the parameter <code>readDataFiles</code> must be set to
 * <code>true</code> and the parameters <code>runNumber</code> and also
 * <code>filepath</code> must be defined. <code>runNumber</code> defines
 * the run number of the configuration to be loaded and the parameter
 * <code>filepath</code> defines the location of the data file repository.
 * <br/><br/>
 * This driver must be included in the driver chain if any other drivers
 * in the chain rely on <code>ConfigurationManager</code>, as it can
 * not be initialized otherwise.
 * 
 * @author Kyle McCarty
 * @see ConfigurationManager
 */
public class DAQConfigDriver extends Driver {
    private int runNumber = -1;
    private String filepath = null;
    private boolean firstEvent = true;
    private boolean readDataFiles = false;
    private File[] dataFiles = new File[3];
    private int[] crateNumber = { 46, 37, 39 };
    
    /**
     * Verifies the parameter <code>filepath</code> for the data file
     * repository and checks that appropriate data files exist for the
     * requested run number if the driver is set to read from data files.
     * Otherwise, this does nothing.
     */
    @Override
    public void startOfData() {
        // Check whether to use stored data files or the EvIO data stream
        // as the source of the DAQ settings. Nothing needs to be done
        // in the latter case.
        if(readDataFiles) {
            // The user must define a data file prefix and repository
            // location for this option to be used.
            if(filepath == null) {
                throw new NullPointerException("DAQ settings repository filepath must be defined.");
            } if(runNumber == -1) {
                throw new NullPointerException("Run number must be defined.");
            }
            
            // Verify that the repository actually exist.
            File repository = new File(filepath);
            if(!repository.exists() || !repository.isDirectory()) {
                throw new IllegalArgumentException("Repository location \"" + filepath + "\" must be an existing directory.");
            }
            
            // Define the data file objects.
            for(int i = 0; i < dataFiles.length; i++) {
                try {
                    dataFiles[i] = new File(repository.getCanonicalPath() + "/" + runNumber + "_" + crateNumber[i] + ".txt");
                } catch(IOException e) {
                    throw new RuntimeException("Error resolving absolute repository filepath.");
                }
            }
            
            // Verify that the data files actually exist.
            for(File dataFile : dataFiles) {
                if(!dataFile.exists() || !dataFile.canRead()) {
                    throw new IllegalArgumentException("Data file \"" + dataFile.getName() + "\" does not exist or can not be read.");
                }
            }
        }
    }
    
    /**
     * Checks an event for the DAQ configuration banks and passes them
     * to the <code>ConfigurationManager</code> if the driver is set to
     * read from the EvIO data stream. Otherwise, this will parse the
     * data files on the first event and then do nothing.
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
            EvioDAQParser daqConfig = new EvioDAQParser();
            for(int i = 0; i < dataFiles.length; i++) {
                daqConfig.parse(crateNumber[i], runNumber, data[i]);
            }
            
            // Update the configuration manager.
            ConfigurationManager.updateConfiguration(daqConfig);
        }
        
        // Check if a trigger configuration bank exists.
        if(!readDataFiles && event.hasCollection(EvioDAQParser.class, "TriggerConfig")) {
            // Get the trigger configuration bank. There should only be
            // one in the list.
            List<EvioDAQParser> configList = event.get(EvioDAQParser.class, "TriggerConfig");
            EvioDAQParser daqConfig = configList.get(0);
            
            // Get the DAQ configuration and update it with the new
            // configuration object.
            ConfigurationManager.updateConfiguration(daqConfig);
        }
        
        // Note that it is no longer the first event.
        firstEvent = false;
    }
    
    /**
     * Converts DAQ configuration data files into an array of strings
     * where each array entry represents a line in the configuration
     * file. The first array index of the returned object corresponds
     * to the file, and the second array index corresponds to the line.
     * @param dataFiles - An array of <code>File</code> objects pointing
     * to the data files that are to be converted. These are expected
     * to be plain text files.
     * @return Returns a two-dimensional array of <code>String</code>
     * objects where the first array index corresponds to the object
     * of the same index in the <code>File</code> array and the second
     * array index corresponds to the lines in the file referenced by
     * the <code>File</code> object.
     * @throws IOException Occurs if there is an issue with accessing
     * or reading the objects in the objects referred to by the files
     * pointed to in the <code>dataFiles</code> array.
     */
    private static final String[][] getDataFileArrays(File[] dataFiles) throws IOException {
        // Create file readers to process the data files.
        FileReader[] fr = new FileReader[dataFiles.length];
        BufferedReader[] reader = new BufferedReader[dataFiles.length];
        for(int i = 0; i < dataFiles.length; i++) {
            fr[i] = new FileReader(dataFiles[i]);
            reader[i] = new BufferedReader(fr[i]);
        }
        
        // Generate String arrays where each entry in the array is
        // a line from the data file.
        String[][] data = new String[dataFiles.length][0];
        for(int i = 0; i < dataFiles.length; i++) {
            // Create a list to hold the raw strings.
            List<String> rawData = new ArrayList<String>();
            
            // Add each line from the current data file to the list
            // as a single entry.
            String curLine = null;
            while((curLine = reader[i].readLine()) != null) {
                rawData.add(curLine);
            }
            
            // Convert the list into a String array.
            data[i] = rawData.toArray(new String[rawData.size()]);
        }
        
        // Return the data array.
        return data;
    }
    
    /**
     * Sets the run number of the DAQ configuration being processed.
     * This is only used when reading from data files.
     * @param run - The run number of the data files to be used.
     */
    public void setRunNumber(int run) {
        runNumber = run;
    }
    
    /**
     * Sets the location of the DAQ configuration data files. This is
     * only used when reading from the data files.
     * @param filepath - The file path of the data file repository.
     */
    public void setDataFileRepository(String filepath) {
        this.filepath = filepath;
    }
    
    /**
     * Sets whether or not to read the DAQ configuration directly from
     * the EvIO data stream or whether to read the configuration from
     * data files. Parameters <code>runNumber</code> and <code>filepath</code>
     * must also be defined if this is set to <code>true</code>.
     * @param state - <code>true</code> indicates that the configuration
     * should be read from data files, and <code>false</code> that it
     * should be read from the EvIO stream.
     */
    public void setReadDataFiles(boolean state) {
        readDataFiles = state;
    }
}