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


package org.simula.mosh.test;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.papyrus.moka.fuml.FUMLExecutionEngine;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.ExtensionalValue;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.FeatureValue;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.RealValue;
import org.eclipse.papyrus.moka.fuml.Semantics.CommonBehaviors.BasicBehaviors.Execution;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.StateMachines.StateConfiguration;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.StateMachines.StateMachineExecution;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.StateMachines.TransitionActivation;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.StateMachines.VertexActivation;
import org.eclipse.uml2.uml.ChangeEvent;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Event;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.Transition;
import org.eclipse.uml2.uml.Trigger;
import org.simula.mosh.model.Semantics.ActiveObject;
import org.simula.mosh.model.loci.ActiveObjectActivation;
import org.simula.mosh.model.loci.MoshExecutor;
import org.simula.mosh.test.changeevent.ChangeEventItem;

public class FeatureValueMonitor implements Runnable {
	
	private List<Execution> outputOperations;
	private List<ChangeEventItem> registeredChangeEvents;
	
	private MoshExecutor executor;
	
	public FeatureValueMonitor(List<ExtensionalValue> extensionalValues, MoshExecutor executor) {
		
		outputOperations = new ArrayList<Execution>();
		
		registeredChangeEvents = new ArrayList<ChangeEventItem>();
		
		for(ExtensionalValue value : extensionalValues) {
			if(value instanceof ActiveObject) {
				addOutputOperations((ActiveObject)value);
			}
		}
		
		this.executor = executor;
		
	}
	
	public synchronized void init(List<ExtensionalValue> extensionalValues){
		
		outputOperations.clear();
		registeredChangeEvents.clear();
		
		for(ExtensionalValue value : extensionalValues) {
			if(value instanceof ActiveObject) {
				addOutputOperations((ActiveObject)value);
			}
		}
	}
	
	private void addOutputOperations(ActiveObject object){
		
		for(Class type : object.types){
			for(Operation operation : type.getOperations()) {
				if(operation.getAppliedStereotype("testing::OutputOperation") != null){
					outputOperations.add(object.operationExecutions.get(operation.getName()));
				}
			}
		}
		
	}
	
	public boolean update(){
		
		assert(outputOperations.size() != 0);
		
		for(Execution outputAction : outputOperations){
			outputAction.execute();
			
			if(FUMLExecutionEngine.eInstance.isTerminated()){
				return false;
			}
		}
		
		return true;
	}
	
	public void run() {
		
		while(executor == null){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		while(!FUMLExecutionEngine.eInstance.isTerminated()){
			
			boolean needWait = false;
			synchronized(executor){
				for(Execution outputAction : outputOperations){
					outputAction.execute();
					
					if(FUMLExecutionEngine.eInstance.isTerminated()){
						break;
					}
				}
			}
			
			int sleepTime = 10;
			
			if(needWait){
				sleepTime = 5000;
			}
			else{
				evaluateChangeEvents();
			}
			
			// TODO here should wait until next cycle has completed
			
			try {
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}
		
	}
	
	public synchronized boolean evaluateChangeEvents() {
		
		List<ChangeEventItem> notTriggeredChangeEvents = new ArrayList<ChangeEventItem>();
		
		boolean changeEventGenerated = false;
		
		for(ChangeEventItem item : registeredChangeEvents){
			if(item.evaluate()){
				item.sendChangeEvent();
				changeEventGenerated = true;
			}
			else{
				notTriggeredChangeEvents.add(item);
			}
		}
		
		this.registeredChangeEvents = notTriggeredChangeEvents;
		return changeEventGenerated;
	}
	
	public synchronized void registerChangeEvent(ActiveObjectActivation objectActivation){
		StateMachineExecution execution = (StateMachineExecution)(objectActivation.classifierBehaviorInvocations.get(0).execution);
		registerChangeEvent(objectActivation, execution.getConfiguration().getRoot());
		
	}
	
	private void registerChangeEvent(ActiveObjectActivation objectActivation, StateConfiguration configuration){

		for (StateConfiguration child : configuration.getChildren()) {
			registerChangeEvent(objectActivation, child);
		}

		VertexActivation vertexActivation = configuration.getVertexActivation();
		if (vertexActivation == null) {
			return;
		}

		for (TransitionActivation transition : vertexActivation.getOutgoingTransitions()) {
			List<Trigger> triggers = ((Transition) transition.getNode()).getTriggers();
			for (Trigger trigger : triggers) {
				Event event = trigger.getEvent();
				if (event instanceof ChangeEvent) {
					ChangeEventItem item = new ChangeEventItem(objectActivation,
							((ChangeEvent) event).getChangeExpression());
					if(item.evaluate()){
						item.sendChangeEvent();
					}
					else{
						addChangeEventItem(item);
					}
				}
			}
		}
		
	}
	
	private void addChangeEventItem(ChangeEventItem item){
		
		for(ChangeEventItem registeredItem : registeredChangeEvents){
			if(registeredItem.equals(item)){
				return;
			}
		}
		registeredChangeEvents.add(item);
	}

}