package jadeCW;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class RespondToQuery extends CyclicBehaviour {

    private final HospitalAgent hospitalAgent;

    public RespondToQuery(HospitalAgent hospitalAgent) {
        this.hospitalAgent = hospitalAgent;
    }

    @Override
    public void action() {
        ACLMessage message = hospitalAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF));

        if (message != null) {
            String queryAllocation = message.getUserDefinedParameter(GlobalAgentConstants.APPOINTMENT_QUERY_FIELD);
            int allocation = Integer.parseInt(queryAllocation);

            ACLMessage messageResponse = new ACLMessage(ACLMessage.QUERY_REF);
            messageResponse.setSender(hospitalAgent.getAID());
            messageResponse.addReceiver(message.getSender());

            if (allocation < 0 || allocation >= hospitalAgent.getAppointmentsNum()) {

                messageResponse.addUserDefinedParameter(
                        GlobalAgentConstants.APPOINTMENT_QUERY_RESPONSE_STATUS,
                        GlobalAgentConstants.APPOINTMENT_QUERY_RESPONSE_STATUS_INVALID);

            } else if (hospitalAgent.isAppointmentFree(allocation)) {

                messageResponse.addUserDefinedParameter(
                        GlobalAgentConstants.APPOINTMENT_QUERY_RESPONSE_STATUS,
                        GlobalAgentConstants.APPOINTMENT_QUERY_RESPONSE_STATUS_FREE);

            } else {

                messageResponse.addUserDefinedParameter(
                        GlobalAgentConstants.APPOINTMENT_QUERY_RESPONSE_STATUS,
                        GlobalAgentConstants.APPOINTMENT_QUERY_RESPONSE_STATUS_ALLOCATED);

                messageResponse.addUserDefinedParameter(
                        GlobalAgentConstants.APPOINTMENT_QUERY_RESPONSE_AGENT_ID,
                        hospitalAgent.getAppointmentHolderID(allocation));

            }

            hospitalAgent.send(messageResponse);
        }

    }

}
