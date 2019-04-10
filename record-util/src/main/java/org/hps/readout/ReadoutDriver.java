package org.hps.readout;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.hps.readout.util.collection.TriggeredLCIOData;
import org.lcsim.geometry.IDDecoder;
import org.lcsim.util.Driver;

/**
 * Class <code>ReadoutDriver</code> is an abstract framework for
 * defining a driver that functions with the HPS readout simulation.
 * A readout driver is responsible for interfacing with the {@link
 * org.hps.readout.ReadoutDataManager ReadoutDataManager} for both
 * the purpose of creating data output collections, and accessing
 * input data collections. It is also responsible for declaring its
 * own time offsets and collection dependencies. To this end, it must
 * perform several tasks:<br/>
 * <ul>
 * <li><b>Declare itself:</b> A readout driver must declare itself to
 * the <code>ReadoutDataManager</code>. This is done by default via
 * the default constructor, but if this is overridden, it then the
 * superclass constructor must be called, or the driver must be
 * manually declared via the method {@link
 * org.hps.readout.ReadoutDataManager#registerReadoutDriver(ReadoutDriver)
 * ReadoutDataManager.registerReadoutDriver(ReadoutDriver)}. Note
 * that if the driver is a trigger driver, it must instead use the
 * method {@link
 * org.hps.readout.ReadoutDataManager#registerTrigger(TriggerDriver)
 * ReadoutDataManager.registerTrigger(TriggerDriver)} instead.</li>
 * <li><b>Declare input collections:</b> All readout drivers must
 * specify which data collections they require as input. This is used
 * to correctly calculate the total time displacement of a driver by
 * the <code>ReadoutDataManager</code> and is accomplished by calling
 * {@link org.hps.readout.ReadoutDriver#addDependency(String)
 * addDependency(String)} for each input collection. This should be
 * done in the {@link org.lcsim.util.Driver#startOfData()
 * startOfData()} method.</li>
 * <li><b>Declare time displacement:</b> Readout drivers must specify
 * the amount of simulation time (events) that they need to process
 * their input data before they can produce output from it. For
 * instance, a driver that needs to buffer 16 ns into the "future" to
 * make its decisions would need to specify an offset of 16 ns.</li>
 * <li><b>Declare output collections:</b> All readout drivers must
 * also declare what collections they produce. This is performed by
 * the method {@link
 * org.hps.readout.ReadoutDataManager#registerCollection(LcsimCollection)
 * ReadoutDataManager.registerCollection(LcsimCollection)} and must
 * be done after all input collections have been declared and the
 * driver time offset calculated. This is also generally performed
 * in the <code>startOfData()</code> method.</li>
 * <li><b>Produce on-trigger data:</b> Some drivers may need to
 * produce data to write into triggered readout files that is not or
 * can not be stored in the <code>ReadoutDataManager</code> itself.
 * In this case, each driver is allowed to produce its own special
 * collections data via the method {@link
 * org.hps.readout.ReadoutDriver#getOnTriggerData(double)
 * getOnTriggerData(double)}, where the argument is the true trigger
 * time (i.e. the simulation time of the trigger event with a time
 * correction applied based on the time displacements of the driver
 * chain leading up to it). Drivers that require this functionality
 * may override the aforementioned method. This method is only called
 * on a trigger write-out event, and by default will return a value
 * of <code>null</code>. If a driver does not require this
 * functionality, the method may be ignored.</li>
 * <li><b>Declare special data time displacement:</b> Drivers must
 * also declare the total amount of time that they need to produce
 * their local on-trigger data. If no on-trigger data is produced,
 * this should be set to 0 ns.</li>
 * </ul>
 * The goal of a readout driver is to be able to obtain needed input
 * data from any collection, regardless of the relative time offsets
 * of those collections, and to output its own collections, without
 * needing to know any information about how the input collections
 * are generated. A driver should only need to know the name of the
 * input collection. This is accomplished by using the above declared
 * information in the <code>ReadoutDataManager</code> to handle time
 * offset alignment between data sets automatically.<br/><br/>
 * By knowing the what collections a driver needs, and how long it
 * takes to produce its output, the data manager can determine what
 * the "true" data time for each output collection from each driver
 * is. It then allows drivers to request data from within a given
 * "true" time range via the method {@link
 * org.hps.readout.ReadoutDataManager#getData(double, double, String, Class)
 * ReadoutDataManager.getData(double, double, String, Class)} without
 * that driver needing to manually sync itself with its dependencies.
 * It is this method that should then be used to obtain data, rather
 * than pulling it from the event stream directly.</br><br/>
 * Data is output similarly to the data manager directly. This is
 * done via the method {@link
 * org.hps.readout.ReadoutDataManager#addData(String, Collection, Class)
 * ReadoutDataManager.addData(String, Collection, Class)}. The needed
 * time correction is then automatically applied.<br/><br/>
 * A driver should make sure that its input collection is populated
 * to the needed time before requesting the data. This can be done
 * through the method {@link
 * org.hps.readout.ReadoutDataManager#checkCollectionStatus(String, double)
 * ReadoutDataManager.checkCollectionStatus(String, double)} for the
 * appropriate collection.<br/><br/>
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public abstract class ReadoutDriver extends Driver {
    /**
     * Stores the names of the collections which this driver requires
     * as input.
     */
    private final Set<String> dependencies = new HashSet<String>();
    /**
     * Specifies whether this collection should be written to the
     * output LCIO file.
     */
    private boolean persistent = false;
    /**
     * Specifies the time window after the trigger time in which this
     * collection data should be written.
     */
    private double readoutWindowAfter = Double.NaN;
    /**
     * Specifies the time window before the trigger time in which
     * this collection data should be written.
     */
    private double readoutWindowBefore = Double.NaN;
    
    /**
     * Instantiates the readout driver.
     */
    protected ReadoutDriver() {
        ReadoutDataManager.registerReadoutDriver(this);
    }
    
    /**
     * Specifies that the output of this readout driver depends on
     * the specified input collection.
     * @param collectionName - The name of the input collection.
     */
    protected void addDependency(String collectionName) {
        dependencies.add(collectionName);
    }
    
    /**
     * Indicates whether this collection is persisted into the output
     * LCIO file or not.
     * @return Returns <code>true</code> if the collection is
     * persisted, and <code>false</code> if it is not.
     * @throws UnsupportedOperationException Occurs if a particular
     * implementing driver class does not support persisted readout.
     */
    protected boolean isPersistent() throws UnsupportedOperationException {
        return persistent;
    }
    
    /**
     * Returns a {@link java.util.Collection Collection} of type
     * {@link java.lang.String String} containing the names of the
     * input collections used by this driver.
     * @return Returns a collection of <code>String</code> objects
     * representing the driver input collection names.
     */
    protected Collection<String> getDependencies() {
        return dependencies;
    }
    
    /**
     * Gets the {@link org.lcsim.geometry.IDDecoder IDDecoder} needed
     * to read the cell IDs for the objects produced by the indicated
     * collection. This will result in an exception if the requested
     * collection is not managed by this driver.
     * @return Either returns the ID decoder used by the specified
     * collection.
     * @throws IllegalArgumentException Occurs if the requested
     * collection is not managed by this driver.
     * @throws UnsupportedOperationException Occurs if there is no
     * <code>IDDecoder</code> object for the specified collection.
     */
    protected IDDecoder getIDDecoder(String collectionName) throws IllegalArgumentException, UnsupportedOperationException {
        throw new UnsupportedOperationException("IDDecoder objects are not supported by ReadoutDriver class \"" + this.getClass().getSimpleName() + "\".");
    }
    
    /**
     * Generates a {@link java.util.Collection Collection} containing
     * any special output data produced by the driver that should be
     * included in triggered output.<br/><br/>
     * By default, this outputs <code>null</code>. Individual drivers
     * must override the method as needed.
     * @param triggerTime - The time at which the trigger occurred.
     * @return Returns a collection containing all special output
     * data.
     */
    protected Collection<TriggeredLCIOData<?>> getOnTriggerData(double triggerTime) {
        return null;
    }
    
    /**
     * Gets the time window after the trigger time in which objects
     * from this collection will be written into the output file.
     * @return Returns the post-trigger readout window in units of
     * nanoseconds.
     * @throws UnsupportedOperationException Occurs if a particular
     * implementing driver class does not support persisted readout,
     * or does not support custom readout windows.
     */
    protected double getReadoutWindowAfter() throws UnsupportedOperationException {
        return readoutWindowAfter;
    }
    
    /**
     * Gets the time window before the trigger time in which objects
     * from this collection will be written into the output file.
     * @return Returns the pre-trigger readout window in units of
     * nanoseconds.
     * @throws UnsupportedOperationException Occurs if a particular
     * implementing driver class does not support persisted readout,
     * or does not support custom readout windows.
     */
    protected double getReadoutWindowBefore() throws UnsupportedOperationException {
        return readoutWindowBefore;
    }
    
    /**
     * Specifies the amount of simulation time that the driver needs
     * to produce its output. This indicates that the driver's
     * present output was generated based on input a time equal to
     * ({@link org.hps.readout.ReadoutDataManager#getCurrentTime()
     * getCurrentTime()} - <code>getTimeDisplacement()</code>).
     * @return Returns the time displacement of output data as a
     * <code>double</code>.
     */
    protected abstract double getTimeDisplacement();
    
    /**
     * Specifies the amount of simulation time that the driver needs
     * to produce special on-trigger data.
     * @return Returns the time displacement of output data as a
     * <code>double</code>.
     */
    protected abstract double getTimeNeededForLocalOutput();
    
    /**
     * Sets whether this data collection should be written to the
     * output LCIO file.
     * @param state - <code>true</code> means that the collection is
     * persisted, and <code>false</code> that it is not.
     * @throws UnsupportedOperationException Occurs if a particular
     * implementing driver class does not support persisted readout.
     */
    public void setPersistent(boolean state) throws UnsupportedOperationException {
        persistent = state;
    }
    
    /**
     * Set the time window after the trigger time in which this
     * collection data should be written.
     * @param value - The length of the readout window after the
     * trigger time, in units of nanoseconds.
     * @throws UnsupportedOperationException Occurs if a particular
     * implementing driver class does not support persisted readout,
     * or does not support custom readout windows.
     */
    public void setReadoutWindowAfter(double value) throws UnsupportedOperationException {
        readoutWindowAfter = value;
    }
    
    /**
     * Set the time window before the trigger time in which this
     * collection data should be written.
     * @param value - The length of the readout window before the
     * trigger time, in units of nanoseconds.
     * @throws UnsupportedOperationException Occurs if a particular
     * implementing driver class does not support persisted readout,
     * or does not support custom readout windows.
     */
    public void setReadoutWindowBefore(double value) throws UnsupportedOperationException {
        readoutWindowBefore = value;
    }
}