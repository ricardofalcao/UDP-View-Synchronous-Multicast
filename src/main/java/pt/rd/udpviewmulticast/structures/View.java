package pt.rd.udpviewmulticast.structures;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class View {

    private final int id;

    private final ProcessGroup group;

    private final Collection<Process> members;

    /*

     */

    public View(int id, ProcessGroup group, Collection<Process> members) {
        this.id = id;
        this.group = group;
        this.members = Collections.unmodifiableCollection(members);
    }

    /*

     */

    public int getId() {
        return id;
    }

    public Collection<Process> getMembers() {
        return members;
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

        return new View(this.id + 1, this.group, newProcesses);
    }

    public View removeMember(Process process) {
        Collection<Process> newProcesses = Set.copyOf(members);
        newProcesses.remove(process);

        return new View(this.id + 1, this.group, newProcesses);
    }

    /*

     */
}
