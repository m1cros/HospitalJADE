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
            splitPref = splitPref.trim();
            if (!splitPref.equals("")) {
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

        if (preferenceLevels.isEmpty()) {
            // we don't care, so we agree to whatever
            return true;
        }

        Iterator<PreferenceLevel> preferenceLevelIterator = preferenceLevels.iterator();

        while (preferenceLevelIterator.hasNext()) {

            PreferenceLevel preferenceLevel = preferenceLevelIterator.next();

            if(preferenceLevel.containsAllocation(newAllocation)) {
                return true;
            } else if(preferenceLevel.containsAllocation(currentAllocation)) {
                return false;
            }

        }

        return false;
    }

}
