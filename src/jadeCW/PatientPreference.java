package jadeCW;

import java.util.ArrayList;
import java.util.List;

public class PatientPreference {

    private List<PreferenceLevel> preferenceLevels = new ArrayList<PreferenceLevel>();

    public PatientPreference() {}

    public PatientPreference(String preferenceSpecification) {
        String[] splitPrefs = preferenceSpecification.split("-");

        for (String splitPref : splitPrefs) {

            // trim the whitespace
            splitPref = splitPref.trim();

            // split these by the spaces
            PreferenceLevel preferenceLevel = new PreferenceLevel(splitPref);

            preferenceLevels.add(preferenceLevel);
        }
    }


}
