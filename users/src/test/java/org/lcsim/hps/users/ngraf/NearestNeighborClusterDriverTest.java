package org.lcsim.hps.users.ngraf;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.hps.users.ngraf.NearestNeighborClusterDriver;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;

import hep.aida.IAnalysisFactory;
import hep.aida.IPlotter;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 *
 * @author ngraf
 */
public class NearestNeighborClusterDriverTest extends TestCase
{

    static final String testURLBase = "http://www.slac.stanford.edu/~ngraf/hps_data/";
    static final String testFileName = "outfile3.slcio";
    private final int nEvents = 500;

    public void testClustering() throws Exception
    {
        File lcioInputFile = null;
        URL testURL = new URL(testURLBase + "/" + testFileName);
        FileCache cache = new FileCache();
        lcioInputFile = cache.getCachedFile(testURL);
        //Process and write out the file
        LCSimLoop loop = new LCSimLoop();
        loop.setLCIORecordSource(lcioInputFile);
        loop.add(new SimpleConverterDriver());
        NearestNeighborClusterDriver clusterer = new NearestNeighborClusterDriver();
        clusterer.setEcalCollectionName("CalorimeterHits");
        clusterer.setClusterCollectionName("NearestNeighborEcalClusters");
        loop.add(clusterer);
        loop.add(new SimpleAnalysis());
        loop.loop(nEvents, null);
        loop.dispose();
    }

    class SimpleConverterDriver extends Driver
    {

        @Override
        protected void process(EventHeader event)
        {
            if (event.hasCollection(SimCalorimeterHit.class, "EcalHits")) {
                // Get the list of sim ECal hits.
                List<SimCalorimeterHit> simhits = event.get(SimCalorimeterHit.class, "EcalHits");

                // create a list of CalorimeterHits
                List<CalorimeterHit> calHits = new ArrayList<CalorimeterHit>();
                for (SimCalorimeterHit hit : simhits) {
                    calHits.add(hit);
                }
                event.put("CalorimeterHits", calHits);
            }
        }
    }

    class SimpleAnalysis extends Driver
    {

        // Histogram manager
        private AIDA aida = AIDA.defaultInstance();
        // Set the following to true in order to see the plots
        private boolean debug = false;
        
        @Override
        protected void process(EventHeader event)
        {
            List<Cluster> clusters = event.get(Cluster.class, "NearestNeighborEcalClusters");
            if(debug) System.out.println("found " + clusters.size() + " clusters");
            double eTop = 0.;
            double eBottom = 0.;
            for (Cluster clus : clusters) {
                if(debug) System.out.println("x: " + clus.getPosition()[0] + " y: " + clus.getPosition()[1] + " iPhi: " + clus.getIPhi() + " iTheta " + clus.getITheta());
                if (clus.getPosition()[1] > 0) {
                    eTop += clus.getEnergy();
                } else {
                    eBottom += clus.getEnergy();
                }
            }
            //fill the histograms
            aida.cloud1D("Top energy sum").fill(eTop);
            aida.cloud1D("Bottom energy sum").fill(eBottom);
            aida.cloud1D("energy diff").fill(eTop - eBottom);
        }

        @Override
        protected void endOfData()
        {
            System.out.println("end of data");
            if (debug) {
                try{
                //BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
                IAnalysisFactory af = IAnalysisFactory.create();
                IPlotter plotter = af.createPlotterFactory().create("Nearest Neighbor Clustering Analysis");
                plotter.createRegions(1, 2, 0);
                plotter.region(0).plot(aida.cloud1D("Top energy sum"));
                plotter.region(1).plot(aida.cloud1D("Bottom energy sum"));    
                plotter.show();
                //stdin.readLine();
                Thread.sleep(2000);
                }
                catch(Exception e)
                {                 
                }
            }
        }
    }
}
