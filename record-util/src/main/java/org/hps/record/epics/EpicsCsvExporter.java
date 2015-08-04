package org.hps.record.epics;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.util.log.DefaultLogFormatter;
import org.lcsim.util.log.LogUtil;

/**
 * Export EPICS data banks from EVIO to a CSV file.
 * <p>
 * The EPICS header information is appended to each row (run, sequence, timestamp).
 *
 * @author Jeremy McCormick, SLAC
 */
public class EpicsCsvExporter extends EpicsEvioProcessor {

    private static final Logger LOGGER = LogUtil.create(EpicsCsvExporter.class, new DefaultLogFormatter(), Level.ALL);

    private final File csvOutputFile;

    private final Set<EpicsData> epicsDataSet = new LinkedHashSet<EpicsData>();

    public EpicsCsvExporter() {
        this(new File("epics.csv"), true);
    }

    public EpicsCsvExporter(final File csvOutputFile, final boolean append) {
        if (csvOutputFile == null) {
            throw new IllegalArgumentException("csvOutputFile is null.");
        }
        if (csvOutputFile.exists() && !append) {
            throw new IllegalArgumentException("The file " + csvOutputFile.getPath()
                    + " already exists and append = false.");
        }
        this.csvOutputFile = csvOutputFile;
    }

    @Override
    public void endJob() {
        try {
            LOGGER.info("writing EPICS data to " + csvOutputFile.getPath() + " ...");
            this.write(csvOutputFile);
            LOGGER.info("wrote EPICS data to CSV");
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void process(final EvioEvent evioEvent) {
        super.process(evioEvent);
        epicsDataSet.add(this.getEpicsData());
    }

    /**
     * Write the set of {@link EpicsData} from event processing to a CSV file.
     *
     * @param file the output CSV file
     * @throws IOException if there is an IO problem with files operations
     */
    private void write(final File file) throws IOException {

        FileWriter fileWriter = null;
        CSVPrinter csvPrinter = null;

        try {

            boolean append = false;
            if (file.exists()) {
                LOGGER.info("appending EPICS data to existing file " + file.getPath());
                append = true;
            }

            fileWriter = new FileWriter(file, append);
            csvPrinter = new CSVPrinter(fileWriter, CSVFormat.DEFAULT);

            if (!append) {
                // Add field names as first row if file is new.
                final List<String> fieldNames = new ArrayList<String>();
                fieldNames.add("run");
                fieldNames.add("sequence");
                fieldNames.add("timestamp");
                fieldNames.addAll(EpicsData.getVariableNames());
                csvPrinter.printRecord(fieldNames);
            }

            // Loop over all EPICS data that was processed and saved.
            for (final EpicsData epicsData : epicsDataSet) {

                // Data record for CSV output.
                final List<Object> record = new ArrayList<Object>();

                // The EPICs header data.
                final EpicsHeader epicsHeader = epicsData.getEpicsHeader();

                // Append header information first if it exists (defaults to all zeroes if not present).
                int run = 0;
                int sequence = 0;
                int timestamp = 0;
                if (epicsHeader != null) {
                    run = epicsHeader.getRun();
                    sequence = epicsHeader.getSequence();
                    timestamp = epicsHeader.getTimeStamp();
                }
                record.add(run);
                record.add(sequence);
                record.add(timestamp);

                // Append EPICS data variables to the record (non-existent variables have value of 0).
                for (final String key : EpicsData.getVariableNames()) {
                    final Object value = 0;
                    if (epicsData.hasKey(key)) {
                        record.add(epicsData.getValue(key));
                    }
                    record.add(value);
                }

                // Write the record to CSV.
                csvPrinter.printRecord(record);
            }

        } finally {
            try {
                // Close the writers.
                fileWriter.flush();
                fileWriter.close();
                csvPrinter.close();
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
