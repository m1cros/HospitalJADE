package jadeCW;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class PreferenceLevel {

    private Set<Integer> appointmentsId = new TreeSet<Integer>();

    public PreferenceLevel(String appointmentsLevel) {
        String[] appointments = appointmentsLevel.split("\\s+");

        for (String appointment : appointments) {
            appointmentsId.add(Integer.parseInt(appointment.trim()));
        }
    }

    public boolean addPreferredAllocations(int currentAllocation, List<Integer> preferredAllocations) {

        boolean hasPreferredAllocations = appointmentsId.contains(currentAllocation);

        if(!hasPreferredAllocations) {
            preferredAllocations.addAll(appointmentsId);
        }

        return hasPreferredAllocations;

    }

    public boolean containsAllocation(int newAllocation) {
        return appointmentsId.contains(newAllocation);
    }

    public String toString() {
        return appointmentsId.toString();
    }
}
