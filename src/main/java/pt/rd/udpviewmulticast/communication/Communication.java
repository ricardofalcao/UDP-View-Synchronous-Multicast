package pt.rd.udpviewmulticast.communication;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import pt.rd.udpviewmulticast.Main;
import pt.rd.udpviewmulticast.communication.message.ReceivedMessage;
import pt.rd.udpviewmulticast.communication.message.SentMessage;
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

    public static long PACKET_FREQUENCY = 100;

    public static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    /*

     */

    protected final Logger logger = Logger.getLogger(this.getClass().getSimpleName());

    /*

     */

    protected MulticastSocket socket;

    protected boolean running = false;

    protected CommunicationState state = CommunicationState.JOINING;

    protected View currentView, pendingView;

    protected ScheduledFuture changeViewTimeout;

    //

    protected Queue<Packet> pendingPackets = new LinkedList<>();

    protected Map<Short, SentMessage> sentPackets = new HashMap<>();

    protected short lastSentSeq = 0;

    //

    protected Map<InetAddress, Node> nodes = new HashMap<>();

    // stats

    protected int stabilizedPacketsAmount = 0;

    protected int sentPacketsAmount = 0;

    protected int retries = 0;

    protected long viewStartTime = -1;

    protected long lastViewTime = -1;

    protected long stabilizeRtt = 0;

    protected long smoothStabilizeRtt = 0;

    //================================================================================
    // Initialization methods
    //================================================================================

    protected int bindPort() {
        return DEFAULT_PORT;
    }

    public void setup() throws IOException {
        this.socket = new MulticastSocket(bindPort());
        this.socket.setReuseAddress(true);
        this.running = true;

        floodPackets();
    }

    public void floodPackets() {
        System.out.println("Flooding packets");
        PacketHello hello = new PacketHello(5);

        scheduler.scheduleAtFixedRate(() -> {
            if (this.state == CommunicationState.NORMAL) {
                try {
                    multicastPacket(hello);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, (long) (Math.random() * 1_000_000), 1_000_000 / PACKET_FREQUENCY, TimeUnit.MICROSECONDS);

        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (this.state == CommunicationState.NORMAL) {
                    String message = String.format("%d;%s;%d;%d;%d;%d;%d;%d;%d;%d\n",
                        System.currentTimeMillis(),
                        Main.IP,
                        this.currentView.getId(),
                        this.currentView.getMembers().size(),
                        this.lastViewTime,
                        this.getPacketsSent(),
                        this.getPacketsAck(),
                        this.retries,
                        this.stabilizeRtt,
                        this.getSmoothStabilizeRtt()
                    );

                    this.lastViewTime = -1;

                    Main.LOG.write(
                        message
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 200, TimeUnit.MILLISECONDS);
    }

    public void listenForPackets() {
        byte[] buf = new byte[508];
        DatagramPacket datagram = new DatagramPacket(buf, buf.length);

        System.out.println(String.format("Started listening for packets [%d]", socket.getLocalPort()));

        while (this.running) {
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

        SentMessage.scheduler.shutdown();
    }

    //================================================================================
    // Packet events
    //================================================================================

    protected boolean acceptUnknownSources() {
        return this.state == CommunicationState.JOINING;
    }

    protected void onSend(InetAddress address, short seq, Packet packet) throws IOException {
        if (packet.shouldDebug()) {
            logger.info(String.format("Sent packet '%d:%s' to '%s'.", seq, packet.toString(), address.getHostAddress()));
        }

        if (packet instanceof PacketACK) {
            return;
        }

        SentMessage message = new SentMessage(seq, null, packet);
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

    protected void onReceive(InetAddress sourceAddress, short seq, Packet packet) throws IOException {
        if (packet.shouldDebug()) {
            logger.info(String.format("Received packet '%d:%s' from '%s'.", seq, packet,
                sourceAddress.getHostAddress()));
        }

        if (packet instanceof PacketACK) {
            this.onReceiveACK(sourceAddress, seq, packet);
            return;
        }

        this.ack(sourceAddress, seq);

        Node node = this.nodes.get(sourceAddress);
        if (node == null) {
            //throw new IOException(String.format("Unknown node from ip '%s'", sourceAddress.getHostAddress()));
            if (acceptUnknownSources()) {
                node = new Node(sourceAddress);
                this.nodes.put(sourceAddress, node);
            } else {
                return;
            }

        }

        ReceivedMessage message = new ReceivedMessage(sourceAddress, null, seq, packet);
        node.storePacket(message);

        Collection<ReceivedMessage> deliverable = node.getDeliverablePackets();
        for (ReceivedMessage deliver : deliverable) {
            node.deliverPacket(deliver);

            this.onDeliver(deliver);
        }
    }

    protected void onReceiveACK(InetAddress sourceAddress, short seq, Packet ack) throws IOException {
        SentMessage message = this.sentPackets.get(seq);
        if (message == null) {
            return;
        }

        message.markAck(sourceAddress);

        if (state == CommunicationState.JOINING) {
            message.clearTimeout();

            this.sentPackets.remove(seq);
            return;
        }

        if (
            (this.state == CommunicationState.NORMAL && message.isStable(this.currentView)) ||
                (this.state == CommunicationState.FLUSHING && message.isStableIntr(this.currentView, this.pendingView))
        ) {
            message.clearTimeout();

            this.sentPackets.remove(seq);
            this.stabilizedPacketsAmount++;

            long nanos = System.nanoTime() - message.getSentTime();

            float factor = 0.875f;
            this.stabilizeRtt = nanos;
            this.smoothStabilizeRtt = (long) (this.smoothStabilizeRtt * factor + nanos * (1 - factor));
        }

        if (this.state == CommunicationState.FLUSHING && this.sentPackets.isEmpty()) {
            this.state = CommunicationState.FLUSHED;

            checkFlush();
        }
    }

    protected void onDeliver(ReceivedMessage message) throws IOException {
        Packet packet = message.getPacket();

        if (packet.shouldDebug()) {
            logger.info(String.format("Delivered packet '%d:%s' from '%s'.", message.getSeq(), message.getPacket(),
                message.getSourceAddress().getHostAddress()));
        }

        if (packet instanceof PacketNewView) {
            PacketNewView newView = (PacketNewView) packet;

            View view = newView.getView();

            if (this.state == CommunicationState.JOINING) {
                this.join(view);
                return;
            }

            this.pendingView = view;
            this.state = CommunicationState.FLUSHING;

            this.sentPackets.entrySet().removeIf(entry -> entry.getValue().isStableIntr(this.currentView, this.pendingView));
            logger.info("Sending flush packet. " + this.state);

            multicastPacket(new PacketFlush());

            if (this.changeViewTimeout != null) {
                this.changeViewTimeout.cancel(true);
            }

            this.changeViewTimeout = scheduler.schedule(() -> {
                try {
                    this.join(pendingView);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }, 1, TimeUnit.SECONDS);

            return;
        }

        if (packet instanceof PacketFlush) {
            Node node = this.nodes.get(message.getSourceAddress());

            node.setFlushed(true);

            checkFlush();
            return;
        }

        if (packet instanceof PacketLeave) {
            if (Main.LEADER) {
                View newView = this.currentView.removeMember(message.getSourceAddress());
                this.multicastPacket(new PacketNewView(newView));
                return;
            }
        }
    }

    public void timerView() {
        this.viewStartTime = System.nanoTime();
    }

    //================================================================================
    // Flush methods
    //================================================================================

    protected void checkFlush() throws IOException {
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

    //================================================================================
    // Membership methods
    //================================================================================

    public void join(View view) throws IOException {
        if (viewStartTime > 0) {
            this.lastViewTime = System.nanoTime() - viewStartTime;
            this.viewStartTime = -1;
        }

        this.leave();

        this.currentView = view;
        this.socket.joinGroup(view.getSubnetAddress());
        this.socket.setLoopbackMode(false);

        this.sentPackets.clear();
        this.lastSentSeq = 0;

        for (InetAddress member : this.currentView.getMembers()) {
            this.nodes.put(member, new Node(member));
        }

        logger.info(String.format("Joined group with ID %d and ip '%s'.", view.getId(), view.getSubnetAddress().getHostAddress()));

        this.state = CommunicationState.NORMAL;

        try {
            Packet packet;
            while ((packet = this.pendingPackets.poll()) != null) {
                multicastPacket(packet);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        /*scheduler.schedule(() -> {
        }, 10, TimeUnit.MILLISECONDS);*/
    }

    protected void leave() throws IOException {
        if (this.currentView != null) {
            this.socket.leaveGroup(this.currentView.getSubnetAddress());
            this.currentView = null;
            this.nodes.clear();
        }
    }

    //

    public void requestJoin() throws IOException {
        this.state = CommunicationState.JOINING;

        this.socket.setLoopbackMode(true);
        this.multicastPacket(new PacketJoin());
    }

    public void requestLeave() throws IOException {
        this.socket.setLoopbackMode(true);
        this.multicastPacket(new PacketLeave());
    }

    //================================================================================
    // Packet sending methods
    //================================================================================

    public int targetPort() {
        return this.state == CommunicationState.JOINING ? DEFAULT_PORT + 1 : DEFAULT_PORT;
    }

    public void sendPacket(short seq, Packet packet, InetAddress targetAddress) throws IOException {
        if (packet.shouldQueue() && this.state != CommunicationState.NORMAL && this.state != CommunicationState.JOINING) {
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
                DatagramPacket datagram = new DatagramPacket(value, value.length, targetAddress, this.targetPort());

                boolean virgin = !this.sentPackets.containsKey(seq);

                this.onSend(targetAddress, seq, packet);
                this.socket.send(datagram);

                if (!(packet instanceof PacketACK)) {
                    if (this.state != CommunicationState.JOINING && currentView != null) {
                        if (virgin) {
                            sentPacketsAmount++;
                        }
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

    protected void ack(InetAddress targetAddress, short seq) throws IOException {
        this.multicastPacket(targetAddress, seq, new PacketACK());
    }

    //================================================================================
    // Getters & Setters
    //================================================================================

    public Map<InetAddress, Node> getNodes() {
        return nodes;
    }

    public short getLastSentSeq() {
        return lastSentSeq;
    }

    public int getPacketsSent() {
        return sentPacketsAmount;
    }

    public int getPacketsAck() {
        return stabilizedPacketsAmount;
    }

    public float getPacketLoss() {
        return 1 - (float) getPacketsAck() / (float) getPacketsSent();
    }

    public int getRetries() {
        return retries;
    }

    public long getSmoothStabilizeRtt() {
        return smoothStabilizeRtt;
    }
}
