/**
 * The Monitoring Application is a flexible framework for monitoring the event processing chain of the HPS experiment in
 * an online environment. It implements the conversion of ET byte buffers to EVIO and then the building of LCIO events
 * from the raw EVIO data.
 * <p>
 * It provides three primary GUI components:
 * <ul>
 * <li>run dashboard showing basic information about data received</li>
 * <li>tab panel showing various information panels</li>
 * <li>plotting window that displays any plots generated through the AIDA API</li>
 * </ul>
 * <p>
 * The FreeHep framework is used extensively for the record processing. Every part of the event processing chain uses
 * the <a href="http://java.freehep.org/freehep-record/">freehep-record</a> module to manage the flow of records and
 * activate any processors that are listening on the record loops.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
package org.hps.monitoring.application;

