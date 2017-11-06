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


package org.simula.mosh.test.reinforcement.qlearning;

import java.util.List;

import org.eclipse.uml2.uml.CallEvent;
import org.eclipse.uml2.uml.ChangeEvent;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.Event;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.Transition;
import org.eclipse.uml2.uml.Trigger;
import org.simula.mosh.model.Semantics.MoshExternalTransitionActivation;
import org.simula.mosh.model.loci.ActiveObjectActivation;
import org.simula.mosh.test.invocation.Invocation;

public class MDPTransition {
	
	public MoshExternalTransitionActivation transitinoActivation;
	
	public MDPState source;
	public MDPState target;
	
	private double q;
	
	private QIntervalValue faultDetectionProbability;
	
	public Invocation invocation;
	
	public boolean useShadow = false;
	
	public MDPTransition(MoshExternalTransitionActivation transitionActivation, MDPState source, MDPState target){
		this.transitinoActivation = transitionActivation;
		this.source = source;
		this.target = target;
		
		this.faultDetectionProbability = new QIntervalValue();
		this.q = QLearningTester.initialReward;
		
		if(transitionActivation == null){
			invocation = null;
			return;
		}
		
		ActiveObjectActivation objectActivation = (ActiveObjectActivation)transitionActivation.getExecutionContext().objectActivation;
		Transition transition = (Transition) transitionActivation.getNode();
		Trigger trigger = transition.getTriggers().get(0);
		Event event = trigger.getEvent();
		if (event instanceof CallEvent) {
			
			invocation = new Invocation(objectActivation, transitionActivation, (CallEvent)event);

		} else if (event instanceof ChangeEvent) {
			
			Stereotype stereotype = event.getAppliedStereotype("healing::Fault");
			if (stereotype != null) {
				invocation = new Invocation(objectActivation, transitionActivation, (ChangeEvent) event);
			}
		}
		
		if(invocation.operation.getName().equals("stop")){
			q = 0;
		}
		
	}
	
	public void invoke(){
		
		invocation.invoke();
		
	}
	
	public boolean isFaultInjection(){
		return invocation.isFaultInjection();
	}
	
	public Invocation getInvocation(){
		return invocation;
	}
	
	private double getReward(){
		
		if(invocation.operation.getName().equals("stop")){
			return 0;
		}
		
		if(faultDetectionProbability.getSampleSize() == 0){
			return QLearningTester.initialReward;
		}
		
		double upper = faultDetectionProbability.getUpperValue(); // TODO * 0.9 + distanceToTerminal * 0.1;
//		double lower = faultDetectionProbability.getLowerValue();
		double max = faultDetectionProbability.getMax();
		int size = faultDetectionProbability.getSampleSize();
		
		double reward = upper;//(upper - 0.5);// + faultDetectionProbability.bonus;
//		reward = Math.pow(reward, size);
		
		if(upper < 0.5){
			return 0;
		}
		else if(size >= 10 && upper < 0.8){
			return 0;
		}
		else if(size >= 20 && max < 0.9){
			return 0;
		}
		return reward;
	}
	
	public void updateQ(){
		
		double reward = getReward();
		if(!invocation.isFaultInjection()){ // fault injection is the last action
			q = reward;
		}
		else{
			if(reward > QLearningTester.discountFactor * target.getQ(useShadow)){
				q = reward;
			}
			else{
				q = QLearningTester.discountFactor * target.getQ(useShadow);
			}
		}
		
	}
	
	public void updateQ(double fdpSample){
		
		faultDetectionProbability.addSample(fdpSample);
		
		updateQ();
		
	}
	
	public double getCoverage(){
		return 1 - faultDetectionProbability.getSampleSize();
	}
	
	public double getQ(){
		
		return q;
		
//		if(faultDetectionProbability.getSampleSize() < 3){
//			return Math.pow(q, faultDetectionProbability.getSampleSize());
//		}
//		else{
//			double variance = faultDetectionProbability.getUpperValue() - faultDetectionProbability.getLowerValue();
//			double extraWeight = variance / 0.01;  // if this is small, it means faultDetectionProbability is stable. 
//													//  we should reduce its q
//			if(extraWeight > 1){
//				extraWeight = 1;
//			}
//			int n = faultDetectionProbability.getSampleSize() + (int)(10 * (1 - extraWeight));
//			
//			
//			double qValue = Math.pow(q, n);
//			return qValue;
//			
//		}
		
		
	}
	
	public String getOperationName(){
		if(invocation == null)
			return "";
		else
			return invocation.operation.getName();
	}
	
	public void updateInvocation4NewTest(MoshExternalTransitionActivation newTransitionActivation){
		
		this.transitinoActivation = newTransitionActivation;
		
		if(transitinoActivation == null){
			System.err.println("!!!   transitinoActivation is null...");
			return;
		}
		
		invocation.objectActivation = (ActiveObjectActivation)newTransitionActivation.getExecutionContext().objectActivation;
		invocation.transitionActivation = newTransitionActivation;
		
		Transition transition = (Transition) newTransitionActivation.getNode();
		Trigger trigger = transition.getTriggers().get(0);
		Event event = trigger.getEvent();
		if (event instanceof CallEvent) {

			invocation.operation = ((CallEvent) event).getOperation();
			invocation.guard = transition.getGuard();
			

		} else if (event instanceof ChangeEvent) {
			Stereotype stereotype = event.getAppliedStereotype("healing::Fault");
			if (stereotype != null) {

				List<Operation> operations = (List<Operation>) event.getValue(stereotype, "injectionOperation");
				List<Constraint> conditions = (List<Constraint>) event.getValue(stereotype, "condition");
				invocation.operation = operations.get(0);
				invocation.guard = null;
				if (!conditions.isEmpty()) {
					invocation.guard = conditions.get(0);
				}
				invocation.faultEvent = (ChangeEvent) event;

			}
		}
		
	}
	
	public boolean isNullTransition(){
		
		return this.transitinoActivation == null;
		
	}
	
	public boolean isCovered(){
		return faultDetectionProbability.getSampleSize() > 0;
	}
	
	public String toString(){
		
		StringBuffer str = new StringBuffer();
		
		if(this.transitinoActivation == null){
			str.append("*Null transition ");
		}
		else{
			str.append(this.invocation.operation.getName() + " ");
		}
//		str.append("S" + source.id + " -> ");
		str.append("S");
		str.append(target.id);
		str.append(" q(");
		str.append(q);
		str.append(") v");
		str.append(faultDetectionProbability);
		
		
		return str.toString();
	}
	
	
	
}