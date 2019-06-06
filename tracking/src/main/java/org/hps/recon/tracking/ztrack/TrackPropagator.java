package org.hps.recon.tracking.ztrack;

/**
 *
 * @author Norman A. Graf
 *
 * @version $Id:
 */
public interface TrackPropagator {

    /**
     * \brief Track parameter propagation. \param[in] parIn Pointer to initial
     * track parameters. \param[out] parOut Pointer to output track parameters.
     * \param[in] zOut Z position to propagate to [cm]. \param[out] F Output
     * transport matrix. If F == NULL than transport matrix is not calculated.
     * \return Propagation status.
     */
    Status Propagate(
            ZTrackParam parIn,
            ZTrackParam parOut,
            double zOut,
            double[] F);

    Status Propagate(
            ZTrackParam parIn,
            ZTrackParam parOut,
            DetectorPlane p,
            double[] F);

    /**
     * \brief Track parameter propagation. \param[in,out] par Pointer to initial
     * and output track parameters. \param[in] zOut Z position to propagate to
     * [cm]. \param[out] F Output transport matrix. If F == NULL than transport
     * matrix is not calculated. \return Propagation status.
     */
    Status Propagate(
            ZTrackParam par,
            double zOut,
            double[] F
    );

    Status Propagate(
            ZTrackParam par,
            DetectorPlane p,
            double[] F);
}
