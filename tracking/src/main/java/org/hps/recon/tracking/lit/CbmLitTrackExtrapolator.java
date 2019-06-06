package org.hps.recon.tracking.lit;

/**
 *
 * @author Norman A. Graf
 *
 * @version $Id:
 */
public interface CbmLitTrackExtrapolator {

    /**
     * \brief Track parameters extrapolation with calculation of transport
     * matrix. \param[in] parIn Pointer to initial track parameters. \param[out]
     * parOut Pointer to output track parameters. \param[in] zOut Z position to
     * extrapolate to [cm]. \param[out] F Output transport matrix. If F == NULL
     * than transport matrix is not calculated. \return Extrapolation status.
     */
    public LitStatus Extrapolate(
            CbmLitTrackParam parIn,
            CbmLitTrackParam parOut,
            double zOut,
            double[] F);

    public LitStatus Extrapolate(
            CbmLitTrackParam parIn,
            CbmLitTrackParam parOut,
            DetectorPlane det,
            double[] F);

    public LitStatus Extrapolate(
            CbmLitTrackParam parIn,
            DetectorPlane det,
            double[] F);

    /**
     * brief Track parameters extrapolation with calculation of transport
     * matrix. \param[in,out] par Pointer to initial and output track
     * parameters. \param[in] zOut Z position to extrapolate to [cm].
     * \param[out] F Output transport matrix. If F == NULL than transport matrix
     * is not calculated. \return Extrapolation status.
     */
    public LitStatus Extrapolate(
            CbmLitTrackParam par,
            double zOut,
            double[] F);
}
