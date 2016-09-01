package org.hps.users.spaul;

import java.io.IOException;
import java.util.Arrays;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogramFactory;
import hep.aida.ITree;

public class PrintAidaTree {
    public static void main(String arg[]) throws IllegalArgumentException, IOException{
        String input = arg[0];
        IAnalysisFactory af = IAnalysisFactory.create();
        System.out.println("opening file..");
        ITree tree = af.createTreeFactory().create(input, "xml", true);
        System.out.println("opened file");
        String[] names = tree.listObjectNames(".", true);
        String[] types = tree.listObjectTypes(".", true);
        for(int i = 0; i<names.length; i++){
            System.out.println(names[i] + "\t" + types[i]);
        }
    }
}
