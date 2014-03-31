package org.hps.conditions.deprecated;

import java.io.IOException;
import java.io.InputStream;

import org.lcsim.conditions.ConditionsManager;
import org.lcsim.conditions.ConditionsReader;

/**
 * This is a simple extension of {@link org.lcsim.conditions.ConditionsReader} to find
 * text file conditions data for the HPS Test Run 2012.  It basically just checks
 * two resource locations for files and fails if they do not exist.
 * 
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @version $Id: TestRunConditionsReader.java,v 1.4 2013/10/17 23:04:19 jeremy Exp $
 */
public class TestRunConditionsReader extends ConditionsReader {

    private String detectorName = null;

    public TestRunConditionsReader() {
    }
    
    // FIXME: The reader argument is never used anywhere so this ctor is not needed.
    public TestRunConditionsReader(ConditionsReader reader) {
    }
    
    public InputStream open(String name, String type) throws IOException {
        
        //System.out.println(this.getClass().getSimpleName() + ".open - " + name + ", " + type);
        
        // Check the detector base directory.        
        InputStream in = getClass().getResourceAsStream("/" + detectorName + "/" + name + "." + type);
        if (in == null) {
            // Check for embedded jar resources e.g. in hps-java.
            in = getClass().getResourceAsStream("/org/hps/calib/testrun/" + name + "." + type);

            // If these failed to find conditions, then something went wrong.
            if (in == null) {
                throw new IOException("Conditions " + name + " for " + detectorName + " with type " + type + " were not found");
            }       
        }
        return in;
    }

    public void close() throws IOException {
    }

    public boolean update(ConditionsManager manager, String detectorName, int run) throws IOException {
        this.detectorName = detectorName;
        return true;
    }
}
