package pt.rd.udpviewmulticast.communication.channels;

import pt.rd.udpviewmulticast.communication.Channel;
import pt.rd.udpviewmulticast.communication.Packet;
import pt.rd.udpviewmulticast.structures.View;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;

public class UnreliableChannel implements Channel {

    private View view;

    private MulticastSocket socket;

    /*

     */

    @Override
    public void open(View view) {
        this.view = view;

        try {
            this.socket = new MulticastSocket(5555);
            this.socket.joinGroup(this.view.getSubnetAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        if (this.socket != null) {
            this.socket.close();
        }
    }

    /*

     */

    @Override
    public void send(Packet packet) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream(); DataOutputStream stream = new DataOutputStream(byteStream)) {
            packet.serialize(stream);

            byte[] buf = byteStream.toByteArray();

            DatagramPacket datagram = new DatagramPacket(buf, buf.length, view.getSubnetAddress(), 5555);
            socket.send(datagram);
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void receive(Packet packet) {

    }
}
