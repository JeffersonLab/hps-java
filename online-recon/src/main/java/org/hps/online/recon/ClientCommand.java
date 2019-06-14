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
     * Kill an online reconstruction process.
     */
    static class StopCommand extends ClientCommand {
               
        StopCommand() {
            super("stop");
        }
        
        @Override        
        Options getOptions() {
            Options options = new Options();
            options.addOption(new Option("i", "id", true, "id of process (no ID means all processes)"));
            return options;
        }
        
        void setID(Integer id) {
            this.setParameter("id", id.toString());
        }

        @Override
        void process(CommandLine cl) {
            if (cl.hasOption("i")) {
                setID(Integer.valueOf(cl.getOptionValue("i")));
            }            
        }         
    }
    
    static class ListCommand extends ClientCommand {

        ListCommand() {
            super("list");
        }
        
        @Override        
        Options getOptions() {
            Options options = new Options();
            options.addOption(new Option("i", "id", true, "id of process (no ID means all processes)"));
            return options;
        }
        
        void setID(Integer id) {
            this.setParameter("id", id.toString());
        }

        @Override
        void process(CommandLine cl) {
            if (cl.hasOption("i")) {
                setID(Integer.valueOf(cl.getOptionValue("i")));
            }            
        }          
    }
    
    static ClientCommand getCommand(String name) {
        if (name.equals("start")) {
            return new StartCommand();
        } else if (name.equals("stop")) {
            return new StopCommand();
        } else if (name.equals("list")) {
            return new ListCommand();
        }
        return null;
    }
}
