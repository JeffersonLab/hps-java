package org.hps.online.recon;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.hps.online.recon.commands.CommandFactory;

/**
 * Interactive console for online reconstruction client.
 * 
 * @author jeremym
 */
public class Console {

    /**
     * Reference to the client object.
     */
    private final Client client;

    /**
     * True to echo commands as they are executed.
     */
    private boolean echo = false;
    
    /**
     * Factory class for creating <code>Command</code> objects.
     */
    private CommandFactory cf = new CommandFactory();
    
    /**
     * Class constructor.
     * @param client The reference to the client object
     */
    Console(Client client) {
        this.client = client;
    }
    
    /**
     * Set whether to echo commands back to the terminal.
     * @param echo True to echo commands back to the terminal
     */
    void setEcho(boolean echo) {
        this.echo = echo;
    }
    
    /**
     * Run the console, accepting and executing user input.
     */
    void run() {
        String userInput;
        
        Scanner sn = new Scanner(System.in);

        System.out.println("HPS Online Reconstruction");
        System.out.println("Type 'help' or 'help [command]' for more information or 'exit' to quit.");
        
        while (true) {
            System.out.print("online> ");
            userInput = sn.nextLine().trim();
            boolean exit = false;
            try {
                exit = exec(userInput);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (exit) {
                break;
            }

        }
        
        sn.close();
    }
    
    /**
     * Execute a line of input.
     * @param userInput The user input to execute
     * @return True if console should exit after this command
     */
    private boolean exec(String userInput) {
        if (echo) {
            System.out.println(userInput);
        }
        if (userInput.length() > 0) {
            // Ignore comments.
            if (!userInput.startsWith("#")) {
                String rawInputArr[] = userInput.split(" ");
                String cmdStr = rawInputArr[0];
                List<String> args = new ArrayList<String>(Arrays.asList(rawInputArr));
                if (args.size() > 0) {
                    args.remove(0);
                }

                if (cmdStr.equals("exit")) {
                    return true;
                } else if (cmdStr.equals("help")) {
                    if (args.size() == 0) {
                        printHelp();
                    } else {                        
                        printCommandHelp(args.get(0));
                    }
                } else if (cmdStr.equals("port")) {
                    if (args.size() == 0) {
                        System.out.println(client.getPort());
                    } else {
                        try {
                            Integer port = Integer.parseInt(args.get(0));
                            client.setPort(port);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else if (cmdStr.equals("host")) {
                    if (args.size() == 0) {
                        System.out.println(client.getHostname());
                    } else {
                        String hostname = args.get(0);
                        client.setHostname(hostname);
                    }
                } else if (cmdStr.equals("file")) {
                    if (args.size() == 0) {
                        if (client.getOutputFile() != null) {
                            System.out.println(client.getOutputFile().toPath());
                        } else {
                            System.out.println("Output is being written to the terminal.");
                        }
                    } else {
                        String filename = args.get(0);                        
                        client.setOutputFile(filename);                        
                    }
                } else if (cmdStr.equals("terminal")) {
                    if (client.getOutputFile() != null) {
                        client.setOutputFile(null);
                        System.out.println("Redirected output back to the terminal.");
                    }
                } else if (cmdStr.equals("append")) {
                    if (args.size() > 0) {
                        client.setAppend(Boolean.parseBoolean(args.get(0)));
                    } else {
                        System.out.println(client.getAppend());
                    }
                } else {
                    if (cf.has(cmdStr)) {
                        Command cmd = cf.create(cmdStr);
                        DefaultParser parser = new DefaultParser();
                        try {
                            String cmdArr[] = args.toArray(new String[0]);
                            CommandLine cl = parser.parse(cmd.getOptionsNoHelp(), cmdArr);
                            try {
                                cmd.process(cl);
                                client.send(cmd);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    } else {
                        System.err.println("Unknown client commnd: " + cmdStr);
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Execute a file containing commands.
     * @param file The path to the file
     * @throws FileNotFoundException If the file does not exist
     * @throws IOException If there is a problem reading the file
     */
    void execFile(File file) throws FileNotFoundException, IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                this.exec(line);
            }
        }
    }
    
    /**
     * Print help for the interactive console.
     */
    void printHelp() {
        System.out.println('\n' + "GENERAL" + '\n');
        System.out.println("    help - print general information");
        System.out.println("    help [cmd] - print information for specific command");
        System.out.println("    exit - quit the console");
        System.out.println('\n' + "SETTINGS" + '\n');
        System.out.println("    port [port] - set the server port");
        System.out.println("    host [host]- set the server hostname");
        System.out.println("    file [filename] - write server output to a file");
        System.out.println("    append [true|false] - true to append to output file or false to overwrite");
        System.out.println("    terminal - redirect server output back to the terminal");
        System.out.println('\n' + "COMMANDS" + '\n');
        for (String command : cf.getCommandNames()) {
            System.out.println("    " + command + " - " + cf.create(command).getDescription());
        }
    }
    
    /**
     * Print the help for a specific command.
     * @param command The name of the command to print
     */
    void printCommandHelp(String command) {
        if (cf.has(command)) {
            cf.create(command).printUsageNoHelp();
        } else {
            System.err.println("Unknown command: " + command);
        }
    }
}
