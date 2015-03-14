package org.hps.conditions.svt;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import org.hps.conditions.svt.SvtChannel.SvtChannelCollection;
import org.hps.conditions.svt.SvtDaqMapping.SvtDaqMappingCollection;

/**
 *  Handler for DAQ map events.
 * 
 *  @author Omar Moreno <omoreno1@ucsc.edu>
 */
public class DaqMapHandler extends DefaultHandler {
    
    // Collection of DAQ map objects
    private SvtDaqMappingCollection daqMap = new SvtDaqMappingCollection();
    
    // Collection of SVT channel objects
    private SvtChannelCollection svtChannels = new SvtChannelCollection(); 
 
    // An SVT DAQ map object 
    private SvtDaqMapping daqMapping = null;
   
    // Text node inside of an XML element
    String content;
    
    // Current SVT channel ID.  This gets incremented every time an SvtChannel
    // gets added to the map.
    int currentSvtChannelID = 0; 
    
    
    // FEB ID (0-9)
    int febID = 0;
    // Hybrid ID (0-3)
    int hybridID = 0;
    
    /**
     *  Default Constructor
     */
    public DaqMapHandler() { 
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
                break;
            case "Hybrid":
                hybridID = Integer.parseInt(attributes.getValue("id"));
                daqMapping = new SvtDaqMapping(febID, hybridID); 
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
            case "Hybrid":
                daqMap.add(daqMapping);
                this.addSvtChannels(febID, hybridID);
                break; 
            case "Half":
                daqMapping.setSvtHalf(content);
                break;
            case "Layer": 
                daqMapping.setLayerNumber(Integer.parseInt(content));
                break;
            case "Side": 
                daqMapping.setSide(content);
                break;
            case "Orientation":
                daqMapping.setOrientation(content);
                break;
        }
    }
    
   /**
    * Method called to extract character data inside of an element.
    * 
    * @param ch : The characters.
    * @param start : The start position in the character array.
    * @param length : The number of characters to use from the character array.
    * @throws Any SAX exception, possibly wrapping another exception. 
    */
   @Override
   public void characters(char[] ch, int start, int length)
       throws SAXException { 
       content = String.copyValueOf(ch, start, length).trim();
   }
   
   /**
    *   Add a set of {@link SvtChannel}s to the {@link SvtChannelCollection} 
    *   for each of the hybrids.  A total of 639 channels are added per hybrid.
    *   
    *   @param febID : The Front End Board (FEB) ID 
    *   @param febHybridID : The FEB hybrid ID
    *   
    */
   public void addSvtChannels(int febID, int febHybridID) { 
       for (int channel = 0; channel < 640; channel++) { 
           this.svtChannels.add(new SvtChannel(
                   this.currentSvtChannelID, this.febID, this.hybridID, channel));
           this.currentSvtChannelID++;
       }
   }
   
    /**
     *  Get the {@link SvtDaqMappingCollection} built from parsing the XML 
     *  input file.
     *  
     *   @return The collection of {@link SvtDaqMappingCollection}s
     */
    public SvtDaqMappingCollection getDaqMap() { 
        return daqMap;
    }
    
    /**
     *  Get the {@link SvtChannelCollection} build from parsing the XML input
     *  file.
     * 
     */
    public SvtChannelCollection getSvtChannels() { 
        return svtChannels; 
    }

}
