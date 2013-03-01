package jadeCW;

import jade.content.Predicate;

public class AllocationSwapSummary implements Predicate {

    private String proposingAgent;
    private String receivingAgent;
    private String timestamp;
    private int proposingAgentOldAppointment;
    private int receivingAgentOldAppointment;

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AllocationSwapSummary that = (AllocationSwapSummary) o;
        AllocationSwapSummary revertedThat = that.getRevertedAllocationSwapSummary();

        return isTheSame(that) || isTheSame(revertedThat);
    }

    @Override
    public int hashCode() {
        int result = proposingAgent.hashCode();
        result += receivingAgent.hashCode();
        result += timestamp.hashCode();
        result += proposingAgentOldAppointment;
        result += receivingAgentOldAppointment;
        return result;
    }

    public AllocationSwapSummary getRevertedAllocationSwapSummary() {
        AllocationSwapSummary allocationSwapSummary = new AllocationSwapSummary();
        allocationSwapSummary.setTimestamp(getTimestamp());

        allocationSwapSummary.setReceivingAgent(getProposingAgent());
        allocationSwapSummary.setReceivingAgentOldAppointment(getProposingAgentOldAppointment());

        allocationSwapSummary.setProposingAgent(getReceivingAgent());
        allocationSwapSummary.setProposingAgentOldAppointment(getReceivingAgentOldAppointment());

        return allocationSwapSummary;
    }

    public boolean isTheSame(AllocationSwapSummary that) {
        if (proposingAgentOldAppointment != that.proposingAgentOldAppointment) return false;
        if (receivingAgentOldAppointment != that.receivingAgentOldAppointment) return false;
        if (!proposingAgent.equals(that.proposingAgent)) return false;
        if (!receivingAgent.equals(that.receivingAgent)) return false;
        if (!timestamp.equals(that.timestamp)) return false;

        return true;
    }
}

