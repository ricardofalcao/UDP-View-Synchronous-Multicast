package pt.rd.udpviewmulticast.communication;

import pt.rd.udpviewmulticast.structures.View;

public interface Channel {

    void open(View view);

    void close();

    //

    void send(Packet packet);

    void receive(Packet packet);

}
