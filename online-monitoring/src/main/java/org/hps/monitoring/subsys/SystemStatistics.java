package org.hps.monitoring.subsys;

import java.io.PrintStream;

/**
 * This is an interface for a set of basic statistics about an online event processing system.
 */
public interface SystemStatistics {

    /**
     * Set the desired timer tick length in millis.
     * @param tickLengthMillis The desired tick length in millis.
     */
    void setNominalTickLengthMillis(long tickLengthMillis);

    /**
     * Get the nominal length of one tick in millis.
     *  Actual tick lengths lengths may vary slightly.
     * @return The nominal tick length in millis.
     */
    long getNominalTickLengthMillis();
    
    /**
     * Get the end of the tick in Unix time (milliseconds since the epoch).
     * @return The tick end in Unix time.
     */
    long getTickEndTimeMillis();

    /**
     * Start the timer thread for accumulating statistics.
     */
    void start();

    /**
     * Stop the timer thread for accumulating statistics.
     */
    void stop();

    /**
     * Update the statistics by incrementing the event count by one and then adding <tt>size</tt> to
     * the number of bytes received.
     * @param size The number of bytes received.
     */
    void update(int size);

    /**
     * Get the number of millis since the session started.
     * @return The number of millis since session start.
     */
    long getTotalElapsedMillis();

    /**
     * Get the Unix start time of the session.
     * @return The start time in millis.
     */
    long getStartTimeMillis();

    /**
     * Get the Unix stop time of the session.
     * @return The stop time in millis.
     */
    long getStopTimeMillis();

    /**
     * Get the number of events in the current tick.
     * @return The number of events in the current tick.
     */
    long getEventsReceived();

    /**
     * Get the total number of events processed thusfar.
     * @return The total number of events processed so far.
     */
    long getTotalEvents();

    /**
     * Get the average number of events per second in the session. It simply divides the number of
     * events by the session time.
     * @return The average events per second.
     */
    double getAverageEventsPerSecond();

    /**
     * Get the number of bytes received in the current tick.
     * @return The number of bytes received in the tick.
     */
    long getBytesReceived();

    /**
     * Get the total number of megabytes of data received thusfar.
     * @return The amount of data in megabytes received in the session.
     */
    double getTotalMegabytes();

    /**
     * Get the average Mb per second of the session, which is the total amount of data divided by
     * the total time.
     * @return The average megabytes per second.
     */
    double getAverageMegabytesPerSecond();

    /**
     * Get the immediate event rate which is the number of events received in the current tick over
     * the time elapsed in the tick.
     * @return The event rate in [events/second].
     */
    double getEventsPerSecond();

    /**
     * Get the immediate data rate which is the amount of data in bytes received in the current tick
     * over the tim elapsed in the tick.
     * @return The data rate in [bytes/second].
     */
    public double getBytesPerSecond();
    
    /**
     * Get the immediate data rate which is the amount of data in megabytes received in the current tick
     * over the time elapsed in the tick.
     * @return The data rate in [bytes/second].
     */
    public double getMegabytesPerSecond();

    /**
     * Get the number of milliseconds since the last tick.
     * @return The number of millis elapsed in the current tick.
     */
    long getTickElapsedMillis();

    /**
     * Print session statistics.
     * @param ps The PrintStream for display.
     */
    void printSession(PrintStream ps);

    /**
     * Print tick statistics.
     * @param ps The PrintStream for display.
     */
    void printTick(PrintStream ps);
    
    /**
     * 
     * @param listener
     */
    void addSystemStatisticsListener(SystemStatisticsListener listener);   
}
