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

        } catch (OntologyException e) {
            throw new RuntimeException(e);
        }


    }

}
