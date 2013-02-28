package jadeCW;

import java.util.ArrayList;
import java.util.Iterator;
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


    public List<Integer> queryPreferredAllocations(int currentAllocation) {

        List<Integer> preferredAllocations = new ArrayList<Integer>();
        Iterator<PreferenceLevel> preferenceLevelIterator = preferenceLevels.iterator();
        boolean foundCurrentAllocationPosition = false;

        while(!foundCurrentAllocationPosition && preferenceLevelIterator.hasNext()) {

            PreferenceLevel preferenceLevel = preferenceLevelIterator.next();
            foundCurrentAllocationPosition = preferenceLevel.addPreferredAllocations(currentAllocation,preferredAllocations);

        }

        return preferredAllocations;
    }
}
