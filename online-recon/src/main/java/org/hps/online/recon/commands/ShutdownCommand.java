package org.hps.online.recon.commands;

import org.hps.online.recon.Command;

public class ShutdownCommand extends Command {

    public ShutdownCommand() {
        super("shutdown", "Shutdown the server", "", "");
    }

    @Override
    protected void process() {
        if (cl.getArgList().size() > 0) {
            this.setParameter("wait", Integer.valueOf(cl.getArgList().get(0)));
        } else {
            this.setParameter("wait", 0);
        }
    }
}
