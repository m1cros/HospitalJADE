package jadeCW;

import jade.content.Predicate;

public class AllocationSwapSummary implements Predicate {

    private String proposingAgent;
    private String receivingAgent;
    private int proposingAgentOldAppointment;
    private int receivingAgentOldAppointment;

    public String getProposingAgent() {
        return proposingAgent;
    }

    public void setProposingAgent(String proposingAgent) {
        this.proposingAgent = proposingAgent;
    }

    public String getReceivingAgent() {
        return receivingAgent;
    }

    public void setReceivingAgent(String receivingAgent) {
        this.receivingAgent = receivingAgent;
    }

    public int getReceivingAgentOldAppointment() {
        return receivingAgentOldAppointment;
    }

    public void setReceivingAgentOldAppointment(int receivingAgentOldAppointment) {
        this.receivingAgentOldAppointment = receivingAgentOldAppointment;
    }

    public int getProposingAgentOldAppointment() {
        return proposingAgentOldAppointment;
    }

    public void setProposingAgentOldAppointment(int proposingAgentOldAppointment) {
        this.proposingAgentOldAppointment = proposingAgentOldAppointment;
    }

    public String toString() {
        return "proposing agent: " + proposingAgent +
                " proposing agent old appointment: " + proposingAgentOldAppointment +
                " receiving agent: " + receivingAgent +
                " receiving agent old appointment: " + receivingAgentOldAppointment;
    }

}

