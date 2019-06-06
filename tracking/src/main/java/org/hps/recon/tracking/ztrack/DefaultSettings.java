package org.hps.recon.tracking.ztrack;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class DefaultSettings {

    // Track propagation settings
    static final double LINE_EXTRAPOLATION_START_Z = 450.; // z coordinate [cm] after which linear track extrapolation starts
    static final double ENERGY_LOSS_CONST = 0.00354; // energy lost  finalant [GeV/c] used in the simple energy loss calculation
    static final double MINIMUM_PROPAGATION_DISTANCE = 1e-6; // minimum propagation distance [cm]
    static final double MAXIMUM_NAVIGATION_DISTANCE = 25.; // maximum distance used in the detector navigation
}
