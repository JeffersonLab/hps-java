package org.hps.online.example;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

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

public class ChronoClient {

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
    IPlotter plotter = pf.create("Chrono Plots");
    Map<IDataPointSet, IPlotterRegion> plots =
            new HashMap<IDataPointSet, IPlotterRegion>();
    String path = "/chrono";

    Options options = new Options();

    public ChronoClient() {
        options.addOption(new Option("h", "help",   false, "Print help and exit"));
        options.addOption(new Option("p", "port",   true,  "Network port of server"));
        options.addOption(new Option("s", "server", true,  "Name of RMI server"));
        options.addOption(new Option("H", "host",   true,  "Host name or IP address of server"));

        IPlotterStyle style = plotter.style();
        style.xAxisStyle().setParameter("type", "date");
        style.dataStyle().fillStyle().setColor("black");
        style.dataStyle().markerStyle().setColor("black");
        style.dataStyle().errorBarStyle().setVisible(false);
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

    private void addDataPointSet(String name, String yAxisLabel, int regionInd) {
        IDataPointSet dps = (IDataPointSet) clientTree.find(path + "/" + name);
        IPlotterRegion region = plotter.region(regionInd);
        IPlotterStyle regStyle = pf.createPlotterStyle();
        regStyle.yAxisStyle().setLabel(yAxisLabel);
        plotter.region(regionInd).setStyle(regStyle);
        region.plot(dps);
        plots.put(dps, region);
    }

    private void update() {
        for (Map.Entry<IDataPointSet, IPlotterRegion> entry : plots.entrySet()) {
            update(entry.getKey(), entry.getValue());
        }
    }

    private void update(IDataPointSet dps, IPlotterRegion region) {
        IDataPoint dp = dps.point(dps.size() - 1);
        double sec = dp.coordinate(0).value();
        double before = sec - 10;
        double after = sec + 10;
        AxisStyle xStyle = (AxisStyle) region.style().xAxisStyle();
        xStyle.setLowerLimit(Double.toString(before).toString());
        xStyle.setUpperLimit(Double.toString(after).toString());
        region.refresh();
    }

    private void show() {
        plotter.show();
    }

    private void createRegions(int rows, int cols) {
        plotter.createRegions(rows, cols);
    }

    private void run() {
        try {
            connect();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        createRegions(2, 2);
        addDataPointSet("Events Per Second", "Hz",     0);
        addDataPointSet("Events",            "Events", 1);
        addDataPointSet("Millis Per Event",  "Millis", 2);
        addDataPointSet("Avg Per Event",     "Millis", 3);
        show();
        try {
            while(true) {
                update();
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } finally {
            // FIXME: Never executed if Ctrl+C is used to exit
            try {
                close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        final ChronoClient client = new ChronoClient();
        client.parse(args);
        client.run();
    }
}
