package pt.rd.udpviewmulticast.communication.channels;

import java.util.HashMap;
import java.util.Map;

public enum EnumChannels {

    UNRELIABLE((byte) 1, UnreliableChannel.class),
    RELIABLE_NEG((byte) 2, ReliableNegChannel.class);

    /*

     */

    private final byte id;

    private final Class<? extends Channel> channelClass;

    /*

     */

    EnumChannels(byte id, Class<? extends Channel> channelClass) {
        this.id = id;
        this.channelClass = channelClass;
    }

    /*

     */

    private static final Map<Byte, Class<? extends Channel>> idToChannel = new HashMap<>();

    private static final Map<Class<? extends Channel>, Byte> channelToId = new HashMap<>();

    static {
        for(EnumChannels channel : EnumChannels.values()) {
            idToChannel.put(channel.id, channel.channelClass);
            channelToId.put(channel.channelClass, channel.id);
        }
    }

    public static Byte getChannelId(Class<? extends Channel> channelClass) {
        return channelToId.get(channelClass);
    }

    public static Class<? extends Channel> getChannelFromId(byte id) {
        return idToChannel.get(id);
    }

}
