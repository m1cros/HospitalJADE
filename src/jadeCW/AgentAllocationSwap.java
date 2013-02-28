package jadeCW;

import jade.content.Predicate;
public class AgentAllocationSwap implements Predicate  {

    private int currentAllocation;
    private int desiredAllocation;

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
