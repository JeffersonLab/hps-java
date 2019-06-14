package org.hps.online.recon;

import java.io.BufferedReader;
import java.io.File;
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
public class Client {

    private static Logger LOGGER = Logger.getLogger(Client.class.getPackageName());
    
    private String hostname = "localhost";
    private int port = 22222;

    private File outputFile;
    
    private CommandLineParser parser = new DefaultParser();
            
    final void printUsage(Options options) {
        final HelpFormatter help = new HelpFormatter();
        help.printHelp("Client [command]", "Send commands to the HPS online recon server", options, "Commands: start, etc.");
    }

    void run(String args[]) {
        
        Options options = new Options();
        options.addOption(new Option("help", false, "print help"));
        options.addOption(new Option("p", "port", true, "server port"));
        options.addOption(new Option("h", "host", true, "server hostname"));
        options.addOption(new Option("o", "output", true, "output file (defaults writes server response to System.out)"));
        
        if (args.length == 0) {
            printUsage(options);
            System.exit(0);
        }
        
        CommandLine cl;
        try {
            cl = this.parser.parse(options, args, true);
        } catch (ParseException e) {
            throw new RuntimeException("Error parsing arguments.", e);
        }
                        
        if (cl.hasOption("p")) {
            this.port = Integer.parseInt(cl.getOptionValue("p"));
            LOGGER.config("Port set to <" + this.port + ">");
        }
        
        if (cl.hasOption("H")) {
            this.hostname = cl.getOptionValue("H");
            LOGGER.config("Hostname set to <" + this.hostname + ">");
        }
        
        if (cl.hasOption("o")) {
            this.outputFile = new File(cl.getOptionValue("o"));
            LOGGER.config("Output file set to <" + this.outputFile.getPath() + ">");
        }
        
        // Get command to execute and see if it is valid.
        final String commandName = cl.getArgs()[0];        
        ClientCommand command = ClientCommand.getCommand(commandName);
        if (command == null) {
            printUsage(options);
            throw new RuntimeException("Unknown client command <" + commandName + ">");
        }
        
        // Copy remaining arguments for the command.
        final String[] commandArgs = new String[cl.getArgs().length - 1];
        System.arraycopy(cl.getArgs(), 1, commandArgs, 0, commandArgs.length);
        
        // Parse command options.
        DefaultParser commandParser = new DefaultParser();
        CommandLine cmdResult = null;
        try {
            cmdResult = commandParser.parse(command.getOptions(), commandArgs);
        } catch (ParseException e) {
            // Print sub-command usage here.
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

            // Get server response.
            InputStream is = socket.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String resp = br.readLine();
            
            // Print server response or write to output file.
            PrintWriter pw = null;
            if (this.outputFile != null) {
                pw = new PrintWriter(this.outputFile);
            }
            if (resp.startsWith("{")) {
                // Handle JSON object.
                JSONObject jo = new JSONObject(resp);
                if (pw != null) {
                    pw.write(jo.toString(4));
                } else {
                    System.out.println(jo.toString(4));
                }
            } else if (resp.startsWith("[")) {
                // Handle JSON array.
                JSONArray ja = new JSONArray(resp);
                if (pw != null) {
                    pw.write(ja.toString(4));
                } else {
                    System.out.println(ja.toString(4));
                }
            } else {
                // Response from server isn't valid JSON.
                throw new RuntimeException("Invalid server response <" + resp.toString() + ">");
            }
            if (pw != null) {
                LOGGER.info("Wrote output to <" + this.outputFile.getPath() + ">");
                pw.flush();
                pw.close();
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
