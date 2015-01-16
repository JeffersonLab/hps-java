package org.hps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.job.JobManager;
import org.hps.steering.SteeringFileCatalog;

/**
 * Perform a dry run on all steering files to make sure they are valid
 * and do not reference non-existent Drivers or define invalid parameters.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SteeringFilesTest extends TestCase {
    
    /**
     * Dummy job manager.
     */
    static class DummyJobManager extends JobManager {
        protected void setupInputFiles() {
            // We don't actually want this to happen, as dummy variables are being used!
        }
    }
    
    /**
     * Perform a dry run job on every steering file in the steering-files module.
     * @throws Exception If there was an error initializing from one of the steering files.
     */
    public void testSteeringFiles() throws Exception {
        DatabaseConditionsManager.getInstance().setLogLevel(Level.SEVERE);
        List<String> steeringResources = SteeringFileCatalog.find();
        for (String steeringResource : steeringResources) {
            steeringFileCheck(steeringResource);
        }
    }
        
    /**
     * Perform a dry run job on a steering file with the given resource path.
     * @param resourcePath The resource path in the jar.
     * @throws Exception If there was an error initializing from this steering file.
     */
    private static void steeringFileCheck(String resourcePath) throws Exception {        
        System.out.println("running steering file check on " + resourcePath);        
        JobManager job = new DummyJobManager();        
        Set<String> variableNames = findVariableNames(resourcePath);
        for (String variableName : variableNames) {
            String value = "dummy";
            if (variableName.equals("runNumber")) {
                value = "1";
            }
            job.addVariableDefinition(variableName, value);
        }
        job.setup(resourcePath);
        job.setPerformDryRun(true);
        job.run();
    }
    

    static final Pattern varPattern = Pattern.compile("[$][{][a-zA-Z_-]*[}]");
    /**
     * Find the variable names in an lcsim steering file.
     * @param resourcePath The resource path to the file.
     * @return The set of variable names.
     * @throws IOException If there was an file IO error. 
     */
    private static Set<String> findVariableNames(String resourcePath) throws IOException {        
        Set<String> variableNames = new HashSet<String>();
        InputStream inputStream = SteeringFileCatalog.class.getResourceAsStream(resourcePath);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while((line = reader.readLine()) != null) {
            Matcher match = varPattern.matcher(line);
            while (match.find()) {

                // Variable string with ${...} enclosure included.
                String variable = match.group();

                // The name of the variable for lookup.
                String variableName = variable.substring(2, variable.length() - 1);

                variableNames.add(variableName);
            }
        }            
        reader.close();
        inputStream.close();        
        return variableNames;        
    }    
}
