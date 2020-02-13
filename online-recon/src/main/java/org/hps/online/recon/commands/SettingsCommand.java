package org.hps.online.recon.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.hps.online.recon.Command;

/**
 * Update values of server settings.
 * 
 * The new settings will only take effect for new stations.
 * 
 * Running this command with no arguments returns the current settings.
 */
public final class SettingsCommand extends Command {
    
    SettingsCommand() {            
        super("settings", "Update or get server settings", "[options]",
                "Updated settings will take effect only for newly created stations." + '\n' +
                "Run with no arguments to get the current settings.");
    }
    
    protected Options getOptions() {
        options.addOption(new Option("s", "start", true, "starting station ID"));
        options.addOption(new Option("w", "workdir", true, "work dir (default is current dir where server is started)"));
        options.addOption(new Option("b", "basename", true, "station base name"));
        return options;
    }
    
    protected void process(CommandLine cl) {
        if (cl.hasOption("s")) {
            setParameter("start", cl.getOptionValue("s"));
        }
        if (cl.hasOption("w")) {
            setParameter("workdir", cl.getOptionValue("w"));
        }
        if (cl.hasOption("b")) {
            setParameter("basename", cl.getOptionValue("b"));
        }
    }
}    
