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
     * Process the command line options.
     * 
     * This method checks for the help option, and if it exists,
     * prints usage and exits the program.
     * 
     * Sub-classes should always call the super method when overriding
     * for usage to be printed properly.
     * 
     * @param cl
     */
    void process(CommandLine cl) {
        if (cl.hasOption("help")) {
            printUsage();
            System.exit(0);
        }
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
        void process(CommandLine cl) {
            super.process(cl);
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

        private List<Integer> ids = new ArrayList<Integer>();
        
        StopCommand() {            
            super("stop", "Stop a station", "[IDs]", 
                    "Provide a list of IDs or none for all");
        }
            
        void process(CommandLine cl) {
            super.process(cl);
            for (String arg : cl.getArgList()) {
                ids.add(Integer.parseInt(arg));
            }
        }
        
        public JSONObject toJSON() {
            JSONObject jo = super.toJSON();
            JSONObject params = jo.getJSONObject("parameters");
            params.put("ids", this.ids);
            return jo;
        }
    }
    
    /**
     * Get a list of station info as JSON from a list of IDs or
     * if none are given then return info for all stations.
     */
    static final class ListCommand extends ClientCommand {

        private List<Integer> ids = new ArrayList<Integer>();
        
        ListCommand() {
            super("list", "List station information in JSON format", "[IDs]",
                    "Provide a list of IDs or none to list information for all");
        }
                
        void process(CommandLine cl) {
            super.process(cl);
            for (String arg : cl.getArgList()) {
                ids.add(Integer.parseInt(arg));
            }
        }
        
        public JSONObject toJSON() {
            JSONObject jo = super.toJSON();
            JSONObject params = jo.getJSONObject("parameters");
            params.put("ids", this.ids);
            return jo;
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
        void process(CommandLine cl) {
            super.process(cl);
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
        
        private List<Integer> ids = new ArrayList<Integer>();
        
        RemoveCommand() {            
            super("remove", "Remove a station that is inactive", "[IDs]",
                    "Provide a list of IDs or none to remove all");
        }
                
        void process(CommandLine cl) {
            super.process(cl);
            for (String arg : cl.getArgList()) {
                ids.add(Integer.parseInt(arg));
            }
        }
        
        public JSONObject toJSON() {
            JSONObject jo = super.toJSON();
            JSONObject params = jo.getJSONObject("parameters");
            params.put("ids", this.ids);
            return jo;
        }        
    }
    
    /**
     * Start a list of existing stations by their IDs or 
     * attempt to start all stations if no IDs are provided.
     */
    static final class StartCommand extends ClientCommand {
        
        private List<Integer> ids = new ArrayList<Integer>();
        
        StartCommand() {            
            super("start", "Start a station that is inactive", "[IDs]",
                    "Provide a list of IDs or none to start all inactive stations");
        }
                
        void process(CommandLine cl) {
            super.process(cl);
            for (String arg : cl.getArgList()) {
                ids.add(Integer.parseInt(arg));
            }
        }
        
        public JSONObject toJSON() {
            JSONObject jo = super.toJSON();
            JSONObject params = jo.getJSONObject("parameters");
            params.put("ids", this.ids);
            return jo;
        }        
    }
    
}
