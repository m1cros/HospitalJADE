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


            if (patientAgent.getCurrentAllocation() != agentAllocationSwap.getDesiredAllocation()) {
                AppointmentNotInPossession appointmentNotInPossession = new AppointmentNotInPossession();
                appointmentNotInPossession.setCurrentAppointment(patientAgent.getCurrentAllocation());
                refuseSwapProposal(message, appointmentNotInPossession);
            }

            else if (patientAgent.getPatientPreference().isAllocationSwapAcceptable(agentAllocationSwap.getCurrentAllocation(),
                    patientAgent.getCurrentAllocation())) {
                patientAgent.setCurrentAllocation(agentAllocationSwap.getCurrentAllocation());
                replyWithAcceptance(message);
                informHospitalAgentOfSwap(agentAllocationSwap, dfSubscription.getAgentDescription().getName(), message.getSender());

            } else {
                AppointmentNotPreferred appointmentNotPreferred = new AppointmentNotPreferred();
                refuseSwapProposal(message, appointmentNotPreferred);
            }
        }
    }

    private void refuseSwapProposal(ACLMessage message, Predicate reason) {

        ACLMessage refuseSwapMessage = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
        refuseSwapMessage.setProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);
        refuseSwapMessage.setLanguage(patientAgent.getCodec().getName());
        refuseSwapMessage.setOntology(HospitalOntology.NAME);
        refuseSwapMessage.addReceiver(message.getSender());

        try {
            patientAgent.getContentManager().fillContent(refuseSwapMessage, reason);
        } catch (Codec.CodecException e) {
            throw new RuntimeException(e);
        } catch (OntologyException e) {
            throw new RuntimeException(e);
        }

        patientAgent.send(refuseSwapMessage);

    }

    private void replyWithAcceptance(ACLMessage message) {

        ACLMessage acceptSwapMessage = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
        acceptSwapMessage.setProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);
        acceptSwapMessage.setLanguage(patientAgent.getCodec().getName());
        acceptSwapMessage.setOntology(HospitalOntology.NAME);
        acceptSwapMessage.addReceiver(message.getSender());

        patientAgent.send(acceptSwapMessage);

    }

    private void informHospitalAgentOfSwap(AgentAllocationSwap agentAllocationSwap,
                                           AID hospitalAgent, AID swapProposalAgent) {

        ACLMessage acceptSwapMessage = new ACLMessage(ACLMessage.INFORM);
        acceptSwapMessage.setProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);
        acceptSwapMessage.setLanguage(patientAgent.getCodec().getName());
        acceptSwapMessage.setOntology(HospitalOntology.NAME);
        acceptSwapMessage.addReceiver(hospitalAgent);

        AllocationSwapSummary allocationSwapSummary = new AllocationSwapSummary();
        allocationSwapSummary.setProposingAgentOldAppointment(agentAllocationSwap.getCurrentAllocation());
        allocationSwapSummary.setProposingAgent(swapProposalAgent.getName());
        allocationSwapSummary.setReceivingAgentOldAppointment(agentAllocationSwap.getDesiredAllocation());
        allocationSwapSummary.setReceivingAgent(patientAgent.getName());

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
