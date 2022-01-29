package pt.rd.udpviewmulticast.communication.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;
import java.util.HashSet;
import pt.rd.udpviewmulticast.communication.Packet;
import pt.rd.udpviewmulticast.structures.View;

public class PacketNewView implements Packet {

    private View view;

    /*

     */

    public PacketNewView() {
    }

    public PacketNewView(View view) {
        this.view = view;
    }

    @Override
    public void serialize(DataOutputStream outputStream) throws IOException {
        outputStream.writeByte(view.getId());

        Collection<InetAddress> members = view.getMembers();
        outputStream.writeByte(members.size());

        for (InetAddress member : members) {
            byte[] value = member.getAddress();
            for (int i = 0; i < 4; i++) {
                outputStream.writeByte(value[i]);
            }
        }
    }

    @Override
    public void deserialize(DataInputStream inputStream) throws IOException {
        byte id = inputStream.readByte();
        int size = inputStream.readByte();

        Collection<InetAddress> members = new HashSet<>(size);
        for (int i = 0; i < size; i++) {
            members.add(InetAddress.getByAddress(new byte[]{
                inputStream.readByte(), inputStream.readByte(), inputStream.readByte(), inputStream.readByte(),
            }));
        }

        this.view = new View(id, members);
    }

    /*

     */

    public View getView() {
        return view;
    }

    @Override
    public String toString() {
        return "NewView{" +
            "view=" + view +
            '}';
    }

    @Override
    public boolean shouldQueue() {
        return false;
    }

    @Override
    public boolean shouldDebug() {
        return false;
    }
}
