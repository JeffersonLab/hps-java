package org.hps.monitoring.subsys;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Implementation of {@link SystemStatistics}.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
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
    public double getCumulativeMegaBytes() {
        return bytesToMb(totalBytes);
    }

    @Override
    public double getAverageMegaBytesPerSecond() {
        try { 
            return Double.parseDouble(decimalFormat.format(bytesToMb(totalBytes) / millisToSeconds(getTimeElapsedMillis())));
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    @Override
    public long getEventsSinceTick() {
        return eventsSinceTick;
    }

    @Override
    public long getBytesSinceTick() {
        return bytesSinceTick;
    }
            
    @Override
    public long getTickElapsedMillis() {
        return tickElapsedMillis;
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
                nextTick();
            }
        };        
        timer = new Timer();
        timer.schedule(task, 0, tickLengthMillis);
    }

    @Override
    public void stop() {        
        // Kill the Timer.
        if (timer != null)
            timer.cancel();
        
        // Set stop time.
        stopTimeMillis = System.currentTimeMillis();
    }    
    
    @Override
    public void printSession(PrintStream ps) {
        ps.println("session statistics ...");
        ps.println("  getTimeElapsedMillis = " + this.getTimeElapsedMillis());
        ps.println("  getCumulativeEvents = " + this.getCumulativeEvents());
        ps.println("  getAverageEventsPerSecond = " + this.getAverageEventsPerSecond());
        ps.println("  getAverageMegaBytesPerSecond = " + this.getAverageMegaBytesPerSecond());
        
    }
    
    @Override
    public void printTick(PrintStream ps) {
        ps.println("tick statistics ...");
        ps.println("  getTickElapsedMillis = " + this.getTickElapsedMillis());
        ps.println("  getEventsSinceTick = " + this.getEventsSinceTick());
        ps.println("  getBytesSinceTick = " + this.getBytesSinceTick());
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
}
