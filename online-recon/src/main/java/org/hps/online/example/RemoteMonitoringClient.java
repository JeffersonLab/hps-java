package org.hps.online.example;

import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JFrame;
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
import hep.aida.ITextStyle;
import hep.aida.ITree;
import hep.aida.ITreeFactory;
import hep.aida.ref.AnalysisFactory;
import hep.aida.ref.plotter.AxisStyle;
import hep.aida.ref.plotter.Plotter;
import hep.aida.ref.remote.rmi.client.RmiStoreFactory;

/**
 * Displays plots from the <code>RemoteChronoDriver</code>
 * through an RMI connection
 */
public class RemoteMonitoringClient {

    private static final int COLS = 4;

    static {
        System.setProperty("hep.aida.IAnalysisFactory", AnalysisFactory.class.getName());
    }

    private Options options = new Options();

    private String host = null;
    private Integer port = 2001;
    private String serverName = "RmiAidaServer";

    private ITree clientTree = null;
    private IAnalysisFactory af = IAnalysisFactory.create();
    private IPlotterFactory pf = af.createPlotterFactory();
    private ITreeFactory tf = af.createTreeFactory();

    private IPlotter plotter = pf.create("Monitoring Plots");

    private Map<IDataPointSet, IPlotterRegion> plots =
            new HashMap<IDataPointSet, IPlotterRegion>();

    private int NBEFORE = 10;
    private int NAFTER = 10;

    private boolean windowClosed = false;

    public RemoteMonitoringClient() {
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
        ITextStyle titStyle = pf.createTextStyle();
        titStyle.setBold(true);
        titStyle.setFontSize(14);
        style.titleStyle().setTextStyle(titStyle);
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
    }

    private void connect() throws IOException {
        boolean clientDuplex = true;
        boolean hurry = false;
        String treeBindName = "//"+host+":"+port+"/"+serverName;
        System.out.println("Connecting to RMI server: " + treeBindName);
        String options = "duplex=\""+clientDuplex+"\",RmiServerName=\"rmi:"+treeBindName+"\",hurry=\""+hurry+"\"";
        clientTree = tf.create(host, RmiStoreFactory.storeType, true, false, options);
        System.out.println("Successfully connected!");
    }

    private void close() throws IOException {
        clientTree.close();
    }

    private void addPlots() {
        String[] names = clientTree.listObjectNames("/", true);
        String[] types = clientTree.listObjectTypes("/", true);
        List<IDataPointSet> plots = new ArrayList<IDataPointSet>();
        for (int i = 0; i < names.length; i++) {
            if (types[i] == "IDataPointSet") {
                plots.add((IDataPointSet) clientTree.find(names[i]));
            }
        }
        int n = plots.size();
        int rowsCols = (int) Math.ceil(Math.sqrt(n));
        plotter.createRegions(rowsCols, rowsCols);
        System.out.println("Created regions: " + plotter.numberOfRegions());
        System.out.println("Rows & col size: " + rowsCols);
        int regionInd = 0;
        plotter.setCurrentRegionNumber(regionInd);
        for (IDataPointSet plot : plots) {
            System.out.println("Adding plot: " + plot.title());
            String label = "";
            if (plot.annotation().hasKey("label")) {
                label = plot.annotation().value("label");
            }
            IPlotterRegion region = plotter.region(regionInd);
            IPlotterStyle regStyle = pf.createPlotterStyle();
            regStyle.yAxisStyle().setLabel(label);
            region.setStyle(regStyle);
            region.plot(plot);
            this.plots.put(plot, region);
            ++regionInd;
        }
    }

    private void update() {
        for (Entry<IDataPointSet, IPlotterRegion> entry : this.plots.entrySet()) {
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
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(((Plotter) plotter).panel());
        frame.setExtendedState(frame.getExtendedState() | Frame.MAXIMIZED_BOTH);
        SwingUtilities.getWindowAncestor(((Plotter) plotter).panel()).addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                RemoteMonitoringClient.this.windowClosed = true;
            }
        });
    }

    private void run() {
        try {
            connect();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        addPlots();
        show();
        try {
            while(!this.windowClosed) {
                update();
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
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
        final RemoteMonitoringClient client = new RemoteMonitoringClient();
        client.parse(args);
        client.run();
    }
}
