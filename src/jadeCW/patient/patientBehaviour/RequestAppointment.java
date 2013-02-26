package jadeCW.patient.patientBehaviour;

import jade.core.behaviours.Behaviour;

public class RequestAppointment extends Behaviour {

    private boolean done = false;

    @Override
    public void action() {

    }

    @Override
    public boolean done() {
        return done;
    }
}
