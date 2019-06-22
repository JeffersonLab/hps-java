package org.hps.online.recon;

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

    private final Client client;
    
    private CommandFactory cf = new CommandFactory();
    
    Console(Client client) {
        this.client = client;
    }
         
    public void run() {
        String userInput;    
        
        Scanner sn = new Scanner(System.in);

        System.out.println("HPS Online Reconstruction");
        System.out.println("Type 'help' or 'help [command]' for more information or 'exit' to quit.");
        
        while (true) {
            System.out.print("online> ");
            userInput = sn.nextLine();
            userInput = userInput.trim();
            //System.out.println("userInput: " + userInput);
            if (userInput.length() > 0) {
                String rawInputArr[] = userInput.split(" ");
                String cmdStr = rawInputArr[0];
                List<String> args = new ArrayList<String>(Arrays.asList(rawInputArr));
                //System.out.println("args: " + args);
                if (args.size() > 0) {
                    args.remove(0);
                }
                //System.out.println("cmd args: " + args);
                if (cmdStr.equals("exit")) {
                    break;
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
                            CommandLine cl = parser.parse(cmd.getOptions(), cmdArr);
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
        
        sn.close();
    }
    
    void printHelp() {
        System.out.println('\n' + "GENERAL" + '\n');
        System.out.println("    help - print general information");
        System.out.println("    help [cmd] - print information for specific command");
        System.out.println("    exit - quit the console");
        System.out.println('\n' + "SETTINGS" + '\n');
        System.out.println("    port [port] - set the server port");
        System.out.println("    host [host]- set the server hostname");
        System.out.println("    file [filename] - write server output to a file");
        System.out.println("    terminal - redirect server output back to the terminal");
        System.out.println("    append [true|false] - true to append to output file or false to overwrite");
        System.out.println('\n' + "COMMANDS" + '\n');
        for (String command : cf.getCommands()) {
            System.out.println("    " + command + " - " + cf.create(command).getDescription());
        }
    }
    
    void printCommandHelp(String command) {
        if (cf.has(command)) {
            cf.create(command).printUsage();        
        } else {
            System.err.println("Unknown command: " + command);
        }
    }
}
