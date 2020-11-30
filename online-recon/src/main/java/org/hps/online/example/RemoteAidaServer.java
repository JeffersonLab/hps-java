package org.hps.online.example;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Random;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogramFactory;
import hep.aida.ITreeFactory;
import hep.aida.dev.IDevTree;
import hep.aida.ref.remote.RemoteServer;
import hep.aida.ref.remote.rmi.interfaces.RmiServer;
import hep.aida.ref.remote.rmi.server.RmiServerImpl;

public class RemoteAidaServer {

    public static void main(String[] args) {
        String localHost = null;
        try {
            localHost = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            e.printStackTrace();
        }
        assert(localHost != null);

        int port = 2001;
        String serverName = "RmiAidaServer";

        String histPath = "/hists";
        String hist1DTitle = "Aida 1D Histogram";
        int xbins = 40;
        int nEntries = 10000;
        double xLowerEdge = -2.3;
        double xUpperEdge = 4.2;

        IAnalysisFactory af = IAnalysisFactory.create();
        ITreeFactory tf = af.createTreeFactory();
        IDevTree serverTree = (IDevTree) tf.create();
        IHistogramFactory hf = af.createHistogramFactory(serverTree);

        serverTree.mkdir(histPath);
        serverTree.cd(histPath);
        IHistogram1D h1 = hf.createHistogram1D(hist1DTitle, xbins, xLowerEdge, xUpperEdge);

        RemoteServer treeServer;
        RmiServer rmiTreeServer;
        String treeBindName = "//"+localHost+":"+port+"/"+serverName;
        System.out.println("server tree: " + treeBindName);
        try {
            boolean serverDuplex = true;
            treeServer = new RemoteServer(serverTree, serverDuplex);
            rmiTreeServer = new RmiServerImpl(treeServer, treeBindName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            Random r = new Random();
            for (int i = 0; i < 10 * nEntries; i++) {
                if (i % 10 == 0) {
                    System.out.println("Filling entry: " + i);
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                double xVal = r.nextGaussian();
                double w = r.nextDouble();
                h1.fill(xVal, w);
            }
        } finally {
            ((RmiServerImpl) rmiTreeServer).disconnect();
            treeServer.close();
            try {
                serverTree.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}