package org.hps.conditions.svt;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import org.hps.conditions.svt.SvtChannel.SvtChannelCollection;
import org.hps.conditions.svt.SvtCalibration.SvtCalibrationCollection;
import org.hps.conditions.database.DatabaseConditionsManager;

/**
 *  Handler for calibration events.
 *   
 *  @author Omar Moreno <omoreno1@ucsc.edu>
 */
class CalibrationHandler extends DefaultHandler {

    // List of SVT channels
    private SvtChannelCollection svtChannels;

    // List of SVT calibrations
    private SvtCalibrationCollection calibrations 
        = new SvtCalibrationCollection();

    // An SVT calibration object encapsulating the baseline and noise values
    // for an SVT channel
    private SvtCalibration calibration = null;
    
    String content;

    // FEB ID (0-9)
    int febID = 0;
    // Hybrid ID (0-3)
    int hybridID = 0;
    // Hybrid (0-639)
    int channel = 0;
    // Baseline sample ID (0-5)
    int baselineSampleID = 0;
    // Noise sample ID (0-5)
    int noiseSampleID = 0; 
    
    /**
     *  Default Constructor
     */
    public CalibrationHandler() {
        svtChannels = DatabaseConditionsManager.getInstance()
                .getSvtConditions().getChannelMap();
    }

    /**
     *  Method that is triggered when the start tag is encountered.
     * 
     *  @param uri : The Namespace URI.
     *  @param locaName : The local name (without prefix).
     *  @param qName : The qualified name (with prefix).
     *  @param attributes :The attributes attached to the element.  
     *  @throws Any SAX exception, possibly wrapping another exception. 
     */
    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {

        switch (qName) {
            case "Feb":
                febID = Integer.parseInt(attributes.getValue("id"));
                System.out.println("FEB ID: " + febID);
                break;
            case "Hybrid":
                hybridID = Integer.parseInt(attributes.getValue("id"));
                System.out.println("Hybrid ID: " + hybridID);
                break;
            case "channel":
                channel = Integer.parseInt(attributes.getValue("id"));
                calibration = new SvtCalibration(svtChannels.findChannelID(febID, hybridID, channel));
                break;
            case "baseline":
                baselineSampleID = Integer.parseInt(attributes.getValue("id"));
                break;
            case "noise":
                noiseSampleID = Integer.parseInt(attributes.getValue("id"));
                break;
        }
    }
   
    /**
     *  Method that is triggered when the end of a tag is encountered. 
     *
     *  @param uri : The Namespace URI.
     *  @param locaName : The local name (without prefix).
     *  @param qName : The qualified name (with prefix).
     *  @throws Any SAX exception, possibly wrapping another exception. 
     */
    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException { 
        
        switch (qName) { 
            case "channel":
                calibrations.add(calibration);
                break;
            case "baseline":
                calibration.setPedestal(baselineSampleID, Double.parseDouble(content)); 
                break;
            case "noise": 
                calibration.setNoise(baselineSampleID, Double.parseDouble(content)); 
                break;
        }
    }
   
   /**
    * Method called to extract character data inside of an element.
    * 
    * @param ch : The characters.
    * @param start : The start position in the character array.
    * @param length : The number of characters to use from the character array.
    *  @throws Any SAX exception, possibly wrapping another exception. 
    */
   @Override
   public void characters(char[] ch, int start, int length)
       throws SAXException { 
       content = String.copyValueOf(ch, start, length).trim();
   }
    
    /**
     *  Get the collection of {@link SvtCalibration}s built from parsing the
     *  XML input file.
     *  
     *   @return The collection of {@link SvtCalibration}s
     */
    public SvtCalibrationCollection getCalibrations() { 
        return calibrations;
    }
}