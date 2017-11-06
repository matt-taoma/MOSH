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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.StateMachines.StateActivation;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.StateMachines.StateConfiguration;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.StateMachines.StateMachineExecution;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.StateMachines.TransitionActivation;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.StateMachines.TransitionActivation.TransitionMetadata;
import org.eclipse.uml2.uml.CallEvent;
import org.eclipse.uml2.uml.ChangeEvent;
import org.eclipse.uml2.uml.Event;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.Transition;
import org.eclipse.uml2.uml.Trigger;
import org.simula.mosh.model.Semantics.MoshStateActivation;
import org.simula.mosh.model.loci.ActiveObjectActivation;
import org.simula.mosh.test.invocation.Invocation;
import org.simula.mosh.test.log.TestLogger;
import org.simula.mosh.test.reinforcement.ReinforcementTester;
import org.simula.mosh.test.sut.SUTObject;
import org.simula.mosh.test.utils.PropertyUtil;

public class QLearningTester extends ReinforcementTester{
	
	protected MarkovDecisionProcess mdp = new MarkovDecisionProcess();
	
	protected ArrayList<MDPTransition> lastTransitions = null;
	
	public static final double discountFactor = 0.98;
	public static final double initialReward = 1;
	private static final double softmaxFactor = 2;
	
	protected Random random = new Random(1);
	
	@Override
	public void episodeStart(SUTObject sut) {
		
		mdp.updateReferences(sut.getActiveObjectActivations());
		
		lastTransitions = null;
	}
	
	@Override
	public void episodeFinish(){
		
	}
	
	@Override
	public Invocation nextEpoch(SUTObject sut, double reward, boolean stop) {
		
		Map<String, ActiveObjectActivation> activeObjectActivations = sut.getActiveObjectActivations();
		
		if(lastTransitions == null){ // the first time 
			Set<StateActivation> substates = getActiveStates(activeObjectActivations);
			mdp.initRoot(substates);
			
			mdp.updateAllQvalues();
		}
		else{
			// because there are some change events 
			// currentState may not be the true currentState
			Set<StateActivation> substates = getActiveStates(activeObjectActivations);
			substates = getActiveStates(activeObjectActivations);
			mdp.correctCurrentState(substates, lastTransitions.get(lastTransitions.size() - 1));
		}
		
		for (ActiveObjectActivation objectActivation : activeObjectActivations.values()) {

			assert (objectActivation.classifierBehaviorInvocations.size() == 1);
			StateMachineExecution execution = (StateMachineExecution) (objectActivation.classifierBehaviorInvocations
					.get(0).execution);
			updateNextMDPStates(objectActivation, execution.getConfiguration().getRoot());

		}
		
		//System.out.println(mdp);
		
		if(lastTransitions != null && (!lastTransitions.isEmpty())){
			lastTransitions.get(lastTransitions.size() - 1).updateQ(reward);
			
			int index = lastTransitions.size() - 2;
			int end = lastTransitions.size() - 11;
			while(index >= 0 && index >= end){
				lastTransitions.get(index).updateQ();
				index --;
			}
		}
		
		
		return explore(stop);
		
	}
	
	
	protected void updateNextMDPStates(ActiveObjectActivation objectActivation, StateConfiguration configuration) {

		for (StateConfiguration child : configuration.getChildren()) {
			updateNextMDPStates(objectActivation, child);
		}

		if (configuration.getVertexActivation() == null) {
			return;
		}
		
		for (TransitionActivation transitionActivation : configuration.getVertexActivation().getOutgoingTransitions()) {
			Transition transition = (Transition) transitionActivation.getNode();
			List<Trigger> triggers = transition.getTriggers();
			for (Trigger trigger : triggers) {
				Event event = trigger.getEvent();
				if (event instanceof CallEvent) {

//					Operation operation = ((CallEvent) event).getOperation();
//					Constraint guard = transition.getGuard();
					updateNextMDPStates(transitionActivation);

				}
				else if (event instanceof ChangeEvent) {
					Stereotype stereotype = event.getAppliedStereotype("healing::Fault");
					if (stereotype != null) {
//
//						List<Operation> operations = (List<Operation>) event.getValue(stereotype, "injectionOperation");
//						List<Constraint> conditions = (List<Constraint>) event.getValue(stereotype, "condition");
//						Operation operation = operations.get(0);
//						Constraint condition = null;
//						if (!conditions.isEmpty()) {
//							condition = conditions.get(0);
//						}
						updateNextMDPStates(transitionActivation);

					}
				}
			}
		}
	}
	
	
	private void updateNextMDPStates(TransitionActivation transitionActivation) {
		
		Transition transition = (Transition) transitionActivation.getNode();
		Trigger trigger = transition.getTriggers().get(0);
		Event event = trigger.getEvent();
		
		if (event instanceof CallEvent) {
		}
		else if (event instanceof ChangeEvent && event.getAppliedStereotype("healing::Fault") != null) {
		}
		else{
			return;
		}
		
		mdp.updateNextState(transitionActivation);
		
	}
	
	protected void recordTransition(MDPTransition selectedTransition){
		if(lastTransitions == null){
			lastTransitions = new ArrayList<MDPTransition>();
		}
		
		lastTransitions.add(selectedTransition);
	}
	
	protected Invocation explore(boolean want2stop){
		
		if(lastTransitions != null && lastTransitions.size() > 0 &&
				lastTransitions.get(lastTransitions.size() - 1).getOperationName().equals("stop")){
			return null; // already stop
		}
		
		MDPTransition bestTransition = null;
		
		List<MDPTransition> transitions = new ArrayList<MDPTransition>();
		
		for(MDPTransition transition : mdp.getCurrentState().outgoingTransitions){
			
			if(transition.isNullTransition()){
				continue;
			}
			
			if(want2stop){
				if(transition.getOperationName().equals("mode") || 
						transition.getOperationName().equals("stop")){
					bestTransition = transition;
				}
			}
			
			transitions.add(transition);
			
		}
		
		if(bestTransition == null){
			if(!mdp.allTransitionCovered()){
				bestTransition = selectHighestQvalue(transitions);
			}
			else{
				bestTransition = selectAccordingQvalue(transitions);
				if(bestTransition == null){
					
					for(MDPTransition transition : transitions){
						if(transition.getOperationName().equals("mode") || 
								transition.getOperationName().equals("stop")){
							bestTransition = transition;
						}
					}
					
				}
			}
		}
		
		if(bestTransition == null){
			return null;
		}
		
		mdp.invoke(bestTransition);
		
		recordTransition(bestTransition);
		
		
		return bestTransition.getInvocation();
		
	}
	
	private MDPTransition selectHighestQvalue(List<MDPTransition> transitions){
		
		double qvalue = -1;
		MDPTransition best = null;
		for(MDPTransition transition : transitions){
			if(transition.getQ() > qvalue){
				qvalue = transition.getQ();
				best = transition;
			}
		}
		return best;
	}
	
	private double[] getWeights(List<MDPTransition> transitions){
		
		double[] qvalues = new double[transitions.size()];
		for(int i = 0; i < transitions.size(); i ++){
			qvalues[i] = transitions.get(i).getQ() * softmaxFactor;
			if(qvalues[i] > 0) {
				qvalues[i] = Math.pow(Math.E, qvalues[i]); // [1, 7.389]
			}
		}
		
		return qvalues;
		
	}
	
//	private double[] getWeights(List<MDPTransition> transitions){
//		
//		double minQvalue = 1;
//		double[] qvalues = new double[transitions.size()];
//		for(int i = 0; i < transitions.size(); i ++){
//			qvalues[i] = transitions.get(i).getQ();
//			if(minQvalue > qvalues[i] && qvalues[i] > 0){
//				minQvalue = qvalues[i];
//			}
//		}
//		
//		double maxValue = 0;
//		for(int i = 0; i < qvalues.length; i++){
//			qvalues[i] = qvalues[i] - minQvalue;
//			if(qvalues[i] < 0){
//				qvalues[i] = 0;
//			}
//			if(maxValue < qvalues[i]){
//				maxValue = qvalues[i];
//			}
//		}
//		
//		if(maxValue == 0){
//			return null;
//		}
//		
//		double factor = 3 / maxValue;
//		for (int i = 0; i < qvalues.length; i++) {
//			qvalues[i] = qvalues[i] * factor;
//			
//			System.out.println(factor + " " + qvalues[i]);
//			
//			BigDecimal b = new BigDecimal(qvalues[i]);
//			qvalues[i] = b.setScale(5,   BigDecimal.ROUND_HALF_UP).doubleValue();  
//		}
//		
//		return qvalues;
//		
//	}
	
	
	private MDPTransition selectAccordingQvalue(List<MDPTransition> transitions){
		if(transitions.size() == 1){
			return transitions.get(0);
		}
		
		double[] qvalues = getWeights(transitions);
		if(qvalues == null){
			return null;
		}
		
		double sum = 0;
		for(int i = 0; i < transitions.size(); i ++){
			sum += qvalues[i];
		}
		if(sum == 0){
			return transitions.get(random.nextInt(transitions.size()));
		}
		
		for(int i = 0; i < qvalues.length; i ++){
			qvalues[i] = qvalues[i] / sum;
		}
		double accumulative = qvalues[0];
		for(int i = 1; i < qvalues.length; i ++){
			qvalues[i] = qvalues[i] + accumulative;
			accumulative = qvalues[i];
		}
		
		double value =random.nextDouble();
		for(int i = 0; i < qvalues.length; i ++){
			if(value < qvalues[i]){
				return transitions.get(i);
			}
		}
		
		return null;
	}
	
	private MDPTransition getUncoveredTransition(List<MDPTransition> transitions){
		
		List<MDPTransition> uncovered = new ArrayList<MDPTransition>();
		
		for(MDPTransition transition : transitions){
			
			if(transition.isNullTransition()){
				continue;
			}
			
			if(!transition.isCovered()){
				uncovered.add(transition);
			}
		}
		
		if(uncovered.isEmpty()){
			return null;
		}
		else{
			return uncovered.get(random.nextInt(uncovered.size()));
		}
		
	}
	
	protected Set<StateActivation> getActiveStates(Map<String, ActiveObjectActivation> activeObjectActivations){
		
		Set<StateActivation> substates = new HashSet<StateActivation>();
		
		for (ActiveObjectActivation objectActivation : activeObjectActivations.values()) {

			StateMachineExecution execution = (StateMachineExecution) (objectActivation.classifierBehaviorInvocations
					.get(0).execution);
			substates.addAll(getActiveStates(execution.getConfiguration().getRoot()));

		}
		
		return substates;
	}
	
	private List<MoshStateActivation> getActiveStates(StateConfiguration configuration){
		
		List<MoshStateActivation> activeStates = new ArrayList<MoshStateActivation>();
		for (StateConfiguration child : configuration.getChildren()) {
			activeStates.addAll(getActiveStates(child));
		}
		if(activeStates.size() > 0 || configuration.getVertexActivation() == null){
			return activeStates;
		}
		
		MoshStateActivation stateActivation = (MoshStateActivation)configuration.getVertexActivation();
		activeStates.add(stateActivation);
		return activeStates;
		
	}
	
	public MarkovDecisionProcess getMDP(){
		return mdp;
	}
	
	public String toString(){
		
		StringBuffer str = new StringBuffer();
		
		str.append("state: \n");
		
		if(lastTransitions != null){
			for(MDPTransition transition : lastTransitions){
				
				str.append("S" + transition.source.id + ", ");
			}
		}
		str.append("\n");
		str.append(mdp.toString());
		return str.toString();
	}
	
}