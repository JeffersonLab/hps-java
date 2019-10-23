package org.hps.analysis.util;

/**
 * <code>FlagArgument</code> is a simple implementation of {@link
 * org.hps.analysis.util.Argument Argument} that does not accept a
 * value. Instead, it simply tracks whether or not an instance of the
 * argument has been seen or not.
 * 
 * @see org.hps.analysis.util.Argument
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class FlagArgument extends Argument {
    private boolean defined = false;
    
    /**
     * Instantiates a new instance of a flag argument.
     * @param shortOption - Defines the short option text used to
     * declare this argument on the command line.
     * @param fullOption - Defines the full option text used to
     * declare this argument on the command line.
     * @param description - Defines the description text used for
     * displaying usage information.
     * @param required - Specifies whether or not the argument is
     * required for the associated command to run.
     */
    public FlagArgument(String shortOption, String fullOption, String description, boolean required) {
        super(shortOption, fullOption, description, required);
    }
    
    @Override
    boolean acceptsValues() {
        return false;
    }
    
    @Override
    boolean allowsMultipleInstances() {
        return false;
    }
    
    @Override
    boolean isDefined() {
        return defined;
    }
    
    /**
     * Sets whether or not an instance of this argument was seen.
     * @param state - <code>true</code> indicates that an instance of
     * this argument was seen and <code>false</code> that it was not.
     */
    void setDefined(boolean state) {
        defined = state;
    }
}