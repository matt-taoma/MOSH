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

public class AndOpNode extends OperationNode {
	
	public static String symbol = "and";
	
	public AndOpNode(ConstraintNode sourceNode, ConstraintNode argumentNode) {
		super(sourceNode, argumentNode);
	}

	@Override
	public Object getValue() {
		return (Boolean)sourceNode.getValue() && (Boolean)argumentNode.getValue();
	}
	
	@Override
	public double getSafeDistance(){
		double d1 = sourceNode.getSafeDistance();
		double d2 = argumentNode.getSafeDistance();
		return (d1 < d2) ? d1 : d2;
	}
	
	@Override
	public double getViolationDistance(){
		double d1 = sourceNode.getViolationDistance();
		double d2 = argumentNode.getViolationDistance();
		
		if(d1 == 0 && d2 == 0){
			return 0;
		}
		else{
			return 1 - 1 / (d1 + d2 + 1);
		}
	}
	
}