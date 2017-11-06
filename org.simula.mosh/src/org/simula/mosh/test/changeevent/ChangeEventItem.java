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


package org.simula.mosh.test.changeevent;

import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.CommonBehavior.ChangeEventOccurrence;
import org.eclipse.uml2.uml.ValueSpecification;
import org.simula.constraint.BooleanValueSpecificationInstance;
import org.simula.constraint.ConstraintInstanceFactory;
import org.simula.mosh.model.loci.ActiveObjectActivation;

public class ChangeEventItem {
	
	private ActiveObjectActivation objectActivation = null;
	private ValueSpecification changeExpression = null;
	private BooleanValueSpecificationInstance specificationInstance = null;
	
	public ChangeEventItem(ActiveObjectActivation objectActivation, ValueSpecification changeExpression){
		this.objectActivation = objectActivation;
		this.changeExpression = changeExpression;
		
		specificationInstance = ConstraintInstanceFactory.instance.
				createBooleanValueSpecificationInstance(changeExpression, objectActivation.object);
	}
	
	public boolean evaluate(){
		return specificationInstance.evaluate();
	}
	
	public void sendChangeEvent(){
		
		ChangeEventOccurrence changeEventOccurrence = new ChangeEventOccurrence();
		changeEventOccurrence.changeExpression = this.changeExpression;
		objectActivation.send(changeEventOccurrence);
		
	}
	
	public boolean equals(ChangeEventItem item){
		
		if(objectActivation.equals(item.objectActivation) && 
			changeExpression.equals(item.changeExpression)){
			return true;
		}
		else{
			return false;
		}
		
	}
	
}