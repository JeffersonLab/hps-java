package org.hps.online.recon.commands;

import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.hps.online.recon.Command;

/**
 * Set a configuration property
 */
public final class SetCommand extends Command {

    SetCommand() {
        super("set", "Set a configuration property", "[name] [value]", "");
    }

    protected void process(CommandLine cl) {
        List<String> argList = cl.getArgList();
        if (argList.size() < 2) {
            throw new RuntimeException("Name and value of property are both required!");
        }
        String name = argList.get(0);
        argList.remove(0);
        String value = String.join(" ", argList);
        setParameter("name", name);
        setParameter("value", value);
    }
}
