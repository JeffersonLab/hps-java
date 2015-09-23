package org.hps.crawler;

import java.io.File;

public class CrawlerFileUtilities {
         
    static boolean isHpsFile(File file) {
        return file.getName().startsWith("hps");
    }
    
    static int getRunFromFileName(File file) {
        String name = file.getName();
        return Integer.parseInt(name.substring(4, 8));
    }
}
