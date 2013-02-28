package jadeCW;

import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class RespondToProposal1 extends CyclicBehaviour {

    private final PatientAgent patientAgent;

    private final DFPatientSubscription dfSubscription;

    public RespondToProposal1(DFPatientSubscription dfSubscription, PatientAgent patientAgent) {
        this.patientAgent = patientAgent;
        this.dfSubscription = dfSubscription;
    }


    @Override
    public void action() {

        DFAgentDescription appointmentAgentDescription = dfSubscription.getAgentDescription();

        if (appointmentAgentDescription != null) {
            receiveSwapRequest();
        }
    }

    private void receiveSwapRequest() {

        ACLMessage message = patientAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));

        if (message != null) {

            // get agentsAppointment, and thisAgentsAppointment
            int senderAppointment = 0;
            int receiverAppointment = 0;
            if (patientAgent.getCurrentAllocation() != receiverAppointment) {
                // refuse! since we have already changed our allocation.
            }

            if (patientAgent.getPatientPreference().isAllocationSwapAcceptable(senderAppointment, patientAgent.getCurrentAllocation())) {
                // do the swap, and reply
                // notify the hospital agent
                replyWithAcceptance(message.getSender());
                informHospitalAgentOfSwap(patientAgent.getCurrentAllocation(), senderAppointment, dfSubscription.getAgentDescription().getName());

                // set the swap
                patientAgent.setCurrentAllocation(senderAppointment);
            } else {
                // swap not acceptable, reply
                replyWithRejection(message.getSender());
            }


        }

    }

    private void replyWithAcceptance(AID receiver) {

        ACLMessage acceptSwapMessage = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);

        acceptSwapMessage.addReceiver(receiver);
        acceptSwapMessage.setSender(patientAgent.getAID());

        patientAgent.send(acceptSwapMessage);

    }

    private void replyWithRejection(AID receiver) {

        ACLMessage rejectSwapMessage = new ACLMessage(ACLMessage.REJECT_PROPOSAL);

        rejectSwapMessage.addReceiver(receiver);
        rejectSwapMessage.setSender(patientAgent.getAID());

        patientAgent.send(rejectSwapMessage);
    }

    private void informHospitalAgentOfSwap(int oldAllocation, int newAllocation, AID hospitalAgent) {

        ACLMessage notifyMessage = new ACLMessage(ACLMessage.INFORM);
        // change the content to be more logical.
        notifyMessage.setContent("" + oldAllocation + "," + newAllocation);

        notifyMessage.addReceiver(hospitalAgent);
        notifyMessage.setSender(patientAgent.getAID());

        patientAgent.send(notifyMessage);

    }

}
