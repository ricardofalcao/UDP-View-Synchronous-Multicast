package pt.rd.udpviewmulticast.communication.message;

import java.net.InetAddress;
import pt.rd.udpviewmulticast.communication.Packet;

public class ReceivedMessage implements Comparable<ReceivedMessage> {

    private final InetAddress sourceAddress;

    private final short seq;

    private final Packet packet;

    /*

     */

    public ReceivedMessage(InetAddress sourceAddress, short seq, Packet packet) {
        this.sourceAddress = sourceAddress;
        this.seq = seq;
        this.packet = packet;
    }

    /*

     */

    public InetAddress getSourceAddress() {
        return sourceAddress;
    }

    public short getSeq() {
        return seq;
    }

    public Packet getPacket() {
        return packet;
    }

    /*

     */

    @Override
    public int compareTo(ReceivedMessage o) {
        return Short.compare(seq, o.seq);
    }
}
