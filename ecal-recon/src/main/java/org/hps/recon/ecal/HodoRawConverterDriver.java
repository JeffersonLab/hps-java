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

    @Override
    public void startOfData() {
        if (hodoCollectionName == null) {
            throw new RuntimeException("The parameter ecalCollectionName was not set!");
        }

    }

    @Override
    public void detectorChanged(Detector detector) {

//        System.out.println("==== The Detector Name is " + detector.getDetectorName());
//        System.out.println("==== The DetectorElement Name is " + detector.getDetectorElement().getName() );
//        System.out.println("==== The Detector SubDetector hit collection name " + detector.getSubdetector("Hodoscope").getHitsCollectionName() );
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

            // =========== The CalorimeterHit Object is not a perfect collection for storing hodo hit
            // =========== since it doesn't provide ready methods for getting, ix, iy, layer, hole
            // =========== therefore Hodo hits will be stored in a genertic object.
            // === There will be 6 genericCollections for ix, iy, layer, hole, Energy and time.
            // === All these collections will have same size, which is the number of all hits.
            // === i-th value of each collection shows the corresponding value of the i-th hit
            int[] hit_ix = new int[]{};           // This array contains ix of hits
            int[] hit_iy = new int[]{};           // THis array contains iy of hits
            int[] hit_layer = new int[]{};        // THis array contains layer of hits
            int[] hit_hole = new int[]{};         // THis array contains the hole of hits
            double[] hit_Energy = new double[]{}; // THis array contains the energy of hits
            double[] hit_Time = new double[]{};   // THis array contains the Time of hits

            // ======== Defining GenericObjects for Hodo Hit components, ix, it etc
            SimpleGenericObject generic_ix = new SimpleGenericObject();
            SimpleGenericObject generic_iy = new SimpleGenericObject();
            SimpleGenericObject generic_layer = new SimpleGenericObject();
            SimpleGenericObject generic_hole = new SimpleGenericObject();
            SimpleGenericObject generic_energy = new SimpleGenericObject();
            SimpleGenericObject generic_time = new SimpleGenericObject();

            // ====== In order to write generic object in the file, 1st we should add generic objects
            // ====== in the List, so we will add above genericObjects into the list below
            List<SimpleGenericObject> hodo_hit_list = new ArrayList();

            // ======= Getting list of Mode1 hits from the event ======
            List<RawTrackerHit> hits = event.get(RawTrackerHit.class, rawCollectionName);

            // ======= Loop over hits, and reconstruct energies and times for all hits =====
            for (RawTrackerHit hit : hits) {

                // Getting the cellID of the hit
                final long cellID = hit.getCellID();

                double ped = converter.getPedestal(event, cellID);

                int[] hodo_identifiers = converter.getHodoIdentifiers(cellID);

                //System.out.println(Arrays.toString(hodo_identifiers));

                // ====== Get Number of Threshold Crossings ==========
                ArrayList<Integer> thr_crosings = converter.FindThresholdCrossings(hit, ped);

                //System.out.println("# of thr crossings = " + thr_crosings.size());
                // ===== For now we will calculate coars time, which is the threshold crossing sample time.
                // ===== Later will implement the mode7 time
                ArrayList<CalorimeterHit> hits_in_this_channel = converter.getCaloHits(hit, thr_crosings, ped);

                int n_hit_in_this_channel = hits_in_this_channel.size();

                // ===== Loop over all hits for this channel, and fill corresponding arrays for ix, iy etc
                for (int ind_cur_hit = 0; ind_cur_hit < n_hit_in_this_channel; ind_cur_hit++) {
                    double energy = hits_in_this_channel.get(ind_cur_hit).getRawEnergy();
                    double time = hits_in_this_channel.get(ind_cur_hit).getTime();                    
                    
                    hit_ix = ArrayUtils.add(hit_ix, hodo_identifiers[0]);
                    hit_iy = ArrayUtils.add(hit_iy, hodo_identifiers[1]);
                    hit_layer = ArrayUtils.add(hit_layer, hodo_identifiers[2]);
                    hit_hole = ArrayUtils.add(hit_hole, hodo_identifiers[3]);
                    hit_Energy = ArrayUtils.add(hit_Energy, energy);
                    hit_Time = ArrayUtils.add(hit_Time, time);
                }

                hodoHits.addAll(hits_in_this_channel);

                // ===== The following few lines are the equivalent of the cin.ignore() of C
//                try {
//                    System.in.read();
//                } catch (Exception e) {
//                }
            }
            
            // ====== Adding hodo hits to as a Calorimeter hit to the event, however we might later drop this
            // ====== sinc this is doesn't provide more infoarmation that exists in the "HodoGenericHits" collection
            event.put(hodoCollectionName, hodoHits, CalorimeterHit.class, flags, hodoReadoutName);

            generic_ix.setIntValues(hit_ix);
            generic_iy.setIntValues(hit_iy);
            generic_layer.setIntValues(hit_layer);
            generic_hole.setIntValues(hit_hole);
            generic_energy.setDoubleValues(hit_Energy);
            generic_time.setDoubleValues(hit_Time);

            hodo_hit_list.add(generic_ix);
            hodo_hit_list.add(generic_iy);
            hodo_hit_list.add(generic_layer);
            hodo_hit_list.add(generic_hole);
            hodo_hit_list.add(generic_energy);
            hodo_hit_list.add(generic_time);

            // Writing HodoHit data into the event
            event.put("HodoGenericHits", hodo_hit_list, SimpleGenericObject.class, 0);

        }
    }

}
