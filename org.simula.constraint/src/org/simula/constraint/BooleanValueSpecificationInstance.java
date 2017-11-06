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


package org.simula.constraint;

import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.ValueSpecification;
import org.simula.constraint.node.ConstraintNode;

public class BooleanValueSpecificationInstance {
	
	private ValueSpecification specification;
	private ConstraintNode validator;
	
	public BooleanValueSpecificationInstance(ValueSpecification specification, ConstraintNode validator){
		this.specification = specification;
		this.validator = validator;
	}
	
	public boolean evaluate() {
		
		return (Boolean)validator.getValue();
	}
	
	public String toString(){
		return specification.toString();
	}
	
}