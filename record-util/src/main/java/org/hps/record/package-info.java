/**
 * The package <code>org.hps.record</code> and its sub-packages use the <a
 * href="http://java.freehep.org/freehep-record/">FreeHep Record</a> module to implement a flexible record processing
 * backend. Its current primary usage is providing the record processing chain for the HPS Monitoring Application, but
 * it can be used stand-alone outside of that framework. The primary class for user interaction is the
 * {@link org.hps.record.composite.CompositeLoop} class which implements a record loop that can convert
 * <code>EtEvent</code> objects to <code>EvioEvent</code> objects and then finally build LCSim event, or
 * <code>EventHeader</event> objects from the EVIO, using a series of adapter classes on the loop.  The loop implementation
 * is flexible so that it may be configured to use an ET server, an EVIO file or an LCIO file for the record source.
 * The {@link org.hps.record.composite.CompositeLoopConfiguration} class should be used to configure the loop by the user.
 */
package org.hps.record;