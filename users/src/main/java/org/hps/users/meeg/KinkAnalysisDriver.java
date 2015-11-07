package org.hps.users.meeg;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Vertex;
import org.lcsim.geometry.Detector;
import org.lcsim.units.clhep.PhysicalConstants;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/*
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: KinkAnalysisDriver.java,v 1.3 2013/10/24 18:11:43 meeg Exp $
 */
public class KinkAnalysisDriver extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    IHistogram2D dThetaLayer;
    IHistogram2D dThetaLayerLargestT;
    IHistogram2D dThetaLayerLargestE;
    IHistogram2D dThetaLayerScatter;
    IHistogram2D dThetaLayerNoScatter;
    IHistogram1D dThetaLargestT;
    IHistogram1D dThetaLargestE;
    IHistogram1D layerLargestT;
    IHistogram1D layerLargestE;
    IHistogram2D frontDPDT;
    IHistogram1D frontDP;
    IHistogram1D frontDT;
    IHistogram1D bsZ;
    IHistogram2D twoTrackFrontDT;
    IHistogram1D frontDTSum;
    int neventsWithScatter = 0;
    int hardScatterIsLargestScatter = 0;

    @Override
    protected void detectorChanged(Detector detector) {
        dThetaLayer = aida.histogram2D("deflection in Y vs. layer", 12, 0.5, 12.5, 1000, -0.02, 0.02);
        dThetaLayerScatter = aida.histogram2D("deflection in Y vs. layer, with hard scatter", 12, 0.5, 12.5, 1000, -0.02, 0.02);
        dThetaLayerNoScatter = aida.histogram2D("deflection in Y vs. layer, no hard scatter", 12, 0.5, 12.5, 1000, -0.02, 0.02);
        dThetaLayerLargestT = aida.histogram2D("deflection in Y vs. layer, largest scatter in track", 12, 0.5, 12.5, 1000, -0.02, 0.02);
        dThetaLayerLargestE = aida.histogram2D("deflection in Y vs. layer, largest scatter in event", 12, 0.5, 12.5, 1000, -0.02, 0.02);
        dThetaLargestT = aida.histogram1D("largest deflection in Y in track", 1000, -0.02, 0.02);
        dThetaLargestE = aida.histogram1D("largest deflection in Y in event", 1000, -0.02, 0.02);
        layerLargestT = aida.histogram1D("layer of largest deflection in Y in track", 12, 0.5, 12.5);
        layerLargestE = aida.histogram1D("layer of largest deflection in Y in event", 12, 0.5, 12.5);
        frontDPDT = aida.histogram2D("deflection in Y vs. energy loss, front layers", 200, 0.0, 3.0, 1000, -0.02, 0.02);
        frontDP = aida.histogram1D("energy loss, front layers", 200, 0.0, 3.0);
        frontDT = aida.histogram1D("deflection in Y, front layers", 1000, -0.02, 0.02);
        bsZ = aida.histogram1D("vertex Z, BS constrained", 200, -10.0, 20.0);
        twoTrackFrontDT = aida.histogram2D("deflection in Y, two tracks", 200, -0.02, 0.02, 200, -0.02, 0.02);
        frontDTSum = aida.histogram1D("sum of deflections in Y, two tracks", 500, -0.02, 0.02);
    }

    @Override
    public void process(EventHeader event) {

//        for (List<ReconstructedParticle> list : event.get(ReconstructedParticle.class)) {
//            for (ReconstructedParticle particle : list) {
//                Vertex vertex = particle.getStartVertex();
//                if (vertex != null) {
//                    System.out.format("%s: z %f, chisq %f\n", event.getMetaData(list).getName(), vertex.getPosition().x(), vertex.getChi2());
//                }
//            }
//        }
        if (event.hasCollection(ReconstructedParticle.class, "AprimeBeamspotConstrained")) {
            List<ReconstructedParticle> particles = event.get(ReconstructedParticle.class, "AprimeBeamspotConstrained");
            int nvertices = 0;
            Vertex theVertex = null;
            for (ReconstructedParticle particle : particles) {
                Vertex vertex = particle.getStartVertex();
                if (vertex != null) {
                    nvertices++;
                    theVertex = vertex;
//                    System.out.println(vertex.getPosition().x());
                }
            }
            if (nvertices == 1) {
                bsZ.fill(theVertex.getPosition().x());
            }
        }

        List<MCParticle> MCParticles = event.getMCParticles();
        List<Hep3Vector> hardScatters = new ArrayList<Hep3Vector>();

        for (MCParticle particle : MCParticles) {
            if (particle.getOrigin().magnitude() > 10.0) {
                hardScatters.add(particle.getOrigin());
            }
        }

        List<SimTrackerHit> trackerHits = event.get(SimTrackerHit.class, "TrackerHits");

//        Map<MCParticle, List<SimTrackerHit>> hitMap = new HashMap<MCParticle, List<SimTrackerHit>>();
        Map<MCParticle, Map<Integer, SimTrackerHit>> trackMap = makeTrackHitMap(trackerHits, hardScatters);

        List<MCParticle> particlesWithoutTracks = new ArrayList<MCParticle>();
        for (MCParticle particle : trackMap.keySet()) {
            Set<Integer> layers = trackMap.get(particle).keySet();
            int pairCount = 0;
            for (Integer layer : layers) {
                if (layer % 2 == 0 && layers.contains(layer - 1)) {
                    pairCount++;
                }
            }
            if (pairCount < 4) {
                particlesWithoutTracks.add(particle);
            }
        }
        for (MCParticle particle : particlesWithoutTracks) {
            trackMap.remove(particle);
        }

        MCParticle electron = null, positron = null;
        for (MCParticle particle : trackMap.keySet()) {
            if (particle.getCharge() > 0) {
                if (positron != null) {
                    return;
                }
                positron = particle;
            }
            if (particle.getCharge() < 0) {
                if (electron != null) {
                    return;
                }
                electron = particle;
            }
        }
        if (electron == null || positron == null) {
            return;
        }

        double maxEventDT = 0.0;
        int maxEventDTLayer = 0;
        boolean maxEventDTHardScatter = false;

        double deflection12_ele = deflection(trackMap.get(electron), 0, 4);
        double deflection12_pos = deflection(trackMap.get(positron), 0, 4);
        twoTrackFrontDT.fill(deflection12_ele, deflection12_pos);
        frontDTSum.fill(deflection12_ele + deflection12_pos);

        for (MCParticle particle : trackMap.keySet()) {
            Map<Integer, SimTrackerHit> layerMap = trackMap.get(particle);
            List<Integer> layers = new ArrayList<Integer>(layerMap.keySet());
            Collections.sort(layers);
            double maxDT = 0.0;
            int maxDTLayer = 0;
            boolean maxDTHardScatter = false;

            double angle1 = angle(layers, layerMap, 0, 1);
            Hep3Vector p1 = particle.getMomentum();

            double angle2 = angle(layers, layerMap, 4, 5);
            Hep3Vector p2 = new BasicHep3Vector(layerMap.get(layers.get(1)).getMomentum());

            double deflection12 = (angle2 - angle1) * Math.signum(angle1);

            frontDPDT.fill(p1.magnitude() - p2.magnitude(), deflection12);
            frontDP.fill(p1.magnitude() - p2.magnitude());
            frontDT.fill(deflection12);

            for (int i = 0; i < layers.size() - 1; i++) {
                SimTrackerHit hit = layerMap.get(layers.get(i));

                boolean nearHardScatter = false;
                for (Hep3Vector scatter : hardScatters) {
                    if (VecOp.sub(hit.getPositionVec(), scatter).magnitude() < 5.0) {
                        nearHardScatter = true;
                    }
                }

                SimTrackerHit lastHit = null;
                if (i > 0) {
                    lastHit = layerMap.get(layers.get(i - 1));
//                    double theta = Constants.fieldConversion*0.5*PhysicalConstants.c_light;
                }

                SimTrackerHit nextHit = layerMap.get(layers.get(i + 1));

                double inAngle = angle(lastHit, hit);
                double outAngle = angle(hit, nextHit);

                double deflectionY = (outAngle - inAngle) * Math.signum(inAngle);
//                    System.out.format("in: %f, out: %f, delta: %f\n", inAngle,outAngle,deflectionY);
                dThetaLayer.fill(layers.get(i), deflectionY);
                if (nearHardScatter) {
                    dThetaLayerScatter.fill(layers.get(i), deflectionY);
                } else {
                    dThetaLayerNoScatter.fill(layers.get(i), deflectionY);
                }
                if (Math.abs(deflectionY) > Math.abs(maxDT)) {
                    maxDT = deflectionY;
                    maxDTLayer = layers.get(i);
                    maxDTHardScatter = nearHardScatter;
                }
            }
            if (maxDT != 0.0) {
                dThetaLargestT.fill(maxDT);
                layerLargestT.fill(maxDTLayer);
                dThetaLayerLargestT.fill(maxDTLayer, maxDT);
            }

            if (Math.abs(maxDT) > Math.abs(maxEventDT)) {
                maxEventDT = maxDT;
                maxEventDTLayer = maxDTLayer;
                maxEventDTHardScatter = maxDTHardScatter;
            }
        }

        if (maxEventDT != 0.0) {
            dThetaLargestE.fill(maxEventDT);
            layerLargestE.fill(maxEventDTLayer);
            dThetaLayerLargestE.fill(maxEventDTLayer, maxEventDT);
            neventsWithScatter++;
            if (maxEventDTHardScatter) {
                hardScatterIsLargestScatter++;
            }
        }
    }

    private static double angle(SimTrackerHit hit1, SimTrackerHit hit2) {
        double y1 = hit2.getMCParticle().getOriginY();
//        double z1 = hit2.getMCParticle().getOriginZ();
        double s1 = hit2.getMCParticle().getProductionTime() * PhysicalConstants.c_light;
        if (hit1 != null) {
            y1 = hit1.getPosition()[1];
//            z1 = hit1.getPosition()[2];
            s1 = hit1.getTime() * PhysicalConstants.c_light;
        }

        double y2 = hit2.getPosition()[1];
//        double z2 = hit2.getPosition()[2];
        double s2 = hit2.getTime() * PhysicalConstants.c_light;
//                double outAngle = Math.atan2((nextY - y), (nextZ - z));

        return Math.asin((y2 - y1) / (s2 - s1));
    }

    private static double angle(List<Integer> layers, Map<Integer, SimTrackerHit> layerMap, int layer1, int layer2) {
        SimTrackerHit hit1 = null;
        if (layer1 > 0) {
            hit1 = layerMap.get(layers.get(layer1 - 1));
        }
        SimTrackerHit hit2 = layerMap.get(layers.get(layer2 - 1));

        return angle(hit1, hit2);
    }

    static double deflection(Map<Integer, SimTrackerHit> layerMap, int layer1, int layer2) {
        List<Integer> layers = new ArrayList<Integer>(layerMap.keySet());
        Collections.sort(layers);

        double angle1 = angle(layers, layerMap, layer1, layer1 + 1);
        double angle2 = angle(layers, layerMap, layer2, layer2 + 1);

        return (angle2 - angle1) * Math.signum(angle1);
    }

    static Map<MCParticle, Map<Integer, SimTrackerHit>> makeTrackHitMap(List<SimTrackerHit> trackerHits, List<Hep3Vector> hardScatters) {
        Map<MCParticle, Map<Integer, SimTrackerHit>> trackMap = new HashMap<MCParticle, Map<Integer, SimTrackerHit>>();

        for (SimTrackerHit hit : trackerHits) {
            Map<Integer, SimTrackerHit> layerMap = trackMap.get(hit.getMCParticle());
            if (layerMap == null) {
                layerMap = new HashMap<Integer, SimTrackerHit>();
                trackMap.put(hit.getMCParticle(), layerMap);
            }
            int layer = hit.getIdentifierFieldValue("layer");
            if (layerMap.containsKey(layer)) {
                if (hardScatters != null) {
                    boolean nearHardScatter = false;
                    for (Hep3Vector scatter : hardScatters) {
                        if (VecOp.sub(hit.getPositionVec(), scatter).magnitude() < 5.0) {
                            nearHardScatter = true;
                        }
                    }
                    if (!nearHardScatter) {
                        hardScatters.add(hit.getPositionVec());
                    }
                }
//                System.out.format("Double hit in layer %d, %s\n", layer, nearHardScatter ? "near hard scatter" : "not near hard scatter");
                if (layerMap.get(layer).getPathLength() < hit.getPathLength()) {
                    continue;
                }
            }
            layerMap.put(layer, hit);
        }
        return trackMap;
    }

    @Override
    protected void endOfData() {
        System.out.format("%d events, %d had hard scatter as largest scatter\n", neventsWithScatter, hardScatterIsLargestScatter);
    }
}
