package jadeCW;

import jade.content.Predicate;

public class SwapAllocationUpdate implements Predicate {

    private String holder;
    private int allocation;

    public String getHolder() {
        return holder;
    }

    public void setHolder(String holder) {
        this.holder = holder;
    }

    public int getAllocation() {
        return allocation;
    }

    public void setAllocation(int allocation) {
        this.allocation = allocation;
    }
}
