package org.hps.users.phansson.tools;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IManagedObject;
import hep.aida.ITree;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

/**
 *
 * @author phansson Dumps all histograms in AIDA files to a standard text format
 * @version $Id: DumpAIDATextFiles.java,v 1.3 2013/11/01 19:23:53 meeg Exp $
 */
public class DumpAIDATextFiles {

    private static Options createCommandLineOptions() {

        Options options = new Options();
        //options.addOption(new Option("p", true, "The pattern to match. [NOT IMPLEMENTED YET]"));
        options.addOption(new Option("f", true, "The file to use."));
        options.addOption(new Option("d", true, "The directory with files."));
        return options;

    }

    /**
     * @param args the command line arguments
     */
    public static class TextFileUtil {

        public TextFileUtil() {
        }

        public void createTextFile(String fileName, ITree tree) {
            //System.out.println("-----\nCreating text file "+fileName+" from tree with " + tree.listObjectNames().length + " objects");
            FileWriter fWriter;
            PrintWriter pWriter;
            try {
                fWriter = new FileWriter(fileName);
                pWriter = new PrintWriter(fWriter);

                for (String name : tree.listObjectNames()) {
                    //System.out.println(str);
                    IManagedObject obj = tree.find(name);
                    if (obj instanceof IHistogram1D) {
                        //System.out.println(name + " is a histogram");
                        IHistogram1D h = (IHistogram1D) obj;
                        String htxt = convertHist1D(h);
                        pWriter.println(htxt);
                    }
                    if (obj instanceof IHistogram2D) {
                        //System.out.println(name + " is a histogram");
                        IHistogram2D h = (IHistogram2D) obj;
                        String htxt = convertHist2D(h);
                        pWriter.println(htxt);
                    }
                }

                fWriter.close();
                pWriter.close();
            } catch (IOException ex) {
                Logger.getLogger(DumpAIDATextFiles.class.getName()).log(Level.SEVERE, null, ex);
            }
            //System.out.println("-----");


        }

        public String convertHist1D(IHistogram1D h) {
            //Type title bincontent
            String htxt = "IHistogram1D \"" + h.title() + "\" " + h.axis().bins() + " " + h.axis().lowerEdge() + " " + h.axis().upperEdge();
            for (int ibin = 0; ibin < h.axis().bins(); ++ibin) {
                htxt += " " + h.binEntries(ibin);
                htxt += " " + h.binHeight(ibin);
            }
            return htxt;
        }

        public String convertHist2D(IHistogram2D h) {
            //Type title bincontent
            String htxt = "IHistogram2D \"" + h.title() + "\" " + h.xAxis().bins() + " " + h.xAxis().lowerEdge() + " " + h.xAxis().upperEdge() + " " + h.yAxis().bins() + " " + h.yAxis().lowerEdge() + " " + h.yAxis().upperEdge();
            for (int xbin = 0; xbin < h.xAxis().bins(); ++xbin) {
                for (int ybin = 0; ybin < h.yAxis().bins(); ++ybin) {
                    htxt += " " + h.binEntries(xbin, ybin);
                    htxt += " " + h.binHeight(xbin, ybin);
                }
            }
            return htxt;
        }
    }

    public static void main(String[] args) {
        // TODO code application logic here

        Options options = createCommandLineOptions();
        if (args.length == 0) {
            System.out.println("TestRunEvioToLcio [options] [evioFiles]");
            HelpFormatter help = new HelpFormatter();
            help.printHelp(" ", options);
            System.exit(1);
        }
        CommandLineParser parser = new PosixParser();
        CommandLine cl = null;
        try {
            cl = parser.parse(options, args);
        } catch (ParseException e) {
            throw new RuntimeException("Problem parsing command line options.", e);
        }
        List<String> fileList = new ArrayList<String>();
        if (cl.hasOption("f")) {
            fileList.add(cl.getOptionValue("f"));
        } else if (cl.hasOption("d")) {
            //check if pattern is to be used or simply use run all aida files
            String dirName = cl.getOptionValue("d");
            if (cl.hasOption("p")) {
                System.out.println("The pattern option is not implemented. Please do it!");
                System.exit(1);
            } else {
                File dir = new File(dirName);
                for (File f : dir.listFiles()) {
                    if (f.isFile() && f.getName().contains(".aida")) {
                        fileList.add(f.getAbsolutePath());
                    }
                }
            }
        }
        IAnalysisFactory af = IAnalysisFactory.create();
        TextFileUtil util = new TextFileUtil();
        for (String file : fileList) {
            System.out.println("Converting file " + file);
            if (!file.contains(".aida")) {
                System.out.println("This is not an AIDA file?!");
                continue;
            }
            ITree tree = null;
            try {
                tree = af.createTreeFactory().create(file);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(DumpAIDATextFiles.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(DumpAIDATextFiles.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (tree == null) {
                System.out.println("Couldn't create \"tree\" for file: " + file);
                continue;
            }
            String txtFileName = file.replaceAll(".aida", ".histtxt");
            util.createTextFile(txtFileName, tree);

        }

    }
}
