package pt.rd.udpviewmulticast.communication;

import pt.rd.udpviewmulticast.structures.View;

import java.net.InetSocketAddress;

public interface Channel {

    void open(View view);

    void listenForPackets();

    void close();

    //

    void send(Packet packet);

    void receive(InetSocketAddress address, Packet packet);

}
