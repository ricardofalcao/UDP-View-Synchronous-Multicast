package pt.rd.udpviewmulticast;

import java.net.UnknownHostException;
import picocli.CommandLine;
import pt.rd.udpviewmulticast.communication.Communication;
import pt.rd.udpviewmulticast.communication.channels.Channel;
import pt.rd.udpviewmulticast.shell.CliCommands;

public class Main {

    public static byte ID = -1;

    public static Communication COMMUNICATION;

    /*

     */

    public static void main(String[] _args) throws UnknownHostException {
        int exitCode = new CommandLine(new CliCommands()).execute(_args);
        System.exit(exitCode);
    }

}
