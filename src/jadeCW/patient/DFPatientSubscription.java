package jadeCW.patient;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.DataStore;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.proto.SubscriptionInitiator;
import jade.util.leap.Iterator;
import jadeCW.ServiceRegistrationException;
import jadeCW.utils.GlobalAgentConstants;

import java.util.ArrayList;
import java.util.List;

public class DFPatientSubscription extends SubscriptionInitiator {

    private List<DFAgentDescription> allocateApptsAgents = new ArrayList<DFAgentDescription>();

    public DFPatientSubscription(Agent a, ACLMessage msg) {
        super(a, msg);
    }


    protected void handleInform(ACLMessage inform) {

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
