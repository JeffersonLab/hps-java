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

    /**
     * Short name of the command.
     */
    private final String name;
    
    /**
     * Description of the command for printing usage.
     */
    private final String description;
    
    /**
     * Extra options string for printing usage.
     */
    private final String commandExtra;
    
    /**
     * Footer text for printing usage.
     */
    private final String commandFooter;
    
    /**
     * Parameter map from parsing command line options.
     * This will be sent to the server as a JSON object.
     */
    private Map<String, Object> parameters = new HashMap<String, Object>();    
    
    /**
     * The command line options for this command.
     */
    protected Options options = new Options();
            
    /**
     * Define a new client command
     * @param name The name of the command
     * @param description The description of the command
     * @param commandExtra The extra command options for printing usage
     * @param commandFooter The footer text for printing usage
     */
    ClientCommand(String name, String description, String commandExtra, String commandFooter) {
        this.name = name;
        this.description = description;
        this.commandExtra = commandExtra;
        this.commandFooter = commandFooter;
        
        options.addOption(new Option("", "help", false, "print command help"));
    }

    /**
     * Get the name of the command.
     * @return The name of the command
     */
    String getName() {
        return this.name;
    }
    
    /**
     * Get the description of the command.
     * @return The description of the command
     */
    String getDescription() {
        return this.description;
    }

    /**
     * Set a parameter for the command.
     * @param name The name of the parameter
     * @param value The value of the parameter
     */
    void setParameter(String name, Object value) {
        parameters.put(name, value);
    }
    
    /**
     * Convert the object to JSON.
     * @return A JSON string with this object's data
     */
    JSONObject toJSON() {
        JSONObject jo = new JSONObject();
        jo.put("command", this.name);
        jo.put("parameters", new JSONObject(parameters));
        return jo;
    }
    
    /**
     * Convert the object to a string, by default in JSON format.
     */
    public String toString() {
        return toJSON().toString();
    }
    
    /**
     * Get the options for command line usage.
     * @return The options for command line usage
     */
    Options getOptions() {
        return options;
    }
    
    /**
     * Print the usage statement for this command.
     */
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
     * @param cl The parsed command line
     */
    void process(CommandLine cl) {
        if (cl.hasOption("help")) {
            printUsage();
            System.exit(0);
        }
    }
    
    /**
     * Reset command parameters.
     */
    void reset() {
        this.parameters.clear();
    }
    
    /**
     * Read a station ID list from extra command line arguments and add as a parameter.
     * @param cl The parsed command line
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
     * of the server.  
     */
    static final class CreateCommand extends ClientCommand {

        CreateCommand() {
            super("create", "Create a new station", "", 
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
        
        StopCommand() {            
            super("stop", "Stop a station", "[IDs]", 
                    "Provide a list of IDs or none for all");
        }
            
        void process(CommandLine cl) {
            super.process(cl);
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
                
        void process(CommandLine cl) {
            super.process(cl);
            readStationIDs(cl);
        }       
    }
    
    /**
     * Set server configuration properties from a local file.
     * 
     * Running this command with no arguments returns the current configuration.
     */
    static final class ConfigCommand extends ClientCommand {

        private Properties prop;
                
        ConfigCommand() {
            super("config", "Set new server configuration properties", "",
                    "Configuration will take effect for newly created stations."
                    + " If no new config is provided the existing config will be printed.");
        }
                
        /**
         * Load properties file into command parameters.
         * @param propFile The properties file
         * @throws IOException If there is an error loading the properties
         */
        private void loadProperties(File propFile) throws IOException {
            if (!propFile.exists()) {
                throw new IllegalArgumentException("Prop file does not exist: " + propFile.getPath());
            }
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
            //System.out.println("config arg list: " + cl.getArgList());
            if (cl.getArgList().size() > 0) {                
                File propFile = new File(cl.getArgList().get(0));
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
     * This will only work on inactive stations.
     */
    static final class RemoveCommand extends ClientCommand {
                 
        RemoveCommand() {            
            super("remove", "Remove a station that is inactive", "[IDs]",
                    "Provide a list of IDs or none to remove all");
        }
                
        void process(CommandLine cl) {
            super.process(cl);
            readStationIDs(cl);
        }        
    }
    
    /**
     * Cleanup a station by deleting its working directory.
     * 
     * This will only work on inactive stations.
     */
    static final class CleanupCommand extends ClientCommand {
        
        CleanupCommand() {            
            super("cleanup", "Delete a station's working directory and files", "[IDs]",
                    "Provide a list of IDs or none to cleanup all");
        }
                
        void process(CommandLine cl) {
            super.process(cl);
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

        void process(CommandLine cl) {
            super.process(cl);
            readStationIDs(cl);
        }       
    }
    
    /**
     * Update values of server settings.
     * 
     * The new settings will only take effect for new stations.
     * 
     * Running this command with no arguments returns the current settings.
     */
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
        
        void process(CommandLine cl) {
            super.process(cl);
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
    
    /**
     * Show status summary of online reconstruction server and ET system.
     */
    static final class StatusCommand extends ClientCommand {
               
        StatusCommand() {
            super("status", "Show server and station status", "", "");
        }
        
        Options getOptions() {
            options.addOption(new Option("v", "verbose", false, "show verbose station info"));
            return options;
        }
        
        void setVerbose(boolean verbose) {
            setParameter("verbose", verbose);
        }
        
        void process(CommandLine cl) {
            super.process(cl);
            if (cl.hasOption("v")) {
                setVerbose(true);
            } else {
                setVerbose(false);
            }
        }
    }    
}
 