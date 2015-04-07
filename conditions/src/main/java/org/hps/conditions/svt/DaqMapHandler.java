package org.hps.conditions.svt;

import org.hps.conditions.svt.SvtChannel.SvtChannelCollection;
import org.hps.conditions.svt.SvtDaqMapping.SvtDaqMappingCollection;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Handler for DAQ map events.
 *
 * @author <a href="mailto:omoreno1@ucsc.edu">Omar Moreno</a>
 */
public final class DaqMapHandler extends DefaultHandler {

    /**
     * Max number of channels.
     */
    // FIXME: Probably this constant could be used from some other class.
    private static final int CHANNELS_MAX = 640;

    /**
     * The collection of DAQ map objects.
     */
    private SvtDaqMappingCollection daqMap = new SvtDaqMappingCollection();

    /**
     * The Collection of SVT channel objects.
     */
    private SvtChannelCollection svtChannels = new SvtChannelCollection();

    /**
     * An SVT DAQ map object.
     */
    private SvtDaqMapping daqMapping = null;

    /**
     * Text node inside of an XML element.
     */
    private String content;

    /**
     * Current SVT channel ID. This gets incremented every time an SvtChannel gets added to the map.
     */
    private int currentSvtChannelID = 0;

    /**
     * FEB ID (0-9).
     */
    private int febID = 0;

    /**
     * Hybrid ID (0-3).
     */
    private int hybridID = 0;

    /**
     * Default constructor.
     */
    public DaqMapHandler() {
    }

    /**
     * Method that is triggered when the start tag is encountered.
     *
     * @param uri the Namespace URI
     * @param locaName the local name (without prefix)
     * @param qName the qualified name (with prefix)
     * @param attributes the attributes attached to the element
     * @throws SAXException if there is an error processing the element
     */
    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
            throws SAXException {

        switch (qName) {
            case "Feb":
                febID = Integer.parseInt(attributes.getValue("id"));
                break;
            case "Hybrid":
                hybridID = Integer.parseInt(attributes.getValue("id"));
                daqMapping = new SvtDaqMapping(febID, hybridID);
                break;
            default:
                break;
        }
    }

    /**
     * Method that is triggered when the end of a tag is encountered.
     *
     * @param uri the Namespace URI.
     * @param locaName the local name (without prefix)
     * @param qName the qualified name (with prefix)
     * @throws SAXException if there is an error processing the element
     */
    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {

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
            default:
                break;
        }
    }

    /**
     * Method called to extract character data inside of an element.
     *
     * @param ch the characters
     * @param start the start position in the character array
     * @param length the number of characters to use from the character array
     * @throws SAXException if there is an error processing the element
     */
    @Override
    public void characters(final char[] ch, final int start, final int length) throws SAXException {
        content = String.copyValueOf(ch, start, length).trim();
    }

    /**
     * Add a set of {@link SvtChannel} objects to the {@link SvtChannelCollection} for each of the hybrids. A total of
     * 639 channels are added per hybrid.
     *
     * @param febID the Front End Board (FEB) ID
     * @param febHybridID the FEB hybrid ID
     */
    public void addSvtChannels(final int febID, final int febHybridID) {
        for (int channel = 0; channel < CHANNELS_MAX; channel++) {
            this.svtChannels.add(new SvtChannel(this.currentSvtChannelID, this.febID, this.hybridID, channel));
            this.currentSvtChannelID++;
        }
    }

    /**
     * Get the {@link SvtDaqMappingCollection} built from parsing the XML input file.
     *
     * @return the {@link SvtDaqMappingCollection} from parsing the XML
     */
    public SvtDaqMappingCollection getDaqMap() {
        return daqMap;
    }

    /**
     * Get the {@link SvtChannelCollection} build from parsing the XML input file.
     *
     * @return the {@link SvtChannelCollection} from parsing the XML
     */
    public SvtChannelCollection getSvtChannels() {
        return svtChannels;
    }

}
