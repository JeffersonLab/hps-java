package org.hps.analysis.plots;

import hep.aida.IAnalysisFactory;
import hep.aida.IBaseHistogram;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogram3D;
import hep.aida.IHistogramFactory;
import hep.aida.IManagedObject;
import hep.aida.ITree;
import java.io.File;
import org.apache.commons.lang3.StringUtils;
import org.lcsim.util.aida.AIDA;

/**
 * Converts plots from aida to root format
 *
 * @author Norman A. Graf
 */
public class ConvertPlots {

    public static void main(String[] args) throws Exception {
        String tmp = args[0];
        File inputFile = new File(tmp);
        String outputFileName = StringUtils.removeEnd(tmp, ".aida");

        AIDA aida = AIDA.defaultInstance();
        IAnalysisFactory af = aida.analysisFactory();
        IHistogramFactory hf = aida.histogramFactory();
        ITree myTree = aida.tree();

        ITree srcTree = af.createTreeFactory().create(inputFile.getAbsolutePath());
        // get the list of histograms in this file 
        String[] objectTypes = srcTree.listObjectTypes("/", true);
        String[] objectNames = srcTree.listObjectNames("/", true);
        for (int pathIndex = 0; pathIndex < objectNames.length; pathIndex++) {
            if (objectTypes[pathIndex].startsWith("IHistogram")) {
                String histogramName = objectNames[pathIndex];
                System.out.println("processing: " + histogramName);
                IBaseHistogram src = (IBaseHistogram) srcTree.find(histogramName);
                String path = histogramName.substring(0, histogramName.lastIndexOf('/'));
                aida.tree().mkdirs(path);
                IManagedObject object = srcTree.find(histogramName);
                String filePath = srcTree.findPath(object);
                create(hf, filePath, src);
            }
        }

        aida.saveAs(outputFileName + ".root");
    }

    private static void create(IHistogramFactory factory, String path, IBaseHistogram src) {
        if (src instanceof IHistogram1D) {
            factory.createCopy(path, (IHistogram1D) src);
        } else if (src instanceof IHistogram2D) {
            factory.createCopy(path, (IHistogram2D) src);
        } else if (src instanceof IHistogram3D) {
            factory.createCopy(path, (IHistogram3D) src);
        }
    }
}
