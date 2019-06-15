package org.hps.monitoring.drivers.hodoscope;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.ITree;
import hep.aida.ref.rootwriter.RootFileStore;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author mgraham
 */
public class HodoscopePlots extends Driver {

    // Plotting
    private static ITree tree = null;
    private IAnalysisFactory analysisFactory = AIDA.defaultInstance().analysisFactory();
    private IPlotterFactory plotterFactory = analysisFactory.createPlotterFactory("SVT Occupancy");
    private IHistogramFactory histogramFactory = null;

    private static Map<String, IPlotter> plotters = new HashMap<String, IPlotter>();

    private static Map<String, IHistogram1D> occupancyLayerPlots = new HashMap<String, IHistogram1D>();
    private static Map<String, int[]> occupancyLayerMap = new HashMap<String, int[]>();

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

    private int eventCount = 0;
    private int eventRefreshRate = 1;
    private int runNumber = -1;
    private int resetPeriod = -1;

    private boolean saveRootFile = true;

    @Override
    protected void detectorChanged(Detector detector) {
        tree = AIDA.defaultInstance().tree();
        tree.cd("/");// aida.tree().cd("/");
        histogramFactory = analysisFactory.createHistogramFactory(tree);
        // Histogram maps

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
        
        
        for (IPlotter plotter : plotters.values())
            plotter.show();
    }

    @Override
    public void process(EventHeader event) {

        // Get the run number from the event and store it. This will be used
        // when writing the plots out to a ROOT file
        if (runNumber == -1)
            runNumber = event.getRunNumber();

        if (resetPeriod > 0 && eventCount > resetPeriod) { // reset occupancy numbers after resetPeriod events
            eventCount = 0;
            resetPlots();
        }
        
        // Get all of the collections you need //
        ////e.g.
        /*
         if (!event.hasCollection(RawTrackerHit.class, rawTrackerHitCollectionName))
            return;
        // Get RawTrackerHit collection from event.
        List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, rawTrackerHitCollectionName);

        */
        ////
        

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
}
