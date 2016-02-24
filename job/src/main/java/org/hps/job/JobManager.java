package org.hps.job;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.lcsim.job.JobControlManager;

/**
 * Extension of standard LCSim job manager.
 * <p>
 * Provides setup of database conditions system and adds option to provide conditions system tags.
 *
 * @author Jeremy McCormick, SLAC
 */
public final class JobManager extends JobControlManager {
    
    /**
     * Run the job manager from the command line.
     *
     * @param args the command line arguments
     */
    public static void main(final String args[]) {
        // Run the job.
        final JobManager job = new JobManager();
        job.parse(args);
        job.run();
    }

    /**
     * Class constructor.
     */
    public JobManager() {
        conditionsSetup = new DatabaseConditionsManagerSetup();
    }
    
    /**
     * Get the conditions setup.
     * @return the conditions setup
     */
    public DatabaseConditionsManagerSetup getDatabaseConditionsManagerSetup() {
        return (DatabaseConditionsManagerSetup) this.conditionsSetup;
    }
    
    /**
     * Override creation of command line options.
     * @return the overridden command line options
     */
    @Override
    protected Options createCommandLineOptions() {
        Options options = super.createCommandLineOptions();
        options.addOption("t", "tag", true, "conditions system tag (can be used multiple times)");
        return options;
    }
    
    /**
     * Override command line parsing.
     * @return the overridden, parsed command line
     */
    @Override
    public CommandLine parse(final String args[]) {
        CommandLine commandLine = super.parse(args);
        if (commandLine.hasOption("t")) {
            Set<String> tags = new HashSet<String>();
            for (String tag : commandLine.getOptionValues("t")) {
                tags.add(tag);
            }
            getDatabaseConditionsManagerSetup().setTags(tags);
        }
        return commandLine;
    }
}
