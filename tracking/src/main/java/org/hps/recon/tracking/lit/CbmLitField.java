package org.hps.recon.tracking.lit;

/**
 *
 * @author Norman A. Graf
 *
 * @version $Id:
 */
public interface CbmLitField {

    /**
     * \brief Return field value at (x,y,z) position. \param[in] x X coordinate
     * [cm]. \param[in] y Y coordinate [cm]. \param[in] z Z coordinate [cm].
     * \param[out] Bx Output Bx field value [Tesla]. \param[out] By Output By
     * field value [Tesla]. \param[out] Bz Output Bz field value [Tesla].
     */
    public void GetFieldValue(
            double x,
            double y,
            double z,
            double[] B);
}
