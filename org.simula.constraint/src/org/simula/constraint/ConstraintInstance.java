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
import org.eclipse.uml2.uml.Stereotype;
import org.simula.constraint.node.ConstraintNode;

public class ConstraintInstance {
	
	public int violatedTimes = 0;
	
	private Constraint constraint;
	private ConstraintNode validator;
	private boolean isSafeCondition;
	
	public ConstraintInstance(Constraint constraint, ConstraintNode validator){
		Stereotype safeCondition = constraint.getAppliedStereotype("healing::SafeCondition");
		if(safeCondition == null){
			this.isSafeCondition = false;
		}
		else{
			this.isSafeCondition = true;
		}
		this.constraint = constraint;
		this.validator = validator;
	}
	
	public boolean isSafeCondition(){
		return isSafeCondition;
	}
	
	public boolean evaluate() {
		
		boolean result = (Boolean)validator.getValue();
		
		return result;
	}
	
	public double getViolationDistance(){
		
		double result = validator.getViolationDistance();
		
		return result;
		
	}
	
	public double getSafeDistance(){
		
		double result = validator.getSafeDistance();
		
		if(result > 1){
			result = 1;
		}
		
		return result;
	}
	
	public String toString(){
		return constraint.getSpecification().stringValue();
	}
	
}