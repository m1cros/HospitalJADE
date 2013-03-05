package jadeCW;

import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.onto.OntologyException;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class RespondToProposal2 extends CyclicBehaviour {

    private final HospitalAgent hospitalAgent;

    public RespondToProposal2(HospitalAgent hospitalAgent) {
        this.hospitalAgent = hospitalAgent;
    }


    @Override
    public void action() {

        MessageTemplate messageTemplate = MessageTemplate.and(
                MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE),
                MessageTemplate.and(
                        MessageTemplate.MatchOntology(HospitalOntology.NAME),
                        MessageTemplate.and(
                                MessageTemplate.MatchLanguage(hospitalAgent.getCodec().getName()),
                                MessageTemplate.MatchPerformative(ACLMessage.PROPOSE)
                        )
                )
        );

        ACLMessage message = hospitalAgent.receive(messageTemplate);

        if (message != null) {

            String conversationId = message.getConversationId();
            ContentElement p;
            AgentAllocationSwap agentAllocationSwap;

            try {
                p = hospitalAgent.getContentManager().extractContent(message);
            } catch (Codec.CodecException e) {
                throw new RuntimeException(e);
            } catch (OntologyException e) {
                throw new RuntimeException(e);
            }

            if (p instanceof AgentAllocationSwap) {
                agentAllocationSwap = (AgentAllocationSwap) p;
            } else {
                throw new RuntimeException();
            }

            if (hospitalAgent.isAppointmentFree(agentAllocationSwap.getDesiredAllocation())) {
            	// if the hospital still has the appointment to give (it is free)
            	// then accept the swap
                replyWithAcceptance(message,conversationId);
                hospitalAgent.removeAppointment(agentAllocationSwap.getCurrentAllocation());
                hospitalAgent.setAppointment(agentAllocationSwap.getDesiredAllocation(), message.getSender());

            } else {
            	// otherwise something has changed and the hospital agent no longer
            	// has the appointment to give.
                refuseSwapProposal(message,agentAllocationSwap,conversationId);

            }
        }
    }

    private void refuseSwapProposal(ACLMessage message, AgentAllocationSwap agentAllocationSwap, String conversationId) {

        ACLMessage refuseSwapMessage = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
        refuseSwapMessage.setProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);
        refuseSwapMessage.setLanguage(hospitalAgent.getCodec().getName());
        refuseSwapMessage.setOntology(HospitalOntology.NAME);
        refuseSwapMessage.addReceiver(message.getSender());
        refuseSwapMessage.setConversationId(conversationId);

        AppointmentQuery appointment = new AppointmentQuery();
        appointment.setAllocation(agentAllocationSwap.getDesiredAllocation());
        appointment.setState(GlobalAgentConstants.APPOINTMENT_QUERY_RESPONSE_STATUS_ALLOCATED);
        appointment.setHolder(hospitalAgent.getAppointmentHolderID(agentAllocationSwap.getDesiredAllocation()));

        try {
            hospitalAgent.getContentManager().fillContent(refuseSwapMessage, appointment);
        } catch (Codec.CodecException e) {
            throw new RuntimeException(e);
        } catch (OntologyException e) {
            throw new RuntimeException(e);
        }

        hospitalAgent.send(refuseSwapMessage);

    }

    private void replyWithAcceptance(ACLMessage message, String conversationId) {
   
        ACLMessage acceptSwapMessage = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
        acceptSwapMessage.setProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);
        acceptSwapMessage.setLanguage(hospitalAgent.getCodec().getName());
        acceptSwapMessage.setOntology(HospitalOntology.NAME);
        acceptSwapMessage.setConversationId(conversationId);
        acceptSwapMessage.addReceiver(message.getSender());

        hospitalAgent.send(acceptSwapMessage);

    }
}
