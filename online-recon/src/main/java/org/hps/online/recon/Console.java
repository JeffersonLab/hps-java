package org.hps.online.recon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;

/**
 * Interactive console for online reconstruction client.
 * 
 * @author jeremym
 */
public class Console {

    private final Client client;
    private final Set<String> commands;
    private final Map<String, ClientCommand> commandMap;
    
    Console(Client client) {
        this.client = client;
        this.commands = client.getCommands();
        this.commandMap = client.getCommandMap();
    }
    
    public void run() {
        String userInput;    
        
        Scanner sn = new Scanner(System.in);
        
        while (true) {
            System.out.println("Type 'help' or 'help [command]' for more information or 'exit' to quit.");
            System.out.print("online> ");
            userInput = sn.next().trim();
            if (userInput.length() > 0) {
                String rawInputArr[] = userInput.split(" ");
                String cmdStr = rawInputArr[0];
                //System.out.println("cmd: " + cmdStr);
                List<String> args = new ArrayList<String>(Arrays.asList(rawInputArr));
                if (args.size() > 0) {
                    args.remove(0);
                }
                if (cmdStr.equals("exit")) {
                    break;
                } else if (cmdStr.equals("help")) {
                    if (args.size() == 0) {
                        printHelp();
                    } else {
                        printCommandHelp(cmdStr);
                    }
                } else if (cmdStr.equals("port")) {
                    if (args.size() > 0) {
                        System.out.println(client.getPort());
                    } else {
                        Integer port = Integer.parseInt(args.get(0));
                        client.setPort(port);
                    }
                } else if (cmdStr.equals("host")) {
                    if (rawInputArr.length < 2) {
                        System.out.println(client.getHostname());
                    } else {
                        String hostname = rawInputArr[1];
                        client.setHostname(hostname);
                    }
                } else {
                    if (this.commands.contains(cmdStr)) {
                        ClientCommand cmd = this.commandMap.get(cmdStr);
                        DefaultParser parser = new DefaultParser();
                        try {
                            String cmdArr[] = args.toArray(new String[0]);
                            //System.out.println("args: " + Arrays.asList(cmdArr));
                            CommandLine cl = parser.parse(cmd.getOptions(), cmdArr);
                            cmd.process(cl);
                            client.send(cmd);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    } else {
                        System.err.println("Unknown commnd: " + cmdStr);
                    }
                }
            }
        }
        
        sn.close();
    }
    
    void printHelp() {
        System.out.println('\n' + "SETTINGS" + '\n');
        System.out.println("    port - set the server port");
        System.out.println("    host - set the server hostname" + '\n');        
        System.out.println("COMMANDS");
        for (Entry<String, ClientCommand> entry : this.commandMap.entrySet()) {
            System.out.println("    " + entry.getKey() + " - " + entry.getValue().getDescription());
        }        
    }
    
    void printCommandHelp(String command) {
        if (this.commands.contains(command)) {
            this.commandMap.get(command).printUsage();        
        } else {
            System.err.println("Unknown command: " + command);
        }
    }
}
