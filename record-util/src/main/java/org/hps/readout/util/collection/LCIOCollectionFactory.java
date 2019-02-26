package org.hps.readout.util.collection;

import org.hps.readout.ReadoutDriver;

/**
 * Class <code>LCIOCollectionFactory</code> is responsible for
 * instantiating LCIO collection parameter objects. It allows all of
 * the relevant parameters to be set, at which point one the two
 * methods {@link
 * org.hps.readout.util.collection.LCIOCollectionFactory#produceLCIOCollection(Class)
 * produceLCIOCollection(Class)} or {@link
 * org.hps.readout.util.collection.LCIOCollectionFactory#produceManagedLCIOCollection(Class)
 * produceManagedLCIOCollection(Class)} may be called to generate the
 * actual object.<br/><br/>
 * Note that {@link
 * org.hps.readout.util.collection.ManagedLCIOCollection
 * ManagedLCIOCollection} objects are used by the {@link
 * org.hps.readout.ReadoutDataManager ReadoutDataManager} and
 * generally should not be instantiated by users. {@link
 * org.hps.readout.util.collection.LCIOCollection LCIOCollection}
 * collection class is intended to be defined by {@link
 * org.hps.readout.ReadoutDriver ReadoutDriver} objects for use in
 * either outputting on-triggered or defining their output to the
 * readout manager.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @see org.hps.readout.util.collection.LCIOCollection
 * @see org.hps.readout.util.collection.ManagedLCIOCollection
 */
public class LCIOCollectionFactory {
    private static Integer flags = null;
    private static Boolean persistent = null;
    private static double windowAfter = Double.NaN;
    private static double windowBefore = Double.NaN;
    private static double timeDisplacement = Double.NaN;
    private static String readoutName = null;
    private static String collectionName = null;
    private static ReadoutDriver productionDriver = null;
    
    /**
     * Clones an LCIO collection parameters object. If a parameter is
     * defined in the factory, that parameter will be used for the
     * new cloned object. Otherwise, the parameter value from the
     * argument object will be used instead.<br/><br/>
     * This method is intended to be used to create a new instance of
     * an LCIO collection parameters object with a new parameter
     * value.
     * @param baseParams - The set of default parameters.
     * @return Returns a new LCIO collection parameters object with
     * the same parameter values as the original, unless specifically
     * defined in the factory beforehand.
     */
    public static final <T> LCIOCollection<T> cloneCollection(LCIOCollection<T> baseParams) {
        // Set any undefined parameters to match the base object.
        if(collectionName == null) { setCollectionName(baseParams.getCollectionName()); }
        if(flags == null) { setFlags(baseParams.getFlags()); }
        if(productionDriver == null) { setProductionDriver(baseParams.getProductionDriver()); }
        if(readoutName == null) { setReadoutName(baseParams.getReadoutName()); }
        
        // Produce a new collection parameters object.
        return produceLCIOCollection(baseParams.getObjectType());
    }
    
    /**
     * Creates an {@link
     * org.hps.readout.util.collection.LCIOCollection LCIOCollection}
     * from the parameters set in the factory. The following
     * parameters are used for this object type:
     * <ul>
     * <li>Collection Name</li>
     * <li>Production Driver</li>
     * <li>Flags</li>
     * <li>Readout Name</li>
     * </ul>
     * Of these, the collection name and the production driver are
     * required and, if not set, will generate an exception.
     * <br/><br/>
     * The collection parameters object will be parameterized as per
     * the input class type argument.
     * <br/><br/>
     * Note that all parameters are reset automatically after a
     * collection parameters object is generated.
     * @param objectType - The class type of the objects represented
     * by this collection.
     * @return Returns an LCIO collection parameters object with the
     * specified parameters.
     * @param <T> - The type of object stored in the collection.
     */
    public static final <T> LCIOCollection<T> produceLCIOCollection(Class<T> objectType) {
        // Verify that the necessary parameters have been set.
        String errorMessage = verifyBaseCollectionParameters();
        if(errorMessage != null) {
            throw new IllegalArgumentException(errorMessage);
        }
        
        // Create the new object.
        LCIOCollection<T> collection  = new ManagedLCIOCollection<T>(collectionName, productionDriver, objectType, timeDisplacement,
                (flags == null ? 0 : flags.intValue()), readoutName);
        
        // Reset the factory.
        reset();
        
        // Return the result.
        return collection;
    }
    
    /**
     * Creates an {@link
     * org.hps.readout.util.collection.ManagedLCIOCollection
     * ManagedLCIOCollection} from the parameters set in the factory.
     * The following parameters are used for this object type:
     * <ul>
     * <li>Collection Name</li>
     * <li>Production Driver</li>
     * <li>Flags</li>
     * <li>Readout Name</li>
     * <li>Persistent</li>
     * <li>Time Displacement</li>
     * <li>Window Before</li>
     * <li>Window After</li>
     * </ul>
     * Of these, the collection name, the production driver, and the
     * time displacement are required and, if not set, will generate
     * an exception. <br/><br/>
     * The collection parameters object will be parameterized as per
     * the input class type argument.<br/><br/>
     * Note that all parameters are reset automatically after a
     * collection parameters object is generated.
     * @param objectType - The class type of the objects represented
     * by this collection.
     * @return Returns an LCIO collection parameters object with the
     * specified parameters.
     * @param <T> - The type of object stored in the collection.
     */
    public static final <T> ManagedLCIOCollection<T> produceManagedLCIOCollection(Class<T> objectType) {
        // Verify that the necessary parameters have been set.
        String errorMessage = verifyManagedCollectionParameters();
        if(errorMessage != null) {
            throw new IllegalArgumentException(errorMessage);
        }
        
        // Create the new object.
        ManagedLCIOCollection<T> collection  = new ManagedLCIOCollection<T>(collectionName, productionDriver, objectType, timeDisplacement,
                (flags == null ? 0 : flags.intValue()), readoutName);
        if(!Double.isNaN(windowAfter)) { collection.setWindowAfter(windowAfter); }
        if(!Double.isNaN(windowBefore)) { collection.setWindowBefore(windowBefore); }
        if(persistent != null) { collection.setPersistent(persistent.booleanValue()); }
        
        // Reset the factory.
        reset();
        
        // Return the result.
        return collection;
    }
    
    /**
     * Sets the name of the collection. This must be unique for each
     * collection.
     * @param value - The collection name.
     */
    public static final void setCollectionName(String value) {
        collectionName = value;
    }
    
    /**
     * Sets the LCIO readout flags for the collection. Flag
     * definitions can be found in the {@link
     * org.lcsim.lcio.LCIOConstants LCIOConstants} class.
     * @param value - The LCIO flags.
     */
    public static final void setFlags(int value) {
        flags = value;
    }
    
    /**
     * Defines the amount of time between when an object is created
     * within the simulation and when its originating truth data was
     * seen.<br/><br/>
     * This parameter only applies to managed LCIO collections, and
     * will have no effect otherwise.
     * @param value - The time displacement for the collection.
     */
    public static final void setGlobalTimeDisplacement(double value) {
        timeDisplacement = value;
    }
    
    /**
     * Sets all factory parameters which are not already defined to
     * the values of the argument LCIO collection. Parameters that
     * are not valid for the argument collection remain undefined
     * unless manually altered.
     * @param params - The source parameters object.
     */
    public static final void setParams(LCIOCollection<?> params) {
        setCollectionName(params.getCollectionName());
        setFlags(params.getFlags());
        setProductionDriver(params.getProductionDriver());
        setReadoutName(params.getReadoutName());
    }
    
    /**
     * Sets all factory parameters which are not already defined to
     * the values of the argument LCIO collection
     * @param params - The source parameters object.
     */
    public static final void setParams(ManagedLCIOCollection<?> params) {
        setParams((LCIOCollection<?>) params);
        setGlobalTimeDisplacement(params.getGlobalTimeDisplacement());
        setPersistent(params.isPersistent());
        setWindowAfter(params.getWindowAfter());
        setWindowBefore(params.getWindowBefore());
    }
    
    /**
     * Sets whether or not a managed collection should be persisted
     * to LCIO on a trigger event or not.<br/><br/>
     * This parameter only applies to managed LCIO collections, and
     * will have no effect otherwise.
     * @param value - <code>true</code> means that the collection
     * will be written to LCIO, and <code>false</code> that it will
     * not.
     */
    public static final void setPersistent(boolean state) {
        persistent = state;
    }
    
    /**
     * Defines which driver is responsible for producing the
     * collection.
     * @param driver - The production driver.
     */
    public static final void setProductionDriver(ReadoutDriver driver) {
        productionDriver = driver;
    }
    
    /**
     * Specifies the readout name for the collection. This is used to
     * obtain meta-data and other information about the geometry that
     * is associated with a collection object.
     * @param value - The readout name for the collection.
     */
    public static final void setReadoutName(String value) {
        readoutName = value;
    }
    
    /**
     * Sets a custom time window after the trigger time in which
     * collection objects will be written out into data. If not
     * defined, the default readout window and trigger offset values
     * will be used instead.<br/><br/>
     * This parameter only applies to managed LCIO collections, and
     * will have no effect otherwise.
     * @param value - The time window length in nanoseconds.
     */
    public static final void setWindowAfter(double value) {
        windowAfter = value;
    }
    
    /**
     * Sets a custom time window before the trigger time in which
     * collection objects will be written out into data. If not
     * defined, the default readout window and trigger offset values
     * will be used instead.<br/><br/>
     * This parameter only applies to managed LCIO collections, and
     * will have no effect otherwise.
     * @param value - The time window length in nanoseconds.
     */
    public static final void setWindowBefore(double value) {
        windowBefore = value;
    }
    
    /**
     * Resets all factory parameter values to undefined. Note that
     * this is done automatically after an LCIO collection parameters
     * object is generated, and does not need to be called manually.
     */
    public static final void reset() {
        flags = null;
        persistent = null;
        windowAfter = Double.NaN;
        windowBefore = Double.NaN;
        timeDisplacement = Double.NaN;
        readoutName = null;
        collectionName = null;
        productionDriver = null;
    }
    
    /**
     * Checks that all of the parameters needed to define a basic
     * LCIO collection are in fact defined.
     * @return If all parameters are defined, <code>null</code> is
     * returned. Otherwise, a {@link java.lang.String String} is
     * returned containing an error message.
     */
    private static final String verifyBaseCollectionParameters() {
        boolean setCollectionName = (collectionName != null);
        //boolean setProductionDriver = (productionDriver != null);
        
        if(setCollectionName) {// && setProductionDriver) {
            return null;
        } else {
            StringBuffer errorMessage = new StringBuffer("Error: The following parameters have not been set:");
            if(!setCollectionName) { errorMessage.append("\n\tCollection Name"); }
            //if(!setProductionDriver) { errorMessage.append("\n\tProduction Driver"); }
            return errorMessage.toString();
        }
    }
    
    /**
     * Checks that all of the parameters needed to define a managed
     * LCIO collection are in fact defined.
     * @return If all parameters are defined, <code>null</code> is
     * returned. Otherwise, a {@link java.lang.String String} is
     * returned containing an error message.
     */
    private static final String verifyManagedCollectionParameters() {
        String baseError = verifyBaseCollectionParameters();
        boolean setTimeDisplacement = (!Double.isNaN(timeDisplacement));
        
        if(baseError == null && !setTimeDisplacement) {
            return "Error: The following parameters have not been set:\n\tTime Displacement";
        } else if(baseError != null && !setTimeDisplacement) {
            return baseError + "\n\tTime Displacement";
        } else if(baseError != null && setTimeDisplacement) {
            return baseError;
        } else { return null; }
    }
}