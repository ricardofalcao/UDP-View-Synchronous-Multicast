package pt.rd.udpviewmulticast.communication.message;

import java.util.Objects;

public class StatMessage {

    private final byte viewId;

    private final short seq;

    /*

     */

    public StatMessage(byte viewId, short seq) {
        this.viewId = viewId;
        this.seq = seq;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StatMessage that = (StatMessage) o;
        return viewId == that.viewId &&
            seq == that.seq;
    }

    @Override
    public int hashCode() {
        return Objects.hash(viewId, seq);
    }
}
