package org.hps.conditions.svt;

import java.io.File;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.hps.conditions.svt.CalibrationHandler;
import org.hps.conditions.svt.SvtCalibration.SvtCalibrationCollection;
import org.hps.conditions.svt.SvtDaqMapping.SvtDaqMappingCollection;

/**
 *  Reader used to parse SVT conditions.
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
public class SvtConditionsReader {

    SAXParserFactory parserFactory = SAXParserFactory.newInstance();
    SAXParser parser; 
    
    /**
     * Default Constructor
     * 
     * @throws Exception if a SAX parser can't be created.
     */
    public SvtConditionsReader() throws Exception { 
        
        // Create a new SAX parser 
        parser = parserFactory.newSAXParser();
    }
   
    /**
     *  Parse a calibration file and create {@link SvtCalibration} objects out
     *  of all channel conditions.
     *  
     *  @param calibrationFile : The input calibration file to parse
     *  @return A collection of SvtCalibration objects
     * 
     */
    SvtCalibrationCollection parseCalibrations(File calibrationFile) throws Exception {

        // Instantiate the calibration handler
        CalibrationHandler febHandler = new CalibrationHandler();
        
        // Parse the calibration file and create the collection of SvtCalibrations
        parser.parse(calibrationFile, febHandler);

        // Return the collection of SvtCalibrations
        return febHandler.getCalibrations();
    }
    
    /**
     *  Parse a DAQ map file and create {@link SvtDaqMapping} objects
     *  
     *  @param daqMapFile : The input DAQ map file to parse
     *  @return A collection of SvtDaqMappig objects
     * 
     */
    SvtDaqMappingCollection parseDaqMap(File daqMapFile) throws Exception { 
      
        // Instantiate the DAQ map handler
        DaqMapHandler daqMapHandler = new DaqMapHandler();
        
        // Parse the DAQ map file and create the collection of SvtDaqMapping objects
        parser.parse(daqMapFile, daqMapHandler);
        
        // Return the collection of SvtDaqMapping
        return daqMapHandler.getDaqMap(); 
    }

}
