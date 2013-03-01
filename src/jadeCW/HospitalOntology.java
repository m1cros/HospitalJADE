package jadeCW;

import jade.content.onto.BasicOntology;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
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

    public static final String APPOINTMENT_SWAP_REQUEST = "APPOINTMENT_SWAP_REQUEST";
    public static final String APPOINTMENT_SWAP_REQUEST_CURRENT_ALLOCATION = "currentAllocation";
    public static final String APPOINTMENT_SWAP_REQUEST_DESIRED_ALLOCATION = "desiredAllocation";

    public static final String ALLOCATION_SWAP_SUMMARY = "ALLOCATION_SWAP_SUMMARY";
    public static final String ALLOCATION_SWAP_SUMMARY_PROPOSING_APPOINTMENT = "proposingAgentOldAppointment";
    public static final String ALLOCATION_SWAP_SUMMARY_PROPOSING_AGENT = "proposingAgent";
    public static final String ALLOCATION_SWAP_SUMMARY_RECEIVING_APPOINTMENT = "receivingAgentOldAppointment";
    public static final String ALLOCATION_SWAP_SUMMARY_RECEIVING_AGENT = "receivingAgent";

    public static final String APPOINTMENT_NOT_IN_POSSESSION = "APPOINTMENT_NOT_IN_POSSESSION";
    public static final String APPOINTMENT_NOT_IN_POSSESSION_CUR_APP = "currentAppointment";

    public static final String APPOINTMENT_NOT_PREFERRED = "APPOINTMENT_NOT_PREFERRED";

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
            PrimitiveSchema stringSchema = (PrimitiveSchema) getSchema(BasicOntology.STRING);
            PrimitiveSchema integerSchema = (PrimitiveSchema) getSchema(BasicOntology.INTEGER);

            PredicateSchema appointmentSchema = new PredicateSchema(APPOINTMENT);
            appointmentSchema.add(ALLOCATION, integerSchema, ObjectSchema.MANDATORY);
            add(appointmentSchema, Appointment.class);

            PredicateSchema appointmentQuerySchema = new PredicateSchema(APPOINTMENT_QUERY);
            appointmentQuerySchema.add(APPOINTMENT_QUERY_TIME, integerSchema, ObjectSchema.MANDATORY);
            appointmentQuerySchema.add(APPOINTMENT_QUERY_STATE, stringSchema, ObjectSchema.MANDATORY);
            appointmentQuerySchema.add(APPOINTMENT_QUERY_HOLDER, stringSchema, ObjectSchema.OPTIONAL);
            add(appointmentQuerySchema, AppointmentQuery.class);

            PredicateSchema appointmentSwapRequestSchema = new PredicateSchema(APPOINTMENT_SWAP_REQUEST);
            appointmentSwapRequestSchema.add(APPOINTMENT_SWAP_REQUEST_CURRENT_ALLOCATION, integerSchema, ObjectSchema.MANDATORY);
            appointmentSwapRequestSchema.add(APPOINTMENT_SWAP_REQUEST_DESIRED_ALLOCATION, integerSchema, ObjectSchema.MANDATORY);
            add(appointmentSwapRequestSchema, AgentAllocationSwap.class);

            PredicateSchema allocationSwapSummary = new PredicateSchema(ALLOCATION_SWAP_SUMMARY);
            allocationSwapSummary.add(ALLOCATION_SWAP_SUMMARY_PROPOSING_APPOINTMENT, integerSchema, ObjectSchema.MANDATORY);
            allocationSwapSummary.add(ALLOCATION_SWAP_SUMMARY_PROPOSING_AGENT, stringSchema, ObjectSchema.MANDATORY);
            allocationSwapSummary.add(ALLOCATION_SWAP_SUMMARY_RECEIVING_APPOINTMENT, integerSchema, ObjectSchema.MANDATORY);
            allocationSwapSummary.add(ALLOCATION_SWAP_SUMMARY_RECEIVING_AGENT, stringSchema, ObjectSchema.MANDATORY);
            add(allocationSwapSummary,AllocationSwapSummary.class);

            PredicateSchema appointmentNotInPossession = new PredicateSchema(APPOINTMENT_NOT_IN_POSSESSION);
            appointmentNotInPossession.add(APPOINTMENT_NOT_IN_POSSESSION_CUR_APP, integerSchema, ObjectSchema.OPTIONAL);
            add(appointmentNotInPossession, AppointmentNotInPossession.class);

            PredicateSchema appointmentNotPreferred = new PredicateSchema(APPOINTMENT_NOT_PREFERRED);
            add(appointmentNotPreferred, AppointmentNotPreferred.class);

        } catch (OntologyException e) {
            throw new RuntimeException(e);
        }


    }

}
