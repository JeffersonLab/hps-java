package org.hps.steering;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * This is a static utility class for getting the list of steering file resources. 
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class SteeringFileCatalog {
    
    private SteeringFileCatalog() {        
    }
    
    /**
     * Find the list of steering files in the standard resource directory.     
     * @return The list of steering files.
     */
    public static List<String> find() {
        return find("org/hps/steering");
    }
    
    /**
     * Find a list of steering files with the given resource directory.
     * @param resourceDirectory The resource directory.
     * @return The list of matching steering files.
     */
    public static List<String> find(String resourceDirectory) {        
        List<String> resources = new ArrayList<String>();              
        URL url = SteeringFileCatalog.class.getResource("SteeringFileCatalog.class");        
        String scheme = url.getProtocol();
        if (!"jar".equals(scheme)) {
            throw new IllegalArgumentException("Unsupported URL protocol: " + url.getProtocol());
        }
        try {
            JarURLConnection con = (JarURLConnection) url.openConnection();
            JarFile archive = con.getJarFile();
            Enumeration<JarEntry> entries = archive.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();                
                if (entry.getName().endsWith(".lcsim") && 
                        (resourceDirectory == null || resourceDirectory.length() == 0 || entry.getName().contains(resourceDirectory))) {
                    // Accept the file if it ends with .lcsim and the resource directory matches or is not set (e.g. null or zero length string).
                    resources.add("/" + entry.getName());
                }
            }
            archive.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return resources;
    }  
}