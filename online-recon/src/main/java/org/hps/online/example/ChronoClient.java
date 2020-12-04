package org.hps.online.example;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.SwingUtilities;

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
import hep.aida.ref.plotter.Plotter;
import hep.aida.ref.remote.rmi.client.RmiStoreFactory;

/**
 * Displays remote chrono plots from <code>RemoteChronoDriver</code>
 */
public class ChronoClient {

    static {
        System.setProperty("hep.aida.IAnalysisFactory", AnalysisFactory.class.getName());
    }

    private String host = null;
    private Integer port = 2001;
    private String serverName = "RmiAidaServer";

    private ITree clientTree = null;
    private IAnalysisFactory af = IAnalysisFactory.create();
    private IPlotterFactory pf = af.createPlotterFactory();
    private ITreeFactory tf = af.createTreeFactory();
    private IPlotter plotter = pf.create("Chrono Plots");
    private Map<IDataPointSet, IPlotterRegion> plots =
            new HashMap<IDataPointSet, IPlotterRegion>();
    private String path = "/chrono";

    private Options options = new Options();

    private int NBEFORE = 10;
    private int NAFTER = 10;

    private boolean windowClosed = false;

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
        style.regionBoxStyle().setVisible(true);
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
        //System.out.println("host: " + host);
        //System.out.println("port: " + port);
        //System.out.println("server: " + serverName);
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
        double before = sec - NBEFORE;
        double after = sec + NAFTER;
        AxisStyle xStyle = (AxisStyle) region.style().xAxisStyle();
        xStyle.setLowerLimit(Double.toString(before).toString());
        xStyle.setUpperLimit(Double.toString(after).toString());
        region.refresh();
    }

    private void show() {
        plotter.show();

        SwingUtilities.getWindowAncestor(((Plotter) plotter).panel()).addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                ChronoClient.this.windowClosed = true;
            }
        });
    }

    private void createRegions(int rows, int cols) {
        plotter.createRegions(rows, cols);
    }

    private boolean isWindowClosed() {
        return windowClosed;
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
                if (isWindowClosed()) {
                    break;
                }
            }
        } finally {
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
