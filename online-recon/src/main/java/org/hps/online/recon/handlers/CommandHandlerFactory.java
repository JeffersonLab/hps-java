package org.hps.online.recon.handlers;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hps.online.recon.CommandHandler;
import org.hps.online.recon.Server;
import org.reflections.Reflections;

public class CommandHandlerFactory {

    private Map<String, CommandHandler> handlers = new HashMap<String, CommandHandler>();

    public CommandHandlerFactory(Server server) {
        Reflections reflections = new Reflections(this.getClass().getPackage().getName());
        Set<Class<? extends CommandHandler>> handlerClasses = reflections.getSubTypesOf(CommandHandler.class);
        for (Class<? extends CommandHandler> handlerClass : handlerClasses) {
            try {
                CommandHandler handler =
                        handlerClass.getDeclaredConstructor(Server.class).newInstance(server);
                handlers.put(handler.getCommandName(), handler);
                server.getLogger().config("Added handler: " + handler.getCommandName()
                    + " -> " + handler.getClass().getCanonicalName());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public CommandHandler getCommandHandler(String cmd) {
        if (!handlers.containsKey(cmd)) {
            throw new IllegalArgumentException("No command handler exists for: " + cmd);
        }
        return handlers.get(cmd);
    }

}
