package jadeCW;

import jade.core.behaviours.Behaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import java.util.List;

/**
 * This behaviour is triggered only once, after the initialization of the agent
 */
public class FindAppointmentOwner extends Behaviour {

    private final DFPatientSubscription dfSubscription;
    private final PatientAgent patientAgent;

    private boolean allocationStatesSet;

    public FindAppointmentOwner(DFPatientSubscription dfSubscription, PatientAgent patientAgent) {
        this.dfSubscription = dfSubscription;
        this.patientAgent = patientAgent;
        allocationStatesSet = false;
    }

    @Override
    public void action() {
        DFAgentDescription appointmentAgentDescription = dfSubscription.getAgentDescription();
        if (appointmentAgentDescription != null) {
            AllocationFinder allocFinder = patientAgent.getAllocationFinder();
            List<AllocationState> preferredAllocations
                    = allocFinder.getAllPreferredAllocations(appointmentAgentDescription,
                                                             patientAgent.getCurrentAllocation());

            patientAgent.setAllocationStates(preferredAllocations);
            allocationStatesSet = true;
        }
    }

    @Override
    public boolean done() {
        return allocationStatesSet;
    }
}
