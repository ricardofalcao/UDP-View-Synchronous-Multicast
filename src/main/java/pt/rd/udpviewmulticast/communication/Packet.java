package pt.rd.udpviewmulticast.communication;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface Packet {

    void serialize(DataOutputStream outputStream) throws IOException;

    void deserialize(DataInputStream inputStream) throws IOException;


}
