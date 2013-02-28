package jadeCW;

import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class UpdateAppointments extends CyclicBehaviour {


    Map<AppointmentTuple, ArrayList<AID>> swappedAppointments;

    private final HospitalAgent hospitalAgent;

    public UpdateAppointments(HospitalAgent hospitalAgent) {
        this.hospitalAgent = hospitalAgent;
        swappedAppointments = new HashMap<AppointmentTuple, ArrayList<AID>>();
    }

    @Override
    public void action() {

        receiveSwapMessage();

    }

    private void receiveSwapMessage() {

        ACLMessage message = hospitalAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));

        if (message != null) {

            int oldAppointment = 0;
            int newAppointment = 0;
            AID ownerOldAppointment = null;
            AID ownerNewAppointment = message.getSender();


            AppointmentTuple key = new AppointmentTuple(oldAppointment, newAppointment);
            if (swappedAppointments.containsKey(key)) {
                ArrayList<AID> swappers = swappedAppointments.get(key);
                if (swappers.contains(ownerOldAppointment) && swappers.contains(ownerNewAppointment)) {
                    //
                    hospitalAgent.setAppointment(newAppointment, ownerNewAppointment);
                    hospitalAgent.setAppointment(oldAppointment, ownerOldAppointment);
                }

                // remove from swappedAppointsment
                swappedAppointments.remove(key);
            } else {
                //add to map
                ArrayList<AID> owners = new ArrayList<AID>();
                owners.add(ownerOldAppointment);
                owners.add(ownerNewAppointment);
                swappedAppointments.put(new AppointmentTuple(oldAppointment, newAppointment), owners);
            }
        }
    }
}
