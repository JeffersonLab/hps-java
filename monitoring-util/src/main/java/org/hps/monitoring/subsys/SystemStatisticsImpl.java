package org.hps.monitoring.subsys;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.hps.monitoring.plotting.StripChartUpdater;

/**
 * Implementation of {@link SystemStatistics}.
 */
// FIXME: Rolling averages need to happen over a greater time period like 30 seconds
// instead of 1 second, because otherwise the statistics don't look right.
public class SystemStatisticsImpl implements SystemStatistics {

    long tickLengthMillis = 1000; // default is one second tick
    long sessionElapsedMillis;
    long startTimeMillis;
    long stopTimeMillis;
    long eventsSinceTick;
    long bytesSinceTick;
    long totalEvents;
    long totalBytes;
    long tickStartMillis;
    long tickElapsedMillis;
    static final long Kb = 1 * 1024;
    static final long Mb = Kb * 1024;
    static final double milliToSecond = 0.001;
    static final DecimalFormat decimalFormat = new DecimalFormat("#.####");
    Timer timer;
    List<TimerTask> subtasks = new ArrayList<TimerTask>();

    @Override
    public void update(int size) {
        addEvent();
        addData(size);
        updateElapsedTime();
    }

    @Override
    public void setTickLengthMillis(long tickLengthMillis) {
        this.tickLengthMillis = tickLengthMillis;
    }

    @Override
    public long getTickLengthMillis() {
        return tickLengthMillis;
    }

    @Override
    public long getTotalElapsedMillis() {
        return sessionElapsedMillis;
    }

    @Override
    public long getStartTimeMillis() {
        return this.startTimeMillis;
    }

    @Override
    public long getStopTimeMillis() {
        return this.stopTimeMillis;
    }
    
    @Override
    public long getTickElapsedMillis() {
        return tickElapsedMillis;
    }

    /**
     * Event statistics.
     */
    
    @Override
    public long getEventsReceived() {
        return eventsSinceTick;
    }
     
    @Override
    public long getTotalEvents() {
        return totalEvents;
    }
    
    @Override
    public double getEventsPerSecond() {
        if (eventsSinceTick > 0 && tickElapsedMillis > 0)
            return (double) eventsSinceTick / millisToSeconds(tickElapsedMillis);
        else
            return 0.;
    }

    @Override
    public double getAverageEventsPerSecond() {
        try {
            return Double.parseDouble(decimalFormat.format(totalEvents / millisToSeconds(getTotalElapsedMillis())));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * Data statistics.
     */
    
    @Override
    public long getBytesReceived() {
        return bytesSinceTick;
    }
    
    @Override
    public double getTotalMegabytes() {
        return bytesToMb(totalBytes);
    }

    @Override
    public double getAverageMegabytesPerSecond() {
        try {
            return Double.parseDouble(decimalFormat.format(bytesToMb(totalBytes) / millisToSeconds(getTotalElapsedMillis())));
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
   
    @Override
    public double getBytesPerSecond() {
        if (bytesSinceTick > 0 && tickElapsedMillis > 0)
            return (double) bytesSinceTick / millisToSeconds(tickElapsedMillis);
        else
            return 0.;
    }

    @Override
    public void start() {

        // Set time variables.
        long currentTimeMillis = System.currentTimeMillis();
        startTimeMillis = currentTimeMillis;
        tickStartMillis = currentTimeMillis;

        // Start Timer task which executes at tick length.
        TimerTask task = new TimerTask() {
            public void run() {

                // Run sub-tasks.
                for (TimerTask subtask : subtasks) {
                    subtask.run();
                }

                nextTick();
            }
        };
        timer = new Timer();
        timer.schedule(task, 0, tickLengthMillis);
    }

    @Override
    public void stop() {
        // Kill the Timer.
        if (timer != null) {
            timer.cancel();
            timer.purge();
        }

        // Set stop time.
        stopTimeMillis = System.currentTimeMillis();
    }

    @Override
    public void printSession(PrintStream ps) {
        ps.println("session statistics ...");
        ps.println("  timeElapsedMillis = " + this.getTotalElapsedMillis());
        ps.println("  cumulativeEvents = " + this.getTotalEvents());
        ps.println("  averageEventsPerSecond = " + this.getAverageEventsPerSecond());
        ps.println("  averageMegaBytesPerSecond = " + this.getAverageMegabytesPerSecond());

    }

    @Override
    public void printTick(PrintStream ps) {
        ps.println("tick statistics ...");
        ps.println("  tickElapsedMillis = " + this.getTickElapsedMillis());
        ps.println("  eventsSinceTick = " + this.getEventsReceived());
        ps.println("  bytesSinceTick = " + this.getBytesReceived());
    }

    @Override
    public void addSubTask(TimerTask subtask) {
        this.subtasks.add(subtask);
    }

    void addEvent() {
        eventsSinceTick += 1;
        totalEvents += 1;
    }

    void addData(int size) {
        bytesSinceTick += size;
        totalBytes += size;
    }

    void updateElapsedTime() {
        tickElapsedMillis = System.currentTimeMillis() - tickStartMillis;
        sessionElapsedMillis = System.currentTimeMillis() - startTimeMillis;
    }

    // Bytes to megabytes to 2 decimal places.
    static final double bytesToMb(long size) {
        return Double.parseDouble(decimalFormat.format((double) size / Mb));
    }

    static final double millisToSeconds(long millis) {
        return ((double) millis) / 1000.;
    }

    synchronized void nextTick() {
        eventsSinceTick = 0;
        bytesSinceTick = 0;
        tickElapsedMillis = 0;
        tickStartMillis = System.currentTimeMillis();
    }
    
    public abstract class SystemStatisticsUpdater extends StripChartUpdater {
        SystemStatisticsUpdater() {
            addSubTask(this);
        }
    }
    
    public class AverageEventsPerSecondUpdater extends SystemStatisticsUpdater {

        @Override
        public float nextValue() {
            return (float) getAverageEventsPerSecond();
        }
    }
    
    public class EventsPerSecondUpdater extends SystemStatisticsUpdater {

        @Override
        public float nextValue() {
            return (float) getEventsPerSecond();
        }
    }
            
    public class EventsReceivedUpdater extends SystemStatisticsUpdater {

        @Override
        public float nextValue() {
            return getEventsReceived();
        }
    }

    public class TotalEventsUpdater extends SystemStatisticsUpdater {
        @Override
        public float nextValue() {
            return getTotalEvents();
        }
    }

    public class BytesReceivedUpdater extends SystemStatisticsUpdater {
        @Override
        public float nextValue() {
            return getBytesReceived();
        }
    }

    public class AverageMegabytesPerSecondUpdater extends SystemStatisticsUpdater {
        @Override
        public float nextValue() {
            return (float) getAverageMegabytesPerSecond();
        }
    }

    public class TotalMegabytesUpdater extends SystemStatisticsUpdater {
        @Override
        public float nextValue() {
            return (float) getTotalMegabytes();
        }
    }
    
    public class BytesPerSecondUpdater extends SystemStatisticsUpdater {

        @Override
        public float nextValue() {
            return (float) getBytesPerSecond();
        }
    }
    
    public class MegabytesPerSecondUpdater extends SystemStatisticsUpdater {
        @Override
        public float nextValue() {
            return (float) getBytesPerSecond() / 1000000;
        }
    }
    
    
}
