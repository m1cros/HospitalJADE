package jadeCW;

import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class AllocateAppointment extends CyclicBehaviour {

    private final HospitalAgent hospitalAgent;

    public AllocateAppointment(HospitalAgent hospitalAgent) {
        this.hospitalAgent = hospitalAgent;
    }

    @Override
    public void action() {

        ACLMessage message = hospitalAgent.blockingReceive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

        int freeAppointment = hospitalAgent.getFreeAppointment();

        AID sender = message.getSender();
        if (freeAppointment != GlobalAgentConstants.APPOINTMENT_NUMBERS_NOT_INITIALIZED) {
            hospitalAgent.setAppointment(freeAppointment, sender);

            proposeAppointment(sender, freeAppointment);
        } else {
            refuseAppointment(sender);
        }

    }

    private void refuseAppointment(AID receiver) {
        ACLMessage proposeMessage = new ACLMessage(ACLMessage.REFUSE);
        proposeMessage.addReceiver(receiver);

        hospitalAgent.send(proposeMessage);
    }

    private void proposeAppointment(AID receiver, Integer allocatedAppointment) {

        ACLMessage proposeMessage = new ACLMessage(ACLMessage.PROPOSE);
        proposeMessage.setContent(allocatedAppointment.toString());
        proposeMessage.addReceiver(receiver);

        hospitalAgent.send(proposeMessage);

    }
}
