package org.hps.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.lcsim.event.EventHeader;
import org.lcsim.event.EventHeader.LCMetaData;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.util.Driver;

/**
 * Driver to filter out MCParticle objects that have hits in the event
 * and save them to a new collection or replace the existing MCParticle
 * collection.
 *
 * @author Jeremy McCormick, SLAC
 *
 */
public class MCParticleFilterDriver extends Driver {

    // Logger is defined at class level for now.
    static Logger LOG = Logger.getLogger(MCParticleFilterDriver.class.getCanonicalName());

    Set<MCParticle> saveParticles = new HashSet<MCParticle>();
    List<String> hitCollectionNames = new ArrayList<String>();
    String collName = "FilteredMCParticles";
    boolean replace = false;
    boolean saveFiltered = true;
    boolean addAllHitCollections = false;

    public void startOfData() {
        if (!this.addAllHitCollections && hitCollectionNames.isEmpty()) {
            throw new RuntimeException("No hit collection names provided in steering and addAllHitCollections is not enabled.");
        }
    }

    public void setHitCollectionNames(String[] hitCollectionNames) {
        this.hitCollectionNames.addAll(Arrays.asList(hitCollectionNames));
    }

    public void setHitCollectionName(String hitCollectionName) {
        this.hitCollectionNames.add(hitCollectionName);
    }

    public void setFilteredCollectionName(String collName) {
        this.collName = collName;
    }

    public void setReplaceMCParticleCollection(boolean replace) {
        this.replace = replace;
    }

    public void setSaveFilteredCollection(boolean saveFiltered) {
        this.saveFiltered = saveFiltered;
    }

    public void setAddAllHitCollections(boolean addAllHitCollections) {
        this.addAllHitCollections = addAllHitCollections;
    }

    public void process(EventHeader event) {

        LOG.fine("Processing event " + event.getEventNumber());

        this.saveParticles.clear();

        // Get original MCParticle list.
        List<MCParticle> particles = event.get(MCParticle.class, EventHeader.MC_PARTICLES);

        LOG.fine("Original MCParticle collection size: " + particles.size());

        /*
           Use all collections if none were set and this is enabled, which is checked
           in startOfData(). This will only happen once, at the first event when the
           collection name list is empty. So if collection names vary event-by-event,
           this will not work correctly.
         */
        if (this.hitCollectionNames.size() == 0) {
            LOG.info("Adding all sim hit collection names (this will only happen once!)");
            List<List<SimTrackerHit>> simTrackerHitCollections = event.get(SimTrackerHit.class);
            for (List<SimTrackerHit> simTrackerHitCollection : simTrackerHitCollections) {
                this.hitCollectionNames.add(event.getMetaData(simTrackerHitCollection).getName());
            }
            List<List<SimCalorimeterHit>> simCalorimeterHitCollections = event.get(SimCalorimeterHit.class);
            for (List<SimCalorimeterHit> simCalorimeterHitCollection : simCalorimeterHitCollections) {
                this.hitCollectionNames.add(event.getMetaData(simCalorimeterHitCollection).getName());
            }
        }

        LOG.fine("Checking particles in " + hitCollectionNames.size() + " collections...");

        // Add MCParticle objects which have hits.
        for (String collectionName : hitCollectionNames) {
            if (event.hasItem(collectionName)) {
                LOG.info("Checking for particles in collection: " + collectionName);
                List<?> metaList = (List<?>)event.get(collectionName);
                LCMetaData meta = event.getMetaData(metaList);
                Class<?> collType = meta.getType();
                int nBefore = this.saveParticles.size();
                if (SimTrackerHit.class.isAssignableFrom(collType)) {
                    checkSimTrackerHits(event.get(SimTrackerHit.class, collectionName));
                } else if (SimCalorimeterHit.class.isAssignableFrom(collType)) {
                    checkSimCalorimeterHits(event.get(SimCalorimeterHit.class, collectionName));
                }
                int nAfter = this.saveParticles.size();
                LOG.info("Added " + (nAfter - nBefore) + " particles from " + collectionName);
            } else {
                LOG.warning("Event is missing collection: " + collectionName);
            }
        }

        // Save the full particle parentage chain for any particles with hits.
        Set<MCParticle> savedParents = new HashSet<MCParticle>();
        for (MCParticle particle : this.saveParticles) {
            saveParents(particle, savedParents);
        }
        LOG.fine("Adding " + savedParents.size() + " extra parent particles");
        this.saveParticles.addAll(savedParents);

        // Get flags from the original collection.
        int pflags = event.getMetaData(particles).getFlags();

        // Make a list from the set of saved particles.
        List<MCParticle> filteredParticleCollection = new ArrayList<MCParticle>();
        filteredParticleCollection.addAll(saveParticles);

        LOG.fine("Filtered particle collection size: " + filteredParticleCollection.size());

        // Save the new filtered collection.
        if (this.saveFiltered) {
            LOG.fine("Saving filtered particles to event");
            event.put(collName,
                    filteredParticleCollection,
                    MCParticle.class,
                    pflags);
        }

        // Replace the original collection if enabled.
        if (this.replace) {
            LOG.fine("Replacing original MCParticle collection with filtered particles");
            event.remove(EventHeader.MC_PARTICLES);
            event.put(EventHeader.MC_PARTICLES,
                    filteredParticleCollection,
                    MCParticle.class,
                    pflags);
        }

        LOG.fine("Done processing event!");
    }

    private void checkSimTrackerHits(List<SimTrackerHit> hits) {
        for (SimTrackerHit hit : hits) {
            saveParticles.add(hit.getMCParticle());
        }
    }

    private void checkSimCalorimeterHits(List<SimCalorimeterHit> hits) {
        for (SimCalorimeterHit hit : hits) {
            for (int i = 0; i < hit.getMCParticleCount(); i++) {
                saveParticles.add(hit.getMCParticle(i));
            }
        }
    }

    private void saveParents(MCParticle particle, Set<MCParticle> savedParents) {
        if (particle.getParents().size() == 0) {
            return;
        }
        for (MCParticle parent : particle.getParents()) {
            savedParents.add(parent);
            saveParents(parent, savedParents);
        }
    }
}