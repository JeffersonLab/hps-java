package org.hps.online.recon.commands;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.hps.online.recon.Command;

/**
 * Factory for creating instances of <code>Command</code> class.
 *
 * @author jeremym
 */
public class CommandFactory {

    /**
     * Map of command names to their classes.
     */
    private Map<String, Class<?>> classMap = 
            new LinkedHashMap<String, Class<?>>();
    
    /**
     * The set of valid commands.
     */
    private Set<String> commands;
    
    /**
     * Class constructor.
     * 
     * Defines the class map and the set of valid commands.
     */
    public CommandFactory() {
        
        // Add command classes.
        classMap.put("cleanup", CleanupCommand.class);
        classMap.put("config", ConfigCommand.class);
        classMap.put("create", CreateCommand.class);
        classMap.put("list", ListCommand.class);
        classMap.put("plot-add", PlotAddCommand.class);
        classMap.put("plot-stop", PlotStopCommand.class);
        classMap.put("remove", RemoveCommand.class);
        classMap.put("settings", SettingsCommand.class);
        classMap.put("start", StartCommand.class);
        classMap.put("stop", StopCommand.class);
        classMap.put("status", StatusCommand.class);
        classMap.put("log", LogCommand.class);
        
        // Set of valid commands.
        commands = classMap.keySet();
    }   
    
    /**
     * Get the set of valid commands.
     * @return The set of valid commands
     */
    public Set<String> getCommandNames() {
        return Collections.unmodifiableSet(commands);
    }
    
    /**
     * Create a command by name.
     * @param name The name of the command
     * @return The command or null if does not exist
     */
    public Command create(String name) {
        Command cmd = null;
        Class<?> klass = this.classMap.get(name);
        if (klass != null) {
            try {
                cmd = (Command) klass.newInstance();
            } catch (Exception  e) {
                throw new RuntimeException("Error creating class", e);
            }
        }
        return cmd;
    }
    
    /**
     * Check if a command name is valid.
     * @param name The name of the command
     * @return True if name is a valid command that can be created by the factory
     */
    public boolean has(String name) {
        return this.commands.contains(name);
    }    
}
