package pt.rd.udpviewmulticast.communication.packets;

import pt.rd.udpviewmulticast.communication.Packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PacketHello implements Packet {

    private int id;

    /*

     */

    public PacketHello() {}

    public PacketHello(int id) {
        this.id = id;
    }

    /*

     */

    @Override
    public void serialize(DataOutputStream outputStream) throws IOException {
        outputStream.writeInt(this.id);
    }

    @Override
    public void deserialize(DataInputStream inputStream) throws IOException {
        this.id = inputStream.readInt();
    }

    /*

     */

    @Override
    public String toString() {
        return "Hello{" +
                "id=" + id +
                '}';
    }

    @Override
    public boolean shouldDebug() {
        return false;
    }
}
