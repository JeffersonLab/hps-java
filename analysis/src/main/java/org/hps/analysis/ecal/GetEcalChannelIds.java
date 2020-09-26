/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.analysis.ecal;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.ITree;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ngraf
 */
public class GetEcalChannelIds {

    public static void main(String[] args) throws IllegalArgumentException, IOException {

        String plotFile = "D:/work/hps/analysis/physrun2019/ecalibration/noGainAnalysis/2019_muonECalNoGain_257-261-267-269-271_2Mevents.aida";
        IAnalysisFactory analysisFactory = IAnalysisFactory.create();
        ITree tree = analysisFactory.createTreeFactory().create(new File(plotFile).getAbsolutePath());
// Make a list to contain the plot names.
        List<String> list = new ArrayList<String>();
// Get the histograms names.
        List<String> objectNameList = getTreeFiles(tree);

        for (String s : objectNameList) {
            System.out.println(s);
        }
        //TODO do this crystal-by-crystal instead of just by row.
        for (int ix = 6; ix < 23; ++ix) {
            String histoName = "/dimuon/clusterAnalysis/fiducial mu+ single-crystal cluster track momentum ix " + ix;
            if (objectNameList.contains(histoName)) {
                IHistogram1D hist = (IHistogram1D) tree.find(histoName);
                double mean = hist.mean();
                System.out.println("fiducial mu+ single-crystal cluster track momentum ix "  + ix + " : " + mean);
            }
        }
        for (int ix = -22; ix < -5; ++ix) {
            String histoName = "/dimuon/clusterAnalysis/fiducial mu- single-crystal cluster track momentum ix " + ix;
            if (objectNameList.contains(histoName)) {
                IHistogram1D hist = (IHistogram1D) tree.find(histoName);
                double mean = hist.mean();
                System.out.println("fiducial mu- single-crystal cluster track momentum ix "  + ix + " : " + mean);
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
