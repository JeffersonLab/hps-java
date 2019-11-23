package org.hps.online.recon.commands;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.hps.online.recon.Command;

/**
 * Set server configuration properties from a local file.
 * 
 * Running this command with no arguments returns the current configuration.
 */
public class ConfigCommand extends Command {

    private Properties prop;
            
    ConfigCommand() {
        super("config", "Set new server configuration properties", "[config.properties]",
                "Configuration will take effect for newly created stations."
                + " If no new config is provided the existing config will be printed.");
    }
            
    /**
     * Load properties file into command parameters.
     * @param propFile The properties file
     * @throws IOException If there is an error loading the properties
     */
    private void loadProperties(File propFile) throws IOException {
        if (!propFile.exists()) {
            throw new IllegalArgumentException("Prop file does not exist: " + propFile.getPath());
        }
        prop = new Properties();
        prop.load(new FileInputStream(propFile));
        for (Object ko : this.prop.keySet()) {
            String key = (String) ko;
            this.setParameter(key, this.prop.get(key).toString());
        }
    }
            
    @Override
    protected void process(CommandLine cl) {
        if (cl.getArgList().size() > 0) {                
            File propFile = new File(cl.getArgList().get(0));
            try {
                loadProperties(propFile);
            } catch (IOException e) {
                throw new RuntimeException("Error loading prop file: " + propFile.getPath(), e);
            }
        }
    }          
}
