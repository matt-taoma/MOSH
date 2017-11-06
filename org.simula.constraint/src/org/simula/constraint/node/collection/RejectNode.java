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


package org.simula.constraint.node.collection;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.Object_;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.Value;
import org.simula.constraint.node.ConstraintNode;
import org.simula.constraint.utils.IteratorVariable;

public class RejectNode extends CollectionNode {
	
	ConstraintNode expNode;
	
	public RejectNode(List<Value> collection, IteratorVariable iterator1, IteratorVariable iterator2,
			ConstraintNode expNode) {
		super(collection, iterator1, null);
		assert (iterator1 != null);
		assert (iterator2 == null);
		this.expNode = expNode;
	}

	@Override
	public Object getValue() {
		
		List result = new ArrayList();
		for(Value value : collection){
			iterators[0].setCurObject((Object_)value);
			if(!((Boolean)expNode.getValue())){
				result.add(value);
			}
		}
		return result;
	}
}