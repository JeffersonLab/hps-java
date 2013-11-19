package org.lcsim.hps.conditions.demo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


/**
 * Run-specific set of calibration constants.
 * <p>
 * TEMPORARY IMPLEMENTATION - DEMO.
 *
 * @author onoprien
 * @version $Id: Calibration.java,v 1.1 2013/09/18 02:33:16 jeremy Exp $
 */
public class Calibration {
  
// -- Private parts : ----------------------------------------------------------
  
  private double[] _data;
  
// -- Construction : -----------------------------------------------------------
  
  Calibration(HashMap<Integer, Double> data, int maxChannel) {
    _data = new double[maxChannel+1];
    Arrays.fill(_data, -1.);
    for (Map.Entry<Integer, Double> e : data.entrySet()) {
      _data[e.getKey()] = e.getValue();
    }
  }
  
// -- Getters : ----------------------------------------------------------------
  
  /**
   * Returns calibration value for the specified channel.
   * @throws IllegalArgumentException if the current calibration does not contain data for the given channel.
   */
  public double get(int channel) {
    double out = -1.;
    try {
      out = _data[channel];
    } catch (IndexOutOfBoundsException x) {
      throw new IllegalArgumentException();
    }
    if (out < 0.) throw new IllegalArgumentException();
    return out;
  }
  
  /**
   * Returns calibration value for the specified channel.
   * Returns the specified default value if the current calibration does not contain data for the given channel.
   */
  public double get(int channel, double defaultValue) {
    double out = -1.;
    try {
      out = _data[channel];
    } catch (IndexOutOfBoundsException x) {
      return defaultValue;
    }
    if (out < 0.) return defaultValue;
    return out;
  }
  
// -- Overriding Object : ------------------------------------------------------
  
  public String toString() {
    StringBuilder s = new StringBuilder();
    for (double d : _data) s.append(d).append(" ");
    return s.toString();
  }
  
}
