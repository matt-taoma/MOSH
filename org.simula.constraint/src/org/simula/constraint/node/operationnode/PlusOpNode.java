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

public class PlusOpNode extends OperationNode {
	
	public static String symbol = "+";
	
	public PlusOpNode(ConstraintNode sourceNode, ConstraintNode argumentNode) {
		super(sourceNode, argumentNode);
	}

	@Override
	public Object getValue() {
		
		return getValue(sourceNode.getValue()) + getValue(argumentNode.getValue());
	}
	
	public double getRange(){
		
		double r1 = sourceNode.getRange();
		double r2 = argumentNode.getRange();
		
		if(r1 != 0){
			return r1;
		}
		else if(r2 != 0){
			return r2;
		}
		else{
			return 0;
		}
		
	}

}