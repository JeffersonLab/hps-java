package org.hps.recon.ecal.cluster;

import java.util.Map;
import java.util.Map.Entry;

/**
 * This is the basic implementation of the {@link NumericalCuts} interface.
 * 
 * @see NumericalCuts
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class NumericalCutsImpl implements NumericalCuts {

    String[] names;
    double[] values;
    double[] defaultValues;

    public NumericalCutsImpl(String[] names, double[] defaultValues) {
        this.names = names;
        this.defaultValues = defaultValues;
        this.values = this.defaultValues;
    }

    @Override
    public void setValues(double[] values) {
        if (values.length != this.names.length) {
            throw new IllegalArgumentException("The values array has the wrong length: " + values.length);
        }
        this.values = values;
    }

    @Override
    public double[] getValues() {
        return values;
    }

    @Override
    public double getValue(String name) {
        int index = indexFromName(name);
        if (index == -1) {
            throw new IllegalArgumentException("There is no cut called " + name + " defined by this clusterer.");
        }
        return getValue(index);
    }

    @Override
    public double getValue(int index) {
        if (index > values.length || index < 0) {
            throw new IndexOutOfBoundsException("The index " + index + " is out of bounds for cuts array.");
        }
        return values[index];
    }

    @Override
    public String[] getNames() {
        return names;
    }

    @Override
    public void setValue(int index, double value) {
        values[index] = value;
    }

    @Override
    public boolean isDefaultValues() {
        return values == defaultValues;
    }

    @Override
    public double[] getDefaultValues() {
        return defaultValues;
    }

    @Override
    public void setValue(String name, double value) {
        int index = indexFromName(name);
        values[index] = value;
    }

    @Override
    public void setValues(Map<String, Double> valueMap) {
        for (Entry<String, Double> entry : valueMap.entrySet()) {
            this.setValue(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Get the index of a cut from its name.
     * 
     * @param name The name of the cut.
     * @return The index of the cut from the name.
     */
    protected int indexFromName(String name) {
        for (int index = 0; index < values.length; index++) {
            if (getNames()[index] == name) {
                return index;
            }
        }
        return -1;
    }
}
