package org.hps.online.recon;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.json.JSONObject;

/**
 * Command to be sent to the online reconstruction server.
 */
abstract class ClientCommand {

    private final String name;
    private final String description;
    private final String commandExtra;
    private final String commandFooter;
    
    private Map<String, Object> parameters = new HashMap<String, Object>();    
    
    protected Options options = new Options();
            
    ClientCommand(String name, String description, String commandExtra, String commandFooter) {
        this.name = name;
        this.description = description;
        this.commandExtra = commandExtra;
        this.commandFooter = commandFooter;
        
        options.addOption(new Option("", "help", false, "print command help"));
    }

    /**
     * Get the name of the command.
     * @return
     */
    String getName() {
        return this.name;
    }

    /**
     * Set a parameter for the command.
     * @param name
     * @param value
     */
    void setParameter(String name, Object value) {
        parameters.put(name, value);
    }
    
    /**
     * Convert the object to JSON.
     * @return
     */
    JSONObject toJSON() {
        JSONObject jo = new JSONObject();
        jo.put("command", this.name);
        jo.put("parameters", new JSONObject(parameters));
        return jo;
    }
    
    /**
     * Convert the object to a string (by default returns JSON format).
     */
    public String toString() {
        return toJSON().toString();
    }
    
    /**
     * Get the options for command line usage.
     * @return
     */
    Options getOptions() {
        return options;
    }
    
    void printUsage() {
        final HelpFormatter help = new HelpFormatter();
        help.printHelp(this.name + " " + this.commandExtra, this.description, 
                getOptions(), this.commandFooter);
    }
    
    /**
     * Parse command line options.
     * 
     * This method checks for the help option, and if it exists,
     * prints usage and exits the program.
     * 
     * Sub-classes should always call the super method when overriding
     * so that the help option is activated properly.
     * 
     * @param cl
     */
    void parse(CommandLine cl) {
        if (cl.hasOption("help")) {
            printUsage();
            System.exit(0);
        }
    }
    
    /**
     * Read station ID list from extra arguments and add as list parameter.
     * @param cl
     */
    void readStationIDs(CommandLine cl) {
        List<Integer> ids = new ArrayList<Integer>();
        for (String arg : cl.getArgList()) {
            ids.add(Integer.parseInt(arg));
        }
        this.setParameter("ids", ids);
    }
               
    /**
     * Create one or more new stations using the current configuration properties 
     * from the server (the "config" command can be used to change theSE properties).
     */
    static final class CreateCommand extends ClientCommand {

        CreateCommand() {
            super("create", "Create a new station", "", 
                    "Stations are not started by default.");
        }
                                        
        void setCount(Integer count) {
            this.setParameter("count", count.toString());
        }
        
        void setStart(Boolean start) {
            this.setParameter("start", start);
        }
        
        Options getOptions() {
            options.addOption(new Option("n", "number", true, "number of instances to start (default 1)")); 
            options.addOption(new Option("s", "start", false, "automatically start the new stations"));
            return options;
        }
                
        @Override
        void parse(CommandLine cl) {
            super.parse(cl);
            if (cl.hasOption("n")) {
                setCount(Integer.valueOf(cl.getOptionValue("n")));
            }
            if (cl.hasOption("s")) {
                setStart(true);
            } else {
                setStart(false);
            }
        }
    }    
    
    /**
     * Stop a list of stations by their IDs or if none are given
     * then stop all stations.
     */
    static final class StopCommand extends ClientCommand {
        
        StopCommand() {            
            super("stop", "Stop a station", "[IDs]", 
                    "Provide a list of IDs or none for all");
        }
            
        void parse(CommandLine cl) {
            super.parse(cl);
            readStationIDs(cl);
        }        
    }
    
    /**
     * Get a list of station info as JSON from a list of IDs or
     * if none are given then return info for all stations.
     */
    static final class ListCommand extends ClientCommand {
        
        ListCommand() {
            super("list", "List station information in JSON format", "[IDs]",
                    "Provide a list of IDs or none to list information for all");
        }
                
        void parse(CommandLine cl) {
            super.parse(cl);
            readStationIDs(cl);
        }       
    }
    
    /**
     * Set server configuration properties from a local file.
     * 
     * If no file is provided with the -c option then the existing
     * server configuration will be returned as JSON.
     */
    static final class ConfigCommand extends ClientCommand {

        private Properties prop;
                
        ConfigCommand() {
            super("config", "Set new server configuration properties", "",
                    "Configuration will take effect for newly created stations."
                    + " If no new config is provided the existing config will be printed.");
        }
        
        @Override        
        Options getOptions() {
            options.addOption(new Option("c", "config", true, "config file"));
            return options;
        }
        
        private void loadProperties(File propFile) throws IOException {
            prop = new Properties();
            prop.load(new FileInputStream(propFile));
            for (Object ko : this.prop.keySet()) {
                String key = (String) ko;
                this.setParameter(key, this.prop.get(key).toString());
            }
        }
                
        @Override
        void parse(CommandLine cl) {
            super.parse(cl);
            if (cl.hasOption("c")) {
                File propFile = new File(cl.getOptionValue("c")); 
                try {
                    loadProperties(propFile);
                } catch (IOException e) {
                    throw new RuntimeException("Error loading prop file: " + propFile.getPath(), e);
                }
            }
        }          
    }
    
    /**
     * Remove a list of stations by their IDs or if none are
     * given then try to remove all stations.
     * 
     * Stations can only be removed after they are stopped.
     */
    static final class RemoveCommand extends ClientCommand {
                 
        RemoveCommand() {            
            super("remove", "Remove a station that is inactive", "[IDs]",
                    "Provide a list of IDs or none to remove all");
        }
                
        void parse(CommandLine cl) {
            super.parse(cl);
            readStationIDs(cl);
        }        
    }
    
    static final class CleanupCommand extends ClientCommand {
        
        CleanupCommand() {            
            super("cleanup", "Delete a station's working directory and files", "[IDs]",
                    "Provide a list of IDs or none to cleanup all");
        }
                
        void parse(CommandLine cl) {
            super.parse(cl);
            readStationIDs(cl);
        }
    }
    
    /**
     * Start a list of existing stations by their IDs or 
     * attempt to start all stations if no IDs are provided.
     */
    static final class StartCommand extends ClientCommand {
                
        StartCommand() {            
            super("start", "Start a station that is inactive", "[IDs]",
                    "Provide a list of IDs or none to start all inactive stations");
        }

        void parse(CommandLine cl) {
            super.parse(cl);
            readStationIDs(cl);
        }       
    }
    
    static final class SettingsCommand extends ClientCommand {
        
        SettingsCommand() {            
            super("settings", "Update or get server settings", "[options]",
                    "Updated settings will take effect only for newly created stations." + '\n' +
                    "Run with no arguments to get the current settings.");
        }
        
        Options getOptions() {
            options.addOption(new Option("s", "start", true, "starting station ID"));
            options.addOption(new Option("w", "workdir", true, "work dir (default is current dir where server is started)"));
            options.addOption(new Option("b", "basename", true, "station base name"));
            return options;
        }
        
        void parse(CommandLine cl) {
            super.parse(cl);
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
}
