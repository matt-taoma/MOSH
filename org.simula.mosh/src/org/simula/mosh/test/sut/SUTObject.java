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


package org.simula.mosh.test.sut;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.ExtensionalValue;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.FeatureValue;
import org.simula.mosh.model.Semantics.ActiveObject;
import org.simula.mosh.model.loci.ActiveObjectActivation;
import org.simula.mosh.test.utils.PropertyUtil;

public class SUTObject {
	
	
	private int activeObjectNum = -1;
	
	private Map<String, PropertyUtil> stateVariables;
	private Map<String, ActiveObjectActivation> activeObjectActivations;
	
	public void init(List<ExtensionalValue> extensionalValues){
		
		activeObjectNum = 0;
		
		activeObjectActivations = new HashMap<String, ActiveObjectActivation>();
		
		for (ExtensionalValue value : extensionalValues) {
			if (value instanceof ActiveObject) {
				ActiveObject activeObject = (ActiveObject) value;
				if (activeObject.types.get(0).getClassifierBehavior() != null) {
					activeObjectNum++;
					ActiveObjectActivation activeObjectActivation = (ActiveObjectActivation) activeObject.objectActivation;
					activeObjectActivations.put(activeObjectActivation.getName(), activeObjectActivation);
				}
			}
		}
		
		initStateVariables();
		
	}
	
	public void objectTerminated(ActiveObjectActivation objectActivation) {
		
		ActiveObjectActivation terminatedObject = activeObjectActivations.remove(objectActivation.getName());
		
		if (terminatedObject != null) {
			activeObjectNum--;
		} else {
			System.err.println(objectActivation.getName() + " is not contained in test driver");
		}
		
	}
	
	public int getActiveObjectNum() {
		return activeObjectNum;
	}
	
	public double[] getStateVariableValues() {
		
		double[] state = new double[stateVariables.size()];
		int i = 0;
		for(Entry<String, PropertyUtil> entry : stateVariables.entrySet()){
			state[i] = entry.getValue().getValue();
			i++;
		}
		return state;
	}
	
	public Map<String, ActiveObjectActivation> getActiveObjectActivations() {
		return activeObjectActivations;
	}
	
	public boolean areAllObjectsTerminated() {
		return activeObjectNum == 0;
	}
	
	private void initStateVariables() {
		
		if(stateVariables == null){
			stateVariables = new LinkedHashMap<String, PropertyUtil>();
			for (ActiveObjectActivation objectActivation : activeObjectActivations.values()) {
				for (FeatureValue featureValue : objectActivation.object.featureValues) {
					if (featureValue.feature.getAppliedStereotype("testing::StateVariable") != null) {
						PropertyUtil p = new PropertyUtil(featureValue);
						stateVariables.put(featureValue.feature.getName(), p);
					}
				}
			}
		}
		else{
			for (ActiveObjectActivation objectActivation : activeObjectActivations.values()) {
				for (FeatureValue featureValue : objectActivation.object.featureValues) {
					if (featureValue.feature.getAppliedStereotype("testing::StateVariable") != null) {
						String name = featureValue.feature.getName();
						PropertyUtil p = stateVariables.get(name);
						p.featureValue = featureValue;
					}
				}
			}
		}
		
	}
	
}