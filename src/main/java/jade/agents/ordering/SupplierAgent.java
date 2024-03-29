package jade.agents.ordering;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class SupplierAgent extends Agent {
	//  Catalog of available items, the number of each item in stock and their prices.
	private ItemList _catalog;
	

	// Initialize agent
	protected void setup() {
	   
		// CSV file containing list of items in catalog is passed in as the agent argument
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			String csvFilePath = (String)args[0];
			System.out.println(getAID().getName() + ") Reading catalog from '" + csvFilePath + "'");
			_catalog = new ItemList(csvFilePath);
		} else {
			System.err.println("No catalog file name in agent command lines");
			// Terminate agent
			System.out.println("No target book title specified");
			doDelete();
		}
		System.out.println(getAID().getName() + ") Catalog has " + _catalog.getPartLists().size() + " rows worth $" + _catalog.getPriceInDollars());

		// Register this Supplier service in the Yellow Pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("supplier");
		sd.setName("JADE-supplier");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}

		// Add the behaviour serving queries from buyer agents
		addBehaviour(new OfferRequestsServer());

		// Add the behaviour serving purchase orders from buyer agents
		addBehaviour(new PurchaseOrdersServer());
		
		// Show the Agent starting
		System.out.println("Starting Supplier Agent " + getAID().getName());
	}

	//  Clean-up operations here
	protected void takeDown() {
		// Deregister from the Yellow Pages
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		
		// Show termination in the console
		System.out.println("Supplier Agent " + getAID().getName() + " terminating.");
	}

		
	/**
	   Inner class OfferRequestsServer.
	   This is the behaviour used by the Supplier Agnet to process order requests 
	   from Buyer Agents.
	   If the order can be fulfilled from the catalog then the Supplier Agent replies 
	   with a PROPOSE message specifying the price. Otherwise a REFUSE message is
	   sent back.
	 */
	private class OfferRequestsServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// CFP Message received. Process it
				String title = msg.getContent();
				ACLMessage reply = msg.createReply();
				
				ItemList order = new ItemList(title, false);
				double price = -1.0;
				if (_catalog.contains(order)) {
					ItemList costedOrder = _catalog.getCostedOrder(order);
					price = costedOrder.getPriceInDollars();
				}
				if (price >= 0.0) {
					// The order can be fulfilled from catalog. Reply with the price
					reply.setPerformative(ACLMessage.PROPOSE);
					reply.setContent(String.valueOf(price));
				}
				else {
					// The order can NOT be fulfilled from the catalog.
					reply.setPerformative(ACLMessage.REFUSE);
					reply.setContent("not-available");
				}
				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	}  // End of inner class OfferRequestsServer

	/**
	   Inner class PurchaseOrdersServer.
	   This is the behaviour used by the Supplier Agent to complete
	   orders from Buyer agents.
	   The Supplier Agent subtracts the order from its catalogue 
	   and replies with an INFORM message to notify the Buyer that the
	   purchase has been sucesfully completed.
	 */
	private class PurchaseOrdersServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// ACCEPT_PROPOSAL Message received. Process it
				String title = msg.getContent();
				ACLMessage reply = msg.createReply();

				ItemList order = new ItemList(title, false);
				double price = -1.0;
				if (_catalog.contains(order)) {  
					// Requested order is available in catalog
					_catalog.subtract(order);   // Remove order from catalog
		
					reply.setPerformative(ACLMessage.INFORM); // Tell buyer
					System.out.println(getAID().getName() + ") '" + title + "' sold to agent " 
					  + msg.getSender().getName()
					  + " leaving $" + _catalog.getPriceInDollars() + " worth of material in catalog. Contents '" 
					  + _catalog.getAsString() + "'");
				}	else {
					// Requested order is NOT available in catalog
					reply.setPerformative(ACLMessage.FAILURE);
					reply.setContent("not-available");
				}
				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	}  
}
