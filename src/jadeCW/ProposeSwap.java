package jadeCW;

import jade.content.lang.Codec;
import jade.content.onto.OntologyException;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.*;

public class ProposeSwap extends Behaviour {

    private boolean isHappyWithAppointment = false;
    private final PatientAgent patientAgent;
    private final DFPatientSubscription dfSubscription;
    private final int maxIterationsNum = 40;
    private int iterationsWithNoImprovementCount = 0;

    private Iterator<AllocationState> preferredAllocationsIterator;
    private MessageTemplate expectedMessageTemplate;
    private AgentAllocationSwap currentlyProposedAllocationSwap;
    private int currentSize;

    public ProposeSwap(DFPatientSubscription dfSubscription, PatientAgent patientAgent) {
        this.patientAgent = patientAgent;
        this.dfSubscription = dfSubscription;
    }

    @Override
    public void action() {

        // finding agent for appointment-service
        DFAgentDescription appointmentAgentDescription = dfSubscription.getAgentDescription();
        List<AllocationState> preferredAllocations = patientAgent.getAllocationStates();

        if(patientAgent.getCurrentAllocation() == GlobalAgentConstants.APPOINTMENT_UNINITIALIZED) {
            return;
        } else if(preferredAllocations.isEmpty()) {
            isHappyWithAppointment = true;
        } else if (appointmentAgentDescription != null && !preferredAllocations.isEmpty()) {

            /* Loop executed when action is executed for the first time */
            if(preferredAllocationsIterator == null) {
                /* Querying patient preferences */
                preferredAllocationsIterator = patientAgent.getAllocationStates().iterator();
                currentSize = patientAgent.getAllocationStates().size();
            }


            if(!preferredAllocationsIterator.hasNext()) {
                /* Condition executed when no more swaps available*/
                updatePreferences();

            } else if(expectedMessageTemplate == null) {

                proposeSwap(preferredAllocationsIterator.next(),appointmentAgentDescription);

            } else {
                /* Some message has been sent, awaiting a response */
                ACLMessage expectedMessage = patientAgent.receive(expectedMessageTemplate);
                if(expectedMessage != null) {
                    boolean wasSwapSuccessful = receiveResponse(expectedMessage,appointmentAgentDescription);
                    expectedMessageTemplate = null;
                    currentlyProposedAllocationSwap = null;

                    if(wasSwapSuccessful) {
                        updatePreferences();
                    }
                }
            }

        }

    }

    public boolean hasMadeSwapProposal() {
        return expectedMessageTemplate != null;
    }

    private void updatePreferences() {
        updatePatientAgentState(currentSize);
        preferredAllocationsIterator = patientAgent.getAllocationStates().iterator();
        currentSize = patientAgent.getAllocationStates().size();
    }


    private void updatePatientAgentState(int currentSize) {
        // check if we should terminate
        patientAgent.updatePreferredAllocations();

        List<AllocationState> newPreferredAllocations = patientAgent.getAllocationStates();
        if (newPreferredAllocations.isEmpty() || iterationsWithNoImprovementCount == maxIterationsNum) {
            // we have our favourite allocation
            System.out.println("Optimal appointment found for agent: " + patientAgent.getLocalName() + " appointment:  " + patientAgent.getCurrentAllocation());
            isHappyWithAppointment = true;
        } else if (newPreferredAllocations.size() == currentSize) {
            // no improvement
            iterationsWithNoImprovementCount++;
        } else {
            iterationsWithNoImprovementCount = 0;
        }
    }

    private void proposeSwap(AllocationState preferredAllocation, DFAgentDescription appointmentAgentDescription) {

        String timestamp = System.currentTimeMillis() + "";

        AgentAllocationSwap allocationSwap = new AgentAllocationSwap();
        allocationSwap.setCurrentAllocation(patientAgent.getCurrentAllocation());
        allocationSwap.setDesiredAllocation(preferredAllocation.getAppointment());

        System.out.println("Current appointment of agent : " + patientAgent.getLocalName() + " is " + patientAgent.getCurrentAllocation());

        AID exchangePartnerAgent;
        if (preferredAllocation.getAppointmentStatus().equals(GlobalAgentConstants.APPOINTMENT_QUERY_RESPONSE_STATUS_FREE)) {
            exchangePartnerAgent = appointmentAgentDescription.getName();
        } else {
            System.out.println(patientAgent.getLocalName() + " asking other agent " + "for appointment " + preferredAllocation.getAppointment());
            exchangePartnerAgent = new AID(preferredAllocation.getAppointmentHolder(), AID.ISGUID);
        }

        requestSwapWithAgent(exchangePartnerAgent, allocationSwap, timestamp);
    }

    private void requestSwapWithAgent(AID agent, AgentAllocationSwap agentAllocationSwap, String timestamp) {

        ACLMessage swapMessage = new ACLMessage(ACLMessage.PROPOSE);
        swapMessage.setConversationId(timestamp);
        swapMessage.setProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);
        swapMessage.setLanguage(patientAgent.getCodec().getName());
        swapMessage.setOntology(HospitalOntology.NAME);
        swapMessage.addReceiver(agent);

        try {
            patientAgent.getContentManager().fillContent(swapMessage, agentAllocationSwap);
        } catch (Codec.CodecException e) {
            throw new RuntimeException(e);
        } catch (OntologyException e) {
            throw new RuntimeException(e);
        }

        patientAgent.send(swapMessage);
        expectedMessageTemplate = createMatchingTemplate(timestamp);
        currentlyProposedAllocationSwap = agentAllocationSwap;

    }

    // returns true if swap was made
    private boolean receiveResponse(ACLMessage expectedMessage, DFAgentDescription hospitalAgent) {

        System.out.println("Agent " + patientAgent.getLocalName() + " waiting to receive response");

        boolean successfulSwap = false;
        if (expectedMessage.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {

            if (!expectedMessage.getSender().equals(hospitalAgent.getName())) {
                informHospitalAgentOfSwap(expectedMessage.getSender(),hospitalAgent.getName(), expectedMessage.getConversationId());
                receiveConfirmationFromHospitalAgent(expectedMessage.getConversationId());
            }

            patientAgent.setCurrentAllocation(currentlyProposedAllocationSwap.getDesiredAllocation());
            successfulSwap = true;

        }

        currentlyProposedAllocationSwap = null;
        expectedMessageTemplate = null;
        return successfulSwap;
    }

    private void receiveConfirmationFromHospitalAgent(String timestamp) {

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


    }

    private void informHospitalAgentOfSwap(AID otherAgent, AID hospitalAgent, String timestamp) {

        ACLMessage acceptSwapMessage = new ACLMessage(ACLMessage.INFORM);
        acceptSwapMessage.setProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);
        acceptSwapMessage.setLanguage(patientAgent.getCodec().getName());
        acceptSwapMessage.setOntology(HospitalOntology.NAME);
        acceptSwapMessage.setConversationId(timestamp);
        acceptSwapMessage.addReceiver(hospitalAgent);

        AllocationSwapSummary allocationSwapSummary = new AllocationSwapSummary();
        allocationSwapSummary.setProposingAgentOldAppointment(currentlyProposedAllocationSwap.getCurrentAllocation());
        allocationSwapSummary.setProposingAgent(patientAgent.getName());
        allocationSwapSummary.setReceivingAgentOldAppointment(currentlyProposedAllocationSwap.getDesiredAllocation());
        allocationSwapSummary.setReceivingAgent(otherAgent.getName());
        allocationSwapSummary.setTimestamp(timestamp);

        System.out.println("Proposing agent " + patientAgent.getLocalName() + " informing with " + allocationSwapSummary);

        try {
            patientAgent.getContentManager().fillContent(acceptSwapMessage, allocationSwapSummary);
        } catch (Codec.CodecException e) {
            throw new RuntimeException(e);
        } catch (OntologyException e) {
            throw new RuntimeException(e);
        }

        patientAgent.send(acceptSwapMessage);
    }


    @Override
    public boolean done() {
        return isHappyWithAppointment;
    }

    private MessageTemplate createMatchingTemplate(String timestamp) {
        MessageTemplate messageTemplateAccept = MessageTemplate.and(
                MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE),
                MessageTemplate.and(
                        MessageTemplate.MatchOntology(HospitalOntology.NAME),
                        MessageTemplate.and(
                                MessageTemplate.MatchLanguage(patientAgent.getCodec().getName()),
                                MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
                                        MessageTemplate.MatchConversationId(timestamp))


                        )

                )
        );

        MessageTemplate messageTemplateReject = MessageTemplate.and(
                MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE),
                MessageTemplate.and(
                        MessageTemplate.MatchOntology(HospitalOntology.NAME),
                        MessageTemplate.and(
                                MessageTemplate.MatchLanguage(patientAgent.getCodec().getName()),
                                MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL),
                                        MessageTemplate.MatchConversationId(timestamp))
                        )
                )
        );

        MessageTemplate messageTemplate = MessageTemplate.or(messageTemplateAccept, messageTemplateReject);

        return messageTemplate;
    }


}
