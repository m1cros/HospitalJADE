package jadeCW;

import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class RespondToProposal2 extends CyclicBehaviour {

    private final HospitalAgent hospitalAgent;

    public RespondToProposal2(HospitalAgent hospitalAgent) {
        this.hospitalAgent = hospitalAgent;
    }


    @Override
    public void action() {

        receiveChangeAppointmentMessage();
    }

    private void receiveChangeAppointmentMessage() {

        ACLMessage message = hospitalAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));

        if (message != null) {

            // get agentsAppointment, and thisAgentsAppointment
            int senderAppointment = 0;
            int receiverAppointment = 0;
            if (hospitalAgent.isAppointmentFree(receiverAppointment)) {
                // can change.
                replyWithAcceptance(message.getSender());

                // update the changes
                hospitalAgent.setAppointment(receiverAppointment, message.getSender());
            } else {
                // appointment has already been taken. Reject
                String ownerOfAppointment = hospitalAgent.getAppointmentHolderID(receiverAppointment);
                replyWithRejection(message.getSender(), ownerOfAppointment);
            }
        }
    }

    private void replyWithAcceptance(AID receiver) {

        ACLMessage acceptSwapMessage = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);

        acceptSwapMessage.addReceiver(receiver);
        acceptSwapMessage.setSender(hospitalAgent.getAID());

        hospitalAgent.send(acceptSwapMessage);

    }

    private void replyWithRejection(AID receiver, String holderOfAppointment) {

        ACLMessage rejectSwapMessage = new ACLMessage(ACLMessage.REJECT_PROPOSAL);

        rejectSwapMessage.setContent(holderOfAppointment);
        rejectSwapMessage.addReceiver(receiver);
        rejectSwapMessage.setSender(hospitalAgent.getAID());

        hospitalAgent.send(rejectSwapMessage);
    }
}
