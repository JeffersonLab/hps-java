package org.hps.conditions.svt;

import java.io.File;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.hps.conditions.svt.SvtCalibration.SvtCalibrationCollection;
import org.hps.conditions.svt.SvtChannel.SvtChannelCollection;
import org.hps.conditions.svt.SvtDaqMapping.SvtDaqMappingCollection;

/**
 * Reader used to parse SVT conditions.
 *
 * @author <a href="mailto:omoreno1@ucsc.edu">Omar Moreno</a>
 */
public final class SvtConditionsReader {

    /**
     * SAX handler for calibration elements.
     */
    private CalibrationHandler calibrationHandler;

    /**
     * SAX handler for DAQ map elements.
     */
    private DaqMapHandler daqMapHandler;

    /**
     * SAX parser.
     */
    private final SAXParser parser;

    /**
     * SAX parser factory.
     */
    private final SAXParserFactory parserFactory = SAXParserFactory.newInstance();

    /**
     * Default constructor.
     *
     * @throws Exception if a SAX parser can't be created.
     */
    public SvtConditionsReader() throws Exception {

        // Create a new SAX parser.
        this.parser = this.parserFactory.newSAXParser();
    }

    /**
     * Get the collection of {@link SvtDaqMapping} objects created when parsing the DAQ map. If a DAQ map hasn't been
     * parsed yet, an empty collection will be returned.
     *
     * @return A collection of {@link SvtDaqMapping} objects
     */
    public SvtDaqMappingCollection getDaqMapCollection() {
        return this.daqMapHandler.getDaqMap();
    }

    /**
     * Get the collection of {@link SvtCalibration} objects built from parsing a calibrations file. If a calibrations
     * file hasn't been parsed yet, an empty collection will be returned.
     *
     * @return A collection of {@link SvtCalibration} objects
     */
    public SvtCalibrationCollection getSvtCalibrationCollection() {
        return this.calibrationHandler.getCalibrations();
    }

    /**
     * Get the collection of {@link SvtChannel} objects built from parsing the DAQ map. If a DAQ maps hasn't been parsed
     * yet, an empty collection will be returned.
     *
     * @return A collection of {@link SvtChannel} objects
     */
    public SvtChannelCollection getSvtChannelCollection() {
        return this.daqMapHandler.getSvtChannels();
    }

    /**
     * Parse a calibration file and create {@link SvtCalibration} objects out of all channel conditions.
     *
     * @param calibrationFile the input calibration file to parse
     * @throws Exception if there is an error parsing the calibrations data
     */
    public void parseCalibrations(final File calibrationFile) throws Exception {

        // Instantiate the calibration handler.
        this.calibrationHandler = new CalibrationHandler();

        // Parse the calibration file and create the collection of SvtCalibrations.
        this.parser.parse(calibrationFile, this.calibrationHandler);
    }

    /**
     * Parse a DAQ map file and create {@link SvtDaqMapping} objects.
     *
     * @param daqMapFile the input DAQ map file to parse
     * @throws Exception if there is a problem parsing the DAQ map XML data
     */
    public void parseDaqMap(final File daqMapFile) throws Exception {

        // Instantiate the DAQ map handler.
        this.daqMapHandler = new DaqMapHandler();

        // Parse the DAQ map file and create the collection of SvtDaqMapping objects.
        this.parser.parse(daqMapFile, this.daqMapHandler);

    }
}
