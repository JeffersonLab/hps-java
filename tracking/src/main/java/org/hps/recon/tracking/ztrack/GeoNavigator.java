package org.hps.recon.tracking.ztrack;

import java.util.List;

/**
 *
 * @author Norman A. Graf
 *
 * @version $Id:
 */
public interface GeoNavigator {

    /**
     * \brief Find intersection points with detector material in a certain
     * interval. \param[in] par Input track parameter. Define initial direction
     * and Z position. \param[in] zOut Output Z position [cm]. \param[out] inter
     * Output vector with crossed materials. \return Status code.
     */
    Status FindIntersections(
            ZTrackParam par,
            double zOut,
            List<MaterialInfo> inter);
}
