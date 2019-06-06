package org.hps.recon.tracking.ztrack;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public interface TrackFitter {

    /**
     * \brief Main function to be implemented for concrete track fitter
     * algorithm. \param[in,out] track Pointer to track to be fitted. \param[in]
     * downstream Track fit direction. \return Status code.
     */
    public Status Fit(
            ZTrack track,
            boolean downstream);
}
