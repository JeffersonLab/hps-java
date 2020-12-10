package org.hps.online.recon.remoteaida;

import java.awt.Component;
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
import javax.swing.JPanel;
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

    static {
        System.setProperty("hep.aida.IAnalysisFactory", AnalysisFactory.class.getName());
    }

    private final Options options = new Options();

    private String host = null;
    private Integer port = 2001;
    private String serverName = "RmiAidaServer";

    private ITree clientTree = null;
    private final IAnalysisFactory af = IAnalysisFactory.create();
    private final IPlotterFactory pf = af.createPlotterFactory();
    private final ITreeFactory tf = af.createTreeFactory();    private final IPlotterStyle avgStyle = pf.createPlotterStyle();

    private final IPlotter plotter = pf.create("Monitoring Plots");

    private final Map<IDataPointSet, IPlotterRegion> plots =
            new HashMap<IDataPointSet, IPlotterRegion>();

    private final int NBEFORE = 17;
    private final int NAFTER = 2;

    private boolean windowClosed = false;

    private Integer plotterWidth = 800;
    private Integer plotterHeight = 600;

    private static final long DEFAULT_UPDATE_INTERVAL = 1000L;
    private Long updateInterval = DEFAULT_UPDATE_INTERVAL;

    public RemoteMonitoringClient() {

        options.addOption(new Option("h", "help",   false, "Print help and exit"));
        options.addOption(new Option("p", "port",   true,  "Network port of server"));
        options.addOption(new Option("s", "server", true,  "Name of RMI server"));
        options.addOption(new Option("H", "host",   true,  "Host name or IP address of server"));
        options.addOption(new Option("x", "width",  true,  "Plotter window width (pixels)"));
        options.addOption(new Option("y", "height", true,  "Plotter window height (pixels)"));

        // TODO: option to set update interval in seconds
        // TODO: some way to select or filter plots to display (by dir like 'perf' or 'subdet')

        IPlotterStyle style = plotter.style();
        style.xAxisStyle().setParameter("type", "date");
        style.dataStyle().outlineStyle().setColor("black");
        style.dataStyle().markerStyle().setColor("black");
        style.dataStyle().errorBarStyle().setVisible(false);
        style.regionBoxStyle().setVisible(true);
        style.regionBoxStyle().borderStyle().setLineType("solid");
        ITextStyle titStyle = pf.createTextStyle();
        titStyle.setBold(true);
        titStyle.setFontSize(14);
        style.titleStyle().setTextStyle(titStyle);
        style.legendBoxStyle().setVisible(false);

        avgStyle.dataStyle().outlineStyle().setVisible(false);
        avgStyle.dataStyle().markerStyle().setColor("red");
        avgStyle.dataStyle().markerStyle().setShape("cross");
        avgStyle.dataStyle().errorBarStyle().setVisible(false);
    }

    @SuppressWarnings("deprecation")
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
        if (cl.hasOption("x")) {
            plotterWidth = Integer.parseInt(cl.getOptionValue("x"));
        }
        if (cl.hasOption("y")) {
            plotterHeight = Integer.parseInt(cl.getOptionValue("y"));
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
        System.out.println("Remote tree objects: ");
        clientTree.ls("/", true);
        System.out.println();
    }

    private void close() throws IOException {
        clientTree.close();
    }

    private void addPlots() {
        String[] names = clientTree.listObjectNames("/", true);
        String[] types = clientTree.listObjectTypes("/", true);
        List<String> dpsNames = new ArrayList<String>();
        for (int i = 0; i < names.length; i++) {
            if (types[i].equals("IDataPointSet") && !names[i].contains("/avg")) {
                dpsNames.add(names[i]);
            }
        }
        int n = dpsNames.size();
        int rowsCols = (int) Math.ceil(Math.sqrt(n));
        plotter.createRegions(rowsCols, rowsCols);
        int regionInd = 0;
        for (String dpsName : dpsNames) {
            IDataPointSet dps = (IDataPointSet) clientTree.find(dpsName);
            String label = "";
            if (dps.annotation().hasKey("label")) {
                label = dps.annotation().value("label");
            }
            IPlotterRegion region = plotter.region(regionInd);
            IPlotterStyle regStyle = pf.createPlotterStyle();
            regStyle.yAxisStyle().setLabel(label);
            region.setStyle(regStyle);
            region.plot(dps);
            plots.put(dps, region);

            String[] spl = dpsName.split("/");
            String dirName = spl[1];
            String plotName = spl[2];
            String avgPath = "/" + dirName + "/avg/" + plotName;
            try {
                IDataPointSet avg = (IDataPointSet) clientTree.find(avgPath);
                region.plot(avg, avgStyle);
            } catch (IllegalArgumentException e) {
            }
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

    private void setPanelEnabled(JPanel panel, Boolean isEnabled) {
        panel.setEnabled(isEnabled);
        Component[] components = panel.getComponents();
        for (Component component : components) {
            if (component instanceof JPanel) {
                setPanelEnabled((JPanel) component, isEnabled);
            }
            component.setEnabled(isEnabled);
        }
    }

    private void show() {
        plotter.setParameter("plotterWidth", plotterWidth.toString());
        plotter.setParameter("plotterHeight", plotterHeight.toString());
        plotter.show();
        setPanelEnabled(((Plotter) plotter).panel(), false);
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(((Plotter) plotter).panel());
        frame.addWindowListener(new WindowAdapter() {
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
        while(!this.windowClosed) {
            update();
            try {
                Thread.sleep(updateInterval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        final RemoteMonitoringClient client = new RemoteMonitoringClient();
        client.parse(args);
        client.run(); // TODO: Should probably catch exceptions
    }
}
