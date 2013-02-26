package jadeCW;

import java.util.ArrayList;

import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.proto.SubscriptionInitiator;
import jade.util.leap.Iterator;

@SuppressWarnings("serial")
public class PatientAgent extends Agent {

	private ArrayList<ArrayList<Integer>> apptPref;
	private ArrayList<DFAgentDescription> allocateApptsAgents;
	
	public PatientAgent() {
		super();
		apptPref = new ArrayList<ArrayList<Integer>>();
		allocateApptsAgents = new ArrayList<DFAgentDescription>();
	}
	
	protected void setup() {
		System.out.println("Hello world, my name is " + getLocalName());
		
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			createPreferencesList(args);
		}

		// Subscribe with the DF to be notified of any agents that 
		// provide the "allocate-appointments" service
		// Build the description used as template for the subscription
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription templateSd = new ServiceDescription();
		templateSd.setType("allocate-appointments");
		template.addServices(templateSd);
		
		SearchConstraints sc = new SearchConstraints();
		// We want to receive 10 results at most
		sc.setMaxResults(new Long(10));
  		
		addBehaviour(new SubscriptionInitiator(this, DFService.createSubscriptionMessage(this, getDefaultDF(), template, sc)) {
			protected void handleInform(ACLMessage inform) {
  			System.out.println("Agent "+getLocalName()+": Notification received from DF");
  			try {
				DFAgentDescription[] results = DFService.decodeNotification(inform.getContent());
		  		if (results.length > 0) {
		  			for (int i = 0; i < results.length; ++i) {
		  				DFAgentDescription dfd = results[i];
		  				AID provider = dfd.getName();
		  				// The same agent may provide several services; we are only interested
		  				// in the allocate-appointments one
		  				Iterator it = dfd.getAllServices();
		  				while (it.hasNext()) {
		  					ServiceDescription sd = (ServiceDescription) it.next();
		  					if (sd.getType().equals("allocate-appointments")) {
		  						allocateApptsAgents.add(dfd);
	  							System.out.println("allocate-appointments service found:");
		  						System.out.println("- Service \""+sd.getName()+"\" provided by agent "+provider.getName());
		  					}
		  				}
		  			}
		  		}	
		  	}
		  	catch (FIPAException fe) {
		  		fe.printStackTrace();
		  	}
			}
		} );
		
	}

	protected void takedown() {

	}
	
	private void createPreferencesList(Object[] args) {
		String prefs = (String) args[0];
		String[] splitPrefs = prefs.split("-");
		for (String splitPref : splitPrefs) {
			// trim the whitespace
			splitPref = splitPref.trim();
			// split these by the spaces
			String[] appointments = splitPref.split(" ");
			ArrayList<Integer> currentLevelPrefs = new ArrayList<Integer>();
			for (String appointment : appointments) {
				Integer appt = Integer.parseInt(appointment.trim());
				currentLevelPrefs.add(appt);
			}
			apptPref.add(currentLevelPrefs);
			
		}
	}
	
	
	
}
