package pt.rd.udpviewmulticast.communication;

import com.google.common.base.Suppliers;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import pt.rd.udpviewmulticast.Main;
import pt.rd.udpviewmulticast.communication.message.ReceivedMessage;
import pt.rd.udpviewmulticast.communication.message.SentMessage;
import pt.rd.udpviewmulticast.communication.message.StatMessage;
import pt.rd.udpviewmulticast.communication.node.Node;
import pt.rd.udpviewmulticast.communication.packets.PacketACK;
import pt.rd.udpviewmulticast.communication.packets.PacketFlush;
import pt.rd.udpviewmulticast.communication.packets.PacketHello;
import pt.rd.udpviewmulticast.communication.packets.PacketJoin;
import pt.rd.udpviewmulticast.communication.packets.PacketLeave;
import pt.rd.udpviewmulticast.communication.packets.PacketNewView;
import pt.rd.udpviewmulticast.communication.packets.PacketRegistry;
import pt.rd.udpviewmulticast.structures.View;

public class Communication {

    public static int DEFAULT_PORT = 5555;

    public static InetAddress LEADER_SUBNET;

    static {
        try {
            LEADER_SUBNET = InetAddress.getByAddress(new byte[]{
                (byte) 230, (byte) 0, (byte) 0, (byte) 0
            });
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    //

    private static final Logger logger = Logger.getLogger("Communication");

    public static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    /*

     */

    private MulticastSocket socket;

    private boolean running = false;

    private CommunicationState state = CommunicationState.JOINING;

    private View currentView, pendingView;

    //

    private Queue<Packet> pendingPackets = new LinkedList<>();

    private Map<Short, SentMessage> sentPackets = new HashMap<>();

    private short lastSentSeq = 0;

    private ScheduledFuture changeViewTimeout;

    //

    private Map<InetAddress, Node> nodes = new HashMap<>();

    //

    private final Set<StatMessage> stabilizedPacketsSeq = new HashSet<>();

    private final Set<StatMessage> sentPacketsSeq = new HashSet<>();

    private int retries = 0;

    /*

     */

    public void setup() throws IOException {
        this.socket = new MulticastSocket(DEFAULT_PORT);

        if (Main.LEADER) {
            this.socket.joinGroup(LEADER_SUBNET);
        }

        this.running = true;

        if (!Main.LEADER) {
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    multicastPacket(new PacketHello(5));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }, (long) (Math.random() * 1000), 1, TimeUnit.MILLISECONDS);
        }
    }

    public void listenForPackets() {
        byte[] buf = new byte[256];
        System.out.println(Main.LEADER ? "Started listening for packets as the leader." : "Started listening for packets.");

        while (this.running) {
            DatagramPacket datagram = new DatagramPacket(buf, buf.length);

            try {
                socket.receive(datagram);

                try (ByteArrayInputStream byteStream = new ByteArrayInputStream(datagram.getData(), datagram.getOffset(),
                    datagram.getLength())) {
                    try (DataInputStream dataStream = new DataInputStream(byteStream)) {
                        short seq = dataStream.readShort();

                        byte packetId = dataStream.readByte();
                        Class<? extends Packet> packetClass = PacketRegistry.getPacketFromId(packetId);
                        if (packetClass == null) {
                            throw new IOException(String.format("Packet with ID %d is not registered.", packetId));
                        }

                        try {
                            Packet packet = packetClass.getDeclaredConstructor().newInstance();
                            packet.deserialize(dataStream);

                            this.onReceive(datagram.getAddress(), seq, packet);
                        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
                            throw new IOException(ex);
                        }
                    }
                }

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void shutdown() {
        this.running = false;

        scheduler.shutdown();
        SentMessage.scheduler.shutdown();
    }

    /*
        EVENTS
     */

    private void onSend(InetAddress address, short seq, Packet packet) throws IOException {
        if (packet.shouldDebug()) {
            logger.info(String.format("Sent packet '%d:%s' to '%s'.", seq, packet.toString(), address.getHostAddress()));
        }

        if (packet instanceof PacketACK) {
            return;
        }

        SentMessage message = new SentMessage(seq, packet);
        if (!this.sentPackets.containsKey(message.getId())) {
            this.sentPackets.put(message.getId(), message);
        }

        message.scheduleTimeout(() -> {
            try {
                if (packet.shouldDebug()) {
                    logger.warning(String.format("Timeout packet '%d:%s' to '%s'.", seq, packet.toString(), address.getHostAddress()));
                }

                retries++;
                sendPacket(seq, packet, address);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 100, TimeUnit.MILLISECONDS);
    }

    private void onReceive(InetAddress sourceAddress, short seq, Packet packet) throws IOException {
        if (Main.LEADER) {
            if (packet instanceof PacketJoin) {
                this.sendPacket(seq, new PacketACK(), sourceAddress);

                View newView = this.currentView.addMember(sourceAddress);

                this.sendPacket(lastSentSeq++, new PacketNewView(newView), sourceAddress);
                this.multicastPacket(lastSentSeq, new PacketNewView(newView));

                return;
            }

            if (packet instanceof PacketLeave) {
                this.sendPacket(seq, new PacketACK(), sourceAddress);

                View newView = this.currentView.removeMember(sourceAddress);

                this.multicastPacket(new PacketNewView(newView));

                return;
            }
        }

        if (packet instanceof PacketACK) {
            SentMessage message = this.sentPackets.get(seq);
            if (message == null) {
                return;
            }

            message.ack(sourceAddress);

            if (message.getPacket() instanceof PacketJoin || message.getPacket() instanceof PacketNewView || message
                .getPacket() instanceof PacketLeave) {
                message.clearTimeout();

                this.sentPackets.remove(seq);
            } else if (this.state == CommunicationState.NORMAL && message.isStable(this.currentView)) {
                //logger.info(String.format("Stable message '%d'.", seq));
                message.clearTimeout();

                this.sentPackets.remove(seq);
                this.stabilizedPacketsSeq.add(new StatMessage(currentView.getId(), seq));
            } else if (this.state == CommunicationState.FLUSHING && message.isStableIntr(this.currentView, this.pendingView)) {
                //logger.info(String.format("Stable message '%d'.", seq));
                message.clearTimeout();

                this.sentPackets.remove(seq);
                this.stabilizedPacketsSeq.add(new StatMessage(currentView.getId(), seq));
            }

            if (this.state == CommunicationState.FLUSHING && this.sentPackets.isEmpty()) {
                this.state = CommunicationState.FLUSHED;

                checkFlush();

            }

            return;
        }

        if (currentView == null && packet instanceof PacketNewView) {
            this.sendPacket(seq, new PacketACK(), sourceAddress);

            PacketNewView newView = (PacketNewView) packet;
            this.join(newView.getView());

            return;
        }

        Node node = this.nodes.get(sourceAddress);
        if (node == null) {
            //throw new IOException(String.format("Unknown node from ip '%s'", sourceAddress.getHostAddress()));
            return;
        }

        this.sendPacket(seq, new PacketACK(), sourceAddress);

        ReceivedMessage message = new ReceivedMessage(sourceAddress, seq, packet);
        node.storePacket(message);

        if (message.getPacket().shouldDebug()) {
            logger.info(String.format("Received packet '%d:%s' from '%s'.", message.getSeq(), message.getPacket(),
                message.getSourceAddress().getHostAddress()));
        }

        if (packet instanceof PacketFlush) {
            System.out.println(String.format("[%d] Flush %d/%d - %s", seq, node.getLastReceivedSeq(), node.getLastReceivedSeq(), node.getAddress().getHostAddress()));
        }

        Collection<ReceivedMessage> deliverable = node.getDeliverablePackets();
        for (ReceivedMessage deliver : deliverable) {
            node.deliverPacket(deliver);

            this.onDeliver(deliver);
        }
    }

    private void onDeliver(ReceivedMessage message) throws IOException {
        Packet packet = message.getPacket();

        if (packet.shouldDebug()) {
            logger.info(String.format("Delivered packet '%d:%s' from '%s'.", message.getSeq(), message.getPacket(),
                message.getSourceAddress().getHostAddress()));
        }

        if (packet instanceof PacketNewView) {
            PacketNewView newView = (PacketNewView) packet;

            View view = newView.getView();

            this.pendingView = view;
            this.state = CommunicationState.FLUSHING;

            logger.info("Sending flush packet. " + this.state);

            multicastPacket(new PacketFlush());

            if (this.changeViewTimeout != null) {
                this.changeViewTimeout.cancel(true);
            }

            this.changeViewTimeout = scheduler.schedule(() -> {
                /*try {
                    this.join(pendingView);
                } catch (IOException e) {
                    e.printStackTrace();
                }*/
            }, 1, TimeUnit.SECONDS);
        }

        if (packet instanceof PacketFlush) {
            Node node = this.nodes.get(message.getSourceAddress());

            node.setFlushed(true);

            checkFlush();
        }
    }

    private void checkFlush() throws IOException {
        if (state != CommunicationState.FLUSHED) {
            return;
        }

        boolean allFlushed = true;
        for (Node otherNode : this.nodes.values()) {
            if (pendingView.getMembers().contains(otherNode.getAddress()) && !otherNode.hasFlushed()) {
                allFlushed = false;
                break;
            }
        }

        if (allFlushed) {
            if (this.changeViewTimeout != null) {
                this.changeViewTimeout.cancel(true);
            }

            logger.info("All nodes flushed, changing view.");
            this.join(pendingView);
        }
    }

    /*

     */

    public void join(View view) throws IOException {
        this.leave();

        this.currentView = view;
        this.socket.joinGroup(view.getSubnetAddress());

        this.sentPackets.clear();
        this.lastSentSeq = 0;

        for (InetAddress member : this.currentView.getMembers()) {
            this.nodes.put(member, new Node(member));
        }

        logger.info(String.format("Joined group with ID %d and ip '%s'.", view.getId(), view.getSubnetAddress().getHostAddress()));

        scheduler.schedule(() -> {
            try {
                this.state = CommunicationState.NORMAL;

                Packet packet;
                while((packet = this.pendingPackets.poll()) != null) {
                    multicastPacket(packet);
                }
            } catch(IOException ex) {
                ex.printStackTrace();
            }
        }, 10, TimeUnit.MILLISECONDS);
    }

    private void leave() throws IOException {
        if (this.currentView != null) {
            this.socket.leaveGroup(this.currentView.getSubnetAddress());
            this.currentView = null;
            this.nodes.clear();
        }
    }

    /*

     */

    public void sendPacket(short seq, Packet packet, InetAddress targetAddress) throws IOException {
        if (packet.shouldQueue() && this.state != CommunicationState.NORMAL && this.state != CommunicationState.JOINING) {
            //System.out.println("add message to queue");
            this.pendingPackets.add(packet);
            return;
        }

        Byte packetId = PacketRegistry.getIdFromPacket(packet.getClass());
        if (packetId == null) {
            throw new IOException(String.format("Packet '%s' is not registered.", packet.getClass().getSimpleName()));
        }

        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream()) {
            try (DataOutputStream dataStream = new DataOutputStream(byteStream)) {
                dataStream.writeShort(seq);
                dataStream.writeByte(packetId);
                packet.serialize(dataStream);

                byte[] value = byteStream.toByteArray();
                DatagramPacket datagram = new DatagramPacket(value, value.length, targetAddress, DEFAULT_PORT);

                this.onSend(targetAddress, seq, packet);
                this.socket.send(datagram);

                if (!(packet instanceof PacketACK)) {
                    if (this.state != CommunicationState.JOINING && currentView != null) {
                        sentPacketsSeq.add(new StatMessage(currentView.getId(), seq));
                    }
                }
            }
        }
    }

    /*

     */

    public void multicastPacket(InetAddress targetAddress, short seq, Packet packet) throws IOException {
        sendPacket(seq, packet, targetAddress);
    }

    public void multicastPacket(short seq, Packet packet) throws IOException {
        if (this.currentView != null) {
            multicastPacket(this.currentView.getSubnetAddress(), seq, packet);
        }
    }

    public void multicastPacket(InetAddress targetAddress, Packet packet) throws IOException {
        lastSentSeq++;
        sendPacket(lastSentSeq, packet, targetAddress);
    }

    public void multicastPacket(Packet packet) throws IOException {
        if (this.currentView != null) {
            multicastPacket(this.currentView.getSubnetAddress(), packet);
        }
    }

    /*

     */

    public Map<InetAddress, Node> getNodes() {
        return nodes;
    }

    public short getLastSentSeq() {
        return lastSentSeq;
    }

    public int getPacketsSent() {
        return sentPacketsSeq.size();
    }

    public int getPacketsAck() {
        return stabilizedPacketsSeq.size();
    }

    public float getPacketLoss() {
        return 1 - (float) getPacketsAck() / (float) getPacketsSent();
    }

    public int getRetries() {
        return retries;
    }
}
