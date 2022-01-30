package pt.rd.udpviewmulticast.communication.message;

import java.net.InetAddress;
import pt.rd.udpviewmulticast.communication.Packet;
import pt.rd.udpviewmulticast.utils.VClock;

public class ReceivedMessage implements Comparable<ReceivedMessage> {

    private final InetAddress sourceAddress;

    private final short seq;

    private final VClock<InetAddress> clock;

    private final Packet packet;

    private long receivedTime;

    /*

     */

    public ReceivedMessage(InetAddress sourceAddress, VClock<InetAddress> clock, short seq, Packet packet) {
        this.sourceAddress = sourceAddress;
        this.clock = clock;
        this.seq = seq;
        this.packet = packet;
        this.receivedTime = System.nanoTime();
    }

    /*

     */

    public InetAddress getSourceAddress() {
        return sourceAddress;
    }

    public short getSeq() {
        return seq;
    }

    public VClock<InetAddress> getClock() {
        return clock;
    }

    public Packet getPacket() {
        return packet;
    }

    public long getReceivedTime() {
        return receivedTime;
    }

    /*

     */

    @Override
    public int compareTo(ReceivedMessage o) {
        return Short.compare(seq, o.seq);
    }
}
