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


package org.simula.mosh.test.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.RealValue;
import org.eclipse.papyrus.moka.fuml.Semantics.CommonBehaviors.BasicBehaviors.ParameterValue;
import org.eclipse.uml2.uml.Parameter;

public class RealRange extends Range{
	
	private Double min = 0.0;
	private Double max = 9999.0;
	private List<Double> inequals = null;
	
	@Override
	public void setEqualValue(String value) {
		min = Double.parseDouble(value);
		max = min;
	}
	@Override
	public void setInequalValue(String value) {
		if(inequals == null){
			inequals = new ArrayList<Double>();
		}
		inequals.add( Double.parseDouble(value));
	}
	@Override
	public void setMinValue(String value) {
		min = Double.parseDouble(value);
	}
	@Override
	public void setMaxValue(String value) {
		max = Double.parseDouble(value);
	}
	
	@Override
	public ParameterValue getValidValue(Parameter p, Random random) {
		ParameterValue pv = new ParameterValue();
		pv.parameter = p;
		
		RealValue v = new RealValue();
		
		if(equal(min, max)){
			v.value = min;
		}
		else{
			v.value = random.nextDouble() * (max - min) + min;
		}
		pv.values.add(v);
		return pv;
	}
	
	private boolean equal(double a, double b){
		if(Math.abs(a - b) < 0.00000000001){
			return true;
		}
		else{
			return false;
		}
	}
	@Override
	public double getMinValue() {
		return min;
	}
	@Override
	public double getMaxValue() {
		return max;
	}
	@Override
	public boolean isSingle() {
		return false;
	}
	
}