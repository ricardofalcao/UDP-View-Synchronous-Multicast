package pt.rd.udpviewmulticast.structures;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

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
            (byte) 230, (byte) 0, (byte) id, (byte) 0
        });
    }

    /*

     */

    public int getId() {
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
        Collection<InetAddress> newProcesses = Set.copyOf(members);
        newProcesses.add(process);

        return new View((byte) (this.id + 1), newProcesses);
    }

    public View removeMember(InetAddress process) throws UnknownHostException {
        Collection<InetAddress> newProcesses = Set.copyOf(members);
        newProcesses.remove(process);

        return new View((byte) (this.id + 1), newProcesses);
    }

    /*

     */
}
