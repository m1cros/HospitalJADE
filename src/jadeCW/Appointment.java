package jadeCW;

import jade.content.Predicate;

public class Appointment implements Predicate
{

    private int allocation;

    public int getAllocation() {
        return allocation;
    }

    public void setAllocation(int allocation) {
        this.allocation = allocation;
    }
}
