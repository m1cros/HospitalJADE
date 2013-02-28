package jadeCW;

import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.onto.OntologyException;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.*;


public class UpdateAppointments extends CyclicBehaviour {


    Map<AppointmentTuple, Set<AID>> swappedAppointments;

    private final HospitalAgent hospitalAgent;

    public UpdateAppointments(HospitalAgent hospitalAgent) {
        this.hospitalAgent = hospitalAgent;
        swappedAppointments = new HashMap<AppointmentTuple, Set<AID>>();
    }

    @Override
    public void action() {

        MessageTemplate messageTemplateAccept = MessageTemplate.and(
                MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE),
                MessageTemplate.and(
                        MessageTemplate.MatchOntology(HospitalOntology.NAME),
                        MessageTemplate.and(
                                MessageTemplate.MatchLanguage(hospitalAgent.getCodec().getName()),
                                MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL)
                        )
                )
        );

        ACLMessage message = hospitalAgent.receive(messageTemplateAccept);

        if (message != null) {

            ContentElement p = null;
            AllocationSwapSummary allocationSwapSummary;

            try {
                p = hospitalAgent.getContentManager().extractContent(message);
            } catch (Codec.CodecException e) {
                throw new RuntimeException(e);
            } catch (OntologyException e) {
                throw new RuntimeException(e);
            }

            if (p instanceof AllocationSwapSummary) {
                allocationSwapSummary = (AllocationSwapSummary) p;
            } else {
                throw new RuntimeException();
            }


            int leftAllocation = allocationSwapSummary.getLeftAllocation();
            int rightAllocation = allocationSwapSummary.getRightAllocation();
            String leftAllocationHolder = allocationSwapSummary.getLeftHolder();
            String rightAllocationHolder = allocationSwapSummary.getRightHolder();

            AID leftAllocationHolderAID = new AID(leftAllocationHolder, AID.ISGUID);
            AID rightAllocationHolderAID = new AID(rightAllocationHolder, AID.ISGUID);

            AppointmentTuple key = new AppointmentTuple(leftAllocation, rightAllocation);
            if (swappedAppointments.containsKey(key)) {
                Set<AID> swappers = swappedAppointments.get(key);
                if (swappers.contains(leftAllocation) && swappers.contains(rightAllocation)) {
                    //
                    hospitalAgent.setAppointment(leftAllocation, leftAllocationHolderAID);
                    hospitalAgent.setAppointment(rightAllocation, rightAllocationHolderAID);
                }

                // remove from swappedAppointsment
                swappedAppointments.remove(key);
            } else {
                //add to map
                Set<AID> owners = new HashSet<AID>();
                owners.add(leftAllocationHolderAID);
                owners.add(rightAllocationHolderAID);
                swappedAppointments.put(new AppointmentTuple(leftAllocation, rightAllocation), owners);
            }
        }

    }

}
