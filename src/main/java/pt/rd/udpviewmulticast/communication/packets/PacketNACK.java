package pt.rd.udpviewmulticast.communication.packets;

import pt.rd.udpviewmulticast.communication.Packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PacketNACK implements Packet {

    /*

     */

    public PacketNACK() {}

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
    public String toString() {
        return "NACK";
    }
}
