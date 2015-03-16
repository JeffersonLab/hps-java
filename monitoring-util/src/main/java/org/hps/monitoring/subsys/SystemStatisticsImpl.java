package org.hps.monitoring.subsys;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.hps.monitoring.plotting.ValueProvider;

/**
 * Implementation of {@link SystemStatistics}.
 */
public class SystemStatisticsImpl implements SystemStatistics {

    long nominalTickLengthMillis = 1000; // default is 1 second tick
    long tickStartTimeMillis;
    long tickEndTimeMillis;    

    long eventsInTick;
    long bytesInTick;
    
    long startTimeMillis;
    long stopTimeMillis;
    
    long totalEvents;
    long totalBytes;

    static final long Kb = 1 * 1024;
    static final long Mb = Kb * 1024;
    static final double milliToSecond = 0.001;
    static final DecimalFormat decimalFormat = new DecimalFormat("#.####");
    Timer timer;
    
    List<SystemStatisticsListener> listeners = new ArrayList<SystemStatisticsListener>();
    
    @Override
    public void update(int size) {
        addEvent();
        addData(size);
    }

    @Override
    public void setNominalTickLengthMillis(long tickLengthMillis) {
        this.nominalTickLengthMillis = tickLengthMillis;
    }

    @Override
    public long getNominalTickLengthMillis() {
        return nominalTickLengthMillis;
    }

    @Override
    public long getTotalElapsedMillis() {
        return System.currentTimeMillis() - startTimeMillis;
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
        return System.currentTimeMillis() - tickStartTimeMillis;
    }
    
    @Override
    public long getTickEndTimeMillis() {
        return tickEndTimeMillis;
    }

    /**
     * Event statistics.
     */
    
    @Override
    public long getEventsReceived() {
        return eventsInTick;
    }
     
    @Override
    public long getTotalEvents() {
        return totalEvents;
    }
    
    @Override
    public double getEventsPerSecond() {
        if (eventsInTick > 0 && getTickElapsedMillis() > 0) {
            return (double) eventsInTick / millisToSeconds(getTickElapsedMillis());
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
        return bytesInTick;
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
        if (bytesInTick > 0 && getTickElapsedMillis() > 0)
            return (double) bytesInTick / millisToSeconds(getTickElapsedMillis());
        else
            return 0.;
    }
    
    @Override
    public double getMegabytesPerSecond() {
        double bytes = getBytesPerSecond();
        if (bytes > 0) {
            return bytesToMb(bytes);
        } else {
            return 0;
        }
    }
    
    @Override
    public void start() {

        // Set session start time variables.
        long currentTimeMillis = System.currentTimeMillis();
        startTimeMillis = currentTimeMillis;
        tickStartTimeMillis = currentTimeMillis;
        
        // Notify listeners of start.
        for (SystemStatisticsListener listener : listeners) {
            listener.started(this);
        }
        
        // Start timer task which executes at the nominal tick length to calculate statistics periodically.
        TimerTask task = new TimerTask() {
            public void run() {
                
                // End the current tick.
                endTick();
               
                // Start the new tick.
                nextTick();
            }
        };
        timer = new Timer();
        timer.schedule(task, 0, nominalTickLengthMillis);
    }
    
    void endTick() {
 
        // Set absolute end time of current tick.
        this.tickEndTimeMillis = System.currentTimeMillis();
        
        // Activate listeners.
        for (SystemStatisticsListener listener : listeners) {
            listener.endTick(this);
        }
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
        
        // Notify listeners of stop.
        for (SystemStatisticsListener listener : listeners) {
            listener.stopped(this);
        }
    }
    
    public void addSystemStatisticsListener(SystemStatisticsListener listener) {
        listeners.add(listener);
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
        eventsInTick += 1;
        totalEvents += 1;
    }

    void addData(int size) {
        bytesInTick += size;
        totalBytes += size;
    }

    // Bytes to megabytes to 2 decimal places.
    static final double bytesToMb(long size) {
        return Double.parseDouble(decimalFormat.format((double) size / Mb));
    }
    
    // Bytes to megabytes to 2 decimal places.
    static final double bytesToMb(double size) {
        return Double.parseDouble(decimalFormat.format(size / Mb));
    }

    static final double millisToSeconds(long millis) {
        return ((double) millis) / 1000.;
    }

    synchronized void nextTick() {
        eventsInTick = 0;
        bytesInTick = 0;
        tickStartTimeMillis = System.currentTimeMillis();
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