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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.StateMachines.InitialPseudostateActivation;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.StateMachines.RegionActivation;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.StateMachines.StateActivation;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.StateMachines.StateMachineExecution;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.StateMachines.TransitionActivation;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.StateMachines.VertexActivation;
import org.eclipse.uml2.uml.NamedElement;
import org.simula.mosh.model.Semantics.ActiveObject;
import org.simula.mosh.model.Semantics.MoshExternalTransitionActivation;
import org.simula.mosh.model.loci.ActiveObjectActivation;
import org.simula.mosh.test.log.TestLogger;
import org.simula.mosh.test.utils.TestInputGenerator;

public class MarkovDecisionProcess {

	private MDPState root = null;

	private MDPState cursor = null;

	private int stateNum = 1;

	private List<MDPState> states = new ArrayList<MDPState>();

	private boolean allTransitionsCovered = false;

	public void initRoot(Set<StateActivation> substates) {
		if (root == null) {
			root = new MDPState(substates, stateNum++);
			states.add(root);
		}
		cursor = root;
	}

	public void updateAllQvalues() {

		// reduce the effect of loop
		for (int i = 0; i < 20; i++) {
			for (MDPState state : states) {
				for (MDPTransition transition : state.outgoingTransitions) {
					transition.updateQ();
				}
			}
		}

	}

	public void correctCurrentState(Set<StateActivation> activeSubstates, MDPTransition lastTransition) {

		if (!cursor.equals(activeSubstates)) {

			MDPState nextState = getState(activeSubstates);

			if (cursor.shadowState == null) {
				cursor.shadowState = nextState;
			}
			else if (!cursor.shadowState.equals(nextState)) {

				TestLogger.logError("shadow state error (" + cursor.toString() + ", " + nextState + ")");
				cursor.shadowState = nextState;
			}

			lastTransition.useShadow = true;

			cursor = nextState;
		}

	}

	public MDPState getCurrentState() {
		return cursor;
	}

	public void invoke(MDPTransition transition) {
		transition.invoke();
		cursor = transition.target;
	}

	public void updateNextState(TransitionActivation transitionActivation) {

		for (MDPTransition transition : cursor.outgoingTransitions) {
			if (transitionActivation.equals(transition.transitinoActivation)) {
				return;
			}
		}

		Set<StateActivation> substates = new HashSet<StateActivation>();
		substates.addAll(cursor.substates);

		StateActivation sourceState = (StateActivation) transitionActivation.getSourceActivation();
		StateActivation targetState = (StateActivation) transitionActivation.getTargetActivation();

		// if source state is contained in a composite state
		VertexActivation sourceParantState = sourceState.getParentVertexActivation();
		if (targetState != null && sourceParantState != null && sourceParantState instanceof StateActivation) {
			StateActivation parantState = (StateActivation) sourceParantState;
			if (parantState.regionActivation.indexOf(targetState.getParent()) < 0) {
				// target state is not contained in source composit state
				Set<StateActivation> tmp = new HashSet<StateActivation>();
				for (StateActivation state : substates) {
					if (parantState.regionActivation.indexOf(state.getParent()) < 0) {
						tmp.add(state);
					}
				}
				substates = tmp;
			}
		}

		// if source state is a composite state
		if (!sourceState.regionActivation.isEmpty()) {
			Set<StateActivation> tmp = new HashSet<StateActivation>();
			for (StateActivation state : substates) {
				if (sourceState.regionActivation.indexOf(state.getParent()) < 0) {
					tmp.add(state);
				}
			}
			substates = tmp;
		}

		// is source state is normal
		substates.remove(sourceState);

		// if target state is a composite state
		if (targetState.regionActivation.size() > 0) {
			for (RegionActivation regionActivation : targetState.regionActivation) {
				substates.add(getFirstState(regionActivation));
			}
		}
		else {
			substates.add(targetState);
		}

		MDPState nextState = getState(substates);

		MDPTransition newTransition = new MDPTransition((MoshExternalTransitionActivation) transitionActivation, cursor,
				nextState);

		cursor.addOutgoingTransition(newTransition);
		nextState.addIncomingTransition(newTransition);

	}

	private StateActivation getFirstState(RegionActivation regionActivation) {
		for (VertexActivation vertexActivation : regionActivation.getVertexActivations()) {
			if (vertexActivation instanceof InitialPseudostateActivation) {
				return (StateActivation) ((InitialPseudostateActivation) vertexActivation).getOutgoingTransitions()
						.get(0).getTargetActivation();
			}
		}

		return null;
	}

	private MDPState getState(Set<StateActivation> substates) {

		for (MDPState state : states) {
			if (state.equals(substates)) {
				return state;
			}
		}

		MDPState state = new MDPState(substates, stateNum++);
		states.add(state);
		return state;
	}

	public void updateReferences(Map<String, ActiveObjectActivation> activeObjectActivations) {

		Map<String, Map<NamedElement, StateActivation>> objectStates = new HashMap<String, Map<NamedElement, StateActivation>>();
		Map<String, Map<NamedElement, TransitionActivation>> objectTransitions = new HashMap<String, Map<NamedElement, TransitionActivation>>();

		for (String name : activeObjectActivations.keySet()) {
			Map<NamedElement, StateActivation> stateActivations = getAllStateActivations(
					activeObjectActivations.get(name));
			objectStates.put(name, stateActivations);
			Map<NamedElement, TransitionActivation> transitionActivations = getAllTransitionActivations(
					activeObjectActivations.get(name));
			objectTransitions.put(name, transitionActivations);
		}

		for (MDPState state : states) {
			update4NewTest(state, objectStates, objectTransitions);
		}

	}

	private Map<NamedElement, StateActivation> getAllStateActivations(ActiveObjectActivation activeObjectActivation) {

		StateMachineExecution execution = (StateMachineExecution) (activeObjectActivation.classifierBehaviorInvocations
				.get(0).execution);

		Map<NamedElement, StateActivation> res = new HashMap<NamedElement, StateActivation>();

		for (RegionActivation regionActivation : execution.getRegionActivation()) {
			for (VertexActivation vertextActivation : regionActivation.getVertexActivations()) {
				if (vertextActivation instanceof StateActivation) {
					getAllStateActivations((StateActivation) vertextActivation, res);
				}
			}
		}

		return res;
	}

	private void getAllStateActivations(StateActivation stateActivation,
			Map<NamedElement, StateActivation> stateActivations) {

		stateActivations.put(stateActivation.getNode(), stateActivation);

		for (RegionActivation regionActivation : stateActivation.getRegionActivation()) {
			for (VertexActivation vertextActivation : regionActivation.getVertexActivations()) {
				if (vertextActivation instanceof StateActivation) {
					getAllStateActivations((StateActivation) vertextActivation, stateActivations);
				}
			}
		}
	}

	private Map<NamedElement, TransitionActivation> getAllTransitionActivations(
			ActiveObjectActivation activeObjectActivation) {

		StateMachineExecution execution = (StateMachineExecution) (activeObjectActivation.classifierBehaviorInvocations
				.get(0).execution);

		Map<NamedElement, TransitionActivation> res = new HashMap<NamedElement, TransitionActivation>();

		for (RegionActivation regionActivation : execution.getRegionActivation()) {
			getAllTransitionActivations(regionActivation, res);
		}

		return res;
	}

	private void getAllTransitionActivations(RegionActivation regionActivation,
			Map<NamedElement, TransitionActivation> transitionActivations) {

		for (TransitionActivation transitionActivation : regionActivation.getTransitionActivations()) {
			transitionActivations.put(transitionActivation.getNode(), transitionActivation);
		}

		for (VertexActivation vertextActivation : regionActivation.getVertexActivations()) {
			if (vertextActivation instanceof StateActivation) {
				StateActivation stateActivation = (StateActivation) vertextActivation;
				for (RegionActivation childRegionActivation : stateActivation.getRegionActivation()) {
					getAllTransitionActivations(childRegionActivation, transitionActivations);
				}
			}
		}
	}

	private void update4NewTest(MDPState state, Map<String, Map<NamedElement, StateActivation>> objectStates,
			Map<String, Map<NamedElement, TransitionActivation>> objectTransitions) {

		Set<StateActivation> newSubstates = new HashSet<StateActivation>();
		for (StateActivation substate : state.substates) {
			String name = ((ActiveObject) substate.getExecutionContext()).getName();
			Map<NamedElement, StateActivation> stateActivations = objectStates.get(name);
			StateActivation stateActivation = stateActivations.get(substate.getNode());
			if (stateActivation == null) {
				System.err.println(substate.getNode().getName() + " is missing");
			}
			newSubstates.add(stateActivation);
		}
		if (newSubstates.size() != state.substates.size()) {
			System.err.println("mdp states size not equal");
		}
		state.substates = newSubstates;

		for (MDPTransition transition : state.incomingTransitions) {
			if (transition.transitinoActivation == null) {
				continue;
			}

			String name = ((ActiveObject) transition.transitinoActivation.getExecutionContext()).getName();
			Map<NamedElement, TransitionActivation> transitionActivations = objectTransitions.get(name);
			MoshExternalTransitionActivation transitinoActivation = (MoshExternalTransitionActivation) transitionActivations
					.get(transition.transitinoActivation.getNode());
			transition.updateInvocation4NewTest(transitinoActivation);
		}

		for (MDPTransition transition : state.outgoingTransitions) {
			if (transition.transitinoActivation == null) {
				continue;
			}
			String name = ((ActiveObject) transition.transitinoActivation.getExecutionContext()).getName();
			Map<NamedElement, TransitionActivation> transitionActivations = objectTransitions.get(name);
			MoshExternalTransitionActivation transitinoActivation = (MoshExternalTransitionActivation) transitionActivations
					.get(transition.transitinoActivation.getNode());
			transition.updateInvocation4NewTest(transitinoActivation);
		}

	}

	// public void updateTransition(MDPState sourceState, MDPState targetState,
	// TransitionActivation transitionActivation){
	//
	// boolean found = false;
	// for(MDPTransition transition : sourceState.outgoingTransitions){
	// if(transition.target.equals(targetState)){
	// found = true;
	// break;
	// }
	// }
	// if(!found){
	// MDPTransition newTransition = new
	// MDPTransition((MoshExternalTransitionActivation)transitionActivation,
	// sourceState, targetState);
	// sourceState.outgoingTransitions.add(newTransition);
	// targetState.incomingTransitions.add(newTransition);
	// }
	//
	// }

	public String toString() {

		StringBuffer res = new StringBuffer();
		for (MDPState state : states) {

			res.append(state);

			if (state.shadowState != null) {
				res.append("   ***   \n");
			}
			else {
				res.append("   ---   \n");
			}

			for (MDPTransition transition : state.outgoingTransitions) {
				res.append("   ");
				res.append(transition);
				res.append("; \n");
			}
			res.append("\n");
		}

		return res.toString();

	}

	public String getStateCoverage() {

		int covered = 0;
		for (MDPState state : states) {
			if (state.isCovered()) {
				covered++;
			}
		}
		return covered + "/" + states.size();
	}

	public boolean allTransitionCovered() {
		return allTransitionsCovered;
	}

	private int lastCovered = 0;
	private int noMoreCovered = 0;

	public String getTransitionCoverage() {

		int covered = 0;
		int total = 0;
		for (MDPState state : states) {
			if (state.shadowState != null) {
				covered++;
				total++;
			}
			covered += state.getOutgoingTransitionCoverage();
			total += state.outgoingTransitions.size();
		}

		if (!allTransitionsCovered) {
			if (lastCovered == covered) {
				noMoreCovered++;

				if (noMoreCovered > 3) {
					allTransitionsCovered = true;
				}
			}
			else {
				noMoreCovered = 0;
				lastCovered = covered;
			}
		}

		return covered + "/" + total;
	}

}