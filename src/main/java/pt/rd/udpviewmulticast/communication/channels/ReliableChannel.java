package pt.rd.udpviewmulticast.communication.channels;

import pt.rd.udpviewmulticast.communication.Channel;
import pt.rd.udpviewmulticast.communication.Packet;
import pt.rd.udpviewmulticast.communication.packets.PacketAck;
import pt.rd.udpviewmulticast.communication.packets.PacketRegistry;
import pt.rd.udpviewmulticast.structures.Process;
import pt.rd.udpviewmulticast.structures.View;
import pt.rd.udpviewmulticast.utils.CRC16;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.zip.CRC32;

public class ReliableChannel implements Channel {

    public static int DEFAULT_PORT = 5555;
    public short _lastOutgoingSequence = 0;

    /*

     */


    private View view;

    private MulticastSocket socket;

    private boolean running = false;

    private Map<String, ReliableNode> nodes = new HashMap<>();


    /*

     */

    @Override
    public void open(View view) {
        this.view = view;

        this.nodes.clear();
        for (Process member : this.view.getMembers()) {
            this.nodes.put(member.getId(), new ReliableNode());
        }

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

            // Camada transporte
            // pegar no seqid
            // possivelmente adicionar a uma fila de espera

            try {
                socket.receive(datagram);

                InetSocketAddress sourceAddress = (InetSocketAddress) datagram.getSocketAddress();

                try (ByteArrayInputStream byteStream = new ByteArrayInputStream(datagram.getData(), 0, datagram.getLength()); DataInputStream stream = new DataInputStream(byteStream)) {
                    short seqId = stream.readShort();

                    byte packetId = stream.readByte();
                    Class<? extends Packet> packetClass = PacketRegistry.getPacketFromId(packetId);
                    if (packetClass == null) {
                        throw new RuntimeException(String.format("Packet with ID %d is not registered.", packetId));
                    }

                    Packet packet = packetClass.getDeclaredConstructor().newInstance();
                    packet.deserialize(stream);

                    // verifiar hash
                    // se nok entao para

                    if (!(packet instanceof PacketAck)) {
                        // ack
                        _send(seqId, new PacketAck(), sourceAddress.getAddress(), false);
                    }

                    this.receive(sourceAddress, packet);
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

    private DatagramPacket _send(short seqId, Packet packet, InetAddress address, boolean useChecksum) throws IOException {
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

            if (useChecksum) {
                short checksum = CRC16.get(buf);
                stream.writeShort(checksum);
            }

            DatagramPacket datagram = new DatagramPacket(buf, buf.length, address, DEFAULT_PORT);
            socket.send(datagram);

            return datagram;
        }
    }

    @Override
    public void send(Packet packet) {
        try {
            //Incrementa sequencia
            _lastOutgoingSequence++;

            DatagramPacket datagram = _send(_lastOutgoingSequence, packet, view.getSubnetAddress(), true);

            ReliablePacket reliablePacket = new ReliablePacket(_lastOutgoingSequence, (byte) 0, datagram);
            for (ReliableNode node : this.nodes.values()) {
                node.outgoingPackets.add(reliablePacket);
            }

            System.out.println(String.format("Sent packet '%s': %s", packet.getClass().getSimpleName(), packet.toString()));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void receive(InetSocketAddress source, Packet packet) {
        System.out.println(String.format("Received packet '%s' from '%s:%d': %s.", packet.getClass().getSimpleName(), source.getHostString(), source.getPort(), packet.toString()));


    }

    /*

     */

    public class ReliableNode {

        public Queue<ReliablePacket> outgoingPackets = new LinkedList<>();

    }

    public class ReliablePacket {

        public short seqId;

        public byte attempts;

        public DatagramPacket packet;

        /*

         */

        public ReliablePacket(short seqId, byte attempts, DatagramPacket packet) {
            this.seqId = seqId;
            this.attempts = attempts;
            this.packet = packet;
        }
    }
}
