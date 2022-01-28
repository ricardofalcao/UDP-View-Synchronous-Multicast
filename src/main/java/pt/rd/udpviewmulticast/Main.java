package pt.rd.udpviewmulticast;

import java.net.UnknownHostException;
import picocli.CommandLine;
import pt.rd.udpviewmulticast.communication.Communication;
import pt.rd.udpviewmulticast.shell.CliCommands;

public class Main {

    public static boolean LEADER = false;

    public static Communication COMMUNICATION;

    /*

     */

    public static void main(String[] _args) throws UnknownHostException {
        System.setProperty("java.util.logging.SimpleFormatter.format",
            "%1$tF %1$tT %4$s %5$s%6$s%n");

        int exitCode = new CommandLine(new CliCommands()).execute(_args);
        System.exit(exitCode);
    }

}
