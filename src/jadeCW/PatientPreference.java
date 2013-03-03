package jadeCW;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PatientPreference {

    private List<PreferenceLevel> preferenceLevels = new ArrayList<PreferenceLevel>();

    public PatientPreference() {
    }

    public PatientPreference(String preferenceSpecification) {
        String[] splitPrefs = preferenceSpecification.split("-");

        for (String splitPref : splitPrefs) {

            // trim the whitespace
            splitPref = splitPref.trim();

            if (!splitPref.equals("")) {
                // split these by the spaces
                PreferenceLevel preferenceLevel = new PreferenceLevel(splitPref);
                preferenceLevels.add(preferenceLevel);
            }
        }
    }


    public List<Integer> queryPreferredAllocations(int currentAllocation) {

        List<Integer> preferredAllocations = new ArrayList<Integer>();
        Iterator<PreferenceLevel> preferenceLevelIterator = preferenceLevels.iterator();
        boolean foundCurrentAllocationPosition = false;

        while (!foundCurrentAllocationPosition && preferenceLevelIterator.hasNext()) {

            PreferenceLevel preferenceLevel = preferenceLevelIterator.next();
            foundCurrentAllocationPosition = preferenceLevel.addPreferredAllocations(currentAllocation, preferredAllocations);

        }

        return preferredAllocations;
    }

    public boolean isAllocationSwapAcceptable(int newAllocation, int currentAllocation) {

        Iterator<PreferenceLevel> preferenceLevelIterator = preferenceLevels.iterator();

        while (preferenceLevelIterator.hasNext()) {

            PreferenceLevel preferenceLevel = preferenceLevelIterator.next();

            if(preferenceLevel.containsAllocation(newAllocation) && !preferenceLevel.containsAllocation(currentAllocation)) {
                return true;
            } else if(preferenceLevel.containsAllocation(currentAllocation)) {
                return false;
            }

        }

        return false;
    }

}
