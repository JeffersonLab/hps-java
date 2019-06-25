package org.hps.online.recon.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.hps.online.recon.Command;

/**
 * Create one or more new stations using the current configuration properties 
 * of the server.  
 */
public class CreateCommand extends Command {

    CreateCommand() {
        super("create", "Create a new station", "[options] [number_of_stations]",
                "Stations are not started by default.");
    }
    
    /**
     * Set the number of stations to create
     * @param count The number of stations to create
     */
    void setCount(Integer count) {
        this.setParameter("count", count.toString());
    }
    
    /**
     * Set whether or not the stations should be started automatically.
     * @param start True if stations should be started automatically
     */
    void setStart(Boolean start) {
        this.setParameter("start", start);
    }
    
    protected Options getOptions() {
        options.addOption(new Option("s", "start", false, "automatically start the new stations"));
        return options;
    }
            
    @Override
    protected void process(CommandLine cl) {
        if (cl.getArgList().size() == 1) {
            setCount(Integer.valueOf(cl.getArgList().get(0)));
        } else if (cl.getArgList().size() > 1) {
            throw new IllegalArgumentException("Too many extra args.");
        }
        if (cl.hasOption("s")) {
            setStart(true);
        } else {
            setStart(false);
        }
    }
}    