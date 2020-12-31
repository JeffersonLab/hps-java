package org.hps.online.recon.commands;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.hps.online.recon.Command;
import org.reflections.Reflections;

/**
 * Factory for creating instances of <code>Command</code> class.
 *
 * @author jeremym
 */
public class CommandFactory {

    /**
     * Map of command names to their classes.
     */
    private Map<String, Class<? extends Command>> commands =
            new LinkedHashMap<String, Class<? extends Command>>();

    /**
     * Class constructor which registers all commands using reflection.
     */
    public CommandFactory() {

        Reflections reflections = new Reflections(this.getClass().getPackage().getName());
        Set<Class<? extends Command>> classes = reflections.getSubTypesOf(Command.class);
        for (Class<? extends Command> klass : classes) {
            try {
                commands.put(klass.getConstructor().newInstance().getName(), klass);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Create a command by name.
     * @param name The name of the command
     * @return The command with the given name
     * @throw IllegalArgumentException If the command does not exist
     * @throws RuntimeException If there is a problem creating the command
     */
    public Command create(String name) {
        if (!commands.containsKey(name)) {
            throw new IllegalArgumentException("Unknown command name: " + name);
        }
        try {
            return commands.get(name).getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Error creating command: " + name, e);
        }
    }

    /**
     * Get the list of command names
     * @return The list of command names
     */
    public Set<String> getCommandNames() {
        return commands.keySet();
    }
}
