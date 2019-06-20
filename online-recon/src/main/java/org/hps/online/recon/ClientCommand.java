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
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.json.JSONObject;

/**
 * Command to be sent to the online reconstruction server.
 */
abstract class ClientCommand {

    private final String name;
    private Map<String, Object> parameters = new HashMap<String, Object>();
    
    ClientCommand(String name) {
        this.name = name;
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
        Options options = new Options();
        return options;
    }
    
    /**
     * Process the command line options.
     * Sub-classes must override and define this method.
     * @param cl
     */
    abstract void process(CommandLine cl);
               
    /**
     * Start one or more new online reconstruction stations using
     * the current configuration properties from the server
     * (the "config" command can be used to change the properties).
     */
    static final class StartCommand extends ClientCommand {

        StartCommand() {
            super("start");
        }
                                        
        void setCount(Integer count) {
            this.setParameter("count", count.toString());
        }
        
        Options getOptions() {
            Options options = new Options();
            options.addOption(new Option("n", "number", true, "number of instances to start (default 1)")); 
            return options;
        }
                
        @Override
        void process(CommandLine cl) {
            if (cl.hasOption("n")) {
                setCount(Integer.valueOf(cl.getOptionValue("n")));
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
            super("stop");
        }
            
        void process(CommandLine cl) {
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
            super("list");
        }
                
        void process(CommandLine cl) {
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
            super("config");
        }
        
        @Override        
        Options getOptions() {
            Options options = new Options();
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
            super("remove");
        }
                
        void process(CommandLine cl) {
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
    
    static ClientCommand getCommand(String name) {
        if (name.equals("start")) {
            return new StartCommand();
        } else if (name.equals("stop")) {
            return new StopCommand();
        } else if (name.equals("list")) {
            return new ListCommand();
        } else if (name.equals("config")) {
            return new ConfigCommand();
        } else if (name.equals("remove")) {
            return new RemoveCommand();
        }
        return null;
    }
}
