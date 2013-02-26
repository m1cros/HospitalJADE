package jadeCW.hospital;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jadeCW.ServiceRegistrationException;

public class HospitalAgent extends Agent {

    private static final String SERVICE_TYPE = "allocate-appointments";
    private static final int NOT_INITIALIZED = -1;
    private int appointmentsNum = NOT_INITIALIZED;

    protected void setup() {

        appointmentsNum = readInAppointments();
        // Register the service
        System.out.println("Agent " + getLocalName() + " registering service of type " + SERVICE_TYPE);

        try {
            registerService();
        } catch (FIPAException fe) {
            throw new ServiceRegistrationException(fe);
        }

        System.out.println("Successful setup of hospital agent: " + getLocalName());
    }

    private void registerService() throws FIPAException {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        ServiceDescription sd = new ServiceDescription();
        sd.setType(SERVICE_TYPE);

        dfd.addServices(sd);
        DFService.register(this, dfd);
    }

    private int readInAppointments() {
        // Read the number of appointments from the input
        int appointments;
        Object[] args = getArguments();
        if (args != null && args.length == 1 && args[0] instanceof String) {
            appointments = new Integer((String) args[0]);
        } else {
            throw new InvalidAppointmentFormatException();
        }

        return appointments;
    }


}
