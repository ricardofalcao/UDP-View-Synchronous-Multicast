package pt.rd.udpviewmulticast.shell;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.shell.jline3.PicocliCommands;
import pt.rd.udpviewmulticast.Main;
import pt.rd.udpviewmulticast.benchmark.NetworkDegrader;
import pt.rd.udpviewmulticast.communication.Communication;
import pt.rd.udpviewmulticast.communication.node.Node;
import pt.rd.udpviewmulticast.communication.packets.PacketHello;

/**
 * Top-level command that just prints help.
 */
@Command(
        name = "",
        description = {"Interact with the application"},
        footer = {"", "Press Ctrl-D to exit."},
        subcommands = {PicocliCommands.ClearScreen.class, CommandLine.HelpCommand.class}
)
public class ShellCommands implements Runnable {

    public void run() {
        System.out.println(new CommandLine(this).getUsageMessage());
    }

    /*

     */

    @Command(mixinStandardHelpOptions = true,
        description = "Shows the current node IP")
    public void ip() {
        try {
            System.out.println(InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    @Command(mixinStandardHelpOptions = true,
        description = "Shows system stats")
    public void stats() {
        Communication communication = Main.COMMUNICATION;
        System.out.println(String.format("Average RTT: %d ns", communication.getSmoothStabilizeRtt()));
        System.out.println(String.format("Last sent sequence: %d", communication.getLastSentSeq()));
        System.out.println(String.format("Packet loss: %.1f (%d acked in %d) [%d retries]", communication.getPacketLoss() * 100, communication.getPacketsAck(), communication.getPacketsSent(), communication.getRetries()));

        System.out.println("Nodes:");
        for (Map.Entry<InetAddress, Node> entry : communication.getNodes().entrySet()) {
            System.out.println(String.format("  %s: %d/%d", entry.getKey().getHostAddress(), entry.getValue().getLastDeliveredSeq(), entry.getValue().getLastReceivedSeq()));
        }
    }

    @Command(mixinStandardHelpOptions = true,
            description = "Pings an IP")
    public void ping(@CommandLine.Parameters(paramLabel = "IP", description = "The IP to ping.") String ip) {
        try {

            long start = System.nanoTime();

            System.out.print("pinging... ");
            boolean reachable = InetAddress.getByName(ip).isReachable(5000);
            System.out.println(reachable ? String.format("success (%d ns)", (System.nanoTime() - start)) : "failed");

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    @Command(mixinStandardHelpOptions = true,
            description = "Sends an hello packet")
    public void hello(@CommandLine.Parameters(paramLabel = "NUM", description = "The NUM to send.") int num) {
        for(int i = 0; i < num; i++) {
            try {
                Main.COMMUNICATION.multicastPacket(new PacketHello(69 + i));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Command(mixinStandardHelpOptions = true,
            description = "Sets the network rules")
    public void net(
            @CommandLine.Option(names = {"-d", "--device"}, paramLabel = "DEVICE", description = "the network device", defaultValue = "eth0") String device,
            @CommandLine.Parameters(paramLabel = "RULE", description = {
                    "the rules",
                    "delay 100ms      - delays every packet by 100ms",
                    "loss 50%%         - drops 50%% of the outgoing packets",
                    "corrupt 10%%      - corrupts 10%% of the outgoing packets",
                    "duplicate 5%%     - duplicates 5%% of the outgoing packets",
                    "gap 5 delay 10ms - every 5th packet goes immediately and every other packet to be delayed by 10ms"
            }) String rule
    ) {
        try {
            System.out.print(String.format("Clearing rules (%s): ", device));

            int result = NetworkDegrader.clearRules(device);
            if (result == 0) {
                System.out.println("success");
            } else {
                System.out.println(String.format("error (%d)", result));
            }

            System.out.print(String.format("Adding rule (%s): ", rule));

            result = NetworkDegrader.addRule(device, rule);
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
