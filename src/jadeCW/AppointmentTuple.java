package jadeCW;


public class AppointmentTuple {
    
    int a;
    int b;

    public AppointmentTuple(int a, int b) {
        this.a = a;
        this.b = b;
    }

    public boolean equals(Object obj) {
        if (obj instanceof AppointmentTuple) {
            AppointmentTuple cmp = (AppointmentTuple)obj;
            return ((a == cmp.a) && (b == cmp.b));
        }
        return false;
    }

    public int hashCode() {
        return a*b;
    }
    
    
    
}
