package org.hps.analysis.MC;

import junit.framework.TestCase;

/**
 *
 * @author Norman A> Graf
 */
public class GenerateSingleParticleStdhepEventsTest extends TestCase {

    public void testIt() throws Exception {
        String[] args = {"11", "true", "4.55", "-7.5", "10000"};
        GenerateSingleParticleStdhepEvents.main(args);
    }

}
