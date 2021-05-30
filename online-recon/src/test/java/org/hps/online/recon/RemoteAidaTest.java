package org.hps.online.recon;

import java.net.InetAddress;
import java.util.Random;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogramFactory;
import hep.aida.ITree;
import hep.aida.ITreeFactory;
import hep.aida.dev.IDevTree;
import hep.aida.ref.remote.RemoteServer;
import hep.aida.ref.remote.rmi.client.RmiStoreFactory;
import hep.aida.ref.remote.rmi.interfaces.RmiServer;
import hep.aida.ref.remote.rmi.server.RmiServerImpl;
import junit.framework.TestCase;

// Based on TestRHistogram in freehep-jaida-remote
public class RemoteAidaTest extends TestCase {

    private IDevTree serverTree;
    private ITree clientTree;
    private RemoteServer treeServer;
    private RmiServer rmiTreeServer;
    private String localHost;
    private int port;
    private String serverName;

    private int xbins = 40;
    private int nEntries = 1234;
    private double xLowerEdge = -2.3;
    private double xUpperEdge = 4.2;

    private String hist1DTitle = "Aida 1D Histogram";

    private String histPath = "/hists";

    protected void setUp() throws Exception {
        super.setUp();

        // Set host name, port, and server name
        localHost = null;
        try {
            localHost = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertTrue(localHost != null);

        port = 2001;
        serverName = "RmiAidaServer";
    }

    // is run by JUnit framework after the test
    protected void tearDown() throws Exception {
        super.tearDown();

        // disconnect client
        clientTree.close();

        // disconnect and shut down server
        ((RmiServerImpl) rmiTreeServer).disconnect();
        rmiTreeServer = null;

        treeServer.close();
        treeServer = null;

        serverTree.close();
        serverTree = null;
    }

    public void testRemoteAida() {

        // create AIDA factories and AIDA Tree
        IAnalysisFactory af = IAnalysisFactory.create();
        ITreeFactory tf = af.createTreeFactory();
        serverTree = (IDevTree) tf.create();
        IHistogramFactory hf = af.createHistogramFactory(serverTree);

        serverTree.mkdir(histPath);

        serverTree.cd(histPath);
        IHistogram1D h1 = hf.createHistogram1D(hist1DTitle, xbins, xLowerEdge, xUpperEdge);

        Random r = new Random();
        for (int i = 0; i < 10 * nEntries; i++) {
            double xVal = r.nextGaussian();
            double w = r.nextDouble();
            h1.fill(xVal, w);
        }

        ////////////////////////////////
        // Now create RMI Server
        ////////////////////////////////

        // RMI bind name for server
        String treeBindName = "//"+localHost+":"+port+"/"+serverName;
        System.out.println("server tree: " + treeBindName);
        try {
            // General server that uses Remote AIDA interfaces (hep.aida.ref.remote.interfaces)
            boolean serverDuplex = true;
            treeServer = new RemoteServer(serverTree, serverDuplex);

            // Transport-layer RMI server that talks Remote AIDA to treeServer and RMI to the client
            rmiTreeServer = new RmiServerImpl(treeServer, treeBindName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertTrue(treeServer != null);
        assertTrue(rmiTreeServer != null);

        ////////////////////////////////
        // Now create RMI Client
        // Use RMIStoreFactory for that
        ////////////////////////////////

        // Create Rmi Client Tree
        boolean clientDuplex = true;
        boolean hurry = false;
        String options = "duplex=\""+clientDuplex+"\",RmiServerName=\"rmi:"+treeBindName+"\",hurry=\""+hurry+"\"";
        System.out.println("options: " + options);
        try {
            clientTree = tf.create(localHost, RmiStoreFactory.storeType, true, false, options);
        } catch (Exception e) {
            e.printStackTrace();
        }

        assertTrue(clientTree != null);

        // Retrieve AIDA instances using the RMI
        IHistogram1D rh1 = null;
        try {
            System.out.println("Finding remote histogram: " + histPath + "/" + hist1DTitle);
            rh1 = (IHistogram1D) clientTree.find(histPath + "/" + hist1DTitle);
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertTrue(rh1 != null);

        System.out.println("Remote H1D has n entries: " + rh1.entries());
        System.out.println("Local H1D has n entries: " + h1.entries());
        assertEquals("Number of entries in local and remote histograms do not match.", rh1.entries(), h1.entries());


    }
}
