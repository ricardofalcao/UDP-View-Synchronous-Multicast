package pt.rd.udpviewmulticast;

import com.google.common.base.Charsets;
import com.google.common.io.CharSink;
import com.google.common.io.FileWriteMode;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import picocli.CommandLine;
import pt.rd.udpviewmulticast.benchmark.NetworkDegrader;
import pt.rd.udpviewmulticast.communication.Communication;
import pt.rd.udpviewmulticast.communication.packets.PacketACK;
import pt.rd.udpviewmulticast.communication.packets.PacketFlush;
import pt.rd.udpviewmulticast.communication.packets.PacketHello;
import pt.rd.udpviewmulticast.communication.packets.PacketJoin;
import pt.rd.udpviewmulticast.communication.packets.PacketLeave;
import pt.rd.udpviewmulticast.communication.packets.PacketNewView;
import pt.rd.udpviewmulticast.communication.packets.PacketRegistry;
import pt.rd.udpviewmulticast.shell.CliCommands;

public class Main {

    public static String IP = "";

    public static boolean LEADER = false;

    public static Communication COMMUNICATION;

    public static CharSink LOG;

    /*

     */

    public static void main(String[] _args) throws IOException, InterruptedException {
        String ip = InetAddress.getLocalHost().getHostAddress();
        IP = ip;

        System.setProperty("java.util.logging.SimpleFormatter.format",
            "%1$tT %3$s %4$s %5$s%6$s%n");

        /*

         */

        PacketRegistry.registerPacket((byte) 99, PacketACK.class);
        PacketRegistry.registerPacket((byte) 1, PacketHello.class);
        PacketRegistry.registerPacket((byte) 2, PacketJoin.class);
        PacketRegistry.registerPacket((byte) 3, PacketLeave.class);
        PacketRegistry.registerPacket((byte) 4, PacketNewView.class);
        PacketRegistry.registerPacket((byte) 5, PacketNewView.class);
        PacketRegistry.registerPacket((byte) 6, PacketFlush.class);

        int exitCode = new CommandLine(new CliCommands()).execute(_args);
        System.exit(exitCode);
    }

}
