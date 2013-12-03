package org.lcsim.hps.users.phansson;

import hep.aida.*;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.apache.commons.cli.*;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author phansson
 * @version $Id: mergeSimpleAIDA.java,v 1.4 2012/12/11 23:50:48 meeg Exp $
 */
public class mergeSimpleAIDA {

    /**
     * @param args the command line arguments
     */
    private static Options createCommandLineOptions() {
        Options option = new Options();
        option.addOption("r", true, "Regular expression to match files");
        option.addOption("d", true, "File directory");
        option.addOption("o", true, "Merged file name");
        option.addOption("t", false, "Print files to be merged only");
        option.addOption("a", false, "Average histograms across files instead of just summing them");
        return option;
    }

    public static void main(String[] args) {
        // TODO code application logic here

        Options opts = createCommandLineOptions();
        if (args.length == 0) {
            HelpFormatter help = new HelpFormatter();
            help.printHelp(" ", opts);
            System.exit(1);
        }
        CommandLineParser parser = new PosixParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(opts, args);
        } catch (ParseException ex) {
            Logger.getLogger(mergeSimpleAIDA.class.getName()).log(Level.SEVERE, null, ex);
        }
        String regexp = ".*aida$";
        if (cmd.hasOption("r")) {
            regexp = cmd.getOptionValue("r");
        }

        String path = ".";
        if (cmd.hasOption("d")) {
            path = cmd.getOptionValue("d");
        }
        String outfile = "merged";
        if (cmd.hasOption("o")) {
            outfile = cmd.getOptionValue("o");
        }
        boolean testOnly = cmd.hasOption("t");

        boolean doAverage = cmd.hasOption("a");

        File[] files = listFilesMatching(path, regexp);

        if (files == null || files.length == 0) {
            System.out.println("No files matched " + regexp + " in " + path);
            System.exit(1);
        }

        System.out.println("Found " + files.length + " matching files");

        if (testOnly) {
            for (File f : files) {
                System.out.println(f.getName());
            }
        } else {
            mergeFiles(files, outfile, doAverage);
        }
    }

    public static void mergeFiles(File[] files, String outFileName, boolean doAverage) {

        System.out.println("Merging " + files.length + " into " + outFileName);
        AIDA aida = AIDA.defaultInstance();
        IAnalysisFactory af = aida.analysisFactory();
        IHistogramFactory hf = aida.histogramFactory();
        int count = 0;

        for (File f : files) {

            System.out.println("Processing file f " + f.toString());
            ITree tree = null;
            try {
                tree = af.createTreeFactory().create(f.toString());
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(mergeSimpleAIDA.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(mergeSimpleAIDA.class.getName()).log(Level.SEVERE, null, ex);
            }
            for (String str : tree.listObjectNames()) {
                //if(!str.contains("Cluster energy x")) continue;
                IManagedObject obj = tree.find(str);
                if (IHistogram1D.class.isInstance(obj)) {
                    IHistogram1D h = (IHistogram1D) obj;
                    if (h == null) {
                        System.out.println("Error " + str + " had problems to be cast to 1D?");
                        continue;
                    }
                    String name = h.title();// + " merged";
                    IHistogram1D hc = null;// = aida.histogram1D(name);
                    try {
                        hc = (IHistogram1D) aida.tree().find(name);
                    } catch (IllegalArgumentException ex) {
                        System.out.println(" creating " + name);
                    }
                    if (hc == null) {

                        hc = hf.createCopy(name, h);
                    } else {
                        hc = hf.add(hc.title(), hc, h);
                    }
                    if (name.contains("x=1 y=1")) {
                        System.out.println("Now " + hc.entries() + " entries (<m>=" + hc.mean() + " RMS=" + h.rms() + ")");
                    }
                }
                if (IHistogram2D.class.isInstance(obj)) {
                    IHistogram2D h = (IHistogram2D) obj;
                    if (h == null) {
                        System.out.println("Error " + str + " had problems to be cast to 1D?");
                        continue;
                    }
                    String name = h.title();// + " merged";
                    IHistogram2D hc = null;// = aida.histogram1D(name);
                    try {
                        hc = (IHistogram2D) aida.tree().find(name);
                    } catch (IllegalArgumentException ex) {
                        System.out.println(" creating " + name);
                    }
                    if (hc == null) {

                        hc = hf.createCopy(name, h);
                    } else {
                        hc = hf.add(hc.title(), hc, h);
                    }
                }
            }
            ++count;
        }

        // divide all histograms by the count to get the average
        if (doAverage) {
            for (String str : aida.tree().listObjectNames()) {
                //if(!str.contains("Cluster energy x")) continue;
                IManagedObject obj = aida.tree().find(str);
                if (IHistogram.class.isInstance(obj)) {
                    IHistogram h = (IHistogram) obj;
                    if (h == null) {
                        System.out.println("Error " + str + " had problems to be cast to 1D?");
                        continue;
                    }
                    h.scale(1.0 / count);
                }
            }
        }

        try {
            aida.saveAs(outFileName);
        } catch (IOException ex) {
            Logger.getLogger(mergeSimpleAIDA.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static File[] listFilesMatching(String path, String regexp) {
        //Find files matching the reg exp
        System.out.println("Find files in " + path + " matching the reg exp " + regexp);
        File dir = new File(path);
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException(path + " is no directory.");

            //System.out.println(path+" is not a dir?!");
        }
        final Pattern p = Pattern.compile(regexp);
        System.out.println("pattern " + p.toString());

        return dir.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File file, String name) {
                boolean match = p.matcher(name).matches();
                //System.out.println("accepting file " + name + ": " + match);
                return match;
            }
        });
    }
}
