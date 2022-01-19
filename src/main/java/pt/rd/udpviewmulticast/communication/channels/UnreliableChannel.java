package pt.rd.udpviewmulticast.communication.channels;

import pt.rd.udpviewmulticast.communication.Channel;
import pt.rd.udpviewmulticast.communication.Packet;
import pt.rd.udpviewmulticast.communication.packets.PacketRegistry;
import pt.rd.udpviewmulticast.structures.View;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.*;

public class UnreliableChannel implements Channel {

    public static int DEFAULT_PORT = 5555;

    /*

     */

    private View view;

    private MulticastSocket socket;

    private boolean running = false;

    /*

     */

    @Override
    public void open(View view) {
        this.view = view;

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

                try (ByteArrayInputStream byteStream = new ByteArrayInputStream(datagram.getData(), 0, datagram.getLength()); DataInputStream stream = new DataInputStream(byteStream)) {
                    byte packetId = stream.readByte();
                    Class<? extends Packet> packetClass = PacketRegistry.getPacketFromId(packetId);
                    if (packetClass == null) {
                        throw new RuntimeException(String.format("Packet with ID %d is not registered.", packetId));
                    }

                    Packet packet = packetClass.getDeclaredConstructor().newInstance();
                    packet.deserialize(stream);

                    this.receive((InetSocketAddress) datagram.getSocketAddress(), packet);
                } catch(NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
                    ex.printStackTrace();
                }

            } catch(SocketException ignored) {
            } catch(IOException ex) {
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

    @Override
    public void send(Packet packet) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream(); DataOutputStream stream = new DataOutputStream(byteStream)) {
            Byte packetId = PacketRegistry.getIdFromPacket(packet.getClass());
            if (packetId == null) {
                throw new RuntimeException(String.format("Packet '%s' is not registered.", packet.getClass().getSimpleName()));
            }

            stream.writeByte(packetId);
            packet.serialize(stream);

            byte[] buf = byteStream.toByteArray();

            DatagramPacket datagram = new DatagramPacket(buf, buf.length, view.getSubnetAddress(), DEFAULT_PORT);
            socket.send(datagram);

            System.out.println(String.format("Sent packet '%s' with ID %d: %s", packet.getClass().getSimpleName(), packetId, packet.toString()));
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void receive(InetSocketAddress source, Packet packet) {
        System.out.println(String.format("Received packet '%s' from '%s:%d': %s.", packet.getClass().getSimpleName(), source.getHostString(), source.getPort(), packet.toString()));
    }
}
