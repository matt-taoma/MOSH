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

import org.eclipse.papyrus.moka.fuml.FUMLExecutionEngine;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.Object_;
import org.eclipse.papyrus.moka.fuml.Semantics.CommonBehaviors.Communications.EventOccurrence;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.StateMachines.ConnectionPointActivation;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.StateMachines.RegionActivation;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.StateMachines.StateActivation;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.StateMachines.StateMachineExecution;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.StateMachines.TransitionActivation;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.StateMachines.VertexActivation.StateMetadata;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.Pseudostate;
import org.eclipse.uml2.uml.Region;
import org.eclipse.uml2.uml.State;
import org.simula.constraint.ConstraintInstance;
import org.simula.constraint.ConstraintInstanceFactory;
import org.simula.mosh.debug.TestControlDelegate;

public class MoshStateActivation extends StateActivation {
	
	public ConstraintInstance stateInvariant;
	
	public MoshStateActivation(){
		super();
	}
	
	private void instantiateInvariant(){
		
		Constraint invariant = ((State) this.getNode()).getStateInvariant();
		if(invariant != null){
			stateInvariant = ConstraintInstanceFactory.instance.createConstraintInstance(invariant, this.getExecutionContext());
		}
	}
	
	public void activate(){
		// Instantiate visitors for all vertices owned by this region 
		State state = (State) this.getNode();
		
		instantiateInvariant();
		
		if(state.isComposite()){
			Object_ context = this.getExecutionContext();
			for(Pseudostate connectionPoint : state.getConnectionPoints()){
				ConnectionPointActivation activation = (ConnectionPointActivation)context.locus.factory.instantiateVisitor(connectionPoint);
				activation.setParent(this);
				activation.setNode(connectionPoint);
				this.connectionPointActivation.add(activation);
			}
			for(Region region: state.getRegions()){
				RegionActivation activation = (RegionActivation) context.locus.factory.instantiateVisitor(region);
				activation.setParent(this);
				activation.setNode(region);
				activation.activate();
				this.regionActivation.add(activation);
			}
		}
	}
	
}