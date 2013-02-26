package jadeCW;

import jade.core.Agent;

@SuppressWarnings("serial")
public class PatientAgent extends Agent {

	protected void setup() {
		System.out.println("Hello world, my name is " + getLocalName());
		
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			
			
			
		}
		
	}
	
	protected void takedown() {
		
	}
	
}
