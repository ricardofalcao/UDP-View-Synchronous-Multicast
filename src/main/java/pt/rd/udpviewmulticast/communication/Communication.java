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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;
import pt.rd.udpviewmulticast.communication.channels.Channel;
import pt.rd.udpviewmulticast.communication.channels.EnumChannels;
import pt.rd.udpviewmulticast.structures.View;

public class Communication {

    public static int DEFAULT_PORT = 5555;

    private static final Logger logger = Logger.getLogger("Communication");

    /*

     */

    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    //

    private MulticastSocket socket;

    private boolean running = false;

    private View currentView;

    /*

     */

    public void setup() throws IOException {
        this.socket = new MulticastSocket(DEFAULT_PORT);
        this.running = true;
    }

    public void listenForPackets() {
        byte[] buf = new byte[256];
        System.out.println("Started listening for packets.");

        while (this.running) {
            DatagramPacket datagram = new DatagramPacket(buf, buf.length);

            try {
                socket.receive(datagram);

                try (ByteArrayInputStream byteStream = new ByteArrayInputStream(datagram.getData(), datagram.getOffset(),
                    datagram.getLength())) {
                    try (DataInputStream dataStream = new DataInputStream(byteStream)) {
                        byte channelId = dataStream.readByte();

                        Class<? extends Channel> channelClass = EnumChannels.getChannelFromId(channelId);
                        if (channelClass == null) {
                            throw new IOException(String.format("Could not find channel from ID '%d'", channelId));
                        }

                        Channel channel = getChannel(channelClass);
                        Packet packet = channel.read(datagram.getAddress(), dataStream);
                        if (packet.shouldDebug()) {
                            logger.info(String.format("Received packet '%s' from '%s'", packet == null ? "null" : packet.toString(), datagram.getAddress().getHostAddress()));
                        }
                    }
                }

            } catch (IOException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void shutdown() {
        this.running = false;
        scheduler.shutdown();
    }

    /*

     */

    public void join(View view) throws IOException {
        this.leave();

        this.currentView = view;
        this.socket.joinGroup(view.getSubnetAddress());

        logger.info(String.format("Joined group with ID %d and ip '%s'.", view.getId(), view.getSubnetAddress().getHostAddress()));
    }

    private void leave() throws IOException {
        if (this.currentView != null) {
            this.socket.leaveGroup(this.currentView.getSubnetAddress());
            this.currentView = null;
        }
    }

    /*

     */

    private final Map<Class<? extends Channel>, Channel> channels = new HashMap<>();

    private Channel getChannel(Class<? extends Channel> channelClass)
        throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Channel channel = channels.get(channelClass);
        if (channel == null) {
            channel = channelClass.getDeclaredConstructor(Communication.class).newInstance(this);
            channels.put(channelClass, channel);
        }

        return channel;
    }

    public void sendPacket(Channel channel, Packet packet, InetAddress targetAddress, Object... args) throws IOException {
        Byte channelId = EnumChannels.getChannelId(channel.getClass());
        if (channelId == null) {
            throw new IOException(String.format("Could not find channel ID from '%s'", channel.getClass().getSimpleName()));
        }

        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream()) {
            try (DataOutputStream dataStream = new DataOutputStream(byteStream)) {
                dataStream.writeByte(channelId);
                channel.write(packet, dataStream, args);

                byte[] value = byteStream.toByteArray();
                DatagramPacket datagram = new DatagramPacket(value, value.length, targetAddress, DEFAULT_PORT);

                if (packet.shouldDebug()) {
                    logger.info(String.format("Sending packet '%s' to '%s'", packet.getClass().getSimpleName(), targetAddress.getHostAddress()));
                }

                this.socket.send(datagram);
            }
        }
    }

    public void sendPacket(Class<? extends Channel> channelClass, Packet packet, InetAddress targetAddress, Object... args)
        throws IOException {
        try {
            Channel channel = getChannel(channelClass);

            sendPacket(channel, packet, targetAddress, args);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            throw new IOException(ex);
        }
    }

    /*

     */

    public void multicastPacket(Channel channel, Packet packet) throws IOException {
        if (this.currentView != null) {
            sendPacket(channel, packet, this.currentView.getSubnetAddress());
        }
    }

    public void multicastPacket(Class<? extends Channel> channelClass, Packet packet) throws IOException {
        if (this.currentView != null) {
            sendPacket(channelClass, packet, this.currentView.getSubnetAddress());
        }
    }

    /*

     */

}
