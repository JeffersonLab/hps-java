package org.hps.recon.tracking.lit;

import java.util.List;

/**
 *
 * @author Norman A. Graf
 *
 * @version $Id:
 */
public interface CbmLitGeoNavigator {

    /**
     * \brief Find intersection points with detector material in a certain
     * interval. \param[in] par Input track parameter. Define initial direction
     * and Z position. \param[in] zOut Output Z position [cm]. \param[out] inter
     * Output vector with crossed materials. \return Status code.
     */
    LitStatus FindIntersections(
            CbmLitTrackParam par,
            double zOut,
            List<CbmLitMaterialInfo> inter);
}
