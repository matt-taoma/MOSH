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


package org.simula.constraint.utils;

import java.util.List;

import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.FeatureValue;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.Object_;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.Value;

public class IteratorVariable extends FeatureValue{
	
	private Object_ curObject = null;
	
	public IteratorVariable(String name){
		DummyFeature feature = new DummyFeature();
		feature.setName(name);
	}
	
	public void setCurObject(Object_ curObject){
		this.curObject = curObject;
	}
	
	public Value getFeatureValue(List<String> navigations){
		Object_ object = null;
		Value value = null;
		for(String navigation : navigations){
			if(object == null){
				object = curObject;
			}
			else{
				object = (Object_)value;
			}
			value = getValue(object, navigation);
		}
		return value;
	}
	
	private Value getValue(Object_ object, String navigation){
		for(FeatureValue feature : curObject.featureValues){
			if(feature.feature.getName().equals(navigation)){
				return feature.values.get(0);
			}
		}
		return null;
	}
	
}