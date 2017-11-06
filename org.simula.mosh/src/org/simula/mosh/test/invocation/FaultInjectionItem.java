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


package org.simula.mosh.test.invocation;

import java.util.List;

import org.eclipse.papyrus.moka.fuml.Semantics.CommonBehaviors.BasicBehaviors.ParameterValue;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.Operation;
import org.simula.mosh.model.loci.ActiveObjectActivation;

public class FaultInjectionItem {
	
	private ActiveObjectActivation objectActivation;
	private Operation operation;
	private List<ParameterValue> parameters;
	
	private int flag = 2;
	
	public FaultInjectionItem(ActiveObjectActivation objectActivation, Operation operation, List<ParameterValue> parameters){
		this.objectActivation = objectActivation;
		this.operation = operation;
		this.parameters = parameters;
	}
	
	public void trigger(){
		objectActivation.callOperation(operation, parameters);
	}
	
	public boolean canTrigger(){
		flag--;
		if(flag < 0){
			return true;
		}
		return false;
	}
	
}