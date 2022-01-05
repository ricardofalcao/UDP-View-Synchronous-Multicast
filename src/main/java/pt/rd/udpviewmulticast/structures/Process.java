package pt.rd.udpviewmulticast.structures;

import java.util.HashSet;
import java.util.Set;

public class Process {

    private final String id;

    private final Set<ProcessGroup> groups = new HashSet<>();

    /*

     */

    public Process(String id) {
        this.id = id;
    }

    /*

     */

    public String getId() {
        return id;
    }

    public Set<ProcessGroup> getGroups() {
        return groups;
    }

    /*

     */

    public void addGroup(ProcessGroup group) {
        this.groups.add(group);
    }

    public void removeGroup(ProcessGroup group) {
        this.groups.remove(group);
    }

    /*

     */
}
