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

    private final Set<AllocationSwapSummary> swappedAppointments = new TreeSet<AllocationSwapSummary>();
    private final HospitalAgent hospitalAgent;

    public UpdateAppointments(HospitalAgent hospitalAgent) {
        this.hospitalAgent = hospitalAgent;
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
                throw new RuntimeException();
            }

            System.out.println("Received update message : " + message);
            System.out.println("With summary: " + allocationSwapSummary);

            AID proposingAgentAID = new AID(allocationSwapSummary.getProposingAgent(), AID.ISGUID);
            AID receivingAgentAID = new AID(allocationSwapSummary.getReceivingAgent(), AID.ISGUID);

            if (swappedAppointments.contains(allocationSwapSummary)) {

                hospitalAgent.setAppointment(allocationSwapSummary.getProposingAgentOldAppointment(), proposingAgentAID);
                hospitalAgent.setAppointment(allocationSwapSummary.getReceivingAgentOldAppointment(), receivingAgentAID);
                swappedAppointments.remove(allocationSwapSummary);

            } else {

                swappedAppointments.add(allocationSwapSummary);

            }
        }

    }

}
