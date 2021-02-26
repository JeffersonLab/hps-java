package org.hps.test.it;

/**
 * Test Moller reconstruction on 2016 data
 */
public class PhysRun2016MollerReconTest extends ReconTest {

    static final String DETECTOR = "HPS-PhysicsRun2016-v5-3-fieldmap_v4_globalAlign";
    static final String TEST_FILE_NAME = "hps_007796_mollerskim.evio";
    static final String STEERING = "/org/hps/steering/recon/legacy_drivers/PhysicsRun2016FullRecon.lcsim";
    static final int NEVENTS = 5000;
    static final long MAX_EVENT_TIME = 100;

    public PhysRun2016MollerReconTest() {
        super(PhysRun2016MollerReconTest.class,
                DETECTOR,
                TEST_FILE_NAME,
                STEERING,
                NEVENTS,
                new PhysRun2016MollerRecon(),
                DEFAULT_TOLERANCE,
                MAX_EVENT_TIME,
                true,
                true);
    }
}
