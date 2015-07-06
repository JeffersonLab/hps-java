package org.hps.conditions.svt;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.api.ConditionsObjectException;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtCalibration.SvtCalibrationCollection;
import org.hps.conditions.svt.SvtChannel.SvtChannelCollection;
import org.lcsim.util.log.DefaultLogFormatter;
import org.lcsim.util.log.LogUtil;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Handler for calibration events.
 *
 * @author Omar Moreno, UCSC
 */
class CalibrationHandler extends DefaultHandler {

    /**
     * Initialize the logger.
     */
    private static Logger logger = LogUtil.create(SvtConditionsLoader.class.getSimpleName(), new DefaultLogFormatter(),
            Level.INFO);

    /**
     * Baseline sample ID (0-5).
     */
    private int baselineSampleID = 0;

    /**
     * An SVT calibration object encapsulating the baseline and noise values for a channel.
     */
    private SvtCalibration calibration = null;

    /**
     * List of SVT calibrations.
     */
    private final SvtCalibrationCollection calibrations = new SvtCalibrationCollection();

    /**
     * Channel number (0-639).
     */
    private int channel = 0;

    /**
     * The string content from parsing an XML calibration.
     */
    private String content;

    /**
     * The FEB ID (0-9).
     */
    private int febID = 0;

    /**
     * The Hybrid ID (0-3).
     */
    private int hybridID = 0;

    /**
     * Flag denoting whether the calibrations of a given channel should be loaded into the conditions DB. If a channel
     * is found to be missing baseline or noise values, is will be marked invalid.
     */
    private boolean isValidChannel = false;

    /**
     * Noise sample ID (0-5).
     */
    // FIXME: This variable is unused.
    private int noiseSampleID = 0;

    /**
     * List of SVT channels.
     */
    private final SvtChannelCollection svtChannels;

    /**
     * Default constructor.
     */
    public CalibrationHandler() {
        this.svtChannels = DatabaseConditionsManager.getInstance()
                .getCachedConditions(SvtChannelCollection.class, "svt_channels").getCachedData();
    }

    /**
     * Method called to extract character data inside of an element.
     *
     * @param ch the characters
     * @param start the start position in the character array
     * @param length the number of characters to use from the character array
     * @throws SAXException if there is an error processing the element (possibly wraps another exception type)
     */
    @Override
    public void characters(final char[] ch, final int start, final int length) throws SAXException {
        this.content = String.copyValueOf(ch, start, length).trim();
    }

    /**
     * Method that is triggered when the end of a tag is encountered.
     *
     * @param uri the Namespace URI
     * @param locaName the local name (without prefix)
     * @param qName the qualified name (with prefix)
     * @throws SAXException if there is an error processing the element
     */
    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {

        switch (qName) {
            case "channel":
                if (this.isValidChannel) {
                    try {
                        this.calibrations.add(this.calibration);
                    } catch (final ConditionsObjectException e) {
                        throw new RuntimeException(e);
                    }
                }
                break;
            case "baseline":
                this.calibration.setPedestal(this.baselineSampleID, Double.parseDouble(this.content));
                this.isValidChannel = true;
                break;
            case "noise":
                this.calibration.setNoise(this.baselineSampleID, Double.parseDouble(this.content));
                this.isValidChannel = true;
                break;
            default:
                break;
        }
    }

    /**
     * Get the {@link SvtCalibrationCollection} created from parsing the XML input file.
     *
     * @return the {@link SvtCalibrationCollection} created from parsing the XML
     */
    public SvtCalibrationCollection getCalibrations() {
        return this.calibrations;
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
                this.febID = Integer.parseInt(attributes.getValue("id"));
                break;
            case "Hybrid":
                this.hybridID = Integer.parseInt(attributes.getValue("id"));
                logger.info("Processing calibrations for FEB " + this.febID + " Hybrid " + this.hybridID);
                break;
            case "channel":
                this.channel = Integer.parseInt(attributes.getValue("id"));
                this.calibration = new SvtCalibration(this.svtChannels.findChannelID(this.febID, this.hybridID,
                        this.channel));
                this.isValidChannel = false;
                break;
            case "baseline":
                this.baselineSampleID = Integer.parseInt(attributes.getValue("id"));
                break;
            case "noise":
                this.noiseSampleID = Integer.parseInt(attributes.getValue("id"));
                break;
            default:
                break;
        }
    }
}
