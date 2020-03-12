/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package org.hps.recon.ecal;

import org.lcsim.util.Driver;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.hodoscope.HodoscopeConditions;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.lcio.LCIOConstants;

import java.util.List;
import java.util.ArrayList;
//import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;

/**
 *
 * @author rafopar
 */
public class HodoRawConverterDriver extends Driver {

    // To import database conditions
    private HodoscopeConditions hodoConditions = null;

    private HodoRawConverter converter = null;

    // ===== The Mode1 Hodo hit collection name =====
    private String rawCollectionName = "HodoReadoutHits";

    private String hodoCollectionName = "HodoCalHits";

    // ===== **NOTE** Seems this name can not be arbitrary, it is taken from the detector
    // ===== For example you can find out this by running this method of the detector
    // ===== System.out.println("==== The Detector SubDetector hit collection name " + detector.getSubdetector("Hodoscope").getHitsCollectionName() );
    private final String hodoReadoutName = "HodoscopeHits";

    public HodoRawConverterDriver() {
        converter = new HodoRawConverter();
    }

    public void setUseRunningPedestal(boolean useRunningPedestal) {
        converter.setUseRunningPedestal(useRunningPedestal);
    }
    
    public void setUseUserGains(double aUserGain){
        converter.setUseUserGain(aUserGain);
    }

    public void setTETAllChannels(int arg_tet) {
        if (arg_tet <= 0)
            throw new RuntimeException("TET value should be a positive integer");

        converter.setTETAllChannels(arg_tet);
    }

    @Override
    public void startOfData() {
        if (hodoCollectionName == null)
            throw new RuntimeException("The parameter hodoCollectionName was not set!");

    }

    @Override
    public void detectorChanged(Detector detector) {

        // Hodo conditions object.
        hodoConditions = DatabaseConditionsManager.getInstance().getHodoConditions();

        converter.setConditions(hodoConditions);

    }

    @Override
    public void process(EventHeader event) {

        ArrayList<CalorimeterHit> hodoHits = new ArrayList<CalorimeterHit>();

        if (event.hasCollection(RawTrackerHit.class, rawCollectionName)) {

            int flags = 0;
            flags += 1 << LCIOConstants.RCHBIT_TIME; // store hit time
            flags += 1 << LCIOConstants.RCHBIT_LONG; // store hit position; this flag has no effect for RawCalorimeterHits

            // ======= Getting list of Mode1 hits from the event ======
            List<RawTrackerHit> hits = event.get(RawTrackerHit.class, rawCollectionName);

            // ======= Loop over hits, and reconstruct energies and times for all hits =====
            for (RawTrackerHit hit : hits) {

                // Getting the cellID of the hit
                final long cellID = hit.getCellID();

                double ped = converter.getPedestal(event, cellID);

                //System.out.println(Arrays.toString(hodo_identifiers));
                // ====== Get Number of Threshold Crossings ==========

                ArrayList<Integer> thr_crosings = converter.FindThresholdCrossings(hit, ped);

                // ===== For now we will calculate coarse time, which is the threshold crossing sample time.
                // ===== Later will implement the mode7 time
                ArrayList<CalorimeterHit> hits_in_this_channel = converter.getCaloHits(hit, thr_crosings, ped);
                
                // Propagate the detector element information to these found_hits so it can be used later.
                for(CalorimeterHit found_hit: hits_in_this_channel) {
                    found_hit.setDetectorElement(hit.getDetectorElement());
                }

                hodoHits.addAll(hits_in_this_channel);

            }

            // ====== Adding hodo hits to as a Calorimeter hit to the event.
            event.put(hodoCollectionName, hodoHits, CalorimeterHit.class, flags, hodoReadoutName);

            //== == == == == == == == == == == == == == == == == == == == == == 
            //  Now start to do a clustering, which is combining hits together from hits
            //  that are from the same time, but are in opposite holes. They should also be 
            //  in time coincidence
            // =========== The EVENT::Cluster  Object is not a perfect collection for storing hodo clusters
            // =========== since it doesn't provide ready methods for getting, ix, iy, layer
            // =========== therefore Hodo clusters will be stored in a genertic object.
            // === There will be 5 genericCollections for ix, iy, layer,, Energy and time.
            // === All these collections will have same size, which is the number of all hits.
            // === i-th value of each collection shows the corresponding value of the i-th hit
            int[] cl_ix = new int[]{};           // This array contains ix of hits
            int[] cl_iy = new int[]{};           // THis array contains iy of hits
            int[] cl_layer = new int[]{};        // THis array contains layer of hits
            double[] cl_Energy = new double[]{}; // THis array contains the energy of hits
            double[] cl_Time = new double[]{};   // THis array contains the Time of hits
            int[] cl_detid = new int[]{};
            // ======== Defining GenericObjects for Hodo Hit components, ix, it etc
            SimpleGenericObject generic_cl_ix = new SimpleGenericObject();
            SimpleGenericObject generic_cl_iy = new SimpleGenericObject();
            SimpleGenericObject generic_cl_layer = new SimpleGenericObject();
            SimpleGenericObject generic_cl_energy = new SimpleGenericObject();
            SimpleGenericObject generic_cl_time = new SimpleGenericObject();
            SimpleGenericObject generic_cl_detid = new SimpleGenericObject();
            ArrayList<Integer> paired = new ArrayList<Integer>();

            for (int i = 0; i < hodoHits.size(); i++) {

                // Check if this hit is already paired, if so, then let's pass to the next hit
                if (paired.contains(i))
                    continue;

                CalorimeterHit this_hit = hodoHits.get(i);
               
                // ====== These tiles are readout with a single PMT channel, and therefore for these 
                // ====== tiles cluster and the hit are identical 
                int hit_ix = this_hit.getIdentifierFieldValue("ix");
                int hit_iy = this_hit.getIdentifierFieldValue("iy");
                int hit_ilayer=this_hit.getIdentifierFieldValue("layer");
                int hit_ihole=this_hit.getIdentifierFieldValue("hole");
                
                if (hit_ix == 0 || hit_ix == 4) {

                    cl_ix = ArrayUtils.add(cl_ix, hit_ix);
                    cl_iy = ArrayUtils.add(cl_iy, hit_iy);
                    cl_layer = ArrayUtils.add(cl_layer, hit_ilayer);
                    cl_Energy = ArrayUtils.add(cl_Energy, this_hit.getRawEnergy());
                    cl_Time = ArrayUtils.add(cl_Time, this_hit.getTime());
                    cl_detid = ArrayUtils.add(cl_detid, (int)this_hit.getDetectorElement().getIdentifier().getValue());

                    continue;
                }

                boolean pair_found = false;

                for (int j = i + 1; j < hodoHits.size(); j++) {
                    
                    CalorimeterHit that_hit = hodoHits.get(j);
                    // ====== These tiles are readout with a single PMT channel, and therefore for these 
                    // ====== tiles cluster and the hit are identical 
                    int hit_jx = that_hit.getIdentifierFieldValue("ix");
                    int hit_jy = that_hit.getIdentifierFieldValue("iy");
                    int hit_jlayer=that_hit.getIdentifierFieldValue("layer");
                    int hit_jhole=that_hit.getIdentifierFieldValue("hole");
                    

                    // Check if this hit is already paired, if so, then let's pass to the next hit
                    if (paired.contains(j))
                        continue;

                    if (hit_ix == hit_jx && hit_iy == hit_jy && hit_ilayer == hit_jlayer
                            && Math.abs(this_hit.getTime() - that_hit.getTime()) < HodoConstants.cl_hit_dtMax && 
                            ( hit_ihole * hit_jhole == -1)) {

                        pair_found = true;
                        paired.add(j);

                        cl_ix = ArrayUtils.add(cl_ix, hit_ix);
                        cl_iy = ArrayUtils.add(cl_iy, hit_iy);
                        cl_layer = ArrayUtils.add(cl_layer, hit_ilayer);
                        cl_detid = ArrayUtils.add(cl_detid, (int)this_hit.getDetectorElement().getIdentifier().getValue());

                        double energy = HodoConstants.cl_Esum_scale * (this_hit.getRawEnergy() + that_hit.getRawEnergy()) / 2.;

                        // === SOme kind of an semiarbitrary function. This makes the hit time to be closer to the one with the highest energy
                        double time = this_hit.getTime() + ( that_hit.getTime() - this_hit.getTime()) * that_hit.getRawEnergy() / (this_hit.getRawEnergy() + that_hit.getRawEnergy());

                        cl_Energy = ArrayUtils.add(cl_Energy, energy);
                        cl_Time = ArrayUtils.add(cl_Time, time);

                        continue;
                    }

                }

                // ===== In case if no pair is found, then make a cluster, which will have identical energy and time as this hit
                if (!pair_found) {
                    cl_ix = ArrayUtils.add(cl_ix, hit_ix);
                    cl_iy = ArrayUtils.add(cl_iy, hit_iy);
                    cl_layer = ArrayUtils.add(cl_layer, hit_ilayer);
                    cl_Energy = ArrayUtils.add(cl_Energy, this_hit.getRawEnergy());
                    cl_Time = ArrayUtils.add(cl_Time, this_hit.getTime());
                    cl_detid = ArrayUtils.add(cl_detid, (int)this_hit.getDetectorElement().getIdentifier().getValue());
                }

            }

            // ====== At this point clustering is finished, we are ready to write them into data =====
            generic_cl_ix.setIntValues(cl_ix);
            generic_cl_iy.setIntValues(cl_iy);
            generic_cl_layer.setIntValues(cl_layer);
            generic_cl_energy.setDoubleValues(cl_Energy);
            generic_cl_time.setDoubleValues(cl_Time);
            generic_cl_detid.setIntValues(cl_detid);

            // ====== In order to write generic object in the file, 1st we should add generic objects
            // ====== in the List, so we will add above genericObjects into the list below
            List<SimpleGenericObject> hodo_cl_list = new ArrayList();

            hodo_cl_list.add(generic_cl_ix);
            hodo_cl_list.add(generic_cl_iy);
            hodo_cl_list.add(generic_cl_layer);
            hodo_cl_list.add(generic_cl_energy);
            hodo_cl_list.add(generic_cl_time);
            hodo_cl_list.add(generic_cl_detid);

            // Writing HodoCluster data into the event
            event.put("HodoGenericClusters", hodo_cl_list, SimpleGenericObject.class, 0);

        }
    }

    /**
     * Set to <code>true</code> to use a running pedestal calibration from mode
     * 7 data.
     * <p>
     * The running pedestal values are retrieved from the event collection
     * "HodoRunningPedestals" which is a <code>Map</code> between
     * {@link org.hps.conditions.ecal.HodoscopeChannel} objects are their
     * average pedestal.
     *
     * @param useRunningPedestal True to use a running pedestal value.
     */
}
