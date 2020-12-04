package org.hps.online.example;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Parser;

import hep.aida.IAnalysisFactory;
import hep.aida.IDataPoint;
import hep.aida.IDataPointSet;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterRegion;
import hep.aida.IPlotterStyle;
import hep.aida.ITree;
import hep.aida.ITreeFactory;
import hep.aida.ref.AnalysisFactory;
import hep.aida.ref.plotter.AxisStyle;
import hep.aida.ref.remote.rmi.client.RmiStoreFactory;

public class RemoteAidaClient {

    static {
        System.setProperty("hep.aida.IAnalysisFactory", AnalysisFactory.class.getName());
    }

    String host = null;
    Integer port = 2001;
    String serverName = "RmiAidaServer";

    ITree clientTree = null;
    IAnalysisFactory af = IAnalysisFactory.create();
    IPlotterFactory pf = af.createPlotterFactory();
    ITreeFactory tf = af.createTreeFactory();

    Options options = new Options();

    // FIXME: is this needed???
    //static {
    //    final RmiStoreFactory rsf = new RmiStoreFactory();
    //}

    public RemoteAidaClient() {
        options.addOption(new Option("h", "help",   false, "Print help and exit"));
        options.addOption(new Option("p", "port",   true,  "Network port of server"));
        options.addOption(new Option("s", "server", true,  "Name of RMI server"));
        options.addOption(new Option("H", "host",   true,  "Host name or IP address of server"));
    }

    private void parse(String[] args) {
        Parser parser = new BasicParser();
        CommandLine cl = null;
        try {
            cl = parser.parse(options, args);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        if (cl.hasOption("h")) {
            HelpFormatter help = new HelpFormatter();
            help.printHelp("r", "", options, "", true);
            System.exit(0);
        }

        if (cl.hasOption("H")) {
            host = cl.getOptionValue("H");
        } else {
            try {
                host = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
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

    private void connect() throws IOException {
        boolean clientDuplex = true;
        boolean hurry = false;
        String treeBindName = "//"+host+":"+port+"/"+serverName;
        String options = "duplex=\""+clientDuplex+"\",RmiServerName=\"rmi:"+treeBindName+"\",hurry=\""+hurry+"\"";
        clientTree = tf.create(host, RmiStoreFactory.storeType, true, false, options);
    }

    private void close() throws IOException {
        clientTree.close();
    }

    private void plotDataPointSet(String objectPath) {
        IDataPointSet dps2D = (IDataPointSet) clientTree.find(objectPath);
        IPlotter plotter = pf.create("Chrono Plots");
        IPlotterStyle dateAxisStyle = pf.createPlotterStyle();
        dateAxisStyle.xAxisStyle().setParameter("type", "date");
        IPlotterRegion region = plotter.createRegion();
        IPlotterStyle plotStyle = pf.createPlotterStyle();
        plotStyle.dataStyle().fillStyle().setColor("black");
        plotStyle.dataStyle().markerStyle().setColor("black");
        plotStyle.dataStyle().errorBarStyle().setVisible(false);
        plotter.setStyle(plotStyle);
        region.plot(dps2D, dateAxisStyle);
        plotter.show();
        while(true) {
            update(dps2D, region);
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void update(IDataPointSet dps2D, IPlotterRegion region) {
        IDataPoint dp = dps2D.point(dps2D.size() - 1);
        double sec = dp.coordinate(0).value();
        double before = sec - 10;
        double after = sec + 10;
        AxisStyle xStyle = (AxisStyle) region.style().xAxisStyle();
        xStyle.setLowerLimit(Double.toString(before).toString());
        xStyle.setUpperLimit(Double.toString(after).toString());
        region.refresh();
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
            client.plotDataPointSet("/chrono/Events Per Second");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
