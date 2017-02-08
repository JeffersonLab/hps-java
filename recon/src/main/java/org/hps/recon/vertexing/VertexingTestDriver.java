package org.hps.recon.vertexing;

import org.lcsim.util.Driver;

public class VertexingTestDriver extends Driver {

        public VertexingTestDriver() {

            //  Set the diagnostics flag true if you want to run with the diagnostic package turned on
            boolean diagnostics = false;

        //  Instantiate the main tracking driver
//        MainTrackingDriver trackingdriver = new MainTrackingDriver();

        //  Turn on the optional diagnostics if requested
//        if (diagnostics) {
//            ISeedTrackerDiagnostics diag = new SeedTrackerDiagnostics();
//            trackingdriver.getSeedTracker().setDiagnostics(diag);
//        }

        //  Instantiate the analysis driver
        AnalysisDriver analysisdriver = new AnalysisDriver();

        //  Get the list of strategies to be used and pass them to the tracking and analysis drivers
//        String sfile = "autogen_ttbar_sidloi3.xml";
//        List<SeedStrategy> slist = StrategyXMLUtils.getStrategyListFromResource(
//                StrategyXMLUtils.getDefaultStrategiesPrefix() + sfile);
//        trackingdriver.getSeedTracker().putStrategyList(slist);
//        analysisdriver.setStrategies(slist);

        //  Add the tracking and analysis drivers
//        add(trackingdriver);
            add(analysisdriver);
    }


}
