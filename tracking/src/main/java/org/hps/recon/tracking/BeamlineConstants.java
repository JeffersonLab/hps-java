package org.hps.recon.tracking;

public final class BeamlineConstants {

    private BeamlineConstants() {
    }

    public static final double ECAL_FACE = 1448.0; // mm Matches z of ECal clusters as returned in reconstruction Norman A. Graf, 3/8/2023
    public static final double ECAL_FACE_ENGINEERING_RUNS = 1394.0; // mm Email from Takashi Jan 15th, 2015
    public static final double ECAL_FACE_TESTRUN = 1524; // mm
    public static final double DIPOLE_EDGE_TESTRUN = 457.2 + 457.2; // 452.2 +
    // 462.2;
    // //914; //
    // mm
    public static final double DIPOLE_EDGELOW_TESTRUN = 0.; // 452.2 - 462.2; //
    // mm
    public static final double HARP_POSITION_TESTRUN = -674.062; // mm

    public static final double DIPOLE_EDGE_ENG_RUN = 457.2 + 1080 / 2;

    public static final double ECAL_TRACK_EXTRAPOLATION_START_Z = 800; // Starting point for track extrapolation to ECAL -- MWH 2024/4/15

    public static final double HODO_L1_ZPOS = 1103.5; //extracted from detector element mg--7/17/2019
    public static final double HODO_L2_ZPOS = 1115.5; //extracted from detector element mg--7/17/2019

}
