package jadeCW.patient.patientBehaviour;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jadeCW.hospital.HospitalAgent;
import jadeCW.patient.DFPatientSubscription;

public class RequestAppointment extends Behaviour {

    private boolean done = false;
    private DFPatientSubscription dfSubscription;

    public RequestAppointment(DFPatientSubscription dfSubscription) {
        this.dfSubscription = dfSubscription;
    }

    @Override
    public void action() {
        // finding agent for appointment-service
        DFAgentDescription appointmentAgentDescription = dfSubscription.getAgentDescription();

        if(appointmentAgentDescription != null) {
            // appointment-service agent can allocate appointment

            // request appointment

            //
        }

    }

    @Override
    public boolean done() {
        return done;
    }
}
