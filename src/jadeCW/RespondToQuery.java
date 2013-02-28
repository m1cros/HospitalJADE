package jadeCW;

import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.onto.OntologyException;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class RespondToQuery extends CyclicBehaviour {

    private final HospitalAgent hospitalAgent;

    public RespondToQuery(HospitalAgent hospitalAgent) {
        this.hospitalAgent = hospitalAgent;
    }

    @Override
    public void action() {

        MessageTemplate messageTemplate = MessageTemplate.and(
                MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_QUERY),
                MessageTemplate.and(
                        MessageTemplate.MatchOntology(HospitalOntology.NAME),
                        MessageTemplate.and(
                                MessageTemplate.MatchLanguage(hospitalAgent.getCodec().getName()),
                                MessageTemplate.MatchPerformative(ACLMessage.QUERY_REF)

                        )

                )
        );

        ACLMessage message = hospitalAgent.receive(messageTemplate);

        if (message != null) {
            int allocation;

            ContentElement p = null;
            try {
                p = hospitalAgent.getContentManager().extractContent(message);
            } catch (Codec.CodecException e) {
                throw new RuntimeException(e);
            } catch (OntologyException e) {
                throw new RuntimeException(e);
            }

            if (p instanceof Appointment) {
                Appointment appointment = (Appointment) p;

                allocation = appointment.getAllocation();

            } else {
                throw new RuntimeException();
            }

            ACLMessage messageResponse = new ACLMessage(ACLMessage.QUERY_IF);
            messageResponse.setSender(hospitalAgent.getAID());
            messageResponse.addReceiver(message.getSender());

            messageResponse.setProtocol(FIPANames.InteractionProtocol.FIPA_QUERY);
            messageResponse.setLanguage(hospitalAgent.getCodec().getName());
            messageResponse.setOntology(HospitalOntology.NAME);

            AppointmentQuery appointmentQuery = new AppointmentQuery();
            appointmentQuery.setAllocation(allocation);

            if (allocation < 0 || allocation >= hospitalAgent.getAppointmentsNum()) {

                appointmentQuery.setState(GlobalAgentConstants.APPOINTMENT_QUERY_RESPONSE_STATUS_INVALID);

            } else if (hospitalAgent.isAppointmentFree(allocation)) {

                appointmentQuery.setState(GlobalAgentConstants.APPOINTMENT_QUERY_RESPONSE_STATUS_FREE);

            } else {

                appointmentQuery.setState(GlobalAgentConstants.APPOINTMENT_QUERY_RESPONSE_STATUS_ALLOCATED);
                appointmentQuery.setHolder(hospitalAgent.getAppointmentHolderID(allocation));

            }

            try {

                hospitalAgent.getContentManager().fillContent(messageResponse, appointmentQuery);
                // TODO
                // Umyj swiat i ta funkcje
            } catch (Codec.CodecException e) {
                throw new RuntimeException(e);
            } catch (OntologyException e) {
                throw new RuntimeException(e);
            }

            hospitalAgent.send(messageResponse);
        }

    }

}
