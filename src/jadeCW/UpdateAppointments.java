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
                                MessageTemplate.MatchPerformative(ACLMessage.INFORM)
                        )
                )
        );

        ACLMessage message = hospitalAgent.receive(messageTemplateAccept);

        if (message != null) {

            ContentElement p;
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
                return;
            }

            System.out.println("Received update message : " + message);
            System.out.println("With summary: " + allocationSwapSummary);

            int proposingAgentOldAppointment = allocationSwapSummary.getProposingAgentOldAppointment();
            int receivingAgentOldAppointment = allocationSwapSummary.getReceivingAgentOldAppointment();
            String proposingAgent = allocationSwapSummary.getProposingAgent();
            String receivingAgent = allocationSwapSummary.getReceivingAgent();

            AID proposingAgentAID = new AID(proposingAgent, AID.ISGUID);
            AID receivingAgentAID = new AID(receivingAgent, AID.ISGUID);

            AppointmentTuple key = new AppointmentTuple(proposingAgentOldAppointment, receivingAgentOldAppointment);
            if (swappedAppointments.containsKey(key)) {
                Set<AID> swappers = swappedAppointments.get(key);
                swappers.add(message.getSender());
                if (swappers.contains(proposingAgentOldAppointment)
                        && swappers.contains(receivingAgentOldAppointment)) {
                    // we received both confirmations, update appointment table
                    System.out.println("Swapping!");
                    hospitalAgent.setAppointment(receivingAgentOldAppointment, proposingAgentAID);
                    hospitalAgent.setAppointment(proposingAgentOldAppointment, receivingAgentAID);
                    swappedAppointments.remove(key);
                }
            } else {
                // we received the first confirmation
                Set<AID> owners = new HashSet<AID>();
                owners.add(message.getSender());
                swappedAppointments.put(new AppointmentTuple(proposingAgentOldAppointment,
                        receivingAgentOldAppointment), owners);
            }
        }

    }

}
