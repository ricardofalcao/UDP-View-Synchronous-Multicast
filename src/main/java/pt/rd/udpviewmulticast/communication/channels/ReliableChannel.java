package pt.rd.udpviewmulticast.communication.channels;

import pt.rd.udpviewmulticast.communication.Channel;
import pt.rd.udpviewmulticast.communication.Packet;
import pt.rd.udpviewmulticast.communication.packets.PacketRegistry;
import pt.rd.udpviewmulticast.structures.View;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.util.HashMap;
import java.util.Map;

public class ReliableChannel implements Channel {

    public static int DEFAULT_PORT = 5555;
    //public int _lastOutgoingSequence = 0;

    /*

     */


    private View view;

    private MulticastSocket socket;

    private boolean running = false;

    private Map<InetSocketAddress, ReliableNode> nodes = new HashMap<>();


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
            InetSocketAddress sourceAddress = (InetSocketAddress) datagram.getSocketAddress();

            // Camada transporte
            // pegar no seqid
            // possivelmente adicionar a uma fila de espera

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

                    // verifiar hash
                    // se nok entao para

                    // ack
                    this.receive(sourceAddress, packet);
                } catch(NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
                    ex.printStackTrace();
                }

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

    public void tick() {
        // runs every seconds
    }

    @Override
    public void send(Packet packet) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream(); DataOutputStream stream = new DataOutputStream(byteStream)) {

            // Cabeçalho da camada de transporte
            /*_lastOutgoingSequence++;

            // Allocate the memory
            HeapMemory memory = memoryManager.AllocHeapMemory((uint)payload.Count + 4);

            // Write headers
            memory.Buffer[0] = HeaderPacker.Pack(MessageType.Data);
            memory.Buffer[1] = channelId;

            // Write the sequence
            memory.Buffer[2] = (byte)_lastOutgoingSequence;
            memory.Buffer[3] = (byte)(_lastOutgoingSequence >> 8);

            // Copy the payload
            Buffer.BlockCopy(payload.Array, payload.Offset, memory.Buffer, 4, payload.Count);

            // Add the memory to pending
            _sendSequencer.Set(_lastOutgoingSequence, new PendingOutgoingPacket()
            {
                Attempts = 1,
                LastSent = NetTime.Now,
                FirstSent = NetTime.Now,
                Memory = memory,
                NotificationKey = notificationKey
            });

            // Allocate pointers
            HeapPointers pointers = memoryManager.AllocHeapPointers(1);

            // Point the first pointer to the memory
            pointers.Pointers[0] = memory;

            // Send the message to the router. Tell the router to NOT dealloc the memory as the channel needs it for resend purposes.
            ChannelRouter.SendMessage(pointers, false, connection, noMerge, memoryManager);
        }*/



            // Camada de aplicaçao

            Byte packetId = PacketRegistry.getIdFromPacket(packet.getClass());
            if (packetId == null) {
                throw new RuntimeException(String.format("Packet '%s' is not registered.", packet.getClass().getSimpleName()));
            }

            stream.writeByte(packetId);
            packet.serialize(stream);

            // Final da camada de transporte

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

    /*

     */

    public class ReliableNode {

    }
}
