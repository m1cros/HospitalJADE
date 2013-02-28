package jadeCW;

import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.proto.SubscriptionInitiator;
import jade.util.leap.Iterator;

import java.util.ArrayList;
import java.util.List;

public class DFPatientSubscription extends SubscriptionInitiator {

    private List<DFAgentDescription> allocateApptsAgents = new ArrayList<DFAgentDescription>();

    public DFPatientSubscription(Agent a, ACLMessage msg) {
        super(a, msg);
    }

    public synchronized DFAgentDescription getAgentDescription() {

        if(!allocateApptsAgents.isEmpty()) {
            return allocateApptsAgents.get(0);
        } else {
            return null;
        }

    }


    protected synchronized void handleInform(ACLMessage inform) {

        System.out.println("Handling information: " + inform.toString());

        try {
            DFAgentDescription[] results = DFService.decodeNotification(inform.getContent());
            if (results.length > 0) {
                for (int i = 0; i < results.length; ++i) {
                    DFAgentDescription dfd = results[i];
                    AID provider = dfd.getName();
                    // The same agent may provide several services; we are only interested
                    // in the allocate-appointments one
                    Iterator it = dfd.getAllServices();
                    while (it.hasNext()) {
                        ServiceDescription sd = (ServiceDescription) it.next();
                        if (sd.getType().equals(GlobalAgentConstants.APPOINTMENT_SERVICE_TYPE)) {
                            allocateApptsAgents.add(dfd);
                            System.out.println("allocate-appointments service found:");
                            System.out.println("- Service \"" + sd.getName() + "\" provided by agent " + provider.getName());
                        }
                    }
                }
            }

        } catch (FIPAException fe) {
            throw new ServiceRegistrationException(fe);
        }

    }

}
