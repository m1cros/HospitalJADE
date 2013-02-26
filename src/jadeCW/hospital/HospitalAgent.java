package jadeCW.hospital;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jadeCW.InvalidAgentInputException;
import jadeCW.ServiceRegistrationException;
import jadeCW.utils.GlobalAgentConstants;

public class HospitalAgent extends Agent {

    private int appointmentsNum = GlobalAgentConstants.APPOINTMENT_NUMBERS_NOT_INITIALIZED;

    protected void setup() {

        appointmentsNum = readInAppointments();
        registerService();

    }

    private void registerService() {

        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());

            ServiceDescription sd = new ServiceDescription();
            sd.setType(GlobalAgentConstants.APPOINTMENT_SERVICE_TYPE);

            dfd.addServices(sd);
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            throw new ServiceRegistrationException(fe);
        }

    }

    private int readInAppointments() {
        // Read the number of appointments from the input
        int appointments;
        Object[] args = getArguments();
        if (args != null && args.length == 1 && args[0] instanceof String) {
            appointments = new Integer((String) args[0]);
        } else {
            throw new InvalidAgentInputException();
        }

        return appointments;
    }


}
