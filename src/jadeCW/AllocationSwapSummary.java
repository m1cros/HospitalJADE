package jadeCW;

import jade.content.Predicate;

public class AllocationSwapSummary implements Predicate {

    private int leftAllocation;
    private String leftHolder;

    private int rightAllocation;
    private String rightHolder;

    public int getLeftAllocation() {
        return leftAllocation;
    }

    public void setLeftAllocation(int leftAllocation) {
        this.leftAllocation = leftAllocation;
    }

    public String getLeftHolder() {
        return leftHolder;
    }

    public void setLeftHolder(String leftHolder) {
        this.leftHolder = leftHolder;
    }

    public int getRightAllocation() {
        return rightAllocation;
    }

    public void setRightAllocation(int rightAllocation) {
        this.rightAllocation = rightAllocation;
    }

    public String getRightHolder() {
        return rightHolder;
    }

    public void setRightHolder(String rightHolder) {
        this.rightHolder = rightHolder;
    }
}

