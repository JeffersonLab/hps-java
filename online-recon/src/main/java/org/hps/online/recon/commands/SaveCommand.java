package org.hps.online.recon.commands;

import org.hps.online.recon.Command;

public class SaveCommand extends Command {

    public SaveCommand() {
        super("save", "Save the current set of plots to a ROOT or AIDA file", "[filename]",
                "Use the .root extension for ROOT output and .aida for a zipped AIDA file");
    }

    @Override
    protected void process() {
        if (cl.getArgList().size() > 0) {
            this.setParameter("filename", cl.getArgList().get(0));
        } else {
            throw new RuntimeException("Missing name of output file");
        }
    }
}
