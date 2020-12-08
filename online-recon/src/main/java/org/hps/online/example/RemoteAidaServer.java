package org.hps.online.example;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Random;

import hep.aida.IAnalysisFactory;
import hep.aida.IDataPointSet;
import hep.aida.IDataPointSetFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogramFactory;
import hep.aida.ITreeFactory;
import hep.aida.dev.IDevTree;
import hep.aida.ref.remote.RemoteServer;
import hep.aida.ref.remote.rmi.client.RmiStoreFactory;
import hep.aida.ref.remote.rmi.interfaces.RmiServer;
import hep.aida.ref.remote.rmi.server.RmiServerImpl;

public class RemoteAidaServer {

    IAnalysisFactory af = IAnalysisFactory.create();
    ITreeFactory tf = af.createTreeFactory();
    IDevTree serverTree = (IDevTree) tf.create();
    IHistogramFactory hf = af.createHistogramFactory(serverTree);

    public static void main(String[] args) {
        RemoteAidaServer server = new RemoteAidaServer();
        server.runIt();
    }

    public void runIt() {

        final RmiStoreFactory rsf = new RmiStoreFactory();

        String localHost = null;
        try {
            localHost = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            e.printStackTrace();
        }
        assert(localHost != null);

        int port = 2001;
        String serverName = "RmiAidaServer";

        String dpsPath = "/dps";
        serverTree.mkdir(dpsPath);
        serverTree.cd(dpsPath);
        IDataPointSetFactory dpsf = af.createDataPointSetFactory(serverTree);
        IDataPointSet dps2D = dpsf.create("dps2D", "two dimensional IDataPointSet", 2);

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
            fill(dps2D);
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

    private void fill(IHistogram1D h1) {
        int nEntries = 10000;
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
    }

    public void fill(IDataPointSet dps2D) {
        int entries = 9999;
        long waitTime = 1000;
        Random r = new Random();
        for (int i = 0; i < entries; i++) {
            long curr = System.currentTimeMillis() / 1000;
            double val = r.nextGaussian();
            System.out.println("Adding point: " + curr + " = " + val);
            dps2D.addPoint();
            dps2D.point(i).coordinate(0).setValue(curr);
            //dps2D.point(i).coordinate(0).setErrorPlus(0);
            dps2D.point(i).coordinate(1).setValue(r.nextGaussian());
            //dps2D.point(i).coordinate(1).setErrorPlus(0);
            //dps2D.point(i).coordinate(1).setErrorMinus(0);
            // Simulate a one second update interval
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /*
    public static void main(String[] args) {

        RmiStoreFactory rsf = new RmiStoreFactory();

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
                //if (i % 10 == 0) {
                //    System.out.println("Filling entry: " + i);
                //}
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
    */
}