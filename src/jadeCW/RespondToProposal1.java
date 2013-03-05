package jadeCW;

import jade.content.ContentElement;
import jade.content.Predicate;
import jade.content.lang.Codec;
import jade.content.onto.OntologyException;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPANames;
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
    	// get the appointment agent description
        DFAgentDescription appointmentAgentDescription = dfSubscription.getAgentDescription();

        // the apointment agent can be null, therefore if it is do nothing and wait
        // till there is an appointment agent.
        if (appointmentAgentDescription != null) {
        	// try to receive the swap request
            receiveSwapRequest();
        }
    }

    private void receiveSwapRequest() {

    	
        MessageTemplate messageTemplate = MessageTemplate.and(
                MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE),
                MessageTemplate.and(
                        MessageTemplate.MatchOntology(HospitalOntology.NAME),
                        MessageTemplate.and(
                                MessageTemplate.MatchLanguage(patientAgent.getCodec().getName()),
                                MessageTemplate.MatchPerformative(ACLMessage.PROPOSE)
                        )
                )
        );

        ACLMessage message = patientAgent.receive(messageTemplate);

        if (message != null) {

            ContentElement p;
            AgentAllocationSwap otherAgentAllocationSwap;
            String conversationId = message.getConversationId();

            try {
                p = patientAgent.getContentManager().extractContent(message);
            } catch (Codec.CodecException e) {
                throw new RuntimeException(e);
            } catch (OntologyException e) {
                throw new RuntimeException(e);
            }

            if (p instanceof AgentAllocationSwap) {
                otherAgentAllocationSwap = (AgentAllocationSwap) p;
            } else {
                throw new RuntimeException();
            }

            boolean requestedAppointmentNotInPossession
                    = patientAgent.getCurrentAllocation() != otherAgentAllocationSwap.getDesiredAllocation();
            boolean beneficialAppointment
                    = patientAgent.getPatientPreference()
                                  .isAllocationSwapAcceptable(otherAgentAllocationSwap.getCurrentAllocation(),
                                                              patientAgent.getCurrentAllocation());
            boolean hasMadeSwapProposal = patientAgent.hasMadeSwapProposal();

            if(requestedAppointmentNotInPossession) {
                AppointmentNotInPossession appointmentNotInPossession = new AppointmentNotInPossession();
                appointmentNotInPossession.setCurrentAppointment(patientAgent.getCurrentAllocation());
                refuseSwapProposal(message,appointmentNotInPossession, conversationId);

            } else if (!beneficialAppointment) {
                AppointmentNotPreferred appointmentNotPreferred = new AppointmentNotPreferred();
                refuseSwapProposal(message, appointmentNotPreferred, conversationId);

            } else if (hasMadeSwapProposal) {
                AgentAllocationSwap mySwap = patientAgent.getCurrentlyProposedAllocationSwap();
                if (isSameSwap(mySwap, otherAgentAllocationSwap)) {
                    if (mySwap.getTimestamp().compareTo(otherAgentAllocationSwap.getTimestamp()) < 0) {
                        // my swap was earlier so I refuse with the hope that the other agent accepts
                        AlreadySwappingAppointments alreadySwappingAppointments = new AlreadySwappingAppointments();
                        refuseSwapProposal(message, alreadySwappingAppointments, conversationId);
                    }
                    else {
                        acceptProposal(message, otherAgentAllocationSwap,
								conversationId);
                    }
                }
            } else {
                acceptProposal(message, otherAgentAllocationSwap,
						conversationId);
            }
        }
    }


	private void acceptProposal(ACLMessage message,
			AgentAllocationSwap otherAgentAllocationSwap, String conversationId) {
		AID hospitalAgentAID = dfSubscription.getAgentDescription().getName();
		replyWithAcceptance(message, conversationId);
		informHospitalAgentOfSwap(otherAgentAllocationSwap,hospitalAgentAID,message.getSender(), conversationId);
		receiveConfirmationFromHospitalAgent(conversationId,
		        otherAgentAllocationSwap.getCurrentAllocation(),hospitalAgentAID);
	}

    private void receiveConfirmationFromHospitalAgent(String timestamp, int allocation, AID hospital) {

        MessageTemplate messageTemplateConfirm = MessageTemplate.and(
                MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE),
                MessageTemplate.and(
                        MessageTemplate.MatchOntology(HospitalOntology.NAME),
                        MessageTemplate.and(
                                MessageTemplate.MatchLanguage(patientAgent.getCodec().getName()),
                                MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
                                        MessageTemplate.and(MessageTemplate.MatchConversationId(timestamp),
                                                MessageTemplate.MatchSender(hospital)))


                        )

                )
        );

        patientAgent.blockingReceive(messageTemplateConfirm);
        patientAgent.setCurrentAllocation(allocation);
        patientAgent.updatePreferredAllocations();

    }

    private void refuseSwapProposal(ACLMessage message, Predicate reason, String conversationId) {

        ACLMessage refuseSwapMessage = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
        refuseSwapMessage.setProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);
        refuseSwapMessage.setLanguage(patientAgent.getCodec().getName());
        refuseSwapMessage.setOntology(HospitalOntology.NAME);
        refuseSwapMessage.addReceiver(message.getSender());
        refuseSwapMessage.setConversationId(conversationId);

        try {
            patientAgent.getContentManager().fillContent(refuseSwapMessage, reason);
        } catch (Codec.CodecException e) {
            throw new RuntimeException(e);
        } catch (OntologyException e) {
            throw new RuntimeException(e);
        }

        patientAgent.send(refuseSwapMessage);

    }

    private void replyWithAcceptance(ACLMessage message, String conversationId) {

        ACLMessage acceptSwapMessage = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
        acceptSwapMessage.setProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);
        acceptSwapMessage.setLanguage(patientAgent.getCodec().getName());
        acceptSwapMessage.setOntology(HospitalOntology.NAME);
        acceptSwapMessage.addReceiver(message.getSender());
        acceptSwapMessage.setConversationId(conversationId);

        patientAgent.send(acceptSwapMessage);

    }

    private void informHospitalAgentOfSwap(AgentAllocationSwap agentAllocationSwap,
                                           AID hospitalAgent, AID swapProposalAgent, String timestamp) {

        ACLMessage acceptSwapMessage = new ACLMessage(ACLMessage.INFORM);
        acceptSwapMessage.setProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);
        acceptSwapMessage.setLanguage(patientAgent.getCodec().getName());
        acceptSwapMessage.setOntology(HospitalOntology.NAME);
        acceptSwapMessage.addReceiver(hospitalAgent);
        acceptSwapMessage.setConversationId(timestamp);

        AllocationSwapSummary allocationSwapSummary = new AllocationSwapSummary();
        allocationSwapSummary.setProposingAgentOldAppointment(agentAllocationSwap.getCurrentAllocation());
        allocationSwapSummary.setProposingAgent(swapProposalAgent.getName());
        allocationSwapSummary.setReceivingAgentOldAppointment(agentAllocationSwap.getDesiredAllocation());
        allocationSwapSummary.setReceivingAgent(patientAgent.getName());
        allocationSwapSummary.setTimestamp(timestamp);

        try {
            patientAgent.getContentManager().fillContent(acceptSwapMessage, allocationSwapSummary);

        } catch (Codec.CodecException e) {
            throw new RuntimeException(e);
        } catch (OntologyException e) {
            throw new RuntimeException(e);
        }

        patientAgent.send(acceptSwapMessage);

    }

    private boolean isSameSwap(AgentAllocationSwap swap1 , AgentAllocationSwap swap2) {
        // return true if swaps only differ by timestamp
        return swap1.getCurrentAllocation() == swap2.getDesiredAllocation()
                &&  swap1.getDesiredAllocation() == swap2.getCurrentAllocation();
    }

}
