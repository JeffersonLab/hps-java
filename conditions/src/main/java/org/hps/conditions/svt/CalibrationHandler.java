package org.hps.conditions.svt;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtCalibration.SvtCalibrationCollection;
import org.hps.conditions.svt.SvtChannel.SvtChannelCollection;
import org.lcsim.util.log.DefaultLogFormatter;
import org.lcsim.util.log.LogUtil;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *  Handler for calibration events.
 *
 *  @author <a href="mailto:omoreno1@ucsc.edu">Omar Moreno</a>
 */
class CalibrationHandler extends DefaultHandler {

    /**
     * Initialize the logger.
     */
    private static Logger logger = LogUtil.create(SvtConditionsLoader.class.getSimpleName(),
            new DefaultLogFormatter(), Level.INFO);

    /**
     * List of SVT channels.
     */
    private SvtChannelCollection svtChannels;

    /**
     * List of SVT calibrations.
     */
    private SvtCalibrationCollection calibrations = new SvtCalibrationCollection();

    /**
     * An SVT calibration object encapsulating the baseline and noise values for a channel.
     */
    private SvtCalibration calibration = null;

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
     * Channel number (0-639).
     */
    private int channel = 0;

    /**
     * Baseline sample ID (0-5).
     */
    private int baselineSampleID = 0;

    /**
     * Noise sample ID (0-5).
     */
    // FIXME: This variable is unused.
    private int noiseSampleID = 0;

    /**
     * Flag denoting whether the calibrations of a given channel should be
     * loaded into the conditions DB.  If a channel is found to be missing
     * baseline or noise values, is will be marked invalid.
     */
    private boolean isValidChannel = false;

    /**
     * Default constructor.
     */
    public CalibrationHandler() {
       svtChannels = (SvtChannelCollection) DatabaseConditionsManager.getInstance()
               .getCachedConditions(SvtChannelCollection.class, "svt_channels").getCachedData();
    }

    /**
     *  Method that is triggered when the start tag is encountered.
     *
     *  @param uri the Namespace URI
     *  @param locaName the local name (without prefix)
     *  @param qName the qualified name (with prefix)
     *  @param attributes the attributes attached to the element
     *  @throws SAXException if there is an error processing the element
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
                logger.info("Processing calibrations for FEB " + febID + " Hybrid " + hybridID);
                break;
            case "channel":
                channel = Integer.parseInt(attributes.getValue("id"));
                calibration = new SvtCalibration(svtChannels.findChannelID(febID, hybridID, channel));
                isValidChannel = false;
                break;
            case "baseline":
                baselineSampleID = Integer.parseInt(attributes.getValue("id"));
                break;
            case "noise":
                noiseSampleID = Integer.parseInt(attributes.getValue("id"));
                break;
            default:
                break;
        }
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
    public void endElement(final String uri, final String localName, final String qName)
            throws SAXException {

        switch (qName) {
            case "channel":
                if (isValidChannel) {
                    calibrations.add(calibration);
                }
                break;
            case "baseline":
                calibration.setPedestal(baselineSampleID, Double.parseDouble(content));
                isValidChannel = true;
                break;
            case "noise":
                calibration.setNoise(baselineSampleID, Double.parseDouble(content));
                isValidChannel = true;
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
    * @throws SAXException if there is an error processing the element (possibly wraps another exception type)
    */
   @Override
   public void characters(final char[] ch, final int start, final int length)
       throws SAXException {
       content = String.copyValueOf(ch, start, length).trim();
   }

    /**
     * Get the {@link SvtCalibrationCollection} created from parsing the XML input file.
     *
     * @return the {@link SvtCalibrationCollection} created from parsing the XML
     */
    public SvtCalibrationCollection getCalibrations() {
        return calibrations;
    }
}
