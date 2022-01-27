package pt.rd.udpviewmulticast.communication.channels;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import pt.rd.udpviewmulticast.communication.Communication;
import pt.rd.udpviewmulticast.communication.Packet;

public abstract class Channel {

    protected Communication communication;

    public Channel(Communication communication) {
        this.communication = communication;
    }

    /*

     */

    public abstract void write(Packet packet, DataOutputStream stream, Object... args) throws IOException;

    public abstract Packet read(InetAddress sourceAddress, DataInputStream stream) throws IOException;

}
