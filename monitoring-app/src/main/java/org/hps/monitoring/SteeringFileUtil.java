package org.hps.monitoring;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * This class provides a static utility method to get a list of steering file resources from the package in hps-java that contains these files.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @version $Id: SteeringFileUtil.java,v 1.2 2013/11/05 17:15:04 jeremy Exp $
 */
public class SteeringFileUtil {

    /**
     * Get the files that end in .lcsim from all loaded jar files.
     * @return A list of embedded steering file resources.
     */
    public static String[] getAvailableSteeringFileResources(String packageName) {
        List<String> resources = new ArrayList<String>();
        URL url = SteeringFileUtil.class.getResource("SteeringFileUtil.class");
        String scheme = url.getProtocol();
        if (!"jar".equals(scheme)) {
            throw new IllegalArgumentException("Unsupported scheme: " + scheme);
        }
        try {
            JarURLConnection con = (JarURLConnection) url.openConnection();
            JarFile archive = con.getJarFile();
            Enumeration<JarEntry> entries = archive.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".lcsim") && entry.getName().contains(packageName)) {
                    resources.add(entry.getName());
                }
            }
            archive.close();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }        
        java.util.Collections.sort(resources);
        String[] arr = new String[resources.size()];
        for (int i=0; i<arr.length; i++) {
            arr[i] = resources.get(i);
        }
        return arr;
    }
}
