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

import java.util.ArrayList;
import java.util.List;

import org.simula.constraint.utils.IteratorVariable;

public class IteratorVariableNode extends ConstraintNode {
	
	private IteratorVariable value;
	
	private List<String> navigations = new ArrayList<String>();
	
	public IteratorVariableNode(IteratorVariable value){
		this.value = value;
	}
	
	public void addNavigation(String navigation){
		navigations.add(navigation);
	}
	
	@Override
	public Object getValue() {
		return value.getFeatureValue(navigations);
	}
	
	
	
}