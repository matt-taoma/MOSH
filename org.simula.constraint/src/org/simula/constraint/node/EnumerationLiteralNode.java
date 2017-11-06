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

import org.eclipse.uml2.uml.EnumerationLiteral;

public class EnumerationLiteralNode extends ConstraintNode {
	
	private EnumerationLiteral literal;
	
	public EnumerationLiteralNode(EnumerationLiteral literal){
		this.literal = literal;
	}
	
	@Override
	public Object getValue() {
		return literal;
	}
}