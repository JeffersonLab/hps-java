package org.hps.online.recon;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Client for interacting with the online reconstruction server.
 */
// TODO: add switch for setting file for writing result output
public class Client {

    private static Logger LOGGER = Logger.getLogger(Client.class.getPackageName());
    
    private String hostname = "localhost";
    private int port = 22222;
        
    private static Options OPTIONS = new Options();
    private CommandLineParser parser = new DefaultParser();
        
    static {
        OPTIONS.addOption(new Option("h", "help", false, "print help"));
        OPTIONS.addOption(new Option("p", "port", true, "server port"));
        OPTIONS.addOption(new Option("H", "host", true, "server hostname"));
    }
    
    final void printUsage() {
        final HelpFormatter help = new HelpFormatter();
        help.printHelp("Client [command]", "Send commands to the HPS online recon server", OPTIONS, "Commands: start, etc.");
    }

    void run(String args[]) {
        
        if (args.length == 0) {
            printUsage();
            System.exit(0);
        }
        
        CommandLine commandLine;
        try {
            commandLine = this.parser.parse(OPTIONS, args, true);
        } catch (ParseException e) {
            throw new RuntimeException("Error parsing arguments.", e);
        }
                        
        if (commandLine.hasOption("p")) {
            this.port = Integer.parseInt(commandLine.getOptionValue("p"));
            LOGGER.config("Port set to <" + this.port + ">");
        }
        
        if (commandLine.hasOption("H")) {
            this.hostname = commandLine.getOptionValue("H");
            LOGGER.config("Hostname set to <" + this.hostname + ">");
        }
        
        // Get command to execute and see if it is valid.
        final String commandName = commandLine.getArgs()[0];        
        ClientCommand command = ClientCommand.getCommand(commandName);
        if (command == null) {
            printUsage();
            throw new RuntimeException("Unknown command <" + commandName + ">");
        }
        
        // Copy remaining arguments for the command.
        final String[] commandArgs = new String[commandLine.getArgs().length - 1];
        System.arraycopy(commandLine.getArgs(), 1, commandArgs, 0, commandArgs.length);
        
        // Parse command options.
        DefaultParser commandParser = new DefaultParser();
        CommandLine cmdResult = null;
        try {
            cmdResult = commandParser.parse(command.getOptions(), commandArgs);
        } catch (ParseException e) {
            throw new RuntimeException("Error parsing command options", e);
        }        
        command.process(cmdResult);
        
        // Send command to server.
        LOGGER.info("Sending command " + command.toString());
        send(command);
    }
    
    /**
     * Send a command to the online reconstruction server.
     * @param command
     */
    private void send(ClientCommand command) {
        try (Socket socket = new Socket(hostname, port)) {
            // Send command to the server.           
            PrintWriter writer = new PrintWriter(socket.getOutputStream());
            writer.write(command.toString() + '\n');
            writer.flush();

            // Print server response.
            InputStream is = socket.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String resp = br.readLine();
            
            // TODO: add option to redirect response to a file
            if (resp.startsWith("{")) {
                JSONObject jo = new JSONObject(resp); 
                System.out.println(jo.toString(4));
            } else if (resp.startsWith("[")) {
                JSONArray ja = new JSONArray(resp);
                System.out.println(ja.toString(4));
            }

        } catch (Exception e) {
            throw new RuntimeException("Client error", e);
        }
    }
    
    public static void main(String[] args) {
        Client client = new Client();
        client.run(args);
    }
}
