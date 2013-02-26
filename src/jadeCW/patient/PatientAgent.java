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

    private PatientPreference patientPreference;
    private DFPatientSubscription dfSubscription;
    private RequestAppointment requestAppointmentBehaviour;
    private int currentAllocation = GlobalAgentConstants.APPOINTMENT_UNINITIALIZED;

    public int getCurrentAllocation() {
        return currentAllocation;
    }

    public void setCurrentAllocation(int currentAllocation) {
        this.currentAllocation = currentAllocation;
    }

    protected void setup() {
        System.out.println("Initialization of patient agent: " + getLocalName());

        initializeArguments();
        subscribeToDFAgents();
        addPatientBehaviour();
    }

    private void addPatientBehaviour() {

        requestAppointmentBehaviour = new RequestAppointment(dfSubscription, this);
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
        sc.setMaxResults(GlobalAgentConstants.MAX_AGENT_QUEUE);

        dfSubscription = new DFPatientSubscription(this, DFService.createSubscriptionMessage(this, getDefaultDF(), template, sc));
        addBehaviour(dfSubscription);
    }

    protected void takedown() {
        Integer allocation = null;
        if (currentAllocation != GlobalAgentConstants.APPOINTMENT_UNINITIALIZED) {
            allocation = currentAllocation;
        }
        System.out.println(getName() + ": " + allocation);
    }


}
