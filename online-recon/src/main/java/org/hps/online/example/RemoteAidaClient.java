package org.hps.online.example;

import java.net.InetAddress;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterRegion;
import hep.aida.ITree;
import hep.aida.ITreeFactory;
import hep.aida.ref.remote.rmi.client.RmiStoreFactory;
import hep.aida.ref.remote.rmi.client.RmiClientImpl_Stub;

public class RemoteAidaClient {

    public static void main(String[] args) {

        RmiStoreFactory rsf = new RmiStoreFactory();

        try {
            new RmiClientImpl_Stub(null);
        } catch (Exception e) {
        }

        ITree clientTree;
        IAnalysisFactory af = IAnalysisFactory.create();
        IPlotterFactory pf = af.createPlotterFactory();
        ITreeFactory tf = af.createTreeFactory();

        String localHost = null;
        try {
            localHost = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        int port = 2001;
        String serverName = "RmiAidaServer";
        String treeBindName = "//"+localHost+":"+port+"/"+serverName;

        String hist1DTitle = "Aida 1D Histogram";
        String histPath = "/hists";

        // Create Rmi Client Tree
        boolean clientDuplex = true;
        boolean hurry = false;
        String options = "duplex=\""+clientDuplex+"\",RmiServerName=\"rmi:"+treeBindName+"\",hurry=\""+hurry+"\"";
        System.out.println("options: " + options);
        try {
            clientTree = tf.create(localHost, RmiStoreFactory.storeType, true, false, options);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Retrieve AIDA instances using the RMI
        IHistogram1D rh1 = null;
        try {
            System.out.println("Finding remote histogram: " + histPath + "/" + hist1DTitle);
            rh1 = (IHistogram1D) clientTree.find(histPath + "/" + hist1DTitle);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        IPlotter plotter = pf.create("plots");
        IPlotterRegion region = plotter.createRegion();
        region.plot(rh1);
        plotter.show();
        /*
        while(true) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Remote H1D has n entries: " + rh1.entries());

        }
        */

        /*
        try {
            clientTree.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        */
    }
}
