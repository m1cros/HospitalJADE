package jadeCW;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;

public class HospitalAgent extends Agent {

    public static final String serviceType = "allocate-appointments";

    protected void setup() {

        Integer appointmentsNum = 0;

       	// Read the number of appointments from the input
       	Object[] args = getArguments();
       	if (args != null && args.length > 0) {
            appointmentsNum = new Integer((String) args[0]);
       	}

        // Register the service
        System.out.println("Agent " + getLocalName()+ " registering service of type " + serviceType);
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType(serviceType);
            dfd.addServices(sd);
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }
}
