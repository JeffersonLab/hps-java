package org.hps.online.recon;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.json.JSONObject;

/**
 * Command to be sent to the online reconstruction server.
 */
public abstract class ClientCommand {

    private final String name;
    private Map<String, String> parameters = new HashMap<String, String>();
    
    ClientCommand(String name) {
        this.name = name;
    }
    
    String getName() {
        return this.name;
    }

    void setParameter(String name, String value) {
        parameters.put(name, value);
    }
    
    public JSONObject toJSON() {
        JSONObject jo = new JSONObject();
        jo.put("command", this.name);
        jo.put("parameters", new JSONObject(parameters));
        return jo;
    }
    
    public String toString() {
        return toJSON().toString();
    }
    
    abstract Options getOptions();
    
    abstract void process(CommandLine cl);
           
    /**
     * Start a new reconstruction process with given configuration properties.
     */
    static class StartCommand extends ClientCommand {

        StartCommand() {
            super("start");
        }
                                
        void setProperties(String properties) {
            this.setParameter("properties", properties);
        }
        Options getOptions() {
            Options options = new Options();
            options.addOption(new Option("c", "config", true, "configuration properties file"));
            return options;
        }
                
        @Override
        void process(CommandLine cl) {
            setProperties(cl.getOptionValue("c"));
        }
    }    
    
    static ClientCommand getCommand(String name) {
        if (name.equals("start")) {
            return new StartCommand();
        }
        return null;
    }
}
