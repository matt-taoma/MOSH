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

import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.EnumerationValue;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.RealValue;
import org.eclipse.papyrus.moka.fuml.Semantics.CommonBehaviors.BasicBehaviors.ParameterValue;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.Parameter;

public class EnumerationRange extends Range{
	
	public Enumeration type;
	
	private EnumerationValue equal = null;
	private List<EnumerationValue> inequals = null;
	
	@Override
	public void setEqualValue(String value) {
		equal = getEnumerationValue(value);
	}
	@Override
	public void setInequalValue(String value) {
		if(inequals == null){
			inequals = new ArrayList<EnumerationValue>();
		}
		inequals.add(getEnumerationValue(value));
	}
	@Override
	public void setMinValue(String value) {
		
	}
	@Override
	public void setMaxValue(String value) {
		
	}
	
	private EnumerationValue getEnumerationValue(String value){
		
		EnumerationValue result = null;
		
		int index = 0;
		if( ( index = value.indexOf("::")) > 0){
			value = value.substring(index + 2);
		}
		
		List<EnumerationLiteral> literals = type.getOwnedLiterals();
		for(EnumerationLiteral l : literals){
			if(l.getName().equals(value)){
				result = new EnumerationValue();
				result.type = type;
				result.literal = l;
				return result;
			}
		}
		return null;
	}
	
	@Override
	public ParameterValue getValidValue(Parameter p, Random random) {
		ParameterValue pv = new ParameterValue();
		pv.parameter = p;
		
		EnumerationValue v = new EnumerationValue();
		
		if(equal != null){
			v = equal;
		}
		else{
			v.literal = getLiteral(random);
		}
		pv.values.add(v);
		return pv;
	}
	
	private EnumerationLiteral getLiteral(Random random){
		
		int i = 0;
		boolean found;
		
		List<EnumerationLiteral> literals = type.getOwnedLiterals();
		
		while(i < 100) { // try 100 times to get a valide value
			int t = random.nextInt(literals.size());
			EnumerationLiteral literal = literals.get(t);
			found = true;
			for(EnumerationValue inequal : inequals){
				if( inequal.literal.equals(literal)){
					found = false;
					break;
				}
			}
			
			if(found){
				return literal;
			}
			i++;
		}
		
		System.err.println("valid value not found");
		return null;
	}
	@Override
	public double getMinValue() {
		return 0;
	}
	@Override
	public double getMaxValue() {
		return type.getOwnedLiterals().size();
	}
	@Override
	public boolean isSingle() {
		return true;
	}
	
}