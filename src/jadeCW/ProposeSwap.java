package jadeCW;

import jade.content.ContentElement;
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

    private boolean didSwap = false;
    private final PatientAgent patientAgent;
    private final DFPatientSubscription dfSubscription;


    public ProposeSwap(DFPatientSubscription dfSubscription, PatientAgent patientAgent) {
        this.patientAgent = patientAgent;
        this.dfSubscription = dfSubscription;
    }

    @Override
    public void action() {

        // finding agent for appointment-service
        DFAgentDescription appointmentAgentDescription = dfSubscription.getAgentDescription();

        if (appointmentAgentDescription != null) {

            List<AllocationState> allocationStates = patientAgent.getAllocationStates();
            Iterator<AllocationState> allocationStateIterator = allocationStates.iterator();

            List<AllocationState> updatedAllocationStates = new ArrayList<AllocationState>(allocationStates.size());

            while (allocationStateIterator.hasNext() && !didSwap) {

                AllocationState allocationState = allocationStateIterator.next();

                AgentAllocationSwap agentAllocationSwap = new AgentAllocationSwap();
                agentAllocationSwap.setCurrentAllocation(patientAgent.getCurrentAllocation());
                agentAllocationSwap.setDesiredAllocation(allocationState.getAppointment());

                if (allocationState.getAppointmentStatus().equals(GlobalAgentConstants.APPOINTMENT_QUERY_RESPONSE_STATUS_ALLOCATED)) {
                    // need to send to the allocation holder
                    String allocationHolderName = allocationState.getAppointmentHolder();
                    AID allocationHolderAID = new AID(allocationHolderName, AID.ISGUID);
                    requestSwapWithAgent(allocationHolderAID, agentAllocationSwap);
                    receiveResponse(appointmentAgentDescription.getName(), agentAllocationSwap,
                            allocationStateIterator,updatedAllocationStates);

                } else if (allocationState.getAppointmentStatus().equals(GlobalAgentConstants.APPOINTMENT_QUERY_RESPONSE_STATUS_FREE)) {
                    // need to talk to hospital
                    requestSwapWithAgent(appointmentAgentDescription.getName(), agentAllocationSwap);
                    receiveResponse(appointmentAgentDescription.getName(), agentAllocationSwap, allocationStateIterator, updatedAllocationStates);

                } else {
                    // if there is another kind of query response status
                    throw new RuntimeException();
                }

                updatedAllocationStates.add(allocationState);
            }

            updatedAllocationStates.addAll(allocationStates);

            Collections.sort(updatedAllocationStates, new Comparator<AllocationState>() {
                public int compare(AllocationState allocationState, AllocationState allocationState1) {
                    return patientAgent.getPatientPreference().
                            isAllocationSwapAcceptable(
                                    allocationState.getAppointment(),
                                    allocationState1.getAppointment())
                            ? 1 : -1;
                }
            });

            // resort
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
            // TODO
            // Umyj swiat i ta funkcje
        } catch (Codec.CodecException e) {
            throw new RuntimeException(e);
        } catch (OntologyException e) {
            throw new RuntimeException(e);
        }

        patientAgent.send(swapMessage);
    }

    private void receiveResponse(AID hospitalAgent, AgentAllocationSwap agentAllocationSwap,
                                 Iterator<AllocationState> allocationStateIterator,
                                 List<AllocationState> updatedAllocationStates) {

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

        MessageTemplate messageTemplate = MessageTemplate.or(messageTemplateAccept, messageTemplateReject);

        ACLMessage message = patientAgent.blockingReceive(messageTemplate);

        if (message.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
            // THE AGENT HAS ACCEPTED!
            patientAgent.setCurrentAllocation(agentAllocationSwap.getDesiredAllocation());

            // Inform the HospitalAgent that the two agents have swapped appointments,
            // if a swap was made with an agent that is not a HospitalAgent.
            AID sender = message.getSender();
            if (sender != hospitalAgent) {
                // the sender is not the hospitalAgent
                informHospitalAgentOfSwap(hospitalAgent, agentAllocationSwap);
            }

            didSwap = true;
        } else {
            ContentElement p = null;
            int allocation;
            String holder;

            try {
                p = patientAgent.getContentManager().extractContent(message);
            } catch (Codec.CodecException e) {
                throw new RuntimeException(e);
            } catch (OntologyException e) {
                throw new RuntimeException(e);
            }

            if (p instanceof SwapAllocationUpdate) {
                SwapAllocationUpdate swapAllocationUpdate = (SwapAllocationUpdate) p;

                allocation = swapAllocationUpdate.getAllocation();
                holder = swapAllocationUpdate.getHolder();

            } else {
                throw new RuntimeException();
            }
            // this is a rejection
            if (agentAllocationSwap.getDesiredAllocation() != allocation) {

                allocationStateIterator.remove();
                AllocationState allocationState = new AllocationState(allocation, GlobalAgentConstants.APPOINTMENT_QUERY_RESPONSE_STATUS_ALLOCATED, holder);

                updatedAllocationStates.add(allocationState);
            }
        }
    }

    private void informHospitalAgentOfSwap(AID hospitalAgent, AgentAllocationSwap agentAllocationSwap) {

        ACLMessage notifyMessage = new ACLMessage(ACLMessage.INFORM);
        notifyMessage.setProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);
        notifyMessage.setLanguage(patientAgent.getCodec().getName());
        notifyMessage.setOntology(HospitalOntology.NAME);

        notifyMessage.addReceiver(hospitalAgent);

        try {

            patientAgent.getContentManager().fillContent(notifyMessage, agentAllocationSwap);
            // TODO
            // Umyj swiat i ta funkcje
        } catch (Codec.CodecException e) {
            throw new RuntimeException(e);
        } catch (OntologyException e) {
            throw new RuntimeException(e);
        }

        patientAgent.send(notifyMessage);

    }


    @Override
    public boolean done() {
        return didSwap;
    }
}
