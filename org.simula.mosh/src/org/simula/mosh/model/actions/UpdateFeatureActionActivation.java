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


package org.simula.mosh.model.actions;

import org.eclipse.papyrus.moka.fuml.Semantics.Actions.BasicActions.ActionActivation;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.FeatureValue;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.IntegerValue;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.RealValue;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.Value;
import org.eclipse.papyrus.moka.fuml.Semantics.CommonBehaviors.BasicBehaviors.ParameterValue;
import org.eclipse.uml2.uml.OpaqueAction;
import org.simula.mosh.model.Semantics.ActiveObject;

public class UpdateFeatureActionActivation extends ActionActivation{
	
	public Double parseValue(String str){
		
		try{
			return Double.parseDouble(str);
		}
		catch (NumberFormatException e){
			return null;
		}
		
		
	}
	
	@Override
	public void doAction() {
		
		String script = ((OpaqueAction)node).getBodies().get(0);
		
		String elems[] = script.split(" ");
		
		String nameL = elems[0].trim();
		FeatureValue featureValueL = ((ActiveObject)this.getExecutionContext()).getFeature(nameL);
		//elems[1] : =
		String nameR = elems[2].trim();
		Double v = parseValue(nameR);
		if(v != null){
			RealValue newValue = new RealValue();
			newValue.value = v;
			
			featureValueL.values.clear();
			featureValueL.values.add(newValue);
			return;
		}
		
		FeatureValue featureValueR = ((ActiveObject)this.getExecutionContext()).getFeature(nameR);
		
		if(elems.length == 3){
			RealValue newValue = new RealValue();
			newValue.value = getValue(featureValueR.values.get(0));
			
			featureValueL.values.clear();
			featureValueL.values.add(newValue);
			return;
		}
		
		
		
		String operator = elems[3].trim();
		
		String pName = elems[4].trim();
		v = parseValue(pName);
		if(v == null) {
			Value value = null;
			for(ParameterValue parameter : this.getActivityExecution().parameterValues){
				if(parameter.parameter.getName().equals(pName)){
					value = parameter.values.get(0);
				}
			}
			
			v = getValue(value);
		}
		
		
		
		RealValue newValue = new RealValue();
		if(operator.equals("+")){
			double f = getValue(featureValueR.values.get(0));
			newValue.value = f + v;
		}
		else if(operator.equals("-")){
			double f = getValue(featureValueR.values.get(0));
			newValue.value = f - v;
		}
		else if (operator.equals("/")) {
			double f = getValue(featureValueR.values.get(0));
			newValue.value = f / v;
		}
		else if (operator.equals("*")) {
			double f = getValue(featureValueR.values.get(0));
			newValue.value = f * v;
		}
		
		featureValueL.values.clear();
		featureValueL.values.add(newValue);
	}
	
	private double getValue(Value value){
		if(value instanceof IntegerValue){
			return ((IntegerValue)value).value;
		}
		else if(value instanceof RealValue){
			return ((RealValue)value).value;
		}
		assert(false);
		return 0;
	}
	
}