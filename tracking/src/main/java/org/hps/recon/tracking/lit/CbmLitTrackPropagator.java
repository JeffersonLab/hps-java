package org.hps.recon.tracking.lit;

/**
 *
 * @author Norman A. Graf
 *
 * @version $Id:
 */
public interface CbmLitTrackPropagator {

    /**
     * \brief Track parameter propagation. \param[in] parIn Pointer to initial
     * track parameters. \param[out] parOut Pointer to output track parameters.
     * \param[in] zOut Z position to propagate to [cm]. \param[in] pdg PDG code
     * of particle. \param[out] F Output transport matrix. If F == NULL than
     * transport matrix is not calculated. \param[out] length Length of track
     * segment. \return Propagation status.
     */
    LitStatus Propagate(
            CbmLitTrackParam parIn,
            CbmLitTrackParam parOut,
            double zOut,
            int pdg,
            double[] F,
            double[] length);

    LitStatus Propagate(
            CbmLitTrackParam parIn,
            CbmLitTrackParam parOut,
            DetectorPlane p,
            int pdg,
            double[] F,
            double[] length);

    /**
     * \brief Track parameter propagation. \param[in,out] par Pointer to initial
     * and output track parameters. \param[in] zOut Z position to propagate to
     * [cm]. \param[in] pdg PDG code of particle. \param[out] F Output transport
     * matrix. If F == NULL than transport matrix is not calculated. \param[out]
     * length Length of track segment. \return Propagation status.
     */
    LitStatus Propagate(
            CbmLitTrackParam par,
            double zOut,
            int pdg,
            double[] F,
            double[] length);

    LitStatus Propagate(
            CbmLitTrackParam par,
            DetectorPlane p,
            int pdg,
            double[] F,
            double[] length);
}
