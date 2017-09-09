package org.hps.crawler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.hps.datacat.DatacatConstants;

/**
 * Creates metadata for a file and writes the results to a Python snippet that can be used as input to the SRS datacat.
 * 
 * @author Jeremy McCormick, SLAC
 */
public final class MetadataWriter {

    private static final Logger LOGGER = Logger.getLogger(MetadataWriter.class.getPackage().getName());
    private static final Options OPTIONS = new Options();

    private List<File> inputFiles;
    private File outputDir = new File(".");

    static {
        OPTIONS.addOption("h", "help", false, "print help and exit (overrides all other arguments)");
        OPTIONS.addOption("d", "dir", true, "directory where metadata files should be written");
    }

    public static void main(final String[] args) {
        new MetadataWriter().parse(args).run();
    }

    private MetadataWriter parse(final String[] args) {

        try {
            final CommandLine cl = new PosixParser().parse(OPTIONS, args);

            // Print help.
            if (cl.hasOption("h") || args.length == 0) {
                this.printUsage();
            }

            // List of input files.
            if (!cl.getArgList().isEmpty()) {
                inputFiles = new ArrayList<File>();
                for (String arg : cl.getArgList()) {
                    inputFiles.add(new File(arg));
                }
            } else {
                printUsage();
            }
            if (this.inputFiles.isEmpty()) {
                throw new RuntimeException("Missing at least one input file to process.");
            }

            // Output directory for metadata files.
            if (cl.hasOption("d")) {
                outputDir = new File(cl.getOptionValue("d"));
                if (!outputDir.isDirectory()) {
                    throw new IllegalArgumentException("The file " + outputDir.getPath() + " is not a directory.");
                }
            }

        } catch (final ParseException e) {
            throw new RuntimeException("Error parsing command line options.", e);
        }

        LOGGER.info("Done parsing command line options.");

        return this;
    }

    private void printUsage() {
        final HelpFormatter help = new HelpFormatter();
        help.printHelp(80, "MetadataWriter [options] file1 file2 [...]", "", OPTIONS, "");
        System.exit(0);
    }

    private void run() {
        for (File file : inputFiles) {
            LOGGER.info("Creating metadata for " + file.getPath() + " ...");
            Map<String, Object> metadata = DatacatHelper.createMetadata(file);
            String metadataFileName = this.outputDir + File.separator + file.getName() + ".metadata";
            writeString(toPyDict(metadata), new File(metadataFileName));
            LOGGER.info("Wrote metadata for " + file.getPath() + " to " + metadataFileName);
        }
    }

    private static String toPyDict(Map<String, Object> metadata) {
        StringBuffer sb = new StringBuffer();
        sb.append("{");
        for (String name : DatacatConstants.getSystemMetadata()) {
            if (metadata.containsKey(name)) {
                Object value = metadata.get(name);
                if (value instanceof Number) {
                    sb.append("\"" + name + "\" : " + metadata.get(name) + ", ");
                } else {
                    sb.append("\"" + name + "\" : \"" + metadata.get(name) + "\", ");
                }
            }
        }
        sb.setLength(sb.length() - 2);
        sb.append(", \"versionMetadata\" : {");
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            if (!DatacatConstants.isSystemMetadata(entry.getKey())) {
                Object value = entry.getValue();
                String name = entry.getKey();
                if (value instanceof Number) {
                    sb.append("\"" + name + "\" : " + metadata.get(name) + ", ");
                } else {
                    sb.append("\"" + name + "\" : \"" + metadata.get(name) + "\", ");
                }
            }
        }
        sb.setLength(sb.length() - 2);
        sb.append("}");
        sb.append("}");
        return sb.toString();
    }

    private static void writeString(String dictString, File file) {
        try {
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(dictString);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
