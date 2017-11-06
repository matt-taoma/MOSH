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

import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.IntegerValue;
import org.eclipse.papyrus.moka.fuml.Semantics.CommonBehaviors.BasicBehaviors.ParameterValue;
import org.eclipse.uml2.uml.Parameter;

public class IntegerRange extends Range{
	
	private Integer min = 0;
	private Integer max = 9999;
	private List<Integer> inequals = null;
	
	@Override
	public void setEqualValue(String value) {
		min = Integer.parseInt(value);
		max = min;
	}
	@Override
	public void setInequalValue(String value) {
		if(inequals == null){
			inequals = new ArrayList<Integer>();
		}
		inequals.add(Integer.parseInt(value));
	}
	@Override
	public void setMinValue(String value) {
		min = Integer.parseInt(value);
	}
	@Override
	public void setMaxValue(String value) {
		max = Integer.parseInt(value);
	}
	@Override
	public ParameterValue getValidValue(Parameter p, Random random) {
		ParameterValue pv = new ParameterValue();
		pv.parameter = p;
		
		IntegerValue v = new IntegerValue();
		
		if(min == max){
			v.value = min;
		}
		else if(inequals != null) {
			v.value = getIntMinMaxInequals(random);
		}
		else{
			v.value = random.nextInt(max - min - 1) + min + 1;
		}
		pv.values.add(v);
		return pv;
	}
	
	private int getIntMinMaxInequals(Random random){
		
		int i = 0;
		boolean found;
		
		while(i < 100) { // try 100 times to get a valide value
			int t = random.nextInt(max - min - 1) + min + 1;
			
			found = true;
			for(Integer inequal : inequals){
				if( t == inequal){
					found = false;
					break;
				}
			}
			
			if(found){
				return t;
			}
			i++;
		}
		
		System.err.println("valid value not found");
		return 0;
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
		if(min - max == 0){
			return true;
		}
		else{
			return false;
		}
	}
	
	
}