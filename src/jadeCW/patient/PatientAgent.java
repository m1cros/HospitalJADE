package jadeCW.patient;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jadeCW.InvalidAgentInputException;
import jadeCW.patient.patientBehaviour.RequestAppointment;
import jadeCW.utils.GlobalAgentConstants;

public class PatientAgent extends Agent {

    private static long MAX_AGENT_QUEUE = 10;

    private PatientPreference patientPreference;
    private DFPatientSubscription dfSubscription;
    private RequestAppointment requestAppointmentBehaviour;

    protected void setup() {
        System.out.println("Initialization of patient agent: " + getLocalName());

        initializeArguments();
        subscribeToDFAgents();
        addPatientBehaviour();
    }

    private void addPatientBehaviour() {

        requestAppointmentBehaviour = new RequestAppointment(dfSubscription);
        addBehaviour(requestAppointmentBehaviour);

    }

    private void initializeArguments() {
        Object[] args = getArguments();
        if (args != null && args.length == 1 && args[0] instanceof String) {
            patientPreference = new PatientPreference((String) args[0]);
        } else {
            throw new InvalidAgentInputException();
        }
    }

    private void subscribeToDFAgents() {
        // Subscribe with the DF to be notified of any agents that
        // provide the "allocate-appointments" service
        // Build the description used as template for the subscription
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription templateSd = new ServiceDescription();
        templateSd.setType(GlobalAgentConstants.APPOINTMENT_SERVICE_TYPE);
        template.addServices(templateSd);

        SearchConstraints sc = new SearchConstraints();
        // We want to receive 10 results at most
        sc.setMaxResults(MAX_AGENT_QUEUE);

        dfSubscription = new DFPatientSubscription(this, DFService.createSubscriptionMessage(this, getDefaultDF(), template, sc));
        addBehaviour(dfSubscription);
    }

    protected void takedown() {

    }

}
