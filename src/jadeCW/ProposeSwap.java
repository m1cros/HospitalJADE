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

/**
 * Patient behaviour class which is responsible
 * for proposing swaps to agents which have
 * more preferred appointment than our current one
 */
public class ProposeSwap extends Behaviour {

    /* Patient Agent for which this behaviour is instantiated */
    private final PatientAgent patientAgent;

    /* Subscription to hospital agent */
    private final DFPatientSubscription dfSubscription;

    /* Maximum number of iterations of the algorithm when no improvement
       of the behaviour has been achieved. After extending this number,
       the behaviour shuts down.
     */
    private final int maxIterationsNum = 40;
    private int iterationsWithNoImprovementCount = 0;

    private boolean isHappyWithAppointment = false;

    /* The following variables are used to denote state of behaviour */
    /* The states are described in the method body */
    private Iterator<AllocationState> preferredAllocationsIterator;
    private MessageTemplate expectedMessageTemplate;
    private AgentAllocationSwap currentlyProposedAllocationSwap;
    private int currentSize;

    public ProposeSwap(DFPatientSubscription dfSubscription, PatientAgent patientAgent) {
        this.patientAgent = patientAgent;
        this.dfSubscription = dfSubscription;
    }

    /**
     * @return true if the behaviour has made swap proposal and is
     *         expecting a decision from other patient agent
     */
    public boolean hasMadeSwapProposal() {
        return expectedMessageTemplate != null;
    }

    /**
     * @return details about the currently made offer
     */
    public AgentAllocationSwap getCurrentlyProposedAllocationSwap() {
        return currentlyProposedAllocationSwap;
    }

    @Override
    public boolean done() {
        return isHappyWithAppointment;
    }

    @Override
    public void action() {

        /* Retrieving hospital agent */
        DFAgentDescription appointmentAgentDescription = dfSubscription.getAgentDescription();
        List<AllocationState> preferredAllocations = patientAgent.getAllocationStates();

        if (patientAgent.getCurrentAllocation() == GlobalAgentConstants.APPOINTMENT_UNINITIALIZED) {
            /* Patient agent has not got any appointment from the hospital agent yet */
            return;

        } else if (preferredAllocations.isEmpty()) {
            /* Patient Agent has its most preferred appointment */
            isHappyWithAppointment = true;

        } else if (appointmentAgentDescription != null) {

            /* Loop executed when action is executed for the first time */
            if (preferredAllocationsIterator == null) {
                /* Querying patient preferences */
                preferredAllocationsIterator = patientAgent.getAllocationStates().iterator();
                currentSize = patientAgent.getAllocationStates().size();
            }


            if (!preferredAllocationsIterator.hasNext()) {
                /* Condition executed when no more swaps available */
                updatePreferences();

            } else if (expectedMessageTemplate == null) {

                /* We are not expecting any response, we can make another swap proposal */
                proposeSwap(preferredAllocationsIterator.next(), appointmentAgentDescription);

            } else {
                /* Some message has been sent, awaiting a response */
                ACLMessage expectedMessage = patientAgent.receive(expectedMessageTemplate);
                if (expectedMessage != null) {
                    boolean wasSwapSuccessful = receiveResponse(expectedMessage, appointmentAgentDescription);
                    expectedMessageTemplate = null;
                    currentlyProposedAllocationSwap = null;

                    /* If swap has been successful, we need to update our preference list
                       as the patient agent has got new appointment */
                    if (wasSwapSuccessful) {
                        updatePreferences();
                    }
                }
            }
        }
    }


    /**
     * The method updates the list of more preferred possible allocations
     * in patient agent
     */
    private void updatePreferences() {

        /* Update preferences of the agent */
        patientAgent.updatePreferredAllocations();
        List<AllocationState> newPreferredAllocations = patientAgent.getAllocationStates();
        preferredAllocationsIterator = patientAgent.getAllocationStates().iterator();
        currentSize = patientAgent.getAllocationStates().size();

        if (newPreferredAllocations.isEmpty() || iterationsWithNoImprovementCount >= maxIterationsNum) {
            /* We shut down the behaviour if there are no better appointments
               or we exceeded the possible number of non-improving algorithm iterations
             */
            isHappyWithAppointment = true;

        } else if (newPreferredAllocations.size() >= currentSize) {

            /* No improvement has been made in our algorithm  */
            iterationsWithNoImprovementCount++;
        } else {

            /* Improved the appointment, resetting the counter */
            iterationsWithNoImprovementCount = 0;
        }


    }

    private void proposeSwap(AllocationState preferredAllocation, DFAgentDescription appointmentAgentDescription) {

        /* Timestamp used to identify conversation, the same timestamp
           will be used to throughout the whole conversation */
        String timestamp = System.currentTimeMillis() + "";

        /* Object allocationSwap contains information about swap proposal for some other agent */
        AgentAllocationSwap allocationSwap = new AgentAllocationSwap();
        allocationSwap.setCurrentAllocation(patientAgent.getCurrentAllocation());
        allocationSwap.setDesiredAllocation(preferredAllocation.getAppointment());
        allocationSwap.setTimestamp(timestamp);

        /* Setting exchange agent */
        AID exchangePartnerAgent;

        /* Required appointment is not held by anyone, so we must receive a response from hospital agent */
        if (preferredAllocation.getAppointmentStatus().equals(GlobalAgentConstants.APPOINTMENT_QUERY_RESPONSE_STATUS_FREE)) {
            exchangePartnerAgent = appointmentAgentDescription.getName();
        } else {
            /* otherwise, appointment is held by other agent */
            exchangePartnerAgent = new AID(preferredAllocation.getAppointmentHolder(), AID.ISGUID);
        }

        /* actual request sending */
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

        /* Changing the state of the behaviour
           We are now waiting for the response from other agent which match given template
         */
        expectedMessageTemplate = createMatchingTemplate(timestamp, agent);
        currentlyProposedAllocationSwap = agentAllocationSwap;

    }

    /**
     * @param expectedMessage - expected response from queried agnet
     * @param hospitalAgent
     * @return true if response is positive and we can make a swap
     */
    private boolean receiveResponse(ACLMessage expectedMessage, DFAgentDescription hospitalAgent) {
        boolean successfulSwap = false;
        if (expectedMessage.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {

            /* If the message was not from hospital agent,
               we should inform hospital about the change
             */
            if (!expectedMessage.getSender().equals(hospitalAgent.getName())) {
                /* Inform hospital about the swap */
                informHospitalAgentOfSwap(expectedMessage.getSender(), hospitalAgent.getName(), expectedMessage.getConversationId());

                /* Wait for confirmation */
                receiveConfirmationFromHospitalAgent(expectedMessage.getConversationId(), hospitalAgent.getName());
            }

            /* Changing current allocation */
            patientAgent.setCurrentAllocation(currentlyProposedAllocationSwap.getDesiredAllocation());
            successfulSwap = true;
        }

        /* Changing the state of the behaviour,
           we can now send another propose swap */
        currentlyProposedAllocationSwap = null;
        expectedMessageTemplate = null;
        return successfulSwap;
    }

    /**
     * Informs hospital about the swap
     *
     * @param otherAgent    - agent with which we have made a swap
     * @param hospitalAgent
     * @param timestamp     - conversation id of the swap
     */
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


        try {
            patientAgent.getContentManager().fillContent(acceptSwapMessage, allocationSwapSummary);
        } catch (Codec.CodecException e) {
            throw new RuntimeException(e);
        } catch (OntologyException e) {
            throw new RuntimeException(e);
        }

        patientAgent.send(acceptSwapMessage);
    }

    private void receiveConfirmationFromHospitalAgent(String timestamp, AID hospital) {

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
    }


    private MessageTemplate createMatchingTemplate(String timestamp, AID sender) {
        MessageTemplate messageTemplateAccept = MessageTemplate.and(
                MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE),
                MessageTemplate.and(
                        MessageTemplate.MatchOntology(HospitalOntology.NAME),
                        MessageTemplate.and(
                                MessageTemplate.MatchLanguage(patientAgent.getCodec().getName()),
                                MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
                                        MessageTemplate.and(MessageTemplate.MatchConversationId(timestamp),
                                                MessageTemplate.MatchSender(sender)))


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
                                        MessageTemplate.and(MessageTemplate.MatchConversationId(timestamp),
                                                MessageTemplate.MatchSender(sender)))
                        )
                )
        );

        MessageTemplate messageTemplate = MessageTemplate.or(messageTemplateAccept, messageTemplateReject);

        return messageTemplate;
    }


}
