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


package org.simula.mosh.model.loci;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.papyrus.moka.async.fuml.debug.AsyncControlDelegate;
import org.eclipse.papyrus.moka.async.fuml.debug.AsyncDebug;
import org.eclipse.papyrus.moka.fuml.FUMLExecutionEngine;
import org.eclipse.papyrus.moka.fuml.Semantics.Actions.CompleteActions.AcceptEventActionEventAccepter;
import org.eclipse.papyrus.moka.fuml.Semantics.CommonBehaviors.BasicBehaviors.ParameterValue;
import org.eclipse.papyrus.moka.fuml.Semantics.CommonBehaviors.Communications.EventAccepter;
import org.eclipse.papyrus.moka.fuml.Semantics.CommonBehaviors.Communications.EventOccurrence;
import org.eclipse.papyrus.moka.fuml.Semantics.CommonBehaviors.Communications.InvocationEventOccurrence;
import org.eclipse.papyrus.moka.fuml.Semantics.CommonBehaviors.Communications.SignalEventOccurrence;
import org.eclipse.papyrus.moka.fuml.Semantics.Loci.LociL1.ChoiceStrategy;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.CommonBehavior.CallEventOccurrence;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.CommonBehavior.SM_ObjectActivation;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Operation;
import org.simula.mosh.model.Semantics.ActiveObject;

public class ActiveObjectActivation extends SM_ObjectActivation {

	private boolean isTraversing = false; // true means the state machine is
										  // waiting for some events or is
										  // transitioning

	private String operationBeingInvoked = null;

	public ActiveObjectActivation(Class classifier, List<ParameterValue> inputs) {
		super(classifier, inputs);
	}

	public void setTraversing(boolean isTraversing) {
		this.isTraversing = isTraversing;
	}

	public boolean operationBeingInvoked() {
		return operationBeingInvoked != null;
	}

	public boolean isTraversing() {
		return isTraversing;
	}

	public void send(EventOccurrence eventOccurrence) {
		this.evtPool.send(eventOccurrence);
	}

	public void callOperation(Operation operation, List<ParameterValue> parameterValues) {
		operationBeingInvoked = operation.getName();
		((ActiveObject) object).callOperation(operation.getName(), parameterValues);
	}

	@Override
	public EventOccurrence getNextEvent() {
		// Added for connection with debug API
		if (this.evtPool.isEmpty()) {
			this.currentState = ObjectActivationState.WAITING;
			this.hasBeenWaiting = true;
			((AsyncControlDelegate) FUMLExecutionEngine.eInstance.getControlDelegate()).notifyWaitingStateEntered(this);
		}
		EventOccurrence eventOccurrence = evtPool.getNextEvent();
		// while(true){
		// try {
		// eventOccurrence = this.evtPool.poll(500, TimeUnit.MILLISECONDS);
		// } catch (InterruptedException e) {
		// e.printStackTrace();
		// }
		// if(eventOccurrence == null){
		// isWaiting = true;
		// continue;
		// }
		// else{
		// isWaiting = false;
		// System.out.println("receive " + eventOccurrence);
		// break;
		// }
		// }
		this.currentState = ObjectActivationState.RUNNING;

		if (eventOccurrence != null) {
			if (eventOccurrence instanceof SignalEventOccurrence) {
				AsyncDebug.println("[EVENT CONSUMED] occurrence for a signal:  "
						+ ((SignalEventOccurrence) eventOccurrence).signalInstance);
			}
			if (eventOccurrence instanceof InvocationEventOccurrence) {
				AsyncDebug.println("[EVENT CONSUMED] occurrence to start a classifier behavior: "
						+ ((InvocationEventOccurrence) eventOccurrence).execution);
			}
			if (eventOccurrence instanceof CallEventOccurrence) {
				if (operationBeingInvoked != null && ((CallEventOccurrence) eventOccurrence).execution.operation
						.getName().equals(operationBeingInvoked)) {
					System.out.println("~~~ process call operation event: " + operationBeingInvoked);
					operationBeingInvoked = null; // invocation is done
				}

			}
		}
		return eventOccurrence;
	}
	
	@Override
	public void dispatchNextEvent() {
		
		/* 1. Get next event is blocking if used on a empty event pool */
		EventOccurrence eventOccurrence = this.getNextEvent();
		
		AsyncDebug.println("[dispatchNextEvent] eventOccurrence = " + eventOccurrence);
		/* 2. Look for EventAccepter that match the selected SignalInstance */
		List<Integer> matchingEventAccepterIndexes = new ArrayList<Integer>();
		List<EventAccepter> waitingEventAccepters = this.waitingEventAccepters;
		for (int i = 0; i < waitingEventAccepters.size(); i++) {
			EventAccepter eventAccepter = waitingEventAccepters.get(i);
			if (eventAccepter.match(eventOccurrence)) {
				matchingEventAccepterIndexes.add(i);
			}
		}
		/* 3. Choose one matching event accepter non-deterministically */
		if (matchingEventAccepterIndexes.size() > 0) {
			int j = ((ChoiceStrategy) this.object.locus.factory.getStrategy("choice")).choose(matchingEventAccepterIndexes.size());
			EventAccepter selectedEventAccepter = this.waitingEventAccepters.get(matchingEventAccepterIndexes.get(j - 1));
			// this.waitingEventAccepters.remove(j - 1);
			this.waitingEventAccepters.remove(selectedEventAccepter);
			if (this.hasBeenWaiting) {
				this.hasBeenWaiting = false;
				if (selectedEventAccepter instanceof AcceptEventActionEventAccepter) {
					((AsyncControlDelegate) FUMLExecutionEngine.eInstance.getControlDelegate()).notifyWaitingStateExit(this, (AcceptEventActionEventAccepter) selectedEventAccepter);
				}
			}
			System.out.println("~~~ selected accepter " + selectedEventAccepter);
			selectedEventAccepter.accept(eventOccurrence);
			
		}else{
			if(eventOccurrence instanceof CallEventOccurrence){
				if(!((CallEventOccurrence)eventOccurrence).execution.operation.getName().equals("setRCFail")){
					System.out.println("~~~ event loss " + eventOccurrence);
					for (int i = 0; i < waitingEventAccepters.size(); i++) {
						EventAccepter eventAccepter = waitingEventAccepters.get(i);
						if (eventAccepter.match(eventOccurrence)) {
							matchingEventAccepterIndexes.add(i);
						}
					}
				}
			}
			AsyncDebug.printLostSignal(eventOccurrence, this, out);
		}
	}	
	
	public String getName() {
		return this.classifier.getName();
	}

}