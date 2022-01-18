package pt.rd.udpviewmulticast.shell;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.shell.jline3.PicocliCommands;
import pt.rd.udpviewmulticast.Main;
import pt.rd.udpviewmulticast.benchmark.NetworkDegrader;
import pt.rd.udpviewmulticast.communication.packets.PacketHello;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * Top-level command that just prints help.
 */
@Command(
        name = "",
        description = {"Interact with the application"},
        footer = {"", "Press Ctrl-D to exit."},
        subcommands = {ShellCommands.Network.class, PicocliCommands.ClearScreen.class, CommandLine.HelpCommand.class}
)
public class ShellCommands implements Runnable {

    public void run() {
        System.out.println(new CommandLine(this).getUsageMessage());
    }

    /*

     */

    @Command(mixinStandardHelpOptions = true, subcommands = {CommandLine.HelpCommand.class},
            description = "Shows the current node ID")
    public void id() {
        System.out.println(Main.ID.toString());
    }

    @Command(mixinStandardHelpOptions = true, subcommands = {CommandLine.HelpCommand.class},
            description = "Shows the current node IP")
    public void ip() {
        try {
            System.out.println(InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    @Command(mixinStandardHelpOptions = true, subcommands = {CommandLine.HelpCommand.class},
            description = "Pings an IP")
    public void ping(@CommandLine.Parameters(paramLabel = "IP", description = "The IP to ping.") String ip) {
        try {

            long start = System.currentTimeMillis();

            System.out.print("pinging... ");
            boolean reachable = InetAddress.getByName(ip).isReachable(5000);
            System.out.println(reachable ? String.format("success (%d ms)", System.currentTimeMillis() - start) : "failed");

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    @Command(mixinStandardHelpOptions = true, subcommands = {CommandLine.HelpCommand.class},
            description = "Sends an hello packet")
    public void hello(@CommandLine.Parameters(paramLabel = "NUM", description = "The NUM to send in the packet.") int num) {
        PacketHello packet = new PacketHello(num, "Hello World!");
        Main.CHANNEL.send(packet);
    }

    @Command(name = "net", mixinStandardHelpOptions = true, subcommands = {CommandLine.HelpCommand.class},
            description = "Add and remove network degradations")
    static class Network implements Runnable {

        @CommandLine.Option(names = {"-d", "--device"}, paramLabel = "DEVICE", description = "the network device", defaultValue = "eth0")
        String device;

        public void run() {
            System.out.println(new CommandLine(this).getUsageMessage());
        }

        @Command(mixinStandardHelpOptions = true, subcommands = {CommandLine.HelpCommand.class},
                description = "Clears the network rules")
        public void clear() {
            try {
                System.out.print(String.format("Clearing rules (%s): ", device));

                int result = NetworkDegrader.clearRules(device);
                if (result == 0) {
                    System.out.println("success");
                } else {
                    System.out.println(String.format("error (%d)", result));
                }
            } catch (IOException | InterruptedException ex) {
                System.out.println(String.format("error (%s)", ex.getMessage()));
            }
        }

        @Command(mixinStandardHelpOptions = true, subcommands = {CommandLine.HelpCommand.class},
                description = "Adds a new network rule")
        public void add(
                @CommandLine.Parameters(paramLabel = "RULE", description = "the rules") String[] rules
        ) {
            for (String rule : rules) {
                try {
                    System.out.print(String.format("Adding rule (%s): ", rule));

                    int result = NetworkDegrader.addRule(device, rule);
                    if (result == 0) {
                        System.out.println("success");
                    } else {
                        System.out.println(String.format("error (%d)", result));
                    }
                } catch (IOException | InterruptedException ex) {
                    System.out.println(String.format("error (%s)", ex.getMessage()));
                }
            }
        }
    }
}
