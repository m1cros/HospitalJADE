package jadeCW;

import jade.content.Predicate;

public class AppointmentNotInPossession implements Predicate {

    private int currentAppointment;

    public int getCurrentAppointment() {
        return currentAppointment;
    }

    public void setCurrentAppointment(int currentAppointment) {
        this.currentAppointment = currentAppointment;
    }
}
