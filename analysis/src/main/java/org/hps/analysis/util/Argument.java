package org.hps.analysis.util;

/**
 * Class <code>Argument</code> is a simple representation of a
 * command line argument. It is itself abstract, and only defines the
 * attributes common to all argument types.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
abstract class Argument {
    private final boolean required;
    private final String fullOption;
    private final String shortOption;
    private final String description;
    
    /**
     * Sets the values common to all <code>Argument</code> objects.
     * Note that one of either <code>shortOption</code> or
     * <code>fullOption</code> must be defined.
     * @param shortOption - Defines the short option text used to
     * declare this argument on the command line.
     * @param fullOption - Defines the full option text used to
     * declare this argument on the command line.
     * @param description - Defines the description text used for
     * displaying usage information.
     * @param required - Specifies whether or not the argument is
     * required for the associated command to run.
     */
    Argument(String shortOption, String fullOption, String description, boolean required) {
        if(shortOption == null && fullOption == null) {
            throw new IllegalArgumentException("At least one argument must be defined.");
        }
        
        this.required = required;
        this.fullOption = fullOption;
        this.shortOption = shortOption;
        this.description = description;
    }
    
    /**
     * Specifies whether or not this argument takes a value, or it
     * must simply be declared. i.e. <code>-a $VALUE</code> takes a
     * value; <code>-h</code> does not.
     * @return Returns <code>true</code> if the argument takes a
     * value and <code>false</code> if it is not.
     */
    abstract boolean acceptsValues();
    
    /**
     * Specifies whether this argument permits multiple instances of
     * itself. Arguments that do will compile a list of all values
     * given, while arguments that do not will produce an exception
     * if they are seen more than once.
     * @return Returns <code>true</code> if the argument allows
     * multiple instances of itself and <code>false</code> if it is not.
     */
    abstract boolean allowsMultipleInstances();
    
    /**
     * Indicates whether or not the argument is required.
     * @return Returns <code>true</code> if the argument is required
     * and <code>false</code> if it is not.
     */
    boolean isRequired() {
        return required;
    }
    
    /**
     * Gets the description of the argument as displayed in the usage
     * information.
     * @return Returns the usage information description of this
     * argument.
     */
    String getDescription() {
        return description;
    }
    
    /**
     * Gets the short form of the command line argument.
     * @return Returns the short form of the toption text for this
     * argument.
     */
    String getShortOptionText() {
        return shortOption;
    }
    
    /**
     * Gets the full form of the command line argument.
     * @return Returns the full form of the option text for this
     * argument.
     */
    String getFullOptionText() {
        return fullOption;
    }
    
    /**
     * Specifies whether a value was given for this argument.
     * @return Returns <code>true</code> if a value exists for the
     * argument and <code>false</code> if it does not.
     */
    abstract boolean isDefined();
}