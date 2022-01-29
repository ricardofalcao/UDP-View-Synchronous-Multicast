package pt.rd.udpviewmulticast.communication;

public enum CommunicationState {

    // When the node does not have a view associated
    JOINING,
    // Normal function
    NORMAL,
    // When a new view is pending, and unstable messages are flushing
    FLUSHING,
    // When the node has flushed everything, and is waiting for the acks from the other nodes
    FLUSHED

}
