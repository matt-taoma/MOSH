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

import java.util.List;

import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.FeatureValue;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.Object_;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.Value;
import org.eclipse.uml2.uml.Feature;
import org.simula.constraint.node.ConstraintNode;
import org.simula.constraint.utils.IteratorVariable;

public abstract class CollectionNode extends ConstraintNode{
	
	protected List<Value> collection;
	
	protected IteratorVariable[] iterators = new IteratorVariable[2];
	
	public CollectionNode(List<Value> collection, IteratorVariable iterator1, IteratorVariable iterator2){
		this.collection = collection;
		iterators[0] = iterator1;
		iterators[1] = iterator2;
	}
	
}