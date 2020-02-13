package org.hps.monitoring.drivers.hodoscope;

import java.util.List;
import java.util.logging.Logger;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.hodoscope.HodoscopeConditions;
import org.hps.conditions.hodoscope.HodoscopeChannel;
import org.hps.conditions.hodoscope.HodoscopeChannel.HodoscopeChannelCollection;

import org.hps.recon.ecal.HodoRawConverter;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.ITree;
import hep.aida.ref.rootwriter.RootFileStore;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.Subdetector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

import org.lcsim.event.Cluster;

import org.hps.recon.ecal.SimpleGenericObject;

import org.lcsim.event.RawTrackerHit;

/**
 *
 * @author mgraham
 */
public class HodoscopePlots extends Driver {

    // Get Hodo conditions
    // To import database conditions
    private HodoscopeConditions hodoConditions = null;

    // HodoRawConverter has some methods that we want to use here
    private HodoRawConverter converter = null;

    HodoUtils utils = new HodoUtils();

    // Plotting
    private static ITree tree = null;
    private IAnalysisFactory analysisFactory = AIDA.defaultInstance().analysisFactory();
    private IPlotterFactory plotterFactory = analysisFactory.createPlotterFactory("FADC");
    private IPlotterFactory plotFacthitEnergies = analysisFactory.createPlotterFactory("Hit Energies");
    private IPlotterFactory plotFactTileEnergies = analysisFactory.createPlotterFactory("Tile Energies");
    private IHistogramFactory histogramFactory = null;

    private IIdentifierHelper helper = null;
    private static final Logger LOGGER = Logger.getLogger(HodoscopePlots.class.getCanonicalName());
    private HodoscopeChannelCollection hodoChannels = null;

    private static final int n_PMT_ch = 16;

    private static Map<String, IPlotter> plotters = new HashMap<String, IPlotter>();

    // === Maps channel name to the energy of theat channel
    private static Map<String, Double> m_hit_Energies = new HashMap<String, Double>();

    // === Maps Hoto tile ID to the energy
    private static Map<HodoUtils.HodoTileIdentifier, Double> m_tile_Energies = new HashMap<HodoUtils.HodoTileIdentifier, Double>();

    private static Map<String, IHistogram1D> fADC_Spectrum_Top = new HashMap<String, IHistogram1D>();
    private static Map<String, IHistogram1D> fADC_Spectrum_Bot = new HashMap<String, IHistogram1D>();

    private static Map<String, IHistogram1D> fADC_Spectrum_Top_lvl2 = new HashMap<String, IHistogram1D>();
    private static Map<String, IHistogram1D> fADC_Spectrum_Bot_lvl2 = new HashMap<String, IHistogram1D>();

    private static Map<String, IHistogram1D> tileEnergy = new HashMap<String, IHistogram1D>();
    private static Map<String, IHistogram1D> tileEnergy2 = new HashMap<String, IHistogram1D>();
    private static Map<String, IHistogram1D> tileEnergy3 = new HashMap<String, IHistogram1D>();
    private static Map<String, IHistogram1D> hitEnergy = new HashMap<String, IHistogram1D>();

    private static Map<String, IHistogram1D> matchedHitEnergy = new HashMap<String, IHistogram1D>();

    private static Map<String, IHistogram1D> occupancyLayerPlots = new HashMap<String, IHistogram1D>();
    private static Map<String, int[]> occupancyLayerMap = new HashMap<String, int[]>();

    private static Map<String, IHistogram2D> clust_polots = new HashMap<String, IHistogram2D>();

    private static IHistogram1D hECalCoordtoIndex = null;

    private Subdetector subDetector;
    private static final String subdetectorName = "Hodoscope";
    private static final String SUBDETECTOR_NAME = "Tracker"; // CHANGE THIS to whatever the hodoscope is
    private String triggerBankCollectionName = "TriggerBank";
    private String ecalReadoutCollectionName = "EcalReadoutHits";
    private String ecalClustersCollectionName = "EcalClusters";

    // ===== The Mode1 Hodo hit collection name =====
    private String rawCollectionName = "HodoReadoutHits";
    private String hodoCollectionName = "HodoCalHits";
    private final String hodoReadoutCollectionName = "HodoscopeHits";
    private String hodoHitsCollectionName = "HodoGenericHits";
    private String hodoClustersCollectionName = "HodoGenericClusters";

    private String ecalClusterName = "EcalClusters";
    private String ecalClusterCorrName = "EcalClustersCorr";

    private int eventCount = 0;
    private int eventRefreshRate = 1;
    private int runNumber = -1;
    private int resetPeriod = -1;

    private boolean saveRootFile = false;
    //private boolean saveRootFile = true;

    @Override
    protected void detectorChanged(Detector detector) {

        utils = new HodoUtils();
        hodoConditions = DatabaseConditionsManager.getInstance().getHodoConditions();

        converter = new HodoRawConverter();

        tree = AIDA.defaultInstance().tree();
        tree.cd("/");// aida.tree().cd("/");
        histogramFactory = analysisFactory.createHistogramFactory(tree);
        // Histogram maps

        hECalCoordtoIndex = histogramFactory.createHistogram1D("h_ECalCoord2Index", "", utils.cl_x_edges);

        plotters.put("Top FADC Spectra", plotterFactory.create("Top FADC Spectra"));
        plotters.get("Top FADC Spectra").createRegions(4, 4);

        plotters.put("Bot FADC Spectra", plotterFactory.create("Bot FADC Spectra"));
        plotters.get("Bot FADC Spectra").createRegions(4, 4);

        plotters.put("Top FADC lvl2", plotterFactory.create("Top FADC lvl2"));
        plotters.get("Top FADC lvl2").createRegions(4, 4);

        plotters.put("Bot FADC lvl2", plotterFactory.create("Bot FADC lvl2"));
        plotters.get("Bot FADC lvl2").createRegions(4, 4);

        plotters.put("Hit Energies", plotFacthitEnergies.create("Hit Energies"));
        plotters.get("Hit Energies").createRegions(8, 4);

        plotters.put("Matched Hit Energies", plotFacthitEnergies.create("Matched Hit Energies"));
        plotters.get("Matched Hit Energies").createRegions(8, 4);

        plotters.put("Raw Tile Energies", plotFactTileEnergies.create("Raw Tile Energies"));
        plotters.get("Raw Tile Energies").createRegions(5, 4);

        plotters.put("Tile Energies: Level1", plotFactTileEnergies.create("Tile Energies: Level1"));
        plotters.get("Tile Energies: Level1").createRegions(5, 4);

        plotters.put("Tile Energies: Level2", plotFactTileEnergies.create("Tile Energies: Level2"));
        plotters.get("Tile Energies: Level2").createRegions(5, 4);

        for (int ich = 0; ich < n_PMT_ch; ich++) {
            fADC_Spectrum_Top.put(String.format("Top fADC %d", ich), histogramFactory.createHistogram1D(String.format("Top fADC %d", ich), 33, -0.5, 32.5));
            fADC_Spectrum_Bot.put(String.format("Bot fADC %d", ich), histogramFactory.createHistogram1D(String.format("Bot fADC %d", ich), 33, -0.5, 32.5));

            plotters.get("Top FADC Spectra").region(ich).plot(fADC_Spectrum_Top.get(String.format("Top fADC %d", ich)));
            plotters.get("Bot FADC Spectra").region(ich).plot(fADC_Spectrum_Bot.get(String.format("Bot fADC %d", ich)));

            fADC_Spectrum_Top_lvl2.put(String.format("Top fADC %d", ich), histogramFactory.createHistogram1D(String.format("Top fADC %d", ich), 33, -0.5, 32.5));
            fADC_Spectrum_Bot_lvl2.put(String.format("Bot fADC %d", ich), histogramFactory.createHistogram1D(String.format("Bot fADC %d", ich), 33, -0.5, 32.5));

            plotters.get("Top FADC lvl2").region(ich).plot(fADC_Spectrum_Top_lvl2.get(String.format("Top fADC %d", ich)));
            plotters.get("Bot FADC lvl2").region(ich).plot(fADC_Spectrum_Bot_lvl2.get(String.format("Bot fADC %d", ich)));
        }

        for (HodoscopeChannel cur_ch : hodoConditions.getChannels()) {

            String histname = String.format("x=%d, y=%d, layer=%d, hole = %d", cur_ch.getIX(), cur_ch.getIY(), cur_ch.getLayer(), cur_ch.getHole());
            matchedHitEnergy.put(cur_ch.getName(), histogramFactory.createHistogram1D(histname, 200, -100, 3500.));

            int ind_x = 1 + (6 - 2 * cur_ch.getIX()) + Math.max(0, cur_ch.getHole());
            int ind_y = -1 * cur_ch.getIY() + 2 - cur_ch.getLayer();

            if (cur_ch.getIX() == 0) {
                ind_x = 7;
            } else if (cur_ch.getIX() == 4) {
                ind_x = 0;
            }

            int hist_coord = 4 * ind_x + ind_y;

            plotters.get("Matched Hit Energies").region(hist_coord).plot(matchedHitEnergy.get(cur_ch.getName()));
        }

        for (int ix = 0; ix < 5; ix++) { // 5 is the number of tiles in the given layer

            for (int ilayer = 0; ilayer < 2; ilayer++) {

                for (int iy = 0; iy < 2; iy++) {

                    //hitEnergyHistName(int ix, int iy, int layer, int hole) 
                    int y = -1;
                    if (iy > 0) {
                        y = 1;
                    }

                    String histNameEtile = this.tileEnergyHistName(ix, y, ilayer);
                    tileEnergy.put(histNameEtile, histogramFactory.createHistogram1D(histNameEtile, 100, -100, 3500.));
                    tileEnergy2.put(histNameEtile, histogramFactory.createHistogram1D(histNameEtile, 100, -100, 3500.));
                    tileEnergy3.put(histNameEtile, histogramFactory.createHistogram1D(histNameEtile, 100, -100, 3500.));

                    int ind_y = -1 * y + 2 - ilayer;
                    int ind_tile_x = 4 - ix;

                    int tile_hist_coord = 4 * ind_tile_x + ind_y;

                    plotters.get("Raw Tile Energies").region(tile_hist_coord).plot(tileEnergy.get(histNameEtile));
                    plotters.get("Tile Energies: Level1").region(tile_hist_coord).plot(tileEnergy2.get(histNameEtile));
                    plotters.get("Tile Energies: Level2").region(tile_hist_coord).plot(tileEnergy3.get(histNameEtile));

                    if (ix != 0 && ix != 4) {

                        String histname = this.hitEnergyHistName(ix, y, ilayer, -1);
                        hitEnergy.put(histname, histogramFactory.createHistogram1D(histname, 200, -100, 2000.));

                        System.out.println("== histname is " + histname);

                        int ind_x = 1 + (6 - 2 * ix);
                        int hist_coord = 4 * ind_x + ind_y;

                        plotters.get("Hit Energies").region(hist_coord).plot(hitEnergy.get(histname));

                        histname = this.hitEnergyHistName(ix, y, ilayer, 1);
                        hitEnergy.put(histname, histogramFactory.createHistogram1D(histname, 200, -100, 2000.));
                        System.out.println("== histname is " + histname);

                        ind_x = 1 + (6 - 2 * ix) + 1;
                        hist_coord = 4 * ind_x + ind_y;
                        plotters.get("Hit Energies").region(hist_coord).plot(hitEnergy.get(histname));

                    } else if (ix == 0 || ix == 4) {
                        String histname = this.hitEnergyHistName(ix, y, ilayer, 0);
                        hitEnergy.put(histname, histogramFactory.createHistogram1D(histname, 200, -100, 2000.));
                        System.out.println("== histname is " + histname);

                        int ind_x = 0; // if the ix = 4;
                        if (ix == 0) {
                            ind_x = 7;
                        }

                        int hist_coord = 4 * ind_x + ind_y;

                        plotters.get("Hit Energies").region(hist_coord).plot(hitEnergy.get(histname));
                    }

                }

            }
        }

        plotters.put("ECal plots", plotterFactory.create("ECal plots"));
        plotters.get("ECal plots").createRegions(2, 2);

        clust_polots.put("Ecal Energy VS X", histogramFactory.createHistogram2D("Cluster E vs X", 200, -300., 390., 200, 0.1, 5.));
        plotters.get("ECal plots").region(0).plot(clust_polots.get("Ecal Energy VS X"));

        plotters.put("Tile Occupancy", plotterFactory.create("Tile Occupancy"));
        plotters.get("Tile Occupancy").createRegions(2, 2); // One plot each for L1/L2/Top/Bottom

        plotters.put("PMT Occupancy", plotterFactory.create("PMT Occupancy"));
        plotters.get("PMT Occupancy").createRegions(2, 2);

        occupancyLayerPlots.put("L1 Top Occupancy",
                histogramFactory.createHistogram1D("L1 Top Occupancy", 6, 0, 5));
        occupancyLayerPlots.put("L2 Top Occupancy",
                histogramFactory.createHistogram1D("L2 Top Occupancy", 6, 0, 5));
        occupancyLayerPlots.put("L1 Bottom Occupancy",
                histogramFactory.createHistogram1D("L1 Bottom Occupancy", 6, 0, 5));
        occupancyLayerPlots.put("L2 Bottom Occupancy",
                histogramFactory.createHistogram1D("L2 Bottom Occupancy", 6, 0, 5));

        plotters.get("Tile Occupancy").region(0).plot(occupancyLayerPlots.get("L1 Top Occupancy"));
        plotters.get("Tile Occupancy").region(1).plot(occupancyLayerPlots.get("L2 Top Occupancy"));
        plotters.get("Tile Occupancy").region(2).plot(occupancyLayerPlots.get("L1 Bottom Occupancy"));
        plotters.get("Tile Occupancy").region(3).plot(occupancyLayerPlots.get("L2 Bottom Occupancy"));

        occupancyLayerMap.put("L1 Top", new int[5]);
        occupancyLayerMap.put("L2 Top", new int[5]);
        occupancyLayerMap.put("L1 Bottom", new int[5]);
        occupancyLayerMap.put("L2 Bottom", new int[5]);

        //similar things for other plots and plotters you want..
        //each plotter shows up as a tab in the online monitoring
        //with each histogram (or>1 histograms overlaid)  
        // in the assigned region
        for (IPlotter plotter : plotters.values()) {
            plotter.show();
        }
    }

    @Override
    public void process(EventHeader event) {

        // Get the run number from the event and store it. This will be used
        // when writing the plots out to a ROOT file
        if (runNumber == -1) {
            runNumber = event.getRunNumber();
        }

        if (resetPeriod > 0 && eventCount > resetPeriod) { // reset occupancy numbers after resetPeriod events
            eventCount = 0;
            resetPlots();
        }

        boolean has_cl = event.hasCollection(Cluster.class, ecalClusterName);

        List<Cluster> clusters = null;
        if (has_cl) {
            clusters = event.get(Cluster.class, ecalClusterName);
        }

        // Get all of the collections you need //
        ////e.g.
        if (!event.hasCollection(RawTrackerHit.class, rawCollectionName)) {
            return;
        }

        // =================== Hodo Hit 'Energies' =====================
        // Get all of the collections you need //
        ////e.g.
        if (!event.hasCollection(RawTrackerHit.class, rawCollectionName)) {
            return;
        }
        // Get RawTrackerHit collection from event.
        List<SimpleGenericObject> reconHits = event.get(SimpleGenericObject.class, hodoHitsCollectionName);

        //System.out.println("Size of reconHits is " + reconHits.size());
        int n_hits = reconHits.get(0).getNInt();

        // ======= Loop over hits, and fill corresponding histogram =======
        for (int ihit = 0; ihit < n_hits; ihit++) {
            int ix = reconHits.get(0).getIntVal(ihit);
            int iy = reconHits.get(1).getIntVal(ihit);
            int layer = reconHits.get(2).getIntVal(ihit);
            int hole = reconHits.get(3).getIntVal(ihit);
            double Energy = reconHits.get(4).getDoubleVal(ihit);
            double hit_time = reconHits.get(5).getDoubleVal(ihit);

            //if( iy < 0 ){Energy = 4.*Energy;}
            String histname = hitEnergyHistName(ix, iy, layer, hole);

            //System.out.println(hit_time);
            if (hit_time > utils.hodo_t_min && hit_time < utils.hodo_t_max) {
                hitEnergy.get(histname).fill(Energy);

                //int index = hitEnergy.get(histname).coordToIndex(Energy);
                //System.out.println("Energy = " + Energy + "     index = " + index);
                m_hit_Energies.put(hodoConditions.getChannels().findChannel(ix, iy, layer, hole).getName(), Energy);

                String tileHistName = this.tileEnergyHistName(ix, iy, layer);

                double EnergyTile = Energy;
                if (ix != 0 && ix != 4) {
                    EnergyTile = 1.25 * Energy / 2.;
                }

                //HodoUtils.HodoTileIdentifier tile_id = utils.new HodoTileIdentifier(ix, iy, layer);
                HodoUtils.HodoTileIdentifier tile_id = utils.new HodoTileIdentifier(ix, iy, layer);

                if (m_tile_Energies.get(tile_id) == null) {
                    m_tile_Energies.put(tile_id, EnergyTile);
                } else {
                    double Etmp = m_tile_Energies.get(tile_id);
                    m_tile_Energies.put(tile_id, EnergyTile + Etmp);
                }

            }
        }

        ArrayList<HodoUtils.HodoTileIdentifier> lvl2_hits = new ArrayList();

        for (Map.Entry<HodoUtils.HodoTileIdentifier, Double> cur_tile : m_tile_Energies.entrySet()) {

            HodoUtils.HodoTileIdentifier cur_tile_id = cur_tile.getKey();
            String tileHistName = tileEnergyHistName(cur_tile_id.ix, cur_tile_id.iy, cur_tile_id.ilayer);

            tileEnergy.get(tileHistName).fill(cur_tile.getValue());

            if (cur_tile_id.iy > 0) {
                if (cur_tile_id.ilayer == 0) {
                    occupancyLayerPlots.get("L1 Top Occupancy").fill(cur_tile_id.ix);
                } else {
                    occupancyLayerPlots.get("L2 Top Occupancy").fill(cur_tile_id.ix);
                }
            } else {
                if (cur_tile_id.ilayer == 0) {
                    occupancyLayerPlots.get("L1 Bottom Occupancy").fill(cur_tile_id.ix);
                } else {
                    occupancyLayerPlots.get("L2 Bottom Occupancy").fill(cur_tile_id.ix);
                }
            }

            if (utils.GoodTileHit(cur_tile_id, m_tile_Energies)) {
                tileEnergy2.get(tileHistName).fill(cur_tile.getValue());

                boolean hasRightCluster = false;
                if (has_cl) {

                    // =====  ====== ====== Now loop over clusters, if there is a matching cluster, then fill the tileE histogram
                    for (Cluster cur_clust : clusters) {

                        double cl_x = cur_clust.getPosition()[0];
                        double cl_E = cur_clust.getEnergy();

                        int cl_x_index = hECalCoordtoIndex.coordToIndex(cl_x) + 1;

                        //if (utils.hasRightClust(cur_tile_id, cl_x, cl_E)) {
                        if (utils.hasRightClust(cur_tile_id, cl_x_index, cl_E)) {
                            hasRightCluster = true;
                        }
                    }

                }

                if (hasRightCluster) {
                    tileEnergy3.get(tileHistName).fill(cur_tile.getValue());
                    lvl2_hits.add(cur_tile_id);
                }
            }

        }

        // Get RawTrackerHit collection from event.
        List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, rawCollectionName);

        ////
        // ========== fADC plots ===========
        for (RawTrackerHit cur_hit : rawHits) {

            short[] adc_Values = cur_hit.getADCValues();

            long cellID = cur_hit.getCellID();

            int PMT_ch = hodoConditions.getChannels().findGeometric(cellID).getChannel();
            int iy = hodoConditions.getChannels().findGeometric(cellID).getIY();

            HodoUtils.HodoTileIdentifier cur_tile_identifier = utils.new HodoTileIdentifier(hodoConditions.getChannels().findGeometric(cellID).getIX(), hodoConditions.getChannels().findGeometric(cellID).getIY(),
                    hodoConditions.getChannels().findGeometric(cellID).getLayer());

            boolean islvl2hit = lvl2_hits.contains(cur_tile_identifier);

            //hodoConditions
            //System.out.println("iY = " + iy + "    PMT_ch = " + PMT_ch);
            if (iy == 1) {
                for (int is = 0; is < adc_Values.length; is++) {
                    fADC_Spectrum_Top.get(String.format("Top fADC %d", PMT_ch)).fill(is, adc_Values[is]);
                    if (islvl2hit) {
                        fADC_Spectrum_Top_lvl2.get(String.format("Top fADC %d", PMT_ch)).fill(is, adc_Values[is]);
                    }
                }
            } else if (iy == -1) {
                for (int is = 0; is < adc_Values.length; is++) {
                    fADC_Spectrum_Bot.get(String.format("Bot fADC %d", PMT_ch)).fill(is, adc_Values[is]);
                    if (islvl2hit) {
                        fADC_Spectrum_Bot_lvl2.get(String.format("Bot fADC %d", PMT_ch)).fill(is, adc_Values[is]);
                    }
                }
            }

        }

        // Now Loop over hits is finished, let's look into some matched tile distrivutions
        double E1_Top = 0;

        double E7 = 0;
        double E3 = 0;
        double E2 = 0;
        double E8 = 0;
        double E12 = 0;
        double E13 = 0;
        double E10 = 0;
        double E11 = 0;
        double E4 = 0;
        double E5 = 0;

        if (m_hit_Energies.containsKey(hodoConditions.getChannels().findChannel(0, 1, 0, 0).getName())) {
            E7 = m_hit_Energies.get(hodoConditions.getChannels().findChannel(0, 1, 0, 0).getName());
        }
        if (m_hit_Energies.containsKey(hodoConditions.getChannels().findChannel(2, 1, 0, -1).getName())) {
            E2 = m_hit_Energies.get(hodoConditions.getChannels().findChannel(2, 1, 0, -1).getName());
        }
        if (m_hit_Energies.containsKey(hodoConditions.getChannels().findChannel(2, 1, 0, 1).getName())) {
            E3 = m_hit_Energies.get(hodoConditions.getChannels().findChannel(2, 1, 0, 1).getName());
        }

        if (m_hit_Energies.containsKey(hodoConditions.getChannels().findChannel(1, 1, 1, -1).getName())) {
            E11 = m_hit_Energies.get(hodoConditions.getChannels().findChannel(1, 1, 1, -1).getName());
        }
        if (m_hit_Energies.containsKey(hodoConditions.getChannels().findChannel(1, 1, 1, 1).getName())) {
            E10 = m_hit_Energies.get(hodoConditions.getChannels().findChannel(1, 1, 1, 1).getName());
        }

        if (m_hit_Energies.containsKey(hodoConditions.getChannels().findChannel(1, 1, 0, -1).getName())) {
            E4 = m_hit_Energies.get(hodoConditions.getChannels().findChannel(1, 1, 0, -1).getName());
        }

        if (m_hit_Energies.containsKey(hodoConditions.getChannels().findChannel(1, 1, 0, 1).getName())) {
            E5 = m_hit_Energies.get(hodoConditions.getChannels().findChannel(1, 1, 0, 1).getName());
        }

        if (E7 < 300. && E2 < 250. && E3 < 200 && E10 + E11 > 700.) {
            matchedHitEnergy.get(hodoConditions.getChannels().findChannel(1, 1, 0, -1).getName()).fill(E4 + E5);
        }

        // ==== Now check if there are clusters =====
        if (!event.hasCollection(Cluster.class, ecalClusterName)) {
            return;
        }

        int n_cl = clusters.size();

        for (Cluster cur_clust : clusters) {

            double cl_x = cur_clust.getPosition()[0];
            double cl_E = cur_clust.getEnergy();

            clust_polots.get("Ecal Energy VS X").fill(cl_x, cl_E);

        }

        m_hit_Energies.clear();
        m_tile_Energies.clear();

        lvl2_hits.clear();

        //  loop through the tile hits 
        /// get the layer/top/bottom and the tile number 
        // and increment the maps..
        //  for example
        //        occupancyLayerMap.get("L1 Top")[tilenumber]++;
        //update the occupancy histograms
        /*
            int[] tiles = occupancyMap.get("L1 Top Occupancy");
            occupancyPlots.get("L1 Top Occupancy").reset();
            for (int channel = 0; channel < 5; channel++) {
                    double tileOccupancy = (double) tiles[channel] / (double) eventCount;                   
                    occupancyLayerPlots.get().fill(channel, tileOccupancy);
        }
         */
 /*  to increment a non-occupancy histogram, 
        just do histo.fill(x-coordinate)...just like ROOT 
         */
    }

    private void resetPlots() {

        // Clear the hit counter map of all previously stored data.
        occupancyLayerMap.clear();
        //clear other occupancy maps here

    }

    @Override
    public void endOfData() {

        if (saveRootFile) {
            String rootFile = "run" + runNumber + "_occupancy.root";
            RootFileStore store = new RootFileStore(rootFile);
            try {
                store.open();
                store.add(tree);
                store.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String hitEnergyHistName(int ix, int y, int layer, int hole) {

        String histname = "UNDEFINED";
        if (ix != 0 && ix != 4) {
            histname = String.format(("E_hit: ix = %d, y=%d, L%d, hole %d"), ix, y, layer, hole);
        } else if (ix == 0 || ix == 4) {
            histname = String.format(("E_hit: ix = %d, y=%d, L%d"), ix, y, layer);
        }

        return histname;
    }

    private String tileEnergyHistName(int ix, int y, int layer) {

        String histname = String.format(("E_tile: ix = %d, y=%d, L%d"), ix, y, layer);

        return histname;
    }

}

class hodoID {

    public int ix;
    public int iy;
    public int ilayer;
    public int ihole;
};
