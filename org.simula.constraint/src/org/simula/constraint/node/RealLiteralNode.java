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


package org.simula.constraint.node;

public class RealLiteralNode extends ConstraintNode {

	private double value;
	
	public RealLiteralNode(double value){
		this.value = value;
	}
	
	@Override
	public Object getValue() {
		return value;
	}
	
	
	
}