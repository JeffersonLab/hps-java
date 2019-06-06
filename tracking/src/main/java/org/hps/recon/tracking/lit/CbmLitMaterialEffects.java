package org.hps.recon.tracking.lit;

/**
 * Interface for material effects calculation algorithm.
 *
 * @author Norman A. Graf
 *
 * @version $Id:
 */
public interface CbmLitMaterialEffects {

    /**
     * \brief Main function to be implemented for concrete material effects
     * calculation algorithm. \param[in,out] par Input/Output track parameters.
     * \param[in] mat Material information. \param[in] pdg PDG code \param[in]
     * downstream Propagation direction (true for downstream, false for
     * upstream). \return Status code.
     */
    LitStatus Update(
            CbmLitTrackParam par,
            CbmLitMaterialInfo mat,
            int pdg,
            boolean downstream);
}
