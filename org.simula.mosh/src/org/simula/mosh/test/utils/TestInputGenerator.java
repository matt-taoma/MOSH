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
import java.util.Map;
import java.util.Random;

import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.BooleanValue;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.EnumerationValue;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.IntegerValue;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.RealValue;
import org.eclipse.papyrus.moka.fuml.Semantics.CommonBehaviors.BasicBehaviors.ParameterValue;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.Parameter;
import org.eclipse.uml2.uml.PrimitiveType;
import org.eclipse.uml2.uml.Type;
import org.simula.constraint.ConstraintInstance;
import org.simula.constraint.ConstraintInstanceFactory;
import org.simula.mosh.test.utils.esocl.AVM;

public class TestInputGenerator {
	
	
	private static Random random = new Random();
	
	private static TestInputGenerator instance = null;
	
	public List<ParameterValue> generateTestInput(Constraint constraint, List<Parameter> parameters){
		
		List<ParameterValue> values;
		
		if(constraint == null){
			values = new ArrayList<ParameterValue>();
			for(Parameter p : parameters){
				values.add(getUnlimitedParameterValue(p));
			}
			return values;
		}
		else{
			values = getFixedValue(constraint, parameters);
			if(values != null){
				return values;
			}
		}
		
		
		values = new ArrayList<ParameterValue>();
		for(Parameter p : parameters){
			values.add(getUnlimitedParameterValue(p));
		}
		
//		long beginTime = System.currentTimeMillis();
		
		ConstraintInstance instance = ConstraintInstanceFactory.instance.createConstraintInstance(constraint, values);
		
		AVM avm = new AVM();
		values = avm.getSolution(instance, values);
		
//		long stopTime = System.currentTimeMillis();
		
//		System.out.println(values + " " + (stopTime - beginTime));
		
		return values;
		
	}
	
	/**
	 * The constraint only contains =, i.e., only a single value is valid
	 */
	private List<ParameterValue> getFixedValue(Constraint constraint, List<Parameter> parameters){
		
		String constraintStr = constraint.getSpecification().stringValue();
		
		if(constraintStr.contains(">") || constraintStr.contains("<")){
			return null;
		}
		
		//only contains =
		List<ParameterValue> values = new ArrayList<ParameterValue>();
		String[] items = constraintStr.split("and");
		for(int i = 0; i < items.length; i++){
			String[] pair = items[i].split("=");
			String paramName = pair[0].trim();
			String value = pair[1].trim();
			ParameterValue parameterValue = new ParameterValue();
			parameterValue.parameter = getParameter(parameters, paramName);
			
			Type type = parameterValue.parameter.getType();
			if(type.getName().equals("Integer")){
				IntegerValue v = new IntegerValue();
				v.value = Integer.parseInt(value);
				parameterValue.values.add(v);
			}
			else if(type.getName().equals("Real")){
				RealValue v = new RealValue();
				v.value = Double.parseDouble(value);
				parameterValue.values.add(v);
			}
			else if(type.getName().equals("Boolean")){
				BooleanValue v = new BooleanValue();
				v.value = Boolean.parseBoolean(value);
				parameterValue.values.add(v);
			}
			values.add(parameterValue);
		}
		
		return values;
		
	}
	
	private Parameter getParameter(List<Parameter> parameters, String paramName){
		for(Parameter p : parameters){
			if(p.getName().equals(paramName)){
				return p;
			}
		}
		return null;
	}
	
	private ParameterValue getUnlimitedParameterValue(Parameter p){
		ParameterValue parameterValue = new ParameterValue();
		parameterValue.parameter = p;
		double max = parameterValue.getMax();
		double min = parameterValue.getMin();
		
		// TODO only support Integer Real Boolean Enumeration
		
		Type type = p.getType();
		
		if(type instanceof PrimitiveType){
			
			if(type.getName().equals("Integer")){
				IntegerValue v = new IntegerValue();
				v.value = random.nextInt((int)(max - min)) + (int)min;
				parameterValue.values.add(v);
			}
			else if(type.getName().equals("Real")){
				RealValue v = new RealValue();
				v.value = random.nextDouble() * (max - min) + min;
				parameterValue.values.add(v);
			}
			else if(type.getName().equals("Boolean")){
				BooleanValue v = new BooleanValue();
				v.value = random.nextBoolean();
				parameterValue.values.add(v);
			}
		}
		else if(type instanceof Enumeration){
			
			EnumerationValue v = new EnumerationValue();
			v.type = (Enumeration)type;
			List<EnumerationLiteral> literals = ((Enumeration)type).getOwnedLiterals();
			v.literal = literals.get(random.nextInt(literals.size()));
			parameterValue.values.add(v);
		}
		
		return parameterValue;
	}
	
	public static TestInputGenerator instance(){
		if(instance == null){
			instance = new TestInputGenerator();
		}
		return instance;
	}
	
	
}