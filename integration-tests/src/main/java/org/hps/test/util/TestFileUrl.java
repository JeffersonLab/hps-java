package org.hps.test.util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import junit.framework.TestCase;

import org.lcsim.util.cache.FileCache;

/**
 * Some utility methods for getting test data from the lcsim.org site.
 * 
 * @author Jeremy McCormick, SLAC
 */
public final class TestFileUrl {
    
    /**
     * Should not instantiate this class.
     */
    private TestFileUrl() {
    }
    
    /**
     * Base URL with test data.
     */
    private static final String BASE_URL = "http://www.lcsim.org/test/hps-java/";
        
    
    private static URL createUrl(Class<? extends TestCase> testClass, String fileName) throws MalformedURLException {
        return new URL(BASE_URL + "/" + testClass.getSimpleName() + "/" + fileName);
    }
    
    public static File getInputFile(URL url) throws IOException {
        return new FileCache().getCachedFile(url);
    }
    
    public static File getInputFile(Class<? extends TestCase> testClass, String fileName) throws Exception {
        return getInputFile(createUrl(testClass, fileName));
    }
}
