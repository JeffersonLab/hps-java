package org.hps.recon.tracking.lit;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public interface CbmLitTrackFitter {

    /**
     * \brief Main function to be implemented for concrete track fitter
     * algorithm. \param[in,out] track Pointer to track to be fitted. \param[in]
     * downstream Track fit direction. \return Status code.
     */
    public LitStatus Fit(
            CbmLitTrack track,
            boolean downstream);
}
