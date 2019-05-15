package org.hps.analysis.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * <code>MultipleValueArgument</code> represents a command line
 * argument that allows for a multiple instances of itself. It is
 * an implementation of {@link org.hps.analysis.util.Argument
 * Argument}.
 * 
 * @see org.hps.analysis.util.Argument
 * @author Kyle McCarty
 */
public class MultipleValueArgument extends Argument implements Iterable<String> {
    private List<String> values = new ArrayList<String>();
    
    /**
     * Instantiates a new instance of a multiple-value argument.
     * @param shortOption - Defines the short option text used to
     * declare this argument on the command line.
     * @param fullOption - Defines the full option text used to
     * declare this argument on the command line.
     * @param description - Defines the description text used for
     * displaying usage information.
     * @param required - Specifies whether or not the argument is
     * required for the associated command to run.
     */
    MultipleValueArgument(String shortOption, String fullOption, String description, boolean required) {
        super(shortOption, fullOption, description, required);
    }
    
    @Override
    boolean acceptsValues() {
        return true;
    }
    
    /**
     * Adds a new value associated with this argument.
     * @param value - The value to add.
     */
    void addValue(String value) {
        values.add(value);
    }
    
    /**
     * Adds all objects in the set as values associated with this
     * argument.
     * @param values A {@link java.util.Collection Collection} of
     * values associated with this argument.
     */
    void addAllValues(Collection<String> values) {
        values.addAll(values);
    }
    
    @Override
    boolean allowsMultipleInstances() {
        return true;
    }
    
    /**
     * Gets all of the values associated with this argument.
     * @return Returns an {@link java.util.List List} of all the
     * values associated with this argument.
     */
    List<String> getValues() {
        return values;
    }
    
    @Override
    boolean isDefined() {
        return !values.isEmpty();
    }
    
    /**
     * Removes a value from being associated with this argument.
     * @param value - The value to remove.
     */
    void removeValue(String value) {
        values.remove(value);
    }
    
    /**
     * Removes all the values in a {@link java.util.Collection
     * Collection} from being associated with this argument.
     * @param values - The values to remove.
     */
    void removeAllValues(Collection<String> values) {
        values.removeAll(values);
    }
    
    @Override
    public Iterator<String> iterator() {
        return values.iterator();
    }
}