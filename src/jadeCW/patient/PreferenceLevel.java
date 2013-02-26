package jadeCW.patient;

import java.util.Set;
import java.util.TreeSet;

public class PreferenceLevel {

    private Set<Integer> appointmentsId = new TreeSet<Integer>();

    public PreferenceLevel(String appointmentsLevel) {
        String[] appointments = appointmentsLevel.split(" ");

        for (String appointment : appointments) {
            appointmentsId.add(Integer.parseInt(appointment.trim()));
        }
    }

}
