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

        MessageTemplate mt = GlobalAgentConstants.getFipaHospitalTemplate(
                GlobalAgentConstants.APPOINTMENT_SERVICE_TYPE,
                ACLMessage.REQUEST,
                hospitalAgent.getCodec().getName());

        ACLMessage message = hospitalAgent.receive(mt);

        if (message != null) {
            int freeAppointment = hospitalAgent.getFreeAppointment();

            AID sender = message.getSender();
            if (freeAppointment != GlobalAgentConstants.APPOINTMENT_NUMBERS_NOT_INITIALIZED) {
                hospitalAgent.setAppointment(freeAppointment, sender);

                proposeAppointment(sender, freeAppointment);
            } else {
                refuseAppointment(sender);
            }
        }

    }

    private void refuseAppointment(AID receiver) {
        ACLMessage proposeMessage = new ACLMessage(ACLMessage.REFUSE);
        proposeMessage.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        proposeMessage.setLanguage(hospitalAgent.getCodec().getName());
        proposeMessage.setOntology(HospitalOntology.NAME);
        proposeMessage.setContent(GlobalAgentConstants.APPOINTMENT_SERVICE_TYPE);
        proposeMessage.addReceiver(receiver);

        hospitalAgent.send(proposeMessage);
    }

    private void proposeAppointment(AID receiver, Integer allocatedAppointment) {

        ACLMessage proposeMessage = new ACLMessage(ACLMessage.PROPOSE);
        proposeMessage.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        proposeMessage.setLanguage(hospitalAgent.getCodec().getName());
        proposeMessage.setOntology(HospitalOntology.NAME);
        proposeMessage.setContent(GlobalAgentConstants.APPOINTMENT_SERVICE_TYPE);
        proposeMessage.addReceiver(receiver);

        Appointment appointment = new Appointment();
        appointment.setAllocation(allocatedAppointment);
        try {

            hospitalAgent.getContentManager().fillContent(proposeMessage,appointment);
            // TODO
            // Umyj swiat i ta funkcje
        } catch (Codec.CodecException e) {
            throw new RuntimeException(e);
        } catch (OntologyException e) {
            throw new RuntimeException(e);
        }

        hospitalAgent.send(proposeMessage);

    }
}
