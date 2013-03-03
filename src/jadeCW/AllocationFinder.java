package jadeCW;

import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.onto.OntologyException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AllocationFinder {

    private final PatientAgent patient;

    public AllocationFinder(PatientAgent agent) {
        patient = agent;
    }

    public List<AllocationState> getAllPreferredAllocations(DFAgentDescription allocationProvider, int curAppointment) {

        List<AllocationState> allocationStates = new ArrayList<AllocationState>();
        PatientPreference patientPreference = patient.getPatientPreference();

        /* 1. query patient preference for better appointment - patient could have no appointment */
        List<Integer> preferredAllocations = patientPreference.queryPreferredAllocations(curAppointment);

        printPreferredAllocations(curAppointment, preferredAllocations);

        Iterator<Integer> preferredAllocationIterator = preferredAllocations.iterator();
        boolean foundPreferredAllocation = false;

        while (preferredAllocationIterator.hasNext() && !foundPreferredAllocation) {
            /* 2. ask hospital agent who has got that preference */
            int preferredAppointment = preferredAllocationIterator.next();
            AllocationState allocationState = getAllocationState(allocationProvider, preferredAppointment);

            if (!allocationState.getAppointmentStatus()
                                .equals(GlobalAgentConstants.APPOINTMENT_QUERY_RESPONSE_STATUS_INVALID)) {
                allocationStates.add(allocationState);
            }
        }

        return allocationStates;
    }

    public AllocationState getAllocationState(DFAgentDescription allocationProvider, int appointmentId) {
        sendAllocationQuery(allocationProvider, appointmentId);
        return receiveAllocationResponse(appointmentId);
    }


    private void sendAllocationQuery(DFAgentDescription allocationProvider, int appointmentId) {

        ACLMessage allocationQueryMessage = new ACLMessage(ACLMessage.QUERY_REF);
        allocationQueryMessage.setProtocol(FIPANames.InteractionProtocol.FIPA_QUERY);
        allocationQueryMessage.setLanguage(patient.getCodec().getName());
        allocationQueryMessage.setOntology(HospitalOntology.NAME);
        allocationQueryMessage.addReceiver(allocationProvider.getName());

        Appointment appointment = new Appointment();
        appointment.setAllocation(appointmentId);

        try {
            patient.getContentManager().fillContent(allocationQueryMessage, appointment);
        } catch (Codec.CodecException e) {
            throw new RuntimeException(e);
        } catch (OntologyException e) {
            throw new RuntimeException(e);
        }

        patient.send(allocationQueryMessage);
    }

    private AllocationState receiveAllocationResponse(int appointmentId) {

           /* receiving response... */
           MessageTemplate messageTemplate = MessageTemplate.and(
                   MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_QUERY),
                   MessageTemplate.and(
                           MessageTemplate.MatchOntology(HospitalOntology.NAME),
                           MessageTemplate.and(
                                   MessageTemplate.MatchLanguage(patient.getCodec().getName()),
                                   MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF)

                           )

                   )
           );

           ACLMessage allocationQueryResponseMessage = patient.blockingReceive(messageTemplate);

           ContentElement p;
           try {
               p = patient.getContentManager().extractContent(allocationQueryResponseMessage);
           } catch (Codec.CodecException e) {
               throw new RuntimeException(e);
           } catch (OntologyException e) {
               throw new RuntimeException(e);
           }

           if (p instanceof AppointmentQuery) {
               AppointmentQuery appointmentQuery = (AppointmentQuery) p;
               String responseStatus = appointmentQuery.getState();
               String patientAgentHolding = appointmentQuery.getHolder();
               return new AllocationState(appointmentId, responseStatus, patientAgentHolding);

           } else {
               throw new RuntimeException();
           }

    }

    private void printPreferredAllocations(Integer curAppointment, List<Integer> preferredAllocations) {
        System.out.print("Preferred appointments to app " + curAppointment + ": ");
        for (Integer appointment : preferredAllocations) {
            System.out.print(appointment + " ");
        }
        System.out.println();
    }
}
