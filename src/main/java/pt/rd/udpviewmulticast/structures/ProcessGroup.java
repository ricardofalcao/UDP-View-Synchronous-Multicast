package pt.rd.udpviewmulticast.structures;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;

public class ProcessGroup {

    private final String name;

    private View activeView;

    /*

     */

    public ProcessGroup(String name) throws UnknownHostException {
        this.name = name;
        this.activeView = new View(0, this, Collections.emptyList(), InetAddress.getByName("230.0.0.0"));
    }

    /*

     */

    public String getName() {
        return name;
    }

    /*

     */

    public void addMember(Process process) {
        this.activeView = this.activeView.addMember(process);
    }

    public void removeMember(Process process) {
        this.activeView = this.activeView.removeMember(process);
    }
}
