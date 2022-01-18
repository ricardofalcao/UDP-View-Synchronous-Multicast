package pt.rd.udpviewmulticast.communication.packets;

import pt.rd.udpviewmulticast.communication.Packet;

import java.util.HashMap;
import java.util.Map;

public class PacketRegistry {

    private static Map<Byte, Class<? extends Packet>> byId = new HashMap<>();

    private static Map<Class<? extends Packet>, Byte> byClass = new HashMap<>();

    /*

     */

    public static void registerPacket(byte id, Class<? extends Packet> packetClass) {
        byId.put(id, packetClass);
        byClass.put(packetClass, id);
    }

    /*

     */

    public static Byte getIdFromPacket(Class<? extends Packet> packetClass) {
        return byClass.get(packetClass);
    }

    public static Class<? extends Packet> getPacketFromId(byte id) {
        return byId.get(id);
    }
}
