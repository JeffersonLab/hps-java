package org.hps.online.example;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterRegion;
import hep.aida.ITree;
import hep.aida.ITreeFactory;
import hep.aida.ref.remote.rmi.client.RmiClientImpl_Stub;
import hep.aida.ref.remote.rmi.client.RmiStoreFactory;

public class RemoteAidaClient {

    String hist1DTitle = "Aida 1D Histogram";
    String histPath = "/hists";

    ITree clientTree = null;
    IAnalysisFactory af = IAnalysisFactory.create();
    IPlotterFactory pf = af.createPlotterFactory();
    ITreeFactory tf = af.createTreeFactory();

    RemoteAidaOptions params = new RemoteAidaOptions("RemoteAidaClient");

    // Instantiating these classes avoids some mysterious exceptions
    static RmiStoreFactory rsf = null;
    static {
        rsf = new RmiStoreFactory();
        try {
            new RmiClientImpl_Stub(null);
        } catch (Exception e) {
        }
    }

    public RemoteAidaClient() {
    }

    void parse(String[] args) {
        CommandLine cl = params.parse(args);

        // TODO: Get name of object to plot from extra args
    }

    void connect() throws IOException {
        // Create Rmi Client Tree
        boolean clientDuplex = true;
        boolean hurry = false;
        String treeBindName = params.getTreeBindName();
        String options = "duplex=\""+clientDuplex+"\",RmiServerName=\"rmi:"+treeBindName+"\",hurry=\""+hurry+"\"";
        clientTree = tf.create(params.host, RmiStoreFactory.storeType, true, false, options);

        //String[] objectNames = clientTree.listObjectNames();
        //for (String objectName : objectNames) {
        //    System.out.println("remote object: " + objectName);
        //}
    }

    void close() throws IOException {
        clientTree.close();
    }

    void plot() {
        //System.out.println("Finding remote histogram: " + histPath + "/" + hist1DTitle);
        IHistogram1D rh1 = (IHistogram1D) clientTree.find(histPath + "/" + hist1DTitle);
        IPlotter plotter = pf.create("plots");
        IPlotterRegion region = plotter.createRegion();
        region.plot(rh1);
        plotter.show();
        Boolean object = true;
        synchronized(object) {
            try {
                object.wait(0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        final RemoteAidaClient client = new RemoteAidaClient();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                if (client != null) {
                    try {
                        client.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        try {
            client.parse(args);
            client.connect();
            client.plot();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
