/*****************************************************************************
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* Simula Research Lab, Norway 
*
*****************************************************************************/


package org.simula.mosh.test.reinforcement.qlearning;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.StateMachines.StateActivation;
import org.simula.mosh.model.Semantics.MoshStateActivation;

public class MDPState {
	
	public Set<StateActivation> substates;
	
	public List<MDPTransition> incomingTransitions;
	
	public List<MDPTransition> outgoingTransitions;
	
	public int id;
	
	public MDPState shadowState = null;
	
	public MDPState(Set<StateActivation> states, int id) {
		substates = new HashSet<StateActivation>();
		substates.addAll(states);
		
		this.id = id;
		
		incomingTransitions = new ArrayList<MDPTransition>();
		outgoingTransitions = new ArrayList<MDPTransition>();
	}
	
	public void addOutgoingTransition(MDPTransition transition){
		
		if(transition.transitinoActivation == null){
			assert(outgoingTransitions.size() == 1);
			for(MDPTransition outgoing : outgoingTransitions){
				if(outgoing.transitinoActivation == null){
					return;
				}
			}
		}
		else{
			for(MDPTransition outgoing : outgoingTransitions){
				if(transition.transitinoActivation.equals(outgoing.transitinoActivation)){
					return;
				}
			}
		}

		outgoingTransitions.add(transition);
	}
	
	public void addIncomingTransition(MDPTransition transition){
		if(transition.transitinoActivation == null){
			assert(incomingTransitions.size() == 1);
			for(MDPTransition incoming : incomingTransitions){
				if(incoming.transitinoActivation == null){
					return;
				}
			}
		}
		else{
			for(MDPTransition incoming : incomingTransitions){
				if(transition.transitinoActivation.equals(incoming.transitinoActivation)){
					return;
				}
			}
		}
		
		
		incomingTransitions.add(transition);
	}
	
	private boolean isFinal(){
		for(StateActivation substate : substates){
			String t = substate.getNode().getName();
			if(substate.getNode().getName().equals("Land")){
				return true;
			}
		}
		return false;
	}
	
	public double getQ(boolean useShadow){
		
		if(useShadow){
			return shadowState.getQ(false);
		}
		
		if(isFinal()){
			return 0;
		}
		
		if(outgoingTransitions.isEmpty()){
			return QLearningTester.initialReward; // not covered 
		}
		
		double maxQ = 0;
		for(MDPTransition outgoingTransition : outgoingTransitions){
			double q = outgoingTransition.getQ();
			if(q > maxQ){
				maxQ = q;
			}
		}
		return maxQ;
	}
	
	public boolean equals(Set<StateActivation> activeSubstates){
		
		if(this.substates.size() != activeSubstates.size()){
			return false;
		}
		
		if(!this.substates.containsAll(activeSubstates)){
			return false;
		}
		else{
			return true;
		}
		
	}
	
	public boolean isCovered(){
		
		if(shadowState != null){
			return true;
		}
		
		for(MDPTransition transition : outgoingTransitions){
			if(transition.isCovered()){
				return true;
			}
		}
		return false;
	}
	
	public int getOutgoingTransitionCoverage(){
		
		int covered = 0;
		for(MDPTransition transition : outgoingTransitions){
			if(transition.isCovered()){
				covered++;
			}
		}
		return covered;
	}
	
	public String toString(){
		
		StringBuffer res = new StringBuffer();
		res.append("S");
		res.append(id);
		res.append(" (");
		for(StateActivation substate : substates){
			res.append(substate.getNode().getName());
			res.append(", ");
		}
		res.append(") ");
		
		if(shadowState != null){
			res.append("\n@ ");
			res.append(shadowState.toString());
		}
		
		return res.toString();
	}
	
}