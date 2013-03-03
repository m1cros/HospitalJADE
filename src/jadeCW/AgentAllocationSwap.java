package jadeCW;

import jade.content.Predicate;
public class AgentAllocationSwap implements Predicate  {

    private int currentAllocation;
    private int desiredAllocation;
    private String timestamp;

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public int getCurrentAllocation() {
        return currentAllocation;
    }

    public void setCurrentAllocation(int currentAllocation) {
        this.currentAllocation = currentAllocation;
    }

    public int getDesiredAllocation() {
        return desiredAllocation;
    }

    public void setDesiredAllocation(int desireAllocation) {
        this.desiredAllocation = desireAllocation;
    }


}
