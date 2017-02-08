package org.hps.util;

/**
 * Driver to merge bunch trains for HPS and displace them in time
 * much of this code was taken from org.lcsim.util.OverlayDriver.java
 */
import hep.physics.particle.properties.ParticleType;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.BasicHepLorentzVector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.HepLorentzVector;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.event.EventHeader;
import org.lcsim.event.EventHeader.LCMetaData;
import org.lcsim.event.GenericObject;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.base.BaseLCSimEvent;
import org.lcsim.event.base.BaseMCParticle;
import org.lcsim.event.base.BaseSimCalorimeterHit;
import org.lcsim.event.base.BaseSimTrackerHit;
import org.lcsim.util.Driver;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.lcio.LCIOUtil;
import org.lcsim.lcio.LCIOWriter;
import org.lcsim.lcio.SIOMCParticle;

public class MergeBunches extends Driver {

    String outFile = "default.slcio";
    private LCIOWriter writer;
    BaseLCSimEvent newEvent;
    static double startT = -40.0;
    static double deltaT = 2.0;
    static int nBunches = 40;    
    double offsetT = startT;
    int bunchCounter = 0;
    int eventCounter = 0;
    protected Map<String, Map<Long, SimCalorimeterHit>> caloHitMap;
    protected List<MCParticle> overlayMcParticles;
    protected List<MCParticle> allMcParticles;
    protected Map<MCParticle, MCParticle> mcParticleReferences;

    /**
     * @param args the command line arguments
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        MergeBunches mb = new MergeBunches();
        mb.parseArgs(args);


//         LCReader* lcReader = LCFactory::getInstance()->createLCReader() ;


    }

    public MergeBunches() throws IOException {

//        LCIODriver lcioDriver = new LCIODriver(outFile);
         writer=new LCIOWriter(outFile);
        caloHitMap = new HashMap<String, Map<Long, SimCalorimeterHit>>();
        overlayMcParticles = new ArrayList<MCParticle>();
        allMcParticles = new ArrayList<MCParticle>();
        mcParticleReferences = new HashMap<MCParticle, MCParticle>();

    }

    public void process(EventHeader event) {


        if (bunchCounter == 0)
            newEvent = new BaseLCSimEvent(event.getRunNumber(), eventCounter, event.getDetectorName());
        mergeEvents(newEvent, event, offsetT);
        bunchCounter++;
        offsetT += deltaT;
        if (bunchCounter == nBunches) {
            System.out.println("Writing events #"+eventCounter);
            eventCounter++;
            offsetT = startT;
            bunchCounter = 0;
            try {
                writer.write(newEvent);
            } catch (IOException ex) {
                Logger.getLogger(MergeBunches.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Merges all collections from the given events and applies a time offset
     * to all entries in all collections of the overlay event.
     * @param event the event where everything is merged into
     * @param overlayEvent the event overlaid
     * @param overlayTime the time offset for the overlay event
     */
    protected void mergeEvents(EventHeader event, EventHeader overlayEvent, double overlayTime) {

        // need to copy list of collections to avoid concurrent modification
        List<LCMetaData> overlayCollections = new ArrayList<LCMetaData>(overlayEvent.getMetaData());
        for (LCMetaData overlayCollection : overlayCollections) {
            String overlayCollectionName = overlayCollection.getName();
            if (event.hasItem(overlayCollectionName)) {
                this.mergeCollections(event.getMetaData((List) event.get(overlayCollectionName)), overlayCollection, overlayTime);
            } else {
                // event does not contain corresponding collection from overlayEvent, just put it there
                // First move hits and apply timing cuts
                List collection = this.moveCollectionToTime(overlayCollection, overlayTime);
                this.putCollection(overlayCollection, (List) overlayEvent.get(overlayCollectionName), event);
            }
        }
    }

    /**
     * Shifts an event in time. Moves all entries in all collections
     * in the event by the given offset in time.
     * @param event the event to move in time
     * @param time the time shift applied to all entries in all collections
     */
    protected void moveEventToTime(EventHeader event, double time) {
        // need to copy list of collections to avoid concurrent modification
        List<LCMetaData> collections = new ArrayList<LCMetaData>(event.getMetaData());
        for (LCMetaData collection : collections) {
            List movedCollection = this.moveCollectionToTime(collection, time);
            if (movedCollection != null) {
                // replace the original collection
                event.remove(collection.getName());
                this.putCollection(collection, movedCollection, event);
            }
        }
    }

    /**
     * Shifts a collection in time. Moves all entries in the collection
     * by the given offset in time. If a readout time is set for the
     * given collection, all entries outside of that window will be removed.
     * @param collection the collection to move in time
     * @param time the time shift applied to all entries in the collection
     * @return returns the list of moved entries
     */
    protected List moveCollectionToTime(LCMetaData collection, double time) {
        EventHeader event = collection.getEvent();
        String collectionName = collection.getName();
        Class collectionType = collection.getType();
        int flags = collection.getFlags();
        if (this.getHistogramLevel() > HLEVEL_NORMAL)
            System.out.println("Moving collection: " + collectionName + " of type " + collectionType + " to " + time + "ns");

        double timeWindow = 0;

        List movedCollection;
        if (collectionType.isAssignableFrom(MCParticle.class)) {
            // MCParticles
            // don't create new list, just move existing particles
            movedCollection = event.get(MCParticle.class, collectionName);
            for (MCParticle mcP : (List<MCParticle>) movedCollection) {
                ((SIOMCParticle) mcP).setTime(mcP.getProductionTime() + time);
            }
        } else if (collectionType.isAssignableFrom(SimTrackerHit.class)) {
            // SimTrackerHits
            movedCollection = new ArrayList<SimTrackerHit>();
            for (SimTrackerHit hit : (List<SimTrackerHit>) event.get(SimTrackerHit.class, collectionName)) {
                ((BaseSimTrackerHit) hit).setTime(hit.getTime() + time);
                movedCollection.add(hit);
            }
        } else if (collectionType.isAssignableFrom(SimCalorimeterHit.class)) {
            // SimCalorimeterHits
            movedCollection = new ArrayList<SimCalorimeterHit>();
            // check if hit contains PDGIDs
            boolean hasPDG = LCIOUtil.bitTest(flags, LCIOConstants.CHBIT_PDG);
            List<SimCalorimeterHit> hits = event.get(SimCalorimeterHit.class, collectionName);
            int nSimCaloHits = hits.size();
            int nHitsMoved = 0;
            for (SimCalorimeterHit hit : event.get(SimCalorimeterHit.class, collectionName)) {
                // check if earliest energy deposit is later than relevant time window

                BaseSimCalorimeterHit movedHit = null;
                nHitsMoved++;
                if (this.getHistogramLevel() > HLEVEL_HIGH && nHitsMoved % 100 == 0)
                    System.out.print("Moved " + nHitsMoved + " / " + nSimCaloHits + " hits\n");
                movedHit = (BaseSimCalorimeterHit) hit;
                movedHit.shiftTime(time);//adds this amount of time to all the times of SimCalorimeterHits
                movedCollection.add(movedHit);
            }
        } else if (collectionType.isAssignableFrom(GenericObject.class)) {
            // nothing to do for GenericObjects
            return event.get(GenericObject.class, collectionName);
        } else {
            System.err.println("Unable to move collection: " + collectionName + " of type " + collectionType);
            return null;
        }
        return movedCollection;
    }

    /**
     * Adds a collection to an event using the meta data information from the
     * given collection and the entries from the given list.
     * @param collection the collection to take the meta data from
     * @param entries the list of entries to put into the event
     * @param event the event to put the collection
     */
    protected void putCollection(LCMetaData collection, List entries, EventHeader event) {
        String[] readout = collection.getStringParameters().get("READOUT_NAME");
        if (readout != null) {
            event.put(collection.getName(), entries, collection.getType(), collection.getFlags(), readout[0]);
        } else {
            event.put(collection.getName(), entries, collection.getType(), collection.getFlags());
        }
        if (this.getHistogramLevel() > HLEVEL_NORMAL)
            System.out.println("Putting collection " + collection.getName() + " into event.");
    }

    /**
     * Merges two collections and applies a time offset to all entries in
     * the overlay collection.
     * @param collection the collection where the overlay collection is merged into
     * @param overlayCollection the collection overlaid
     * @param overlayTime the time offset for the overlay collection
     * @return returns <c>false</c> if unable to merge collections, otherwise <c>true</c>
     */
    protected boolean mergeCollections(LCMetaData collection, LCMetaData overlayCollection, double overlayTime) {
        String collectionName = collection.getName();
        Class collectionType = collection.getType();
        Class overlayCollectionType = overlayCollection.getType();
        if (this.getHistogramLevel() > HLEVEL_NORMAL)
        System.out.println("Merging collection: " + collectionName + " of type " + collectionType + ".");
        if (!collectionType.equals(overlayCollectionType)) {
            System.err.println("Can not merge collections: " + collectionName
                    + " of type " + collectionType + " and " + overlayCollectionType);
            return false;
        }

        // move the overlay hits in time, signal should have been moved already
        List overlayEntries = this.moveCollectionToTime(overlayCollection, overlayTime);
        //List overlayEntries = overlayCollection.getEvent().get(overlayCollectionType, overlayCollection.getName());
        // Check if there are actually entries to overlay
        if (overlayEntries.isEmpty())
            return true;
        EventHeader event = collection.getEvent();

        if (collectionType.isAssignableFrom(MCParticle.class)) {
            // Nothing to do. Only add mc particles that are connected to something kept in the event.
            // This is done in the other steps below.
            if (!collectionName.equals(event.MC_PARTICLES)) {
                event.get(MCParticle.class, collectionName).addAll(overlayEntries);
            }

        } else if (collectionType.isAssignableFrom(SimTrackerHit.class)) {
            // SimTrackerHits: just append all hits from overlayEvents
            List<SimTrackerHit> signalTrackerHits = event.get(SimTrackerHit.class, collectionName);

            // add contributing mc particles to lists
            for (SimTrackerHit hit : (List<SimTrackerHit>) overlayEntries) {
                SimTrackerHit overlayHit = copySimTrackerHit(hit, collection);
                signalTrackerHits.add(overlayHit);
            }
//            System.out.println("Overlaid TrackerHits!");
        } else if (collectionType.isAssignableFrom(SimCalorimeterHit.class)) {
            // SimCalorimeterHits: need to merge hits in cells which are hit in both events
            // check if map has already been filled
            Map<Long, SimCalorimeterHit> hitMap;
            List<SimCalorimeterHit> signalCaloHits = event.get(SimCalorimeterHit.class, collectionName);
            if (!caloHitMap.containsKey(collectionName)) {
                // build map of cells which are hit in signalEvent
                hitMap = new HashMap<Long, SimCalorimeterHit>();
                for (SimCalorimeterHit hit : signalCaloHits) {
                    hitMap.put(hit.getCellID(), hit);
                }
                caloHitMap.put(collectionName, hitMap);
            } else {
                hitMap = caloHitMap.get(collectionName);
            }

            boolean hasPDG = LCIOUtil.bitTest(collection.getFlags(), LCIOConstants.CHBIT_PDG);
            // loop over the hits from the overlay event
            int nHitsMerged = 0;
            int nSimCaloHits = overlayEntries.size();
            for (SimCalorimeterHit hit : (List<SimCalorimeterHit>) overlayEntries) {
                long cellID = hit.getCellID();

                nHitsMerged++;
                if (this.getHistogramLevel() > HLEVEL_HIGH && nHitsMerged % 100 == 0)
                    System.out.print("Merged " + nHitsMerged + " / " + nSimCaloHits + " hits\n");
                if (hitMap.containsKey(cellID)) {
                    SimCalorimeterHit oldHit = hitMap.get(hit.getCellID());
                    int nHitMcP = oldHit.getMCParticleCount();
                    int nOverlayMcP = hit.getMCParticleCount();
                    int nMcP = nHitMcP + nOverlayMcP;
                    // arrays of mc particle contributions to the hit
                    Object[] mcpList = new Object[nMcP];
                    float[] eneList = new float[nMcP];
                    float[] timeList = new float[nMcP];
                    int[] pdgList = null;
                    if (hasPDG)
                        pdgList = new int[nMcP];
                    double rawEnergy = 0.;
                    // fill arrays with values from hit
                    for (int i = 0; i != nHitMcP; i++) {
                        mcpList[i] = oldHit.getMCParticle(i);
                        eneList[i] = (float) oldHit.getContributedEnergy(i);
                        timeList[i] = (float) oldHit.getContributedTime(i);
                        if (hasPDG)
                            pdgList[i] = oldHit.getPDG(i);
                        rawEnergy += eneList[i];
                    }
                    // add values of overlay hit
                    for (int i = 0; i != nOverlayMcP; i++) {
                        int j = nHitMcP + i;
                        MCParticle hitMC = hit.getMCParticle(i);
                        if (hitMC != null) {
                            if (!mcParticleReferences.containsKey(hitMC)) {
                                this.addOverlayMcParticle(hitMC);
                            }
                            mcpList[j] = mcParticleReferences.get(hitMC);
                        }
                        eneList[j] = (float) hit.getContributedEnergy(i);
                        timeList[j] = (float) hit.getContributedTime(i);
                        if (hasPDG)
                            pdgList[j] = hit.getPDG(i);
                        rawEnergy += eneList[j];
                    }
                    // need to set time to 0 so it is recalculated from the timeList
                    SimCalorimeterHit mergedHit = new BaseSimCalorimeterHit(oldHit.getCellID(),
                            rawEnergy, 0., mcpList, eneList, timeList, pdgList, collection);
                    // replace old hit with merged hit
                    signalCaloHits.remove(oldHit);
                    signalCaloHits.add(mergedHit);
                    hitMap.put(cellID, mergedHit);
                } else {
                    SimCalorimeterHit overlayHit = copySimCalorimeterHit(hit, collection, hasPDG);
                    signalCaloHits.add(overlayHit);
                    hitMap.put(cellID, overlayHit);
                }

            }
        } else if (collectionType.isAssignableFrom(GenericObject.class)) {
            // need to implement all kinds of possible GenericObjects separately
            if (collectionName.equals("MCParticleEndPointEnergy")) {
                // TODO decide what to do with this collection in the overlay events
                // TODO would need to resolve the position of kept mc particles and keep the same position here
                //event.get(GenericObject.class, collectionName).addAll(overlayEntries);
                //event.remove("MCParticleEndPointEnergy");
            } else {
                System.err.println("Can not merge collection " + collectionName
                        + " of type " + collectionType + ". Unhandled type.");
                return false;
            }
        }
        return true;
    }

    /**
     * Deep copy of an SimCalorimeterHit. Necessary in order to be able to close an
     * overlay event.
     * @param hit The hit to be copied
     * @param meta The meta data that will be attached to the hit
     * @param hasPDG Flag if the pdg code of the mc contriutions should be saved
     * @return The copied SimCalorimeterHit
     */
    protected SimCalorimeterHit copySimCalorimeterHit(SimCalorimeterHit hit, LCMetaData meta, boolean hasPDG) {
        long id = hit.getCellID();
        double rawEnergy = hit.getRawEnergy();
        double time = hit.getTime();
        int nMCP = hit.getMCParticleCount();
        Object[] mcparts = new Object[nMCP];
        float[] energies = new float[nMCP];
        float[] times = new float[nMCP];
        int[] pdgs = null;
        if (hasPDG)
            pdgs = new int[nMCP];
        // fill arrays with values from hit
        for (int i = 0; i != nMCP; i++) {
            MCParticle hitMC = hit.getMCParticle(i);
            if (hitMC != null) {
                this.addOverlayMcParticle(hitMC);
                mcparts[i] = mcParticleReferences.get(hitMC);
            }
            energies[i] = (float) hit.getContributedEnergy(i);
            times[i] = (float) hit.getContributedTime(i);
            if (hasPDG)
                pdgs[i] = hit.getPDG(i);
        }

        BaseSimCalorimeterHit copyHit = new BaseSimCalorimeterHit(id, rawEnergy, time, mcparts, energies, times, pdgs, meta);

        return copyHit;
    }

    /**
     * Deep copy of an SimTrackerHit. Necessary in order to be able to close an
     * overlay event.
     * @param hit The hit to be copied
     * @param meta The meta data that will be attached to the hit
     * @return The copied SimTrackerHit
     */
    protected SimTrackerHit copySimTrackerHit(SimTrackerHit hit, LCMetaData meta) {

        double[] position = new double[3];
        double[] momentum = new double[3];
        double[] hitp = hit.getPosition();
        double[] hitm = hit.getMomentum();
        for (int i = 0; i != 3; i++) {
            position[i] = hitp[i];
            momentum[i] = hitm[i];
        }
        double dEdx = hit.getdEdx();
        double pathLength = hit.getPathLength();
        double time = hit.getTime();
        int cellID = hit.getCellID();
        MCParticle hitMC = hit.getMCParticle();
        MCParticle mcParticle = null;
        if (hitMC != null) {
            this.addOverlayMcParticle(hitMC);
            mcParticle = mcParticleReferences.get(hitMC);
        }
        IDetectorElement de = null;

        return new BaseSimTrackerHit(position, dEdx, momentum, pathLength, time, cellID, mcParticle, meta, de);
    }

    /**
     * Deep copy of an mc particle. Necessary in order to be able to close an
     * overlay event. The parent and daught relations are <b>not</b> set for the
     * copied mc particle. Because those should most likely also point to copies
     * this should be handled somewhere else.
     * @param mcParticle The mc particle to be copied
     * @return the copied mc particle
     */
    static public MCParticle copyMcParticle(MCParticle mcParticle) {
        Hep3Vector origin = new BasicHep3Vector(mcParticle.getOriginX(), mcParticle.getOriginY(), mcParticle.getOriginZ());
        HepLorentzVector p = new BasicHepLorentzVector(mcParticle.getEnergy(), new double[]{mcParticle.getPX(), mcParticle.getPY(), mcParticle.getPZ()});
        ParticleType ptype = mcParticle.getType().getParticlePropertyProvider().get(mcParticle.getPDGID());
        int status = mcParticle.getGeneratorStatus();
        double time = mcParticle.getProductionTime();
        BaseMCParticle copyMcP = new BaseMCParticle(origin, p, ptype, status, time);
        // override the mass and charge from the particle type to prevent unknown particle exceptions
        copyMcP.setMass(mcParticle.getMass());
        copyMcP.setCharge(mcParticle.getCharge());
        copyMcP.setSimulatorStatus(mcParticle.getSimulatorStatus().getValue());
        return copyMcP;
    }

    /**
     * Copies an mc particle and stores it together with  the copy in a map.
     * Adds it to the list of mc particles as well as the overlay mc particles.
     * Also copies and keeps all ancestors.
     * @param particle
     */
    protected void addOverlayMcParticle(MCParticle particle) {
        if (!mcParticleReferences.containsKey(particle)) {
            // keep a copy of the mc particle instead of the original in order to close the background event
            MCParticle mcp = copyMcParticle(particle);
            mcParticleReferences.put(particle, mcp);
            overlayMcParticles.add(mcp);
            allMcParticles.add(mcp);
            List<MCParticle> parents = particle.getParents();
            // keep the parents as well and set the parent daughter relations
            for (MCParticle parent : parents) {
                this.addOverlayMcParticle(parent);
                ((BaseMCParticle) mcParticleReferences.get(parent)).addDaughter(mcp);
            }
        }
    }

    private static void usage() {
        System.out.println("java MergeBunches [-o <format>] [<input> [<output>]]");
        System.exit(0);
    }
    private String format;

    void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("-o".equals(arg)) {
                i++;
                if (i >= args.length)
                    usage();
                format = args[i];
            }

        }


    }
}
