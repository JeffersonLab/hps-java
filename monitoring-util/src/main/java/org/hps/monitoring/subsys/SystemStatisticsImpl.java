package org.hps.monitoring.subsys;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

import org.hps.monitoring.plotting.ValueProvider;

/**
 * Implementation of {@link SystemStatistics}.
 */
public class SystemStatisticsImpl implements SystemStatistics {

    long tickLengthMillis = 1000; // default is 1 second tick
    long totalElapsedMillis;
    long startTimeMillis;
    long stopTimeMillis;
    long eventsSinceTick;
    long bytesSinceTick;
    long totalEvents;
    long totalBytes;
    long tickStartMillis;
    static final long Kb = 1 * 1024;
    static final long Mb = Kb * 1024;
    static final double milliToSecond = 0.001;
    static final DecimalFormat decimalFormat = new DecimalFormat("#.####");
    Timer timer;
    
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
        return totalElapsedMillis;
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
        return System.currentTimeMillis() - tickStartMillis;
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
        if (eventsSinceTick > 0 && getTickElapsedMillis() > 0) {
            return (double) eventsSinceTick / millisToSeconds(getTickElapsedMillis());
        } else {
            return 0.;
        }
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
        if (bytesSinceTick > 0 && getTickElapsedMillis() > 0)
            return (double) bytesSinceTick / millisToSeconds(getTickElapsedMillis());
        else
            return 0.;
    }

    @Override
    public void start() {

        // Set session start time variables.
        long currentTimeMillis = System.currentTimeMillis();
        startTimeMillis = currentTimeMillis;
        tickStartMillis = currentTimeMillis;

        // Start timer task which executes at the nominal tick length to calculate statistics periodically.
        TimerTask task = new TimerTask() {
            public void run() {
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
   
    void addEvent() {
        eventsSinceTick += 1;
        totalEvents += 1;
    }

    void addData(int size) {
        bytesSinceTick += size;
        totalBytes += size;
    }

    void updateElapsedTime() {
        totalElapsedMillis = System.currentTimeMillis() - startTimeMillis;
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
        tickStartMillis = System.currentTimeMillis();
    }
    
    public abstract class SystemStatisticsProvider implements ValueProvider {
    }
    
    public class AverageEventsPerSecondProvider extends SystemStatisticsProvider {

        @Override
        public float[] getValues() {
            return new float[] {(float) getAverageEventsPerSecond()};
        }
    }
    
    public class EventsPerSecondProvider extends SystemStatisticsProvider {

        @Override
        public float[] getValues() {
            return new float[] {(float) getEventsPerSecond()};
        }
    }
            
    public class EventsReceivedProvider extends SystemStatisticsProvider {

        @Override
        public float[] getValues() {
            return new float[] {getEventsReceived()};
        }
    }

    public class TotalEventsProvider extends SystemStatisticsProvider {
        @Override
        public float[] getValues() {
            return new float[] {getTotalEvents()};
        }
    }

    public class BytesReceivedProvider extends SystemStatisticsProvider {
        @Override
        public float[] getValues() {
            return new float[]{getBytesReceived()};
        }
    }

    public class AverageMegabytesPerSecondProvider extends SystemStatisticsProvider {
        @Override
        public float[] getValues() {
            return new float[] {(float) getAverageMegabytesPerSecond()};
        }
    }

    public class TotalMegabytesProvider extends SystemStatisticsProvider {
        @Override
        public float[] getValues() {
            return new float[] {(float) getTotalMegabytes()};
        }
    }
    
    public class BytesPerSecondProvider extends SystemStatisticsProvider {

        @Override
        public float[] getValues() {
            return new float[] {(float) getBytesPerSecond()};
        }
    }
    
    public class MegabytesPerSecondProvider extends SystemStatisticsProvider {
        @Override
        public float[] getValues() {
            return new float[] {(float) getBytesPerSecond() / 1000000};
        }
    }
}