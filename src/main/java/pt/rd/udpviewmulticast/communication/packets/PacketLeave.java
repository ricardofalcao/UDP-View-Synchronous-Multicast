package pt.rd.udpviewmulticast.communication.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import pt.rd.udpviewmulticast.communication.Packet;

public class PacketLeave implements Packet {

    /*

     */

    public PacketLeave() {}

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
        return "Leave";
    }

    @Override
    public boolean shouldQueue() {
        return false;
    }
}
