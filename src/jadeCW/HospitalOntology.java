package jadeCW;

import jade.content.onto.BasicOntology;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.schema.ConceptSchema;
import jade.content.schema.ObjectSchema;
import jade.content.schema.PredicateSchema;
import jade.content.schema.PrimitiveSchema;

public class HospitalOntology extends Ontology {

    /**
     * A symbolic constant, containing the name of this ontology.
     */
    public static final String NAME = "hospital-ontology";
    public static final String APPOINTMENT = "APPOINTMENT";
    public static final String ALLOCATION = "ALLOCATION";

    public static final String APPOINTMENT_QUERY = "APPOINTMENT_QUERY";
    public static final String APPOINTMENT_QUERY_TIME = "allocation";
    public static final String APPOINTMENT_QUERY_STATE = "state";
    public static final String APPOINTMENT_QUERY_HOLDER = "holder";

    private static Ontology theInstance = new HospitalOntology();

    public static Ontology getInstance() {
        return theInstance;
    }

    /**
     * Constructor
     */
    private HospitalOntology() {
        super(NAME, BasicOntology.getInstance());

        try {
            PrimitiveSchema stringSchema = (PrimitiveSchema)getSchema(BasicOntology.STRING);
            PrimitiveSchema integerSchema = (PrimitiveSchema)getSchema(BasicOntology.INTEGER);

            PredicateSchema appointmentSchema = new PredicateSchema(APPOINTMENT);
            appointmentSchema.add(ALLOCATION, integerSchema,  ObjectSchema.MANDATORY);

            add(appointmentSchema, Appointment.class);

            PredicateSchema appointmentQuerySchema = new PredicateSchema(APPOINTMENT_QUERY);
            appointmentQuerySchema.add(APPOINTMENT_QUERY_TIME, integerSchema, ObjectSchema.MANDATORY);
            appointmentQuerySchema.add(APPOINTMENT_QUERY_STATE, stringSchema, ObjectSchema.MANDATORY);
            appointmentQuerySchema.add(APPOINTMENT_QUERY_HOLDER, stringSchema, ObjectSchema.OPTIONAL);
            add(appointmentQuerySchema, AppointmentQuery.class);

        } catch (OntologyException e) {
            throw new RuntimeException(e);
        }


    }

}
