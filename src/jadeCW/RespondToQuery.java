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

            ContentElement p;
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

            // create the response to the query 
            ACLMessage messageResponse = new ACLMessage(ACLMessage.QUERY_IF);
            messageResponse.setSender(hospitalAgent.getAID());
            messageResponse.addReceiver(message.getSender());
            messageResponse.setProtocol(FIPANames.InteractionProtocol.FIPA_QUERY);
            messageResponse.setLanguage(hospitalAgent.getCodec().getName());
            messageResponse.setOntology(HospitalOntology.NAME);

            AppointmentQuery appointmentQuery = new AppointmentQuery();
            appointmentQuery.setAllocation(allocation);

            if (allocation < 1 || allocation > hospitalAgent.getAppointmentsNum()) {
            	// if the allocation is out of bounds
                appointmentQuery.setState(GlobalAgentConstants.APPOINTMENT_QUERY_RESPONSE_STATUS_INVALID);

            } else if (hospitalAgent.isAppointmentFree(allocation)) {
            	// if the appointment is free and can be taken from the hospital agent
                appointmentQuery.setState(GlobalAgentConstants.APPOINTMENT_QUERY_RESPONSE_STATUS_FREE);

            } else {
            	// otherwise the appointment has been allocated to another agent
            	// notify who is holding this appointment (setHolder)
                appointmentQuery.setState(GlobalAgentConstants.APPOINTMENT_QUERY_RESPONSE_STATUS_ALLOCATED);
                appointmentQuery.setHolder(hospitalAgent.getAppointmentHolderID(allocation));
            }

            try {
                hospitalAgent.getContentManager().fillContent(messageResponse, appointmentQuery);
            } catch (Codec.CodecException e) {
                throw new RuntimeException(e);
            } catch (OntologyException e) {
                throw new RuntimeException(e);
            }

            hospitalAgent.send(messageResponse);
        }

    }

}
