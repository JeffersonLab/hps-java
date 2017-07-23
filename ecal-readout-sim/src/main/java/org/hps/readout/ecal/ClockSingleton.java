package org.hps.readout.ecal;

/**
 * Class tracks current simulation time in steps of constant value.
 * The class should be incremented from exclusively from the {@link
 * org.hps.readout.ecal.ClockDriver ClockDriver} class, though its
 * status may be safely read from anywhere.
 * 
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class ClockSingleton {
    /**
     * The total number of steps that have been observed.
     */
    private static int clock;
    /**
     * The time value, in units of nanoseconds, of each step. This
     * must be set equivalent to the beam bunch spacing of the input
     * Monte Carlo.
     */
    private static double dt = 2.0;
    
    /**
     * Instantiates the class. This method is private as no instances
     * of it should be created.
     */
    private ClockSingleton() { }
    
    /**
     * Resets the clock to zero steps.
     */
    public static void init() {
        clock = 0;
    }
    
    /**
     * Gets the current number of steps that have occurred.
     * @return Returns the total number of steps as an
     * <code>int</code> primitive.
     */
    public static int getClock() {
        return clock;
    }
    
    /**
     * Gets the total amount of simulation time that has occurred.
     * @return Returns the simulation time as a <code>double</code>
     * primitive.
     */
    public static double getTime() {
        return dt * clock;
    }
    
    /**
     * Gets the amount of time represented by each step. Step size us
     * in units of nanoseconds.
     * @return Returns the step size in units of nanoseconds as a
     * <code>double</code> primitive.
     */
    public static double getStepSize() {
        return dt;
    }
    
    /**
     * Sets the amount of time represented by each step. Step size us
     * in units of nanoseconds and is 2 ns by default.
     * @param stepSize - The step size, in units of nanoseconds.
     * @throws RuntimeException Occurs if the clock has already been
     * stepped. Step size must be set either before stepping, the
     * method {@link org.hps.readout.ecal.ClockSingleton#init()
     * init()} must be called first.
     */
    public static void setStepSize(double stepSize) throws RuntimeException {
        if(clock != 0) {
            throw new RuntimeException("ClockSingleton: The step size can not be changed after a step has been taken.");
        }
        dt = stepSize;
    }
    
    /**
     * Increments the clock by one step.
     */
    public static void step() {
        clock++;
    }
}