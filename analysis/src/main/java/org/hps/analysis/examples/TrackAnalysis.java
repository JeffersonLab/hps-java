package org.hps.analysis.examples;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import hep.physics.matrix.BasicMatrix;
import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Matrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealMatrix;

import org.lcsim.detector.tracker.silicon.HpsSiSensor; 
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.Identifier;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrack2DHit;
import org.lcsim.fit.helicaltrack.HelicalTrack3DHit;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;

//===> import org.hps.conditions.deprecated.SvtUtils;
import static org.hps.recon.tracking.CoordinateTransformations.transformVectorToTracking;
import org.hps.recon.tracking.TrackerHitUtils;

/**
 *
 * @author Richard Partridge & Matt Graham
 */
// TODO: This class needs to be cleaned up
public class TrackAnalysis {

    private enum HelixPar {

        Curvature, Phi0, DCA, Z0, Slope
    };
    private static final Hep3Vector axial = new BasicHep3Vector(0, 1, 0);

    private MCParticle _mcp = null;
    private int _nhits;
    private int _nbadhits;
    private double _purity;
    private MCParticle _mcpNew = null;
    private int _nhitsNew;
    private int _nbadhitsNew;
    private double _purityNew;
    private int _nAxialhits;
    private int _nZhits;
    private int _nbadAxialhits;
    private int _nbadZhits;
    private boolean _hasLayerOne;
    private List<Integer> badHitList = new ArrayList();
    private List<Integer> sharedHitList = new ArrayList();
    private List<Integer> trackLayerList = new ArrayList();
    private Map<MCParticle, HelicalTrackCross> badhits = new HashMap<MCParticle, HelicalTrackCross>();
    //  Create a map containing the number of hits for each MCParticle associated with the track
    private Map<MCParticle, Integer> mcmap = new HashMap<MCParticle, Integer>();
    private Map<MCParticle, Integer> mcmapAll = new HashMap<MCParticle, Integer>();
    private Map<MCParticle, Integer> mcmapAxial = new HashMap<MCParticle, Integer>();
    private Map<MCParticle, Integer> mcmapZ = new HashMap<MCParticle, Integer>();
    private int[] _nMCHitsPerLayer = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private int[] _nStripHitsPerLayer = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private Map<Integer, Hep3Vector> _hitLocationPerLayer = new HashMap<Integer, Hep3Vector>();

    /**
     * Creates a new instance of TrackAnalysis
     */
    public TrackAnalysis(Track trk, RelationalTable hittomc, RelationalTable rthtosimhit, RelationalTable hittostrip, RelationalTable hittorotated) {
        doAnalysis(trk, hittomc, rthtosimhit, hittostrip, hittorotated);
    }

    public TrackAnalysis(Track trk, RelationalTable hittomc) {
        doAnalysis(trk, hittomc, null, null, null);
    }

    private void doAnalysis(Track trk, RelationalTable hittomc, RelationalTable rthtosimhit, RelationalTable hittostrip, RelationalTable hittorotated) {

        //  Get the number of hits on the track
        _nhits = trk.getTrackerHits().size();

        _hasLayerOne = false;
        //  Loop over the hits on the track and make sure we have HelicalTrackHits (which contain the MC particle)
        for (TrackerHit hit : trk.getTrackerHits()) {

            //  get the set of MCParticles associated with this hit and update the hit count for each MCParticle
            Set<MCParticle> mclist = hittomc.allFrom(hit);
            for (MCParticle mcp : mclist) {
                Integer mchits = 0;
                if (mcmap.containsKey(mcp))
                    mchits = mcmap.get(mcp);
                mchits++;
                mcmap.put(mcp, mchits);
            }

            if (hit instanceof HelicalTrackCross)
                countHit((HelicalTrackCross) hit);
            else if (hit instanceof HelicalTrack2DHit)
                countHit((HelicalTrack2DHit) hit);
            else if (!(hit  instanceof HelicalTrack2DHit )) //probably SOITrackerHit
                countHit(hit, rthtosimhit, hittostrip, hittorotated);
        }

        //  Find the MCParticle that has the most hits on the track
        int nbest = 0;
        MCParticle mcbest = null;
        for (MCParticle mcp : mcmap.keySet()) {
            int count = mcmap.get(mcp);
            if (count > nbest) {
                nbest = count;
                mcbest = mcp;
            }
        }

        if (nbest > 0)
            _mcp = mcbest;
        _purity = (double) nbest / (double) _nhits;
        _nbadhits = _nhits - nbest;

//single strip layer accounting.
        int nbestAll = 0;
        MCParticle mcbestAll = null;
        for (MCParticle mcp : mcmapAll.keySet()) {
            int count = mcmapAll.get(mcp);
            if (count > nbestAll) {
                nbestAll = count;
                mcbestAll = mcp;
            }
        }

        if (nbestAll > 0)
            _mcpNew = mcbestAll;
        _purityNew = (double) nbestAll / (double) _nhitsNew;
        _nbadhitsNew = _nhitsNew - nbestAll;

        for (TrackerHit hit : trk.getTrackerHits())
            if (hit instanceof HelicalTrackCross)
                checkForBadHit((HelicalTrackCross) hit);

        if (_nAxialhits > 0)
            if (mcmapAxial.containsKey(_mcpNew))
                _nbadAxialhits = _nAxialhits - mcmapAxial.get(_mcpNew);
            else
                _nbadAxialhits = _nAxialhits;
        if (_nZhits > 0)
            if (mcmapZ.containsKey(_mcpNew))
                _nbadZhits = _nZhits - mcmapZ.get(_mcpNew);
            else
                _nbadZhits = _nZhits;
    }

    private void countHit(HelicalTrackCross cross) {
        List<HelicalTrackStrip> clusterlist = cross.getStrips();

        for (HelicalTrackStrip cl : clusterlist) {
            int layer = cl.layer();
            if (layer == 1)
                _hasLayerOne = true;

            _nStripHitsPerLayer[layer - 1] = cl.rawhits().size();
            _hitLocationPerLayer.put(layer, clusterPosition(cl));
            _nhitsNew++;
            double axdotu = VecOp.dot(cl.u(), axial);
//            System.out.println(new BasicHep3Vector(cross.getPosition()).toString() + cl.u());
            boolean isAxial = false;
            if (axdotu > 0.5) {
                isAxial = true;
                _nAxialhits++;
            } else
                _nZhits++;
            List<MCParticle> mcPartList = cl.MCParticles();
            _nMCHitsPerLayer[layer - 1] = mcPartList.size();
            for (MCParticle mcp : mcPartList) {
                Integer mchits = 0;
                if (mcmapAll.containsKey(mcp))
                    mchits = mcmapAll.get(mcp);
                mchits++;
                mcmapAll.put(mcp, mchits);
                if (isAxial) {
                    Integer mchitsAxial = 0;
                    if (mcmapAxial.containsKey(mcp))
                        mchitsAxial = mcmapAxial.get(mcp);
                    mchitsAxial++;
                    mcmapAxial.put(mcp, mchitsAxial);
                } else {
                    Integer mchitsZ = 0;
                    if (mcmapZ.containsKey(mcp))
                        mchitsZ = mcmapZ.get(mcp);
                    mchitsZ++;
                    mcmapZ.put(mcp, mchitsZ);
                }
            }
        }
    }

    private void countHit(TrackerHit hit, RelationalTable rthtosimhit, RelationalTable hittostrip, RelationalTable hittorotated) {
        TrackerHit unrotatedHit = (TrackerHit) hittorotated.from(hit);
//        System.out.println("ID: " + unrotatedHit.getCellID());
        Set<TrackerHit> hitlist = hittostrip.allFrom(unrotatedHit);
//        System.out.println("size: " + hitlist.size());
        for (TrackerHit cl : hitlist) {
            int layer = -1;
            int module = -1;
            List<RawTrackerHit> rawHits = cl.getRawHits();
//                System.out.println("RawHits: " + rawHits.size());
            for (RawTrackerHit rawHit : rawHits) {
//                    System.out.println(rawHit.getCellID());
                IIdentifier id = new Identifier(rawHit.getCellID());
                //===> int newLayer = SvtUtils.getInstance().getHelper().getValue(id, "layer");
                int newLayer = ((HpsSiSensor) rawHit.getDetectorElement()).getLayerNumber();
                if (layer != -1 && layer != newLayer)
                    System.out.format("TrackerHit has hits from multiple layers: %d and %d\n", layer, newLayer);
                layer = newLayer;
                //===> int newModule = SvtUtils.getInstance().getHelper().getValue(id, "module");
                int newModule = ((HpsSiSensor) rawHit.getDetectorElement()).getModuleNumber();
                if (module != -1 && module != newModule)
                    System.out.format("TrackerHit has hits from multiple modules: %d and %d\n", module, newModule);
                module = newModule;
//                    System.out.println(SvtUtils.getInstance().getHelper().getValue(id, "strip"));
            }

            if (layer == 1)
                _hasLayerOne = true;
            DiagonalizedCovarianceMatrix covariance = new DiagonalizedCovarianceMatrix(cl);
            _nStripHitsPerLayer[layer - 1] = cl.getRawHits().size();
            _hitLocationPerLayer.put(layer, new BasicHep3Vector(hit.getPosition()));
            _nhitsNew++;

            double axdotu = VecOp.dot(transformVectorToTracking(covariance.getMeasuredVector()), axial);
//            System.out.println(transformVectorToTracking(new BasicHep3Vector(cl.getPosition())).toString() + transformVectorToTracking(covariance.getMeasuredVector()));
            boolean isAxial = false;
            if (axdotu > 0.5) {
                isAxial = true;
                _nAxialhits++;
            } else
                _nZhits++;
            //  get the set of MCParticles associated with this hit and update the hit count for each MCParticle

            Set<MCParticle> mcPartList = new HashSet<MCParticle>();
            for (RawTrackerHit rawHit : rawHits) {
                Set<SimTrackerHit> simhits = (Set<SimTrackerHit>) rthtosimhit.allFrom(rawHit);
                for (SimTrackerHit simhit : simhits)
                    if (simhit != null && simhit.getMCParticle() != null)
                        mcPartList.add(simhit.getMCParticle());
            }
//            System.out.println("MCParticle count: " + mcPartList.size());
            _nMCHitsPerLayer[layer - 1] = mcPartList.size();
            for (MCParticle mcp : mcPartList) {
                Integer mchits = 0;
                if (mcmapAll.containsKey(mcp))
                    mchits = mcmapAll.get(mcp);
                mchits++;
                mcmapAll.put(mcp, mchits);
                if (isAxial) {
                    Integer mchitsAxial = 0;
                    if (mcmapAxial.containsKey(mcp))
                        mchitsAxial = mcmapAxial.get(mcp);
                    mchitsAxial++;
                    mcmapAxial.put(mcp, mchitsAxial);
                } else {
                    Integer mchitsZ = 0;
                    if (mcmapZ.containsKey(mcp))
                        mchitsZ = mcmapZ.get(mcp);
                    mchitsZ++;
                    mcmapZ.put(mcp, mchitsZ);
                }
            }
        }
    }

    private void countHit(HelicalTrack2DHit hit2d) {
        _nhitsNew++;
        _nAxialhits++;
        List<MCParticle> mcPartList = hit2d.getMCParticles();
        //assume that lone hits are all axial
        boolean isAxial = true;
        for (MCParticle mcp : mcPartList) {
            Integer mchits = 0;
            if (mcmapAll.containsKey(mcp))
                mchits = mcmapAll.get(mcp);
            mchits++;
            mcmapAll.put(mcp, mchits);
            Integer mchitsAxial = 0;
            if (mcmapAxial.containsKey(mcp))
                mchitsAxial = mcmapAxial.get(mcp);
            mchitsAxial++;
            mcmapAxial.put(mcp, mchitsAxial);
        }
    }

    private void checkForBadHit(HelicalTrackCross cross) {
        List<HelicalTrackStrip> clusterlist = cross.getStrips();
        for (HelicalTrackStrip cl : clusterlist) {
            trackLayerList.add(cl.layer());
            if (!(cl.MCParticles().contains(_mcpNew))) {
                badHitList.add(cl.layer());
                badhits.put(_mcpNew, cross);
            }
            if (cl.MCParticles().size() > 1)
                sharedHitList.add(cl.layer());
        }
    }

    public static Hep3Vector clusterPosition(HelicalTrackStrip cl) {
        Hep3Vector corigin = cl.origin();
        Hep3Vector u = cl.u();
        double umeas = cl.umeas();
        Hep3Vector uvec = VecOp.mult(umeas, u);
        return VecOp.add(corigin, uvec);

    }

    public MCParticle getMCParticle() {
        return _mcp;
    }

    public int getNHits() {
        return _nhits;
    }

    public int getNBadHits() {
        return _nbadhits;
    }

    public double getPurity() {
        return _purity;
    }

    public MCParticle getMCParticleNew() {
        return _mcpNew;
    }

    public int getNHitsNew() {
        return _nhitsNew;
    }

    public int getNAxialHits() {
        return _nAxialhits;
    }

    public int getNZHits() {
        return _nZhits;
    }

    public int getNBadHitsNew() {
        return _nbadhitsNew;
    }

    public double getPurityNew() {
        return _purityNew;
    }

    public int getNBadAxialHits() {
        return _nbadAxialhits;
    }

    public int getNBadZHits() {
        return _nbadZhits;
    }

    public boolean hasLayerOne() {
        return _hasLayerOne;
    }

    public Hep3Vector getClusterPosition(Integer layer) {
        return _hitLocationPerLayer.get(layer);
    }

    public int getNumberOfMCParticles(int layer) {
        return _nMCHitsPerLayer[layer - 1];
    }

    public int getNumberOfStripHits(int layer) {
        return _nStripHitsPerLayer[layer - 1];
    }

    public List<Integer> getBadHitList() {
        return badHitList;
    }

    public List<Integer> getSharedHitList() {
        return sharedHitList;
    }

    public List<Integer> getTrackLayerList() {
        return trackLayerList;
    }

    public Map<MCParticle, HelicalTrackCross> getBadHits() {
        return badhits;
    }

    public static class DiagonalizedCovarianceMatrix {

        double[] measurement_errors = new double[3];
        Hep3Vector[] measurement_vectors = new Hep3Vector[3];

        public DiagonalizedCovarianceMatrix(TrackerHit hit) {
            SymmetricMatrix cov = new SymmetricMatrix(3, hit.getCovMatrix(), true);
            RealMatrix covMatrix = new Array2DRowRealMatrix(3, 3);
            for (int i = 0; i < 3; i++)
                for (int j = 0; j < 3; j++)
                    covMatrix.setEntry(i, j, cov.e(i, j));
            EigenDecomposition decomposed = new EigenDecomposition(covMatrix);
            BasicHep3Matrix localToGlobal = new BasicHep3Matrix();
            for (int i = 0; i < 3; i++)
                for (int j = 0; j < 3; j++)
                    localToGlobal.setElement(i, j, decomposed.getV().getEntry(i, j));
//            SymmetricMatrix localToGlobal = decomposed.getV().operate(new ArrayRealVector(3))
            {
                double eigenvalue = decomposed.getRealEigenvalue(0);
//                Hep3Vector eigenvector = VecOp.mult(localToGlobal, new BasicHep3Vector());
                Hep3Vector eigenvector = VecOp.mult(Math.signum(eigenvalue), new BasicHep3Vector(decomposed.getVT().getRow(0)));
                measurement_errors[0] = eigenvalue;
                measurement_vectors[0] = eigenvector;
                measurement_errors[2] = eigenvalue;
                measurement_vectors[2] = eigenvector;
            }
            {
                double eigenvalue = decomposed.getRealEigenvalue(1);
                Hep3Vector eigenvector = VecOp.mult(Math.signum(eigenvalue), new BasicHep3Vector(decomposed.getVT().getRow(1)));
                if (eigenvalue > measurement_errors[0]) {
                    measurement_errors[0] = eigenvalue;
                    measurement_vectors[0] = eigenvector;
                }
                if (eigenvalue < measurement_errors[2]) {
                    measurement_errors[2] = eigenvalue;
                    measurement_vectors[2] = eigenvector;
                }
            }
            {
                double eigenvalue = decomposed.getRealEigenvalue(2);
                Hep3Vector eigenvector = VecOp.mult(Math.signum(eigenvalue), new BasicHep3Vector(decomposed.getVT().getRow(2)));
                if (eigenvalue > measurement_errors[0]) {
                    measurement_errors[1] = measurement_errors[0];
                    measurement_vectors[1] = measurement_vectors[0];
                    measurement_errors[0] = eigenvalue;
                    measurement_vectors[0] = eigenvector;
                }
                if (eigenvalue < measurement_errors[2]) {
                    measurement_errors[1] = measurement_errors[2];
                    measurement_vectors[1] = measurement_vectors[2];
                    measurement_errors[2] = eigenvalue;
                    measurement_vectors[2] = eigenvector;
                }
                if (measurement_vectors[1] == null) {
                    measurement_errors[1] = eigenvalue;
                    measurement_vectors[1] = eigenvector;
                }
            }
//            for (int i = 0; i < 3; i++) {
//                System.out.format("%d: resolution %f, vector %s\n", i, measurement_errors[i], measurement_vectors[i].toString());
//            }
        }

        public Hep3Vector getUnmeasuredVector() {
            return measurement_vectors[0];
        }

        public Hep3Vector getMeasuredVector() {
            return measurement_vectors[1];
        }

        public Hep3Vector getNormalVector() {
            return measurement_vectors[2];
        }

    }
}
