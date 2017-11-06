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


package org.simula.mosh.test.uncertainty;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.impl.DynamicEObjectImpl;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.ExtensionalValue;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.FeatureValue;
import org.eclipse.uml2.uml.Stereotype;
import org.simula.mosh.model.Semantics.ActiveObject;
import org.simula.mosh.test.ReinforcementLearningAgentProxy;
import org.simula.mosh.test.utils.PropertyUtil;

public class RLUncertaintyIntroducer extends UncertaintyIntroducer {
	
	private List<Uncertainty> uncertainties;
	
	private ReinforcementLearningAgentProxy rlAgent;
	
	private int timestepLimit = 50;
	
	public RLUncertaintyIntroducer(List<ExtensionalValue> extensionalValues) {
		
		uncertainties = new ArrayList<Uncertainty>();
		
		for (ExtensionalValue value : extensionalValues) {
			if (value instanceof ActiveObject) {
				ActiveObject activeObject = (ActiveObject) value;
				addUncertainties(activeObject);
			}
		}
		
	}
	
	private void addUncertainties(ActiveObject activeObject) {
		
		for(FeatureValue featureValue : activeObject.featureValues) {
			Stereotype stereotype = featureValue.feature.getAppliedStereotype("uncertainty::Uncertainty");
			if(stereotype == null){
				continue;
			}
			
			List<DynamicEObjectImpl> objs = (List<DynamicEObjectImpl>)featureValue.feature.getValue(stereotype, "universe");
			DynamicEObjectImpl obj = objs.get(0);
			Double min = (Double)obj.dynamicGet(0);
			Double max = (Double)obj.dynamicGet(1);
			String name = featureValue.feature.getName();
			Uncertainty unc = new Uncertainty(name, min, max);
			uncertainties.add(unc);
			
		}
		
	}
	
	
	public String getNextUncertaintyValues(double[] state) {
		
		if(rlAgent == null) {
			rlAgent = new ReinforcementLearningAgentProxy();
			rlAgent.start(state.length, uncertainties.size(), timestepLimit, null);
		}
		
		double[] output = rlAgent.nextEpoch(state);
		
		
		NumberFormat formatter = new DecimalFormat("#.#######");  
		
		StringBuffer res = new StringBuffer();
		
		for(int i = 0; i < uncertainties.size(); i++){
			Uncertainty uncertainty = uncertainties.get(i);
			res.append(uncertainty.name);
			res.append(":");
			double value = output[i] * (uncertainty.max - uncertainty.min) + uncertainty.min;
			res.append(formatter.format(value));
			res.append(";");
		}

		return res.toString();
		
	}
	
	public void episodeFinish(double reward) {
		
		rlAgent.episodeFinish(reward);
	}
	
	public static void main(String[] args) {
		NumberFormat formatter = new DecimalFormat("#.#######");  
		double a = 123456.1234567891;
		System.out.println(formatter.format(a));
	}
	
}