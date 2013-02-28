package jadeCW;

import jade.core.behaviours.Behaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FindAppointmentOwner extends Behaviour {

    private final DFPatientSubscription dfSubscription;
    private final PatientAgent patientAgent;
    private final PatientPreference patientPreference;

    private boolean allocationStatesSet;

    public FindAppointmentOwner(DFPatientSubscription dfSubscription, PatientAgent patientAgent, PatientPreference patientPreference) {
        this.dfSubscription = dfSubscription;
        this.patientAgent = patientAgent;
        this.patientPreference = patientPreference;

        allocationStatesSet = false;
    }

    @Override
    public void action() {
        /* Hospital agent */
        DFAgentDescription appointmentAgentDescription = dfSubscription.getAgentDescription();

        if (appointmentAgentDescription != null) {

            int currentAllocation = patientAgent.getCurrentAllocation();
            List<AllocationState> allocationStates = new ArrayList<AllocationState>();

            /* 1. query patient preference for better appointment - patient could have no appointment? */
            List<Integer> preferredAllocationsOverCurrent = patientPreference.queryPreferredAllocations(currentAllocation);
            Iterator<Integer> preferredAllocationIterator = preferredAllocationsOverCurrent.iterator();
            boolean foundPreferredAllocation = false;


            System.out.println("Allocations tested by: " + patientAgent.getLocalName());
            while (preferredAllocationIterator.hasNext() && !foundPreferredAllocation) {
                int preferredAllocation = preferredAllocationIterator.next();
                /* 2. ask hospital agent who has got that preference */

                queryPreferredAllocation(appointmentAgentDescription, preferredAllocation);

                AllocationState allocationState = receivePreferredAllocationResponse(preferredAllocation);

                if (allocationState.getAppointmentStatus() == GlobalAgentConstants.APPOINTMENT_QUERY_RESPONSE_STATUS_INVALID) {
                    allocationStates.add(allocationState);
                }

                System.out.println(patientAgent.getLocalName() + ": allocation state: " + allocationState.toString());
            }

            patientAgent.setAllocationStates(allocationStates);
            allocationStatesSet = true;

        }
    }

    private void queryPreferredAllocation(DFAgentDescription appointmentAgentDescription, int preferredAllocation) {
        ACLMessage allocationQueryMessage = new ACLMessage(ACLMessage.QUERY_IF);
        allocationQueryMessage.setSender(patientAgent.getAID());
        allocationQueryMessage.addReceiver(appointmentAgentDescription.getName());
        allocationQueryMessage.addUserDefinedParameter(GlobalAgentConstants.APPOINTMENT_QUERY_FIELD, preferredAllocation + "");

        patientAgent.send(allocationQueryMessage);
    }

    private AllocationState receivePreferredAllocationResponse(int preferredAllocation) {
        /* receiving response... */
        ACLMessage allocationQueryResponseMessage = patientAgent.blockingReceive(MessageTemplate.MatchPerformative(ACLMessage.QUERY_REF));

        String responseStatus = allocationQueryResponseMessage.getUserDefinedParameter(GlobalAgentConstants.APPOINTMENT_QUERY_RESPONSE_STATUS);
        String patientAgentHolding = null;

        if (responseStatus.equals(GlobalAgentConstants.APPOINTMENT_QUERY_RESPONSE_STATUS_ALLOCATED)) {
            /* Allocated appointment */
            patientAgentHolding = allocationQueryResponseMessage.getUserDefinedParameter(GlobalAgentConstants.APPOINTMENT_QUERY_RESPONSE_AGENT_ID);
        }

        AllocationState allocatedState = new AllocationState(preferredAllocation, responseStatus, patientAgentHolding);

        return allocatedState;
    }

    @Override
    public boolean done() {
        return allocationStatesSet;
    }
}
