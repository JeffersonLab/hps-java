/**
 * <p>
 * The HPS conditions package provides facilities for accessing time dependent conditions
 * for a detector at runtime using a framework built on the LCSim Conditions System package.
 * The {@link DatabaseConditionsReader} has a set of converters for reading data from
 * tables using SQL queries and creating appropriate, typed objects for them.
 * </p>
 * 
 * <p>
 * There is a chain of readers that is called by the manager which looks like:
 * </p>
 * 
 * <p>
 * DetectorConditionsReader => DatabaseConditionsReader => ConditionsReader
 * </p>
 * 
 * <p>
 * The {@link DetectorConditionsReader} extends the {@link DatabaseConditionsReader} and 
 * handles compact.xml files or other files embedded as jar resources in the detector directories 
 * (e.g. from detector-data).  It is the first class which attempts to resolve conditions by 
 * name and type.  When it does not find a set of condition data, it will call its
 * super class's method, which will then attempt to find the data.
 * </p>
 * 
 * <p>
 * The {@link DatabaseConditionsReader} in fact mostly relies on built-in behavior of the 
 * {@link org.lcsim.conditions.ConditionsReader} class, which has a set of converters
 * registered on it.  These converters perform the translation from database table rows
 * to Java objects.
 * </p>
 * 
 * <p>
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * </p>
 */
package org.hps.conditions;