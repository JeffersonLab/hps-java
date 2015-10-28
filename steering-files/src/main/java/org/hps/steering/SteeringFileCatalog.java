package org.hps.steering;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * This is a static utility class for getting the list of steering file resources. 
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class SteeringFileCatalog {
    
    /**
     * Do not allow class instantiation.
     */
    private SteeringFileCatalog() {        
    }
    
    /**
     * Find the list of steering files in the standard resource directory.     
     * @return The list of steering files.
     */
    public static Set<String> getSteeringResources() {        
        return findSteeringResources("org/hps/steering/monitoring");
    }
    
    /**
     * Get input stream for a steering resource.
     * @param resource the steering file resource
     * @return the input stream or <code>null</code> if resource does not exist or is inaccessible
     */
    public static InputStream getInputStream(String resource) {
        return SteeringFileCatalog.class.getResourceAsStream(resource);
    }
    
    /**
     * Find a list of steering files with the given resource directory.
     * @param resourceDirectory The resource directory.
     * @return The list of matching steering files.
     */
    private static Set<String> findSteeringResources(String resourceDirectory) {
        Set<String> resources = new TreeSet<String>();
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
                //System.out.println("entry: " + entry.getName());
                // Accept the file if it ends with .lcsim and the resource directory matches or is null.
                if (entry.getName().endsWith(".lcsim") && entry.getName().startsWith(resourceDirectory)) {
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
