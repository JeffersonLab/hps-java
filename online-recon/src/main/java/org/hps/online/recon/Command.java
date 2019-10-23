package org.hps.online.recon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.json.JSONObject;

/**
 * Command to be sent to the online reconstruction server.
 * 
 * Commands are sent as JSON containing the name of the command
 * and a parameter map.
 */
public abstract class Command {

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
    public Command(String name, String description, String commandExtra, String commandFooter) {
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
    protected void setParameter(String name, Object value) {
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
     * 
     * Sub-classes should override this to add their options.
     * 
     * @return The options for command line usage
     */
    protected Options getOptions() {
        return options;
    }
    
    /**
     * Return options of the command with no "help" option.
     * @return The options without the "help" option
     */
    protected Options getOptionsNoHelp() {
        Options noHelpOptions = new Options();
        for (Option option : getOptions().getOptions()) {
            if (!option.getLongOpt().equals("help")) {
                noHelpOptions.addOption(option);
            }
        }
        return noHelpOptions;
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
     * Print the usage statement for this command without the "help" option.
     */
    void printUsageNoHelp() {
        final HelpFormatter help = new HelpFormatter();
        help.printHelp(this.name + " " + this.commandExtra, this.description, 
                getOptionsNoHelp(), this.commandFooter);
    }
    
    /**
     * Parse command line options.
     * 
     * Sub-classes need to implement this method to handle their arguments.
     * 
     * @param cl The parsed command line
     */
    abstract protected void process(CommandLine cl);
        
    /**
     * Read a station ID list from extra command line arguments and add as a parameter.
     * @param cl The parsed command line
     */
    protected void readStationIDs(CommandLine cl) {
        List<Integer> ids = new ArrayList<Integer>();
        for (String arg : cl.getArgList()) {
            ids.add(Integer.parseInt(arg));
        }
        this.setParameter("ids", ids);
    }
}
 