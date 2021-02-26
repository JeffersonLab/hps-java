package org.hps.test.it;

/**
 *
 * @author Norman A. Graf
 */
public class PhysRun2016V0ReconTest extends ReconTest {

    static final String DETECTOR = "HPS-PhysicsRun2016-v5-3-fieldmap_v4_globalAlign";
    static final String TEST_FILE_NAME = "hps_007796_v0skim.evio";
    static final String STEERING = "/org/hps/steering/recon/legacy_drivers/PhysicsRun2016FullRecon.lcsim";
    static final int NEVENTS = 5000;
    static final long MAX_EVENT_TIME = -1;

    public PhysRun2016V0ReconTest() {
        super(PhysRun2016V0ReconTest.class,
                DETECTOR,
                TEST_FILE_NAME,
                STEERING,
                NEVENTS,
                new PhysRun2016V0Recon(),
                DEFAULT_TOLERANCE,
                MAX_EVENT_TIME,
                true,
                true);
    }
}
