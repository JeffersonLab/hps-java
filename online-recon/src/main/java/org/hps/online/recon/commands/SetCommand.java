package org.hps.online.recon.commands;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.ParseException;
import org.hps.online.recon.Command;

/**
 * Set a configuration property
 */
public final class SetCommand extends Command {

    public SetCommand() {
        super("set", "Set a configuration property", "[name] [value]", "");
    }

    protected void process() {
        List<String> argList = Arrays.asList(this.rawArgs);
        if (argList.size() < 2) {
            throw new RuntimeException("Name and value of property are both required!");
        }
        String name = argList.get(0);
        String value = String.join(" ", argList.subList(1, argList.size()));
        //System.out.println("value="+value);
        setParameter("name", name);
        setParameter("value", value);
    }

    protected void parse(String[] args) throws ParseException {
        this.rawArgs = args;

        // Don't parse options so we can easily allow characters values that would confuse the CL parser like dashes.
    }
}
