package org.hps.online.recon.aida;

import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

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
import hep.aida.IBaseHistogram;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterRegion;
import hep.aida.IPlotterStyle;
import hep.aida.ITextStyle;
import hep.aida.ITree;
import hep.aida.ITreeFactory;
import hep.aida.ref.AnalysisFactory;
import hep.aida.ref.plotter.Plotter;
import hep.aida.ref.remote.rmi.client.RmiStoreFactory;

/**
 * Displays plots from the <code>RemoteChronoDriver</code>
 * through an RMI connection
 */
public class HistClient {

    static {
        System.setProperty("hep.aida.IAnalysisFactory", AnalysisFactory.class.getName());
    }

    private Logger LOG = Logger.getLogger(HistClient.class.getPackage().getName());

    private final Options options = new Options();

    private String host = null;
    private Integer port = 2001;
    private String serverName = "RmiAidaServer";

    private ITree clientTree = null;
    private final IAnalysisFactory af = IAnalysisFactory.create();
    private final IPlotterFactory pf = af.createPlotterFactory();
    private final ITreeFactory tf = af.createTreeFactory();
    private final IPlotter plotter = pf.create("Remote AIDA Plots");

    private boolean windowClosed = false;

    private Integer plotterWidth = 800;
    private Integer plotterHeight = 600;

    private static final long DEFAULT_UPDATE_INTERVAL = 1000L;
    private Long updateInterval = DEFAULT_UPDATE_INTERVAL;

    private String aidaDir = "/";

    public HistClient() {

        options.addOption(new Option("h", "help",     false, "Print help and exit"));
        options.addOption(new Option("p", "port",     true,  "Network port of server"));
        options.addOption(new Option("n", "name",     true,  "Name of RMI server"));
        options.addOption(new Option("H", "host",     true,  "Host name or IP address of server"));
        options.addOption(new Option("i", "interval", true,  "Update interval in millis"));
        options.addOption(new Option("x", "width",    true,  "Plotter window width (pixels)"));
        options.addOption(new Option("y", "height",   true,  "Plotter window height (pixels)"));
        options.addOption(new Option("d", "dir",      true,  "Dir in AIDA tree (default is root dir)"));

        IPlotterStyle style = plotter.style();
        style.dataStyle().lineStyle().setColor("black");
        style.dataStyle().fillStyle().setColor("blue");
        style.dataStyle().errorBarStyle().setVisible(false);
        ITextStyle titStyle = pf.createTextStyle();
        titStyle.setBold(true);
        titStyle.setFontSize(14);
        style.titleStyle().setTextStyle(titStyle);
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
        if (cl.hasOption("n")) {
            serverName = cl.getOptionValue("n");
        }
        if (cl.hasOption("x")) {
            plotterWidth = Integer.parseInt(cl.getOptionValue("x"));
        }
        if (cl.hasOption("y")) {
            plotterHeight = Integer.parseInt(cl.getOptionValue("y"));
        }
        if (cl.hasOption("i")) {
            updateInterval = Long.parseLong(cl.getOptionValue("i"));
            if (updateInterval < 1L) {
                throw new IllegalArgumentException("Invalid update interval: " + updateInterval);
            }
        }
        if (cl.hasOption("d")) {
            aidaDir = cl.getOptionValue("d");
        }
    }

    private void connect() throws IOException {
        boolean clientDuplex = false;
        boolean hurry = false;
        String treeBindName = "//"+host+":"+port+"/"+serverName;
        LOG.config("Connecting to RMI server: " + treeBindName);
        String options = "duplex=\""+clientDuplex+"\",RmiServerName=\"rmi:"+treeBindName+"\",hurry=\""+hurry+"\"";
        clientTree = tf.create(host, RmiStoreFactory.storeType, true, false, options);
    }

    private void close() throws IOException {
        clientTree.close();
    }

    private void addPlots() {
        String[] names = clientTree.listObjectNames(aidaDir, true);
        String[] types = clientTree.listObjectTypes(aidaDir, true);
        List<String> histos = new ArrayList<String>();
        for (int i = 0; i < names.length; i++) {
            if (types[i].contains("Histogram")) {
                histos.add(names[i]);
            }
        }
        int n = histos.size();
        if (n > 3) {
            int rowsCols = (int) Math.ceil(Math.sqrt(n));
            plotter.createRegions(rowsCols, rowsCols);
        } else {
            plotter.createRegions(n);
        }
        int regionInd = 0;
        for (String name : histos) {
            IBaseHistogram hist = (IBaseHistogram) clientTree.find(name);
            String xLabel = "";
            if (hist.annotation().hasKey("xAxisLabel")) {
                xLabel = hist.annotation().value("xAxisLabel");
            }
            String yLabel = "";
            if (hist.annotation().hasKey("yAxisLabel")) {
                yLabel = hist.annotation().value("yAxisLabel");
            }
            IPlotterRegion region = plotter.region(regionInd);
            IPlotterStyle style = pf.createPlotterStyle();
            style.xAxisStyle().setLabel(xLabel);
            style.yAxisStyle().setLabel(yLabel);
            if (hist instanceof IHistogram2D) {
                style.setParameter("hist2DStyle", "colorMap");
                style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
                style.dataStyle().fillStyle().setParameter("showZeroHeightBins", Boolean.FALSE.toString());
            }
            region.setStyle(style);
            region.plot(hist);
            ++regionInd;
        }
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
                HistClient.this.windowClosed = true;
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
        final HistClient client = new HistClient();
        client.parse(args);
        client.run();
    }
}
