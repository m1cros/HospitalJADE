package jadeCW;

import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.onto.OntologyException;
import jade.core.behaviours.Behaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 * Class RequestAppointment requests an initial
 * appointment from hospital agent
 */
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
        /* Finding hospital agent */
        DFAgentDescription appointmentAgentDescription = dfSubscription.getAgentDescription();

        if (appointmentAgentDescription != null) {

            if (!isAllocated) {
                /* Request appointment from hospital agent */
                requestAppointment(appointmentAgentDescription);

                /* Wait for response */
                receiveResponse();
            }
        }
    }

    private void requestAppointment(DFAgentDescription appointmentAgentDescription) {

        ACLMessage appointmentRequestMessage = new ACLMessage(ACLMessage.REQUEST);
        appointmentRequestMessage.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        appointmentRequestMessage.setLanguage(patientAgent.getCodec().getName());
        appointmentRequestMessage.setOntology(HospitalOntology.NAME);
        appointmentRequestMessage.setContent(GlobalAgentConstants.APPOINTMENT_SERVICE_TYPE);

        appointmentRequestMessage.addReceiver(appointmentAgentDescription.getName());
        patientAgent.send(appointmentRequestMessage);

    }

    private void receiveResponse() {

        MessageTemplate messageTemplatePropose = MessageTemplate.and(
                MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
                MessageTemplate.and(
                        MessageTemplate.MatchOntology(HospitalOntology.NAME),
                        MessageTemplate.and(
                                MessageTemplate.MatchLanguage(patientAgent.getCodec().getName()),
                                MessageTemplate.MatchPerformative(ACLMessage.PROPOSE)

                        )

                )
        );

        MessageTemplate messageTemplateRefuse = MessageTemplate.and(
                MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
                MessageTemplate.and(
                        MessageTemplate.MatchOntology(HospitalOntology.NAME),
                        MessageTemplate.and(
                                MessageTemplate.MatchLanguage(patientAgent.getCodec().getName()),
                                MessageTemplate.MatchPerformative(ACLMessage.REFUSE)

                        )

                )
        );

        MessageTemplate messageTemplate = MessageTemplate.or(messageTemplatePropose,messageTemplateRefuse);


        ACLMessage message = patientAgent.blockingReceive(messageTemplate);

        if (message.getPerformative() == ACLMessage.PROPOSE) {

            ContentElement p;
            try {
                p = patientAgent.getContentManager().extractContent(message);
            } catch (Codec.CodecException e) {
                throw new RuntimeException(e);
            } catch (OntologyException e) {
                throw new RuntimeException(e);
            }

            if (p instanceof Appointment) {
                Appointment appointment = (Appointment) p;
                patientAgent.setCurrentAllocation(appointment.getAllocation());
                isAllocated = true;
            }
        }
    }

    @Override
    public boolean done() {
        return isAllocated;
    }
}
