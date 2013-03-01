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

        DFAgentDescription appointmentAgentDescription = dfSubscription.getAgentDescription();

        if (appointmentAgentDescription != null) {
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
            AgentAllocationSwap agentAllocationSwap;
            String timestamp = message.getConversationId();

            try {
                p = patientAgent.getContentManager().extractContent(message);
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


            if(patientAgent.getCurrentAllocation() != agentAllocationSwap.getDesiredAllocation()) {

                AppointmentNotInPossession appointmentNotInPossession = new AppointmentNotInPossession();
                appointmentNotInPossession.setCurrentAppointment(patientAgent.getCurrentAllocation());
                refuseSwapProposal(message,appointmentNotInPossession,timestamp);

            } else if (patientAgent.getPatientPreference().isAllocationSwapAcceptable(agentAllocationSwap.getCurrentAllocation(),patientAgent.getCurrentAllocation())) {

                replyWithAcceptance(message,timestamp);
                informHospitalAgentOfSwap(agentAllocationSwap,dfSubscription.getAgentDescription().getName(),message.getSender(),timestamp);
                receiveConfirmationFromHospitalAgent(timestamp,agentAllocationSwap.getCurrentAllocation());

            } else {

                AppointmentNotPreferred appointmentNotPreferred = new AppointmentNotPreferred();
                refuseSwapProposal(message, appointmentNotPreferred, timestamp);

            }
        }
    }

    private void receiveConfirmationFromHospitalAgent(String timestamp, int allocation) {

        MessageTemplate messageTemplateConfirm = MessageTemplate.and(
                MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE),
                MessageTemplate.and(
                        MessageTemplate.MatchOntology(HospitalOntology.NAME),
                        MessageTemplate.and(
                                MessageTemplate.MatchLanguage(patientAgent.getCodec().getName()),
                                MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
                                        MessageTemplate.MatchConversationId(timestamp))


                        )

                )
        );

        System.out.println(patientAgent.getLocalName() + "awaiting confirmation...");
        patientAgent.blockingReceive(messageTemplateConfirm);
        System.out.println(patientAgent.getLocalName() + "got confirmation...");
        patientAgent.setCurrentAllocation(allocation);

    }

    private void refuseSwapProposal(ACLMessage message, Predicate reason, String timestamp) {

        ACLMessage refuseSwapMessage = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
        refuseSwapMessage.setProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);
        refuseSwapMessage.setLanguage(patientAgent.getCodec().getName());
        refuseSwapMessage.setOntology(HospitalOntology.NAME);
        refuseSwapMessage.addReceiver(message.getSender());
        refuseSwapMessage.setConversationId(timestamp);

        try {
            patientAgent.getContentManager().fillContent(refuseSwapMessage, reason);
        } catch (Codec.CodecException e) {
            throw new RuntimeException(e);
        } catch (OntologyException e) {
            throw new RuntimeException(e);
        }

        patientAgent.send(refuseSwapMessage);

    }

    private void replyWithAcceptance(ACLMessage message, String timestamp) {

        ACLMessage acceptSwapMessage = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
        acceptSwapMessage.setProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);
        acceptSwapMessage.setLanguage(patientAgent.getCodec().getName());
        acceptSwapMessage.setOntology(HospitalOntology.NAME);
        acceptSwapMessage.addReceiver(message.getSender());
        acceptSwapMessage.setConversationId(timestamp);

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

        System.out.println("Responding agent " + patientAgent.getLocalName() + " informing with " + allocationSwapSummary);

        try {

            patientAgent.getContentManager().fillContent(acceptSwapMessage, allocationSwapSummary);

        } catch (Codec.CodecException e) {
            throw new RuntimeException(e);
        } catch (OntologyException e) {
            throw new RuntimeException(e);
        }

        patientAgent.send(acceptSwapMessage);

    }

}
