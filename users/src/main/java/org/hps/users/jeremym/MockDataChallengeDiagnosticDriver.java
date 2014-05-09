package org.hps.users.jeremym;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.EventHeader.LCMetaData;
import org.lcsim.event.GenericObject;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.Vertex;
import org.lcsim.event.base.BaseTrackState;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * <p>
 * This Driver generically plots information from all the collections 
 * of the Mock Data Challenge sample LCIO file provides by Sho.  
 * This includes item counts as well as information specific to each
 * LCIO type.
 * </p>
 * <p>
 * Full list of collection names and classes is here:<br/>
 * <a href="https://confluence.slac.stanford.edu/display/~jeremym/MDC+Collection+Information">MDC Collection Information</a>
 * </p>
 *  
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */ 
@SuppressWarnings( {"rawtypes", "unchecked" } )
public class MockDataChallengeDiagnosticDriver extends Driver {
        
    boolean verbose = false;    
    boolean isSetup = false;
    AIDA aida = AIDA.defaultInstance();    
    double by;        
    Map<String, CollectionPlotter> collectionPlotters = new HashMap<String, CollectionPlotter>();
        
    @Override
    protected void detectorChanged(Detector detector) {        
        by = detector.getFieldMap().getField(new BasicHep3Vector(0., 0., 0.)).y();
    }
        
    private void createCollectionPlotters(EventHeader event) {
        CollectionPlotterFactory factory = new CollectionPlotterFactory();
        Set<List> lists = event.getLists();
        for (List list : lists) {
            LCMetaData meta = event.getMetaData(list);
            String name = meta.getName();            
            Class type = meta.getType();                        
            CollectionPlotter collectionPlotter = factory.createCollectionPlotter(name, type);
            if (collectionPlotter != null) {
                collectionPlotter.setName(name);            
                collectionPlotters.put(name, collectionPlotter);
                collectionPlotter.mkdir();
                collectionPlotter.cd();
                collectionPlotter.definePlots();
            } else {
                throw new RuntimeException("No CollectionPlotter found for " + name);
            }
        }
    }
     
    @Override
    protected void endOfData() {
        try {
            aida.saveAs(this.getClass().getSimpleName() + ".aida");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
        
    @Override
    public void process(EventHeader event) {
        
        if (!isSetup) {
            createCollectionPlotters(event);
            isSetup = true;
        }
        
        // Get all the lists from the event.
        Set<List> lists = event.getLists();
        
        // Loop over the lists.
        for (List list : lists) {
                       
            // Get the collection information.
            LCMetaData meta = event.getMetaData(list);
            String collectionName = meta.getName();

            // Get the collection's plotter.
            CollectionPlotter plotter = collectionPlotters.get(collectionName);
            
            // Fill the plots for the collection.
            if (plotter != null) {
                plotter.cd();
                plotter.plot(list);
            } else {
                System.err.println("WARNING: No plotter found for collection " + collectionName);
            }
        }
    }
    
    public void setVerbose(boolean verbose) { 
        this.verbose = verbose;
    }
    
    abstract class CollectionPlotter<T> {
        
        String name;
        
        CollectionPlotter() {
        }
        
        void plot(List<T> objects) {
            aida.histogram1D("Item Count").fill(objects.size());
        }
                        
        void definePlots() {
            aida.histogram1D("Item Count", 200, 0., 200);
        }
                        
        void setName(String name) {
            this.name = name;
        }
        
        void cd() {
            aida.tree().cd("/" + name);             
        }
        
        void mkdir() {
            aida.tree().cd("/");
            aida.tree().mkdir(name);
        }        
    }
    
    class DefaultPlotter extends CollectionPlotter<Object> {
    }
    
    class CalorimeterHitPlotter extends CollectionPlotter<CalorimeterHit> {
        
        void definePlots() {
            super.definePlots();
            aida.histogram1D("Corrected Energy", 120, -1.0, 5.0);
        }
        
        void plot(List<CalorimeterHit> collection) {
            super.plot(collection);
            for (CalorimeterHit hit : collection) {
                double correctedEnergy = hit.getCorrectedEnergy();
                double[] position = hit.getPosition();            
                aida.histogram1D("Corrected Energy").fill(correctedEnergy);
                aida.cloud1D("Position X").fill(position[0]);
                aida.cloud1D("Position Y").fill(position[1]);
                aida.cloud1D("Position Z").fill(position[2]);
            }
        }    
    }
    
    class ReconstructedParticlePlotter extends CollectionPlotter<ReconstructedParticle> {
        
        void definePlots() {
            aida.histogram1D("Item Count", 20, 0., 20);
            aida.histogram1D("Cluster_E over Track_P", 200, 0., 10.);
            aida.histogram1D("Energy", 200, 0., 5.0);
        }
        
        void plot(List<ReconstructedParticle> collection) {
            super.plot(collection);
            for (ReconstructedParticle particle : collection) {
                aida.histogram1D("Energy").fill(particle.getEnergy());
                aida.cloud1D("N Clusters").fill(particle.getClusters().size());
                aida.cloud1D("N Tracks").fill(particle.getTracks().size());
                Hep3Vector momentum = particle.getMomentum();
                aida.cloud1D("P").fill(momentum.magnitude());
                aida.cloud1D("Px").fill(momentum.x());
                aida.cloud1D("Py").fill(momentum.y());
                aida.cloud1D("Pz").fill(momentum.z());
                aida.cloud1D("Mass").fill(particle.getMass());
                aida.cloud1D("Energy").fill(particle.getEnergy());
                aida.cloud1D("Goodness of PID").fill(particle.getGoodnessOfPID());
                aida.cloud1D("N Particles").fill(particle.getParticles().size());
                Hep3Vector refPoint = particle.getReferencePoint();
                aida.cloud1D("Ref Point X").fill(refPoint.x());
                aida.cloud1D("Ref Point Y").fill(refPoint.y());
                aida.cloud1D("Ref Point Z").fill(refPoint.z());
                Vertex vertex = particle.getStartVertex();
                if (vertex != null) {
                    aida.cloud1D("Vertex chi2").fill(vertex.getChi2());
                    aida.cloud1D("Vertex probability").fill(vertex.getProbability());
                    Hep3Vector vertexPosition = vertex.getPosition();
                    aida.cloud1D("Vertex Position X").fill(vertexPosition.x());
                    aida.cloud1D("Vertex Position Y").fill(vertexPosition.y());
                    aida.cloud1D("Vertex Position Z").fill(vertexPosition.z());
                }
                if (particle.getClusters().size() == 1 && particle.getTracks().size() == 1) {
                    double[] p = particle.getTracks().get(0).getTrackStates().get(0).getMomentum();
                    Hep3Vector pvec = new BasicHep3Vector(p[0], p[1], p[2]);
                    double clusterE = particle.getClusters().get(0).getEnergy();
                    if (clusterE != Double.NaN && clusterE > 0 && pvec.magnitude() != 0)
                        aida.histogram1D("Cluster_E over Track_P").fill(particle.getClusters().get(0).getEnergy() / pvec.magnitude());
                }
            }           
        }        
    }
    
    class ClusterPlotter extends CollectionPlotter<Cluster> {
                        
        void definePlots() {
            aida.histogram1D("Energy", 50, 0., 2.);
            aida.histogram1D("Item Count", 20, 0., 20.);
            aida.histogram1D("Position X", 66, -300., 360.);
            aida.histogram1D("Position Y", 200, -100., 100.);
            aida.histogram1D("Position Z", 100, 1360., 1460.);
            aida.histogram1D("Size", 20, 0., 20.);
        }
        
        void plot(List<Cluster> collection) {
            super.plot(collection);
            for (Cluster cluster : collection) {
                aida.histogram1D("Energy").fill(cluster.getEnergy());
                aida.cloud1D("Phi").fill(cluster.getIPhi());
                aida.cloud1D("Theta").fill(cluster.getITheta());
                aida.histogram1D("Size").fill(cluster.getSize());
                double[] position = cluster.getPosition();
                aida.histogram1D("Position X").fill(position[0]);
                aida.histogram1D("Position Y").fill(position[1]);
                aida.histogram1D("Position Z").fill(position[2]);
            }
        }
    }
    
    class TrackPlotter extends CollectionPlotter<Track> {
        
        void definePlots() {
            aida.histogram1D("Item Count", 25, 0., 25);
            aida.histogram1D("Chi2", 120, 0, 30.0);
            aida.histogram1D("P", 48, 0., 8);
            aida.histogram1D("Px", 48, 0., 8);
            aida.histogram1D("Py", 60, -2.0, 0.4);
            aida.histogram1D("Pz", 80, -0.2, 0.2);
            aida.histogram1D("R of 1st Hit", 96, 92., 116.);
            aida.histogram1D("d0", 48, -8., 8.);
            aida.histogram1D("dEdx", 100, 0., 1.0);
            aida.histogram1D("omega", 96, -0.0012, 0.0012);
            aida.histogram1D("phi", 56, 0.0, 7.0);
            aida.histogram1D("tan lambda", 56, -0.07, 0.07);
            aida.histogram1D("z0", 56, -3.0, 5.0);
        }
        
        void plot(List<Track> collection) {
            super.plot(collection);
            for (Track track : collection) {
                
                aida.histogram1D("Chi2").fill(track.getChi2());
                aida.histogram1D("dEdx").fill(track.getdEdx());
                aida.cloud1D("dEdx error").fill(track.getdEdxError());
                aida.histogram1D("R of 1st Hit").fill(track.getRadiusOfInnermostHit());
                
                TrackState state = track.getTrackStates().get(0);
                double[] momentum = ((BaseTrackState)state).computeMomentum(by);
                Hep3Vector momentumVector = new BasicHep3Vector(momentum[0], momentum[1], momentum[2]);
                double p = momentumVector.magnitude();
                
                aida.histogram1D("Px").fill(momentum[0]);
                aida.histogram1D("Py").fill(momentum[1]);
                aida.histogram1D("Pz").fill(momentum[2]);
                aida.histogram1D("P").fill(p);
                
                aida.histogram1D("d0").fill(state.getD0());
                aida.histogram1D("omega").fill(state.getOmega());
                aida.histogram1D("phi").fill(state.getPhi());
                aida.histogram1D("tan lambda").fill(state.getTanLambda());
                aida.histogram1D("z0").fill(state.getZ0());                              
            }
        }
    }
    
    class MCParticlePlotter extends CollectionPlotter<MCParticle> {
                
        void plot(List<MCParticle> collection) {
            super.plot(collection);
            for (MCParticle particle : collection) {
                
                Hep3Vector momentum = particle.getMomentum();
                Hep3Vector origin = particle.getOrigin();
                double prodTime = particle.getProductionTime();
                
                aida.cloud1D("Energy").fill(particle.getEnergy());
                aida.cloud1D("Mass").fill(particle.getMass());
                
                try {
                    Hep3Vector endpoint = particle.getEndPoint();
                    aida.cloud1D("Endpoint X").fill(endpoint.x());
                    aida.cloud1D("Endpoint Y").fill(endpoint.y());
                    aida.cloud1D("Endpoint Z").fill(endpoint.z());
                } catch (RuntimeException e) {                    
                }
                
                aida.cloud1D("P").fill(momentum.magnitude());
                aida.cloud1D("Px").fill(momentum.x());
                aida.cloud1D("Py").fill(momentum.y());
                aida.cloud1D("Pz").fill(momentum.z());
                aida.cloud1D("Origin X").fill(origin.x());
                aida.cloud1D("Origin Y").fill(origin.y());
                aida.cloud1D("Origin Z").fill(origin.z());
                aida.cloud1D("Production Time").fill(prodTime);
            }
        }
    }
    
    class RawTrackerHitPlotter extends CollectionPlotter<RawTrackerHit> {
        
        void plot(List<RawTrackerHit> collection) {
            super.plot(collection);
            for (RawTrackerHit hit : collection) {
                aida.cloud1D("Time").fill(hit.getTime());
                aida.cloud1D("N ADC Values").fill(hit.getADCValues().length);
                for (short adcValue : hit.getADCValues()) {
                    aida.cloud1D("ADC Values").fill(adcValue);
                }
                // FIXME: Requires missing IdentifierHelper.
                //cloud1D("Layer Number").fill(hit.getLayerNumber());
                
                // FIXME: SimTrackerHit list always points to null.
                //cloud1D("N SimTrackerHits").fill(hit.getSimTrackerHits().size());
            }
        }
    }
    
    class RawCalorimeterHitPlotter extends CollectionPlotter<RawCalorimeterHit> {
        
        void definePlots() {
            super.definePlots();
            aida.histogram1D("Amplitude", 300, 0., 30000);
            aida.histogram1D("Timestamp", 260, 0., 6500);
        }
        
        void plot(List<RawCalorimeterHit> collection) {
            super.plot(collection);
            for (RawCalorimeterHit hit : collection) {
                aida.histogram1D("Amplitude").fill(hit.getAmplitude());
                aida.histogram1D("Timestamp").fill(hit.getTimeStamp());
            }
        }
    }
    
    class TrackerHitPlotter extends CollectionPlotter<TrackerHit> {
        
        void definePlots() {
            super.definePlots();
            aida.histogram1D("dEdx", 500, 0., 1.);
        }
        
        void plot(List<TrackerHit> collection) {
            super.plot(collection);
            for (TrackerHit hit : collection) {
                aida.histogram1D("dEdx").fill(hit.getdEdx());
                aida.cloud1D("edep error").fill(hit.getEdepError());
                double[] position = hit.getPosition();
                aida.cloud1D("Position X").fill(position[0]);
                aida.cloud1D("Position Y").fill(position[1]);
                aida.cloud1D("Position Z").fill(position[2]);
                aida.cloud1D("N RawTrackerHits").fill(hit.getRawHits().size());
                aida.cloud1D("Time").fill(hit.getTime());
            }
        }
    }
    
    class SimCalorimeterHitPlotter extends CollectionPlotter<SimCalorimeterHit> {
        
        void definePlots() {
            super.definePlots();
            aida.histogram1D("Raw Energy", 200, 0., 2.0);
            aida.histogram1D("Position X", 60, -300., 400.);
            aida.histogram1D("Position Y", 100, -100., 100.);
            aida.histogram1D("Position Z", 50, 1465., 1475.);
            aida.histogram1D("Time", 200, 0., 400.);
        }
        
        void plot(List<SimCalorimeterHit> collection) {
            super.plot(collection);
            for (SimCalorimeterHit hit : collection) {
                aida.histogram1D("Raw Energy").fill(hit.getRawEnergy());
                aida.histogram1D("Time").fill(hit.getTime());
                double[] position = hit.getPosition();
                aida.histogram1D("Position X").fill(position[0]);
                aida.histogram1D("Position Y").fill(position[1]);
                aida.histogram1D("Position Z").fill(position[2]);
            }
        }
    }
    
    class SimTrackerHitPlotter extends CollectionPlotter<SimTrackerHit> {
        
        void definePlots() {
            super.definePlots();
        }
        
        void plot(List<SimTrackerHit> collection) {
            super.plot(collection);
            for (SimTrackerHit hit : collection) {
                aida.cloud1D("dEdx").fill(hit.getdEdx());
                double[] endpoint = hit.getEndPoint();
                aida.cloud1D("Endpoint X").fill(endpoint[0]);
                aida.cloud1D("Endpoint Y").fill(endpoint[1]);
                aida.cloud1D("Endpoint Z").fill(endpoint[2]);
                aida.cloud1D("Layer").fill(hit.getLayerNumber());

                double p[] = hit.getMomentum();
                Hep3Vector pvec = new BasicHep3Vector(p[0], p[1], p[2]);
                aida.cloud1D("Px").fill(p[0]);
                aida.cloud1D("Py").fill(p[1]);
                aida.cloud1D("Pz").fill(p[2]);
                aida.cloud1D("P").fill(pvec.magnitude());
                
                aida.cloud1D("Time").fill(hit.getTime());

                double[] start = hit.getStartPoint();
                aida.cloud1D("Startpoint X").fill(start[0]);
                aida.cloud1D("Startpoint Y").fill(start[1]);
                aida.cloud1D("Startpoint Z").fill(start[2]);
                
                Hep3Vector position = hit.getPositionVec();
                aida.cloud1D("Position X").fill(position.x());
                aida.cloud1D("Position Y").fill(position.y());
                aida.cloud1D("Position Z").fill(position.z());
                
                aida.cloud1D("Path Length").fill(hit.getPathLength());
            }
        }
    }
    
    class GenericObjectPlotter extends CollectionPlotter<GenericObject> {
        
        void definePlots() {
            super.definePlots();
        }
        
        void plot(List<GenericObject> collection) {
            super.plot(collection);
            for (GenericObject object : collection) {
                for (int i = 0; i < object.getNDouble(); i++) {
                    double value = object.getDoubleVal(i);
                    aida.cloud1D("Double Values").fill(value);
                }
                for (int i = 0; i < object.getNInt(); i++) {
                    int value = object.getIntVal(i);
                    aida.cloud1D("Int Values").fill(value);
                }
                for (int i = 0; i < object.getNFloat(); i++) {
                    float value = object.getFloatVal(i);
                    aida.cloud1D("Float Values").fill(value);
                }
            }
        }        
    }
    
    class CollectionPlotterFactory {
        
        CollectionPlotter createCollectionPlotter(String name, Class type) {
            CollectionPlotter plotter = null;
            if (type.equals(CalorimeterHit.class)) {
                plotter = new CalorimeterHitPlotter(); 
            } else if (type.equals(Cluster.class)) {
                plotter = new ClusterPlotter();
            } else if (type.equals(ReconstructedParticle.class)) {
                plotter = new ReconstructedParticlePlotter();
            } else if (type.equals(Track.class)) {
                plotter = new TrackPlotter();
            } else if (type.equals(MCParticle.class)) {
                plotter = new MCParticlePlotter();
            } else if (type.equals(RawTrackerHit.class)) {
                plotter = new RawTrackerHitPlotter();                
            } else if (type.equals(TrackerHit.class)) {
                plotter = new TrackerHitPlotter();
            } else if (type.equals(RawCalorimeterHit.class)) {
                plotter = new RawCalorimeterHitPlotter();
            } else if (type.equals(SimCalorimeterHit.class)) {
                plotter = new SimCalorimeterHitPlotter();
            } else if (type.equals(SimTrackerHit.class)) {
                plotter = new SimTrackerHitPlotter();
            } else if (type.equals(GenericObject.class)) {
                plotter = new GenericObjectPlotter();
            } else {
                plotter = new DefaultPlotter();
            }            
            plotter.setName(name);
            return plotter;
        }        
    }    
}
