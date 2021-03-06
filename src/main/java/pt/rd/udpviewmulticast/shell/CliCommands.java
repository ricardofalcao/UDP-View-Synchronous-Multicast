package pt.rd.udpviewmulticast.shell;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.FileWriteMode;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import org.fusesource.jansi.AnsiConsole;
import org.jline.console.SystemRegistry;
import org.jline.console.impl.Builtins;
import org.jline.console.impl.SystemRegistryImpl;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.MaskingCallback;
import org.jline.reader.Parser;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import picocli.CommandLine;
import picocli.shell.jline3.PicocliCommands;
import pt.rd.udpviewmulticast.Main;
import pt.rd.udpviewmulticast.communication.Communication;
import pt.rd.udpviewmulticast.communication.LeaderCommunication;
import pt.rd.udpviewmulticast.communication.packets.PacketACK;
import pt.rd.udpviewmulticast.communication.packets.PacketFlush;
import pt.rd.udpviewmulticast.communication.packets.PacketHello;
import pt.rd.udpviewmulticast.communication.packets.PacketJoin;
import pt.rd.udpviewmulticast.communication.packets.PacketLeave;
import pt.rd.udpviewmulticast.communication.packets.PacketNewView;
import pt.rd.udpviewmulticast.communication.packets.PacketRegistry;
import pt.rd.udpviewmulticast.structures.View;

@CommandLine.Command(name = "udp-multicast", mixinStandardHelpOptions = true, version = "1.0",
    description = "Creates a node in a view multicast group")
public class CliCommands implements Callable<Integer> {

    @CommandLine.Option(names = {"-l", "--leader"}, description = "Sets the node as the leader")
    private boolean leader = false;

    @CommandLine.Option(names = {"-n", "--noshell"}, description = "Does not create shell")
    private boolean shell = true;

    @CommandLine.Option(names = {"-p", "--packets"}, description = "Number of packets per second")
    private int packets = 100;

    @CommandLine.Option(names = {"--log"}, description = "Log name")
    private String log = "";

    @CommandLine.Option(names = {"--net"}, description = "Inject net disturbances")
    private String net = "";

    @Override
    public Integer call() throws Exception {
        Main.LEADER = leader;
        Main.DISTURBANCE = net;

        File file = new File(log.isEmpty() ? "/internal/data.csv" : String.format("/internal/data_%s.csv", log));
        Main.LOG = Files.asCharSink(file, Charsets.UTF_8, FileWriteMode.APPEND);
        if (!file.exists()) {
            Main.LOG.write("UNIX time,Node IP,Packets,Degradation,View ID,View members,View change time (ns),Sent packets,Acked packets,Retries,Stabilize Instant RTT (ns),Stabilize Smooth RTT (ns),Deliver delay (ns),Deliver Smooth delay (ns)\n");
        }

        Communication.PACKET_FREQUENCY = packets;

        View discoveryView = new View(
            (byte) 1,
            Lists.newArrayList(InetAddress.getLocalHost())
        );

        Communication nodeCommunication = new Communication();
        nodeCommunication.setup();

        Main.COMMUNICATION = nodeCommunication;

        Communication leaderCommunication = new LeaderCommunication(nodeCommunication);

        if (leader) {

            View ownView = new View(
                (byte) 10,
                Lists.newArrayList(InetAddress.getLocalHost())
            );

            nodeCommunication.join(ownView);

            leaderCommunication.setup();
            leaderCommunication.join(discoveryView);
            new Thread(leaderCommunication::listenForPackets).start();

        } else {

            nodeCommunication.join(discoveryView);
            nodeCommunication.requestJoin();

        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try  {
                if (!leader) {
                    nodeCommunication.requestLeave();
                }

                nodeCommunication.shutdown();
                leaderCommunication.shutdown();

                System.out.println("Bye");
            } catch(IOException ex) {
                ex.printStackTrace();
            }
        }));

        Thread thread = new Thread(nodeCommunication::listenForPackets);
        thread.start();

        if (shell) {
            createShell();
        } else {
            thread.join();
        }

        return 0;
    }

    /*

     */

    private static void createShell() {
        AnsiConsole.systemInstall();
        try {
            Supplier<Path> workDir = () -> Paths.get(System.getProperty("user.dir"));

            // set up JLine built-in commands
            Builtins builtins = new Builtins(workDir, null, null);
            builtins.rename(Builtins.Command.TTOP, "top");
            builtins.alias("zle", "widget");
            builtins.alias("bindkey", "keymap");

            // set up picocli commands
            ShellCommands commands = new ShellCommands();

            PicocliCommands.PicocliCommandsFactory factory = new PicocliCommands.PicocliCommandsFactory();
            // Or, if you have your own factory, you can chain them like this:
            // MyCustomFactory customFactory = createCustomFactory(); // your application custom factory
            // PicocliCommandsFactory factory = new PicocliCommandsFactory(customFactory); // chain the factories

            CommandLine cmd = new CommandLine(commands, factory);
            PicocliCommands picocliCommands = new PicocliCommands(cmd);

            Parser parser = new DefaultParser();
            try (Terminal terminal = TerminalBuilder.builder().build()) {
                SystemRegistry systemRegistry = new SystemRegistryImpl(parser, terminal, workDir, null);
                systemRegistry.setCommandRegistries(builtins, picocliCommands);
                systemRegistry.register("help", picocliCommands);

                LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .completer(systemRegistry.completer())
                    .parser(parser)
                    .variable(LineReader.LIST_MAX, 50)   // max tab completion candidates
                    .build();

                builtins.setLineReader(reader);
                factory.setTerminal(terminal);

                /*TailTipWidgets widgets = new TailTipWidgets(reader, systemRegistry::commandDescription, 5, TailTipWidgets.TipType.COMPLETER);
                widgets.enable();
                KeyMap<Binding> keyMap = reader.getKeyMaps().get("main");
                keyMap.bind(new Reference("tailtip-toggle"), KeyMap.alt("s"));*/

                String prompt = "> ";
                String rightPrompt = null;

                // start the shell and process input until the user quits with Ctrl-D
                String line;
                while (true) {
                    try {
                        systemRegistry.cleanUp();
                        line = reader.readLine(prompt, rightPrompt, (MaskingCallback) null, null);
                        systemRegistry.execute(line);
                    } catch (UserInterruptException e) {
                        // Ignore
                    } catch (EndOfFileException e) {
                        return;
                    } catch (Exception e) {
                        systemRegistry.trace(e);
                    }
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            AnsiConsole.systemUninstall();
        }
    }
}
