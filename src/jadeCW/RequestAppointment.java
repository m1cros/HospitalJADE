package jadeCW;

import jade.core.behaviours.Behaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class RequestAppointment extends Behaviour {

    private boolean isAllocated = false;
    private final DFPatientSubscription dfSubscription;
    private final PatientAgent patientAgent;

    public RequestAppointment(DFPatientSubscription dfSubscription, PatientAgent patientAgent) {
        this.dfSubscription = dfSubscription;
        this.patientAgent = patientAgent;
    }

    @Override
    public void action() {
        // finding agent for appointment-service
        DFAgentDescription appointmentAgentDescription = dfSubscription.getAgentDescription();

        if (appointmentAgentDescription != null) {
            // appointment-service agent can allocate appointment

            //Check that this agent (i.e. the parent agent of this behaviour) has not already been allocated an appointment
            if (!isAllocated) {

                requestAppointment(appointmentAgentDescription);
                receiveResponse();

            }
        }
    }

    private void requestAppointment(DFAgentDescription appointmentAgentDescription) {

        ACLMessage appointmentRequestMessage = new ACLMessage(ACLMessage.REQUEST);
        appointmentRequestMessage.addReceiver(appointmentAgentDescription.getName());

        patientAgent.send(appointmentRequestMessage);

    }

    private void receiveResponse() {

        ACLMessage message = patientAgent.blockingReceive(MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));

        int currentAllocation = Integer.parseInt(message.getContent());

        patientAgent.setCurrentAllocation(currentAllocation);
        isAllocated = true;
    }

    private void confirmAppointment(ACLMessage message) {


    }

    @Override
    public boolean done() {
        return isAllocated;
    }
}
