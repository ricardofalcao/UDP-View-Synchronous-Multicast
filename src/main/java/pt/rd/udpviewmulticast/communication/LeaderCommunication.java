package pt.rd.udpviewmulticast.communication;

import java.io.IOException;
import java.net.InetAddress;
import pt.rd.udpviewmulticast.Main;
import pt.rd.udpviewmulticast.communication.message.ReceivedMessage;
import pt.rd.udpviewmulticast.communication.message.SentMessage;
import pt.rd.udpviewmulticast.communication.packets.PacketJoin;
import pt.rd.udpviewmulticast.communication.packets.PacketLeave;
import pt.rd.udpviewmulticast.communication.packets.PacketNewView;
import pt.rd.udpviewmulticast.structures.View;

public class LeaderCommunication extends Communication {

    private final Communication nodeCommunication;

    public LeaderCommunication(Communication nodeCommunication) {
        this.nodeCommunication = nodeCommunication;
    }

    /*

     */

    @Override
    public void floodPackets() {

    }

    @Override
    protected int bindPort() {
        return super.bindPort() + 1;
    }

    @Override
    protected boolean acceptUnknownSources() {
        return true;
    }

    @Override
    protected void onDeliver(ReceivedMessage message) throws IOException {
        Packet packet = message.getPacket();
        InetAddress sourceAddress = message.getSourceAddress();

        if (packet.shouldDebug()) {
            logger.info(String.format("Delivered packet '%d:%s' from '%s'.", message.getSeq(), message.getPacket(),
                message.getSourceAddress().getHostAddress()));
        }

        if (packet instanceof PacketJoin) {
            View newView = nodeCommunication.currentView.addMember(sourceAddress);

            nodeCommunication.multicastPacket(new PacketNewView(newView));
            this.multicastPacket(sourceAddress, new PacketNewView(newView));
            nodeCommunication.timerView();

            return;
        }
    }

    @Override
    protected void onReceiveACK(InetAddress sourceAddress, short seq, Packet ack) throws IOException {
        SentMessage message = this.sentPackets.get(seq);
        if (message == null) {
            return;
        }

        if (message.getPacket() instanceof PacketNewView) {
            message.clearTimeout();

            this.sentPackets.remove(seq);
            this.nodes.remove(sourceAddress);
            return;
        }

        super.onReceiveACK(sourceAddress, seq, ack);
    }
}
