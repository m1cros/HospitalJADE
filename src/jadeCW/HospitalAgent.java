package jadeCW;

import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

public class HospitalAgent extends Agent {

    private int appointmentsNum = GlobalAgentConstants.APPOINTMENT_NUMBERS_NOT_INITIALIZED;
    private AID[] appointments;
    private AllocateAppointment allocateAppointment;

    public int getFreeAppointment() {
        int freeAppointment = GlobalAgentConstants.APPOINTMENT_NUMBERS_NOT_INITIALIZED;
        for(int i = 0;i < appointmentsNum && freeAppointment == GlobalAgentConstants.APPOINTMENT_NUMBERS_NOT_INITIALIZED;++i) {
            if(appointments[i] == null) {
                freeAppointment = i;
            }
        }

        return freeAppointment;
    }

    public void setAppointment(int appointmentTime, AID patientID) {
        appointments[appointmentTime] = patientID;
    }

    protected void setup() {
        System.out.println("Initialization of hospital agent: " + getLocalName());

        appointmentsNum = readInAppointments();
        initializeEmptyAppointments();
        registerService();

        allocateAppointment = new AllocateAppointment(this);
        addBehaviour(allocateAppointment);
    }

    private void initializeEmptyAppointments() {
        appointments = new AID[appointmentsNum];
    }

    private void registerService() {

        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());

            ServiceDescription sd = new ServiceDescription();
            sd.setType(GlobalAgentConstants.APPOINTMENT_SERVICE_TYPE);
            sd.setName(getName() + "ServiceDescription");

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
            appointments = Integer.parseInt((String) args[0]);
        } else {
            throw new InvalidAgentInputException();
        }

        return appointments;
    }

    protected void takeDown() {

        for(int i = 0;i < appointments.length;++i) {
            System.out.println(getName() + ": Appointment " + (i + 1) + ": " + appointments[i]);
        }
    }



}
