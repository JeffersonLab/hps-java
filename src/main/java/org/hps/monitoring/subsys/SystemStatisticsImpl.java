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
    static final long Kb = 1  * 1024;
    static final long Mb = Kb * 1024;    
    static final double milliToSecond = 0.001;    
    static final DecimalFormat decimalFormat = new DecimalFormat("#.##");
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
    public long getTimeElapsedMillis() {        
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
    public long getCumulativeEvents() {
        return totalEvents;
    }

    @Override
    public double getAverageEventsPerSecond() {
        try {
            return Double.parseDouble(decimalFormat.format(totalEvents / millisToSeconds(getTimeElapsedMillis())));
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    @Override
    public double getCumulativeMb() {
        return bytesToMb(totalBytes);
    }

    @Override
    public double getAverageMbPerSecond() {
        try { 
            return Double.parseDouble(decimalFormat.format(bytesToMb(totalBytes) / millisToSeconds(getTimeElapsedMillis())));
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    @Override
    public long getEventsInTick() {
        return eventsSinceTick;
    }

    @Override
    public long getBytesInTick() {
        return bytesSinceTick;
    }
            
    @Override
    public long getTickElapsedMillis() {
        return tickElapsedMillis;
    }
    
    @Override
    public double getEventRate() {
        if (eventsSinceTick > 0 && tickElapsedMillis > 0)
            return (double)eventsSinceTick / millisToSeconds(tickElapsedMillis);
        else
            return 0.;
    }
    
    @Override
    public double getDataRateBytes() {
        if (bytesSinceTick > 0 && tickElapsedMillis > 0)
            return (double)bytesSinceTick / millisToSeconds(tickElapsedMillis);
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
        ps.println("  timeElapsedMillis = " + this.getTimeElapsedMillis());
        ps.println("  cumulativeEvents = " + this.getCumulativeEvents());
        ps.println("  averageEventsPerSecond = " + this.getAverageEventsPerSecond());
        ps.println("  averageMegaBytesPerSecond = " + this.getAverageMbPerSecond());
        
    }
    
    @Override
    public void printTick(PrintStream ps) {
        ps.println("tick statistics ...");
        ps.println("  tickElapsedMillis = " + this.getTickElapsedMillis());
        ps.println("  eventsSinceTick = " + this.getEventsInTick());
        ps.println("  bytesSinceTick = " + this.getBytesInTick());
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
        return Double.parseDouble(decimalFormat.format((double)size / Mb));
    }
    
    static final double millisToSeconds(long millis) {
        return ((double)millis) / 1000.;
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
    
    public class AverageEventRateUpdater extends SystemStatisticsUpdater {

        @Override
        public float nextValue() {
            return (float)getAverageEventsPerSecond();
        }        
    }
    
    public class EventsInTickUpdater extends SystemStatisticsUpdater {
       
        @Override
       public float nextValue() {
           return getEventsInTick();
       }
    }
    
    public class CumulativeEventsUpdater extends SystemStatisticsUpdater {
        @Override
        public float nextValue() {
            return getCumulativeEvents();
        }
    }
    
    public class BytesInTickUpdater extends SystemStatisticsUpdater {
        @Override
        public float nextValue() {
            return getBytesInTick();
        }
    }
    
    public class AverageMbUpdater extends SystemStatisticsUpdater {
        @Override
        public float nextValue() {
            return (float)getAverageMbPerSecond();
        }
    }
    
    public class CumulativeMbUpdater extends SystemStatisticsUpdater {
        @Override
        public float nextValue() {
            return (float)getCumulativeMb();
        }
    }        
    
    public class EventRateUpdater extends SystemStatisticsUpdater {
        
        @Override
       public float nextValue() {
           return (float)getEventRate();
       }
    }
    
    public class DataRateUpdater extends SystemStatisticsUpdater {
        
        @Override
       public float nextValue() {
           return (float)getDataRateBytes();
       }
    }
}
