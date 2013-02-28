package jadeCW;

public class AllocationState {

    private final int appointment;
    private final String appointmentStatus;
    private final String appointmentHolder;


    public AllocationState(int appointment, String appointmentStatus, String appointmentHolder) {
        this.appointment = appointment;
        this.appointmentStatus = appointmentStatus;
        this.appointmentHolder = appointmentHolder;
    }

    public int getAppointment() {
        return appointment;
    }

    public String getAppointmentStatus() {
        return appointmentStatus;
    }

    public String getAppointmentHolder() {
        return appointmentHolder;
    }
}
