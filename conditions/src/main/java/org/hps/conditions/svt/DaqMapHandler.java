package org.hps.conditions.svt;

import org.hps.conditions.api.ConditionsObjectException;
import org.hps.conditions.svt.SvtChannel.SvtChannelCollection;
import org.hps.conditions.svt.SvtDaqMapping.SvtDaqMappingCollection;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Handler for DAQ map events.
 *
 * @author Omar Moreno, UCSC
 */
public final class DaqMapHandler extends DefaultHandler {

    /**
     * Max number of channels.
     */
    // FIXME: Probably this constant could be used from some other class.
    private static final int CHANNELS_MAX = 640;

    /**
     * Text node inside of an XML element.
     */
    private String content;

    /**
     * Current SVT channel ID. This gets incremented every time an SvtChannel gets added to the map.
     */
    private int currentSvtChannelID = 0;

    /**
     * The collection of DAQ map objects.
     */
    private final SvtDaqMappingCollection daqMap = new SvtDaqMappingCollection();

    /**
     * An SVT DAQ map object.
     */
    private SvtDaqMapping daqMapping = null;

    /**
     * FEB ID (0-9).
     */
    private int febID = 0;

    /**
     * Hybrid ID (0-3).
     */
    private int hybridID = 0;

    /**
     * The Collection of SVT channel objects.
     */
    private final SvtChannelCollection svtChannels = new SvtChannelCollection();

    /**
     * Default constructor.
     */
    public DaqMapHandler() {
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
            try {
                this.svtChannels.add(new SvtChannel(this.currentSvtChannelID, this.febID, this.hybridID, channel));
            } catch (final ConditionsObjectException e) {
                throw new RuntimeException(e);
            }
            this.currentSvtChannelID++;
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
        this.content = String.copyValueOf(ch, start, length).trim();
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
                try {
                    this.daqMap.add(this.daqMapping);
                } catch (final ConditionsObjectException e) {
                    throw new RuntimeException(e);
                }
                this.addSvtChannels(this.febID, this.hybridID);
                break;
            case "Half":
                this.daqMapping.setSvtHalf(this.content);
                break;
            case "Layer":
                this.daqMapping.setLayerNumber(Integer.parseInt(this.content));
                break;
            case "Side":
                this.daqMapping.setSide(this.content);
                break;
            case "Orientation":
                this.daqMapping.setOrientation(this.content);
                break;
            default:
                break;
        }
    }

    /**
     * Get the {@link SvtDaqMappingCollection} built from parsing the XML input file.
     *
     * @return the {@link SvtDaqMappingCollection} from parsing the XML
     */
    public SvtDaqMappingCollection getDaqMap() {
        return this.daqMap;
    }

    /**
     * Get the {@link SvtChannelCollection} build from parsing the XML input file.
     *
     * @return the {@link SvtChannelCollection} from parsing the XML
     */
    public SvtChannelCollection getSvtChannels() {
        return this.svtChannels;
    }

    /**
     * Method that is triggered when the start tag is encountered.
     *
     * @param uri the Namespace URI
     * @param localName the local name (without prefix)
     * @param qName the qualified name (with prefix)
     * @param attributes the attributes attached to the element
     * @throws SAXException if there is an error processing the element
     */
    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
            throws SAXException {

        switch (qName) {
            case "Feb":
                this.febID = Integer.parseInt(attributes.getValue("id"));
                break;
            case "Hybrid":
                this.hybridID = Integer.parseInt(attributes.getValue("id"));
                this.daqMapping = new SvtDaqMapping(this.febID, this.hybridID);
                break;
            default:
                break;
        }
    }

}
