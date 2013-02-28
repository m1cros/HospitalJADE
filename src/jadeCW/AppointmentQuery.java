package jadeCW;

import jade.content.Predicate;

public class AppointmentQuery implements Predicate
{
    private String state;
    private String holder;
    private int allocation;

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public int getAllocation() {
        return allocation;
    }

    public void setAllocation(int allocation) {
        this.allocation = allocation;
    }

    public String getHolder() {
        return holder;
    }

    public void setHolder(String holder) {
        this.holder = holder;
    }

    public String toString() {
        return "Allocation: " + allocation + " holder " + holder;
    }
}
