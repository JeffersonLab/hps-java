/**
 * <p>
 * The Monitoring Application is a flexible framework for monitoring the 
 * event processing chain of the HPS experiment.  It implements the conversion
 * of ET byte buffers to EVIO and then the building of LCIO events from the 
 * raw EVIO data.
 * <p>
 * <p>  
 * It provides three primary GUI components:
 * </p> 
 * <ul>
 * <li>run dashboard showing basic information about data received</li>
 * <li>system status monitor that can monitor the status of specific subsystems</li>
 * <li>plotting window that displays any plots generated through the AIDA API</li>
 * </ul>
 * <p>
 * The FreeHep framework is used extensively for the record processing.  Every part
 * of the event processing chain uses the
 * <a href="http://java.freehep.org/freehep-record/">freehep-record</a>
 * module to manage the flow of records and activate any processors that are listening
 * on the record loops.
 * </p>
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
package org.hps.monitoring;