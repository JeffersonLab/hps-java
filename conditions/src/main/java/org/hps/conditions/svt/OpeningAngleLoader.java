package org.hps.conditions.svt;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.hps.conditions.run.RunSpreadsheet;
import org.hps.conditions.run.RunSpreadsheet.RunData;
import org.hps.conditions.run.RunSpreadsheet.RunMap;
import org.hps.conditions.svt.MotorPositionLoader.MotorPositionInterval;
import org.hps.conditions.svt.MotorPositionLoader.Side;

/**
 * @author Jeremy McCormick, SLAC
 */
public class OpeningAngleLoader {

    private static final String BOT_FILE = "mya_svt_bot.txt";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMMM dd YYYY HH:mm z");
    private static final String OUT_FILE = "svt_opening_angles.txt";
    private static final String RUN_FILE = "runs.csv";

    private static final String TOP_FILE = "mya_svt_top.txt";

    /**
     * Check if the run record looks good.
     *
     * @param data
     * @return
     */
    private static boolean acceptRun(final RunData data) {
        return !data.getRecord().get("to_tape").equals("JUNK")
                && data.getRecord().get("trigger_config").trim().length() > 0
                && !data.getRecord().get("trigger_config").contains("cosmic") && data.getStartDate() != null;
    }

    private static MotorPositionInterval findInterval(final List<MotorPositionInterval> intervals, final Date date) {
        MotorPositionInterval interval = null;
        final Iterator<MotorPositionInterval> it = intervals.listIterator();
        while ((interval = it.next()) != null) {

            // Start and end dates in the interval.
            final Date startDate = interval.getStartDate();
            final Date endDate = interval.getEndDate();

            // Check if given date is within the interval.
            if ((startDate.compareTo(date) == -1 || startDate.compareTo(date) == 0) && endDate.compareTo(date) == 1
                    || endDate.compareTo(date) == 0) {
                break;
            }

            // Didn't find it.
            if (!it.hasNext()) {
                interval = null;
                break;
            }
        }
        return interval;
    }

    private static List<MotorPositionInterval> getMotorPositionIntervals(final String path, final Side side)
            throws Exception {
        final MotorPositionLoader loader = new MotorPositionLoader();
        loader.setSide(side);
        loader.load(path);
        return loader.findIntervals();
    }

    private static RunMap loadRunMap(final String path) {
        final File runFile = new File(path);
        final RunSpreadsheet runSpreadsheet = new RunSpreadsheet(runFile);
        return runSpreadsheet.getRunMap();
    }

    public static void main(final String[] args) throws Exception {

        // Load top and bottom intervals from MYA dump.
        final List<MotorPositionInterval> topIntervals = getMotorPositionIntervals(TOP_FILE, Side.TOP);
        final List<MotorPositionInterval> botIntervals = getMotorPositionIntervals(BOT_FILE, Side.BOT);

        // Load run map from spreadsheet.
        final RunMap runMap = loadRunMap(RUN_FILE);

        // Write out run data combined with SVT opening angle intervals.
        final PrintStream ps = new PrintStream(new FileOutputStream(OUT_FILE));
        for (final RunData data : runMap.values()) {
            if (acceptRun(data)) {
                final MotorPositionInterval topInterval = findInterval(topIntervals, data.getStartDate());
                final MotorPositionInterval botInterval = findInterval(botIntervals, data.getStartDate());
                printLine(ps, data, topInterval, botInterval);
            }
        }
        ps.close();
    }

    private static void printLine(final PrintStream ps, final RunData data, final MotorPositionInterval topInterval,
            final MotorPositionInterval botInterval) {
        ps.print("run: " + data.getRun() + " @ [" + data.getRecord().get("svt_y_position") + "], ");
        if (topInterval != null) {
            ps.print("start(top): " + DATE_FORMAT.format(topInterval.getStartDate()) + ", ");
            ps.print("end(top): " + DATE_FORMAT.format(topInterval.getEndDate()) + ", ");
        } else if (botInterval != null) {
            ps.print("start(bot): " + DATE_FORMAT.format(botInterval.getStartDate()) + ", ");
            ps.print("end(bot): " + DATE_FORMAT.format(botInterval.getEndDate()) + ", ");
        } else {
            ps.print("start: NONE, end: NONE, ");
        }
        ps.print("topAngle: ");
        if (topInterval != null) {
            ps.print(topInterval.getAngle());
        } else {
            ps.print("NONE");
        }
        ps.print(", botAngle: ");
        if (botInterval != null) {
            ps.print(botInterval.getAngle());
        } else {
            ps.print("NONE");
        }
        ps.print(", topYStage: ");
        if (topInterval != null) {
            ps.print(topInterval.getYStage());
        } else {
            ps.print("NONE");
        }
        ps.print(", botYStage: ");
        if (botInterval != null) {
            ps.print(botInterval.getYStage());
        } else {
            ps.print("NONE");
        }
        ps.println();
        ps.flush();
    }
}
