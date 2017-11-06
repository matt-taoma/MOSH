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

public class AbsOpNode extends OperationNode {
	
public static String symbol = "abs";
	
	public AbsOpNode(ConstraintNode sourceNode, ConstraintNode argumentNode) {
		super(sourceNode, argumentNode);
	}

	@Override
	public Object getValue() {
		Object sourceValue = argumentNode.getValue();
		if(sourceValue instanceof Integer ){
			return Math.abs((Integer)sourceValue);
		}
		else if(sourceValue instanceof Double ){
			return Math.abs((Double)sourceValue);
		}
		return null;
	}
	
	public double getRange(){
		return argumentNode.getRange();
	}
	
}