package pt.rd.udpviewmulticast.communication.channels;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import pt.rd.udpviewmulticast.communication.Communication;
import pt.rd.udpviewmulticast.communication.Packet;
import pt.rd.udpviewmulticast.communication.packets.PacketNACK;
import pt.rd.udpviewmulticast.communication.packets.PacketRegistry;

public class ReliableNegChannel extends Channel {

    /*

     */

    public class ReliableNode {

        public InetAddress nodeAddress;

        public Map<Short, Packet> receivedPackets = new HashMap<>();

        public short lastIncomingSeq = -1;

        /*

         */

        public ReliableNode(InetAddress nodeAddress) {
            this.nodeAddress = nodeAddress;
        }

    }

    /*

     */

    private Map<InetAddress, ReliableNode> nodes = new HashMap<>();

    //

    public Map<Short, Packet> sentPackets = new HashMap<>();

    public short lastOutgoingSeq = 0;

    public ReliableNegChannel(Communication communication) {
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

        if (args.length == 0) {
            lastOutgoingSeq++;
        }

        short seq = lastOutgoingSeq;

        if (args.length > 0) {
            seq = (short) args[0];
        }

        stream.writeShort(seq);
        stream.writeByte(packetId);
        packet.serialize(stream);

        this.sentPackets.put(seq, packet);
    }

    @Override
    public Packet read(InetAddress sourceAddress, DataInputStream stream) throws IOException {
        short seqId = stream.readShort();
        byte packetId = stream.readByte();

        System.out.println(seqId);

        Class<? extends Packet> packetClass = PacketRegistry.getPacketFromId(packetId);
        if (packetClass == null) {
            throw new IOException(String.format("Packet with ID %d is not registered.", packetId));
        }

        ReliableNode node = this.nodes.get(sourceAddress);
        if (node == null) {
            node = new ReliableNode(sourceAddress);
            this.nodes.put(sourceAddress, node);
        }

        try {
            Packet packet = packetClass.getDeclaredConstructor().newInstance();
            packet.deserialize(stream);

            if (packet instanceof PacketNACK) {
                for(short sendId = seqId; sendId <= lastOutgoingSeq; sendId++) {
                    communication.sendPacket(this, sentPackets.get(sendId), sourceAddress, sendId);
                }

                return null;
            }

            // Maintain ordering and dedup, ignore if incoming seq is less than the previously seen
            if (seqId <= node.lastIncomingSeq) {
                return null;
            }

            // Verify is the incoming seq is the next on queue, otherwise send nack
            if (node.lastIncomingSeq != -1 && seqId > node.lastIncomingSeq + 1) {
                communication.sendPacket(this, new PacketNACK(), sourceAddress, (short) (node.lastIncomingSeq + 1));

                return null;
            }

            node.receivedPackets.put(seqId, packet);
            node.lastIncomingSeq = seqId;

            System.out.println(String.format("Last received from %s: %d", node.nodeAddress.getHostAddress(), seqId));

            return packet;
        } catch(NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            throw new IOException(ex);
        }
    }
}
