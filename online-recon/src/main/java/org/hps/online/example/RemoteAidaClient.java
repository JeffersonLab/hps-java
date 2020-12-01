package org.hps.online.example;

import java.io.IOException;
import java.net.InetAddress;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Parser;

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

    String host = null;
    Integer port = 2001;

    String serverName = "RmiAidaServer";
    String treeBindName = null;

    String hist1DTitle = "Aida 1D Histogram";
    String histPath = "/hists";

    ITree clientTree = null;
    IAnalysisFactory af = IAnalysisFactory.create();
    IPlotterFactory pf = af.createPlotterFactory();
    ITreeFactory tf = af.createTreeFactory();

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

    void parse(String[] args) throws ParseException {
        Options options = new Options();
        options.addOption(new Option("h", "help", false, "Print help and exit"));
        options.addOption(new Option("p", "port", true, "Network port of server"));
        options.addOption(new Option("s", "server", true, "Name of RMI server"));
        options.addOption(new Option("H", "host", true, "Host name or IP address of server"));
        Parser parser = new BasicParser();
        CommandLine cl = parser.parse(options, args);
        if (cl.hasOption("H")) {
            host = cl.getOptionValue("H");
        }
        if (cl.hasOption("p")) {
            port = Integer.parseInt(cl.getOptionValue("p"));
        }
        if (cl.hasOption("s")) {
            serverName = cl.getOptionValue("s");
        }
        System.out.println("host: " + host);
        System.out.println("port: " + port);
        System.out.println("server: " + serverName);
    }

    void connect() throws IOException {

        if (host == null) {
            host = InetAddress.getLocalHost().getHostName();
        }

        treeBindName = "//"+host+":"+port+"/"+serverName;

        // Create Rmi Client Tree
        boolean clientDuplex = true;
        boolean hurry = false;
        String options = "duplex=\""+clientDuplex+"\",RmiServerName=\"rmi:"+treeBindName+"\",hurry=\""+hurry+"\"";
        System.out.println("Options: " + options);
        System.out.println("Connecting to remote tree: " + treeBindName);
        clientTree = tf.create(host, RmiStoreFactory.storeType, true, false, options);
    }

    void close() throws IOException {
        clientTree.close();
    }

    void plot() {
        System.out.println("Finding remote histogram: " + histPath + "/" + hist1DTitle);
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
        RemoteAidaClient client = null;
        try {
            System.out.println("Creating client...");
            client = new RemoteAidaClient();
            System.out.println("Parsing CL args...");
            client.parse(args);
            System.out.println("Connecting to server...");
            client.connect();
            System.out.println("Showing plot...");
            client.plot();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("Closing connection...");
            try {
                if (client != null) {
                    client.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Bye!");
    }
}
