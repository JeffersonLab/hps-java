package org.hps.analysis.examples;

import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;

/**
 * This is the stupidest possible Driver that could ever be written.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class DummyAnalysisDriver extends Driver {    
    public void process(EventHeader event) {
        System.out.println("Hello from the DummyAnalysisDriver!  This is event #" + event + " in run # " + event.getRunNumber() + ".");
    }

}
