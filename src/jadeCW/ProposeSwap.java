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

        if (appointmentAgentDescription != null && !preferredAllocations.isEmpty()) {

            if(preferredAllocationsIterator == null) {
                preferredAllocationsIterator = patientAgent.getAllocationStates().iterator();
                currentSize = patientAgent.getAllocationStates().size();
            }

            if(!preferredAllocationsIterator.hasNext()) {
                updatePreferences();
                proposeSwap(preferredAllocationsIterator.next(),appointmentAgentDescription);
            } else if(expectedMessageTemplate == null) {
                proposeSwap(preferredAllocationsIterator.next(),appointmentAgentDescription);
            } else {
                ACLMessage expectedMessage = patientAgent.receive(expectedMessageTemplate);

                if(expectedMessage != null) {
                    boolean wasSwapSuccessful = receiveResponse(expectedMessage,appointmentAgentDescription);

                    expectedMessageTemplate = null;

                    if(wasSwapSuccessful) {
                        updatePreferences();
                    }
                }
            }

        }

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

    private boolean proposeSwap(AllocationState preferredAllocation, DFAgentDescription appointmentAgentDescription) {

        String timestamp = System.currentTimeMillis() + "";

        AgentAllocationSwap allocationSwap = new AgentAllocationSwap();
        allocationSwap.setCurrentAllocation(patientAgent.getCurrentAllocation());
        allocationSwap.setDesiredAllocation(preferredAllocation.getAppointment());

        System.out.println("Current appointment of agent : " + patientAgent.getLocalName() + " is " + patientAgent.getCurrentAllocation());

        boolean shouldContinueSwapping = false;
        if (preferredAllocation.getAppointmentStatus().equals(GlobalAgentConstants.APPOINTMENT_QUERY_RESPONSE_STATUS_FREE)) {

            requestSwapWithAgent(appointmentAgentDescription.getName(), allocationSwap, timestamp);

        } else if(patientAgent.getCurrentAllocation() != preferredAllocation.getAppointment()) {

            System.out.println(patientAgent.getLocalName() + " asking other agent " + "for appointment " + preferredAllocation.getAppointment());
            requestSwapWithAgent(new AID(preferredAllocation.getAppointmentHolder(), AID.ISGUID), allocationSwap, timestamp);

        } else {
            shouldContinueSwapping = true;
        }

        if(!shouldContinueSwapping) {
            shouldContinueSwapping = !receiveResponse(allocationSwap, appointmentAgentDescription.getName(),timestamp);
        }

        return shouldContinueSwapping;
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

    }

    // returns true if swap was made
    private boolean receiveResponse(ACLMessage expectedMessage, DFAgentDescription hospitalAgent) {

        System.out.println("Agent " + patientAgent.getLocalName() + " waiting to receive response");

        boolean successfulSwap;
        if (expectedMessage.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {

            patientAgent.setCurrentAllocation(swapSent.getDesiredAllocation());
            if (!expectedMessage.getSender().equals(hospitalAgent.getName())) {
                informHospitalAgentOfSwap(swapSent, message.getSender(), hospitalAgent, timestamp);
            }

            successfulSwap = true;

        } else {

            if (!message.getSender().equals(hospitalAgent)) {
                // obtain current owner address from the message
                // send request to that guy
                // hospital is sending AllocationQuery
            } else {
                // message received from another patient agent
                // check if the case for refusal was appointment not in possession
                // if so, query hospital agent about the current holder and send request to him
                // else return false
                // patient is sending either AppointmentNotInPossession OR AppointmnetNotPreferred
            }

            successfulSwap = false;
        }

        return successfulSwap;
    }

    private void informHospitalAgentOfSwap(AgentAllocationSwap agentAllocationSwap,
                                           AID otherAgent, AID hospitalAgent, String timestamp) {

        ACLMessage acceptSwapMessage = new ACLMessage(ACLMessage.INFORM);
        acceptSwapMessage.setProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);
        acceptSwapMessage.setLanguage(patientAgent.getCodec().getName());
        acceptSwapMessage.setOntology(HospitalOntology.NAME);
        acceptSwapMessage.setConversationId(timestamp);
        acceptSwapMessage.addReceiver(hospitalAgent);

        AllocationSwapSummary allocationSwapSummary = new AllocationSwapSummary();
        allocationSwapSummary.setProposingAgentOldAppointment(agentAllocationSwap.getCurrentAllocation());
        allocationSwapSummary.setProposingAgent(patientAgent.getName());
        allocationSwapSummary.setReceivingAgentOldAppointment(agentAllocationSwap.getDesiredAllocation());
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
