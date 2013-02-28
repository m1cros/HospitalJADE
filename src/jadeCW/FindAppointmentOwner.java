package jadeCW;

import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.onto.OntologyException;
import jade.core.behaviours.Behaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FindAppointmentOwner extends Behaviour {

    private final DFPatientSubscription dfSubscription;
    private final PatientAgent patientAgent;
    private final PatientPreference patientPreference;

    private boolean allocationStatesSet;

    public FindAppointmentOwner(DFPatientSubscription dfSubscription, PatientAgent patientAgent, PatientPreference patientPreference) {
        this.dfSubscription = dfSubscription;
        this.patientAgent = patientAgent;
        this.patientPreference = patientPreference;

        allocationStatesSet = false;
    }

    @Override
    public void action() {
        /* Hospital agent */
        DFAgentDescription appointmentAgentDescription = dfSubscription.getAgentDescription();

        if (appointmentAgentDescription != null) {

            int currentAllocation = patientAgent.getCurrentAllocation();
            List<AllocationState> allocationStates = new ArrayList<AllocationState>();

            /* 1. query patient preference for better appointment - patient could have no appointment? */
            List<Integer> preferredAllocationsOverCurrent = patientPreference.queryPreferredAllocations(currentAllocation);
            Iterator<Integer> preferredAllocationIterator = preferredAllocationsOverCurrent.iterator();
            boolean foundPreferredAllocation = false;

            System.out.println("Allocations tested by: " + patientAgent.getLocalName());
            System.out.println(patientAgent.getLocalName() + ": " + preferredAllocationsOverCurrent.toString());
            while (preferredAllocationIterator.hasNext() && !foundPreferredAllocation) {
                int preferredAllocation = preferredAllocationIterator.next();
                /* 2. ask hospital agent who has got that preference */

                queryPreferredAllocation(appointmentAgentDescription, preferredAllocation);

                AllocationState allocationState = receivePreferredAllocationResponse(preferredAllocation);

                if (!allocationState.getAppointmentStatus().equals(GlobalAgentConstants.APPOINTMENT_QUERY_RESPONSE_STATUS_INVALID)) {
                    allocationStates.add(allocationState);
                }

                System.out.println(patientAgent.getLocalName() + ": allocation state: " + allocationState.toString());
            }

            System.out.println(patientAgent.getLocalName() + ": more preferred states: " + allocationStates.toString());

            patientAgent.setAllocationStates(allocationStates);
            allocationStatesSet = true;

        }
    }

    private void queryPreferredAllocation(DFAgentDescription appointmentAgentDescription, int preferredAllocation) {

        ACLMessage allocationQueryMessage = new ACLMessage(ACLMessage.QUERY_REF);
        allocationQueryMessage.setProtocol(FIPANames.InteractionProtocol.FIPA_QUERY);
        allocationQueryMessage.setLanguage(patientAgent.getCodec().getName());
        allocationQueryMessage.setOntology(HospitalOntology.NAME);

        allocationQueryMessage.setSender(patientAgent.getAID());
        allocationQueryMessage.addReceiver(appointmentAgentDescription.getName());

        Appointment appointment = new Appointment();
        appointment.setAllocation(preferredAllocation);
        try {

            patientAgent.getContentManager().fillContent(allocationQueryMessage, appointment);
            // TODO
            // Umyj swiat i ta funkcje
        } catch (Codec.CodecException e) {
            throw new RuntimeException(e);
        } catch (OntologyException e) {
            throw new RuntimeException(e);
        }

        patientAgent.send(allocationQueryMessage);
    }

    private AllocationState receivePreferredAllocationResponse(int preferredAllocation) {
        /* receiving response... */

        MessageTemplate messageTemplate = MessageTemplate.and(
                MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_QUERY),
                MessageTemplate.and(
                        MessageTemplate.MatchOntology(HospitalOntology.NAME),
                        MessageTemplate.and(
                                MessageTemplate.MatchLanguage(patientAgent.getCodec().getName()),
                                MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF)

                        )

                )
        );

        ACLMessage allocationQueryResponseMessage = patientAgent.blockingReceive(messageTemplate);

        ContentElement p = null;
        try {
            p = patientAgent.getContentManager().extractContent(allocationQueryResponseMessage);
        } catch (Codec.CodecException e) {
            throw new RuntimeException(e);
        } catch (OntologyException e) {
            throw new RuntimeException(e);
        }

        if (p instanceof AppointmentQuery) {
            AppointmentQuery appointmentQuery = (AppointmentQuery) p;

            String responseStatus = appointmentQuery.getState();
            String patientAgentHolding = appointmentQuery.getHolder();

            AllocationState allocatedState = new AllocationState(preferredAllocation, responseStatus, patientAgentHolding);

            return allocatedState;

        } else {
            throw new RuntimeException();
        }


    }

    @Override
    public boolean done() {
        return allocationStatesSet;
    }
}
