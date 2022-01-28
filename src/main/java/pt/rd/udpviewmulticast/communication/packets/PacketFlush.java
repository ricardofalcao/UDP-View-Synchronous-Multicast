package pt.rd.udpviewmulticast.communication.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import pt.rd.udpviewmulticast.communication.Packet;

public class PacketFlush implements Packet {

    /*

     */

    public PacketFlush() {}

    @Override
    public void serialize(DataOutputStream outputStream) throws IOException {

    }

    @Override
    public void deserialize(DataInputStream inputStream) throws IOException {

    }

    /*

     */

    @Override
    public String toString() {
        return "Flush";
    }

    @Override
    public boolean shouldQueue() {
        return false;
    }
}
