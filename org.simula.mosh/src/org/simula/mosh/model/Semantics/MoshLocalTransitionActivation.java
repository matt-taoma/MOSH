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

import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.papyrus.moka.fuml.FUMLExecutionEngine;
import org.eclipse.papyrus.moka.fuml.Semantics.CommonBehaviors.Communications.EventOccurrence;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.StateMachines.FinalStateActivation;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.StateMachines.LocalTransitionActivation;
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
import org.simula.constraint.ConstraintInstance;
import org.simula.constraint.ConstraintInstanceFactory;
import org.simula.mosh.debug.TestControlDelegate;
import org.simula.mosh.model.loci.ActiveObjectActivation;
import org.simula.mosh.test.log.TestLogger;

public class MoshLocalTransitionActivation extends LocalTransitionActivation {
	
	@Override
	public boolean evaluateGuard(EventOccurrence eventOccurrence){
		
		Transition transition = (Transition) this.node;
		Constraint guard = transition.getGuard();
		if(guard != null){
			ConstraintInstance guardEvaluation = ConstraintInstanceFactory.instance.createConstraintInstance(guard, eventOccurrence);
			return guardEvaluation.evaluate();
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
		
		if(configuration.getVertexActivation().equals(stateActivation)){
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
		// Entering the target of local transition consists in checking if the target can be entered. If
		// this is the case then only when the target is not also the containing state it is entered 
		if(this.vertexTargetActivation.isEnterable(this, false)){	
			if(this.vertexTargetActivation != this.getContainingState()){
				wait4stateUpdate();
				TestLogger.log(this.vertexTargetActivation, false);
				this.vertexTargetActivation.enter(this, eventOccurrence, this.getLeastCommonAncestor());
			}
		}
		else{
			System.out.println("local state not enter");
		}
	}
	
	private void wait4stateUpdate(){
		
		if(vertexTargetActivation instanceof FinalStateActivation){
			return;
		}
		
		if(vertexTargetActivation instanceof StateActivation){
			
			ConstraintInstance invariant = ((MoshStateActivation)vertexTargetActivation).stateInvariant;
			if(invariant == null){
				return;
			}
			
			while(true){
				if(invariant.evaluate()){ 
					ActiveObjectActivation activation = (ActiveObjectActivation)((ActiveObject)this.getExecutionContext()).objectActivation;
					activation.setTraversing(false);
					break; // target state reached
				}
				else{
					ActiveObjectActivation activation = (ActiveObjectActivation)((ActiveObject)this.getExecutionContext()).objectActivation;
					activation.setTraversing(true);
					synchronized(activation){
						try {
							activation.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					
				}
			}
			
		}
	}
	
}