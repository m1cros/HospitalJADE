package jadeCW;

import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.ArrayList;
import java.util.List;

public class PatientAgent extends Agent {


    private PatientPreference patientPreference;
    private AllocationFinder allocationFinder;
    private DFPatientSubscription dfSubscription;
    private int currentAllocation = GlobalAgentConstants.APPOINTMENT_UNINITIALIZED;

    private final ContentManager contentManager = (ContentManager)getContentManager();
    private final Codec codec = new SLCodec();

    private List<AllocationState> allocationStates = new ArrayList<AllocationState>();

    public Codec getCodec() {
        return codec;
    }

    public AllocationFinder getAllocationFinder() {
         return allocationFinder;
     }

    public int getCurrentAllocation() {
        return currentAllocation;
    }

    public void setCurrentAllocation(int currentAllocation) {
        this.currentAllocation = currentAllocation;
    }

    public List<AllocationState> getAllocationStates() {
        return allocationStates;
    }

    public void setAllocationStates(List<AllocationState> allocationStates) {
        this.allocationStates = allocationStates;
    }

    public PatientPreference getPatientPreference() {
        return patientPreference;
     }

    protected void setup() {
        System.out.println("Initialization of patient agent: " + getLocalName());

        contentManager.registerLanguage(codec);
        contentManager.registerOntology(HospitalOntology.getInstance());

        initializeArguments();
        subscribeToDFAgents();

        allocationFinder = new AllocationFinder(this);

        addPatientBehaviours();

        System.out.println("Finished initialization of patient agent: " + getLocalName());
    }

    private void addPatientBehaviours() {

        RequestAppointment requestAppointmentBehaviour = new RequestAppointment(dfSubscription, this);
        FindAppointmentOwner findAppointmentOwner = new FindAppointmentOwner(dfSubscription, this);
        ProposeSwap proposeSwapBehaviour = new ProposeSwap(dfSubscription, this);
        RespondToProposal1 respondToProposal1 = new RespondToProposal1(dfSubscription, this);

        addBehaviour(requestAppointmentBehaviour);
        addBehaviour(findAppointmentOwner);
        addBehaviour(proposeSwapBehaviour);
        addBehaviour(respondToProposal1);

    }

    private void initializeArguments() {
        Object[] args = getArguments();
        if (args != null && args.length > 1 && args[0] instanceof String) {
            patientPreference = new PatientPreference((String) args[0]);
        } else {
            patientPreference = new PatientPreference();
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

        dfSubscription
            = new DFPatientSubscription(this, DFService.createSubscriptionMessage(this, getDefaultDF(), template, sc));
        addBehaviour(dfSubscription);
    }

    protected void takeDown() {
        Integer allocation = null;
        if (currentAllocation != GlobalAgentConstants.APPOINTMENT_UNINITIALIZED) {
            allocation = currentAllocation;
        }
        System.out.println(getLocalName() + ": " + allocation);
    }

    public void updatePreferredAllocations() {
        DFAgentDescription appointmentAgentDescription = dfSubscription.getAgentDescription();
        if (appointmentAgentDescription != null) {
            List<AllocationState> preferredAllocations
                    = allocationFinder.getAllPreferredAllocations(appointmentAgentDescription,
                                                                  getCurrentAllocation());
            setAllocationStates(preferredAllocations);
        }
    }


}
