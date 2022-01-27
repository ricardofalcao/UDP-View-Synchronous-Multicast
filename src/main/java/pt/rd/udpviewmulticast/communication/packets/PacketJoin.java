package pt.rd.udpviewmulticast.communication.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import pt.rd.udpviewmulticast.communication.Packet;

public class PacketJoin implements Packet {

    /*

     */

    public PacketJoin() {}

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
        return "Join";
    }
}
