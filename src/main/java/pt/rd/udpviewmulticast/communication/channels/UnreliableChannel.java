package pt.rd.udpviewmulticast.communication.channels;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import pt.rd.udpviewmulticast.communication.Communication;
import pt.rd.udpviewmulticast.communication.Packet;
import pt.rd.udpviewmulticast.communication.packets.PacketRegistry;

public class UnreliableChannel extends Channel {

    public UnreliableChannel(Communication communication) {
        super(communication);
    }

    /*

     */

    @Override
    public void write(Packet packet, DataOutputStream stream, Object... args) throws IOException {
        Byte packetId = PacketRegistry.getIdFromPacket(packet.getClass());
        if (packetId == null) {
            throw new IOException(String.format("Packet '%s' is not registered.", packet.getClass().getSimpleName()));
        }

        stream.writeByte(packetId);
        packet.serialize(stream);
    }

    @Override
    public Packet read(InetAddress sourceAddress, DataInputStream stream) throws IOException {
        byte packetId = stream.readByte();

        Class<? extends Packet> packetClass = PacketRegistry.getPacketFromId(packetId);
        if (packetClass == null) {
            throw new IOException(String.format("Packet with ID %d is not registered.", packetId));
        }

        try {
            Packet packet = packetClass.getDeclaredConstructor().newInstance();
            packet.deserialize(stream);

            return packet;
        } catch(NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            throw new IOException(ex);
        }
    }
}
