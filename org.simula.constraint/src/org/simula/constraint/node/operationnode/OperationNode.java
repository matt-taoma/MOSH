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


package org.simula.constraint.node.operationnode;

import org.simula.constraint.node.ConstraintNode;

public abstract class OperationNode extends ConstraintNode {

	protected ConstraintNode sourceNode;
	protected ConstraintNode argumentNode;
	
	
	// "oclIsTypeOf", "oclIsKindOf", "oclIsNew", "oclIsUndefined", "oclIsInvalid","notEmpty","isEmpty"
	public static String symbol = "DEFAULT";
	
	public OperationNode(ConstraintNode sourceNode, ConstraintNode argumentNode){
		this.sourceNode = sourceNode;
		this.argumentNode = argumentNode;
	}
	
	protected double getValue(Object v){
		if(v instanceof Integer){
			return (Integer)v;
		}
		else {
			return (Double)v;
		}
		
	}
	
}