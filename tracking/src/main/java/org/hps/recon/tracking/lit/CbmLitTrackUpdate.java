package org.hps.recon.tracking.lit;

/**
 *
 * @author Norman A. Graf
 *
 * @version $Id:
 */
public interface CbmLitTrackUpdate {

    /**
     * \brief Main function to be implemented for concrete track update
     * algorithm. \param[in] pParamIn Pointer to input track parameter.
     * \param[out] pParamOut Pointer to output track parameter. \parma[in] pHit
     * Pointer to hit. \param[out] chiSq Output value of contribution to
     * chi-square. \return Status code.
     */
    LitStatus Update(
            CbmLitTrackParam parIn,
            CbmLitTrackParam parOut,
            CbmLitHit hit,
            double[] chiSq);

    /**
     * \brief Main function to be implemented for concrete track update
     * algorithm. \param[in,out] pParam Pointer to input/output track parameter.
     * \param[in] pHit Pointer to hit. \param[out] chiSq Output value of
     * contribution to chi-square. \return Status code.
     */
    LitStatus Update(
            CbmLitTrackParam par,
            CbmLitHit hit,
            double[] chiSq);
}
