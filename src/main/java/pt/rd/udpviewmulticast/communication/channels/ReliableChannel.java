package pt.rd.udpviewmulticast.communication.channels;

import pt.rd.udpviewmulticast.communication.Channel;
import pt.rd.udpviewmulticast.communication.Packet;
import pt.rd.udpviewmulticast.communication.packets.PacketNACK;
import pt.rd.udpviewmulticast.communication.packets.PacketRegistry;
import pt.rd.udpviewmulticast.structures.View;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class ReliableChannel implements Channel {

    public static int DEFAULT_PORT = 5555;

    /*

     */


    private View view;

    private MulticastSocket socket;

    private boolean running = false;

    //

    private Map<InetSocketAddress, ReliableNode> nodes = new HashMap<>();

    public short lastOutgoingSeq = 0;

    public Map<Short, ReliablePacket> outgoingPackets = new HashMap<>();


    /*

     */

    @Override
    public void open(View view) {
        this.view = view;

        this.nodes.clear();

        //for (Process member : this.view.getMembers()) {
        //    this.nodes.put(member.getId(), new ReliableNode());
        //}

        try {
            this.socket = new MulticastSocket(DEFAULT_PORT);
            this.socket.joinGroup(this.view.getSubnetAddress());
            this.running = true;
            System.out.println(String.format("Joined group with ID %d and ip '%s'.", this.view.getId(), this.view.getSubnetAddress().getHostAddress()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void listenForPackets() {
        byte[] buf = new byte[256];
        System.out.println("Started listening for packets.");

        while (this.running) {
            DatagramPacket datagram = new DatagramPacket(buf, buf.length);

            try {
                socket.receive(datagram);

                InetSocketAddress sourceAddress = (InetSocketAddress) datagram.getSocketAddress();
                ReliableNode node = this.nodes.getOrDefault(sourceAddress, new ReliableNode());
                this.nodes.put(sourceAddress, node);

                try (ByteArrayInputStream byteStream = new ByteArrayInputStream(datagram.getData(), 0, datagram.getLength()); DataInputStream stream = new DataInputStream(byteStream)) {
                    short seqId = stream.readShort();

                    byte packetId = stream.readByte();

                    Class<? extends Packet> packetClass = PacketRegistry.getPacketFromId(packetId);
                    if (packetClass == null) {
                        throw new RuntimeException(String.format("Packet with ID %d is not registered.", packetId));
                    }

                    Packet packet = packetClass.getDeclaredConstructor().newInstance();
                    packet.deserialize(stream);

                    if (packet instanceof PacketNACK) {
                        for(short sendId = seqId; sendId <= lastOutgoingSeq; sendId++) {
                            _send(sendId, outgoingPackets.get(sendId).packet, sourceAddress);
                        }

                        continue;
                    }

                    if (seqId <= node.lastIncomingSeq) {
                        continue;
                    }

                    if (node.lastIncomingSeq != -1 && seqId > node.lastIncomingSeq + 1) {
                        _send((short) (node.lastIncomingSeq + 1), new PacketNACK(), sourceAddress);
                        continue;
                    }

                    System.out.println(String.format("[%d] Received packet '%s' from '%s:%d': %s.", seqId, packet.getClass().getSimpleName(), sourceAddress.getHostString(), sourceAddress.getPort(), packet.toString()));

                    this.receive(sourceAddress, packet);
                    node.lastIncomingSeq = seqId;

                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
                    ex.printStackTrace();
                }

            } catch (SocketException ignored) {
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        }
    }

    @Override
    public void close() {
        if (this.socket != null) {
            this.running = false;
            this.socket.close();
        }
    }

    /*

     */

    public void tick() {
        // runs every seconds
    }

    /*

     */

    private DatagramPacket _sendBytes(short seqId, byte[] value, InetSocketAddress address) throws IOException {
        DatagramPacket datagram = new DatagramPacket(value, value.length, address);
        socket.send(datagram);

        return datagram;
    }

    private DatagramPacket _send(short seqId, Packet packet, InetSocketAddress address) throws IOException {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream(); DataOutputStream stream = new DataOutputStream(byteStream)) {

            // Cabeçalho da camada de transporte;

            // Write Stream
            stream.writeShort(seqId);

            // Camada de aplicaçao

            Byte packetId = PacketRegistry.getIdFromPacket(packet.getClass());
            if (packetId == null) {
                throw new RuntimeException(String.format("Packet '%s' is not registered.", packet.getClass().getSimpleName()));
            }

            stream.writeByte(packetId);
            packet.serialize(stream);

            // Final da camada de transporte

            byte[] buf = byteStream.toByteArray();

            if (!(packet instanceof PacketNACK)) {
                System.out.println(String.format("[%d] Sent packet '%s': %s", seqId, packet.getClass().getSimpleName(), packet.toString()));
            }

            return _sendBytes(seqId, buf, address);
        }
    }

    @Override
    public void send(Packet packet) {
        try {
            //Incrementa sequencia
            lastOutgoingSeq++;

            _send(lastOutgoingSeq, packet, new InetSocketAddress(view.getSubnetAddress(), DEFAULT_PORT));

            ReliablePacket reliablePacket = new ReliablePacket(lastOutgoingSeq, (byte) 0, packet);
            outgoingPackets.put(lastOutgoingSeq, reliablePacket);
        } catch (IOException ex) {
            ex.printStackTrace();

        }
    }

    @Override
    public void receive(InetSocketAddress source, Packet packet) {


    }

    /*

     */

    public class ReliableNode {

        public short lastIncomingSeq = -1;

    }

    public class ReliablePacket {

        public short seqId;

        public byte attempts;

        public Packet packet;

        /*

         */

        public ReliablePacket(short seqId, byte attempts, Packet packet) {
            this.seqId = seqId;
            this.attempts = attempts;
            this.packet = packet;
        }
    }
}
