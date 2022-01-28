package pt.rd.udpviewmulticast.communication.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import pt.rd.udpviewmulticast.communication.Packet;

public class PacketACK implements Packet {

    /*

     */

    public PacketACK() {}

    @Override
    public void serialize(DataOutputStream outputStream) throws IOException {

    }

    @Override
    public void deserialize(DataInputStream inputStream) throws IOException {

    }

    /*

     */

    @Override
    public boolean shouldDebug() {
        return false;
    }

    @Override
    public boolean shouldQueue() {
        return false;
    }

    @Override
    public String toString() {
        return "ACK";
    }
}
