package org.hps.detector;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;

/**
 * Test loading SVT alignment constants into the Java detector model.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public class SvtAlignmentTest extends TestCase {

    public void testSvtAlignment() throws Exception {
        final DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
        // manager.setDetector("HPS-EngRun2015-1_5mm-v1", 5259);
        manager.setDetector("HPS-EngRun2015-1_5mm-v1", 0);
    }
}
