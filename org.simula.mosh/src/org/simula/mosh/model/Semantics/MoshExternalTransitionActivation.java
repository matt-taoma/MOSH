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


package org.simula.mosh.model.Semantics;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.papyrus.moka.fuml.FUMLExecutionEngine;
import org.eclipse.papyrus.moka.fuml.Semantics.CommonBehaviors.Communications.EventOccurrence;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.CommonBehavior.CallEventOccurrence;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.StateMachines.ExternalTransitionActivation;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.StateMachines.FinalStateActivation;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.StateMachines.RegionActivation;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.StateMachines.StateActivation;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.StateMachines.StateConfiguration;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.StateMachines.StateMachineExecution;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.StateMachines.VertexActivation;
import org.eclipse.uml2.uml.ChangeEvent;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Region;
import org.eclipse.uml2.uml.State;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.Transition;
import org.eclipse.uml2.uml.Vertex;
import org.simula.constraint.ConstraintInstance;
import org.simula.constraint.ConstraintInstanceFactory;
import org.simula.mosh.debug.TestControlDelegate;
import org.simula.mosh.model.loci.ActiveObjectActivation;
import org.simula.mosh.test.log.TestLogger;

public class MoshExternalTransitionActivation extends ExternalTransitionActivation {
	
	
	@Override
	public boolean evaluateGuard(EventOccurrence eventOccurrence){
		
		Transition transition = (Transition) this.node;
		Constraint guard = transition.getGuard();
		if(guard != null){
			ConstraintInstance guardEvaluation = ConstraintInstanceFactory.instance.createConstraintInstance(guard, eventOccurrence);
			boolean res = guardEvaluation.evaluate();
			res = guardEvaluation.evaluate();
			return res;
		}
		else{
			return true;
		}
		
	}
	
	private void updateConstraintList(MoshStateActivation stateActivation){
		
		StateMachineExecution execution = (StateMachineExecution)this.getStateMachineExecution();
		StateConfiguration root = execution.getConfiguration().getRoot();
		
		StateConfiguration configuration = getStateConfiguration(root, stateActivation);
		if(configuration != null){
			removeConstraint(configuration);
		}
		else{
			System.err.println(stateActivation + "is not in state configuration");
		}
	}
	
	private void removeConstraint(StateConfiguration configuration){
		VertexActivation activation = configuration.getVertexActivation();
		if(activation instanceof MoshStateActivation && ((MoshStateActivation)activation).stateInvariant != null){
			((TestControlDelegate) FUMLExecutionEngine.eInstance.getControlDelegate()).notifyStateExisted((MoshStateActivation)activation);
		}
		
		for(StateConfiguration child : configuration.getChildren()){
			removeConstraint(child);
		}
	}
	
	private StateConfiguration getStateConfiguration(StateConfiguration configuration, MoshStateActivation stateActivation){
		
		if(stateActivation.equals(configuration.getVertexActivation())){
			return configuration;
		}
		
		for(StateConfiguration child : configuration.getChildren()){
			StateConfiguration res = getStateConfiguration(child, stateActivation);
			if(res != null){
				return res;
			}
		}
		
		return null;
	}
	
	@Override
	protected void exitSource(EventOccurrence eventOccurrence) {
		
		if(vertexSourceActivation instanceof MoshStateActivation){
			updateConstraintList((MoshStateActivation)vertexSourceActivation);
		}
		
		super.exitSource(eventOccurrence);
		TestLogger.log(this.vertexSourceActivation, true);
		
		NamedElement node = this.vertexSourceActivation.getNode();
		if(!(node instanceof State)){
			return;
		}
		State state = (State)node;
		Stereotype adaptatingState = state.getAppliedStereotype("healing::AdaptatingState");
		if (adaptatingState == null) {
			return ;
		}
		Region region = state.getContainer();
		Stereotype healingBehavior = region.getAppliedStereotype("healing::HealingBehavior");
		if (healingBehavior == null) {
			return;
		}
		EObject fault = (EObject) region.getValue(healingBehavior, "fault");
		List<EObject> faultProperties = fault.eCrossReferences();
		for (EObject property : faultProperties) {
			if (property instanceof ChangeEvent) {
				((TestControlDelegate) FUMLExecutionEngine.eInstance.getControlDelegate()).notifyFaultHandled((ChangeEvent)property);
			}
		}
		
		
		
		
		
	}
	
	@Override
	protected void enterTarget(EventOccurrence eventOccurrence) {
		
		// If the target vertex activation can be entered (i.e., its possible prerequisites
		// are satisfied) then the entering process begins. Note that this process may lead
		// to enter other states based on what is the common ancestor exiting between the
		// the source and the target. Besides the prerequisites imposed by the target vertex
		// activation there are no other constraints to enter the target state
		if(this.vertexTargetActivation.isEnterable(this, false)){
			if(eventOccurrence instanceof CallEventOccurrence){
				wait4stateUpdate();
			}
			
			TestLogger.log(this.vertexTargetActivation, false);
			this.vertexTargetActivation.enter(this, eventOccurrence, this.getLeastCommonAncestor());
		}else{
			if(this.vertexTargetActivation instanceof StateActivation){
				StateActivation targetStateActivation = (StateActivation) this.vertexTargetActivation;
				int i = 0;
				RegionActivation containingRegionActivation = null;
				while(containingRegionActivation == null && i < targetStateActivation.regionActivation.size()){
					RegionActivation currentActivation = targetStateActivation.regionActivation.get(i);
					if(currentActivation.getVertexActivation((Vertex)this.vertexSourceActivation.getNode())!=null){
						containingRegionActivation = currentActivation;
					}
					i++;
				}
				if(containingRegionActivation!=null){
					containingRegionActivation.isCompleted = true;
					if(targetStateActivation.hasCompleted()){
						targetStateActivation.notifyCompletion();
					}
				}
			}
			
		}
	}
	
	private Instant startWaitingTime = null;
	
	private void wait4stateUpdate(){
		
		startWaitingTime = Instant.now();
		
		if(vertexTargetActivation instanceof FinalStateActivation){
			return;
		}
		
		if(vertexTargetActivation instanceof StateActivation){
			
			ConstraintInstance invariant = ((MoshStateActivation)vertexTargetActivation).stateInvariant;
			if(invariant == null){
				return;
			}
			
			int true_times = 0;
			while(true){
				if(invariant.evaluate()){ 
					true_times ++;
					
					if(true_times > 10){
						ActiveObjectActivation activation = (ActiveObjectActivation)((ActiveObject)this.getExecutionContext()).objectActivation;
						activation.setTraversing(false);
						break; // target state reached
					}
					
				}
				else{
					true_times = 0;
				}
				
				ActiveObjectActivation activation = (ActiveObjectActivation)((ActiveObject)this.getExecutionContext()).objectActivation;
				activation.setTraversing(true);
				synchronized(activation){
					try {
						activation.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
					
				Instant now = Instant.now();
				long nanos = Duration.between(startWaitingTime, now).toNanos();
				long ms = nanos / 1000000;
				if(ms > 900000 ){ // has been waiting for 900s
					((TestControlDelegate) FUMLExecutionEngine.eInstance.getControlDelegate()).notifyEnteringTargetStateTimeout((StateActivation)vertexTargetActivation);
				}
			}
			
		}
	}
	
}