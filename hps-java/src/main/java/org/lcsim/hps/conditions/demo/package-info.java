/**
 * Classes adding HPS-specific functionality to the <tt>org.lcsim</tt> conditions framework.
 * <p>
 * This is a demo implementation of the package. The goal was to minimize changes 
 * to the <tt>org.lcsim.conditions</tt> package required to support HPS extensions, and
 * to guarantee backward compatibility.
 * <p>
 * To enable reading conditions from the HPS database:
 * <ul>
 * <li>Add the line "ConditionsReader: org.lcsim.hps.conditions.HPSConditionsReader" to
 *     the <tt>detector.properties</tt> file in your conditions ZIP file or directory.</li>
 * <li>Install MySQL driver into your classpath.</li>
 * </ul>
 * Once the exact structure of the database and the calibration data to be retrieved is
 * known, more efficient and robust implementation will be coded.
 * 
 * @see org.lcsim.conditions
 */
package org.lcsim.hps.conditions.demo;

