package jadeCW;

import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.Iterator;
import java.util.List;

public class ProposeSwap extends Behaviour {

    private boolean didSwap = false;
    private final PatientAgent patientAgent;
    private final DFPatientSubscription dfSubscription;


    public ProposeSwap(DFPatientSubscription dfSubscription, PatientAgent patientAgent) {
        this.patientAgent = patientAgent;
        this.dfSubscription = dfSubscription;
    }

    @Override
    public void action() {

        // finding agent for appointment-service
        DFAgentDescription appointmentAgentDescription = dfSubscription.getAgentDescription();

        if (appointmentAgentDescription != null) {

            List<AllocationState> allocationStates = patientAgent.getAllocationStates();
            Iterator<AllocationState> allocationStateIterator = allocationStates.iterator();

            while (allocationStateIterator.hasNext() && !didSwap) {

                AllocationState allocationState = allocationStateIterator.next();

                if (allocationState.getAppointmentStatus() == GlobalAgentConstants.APPOINTMENT_QUERY_RESPONSE_STATUS_ALLOCATED) {
                    // need to send to the allocation holder
                    String allocationHolderName = allocationState.getAppointmentHolder();
                    AID allocationHolderAID = new AID(allocationHolderName, AID.ISLOCALNAME);
                    requestSwapWithAgent(allocationHolderAID);
                    receiveResponse(appointmentAgentDescription.getName(), allocationState.getAppointment());

                } else if (allocationState.getAppointmentStatus() == GlobalAgentConstants.APPOINTMENT_QUERY_RESPONSE_STATUS_FREE) {
                    // need to talk to hospital
                    requestSwapWithAgent(appointmentAgentDescription.getName());
                    receiveResponse(appointmentAgentDescription.getName(), allocationState.getAppointment());

                } else {
                    // if there is another kind of query response status
                    throw new RuntimeException();
                }
            }
        }
    }

    private void requestSwapWithAgent(AID agent) {
        ACLMessage swapMessage = new ACLMessage(ACLMessage.PROPOSE);

        swapMessage.addReceiver(agent);
        swapMessage.setSender(patientAgent.getAID());

        patientAgent.send(swapMessage);
    }

    private void receiveResponse(AID hospitalAgent, int requestedAllocation) {

        MessageTemplate messageTemplate = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
                MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL));

        ACLMessage message = patientAgent.blockingReceive(messageTemplate);

        if (message.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
            // THE AGENT HAS ACCEPTED!
            int newAllocation = Integer.parseInt(message.getContent());
            int oldAllocation = patientAgent.getCurrentAllocation();
            patientAgent.setCurrentAllocation(newAllocation);

            // Inform the HospitalAgent that the two agents have swapped appointments,
            // if a swap was made with an agent that is not a HospitalAgent.
            AID sender = message.getSender();
            if (sender != hospitalAgent) {
                // the sender is not the hospitalAgent
                informHospitalAgentOfSwap(oldAllocation, newAllocation, hospitalAgent);
            }

            didSwap = true;
        }  else {
            // this is a rejection
            if (message.getSender() == hospitalAgent) {
                //update who has this allocation
                String ownerOfAllocation = message.getContent();
                patientAgent.getPatientPreference().updatePreferenceHolder(requestedAllocation, ownerOfAllocation);
            }
        }
    }

    private void informHospitalAgentOfSwap(int oldAllocation, int newAllocation, AID hospitalAgent) {

        ACLMessage notifyMessage = new ACLMessage(ACLMessage.INFORM);
        // change the content to be more logical.
        notifyMessage.setContent("" + oldAllocation + "," + newAllocation);

        notifyMessage.addReceiver(hospitalAgent);
        notifyMessage.setSender(patientAgent.getAID());

        patientAgent.send(notifyMessage);

    }


    @Override
    public boolean done() {
        return didSwap;
    }
}
