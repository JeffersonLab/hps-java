package org.hps.analysis.wab;

import hep.aida.IAnalysisFactory;
import hep.aida.ITree;
import hep.aida.ITuple;
import hep.aida.ITupleFactory;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hps.recon.tracking.TrackType;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author Norman A. Graf
 */
public class CreateWabAnalysisTuple extends Driver {
    
    private AIDA aida = AIDA.defaultInstance();
    private ITuple wabTuple;
    
    String _reconstructedParticleCollectionName = "FinalStateParticles";
    
    @Override
    protected void startOfData() {
        IAnalysisFactory af = IAnalysisFactory.create();
        ITree tree = af.createTreeFactory().create();
        ITupleFactory tf = af.createTupleFactory(tree);
        String columnString = "int run; int event; float emom; float pEnergy";
        wabTuple = tf.create("wabtuple", "wabtuple", columnString, "");
    }
    
    protected void process(EventHeader event) {
        
        int runNumber = event.getRunNumber();
        int eventNumber = event.getEventNumber();
        // get the ReconstructedParticles in this event
        List<ReconstructedParticle> rps = event.get(ReconstructedParticle.class, _reconstructedParticleCollectionName);
        // now add in the FEE candidates
        rps.addAll(event.get(ReconstructedParticle.class, "OtherElectrons"));
        ReconstructedParticle electron = null;
        ReconstructedParticle photon = null;
        int nElectron = 0;
        int nPhoton = 0;
        for (ReconstructedParticle rp : rps) {
            // require the electron to have an associated ECal cluster
            if (rp.getParticleIDUsed().getPDG() == 11 && rp.getClusters().size() == 1 && TrackType.isGBL(rp.getType())) {
                electron = rp;
                nElectron++;
            }
            // require the photon to be fiducial
            if (rp.getParticleIDUsed().getPDG() == 22) {
                // not sure what is going on here with the missing hits...
                if (rp.getClusters().get(0) != null) {
                    if (rp.getClusters().get(0).getCalorimeterHits().get(0) != null) {
                        if (isFiducial(rp.getClusters().get(0).getCalorimeterHits().get(0))) {
                            photon = rp;
                            nPhoton++;
                        }
                    }
                }
            }
        }
        if (nElectron == 1 && nPhoton == 1) {
            float eMom = (float) electron.getMomentum().magnitude();
            float pEnergy = (float) photon.getEnergy();
            wabTuple.fill(0, runNumber);
            wabTuple.fill(1, eventNumber);
            wabTuple.fill(2, eMom);
            wabTuple.fill(3, pEnergy);
            wabTuple.addRow();
        }
    }
    
    @Override
    protected void endOfData() {
        try {
            aida.saveAs("tuple.root");
            aida.saveAs("tuple.aida");
        } catch (IOException ex) {
            Logger.getLogger(CreateWabAnalysisTuple.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public boolean isFiducial(CalorimeterHit hit) {
        int ix = hit.getIdentifierFieldValue("ix");
        int iy = hit.getIdentifierFieldValue("iy");
        // Get the x and y indices for the cluster.
        int absx = Math.abs(ix);
        int absy = Math.abs(iy);

        // Check if the cluster is on the top or the bottom of the
        // calorimeter, as defined by |y| == 5. This is an edge cluster
        // and is not in the fiducial region.
        if (absy == 5) {
            return false;
        }

        // Check if the cluster is on the extreme left or right side
        // of the calorimeter, as defined by |x| == 23. This is also
        // an edge cluster and is not in the fiducial region.
        if (absx == 23) {
            return false;
        }

        // Check if the cluster is along the beam gap, as defined by
        // |y| == 1. This is an internal edge cluster and is not in the
        // fiducial region.
        if (absy == 1) {
            return false;
        }

        // Lastly, check if the cluster falls along the beam hole, as
        // defined by clusters with -11 <= x <= -1 and |y| == 2. This
        // is not the fiducial region.
        if (absy == 2 && ix <= -1 && ix >= -11) {
            return false;
        }

        // If all checks fail, the cluster is in the fiducial region.
        return true;
    }
    
}
