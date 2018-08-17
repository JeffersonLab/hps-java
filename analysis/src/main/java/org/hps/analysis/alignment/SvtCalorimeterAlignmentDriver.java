package org.hps.analysis.alignment;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import java.util.List;
import org.hps.recon.tracking.TrackStateUtils;
import org.hps.recon.tracking.TrackType;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author Norman A. Graf
 */
public class SvtCalorimeterAlignmentDriver extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    private IHistogram2D trkAtEcalXvsNSigmaTop = aida.histogram2D("trackY at Ecal vs nSigma top", 100, 0., 6., 500, 20., 60.);
    private IHistogram2D trkAtEcalXvsNSigmaBottom = aida.histogram2D("-trackY at Ecal vs nSigma bottom", 100, 0., 6., 500, 20., 60.);

    protected void process(EventHeader event) {
        List<ReconstructedParticle> rpList = event.get(ReconstructedParticle.class, "FinalStateParticles");
        for (ReconstructedParticle rp : rpList) {

            if (!TrackType.isGBL(rp.getType())) {
                continue;
            }

            // require both track and cluster
            if (rp.getClusters().size() != 1) {
                continue;
            }

            if (rp.getTracks().size() != 1) {
                continue;
            }

            double nSigma = rp.getGoodnessOfPID();
            Track t = rp.getTracks().get(0);
            TrackState trackAtEcal = TrackStateUtils.getTrackStateAtECal(t);
            double[] tposAtEcal = trackAtEcal.getReferencePoint();

            // look for calorimeter edge wrt SVT
            if (tposAtEcal[2] > 0) {
                trkAtEcalXvsNSigmaTop.fill(nSigma, tposAtEcal[2]);
            } else {
                trkAtEcalXvsNSigmaBottom.fill(nSigma, -tposAtEcal[2]);
            }
        }
    }

    protected void endOfData() {
        //let's try some splitting and fitting
        IAnalysisFactory af = IAnalysisFactory.create();
        IHistogramFactory hf = af.createHistogramFactory(af.createTreeFactory().create());
        IHistogram1D[] bottomSlices = new IHistogram1D[25];
        IHistogram1D[] topSlices = new IHistogram1D[25];
        for (int i = 0; i < 25; ++i) {
            bottomSlices[i] = hf.sliceY("bottom slice " + i, trkAtEcalXvsNSigmaBottom, i);
            topSlices[i] = hf.sliceY("top slice " + i, trkAtEcalXvsNSigmaTop, i);
        }
    }
}
