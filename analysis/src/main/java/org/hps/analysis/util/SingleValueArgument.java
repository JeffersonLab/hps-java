package org.hps.analysis.util;

/**
 * <code>SingleValueArgument</code> represents a command line
 * argument that only allows for a single instance of itself. It is
 * an implementation of {@link org.hps.analysis.util.Argument
 * Argument}.
 * 
 * @see org.hps.analysis.util.Argument
 * @author Kyle McCarty
 */
public class SingleValueArgument extends Argument {
    private String value = null;
    
    /**
     * Instantiates a new instance of a single-value argument.
     * @param shortOption - Defines the short option text used to
     * declare this argument on the command line.
     * @param fullOption - Defines the full option text used to
     * declare this argument on the command line.
     * @param description - Defines the description text used for
     * displaying usage information.
     * @param required - Specifies whether or not the argument is
     * required for the associated command to run.
     */
    public SingleValueArgument(String shortOption, String fullOption, String description, boolean required) {
        super(shortOption, fullOption, description, required);
    }
    
    @Override
    boolean acceptsValues() {
        return true;
    }
    
    @Override
    boolean allowsMultipleInstances() {
        return false;
    }
    
    /**
     * Gets the value that was given with this argument.
     * @return Gets the value as a {@link java.util.String String}.
     */
    String getValue() {
        return value;
    }
    
    @Override
    boolean isDefined() {
        return value != null;
    }
    
    /**
     * Sets the value associated with this argument.
     * @param value - The value.
     */
    void setValue(String value) {
        this.value = value;
    }
}