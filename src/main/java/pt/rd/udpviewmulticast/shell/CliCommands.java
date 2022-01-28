package pt.rd.udpviewmulticast.shell;

import com.google.common.collect.Lists;
import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
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

    @Override
    public Integer call() throws Exception {
        PacketRegistry.registerPacket((byte) 99, PacketACK.class);
        PacketRegistry.registerPacket((byte) 1, PacketHello.class);
        PacketRegistry.registerPacket((byte) 2, PacketJoin.class);
        PacketRegistry.registerPacket((byte) 3, PacketLeave.class);
        PacketRegistry.registerPacket((byte) 4, PacketNewView.class);
        PacketRegistry.registerPacket((byte) 5, PacketNewView.class);
        PacketRegistry.registerPacket((byte) 6, PacketFlush.class);

        Main.LEADER = leader;

        Main.COMMUNICATION = new Communication();
        Main.COMMUNICATION.setup();

        if (leader) {
            View basicView = new View(
                (byte) 1,
                Lists.newArrayList(InetAddress.getLocalHost())
            );

            Main.COMMUNICATION.join(basicView);
        } else {
            Main.COMMUNICATION.multicastPacket(Communication.LEADER_SUBNET, new PacketJoin());
        }

        Thread thread = new Thread(() -> {
            Main.COMMUNICATION.listenForPackets();
        });

        thread.start();

        createShell();

        if (!leader) {
            Main.COMMUNICATION.multicastPacket(Communication.LEADER_SUBNET, new PacketLeave());
        }

        Main.COMMUNICATION.shutdown();

        System.out.println("Bye");

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
