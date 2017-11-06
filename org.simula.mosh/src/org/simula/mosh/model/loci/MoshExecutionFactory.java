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


package org.simula.mosh.model.loci;

import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.Object_;
import org.eclipse.papyrus.moka.fuml.Semantics.CommonBehaviors.BasicBehaviors.Execution;
import org.eclipse.papyrus.moka.fuml.Semantics.Loci.LociL1.SemanticVisitor;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.Loci.SM_ExecutionFactory;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.StateMachines.FinalStateActivation;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.BroadcastSignalAction;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.FinalState;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.OpaqueBehavior;
import org.eclipse.uml2.uml.State;
import org.eclipse.uml2.uml.StateMachine;
import org.eclipse.uml2.uml.Transition;
import org.simula.mosh.model.Semantics.MoshBroadcastSignalActionActivation;
import org.simula.mosh.model.Semantics.MoshExternalTransitionActivation;
import org.simula.mosh.model.Semantics.MoshInternalTransitionActivation;
import org.simula.mosh.model.Semantics.MoshLocalTransitionActivation;
import org.simula.mosh.model.Semantics.MoshStateActivation;
import org.simula.mosh.model.Semantics.MoshStateMachineExecution;
import org.simula.mosh.model.actions.SUTInputActionActivation;
import org.simula.mosh.model.actions.SUTOutputActionActivation;
import org.simula.mosh.model.actions.ShellActionActivation;
import org.simula.mosh.model.actions.StopShellActionActivation;
import org.simula.mosh.model.actions.UpdateFeatureActionActivation;

public class MoshExecutionFactory extends SM_ExecutionFactory {
	
	@Override
	public Execution createExecution(Behavior behavior, Object_ context) {
		// Create an execution object for a given behavior.
		// The execution will take place at the locus of the factory in the
		// given context.
		// If the context is empty, the execution is assumed to provide its own
		// context.
		Execution execution;
		if (behavior instanceof OpaqueBehavior) {
			execution = this.instantiateOpaqueBehaviorExecution((OpaqueBehavior) behavior);
		} else {
			execution = (Execution) this.instantiateVisitor(behavior);
			execution.types.add(behavior);
			execution.createFeatureValues();
		}
		this.locus.add(execution);
		if (context == null) {
			execution.context = execution;
		} else {
			execution.context = context;
		}
		
		
		
		return execution;
	}
	
	@Override
	public SemanticVisitor instantiateVisitor(Element element) {
		
		if(element instanceof StateMachine){
			return new MoshStateMachineExecution();
		}
		else{
			SemanticVisitor visitor = null ;
			if (element instanceof Transition) {
				Transition transition = (Transition) element;
				switch(transition.getKind()){
					case EXTERNAL_LITERAL: visitor = new MoshExternalTransitionActivation(); break;
					case INTERNAL_LITERAL: visitor = new MoshInternalTransitionActivation(); break;
					case LOCAL_LITERAL: visitor = new MoshLocalTransitionActivation(); break;
				}
			}
			else if (element instanceof State) {
				if(element instanceof FinalState){
					visitor = new FinalStateActivation();
				}else{
					visitor = new MoshStateActivation() ; 
					
				}
			}
			else if (element instanceof OpaqueAction){
				if(((OpaqueAction) element).getName().compareToIgnoreCase("Shell") == 0){
					return new ShellActionActivation();
				}
				else if(((OpaqueAction) element).getName().compareToIgnoreCase("SUTInput") == 0){
					return new SUTInputActionActivation();
				}
				else if(((OpaqueAction) element).getName().compareToIgnoreCase("SUTOutput") == 0){
					return new SUTOutputActionActivation();
				}
				else if(((OpaqueAction) element).getName().compareToIgnoreCase("UpdateFeature") == 0){
					return new UpdateFeatureActionActivation();
				}
				else if(((OpaqueAction) element).getName().compareToIgnoreCase("Stop") == 0){
					return new StopShellActionActivation();
				}
			}
			else if (element instanceof BroadcastSignalAction){
				return new MoshBroadcastSignalActionActivation();
			}
			
			
			
			if(visitor != null){
				return visitor;
			}
			else{
				return super.instantiateVisitor(element);
			}
		}
		
	}

}