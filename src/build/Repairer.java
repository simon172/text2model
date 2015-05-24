package build;

import java.util.ArrayList;

import nodes.Cluster;
import nodes.ProcessEdge;
import nodes.ProcessNode;
import models.EPCModel;
import epc.Connector;
import epc.ConnectorAND;
import epc.ConnectorOR;
import epc.ConnectorXOR;
import epc.Event;
import epc.Function;
import epc.Organisation;
import epc.SequenceFlow;

public class Repairer {
	
	private ArrayList<Organisation> orgs = new ArrayList<Organisation>();
	private ArrayList<Function> functions = new ArrayList<Function>();
	private ArrayList<Event> events = new ArrayList<Event>();
	private ArrayList<Connector> connBuffer = new ArrayList<Connector>();
	private ArrayList<SequenceFlow> flows = new ArrayList<SequenceFlow>();
	private ArrayList<ConnectorAND> andSplits = new ArrayList<ConnectorAND>();
	private ArrayList<ConnectorAND> andJoins = new ArrayList<ConnectorAND>();
	private ArrayList<ConnectorOR> orSplits = new ArrayList<ConnectorOR>();
	private ArrayList<ConnectorOR> orJoins = new ArrayList<ConnectorOR>();
	private ArrayList<ConnectorXOR> xorSplits = new ArrayList<ConnectorXOR>();
	private ArrayList<ConnectorXOR> xorJoins = new ArrayList<ConnectorXOR>();
	
	private EPCModel model = null;
	
	public Repairer(EPCModel epc){
		model=epc;
	}
	
	public void repairModel(){
		this.getIncomingOutgoingFlows();
		this.extractElements();
		this.checkConnectorIO();
		this.specifyConnectors();
		this.connectorsCannotDecide();
		this.checkEvents();
		this.checkFunctions();
	}
	
	private void getIncomingOutgoingFlows(){
		for (ProcessEdge a : new ArrayList<ProcessEdge>(model.getFlows())){
			if (a instanceof SequenceFlow){
				SequenceFlow f = (SequenceFlow) a;
				a.getSource().setOutgoing(f);
				a.getTarget().setIncoming(f);
			}
		}
	}
	
	private void extractElements(){
		for (Cluster c : new ArrayList<Cluster>(model.getClusters())){
    		if (c instanceof Organisation){
    			Organisation org = (Organisation) c;
    			orgs.add(org);
    		}
    	}
		for (ProcessNode a : new ArrayList<ProcessNode>(model.getFlowObjects())){
    		if (a instanceof Function){
    			Function f = (Function) a;
    			functions.add(f);
    		}
    		if (a instanceof Connector){
    			Connector c = (Connector) a;
    			connBuffer.add(c);
    		}
    	}
		events=model.getEvents();
		for (ProcessEdge a : new ArrayList<ProcessEdge>(model.getFlows())){
    		if (a instanceof SequenceFlow){
    			SequenceFlow f = (SequenceFlow) a;
    			flows.add(f);
    		}
    	}
	}
	
	private void checkConnectorIO(){
		ArrayList<Connector> newCon = new ArrayList<Connector>();
		for (Connector con : connBuffer){
			if (con.getIncoming().size()>1 && con.getOutgoing().size()>1){
				Connector first = Connector.class.cast(con);
				Connector sec = Connector.class.cast(con);
				SequenceFlow f = new SequenceFlow(first,sec);
				first.overrideIncoming(con.getIncoming());
				first.setOutgoing(f);
				sec.setIncoming(f);
				sec.overrideOutgoing(con.getOutgoing());
				flows.add(f);
				newCon.add(first);
				newCon.add(sec);
				for (SequenceFlow i:con.getIncoming()){
					i.setTarget(first);
				}
				for (SequenceFlow o:con.getOutgoing()){
					o.setSource(sec);
				}
			} else {
				newCon.add(con);
			}
		}
		connBuffer=newCon;
	}
	
	private void specifyConnectors(){
		for (Connector con:connBuffer){
			if (con instanceof ConnectorAND){
				ConnectorAND c = (ConnectorAND) con;
				if (con.getIncoming().size()>1){
					andJoins.add(c);
				} else {
					andSplits.add(c);
				}
			} else {
				if (con instanceof ConnectorOR){
					ConnectorOR c = (ConnectorOR) con;
					if (con.getIncoming().size()>1){
						orJoins.add(c);
					} else {
						orSplits.add(c);
					}
				} else {
					ConnectorXOR c = (ConnectorXOR) con;
					if (con.getIncoming().size()>1){
						xorJoins.add(c);
					} else {
						xorSplits.add(c);
					}
				}
			}
		}
	}
	
	private void connectorsCannotDecide(){
		ArrayList<Connector> buffer = new ArrayList<Connector>();
		buffer.addAll(orSplits);
		buffer.addAll(xorSplits);
		for (Connector c:buffer){
			boolean allEvents=true;
			boolean allFunctions=true;
			for (SequenceFlow f:c.getOutgoing()){
				if (f.getTarget() instanceof Event){
					allFunctions=false;
				}
				if (f.getTarget() instanceof Function){
					allEvents=false;
				}
			}
			if (allEvents){
				Function func = new Function("make decision");
				c.getIncoming().get(0).setTarget(func);
				SequenceFlow sf = new SequenceFlow(func,c);
				functions.add(func);
				flows.add(sf);
			} else {
				Event toMove = (Event) c.getIncoming().get(0).getSource();
				if (allFunctions){
					ArrayList<SequenceFlow> flowBuffer = new ArrayList<SequenceFlow>();
					for (SequenceFlow sf:c.getOutgoing()){
						Event e = (Event) toMove.clone();
						e.setId("" + e.hashCode());
						sf.setSource(e);
						SequenceFlow toAdd = new SequenceFlow(c,e);
						events.add(e);
						flows.add(toAdd);
						flowBuffer.add(toAdd);
					}
					c.overrideOutgoing(flowBuffer);
					c.getIncoming().get(0).setSource(toMove.getIncoming().getSource());
					flows.remove(toMove.getIncoming());
					events.remove(toMove);
				} else {
					ArrayList<SequenceFlow> flowBuffer = new ArrayList<SequenceFlow>();
					for (SequenceFlow sf:c.getOutgoing()){
						if (sf.getTarget() instanceof Function){
							Event e = (Event) toMove.clone();
							e.setId("" + e.hashCode());
							sf.setSource(e);
							SequenceFlow toAdd = new SequenceFlow(c,e);
							events.add(e);
							flows.add(toAdd);
							flowBuffer.add(toAdd);
						} else {
							flowBuffer.add(sf);
						}
						c.overrideOutgoing(flowBuffer);
						c.getIncoming().get(0).setSource(toMove.getIncoming().getSource());
						flows.remove(toMove.getIncoming());
						events.remove(toMove);
					}
				}
			}
		}
	}
	
	private void checkFunctions(){
		for (Function f:functions){
			if (f.getIncoming()==null){
				Event e = new Event("start");
				SequenceFlow sf = new SequenceFlow(e,f);
				events.add(e);
				flows.add(sf);
			}
			if (f.getOutgoing()==null){
				Event e = new Event("end");
				SequenceFlow sf = new SequenceFlow(f,e);
				events.add(e);
				flows.add(sf);
			} else {
				if (f.getOutgoing().getTarget() instanceof Function){
					Event e = new Event("to label");
					f.getOutgoing().setSource(e);
					SequenceFlow sf = new SequenceFlow(f,e);
					events.add(e);
					flows.add(sf);
				} else {
					if (f.getOutgoing().getTarget() instanceof Connector){
						Connector c = (Connector) f.getOutgoing().getTarget();
						for (SequenceFlow sf:c.getOutgoing()){
							if (sf.getTarget() instanceof Function){
								Event e = new Event("to label");
								sf.setSource(e);
								SequenceFlow seq = new SequenceFlow(c,e);
								events.add(e);
								flows.add(seq);
							}
						}
					}
				}
			}
		}
	}
	
	private void checkEvents(){
		for (Event e:events){
			if (e.getOutgoing() != null){
				if (e.getOutgoing().getTarget() instanceof Event){
					e.getIncoming().setTarget(e.getOutgoing().getTarget());
					flows.remove(e.getOutgoing());
					events.remove(e);
				} else {
					if (e.getOutgoing().getTarget() instanceof Connector){
						Connector c = (Connector) e.getOutgoing().getTarget();
						e.getIncoming().setTarget(c);
						c.getIncoming().remove(e.getOutgoing());
						c.setIncoming(e.getIncoming());
						flows.remove(e.getOutgoing());
						events.remove(e);
					}
				}
			}
		}
	}

}
