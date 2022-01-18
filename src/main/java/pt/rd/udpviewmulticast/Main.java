package pt.rd.udpviewmulticast;

import org.fusesource.jansi.AnsiConsole;
import org.jline.console.SystemRegistry;
import org.jline.console.impl.Builtins;
import org.jline.console.impl.SystemRegistryImpl;
import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import picocli.CommandLine;
import picocli.shell.jline3.PicocliCommands;
import pt.rd.udpviewmulticast.communication.Channel;
import pt.rd.udpviewmulticast.communication.channels.ReliableChannel;
import pt.rd.udpviewmulticast.communication.packets.PacketAck;
import pt.rd.udpviewmulticast.communication.packets.PacketHello;
import pt.rd.udpviewmulticast.communication.packets.PacketRegistry;
import pt.rd.udpviewmulticast.shell.ShellCommands;
import pt.rd.udpviewmulticast.structures.View;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Supplier;

public class Main {

    public static UUID ID;

    public static Channel CHANNEL;

    /*

     */

    public static void main(String[] _args) throws UnknownHostException {
        PacketRegistry.registerPacket((byte) 99, PacketAck.class);
        PacketRegistry.registerPacket((byte) 1, PacketHello.class);

        ID = UUID.randomUUID();

        View basicView = new View(
                10,
                null,
                new ArrayList<>(),
                InetAddress.getByName("230.0.0.0")
        );

        CHANNEL = new ReliableChannel();
        Thread thread = new Thread(() -> {
            CHANNEL.open(basicView);
            CHANNEL.listenForPackets();
        });

        thread.start();

        createShell();

        CHANNEL.close();
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
