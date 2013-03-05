package jadeCW;

import jade.content.lang.Codec;
import jade.content.onto.OntologyException;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class AllocateAppointment extends CyclicBehaviour {

    private final HospitalAgent hospitalAgent;

    public AllocateAppointment(HospitalAgent hospitalAgent) {
        this.hospitalAgent = hospitalAgent;
    }

    @Override
    public void action() {
        MessageTemplate mt = MessageTemplate.and(
                 MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
                 MessageTemplate.and(
                     MessageTemplate.MatchOntology(HospitalOntology.NAME),
                     MessageTemplate.and(
                             MessageTemplate.MatchLanguage(hospitalAgent.getCodec().getName()),
                             MessageTemplate.and(
                                     MessageTemplate.MatchContent(GlobalAgentConstants.APPOINTMENT_SERVICE_TYPE),
                                     MessageTemplate.MatchPerformative(ACLMessage.REQUEST)
                             )
                     )

                 )
         );

        ACLMessage message = hospitalAgent.receive(mt);

        if (message != null) {
            int freeAppointment = hospitalAgent.getFreeAppointment();
            AID sender = message.getSender();
            if (freeAppointment != GlobalAgentConstants.APPOINTMENT_NUMBERS_NOT_INITIALIZED) {
                hospitalAgent.setAppointment(freeAppointment, sender);
                proposeAppointment(sender, freeAppointment);
            } else {
                // there are no free appointments
                refuseAppointment(sender);
            }
        }

    }

    private void refuseAppointment(AID receiver) {

        ACLMessage refuseMessage = createResponseMessage(receiver, ACLMessage.REFUSE);
        hospitalAgent.send(refuseMessage);

    }

    private void proposeAppointment(AID receiver, Integer allocatedAppointment) {

        ACLMessage proposeMessage = createResponseMessage(receiver, ACLMessage.PROPOSE);
        Appointment appointment = new Appointment();
        appointment.setAllocation(allocatedAppointment);

        try {
            hospitalAgent.getContentManager().fillContent(proposeMessage,appointment);
        } catch (Codec.CodecException e) {
            throw new RuntimeException(e);
        } catch (OntologyException e) {
            throw new RuntimeException(e);
        }

        hospitalAgent.send(proposeMessage);

    }

    private ACLMessage createResponseMessage(AID receiver, int performative) {
        ACLMessage message = new ACLMessage(performative);
        message.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        message.setLanguage(hospitalAgent.getCodec().getName());
        message.setOntology(HospitalOntology.NAME);
        message.setContent(GlobalAgentConstants.APPOINTMENT_SERVICE_TYPE);
        message.addReceiver(receiver);
        return message;
    }
}
