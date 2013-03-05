package jadeCW;

import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

import java.util.HashMap;
import java.util.Map;

public class HospitalAgent extends Agent {

    private int appointmentsNum = GlobalAgentConstants.APPOINTMENT_NUMBERS_NOT_INITIALIZED;
    private Map<Integer,AID> appointments = new HashMap<Integer, AID>();

    private final ContentManager contentManager = (ContentManager)getContentManager();
    private final Codec codec = new SLCodec();

    public Codec getCodec() {
        return codec;
    }

    public int getFreeAppointment() {
        int freeAppointment = GlobalAgentConstants.APPOINTMENT_NUMBERS_NOT_INITIALIZED;
        for (int i = 1; i <= appointmentsNum && freeAppointment == GlobalAgentConstants.APPOINTMENT_NUMBERS_NOT_INITIALIZED; ++i) {
            if (!appointments.containsKey(i)) {
                freeAppointment = i;
            }
        }

        return freeAppointment;
    }

    public void setAppointment(int appointmentTime, AID patientID) {
        appointments.put(appointmentTime,patientID);
    }

    public void removeAppointment(int appointmentTime) {
        appointments.remove(appointmentTime);
    }

    public int getAppointmentsNum() {
        return appointmentsNum;
    }

    public boolean isAppointmentFree(int allocation) {
        return !appointments.containsKey(allocation);
    }

    public String getAppointmentHolderID(int allocation) {
        return appointments.get(allocation).getName();
    }


    protected void setup() {
        contentManager.registerLanguage(codec);
        contentManager.registerOntology(HospitalOntology.getInstance());

        appointmentsNum = readInAppointments();

        registerService();

        addHospitalBehaviours();
    }

    private void addHospitalBehaviours() {
        AllocateAppointment allocateAppointment = new AllocateAppointment(this);
        RespondToQuery respondToQuery = new RespondToQuery(this);
        RespondToProposal2 respondToProposal2 = new RespondToProposal2(this);
        UpdateAppointments updateAppointments = new UpdateAppointments(this);

        addBehaviour(allocateAppointment);
        addBehaviour(respondToQuery);
        addBehaviour(respondToProposal2);
        addBehaviour(updateAppointments);
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
            throw new RuntimeException(fe);
        }

    }

    private int readInAppointments() {
        // Read the number of appointments from the input
        int appointments;
        Object[] args = getArguments();
        if (args != null && args.length == 1 && args[0] instanceof String) {
            appointments = Integer.parseInt((String) args[0]);
        } else {
            throw new RuntimeException();
        }

        return appointments;
    }

    protected void takeDown() {

        for (int i = 1; i <= appointmentsNum; ++i) {
            String appointmentName = null;
            if (appointments.containsKey(i)) {
                appointmentName = appointments.get(i).getLocalName();
            }
            System.out.println(getLocalName() + ": Appointment " + i  + ": " + appointmentName);
        }
    }

}
