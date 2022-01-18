package pt.rd.udpviewmulticast.communication.packets;

import pt.rd.udpviewmulticast.communication.Packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PacketHello implements Packet {

    private int id;

    private String text;

    /*

     */

    public PacketHello() {}

    public PacketHello(int id, String text) {
        this.id = id;
        this.text = text;
    }

    /*

     */

    @Override
    public void serialize(DataOutputStream outputStream) throws IOException {
        outputStream.writeInt(this.id);
        outputStream.writeUTF(this.text);
    }

    @Override
    public void deserialize(DataInputStream inputStream) throws IOException {
        this.id = inputStream.readInt();
        this.text = inputStream.readUTF();
    }

    /*

     */

    @Override
    public String toString() {
        return "PacketHello{" +
                "id=" + id +
                ", text='" + text + '\'' +
                '}';
    }
}
