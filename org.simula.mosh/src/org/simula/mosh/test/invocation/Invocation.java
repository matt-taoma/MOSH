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


package org.simula.mosh.test.invocation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.BooleanValue;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.IntegerValue;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.RealValue;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.Value;
import org.eclipse.papyrus.moka.fuml.Semantics.CommonBehaviors.BasicBehaviors.ParameterValue;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.StateMachines.TransitionActivation;
import org.eclipse.uml2.uml.CallEvent;
import org.eclipse.uml2.uml.ChangeEvent;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.Parameter;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.Transition;
import org.eclipse.uml2.uml.Type;
import org.simula.mosh.model.loci.ActiveObjectActivation;
import org.simula.mosh.test.log.TestLogger;
import org.simula.mosh.test.utils.TestInputGenerator;

public class Invocation {
	
	public ActiveObjectActivation objectActivation = null;
	public TransitionActivation transitionActivation;
	public Operation operation = null;
	public Constraint guard = null;
	public ChangeEvent faultEvent;
	
	public Double selectedInput = null;
	
//	private List<ParameterValue> bestTestInput = null;
//	private List<ParameterValue> lastTestInput = null;
//	private double highestFaultDetectionProbability = 0;
//	private double resetProbability = 0.01;
	
	public boolean isFaultInjection(){
		return faultEvent != null;
	}
	
	public Invocation(ActiveObjectActivation objectActivation, TransitionActivation transitionActivation, CallEvent event){
		this.objectActivation = objectActivation;
		this.transitionActivation = transitionActivation;
		
		Transition transition = (Transition) transitionActivation.getNode();
		this.operation = ((CallEvent) event).getOperation();
		this.guard = transition.getGuard();
		this.faultEvent = null;
		
	}
	
	public Invocation(ActiveObjectActivation objectActivation, TransitionActivation transitionActivation, ChangeEvent event){
		this.objectActivation = objectActivation;
		this.transitionActivation = transitionActivation;
		
		Stereotype stereotype = event.getAppliedStereotype("healing::Fault");
		List<Operation> operations = (List<Operation>) event.getValue(stereotype, "injectionOperation");
		List<Constraint> conditions = (List<Constraint>) event.getValue(stereotype, "condition");
		this.operation = operations.get(0);
		if (!conditions.isEmpty()) {
			this.guard = conditions.get(0);
		}
		this.faultEvent = event;
	}
	
//	public Invocation(ActiveObjectActivation objectActivation, Operation operation, double value, ChangeEvent faultEvent){
//		this.objectActivation = objectActivation;
//		this.operation = operation;
//		this.guard = null;
//		this.faultEvent = faultEvent;
//		
//		if(!operation.getOwnedParameters().isEmpty()){
//			selectedInput = value;
//		}
//		
//	}
	
	private Value getValue(Parameter p, double v){
		Type type = p.getType();
		Value value = null;
		if(type.getName().equals("Integer")){
			value = new IntegerValue();;
			((IntegerValue)value).value = (int) v;
		}
		else if(type.getName().equals("Real")){
			value = new RealValue();
			((RealValue)value).value = v;
		}
		else if(type.getName().equals("Boolean")){
			value = new BooleanValue();
			if(v == 0){
				((BooleanValue)value).value = false;
			}
			else{
				((BooleanValue)value).value = true;
			}
		}
		return value;
	}
	
	public void invoke(){
		
		if(selectedInput != null){
			
			List<ParameterValue> input = new ArrayList<ParameterValue>();
			
			// TODO : only support one input paprameter 
			if(operation.getOwnedParameters().size() == 1){
				Parameter p = operation.getOwnedParameters().get(0);
				ParameterValue pv = new ParameterValue();
				pv.parameter = p;
				pv.values.add(getValue(p, selectedInput));
				input.add(pv);
			}
			
			objectActivation.callOperation(operation, input);
			TestLogger.log(this);
		}
		else{
			
			List<ParameterValue> lastTestInput = TestInputGenerator.instance().generateTestInput(guard, operation.getOwnedParameters());
			
			if(lastTestInput.size() == 1){
				Value v = lastTestInput.get(0).values.get(0);
				if(v instanceof IntegerValue){
					selectedInput = (double)((IntegerValue)v).value;
				}
				else if(v instanceof RealValue){
					selectedInput = ((RealValue)v).value;
				}
				else if(v instanceof BooleanValue){
					if(((BooleanValue)v).value){
						selectedInput = 1.0;
					}
					else{
						selectedInput = 0.0;
					}
				}
			}
			
			List<ParameterValue> input = new ArrayList<ParameterValue>();
			for(ParameterValue pv : lastTestInput){
				input.add(pv.copy());
			}
			
			objectActivation.callOperation(operation, input);
			TestLogger.log(this);
		}
		
		
	}
	
	
//	public List<ParameterValue> getTestInput(){
//		return this.lastTestInput;
//	}
	
	public String toString(){
		StringBuffer str = new StringBuffer();
		
		if(isFaultInjection()){
			if(guard != null){
				str.append("fault injection: ");
				str.append(objectActivation.getName());
				str.append(".");
				str.append(operation.getName());
				str.append("()[");
				str.append(guard.getSpecification().stringValue());
				str.append("]");
			}
			else{
				str.append("fault injection: ");
				str.append(objectActivation.getName());
				str.append(".");
				str.append(operation.getName());
				str.append("()[]");
			}
		}
		else{
			if(guard != null){
				str.append(objectActivation.getName());
				str.append(".");
				str.append(operation.getName());
				str.append("()[");
				str.append(guard.getSpecification().stringValue());
				str.append("]");
			}
			else{
				str.append(objectActivation.getName());
				str.append(".");
				str.append(operation.getName());
				str.append("()[]");
			}
		}
		
		str.append("   (");
		str.append(selectedInput);
		str.append(", ");
		
//		if(lastTestInput != null){
//			str.append("   (");
//			for(ParameterValue value : lastTestInput){
//				str.append(value.parameter.getName());
//				str.append("=");
//				str.append(value.values.get(0));
//				str.append(", ");
//			}
//			str.append(")");
//		}
		
		
		return str.toString();
	}
	
	
}