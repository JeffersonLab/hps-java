package org.hps.analysis.examples;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.ITree;
import java.util.ArrayList;
import java.util.List;

public class FindSVTPhaseProblems {

    public static void main(String[] args) throws Exception {

        String fileName = "D:/work/hps/analysis/physrun2019/v0Analysis/2019_samplePartitionV0SkimAnalysis_20201027.aida";
        // Get the plots file and open it.
        IAnalysisFactory af = IAnalysisFactory.create();
        ITree tree = af.createTreeFactory().create(fileName);

        // Get the histograms names.
        List<String> objectNameList = getTreeFiles(tree);
        for (String s : objectNameList) {
            if (s.contains(" SVT Cluster time")) {
//                System.out.println(s);
                int run = 0;
                if (s.contains("/9")) {
                    run = Integer.parseInt(s.substring(1, 5));
                } else {
                    run = Integer.parseInt(s.substring(1, 6));
                }
//                System.out.println("run: " + run);
                IHistogram1D histo = (IHistogram1D) tree.find(s);
                int nBins = histo.axis().bins();
                int binMax = 0;
                double maxEntry = 0;
                for (int i = 0; i < nBins; ++i) {
                    if (histo.binHeight(i) > maxEntry) {
                        maxEntry = histo.binHeight(i);
                        binMax = i;
                    }
                }
//                System.out.println("has " + histo.allEntries() + " entries");
//                System.out.println("maxEntries " + maxEntry + " at bin " + binMax);
                if (binMax > 93) {
                    System.out.println("good run: " + run);
                } else {
                    System.out.println("bad run: " + run);
                }
            }
        }

    }

    private static final List<String> getTreeFiles(ITree tree) {
        return getTreeFiles(tree, "/");
    }

    private static final List<String> getTreeFiles(ITree tree, String rootDir) {
        // Make a list to contain the plot names.
        List<String> list = new ArrayList<String>();

        // Iterate over the objects at the indicated directory of the tree.
        String objectNames[] = tree.listObjectNames(rootDir);
        for (String objectName : objectNames) {
            // Convert the object name to a char array and check the
            // last character. Directories end in '/'.
            char[] plotChars = objectName.toCharArray();

            // If the object is a directory, process any objects inside
            // of it as well.
            if (plotChars[plotChars.length - 1] == '/') {
                List<String> dirList = getTreeFiles(tree, objectName);
                list.addAll(dirList);
            } // Otherwise, just add the object to the list.
            else {
                list.add(objectName);
            }
        }

        // Return the compiled list.
        return list;
    }

}
