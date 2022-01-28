package pt.rd.udpviewmulticast.communication.message;

import java.net.InetAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import pt.rd.udpviewmulticast.communication.Packet;
import pt.rd.udpviewmulticast.structures.View;

public class SentMessage {

    public static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    /*

     */

    private final short id;

    private final Packet packet;

    private final Collection<InetAddress> acknowledged = new HashSet<>();

    private final long sentTime;

    private ScheduledFuture timeout;

    /*

     */

    public SentMessage(short id, Packet packet) {
        this.id = id;
        this.packet = packet;
        this.sentTime = System.nanoTime();
    }

    /*

     */

    public void ack(InetAddress address) {
        this.acknowledged.add(address);
    }

    public boolean isStable(View view) {
        return this.acknowledged.containsAll(view.getMembers());
    }

    public boolean isStableIntr(View view, View view2) {
        Set<InetAddress> intersection = new HashSet<>(view.getMembers());
        intersection.retainAll(view2.getMembers());

        return this.acknowledged.containsAll(intersection);
    }

    public void scheduleTimeout(Runnable runnable, long value, TimeUnit unit) {
        this.timeout = scheduler.schedule(runnable, value, unit);
    }

    public void clearTimeout() {
        if (this.timeout != null) {
            this.timeout.cancel(true);
        }
    }

    /*

     */

    public short getId() {
        return id;
    }

    public Packet getPacket() {
        return packet;
    }

    public Collection<InetAddress> getAcknowledged() {
        return acknowledged;
    }

    public long getSentTime() {
        return sentTime;
    }

    /*

     */

    @Override
    public String toString() {
        return "SentMessage{" +
            "id=" + id +
            ", packet=" + packet +
            ", acknowledged=" + acknowledged +
            '}';
    }

    /*

     */
}
