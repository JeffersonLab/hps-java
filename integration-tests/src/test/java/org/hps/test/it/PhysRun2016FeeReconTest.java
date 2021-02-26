package org.hps.test.it;

/**
 * Test FEE reconstruction on 2016 data
 */
public class PhysRun2016FeeReconTest extends ReconTest {

    static final String DETECTOR = "HPS-PhysicsRun2016-v5-3-fieldmap_v4_globalAlign";
    static final String TEST_FILE_NAME = "hps_007796_feeskim.evio";
    static final String STEERING = "/org/hps/steering/recon/legacy_drivers/PhysicsRun2016FullRecon.lcsim";
    static final int NEVENTS = 5000;
    static final long MAX_EVENT_TIME = 35;

    public PhysRun2016FeeReconTest() {
        super(PhysRun2016FeeReconTest.class,
                DETECTOR,
                TEST_FILE_NAME,
                STEERING,
                NEVENTS,
                new PhysRun2016FeeRecon(),
                DEFAULT_TOLERANCE,
                MAX_EVENT_TIME,
                true,
                true);
    }
}
