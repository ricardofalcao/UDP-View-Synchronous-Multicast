package pt.rd.udpviewmulticast.communication.node;

import java.net.InetAddress;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.TreeMap;
import pt.rd.udpviewmulticast.communication.message.ReceivedMessage;
import pt.rd.udpviewmulticast.utils.VClock;

public class Node {

    private final InetAddress address;

    private final TreeMap<Short, ReceivedMessage> receivedPackets = new TreeMap<>();

    private short lastDeliveredSeq = -1, lastReceivedSeq = -1;

    private boolean flushed = false;

    /*

     */

    public Node(InetAddress address) {
        this.address = address;
    }

    /*

     */

    public void storePacket(ReceivedMessage message) {
        this.lastReceivedSeq = (short) Math.max(this.lastReceivedSeq, message.getSeq());
        this.receivedPackets.put(message.getSeq(), message);
    }

    public void deliverPacket(ReceivedMessage message) {
        this.lastDeliveredSeq = message.getSeq();
        this.receivedPackets.remove(message.getSeq());
    }

    /*

     */

    public Collection<ReceivedMessage> getDeliverablePackets(VClock<InetAddress> clock) {
        Collection<ReceivedMessage> output = new LinkedHashSet<>();

        short lastSeq = lastDeliveredSeq;
        if (lastSeq == -1) {
            if (this.receivedPackets.isEmpty()) {
                return output;
            }

            Map.Entry<Short, ReceivedMessage> entry = this.receivedPackets.firstEntry();

            lastSeq = entry.getKey();
            output.add(entry.getValue());
        }

        ReceivedMessage message;
        while((message = this.receivedPackets.get(++lastSeq)) != null) {
            output.add(message);
        }

        if (clock != null) {
            output.removeIf((msg) -> {
                if (msg.getClock() == null) {
                    return true;
                }

                return msg.getClock().isNext(clock);
            });
        }

        return output;
    }

    /*

     */

    public InetAddress getAddress() {
        return address;
    }

    public short getLastDeliveredSeq() {
        return lastDeliveredSeq;
    }

    public short getLastReceivedSeq() {
        return lastReceivedSeq;
    }

    public boolean hasFlushed() {
        return flushed;
    }

    public void setFlushed(boolean flushed) {
        this.flushed = flushed;
    }

    /*

     */

    @Override
    public String toString() {
        return "Node{" +
            "address=" + address +
            ", lastDeliveredSeq=" + lastDeliveredSeq +
            ", lastReceivedSeq=" + lastReceivedSeq +
            '}';
    }
}
