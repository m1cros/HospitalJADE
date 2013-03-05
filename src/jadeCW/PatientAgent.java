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


/*
*  PatientAgent class models the behaviour
*  of the patient. It starts from setting up
*  its arguments: reading in preference lists
*  and setting up its behaviours: requesting initial appointments,
*  find out owners of other appointments and swap appointments
*  with other patients if they are beneficial for both parties
*
* */
public class PatientAgent extends Agent {

    private PatientPreference patientPreference;
    private AllocationFinder allocationFinder;

    /* Subscription to hospital agent */
    private DFPatientSubscription dfSubscription;

    /* Current allocation of the patient agent, it is not initialized at first */
    private int currentAllocation = GlobalAgentConstants.APPOINTMENT_UNINITIALIZED;

    /* Ontology meta data */
    private final ContentManager contentManager = (ContentManager)getContentManager();
    private final Codec codec = new SLCodec();

    /* Sorted list of more preferred allocations and who owns them */
    private List<AllocationState> allocationStates = new ArrayList<AllocationState>();

    /* Patient behaviours */
    private RequestAppointment requestAppointmentBehaviour;
    private FindAppointmentOwner findAppointmentOwner;
    private ProposeSwap proposeSwapBehaviour;
    private RespondToProposal1 respondToProposal1;

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

    public boolean hasMadeSwapProposal() {
        return proposeSwapBehaviour.hasMadeSwapProposal();
    }

    public AgentAllocationSwap getCurrentlyProposedAllocationSwap() {
        return proposeSwapBehaviour.getCurrentlyProposedAllocationSwap();
    }

    /**
     * Initial set up of the patient method,
     * method is called when agent is created
     */
    protected void setup() {

        /* Setting up ontology of the agent */
        contentManager.registerLanguage(codec);
        contentManager.registerOntology(HospitalOntology.getInstance());

        /* Read in preference list */
        initializeArguments();

        /* Subscribe to hospital agent */
        subscribeToDFAgents();

        /* Creating class which will find owners of other appointments */
        allocationFinder = new AllocationFinder(this);

        /* Setting up behaviours */
        addPatientBehaviours();
    }

    /**
     * Initialization of behaviour classes
     * and adding them to patient
     */
    private void addPatientBehaviours() {

        requestAppointmentBehaviour = new RequestAppointment(dfSubscription, this);
        findAppointmentOwner = new FindAppointmentOwner(dfSubscription, this);
        proposeSwapBehaviour = new ProposeSwap(dfSubscription, this);
        respondToProposal1 = new RespondToProposal1(dfSubscription, this);

        addBehaviour(requestAppointmentBehaviour);
        addBehaviour(findAppointmentOwner);
        addBehaviour(proposeSwapBehaviour);
        addBehaviour(respondToProposal1);

    }


    /**
     * Method initializeArguments will read in arguments array
     * and concatenated it
     */
    private void initializeArguments() {
        Object[] args = getArguments();
        if (args != null && args.length > 1) {
            StringBuilder concatenatedArgs = new StringBuilder();
            for (int i = 0; i < args.length; ++i) {
                concatenatedArgs.append(args[i] + " ");
            }
            patientPreference = new PatientPreference(concatenatedArgs.toString());
        } else {
            patientPreference = new PatientPreference();
        }
    }

    /**
     * Subscribes to and find hospital agent
     */
    private void subscribeToDFAgents() {
        /* Subscribe with the DF to be notified of any agents that
        *  provide the "allocate-appointments" service
        *  Build the description used as template for the subscription  */

        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription templateSd = new ServiceDescription();
        templateSd.setType(GlobalAgentConstants.APPOINTMENT_SERVICE_TYPE);
        template.addServices(templateSd);

        SearchConstraints sc = new SearchConstraints();
        /* We want to receive 10 results at most
         * 10 is not supported by any facts, we could
         * use any number bigger than 0 as there is
         * only one hospital agent*/
        sc.setMaxResults(GlobalAgentConstants.MAX_AGENT_QUEUE);

        dfSubscription
            = new DFPatientSubscription(this, DFService.createSubscriptionMessage(this, getDefaultDF(), template, sc));
        addBehaviour(dfSubscription);
    }

    /**
     * Cleaning up after agents
     * In our case, it only prints its final appointment
     */
    protected void takeDown() {
        Integer allocation = null;
        if (currentAllocation != GlobalAgentConstants.APPOINTMENT_UNINITIALIZED) {
            allocation = currentAllocation;
        }
        System.out.println(getLocalName() + ": " + allocation);
    }

    /**
     * Updated the list which contains the preferred
     * allocations and who owns them
     */
    public void updatePreferredAllocations() {
        DFAgentDescription appointmentAgentDescription = dfSubscription.getAgentDescription();

        if (appointmentAgentDescription != null) {
            List<AllocationState> preferredAllocations = allocationFinder.getAllPreferredAllocations(appointmentAgentDescription,getCurrentAllocation());
            setAllocationStates(preferredAllocations);
        }
    }


}
