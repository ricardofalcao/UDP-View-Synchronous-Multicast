package pt.rd.udpviewmulticast.communication.packets;

import pt.rd.udpviewmulticast.communication.Packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PacketAck implements Packet {

    /*

     */

    public PacketAck() {}

    /*

     */

    @Override
    public void serialize(DataOutputStream outputStream) throws IOException {
    }

    @Override
    public void deserialize(DataInputStream inputStream) throws IOException {
    }

    /*

     */
}
