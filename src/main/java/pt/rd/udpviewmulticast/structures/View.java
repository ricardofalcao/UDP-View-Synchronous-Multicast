package pt.rd.udpviewmulticast.structures;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class View {

    /*

     */

    private final byte id;

    private final Collection<InetAddress> members;
    
    private final InetAddress subnetAddress;

    /*

     */

    public View(byte id, Collection<InetAddress> members) throws UnknownHostException {
        this.id = id;
        this.members = Collections.unmodifiableCollection(members);
        this.subnetAddress = InetAddress.getByAddress(new byte[]{
            (byte) 230, (byte) 0, id, (byte) 0
        });
    }

    /*

     */

    public byte getId() {
        return id;
    }

    public Collection<InetAddress> getMembers() {
        return members;
    }

    public InetAddress getSubnetAddress() {
        return subnetAddress;
    }

    /*

     */

    public View addMember(InetAddress process) throws UnknownHostException {
        Collection<InetAddress> newProcesses = new HashSet<>(members);
        newProcesses.add(process);

        return new View((byte) (this.id + 1), newProcesses);
    }

    public View removeMember(InetAddress process) throws UnknownHostException {
        Collection<InetAddress> newProcesses = new HashSet<>(members);
        newProcesses.remove(process);

        return new View((byte) (this.id + 1), newProcesses);
    }

    /*

     */

    @Override
    public String toString() {
        return "View{" +
            "id=" + id +
            ", members={" + members.stream().map(InetAddress::getHostAddress).collect(Collectors.joining(", ")) +
            "}, subnetAddress=" + subnetAddress.getHostAddress() +
            '}';
    }
}
