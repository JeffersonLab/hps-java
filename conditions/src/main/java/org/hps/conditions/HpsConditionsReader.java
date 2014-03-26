package org.hps.conditions;

import java.io.IOException;
import java.io.InputStream;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.conditions.ConditionsReader;

/**
 * 
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: HpsConditionsReader.java,v 1.3 2012/11/20 19:56:40 meeg Exp $
 */
public class HpsConditionsReader extends ConditionsReader {

    private String detectorName = null;
    private int run;

    public HpsConditionsReader(ConditionsReader reader) {
    }

    @Override
    public InputStream open(String name, String type) throws IOException {
        InputStream in = getClass().getResourceAsStream("/" + detectorName + "/" + name + "." + type);
        if (in == null) {
            in = getClass().getResourceAsStream("/org/lcsim/hps/calib/proposal2014/" + name + "." + type);
            if (in == null) {
                throw new IOException("Conditions " + name + " for " + detectorName + " with type " + type + " were not found");
            }
        }
        return in;
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    protected boolean update(ConditionsManager manager, String detectorName, int run) throws IOException {
        this.detectorName = detectorName;
        this.run = run;

        return true;
    }
}
