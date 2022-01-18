package pt.rd.udpviewmulticast.structures;

import java.net.InetAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class View {

    private final int id;

    private final ProcessGroup group;

    private final Collection<Process> members;
    
    private final InetAddress subnetAddress;

    /*

     */

    public View(int id, ProcessGroup group, Collection<Process> members, InetAddress subnetAddress) {
        this.id = id;
        this.group = group;
        this.members = Collections.unmodifiableCollection(members);
        this.subnetAddress = subnetAddress;
    }

    /*

     */

    public int getId() {
        return id;
    }

    public Collection<Process> getMembers() {
        return members;
    }

    public InetAddress getSubnetAddress() {
        return subnetAddress;
    }

    /*

     */

    public Process getMember(String id) {
        for (Process member : members) {
            if (member.getId().equals(id)) {
                return member;
            }
        }

        return null;
    }

    public View addMember(Process process) {
        Collection<Process> newProcesses = Set.copyOf(members);
        newProcesses.add(process);

        return new View(this.id + 1, this.group, newProcesses, subnetAddress);
    }

    public View removeMember(Process process) {
        Collection<Process> newProcesses = Set.copyOf(members);
        newProcesses.remove(process);

        return new View(this.id + 1, this.group, newProcesses, subnetAddress);
    }

    /*

     */
}
