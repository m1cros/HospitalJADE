package jadeCW;

import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.onto.OntologyException;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.*;


public class UpdateAppointments extends CyclicBehaviour {

    private final Set<AllocationSwapSummary> swappedAppointments = new HashSet<AllocationSwapSummary>();
    private final HospitalAgent hospitalAgent;

    public UpdateAppointments(HospitalAgent hospitalAgent) {
        this.hospitalAgent = hospitalAgent;
    }

    @Override
    public void action() {

        MessageTemplate messageTemplateAccept = MessageTemplate.and(
                MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE),
                MessageTemplate.and(
                        MessageTemplate.MatchOntology(HospitalOntology.NAME),
                        MessageTemplate.and(
                                MessageTemplate.MatchLanguage(hospitalAgent.getCodec().getName()),
                                MessageTemplate.MatchPerformative(ACLMessage.INFORM)
                        )
                )
        );

        ACLMessage message = hospitalAgent.receive(messageTemplateAccept);
        if (message != null) {

            ContentElement p;
            AllocationSwapSummary allocationSwapSummary;

            try {
                p = hospitalAgent.getContentManager().extractContent(message);
            } catch (Codec.CodecException e) {
                throw new RuntimeException(e);
            } catch (OntologyException e) {
                throw new RuntimeException(e);
            }

            if (p instanceof AllocationSwapSummary) {
                allocationSwapSummary = (AllocationSwapSummary) p;
            } else {
                throw new RuntimeException();
            }

            AID proposingAgentAID = new AID(allocationSwapSummary.getProposingAgent(), AID.ISGUID);
            AID receivingAgentAID = new AID(allocationSwapSummary.getReceivingAgent(), AID.ISGUID);

            if (swappedAppointments.contains(allocationSwapSummary)) {
            	// there already has been an appointment by one agent,
            	// now since a new message came it means that both agents
            	// have sent the swap appointment message, thus confirming to the hospital agent
                hospitalAgent.setAppointment(allocationSwapSummary.getReceivingAgentOldAppointment(), proposingAgentAID);
                hospitalAgent.setAppointment(allocationSwapSummary.getProposingAgentOldAppointment(), receivingAgentAID);
                // remove the already existing swap appointment
                swappedAppointments.remove(allocationSwapSummary);

                //send the confirmation messages
                confirmSwap(allocationSwapSummary.getReceivingAgent(),message.getConversationId());
                confirmSwap(allocationSwapSummary.getProposingAgent(),message.getConversationId());

            } else {
            	// have to add the appointment to be able to match it to 
            	// a confirmation coming from the other agent
                swappedAppointments.add(allocationSwapSummary);
            }
        }

    }

    private void confirmSwap(String agent, String timestamp) {
    	// send a message to the agent confirming the swap
        ACLMessage appointmentRequestMessage = new ACLMessage(ACLMessage.CONFIRM);
        appointmentRequestMessage.setProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);
        appointmentRequestMessage.setLanguage(hospitalAgent.getCodec().getName());
        appointmentRequestMessage.setOntology(HospitalOntology.NAME);
        appointmentRequestMessage.setConversationId(timestamp);

        appointmentRequestMessage.addReceiver(new AID(agent,AID.ISGUID));
        hospitalAgent.send(appointmentRequestMessage);

    }

}
