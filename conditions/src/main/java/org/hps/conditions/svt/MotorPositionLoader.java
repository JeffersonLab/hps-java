package org.hps.conditions.svt;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

/**
 * Load SVT motor positions from a MYA dump, figure out time ranges (same position for > 10 seconds), and then convert
 * the motor stage to an opening angle.
 * <p>
 * The calculated angle ranges are written out to a comma delimited text file with double-quoted field values.
 */
public class MotorPositionLoader {

    class MotorPositionInterval {

        private final double angle;
        private final Date endDate;
        private final Date startDate;
        private final double yStage;

        MotorPositionInterval(final Date startDate, final Date endDate, final double angle, final double yStage) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.angle = angle;
            this.yStage = yStage;
        }

        double getAngle() {
            return this.angle;
        }

        Date getEndDate() {
            return this.endDate;
        }

        Date getStartDate() {
            return this.startDate;
        }

        double getYStage() {
            return this.yStage;
        }

        @Override
        public String toString() {
            return "MotorPositionInterval { start: " + this.startDate + ", end: " + this.endDate + ", angle: "
                    + this.angle + ", yStage: " + this.yStage + " }";
        }
    }

    private class MotorPositionMyaRecord {

        private final Date date;
        private final double position;

        MotorPositionMyaRecord(final Date date, final double position) {
            this.date = date;
            this.position = position;
        }

        Date getDate() {
            return this.date;
        }

        double getPosition() {
            return this.position;
        }
    }

    enum Side {
        BOT, TOP
    }

    private static final double ANGLE_CONVERSION = 832.714;

    private static final double BOTTOM_ANGLE_CONSTANT = 17.397;
    // private static final double BOTTOM_LAYER_CONSTANT1 = 0.363;
    // private static final double BOTTOM_LAYER_CONSTANT2 = -6.815;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static final long MIN_TIME_INTERVAL = 10000L;

    private static final Options OPTIONS = new Options();

    private static final double TOP_ANGLE_CONSTANT = 17.821;
    // private static final double TOP_LAYER_CONSTANT1 = -0.391;
    // private static final double TOP_LAYER_CONSTANT2 = 7.472;

    static {
        OPTIONS.addOption("h", "help", false, "print help");
        OPTIONS.addOption("s", "side", true, "'top' or 'bot' (required)");
        OPTIONS.getOption("s").setRequired(true);
        OPTIONS.addOption("i", "input-file", true, "input text file dumped from MYA (required)");
        OPTIONS.getOption("i").setRequired(true);
        OPTIONS.addOption("o", "output-file", true, "output text file with computed angle intervals");
        // OPTIONS.addOption("l", "layer", false, "write out layer 1 position instead of computed angle");
    }

    public static void main(final String args[]) {
        new MotorPositionLoader().run(args);
    }

    /**
     * Print the usage statement for this tool to the console.
     */
    private static final void printUsage(final int status) {
        final HelpFormatter help = new HelpFormatter();
        help.printHelp("MotorPositionLoader", "", OPTIONS, "");
        System.exit(status);
    }

    private List<MotorPositionInterval> intervals;

    private double motorConstant;

    private List<MotorPositionMyaRecord> records;

    // private double layerConstant1;
    // private double layerConstant2;

    Side side = null;

    MotorPositionLoader() {
    }

    // private boolean writeLayerPosition = false;

    private double computeAngle(final double yStage) {
        double angle = (this.motorConstant - yStage) / ANGLE_CONVERSION;
        if (Side.BOT.equals(this.side)) {
            angle = -angle;
        }
        return angle;
    }

    List<MotorPositionInterval> findIntervals() {
        this.intervals = new ArrayList<MotorPositionInterval>();
        for (int i = 0; i < this.records.size() - 1; i++) {
            final Date currentDate = this.records.get(i).getDate();
            final Date nextDate = this.records.get(i + 1).getDate();
            final long timeDiff = nextDate.getTime() - currentDate.getTime();
            if (timeDiff >= MIN_TIME_INTERVAL) {
                final double yStage = this.records.get(i).getPosition();
                final double angle = this.computeAngle(yStage);
                this.intervals.add(new MotorPositionInterval(currentDate, nextDate, angle, yStage));
            }
        }
        return this.intervals;
    }

    void load(final String path) throws IOException, ParseException, FileNotFoundException {
        this.records = new ArrayList<MotorPositionMyaRecord>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                final String dateString = line.substring(0, 19);
                final String positionString = line.substring(20);
                try {
                    final Date date = DATE_FORMAT.parse(dateString);
                    final double position = Double.parseDouble(positionString);
                    this.records.add(new MotorPositionMyaRecord(date, position));
                } catch (final NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // private double computeLayer1Position(final double yStage) {
    // return layerConstant1 * yStage + layerConstant2;
    // }

    /**
     * Run from command line arguments.
     *
     * @param args
     */
    void run(final String args[]) {

        final PosixParser parser = new PosixParser();

        CommandLine cl = null;
        try {
            cl = parser.parse(OPTIONS, args);
        } catch (final Exception e) {
            printUsage(1);
            throw new RuntimeException();
        }

        if (cl.hasOption("h")) {
            printUsage(0);
        }

        if (cl.hasOption("s")) {
            this.setSide(Side.valueOf(cl.getOptionValue("s").toUpperCase()));
        } else {
            printUsage(0);
        }

        this.setSide(this.side);

        String path = null;
        if (cl.hasOption("i")) {
            path = cl.getOptionValue("i");
        } else {
            printUsage(1);
        }

        // if (cl.hasOption("l")) {
        // setWriteLayerPosition(true);
        // }

        try {
            this.load(path);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        // Find the time intervals with a certain motor position setting.
        this.findIntervals();

        if (cl.hasOption("o")) {
            final String outputPath = cl.getOptionValue("o");
            try {
                this.toCsv(outputPath);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void setSide(final Side side) {
        this.side = side;
        if (Side.TOP.equals(side)) {
            this.motorConstant = TOP_ANGLE_CONSTANT;
            // this.layerConstant1 = TOP_LAYER_CONSTANT1;
            // this.layerConstant2 = TOP_LAYER_CONSTANT2;
        } else if (Side.BOT.equals(side)) {
            this.motorConstant = BOTTOM_ANGLE_CONSTANT;
            // this.layerConstant1 = BOTTOM_LAYER_CONSTANT1;
            // this.layerConstant2 = BOTTOM_LAYER_CONSTANT2;
        }
    }

    // private void setWriteLayerPosition(final boolean writeLayer) {
    // this.writeLayerPosition = writeLayer;
    // }

    private void toCsv(final String path) throws IOException {
        System.out.println("writing " + this.intervals.size() + " intervals to file to " + path + " ...");
        final FileWriter fw = new FileWriter(new File(path));
        final BufferedWriter bw = new BufferedWriter(fw);
        for (final MotorPositionInterval interval : this.intervals) {
            bw.write("\"" + DATE_FORMAT.format(interval.getStartDate()) + "\",");
            bw.write("\"" + DATE_FORMAT.format(interval.getEndDate()) + "\",");
            bw.write("\"" + interval.getAngle() + "\"");
            bw.write('\n');
        }
        bw.close();
        System.out.println("done writing intervals");
    }
}
