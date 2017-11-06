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


package org.simula.mosh.model.Semantics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.papyrus.moka.async.fuml.Semantics.CommonBehaviors.Communications.AsyncObjectActivation;
import org.eclipse.papyrus.moka.async.fuml.debug.AsyncControlDelegate;
import org.eclipse.papyrus.moka.async.fuml.debug.AsyncDebug;
import org.eclipse.papyrus.moka.fuml.FUMLExecutionEngine;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.BooleanValue;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.EnumerationValue;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.FeatureValue;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.IntegerValue;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.Object_;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.RealValue;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.StringValue;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.Value;
import org.eclipse.papyrus.moka.fuml.Semantics.CommonBehaviors.BasicBehaviors.Execution;
import org.eclipse.papyrus.moka.fuml.Semantics.CommonBehaviors.BasicBehaviors.ParameterValue;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.CommonBehavior.CallEventExecution;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.CommonBehavior.CallEventOccurrence;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.StructuredClassifiers.SM_Object;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.PrimitiveType;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Reception;
import org.eclipse.uml2.uml.Signal;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.StructuralFeature;
import org.eclipse.uml2.uml.Type;
import org.simula.mosh.model.loci.ActiveObjectActivation;

public class ActiveObject extends SM_Object{
	
	public Map<String, Execution> operationExecutions;
	
	private String name = null;
	
	@Override
	public void startBehavior(Class classifier, List<ParameterValue> inputs) {
		
		name = classifier.getName();
		
		for(FeatureValue feature : featureValues){
			for(Value value : feature.values){
				if(value instanceof Object_){
					// TODO only support one type
					Class type = (Class)value.getTypes().get(0);
					if(type.getClassifierBehavior() != null){
						((Object_)value).startBehavior(type, null);
					}
				}
			}
		}
		
		if (this.objectActivation == null) {
			this.objectActivation = new ActiveObjectActivation(classifier, inputs);
			this.objectActivation.object = this;
		}
		/* 1. Create Thread Name */
		String threadName = "Thread_";
		Iterator<Class> classes = this.types.iterator();
		while (classes.hasNext()) {
			threadName += classes.next().getName();
			if (classes.hasNext()) {
				threadName += "|";
			}
		}
		/* 2. Pattern to start the Runnable corresponding to the AsyncObjectActivation */
		// Thread objectactivationThread = new Thread((AsyncObjectActivation)this.objectActivation, threadName);
		objectactivationThread = new Thread((AsyncObjectActivation) this.objectActivation, threadName);

		((AsyncControlDelegate) FUMLExecutionEngine.eInstance.getControlDelegate()).registerObjectActivation(this.objectActivation, threadName); // Added for connection with debug api

		objectactivationThread.start();

		AsyncDebug.println("[NEW THREAD] Active object instance started on a new thread");
	}
	
	@Override
	public void setFeatureValue(StructuralFeature feature, List<Value> values, Integer position) {
		// Set the value(s) of the member of featureValues for the given
		// feature.
		FeatureValue featureValue = this.getFeatureValue(feature);
		if (featureValue == null) {
			featureValue = new FeatureValue();
			this.featureValues.add(featureValue);
		}
		featureValue.feature = feature;
		featureValue.position = position;
		
		if(values.isEmpty() && feature instanceof Property){
			Type type = feature.getType();
			if(type instanceof Class){
				Class featureClass = (Class)feature.getType();
				for(int i = 0; i < feature.getLower(); i ++) {
					values.add(this.locus.instantiate(featureClass));
				}
			}
			else{
				String defaultValue = ((Property)feature).getDefault();
				if(type instanceof Enumeration){
					EnumerationLiteral literal = null;
					if(defaultValue == null){
						literal = ((Enumeration)type).getOwnedLiterals().get(0);
						
					}
					else{
						literal = getLiteral((Enumeration)type, defaultValue);
					}
					EnumerationValue value = new EnumerationValue();
					value.literal = literal;
					value.type = (Enumeration)type;
					values.add(value);
				}
				else if(type instanceof PrimitiveType){
					
					if(type.getName().equals("Boolean")){
						BooleanValue bValue = new BooleanValue();
						if(defaultValue == null){
							bValue.value = false;
						}
						else{
							bValue.value = Boolean.parseBoolean(defaultValue);
						}
						values.add(bValue);
					}
					else if(type.getName().equals("Integer")){
						IntegerValue iValue = new IntegerValue();
						if(defaultValue == null){
							iValue.value = 0;
						}
						else{
							iValue.value = Integer.parseInt(defaultValue);
						}
						values.add(iValue);
					}
					else if(type.getName().equals("Real")){
						RealValue rValue = new RealValue();
						if(defaultValue == null){
							rValue.value = 0;
						}
						else{
							rValue.value = Double.parseDouble(defaultValue);
						}
						values.add(rValue);
					}
					else if(type.getName().equals("String")){
						StringValue sValue = new StringValue();
						if(defaultValue == null){
							sValue.value = "";
						}
						else{
							sValue.value = defaultValue;
						}
						values.add(sValue);
					}
				}
			}
		}
		featureValue.values = values;
		
		Stereotype safetyVariable = feature.getAppliedStereotype("healing::SafetyVariable");
		if(safetyVariable != null){
			featureValue.minValue = (Double)feature.getValue(safetyVariable, "min");
			featureValue.maxValue = (Double)feature.getValue(safetyVariable, "max");
		}
		
	}
	
	private EnumerationLiteral getLiteral(Enumeration enumeration, String literalName){
		for(EnumerationLiteral literal : enumeration.getOwnedLiterals()){
			if(literal.getName().equals(literalName)){
				return literal;
			}
		}
		return null;
	}
	
	public void createOperationExecutions() {
		
		operationExecutions = new HashMap<String, Execution>();
		
		for(Class type : types){
			for(Operation operation : type.getOperations()) {
				
				List<Behavior> methods = operation.getMethods();
				if(methods.isEmpty()){
					continue;
				}
				// TODO only supports one method
				Behavior method = methods.get(0);
				Execution execution = this.locus.factory.createExecution(method, this);
				execution.types.add(method);
				operationExecutions.put(operation.getName(), execution);
			}
		}
		
		
	}
	
	public void callOperation(String operationName, List<ParameterValue> parameterValues){
		
		for(Class type : types){
			
			Execution execution = operationExecutions.get(operationName);
			
			if(execution == null){
				return;
			}
			
			for(ParameterValue parameterValue : parameterValues){
				execution.setParameterValue(parameterValue);
			}
			execution.execute();
			
			
			Operation operation = type.getOperation(operationName, null, null);
			CallEventExecution callEventExecution = new CallEventExecution();
			callEventExecution.context = this;
			callEventExecution.operation = operation;
			callEventExecution.behavior = operation.getMethods().get(0);
			callEventExecution.parameterValues = parameterValues;
			CallEventOccurrence eventOccurrence = new CallEventOccurrence();
			eventOccurrence.execution = callEventExecution;
			((AsyncObjectActivation)objectActivation).send(eventOccurrence);
			
			System.out.println("~~~ send call operation event: " + operation.getName());
			
			break;
		}
	}
	
	public FeatureValue getFeature(String attrName) {
		
		for(FeatureValue featureValue : featureValues){
			if(featureValue.feature.getName().equals(attrName)) {
				return featureValue;
			}
		}
		return null;
	}
	
	public Value getFeatureValue(String attrName, int index) {
		
		for(FeatureValue featureValue : featureValues){
			if(featureValue.feature.getName().equals(attrName)) {
				return featureValue.values.get(index);
			}
		}
		return null;
	}
	
	public Signal getSignal(String signalName){
		Class type = this.types.get(0);
		List<Reception> receptions = type.getOwnedReceptions();
		for(Reception reception : receptions){
			Signal signal = reception.getSignal();
			if(signal.getName().equals(signalName)){
				return signal;
			}
		}
		return null;
	}
	
	public String getName(){
		return name;
	}
	
	public String toString(){
		return "";
	}
	
}