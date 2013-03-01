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

    public ProposeSwap(DFPatientSubscription dfSubscription, PatientAgent patientAgent) {
        this.patientAgent = patientAgent;
        this.dfSubscription = dfSubscription;
    }

    @Override
    public void action() {

        // finding agent for appointment-service
        DFAgentDescription appointmentAgentDescription = dfSubscription.getAgentDescription();

        if (appointmentAgentDescription != null) {

            List<AllocationState> preferredAllocations = patientAgent.getAllocationStates();

            System.out.println("When we enter the allocation states for agent " +
                    patientAgent.getLocalName() + " are " + preferredAllocations.toString());


            for (AllocationState preferredAllocation : preferredAllocations) {
                // try getting a better one
                AgentAllocationSwap allocationSwap = new AgentAllocationSwap();
                allocationSwap.setCurrentAllocation(patientAgent.getCurrentAllocation());
                allocationSwap.setDesiredAllocation(preferredAllocation.getAppointment());

                System.out.println("Current appointment of agent : " + patientAgent.getLocalName() +
                        " is " + patientAgent.getCurrentAllocation());

                boolean slotFree = preferredAllocation.getAppointmentStatus()
                        .equals(GlobalAgentConstants.APPOINTMENT_QUERY_RESPONSE_STATUS_FREE);
                if (slotFree) {
                    // ask hospital agent for this appointment
                    requestSwapWithAgent(appointmentAgentDescription.getName(), allocationSwap);
                }
                else if (patientAgent.getCurrentAllocation() != preferredAllocation.getAppointment()) {
                    // ask other agent
                    System.out.println(patientAgent.getLocalName() + " asking other agent " +
                            "for appointment " + preferredAllocation.getAppointment());
                    String allocationHolderName = preferredAllocation.getAppointmentHolder();
                    AID allocationHolderAID = new AID(allocationHolderName, AID.ISGUID);
                    requestSwapWithAgent(allocationHolderAID, allocationSwap);
                }
                else {
                    break;
                }
                boolean swapMade = receiveResponse(allocationSwap, appointmentAgentDescription.getName());
                if (swapMade) break;
            }

            // check if we should terminate
            patientAgent.updatePreferredAllocations();
            List<AllocationState> newPreferredAllocations = patientAgent.getAllocationStates();
            if (newPreferredAllocations.isEmpty() || iterationsWithNoImprovementCount == maxIterationsNum) {
                // we have our favourite allocation
                System.out.println("Optimal appointment found for agent: "
                        + patientAgent.getLocalName() + " appointment:  " + patientAgent.getCurrentAllocation());
                isHappyWithAppointment = true;
            }
            else if (newPreferredAllocations.size() == preferredAllocations.size()) {
                // no improvement
                iterationsWithNoImprovementCount++;
            }
            else {
                iterationsWithNoImprovementCount = 0;
            }

        }

    }

    private void requestSwapWithAgent(AID agent, AgentAllocationSwap agentAllocationSwap) {

        ACLMessage swapMessage = new ACLMessage(ACLMessage.PROPOSE);
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
    private boolean receiveResponse(AgentAllocationSwap swapSent, AID hospitalAgent) {

        MessageTemplate messageTemplateAccept = MessageTemplate.and(
                MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE),
                MessageTemplate.and(
                        MessageTemplate.MatchOntology(HospitalOntology.NAME),
                        MessageTemplate.and(
                                MessageTemplate.MatchLanguage(patientAgent.getCodec().getName()),
                                MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL)

                        )

                )
        );

        MessageTemplate messageTemplateReject = MessageTemplate.and(
                MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE),
                MessageTemplate.and(
                        MessageTemplate.MatchOntology(HospitalOntology.NAME),
                        MessageTemplate.and(
                                MessageTemplate.MatchLanguage(patientAgent.getCodec().getName()),
                                MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL)
                        )
                )
        );

        System.out.println("Agent " + patientAgent.getLocalName() + " waiting to receive response");

        MessageTemplate messageTemplate = MessageTemplate.or(messageTemplateAccept, messageTemplateReject);
        ACLMessage message = patientAgent.receive(messageTemplate);
        if (message == null) return false;

        boolean swapAccepted = message.getPerformative() == ACLMessage.ACCEPT_PROPOSAL;

        System.out.println("Agent " + patientAgent.getLocalName() + " received his response " + message.toString());

        if (swapAccepted) {
            patientAgent.setCurrentAllocation(swapSent.getDesiredAllocation());
            if (!message.getSender().equals(hospitalAgent)) {
                informHospitalAgentOfSwap(swapSent, message.getSender(), hospitalAgent);
            }
            return true;
        }
        else {
            if (!message.getSender().equals(hospitalAgent)) {
                // obtain current owner address from the message
                // send request to that guy
                // hospital is sending AllocationQuery
            }
            else {
                // message received from another patient agent
                // check if the case for refusal was appointment not in possession
                // if so, query hospital agent about the current holder and send request to him
                // else return false
                // patient is sending either AppointmentNotInPossession OR AppointmnetNotPreferred
            }
            return false;
        }
    }

    private void informHospitalAgentOfSwap(AgentAllocationSwap agentAllocationSwap,
                                           AID otherAgent, AID hospitalAgent) {

        ACLMessage acceptSwapMessage = new ACLMessage(ACLMessage.INFORM);
        acceptSwapMessage.setProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);
        acceptSwapMessage.setLanguage(patientAgent.getCodec().getName());
        acceptSwapMessage.setOntology(HospitalOntology.NAME);
        acceptSwapMessage.addReceiver(hospitalAgent);

        AllocationSwapSummary allocationSwapSummary = new AllocationSwapSummary();
        allocationSwapSummary.setProposingAgentOldAppointment(agentAllocationSwap.getCurrentAllocation());
        allocationSwapSummary.setProposingAgent(patientAgent.getName());
        allocationSwapSummary.setReceivingAgentOldAppointment(agentAllocationSwap.getDesiredAllocation());
        allocationSwapSummary.setReceivingAgent(otherAgent.getName());

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
}
