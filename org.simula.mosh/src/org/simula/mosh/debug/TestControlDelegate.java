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


package org.simula.mosh.debug;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.papyrus.moka.MokaConstants;
import org.eclipse.papyrus.moka.async.fuml.Semantics.CommonBehaviors.Communications.AsyncObjectActivation;
import org.eclipse.papyrus.moka.communication.event.isuspendresume.Suspend_Event;
import org.eclipse.papyrus.moka.communication.event.iterminate.Terminate_Event;
import org.eclipse.papyrus.moka.engine.AbstractExecutionEngine;
import org.eclipse.papyrus.moka.fuml.FUMLExecutionEngine;
import org.eclipse.papyrus.moka.fuml.Semantics.Actions.CompleteActions.AcceptEventActionEventAccepter;
import org.eclipse.papyrus.moka.fuml.Semantics.CommonBehaviors.Communications.EventAccepter;
import org.eclipse.papyrus.moka.fuml.Semantics.CommonBehaviors.Communications.ObjectActivation;
import org.eclipse.papyrus.moka.fuml.debug.FUMLThread;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.StateMachines.StateActivation;
import org.eclipse.papyrus.moka.fuml.statemachines.debug.SM_ControlDelegate;
import org.eclipse.papyrus.moka.ui.presentation.AnimationUtils;
import org.eclipse.uml2.uml.AcceptEventAction;
import org.eclipse.uml2.uml.ChangeEvent;
import org.simula.constraint.ConstraintInstance;
import org.simula.mosh.TestModelExecutionEngine;
import org.simula.mosh.model.Semantics.MoshStateActivation;
import org.simula.mosh.model.loci.ActiveObjectActivation;
import org.simula.mosh.model.loci.MoshExecutor;
import org.simula.mosh.test.log.TestLogger;

public class TestControlDelegate extends SM_ControlDelegate {

	public TestControlDelegate(AbstractExecutionEngine engine) {

		super(engine);
	}

	@Override
	public synchronized void registerObjectActivation(ObjectActivation activation, String activationName) {

		super.registerObjectActivation(activation, activationName);
	}
	
	@Override
	public boolean control(Object object) {
		
		boolean res = super.control(object);
		if(!res){
			return false;
		}
		
		if(object instanceof MoshStateActivation){
			MoshStateActivation stateActivation = (MoshStateActivation)object;
			if(stateActivation.stateInvariant != null){
				((MoshExecutor) ((FUMLExecutionEngine) this.engine).getLocus().executor).notifyStateEntered(stateActivation);
			}
		}
		
		return !this.engine.isTerminated() ;
		
	}
	
//	@Override
//	public void inactive(Object object){
//		
//		
//		if(object instanceof MoshStateActivation){
//			MoshStateActivation stateActivation = (MoshStateActivation)object;
//			if(stateActivation.stateInvariant != null){
//				((MoshExecutor) ((FUMLExecutionEngine) this.engine).getLocus().executor).notifyStateExisted(stateActivation);
//			}
//		}
//		
//		super.inactive(object);
//	}
	
	public synchronized void notifyStateExisted(MoshStateActivation stateActivation) {
		((MoshExecutor) ((FUMLExecutionEngine) this.engine).getLocus().executor).notifyStateExisted(stateActivation);
	}
	
	public synchronized void notifyFaultHandled(ChangeEvent faultEvent) {
		((MoshExecutor) ((FUMLExecutionEngine) this.engine).getLocus().executor).notifyFaultHandled(faultEvent);
	}

	@Override
	public synchronized void notifyWaitingStateEntered(AsyncObjectActivation asyncObjectActivation) {
		((MoshExecutor) ((FUMLExecutionEngine) this.engine).getLocus().executor)
				.notifyWaitingStateEntered((ActiveObjectActivation) asyncObjectActivation);

		FUMLThread thread = this.objectActivationsToFUMLThread.get(asyncObjectActivation);
		if (thread != null) {
			thread.setIsWaiting(true);
			thread.setSuspended(true);
			thread.setStackFrames(new IStackFrame[] {});
			if (MokaConstants.MOKA_AUTOMATIC_ANIMATION && this.mode.equals(ILaunchManager.DEBUG_MODE)) {
				Suspend_Event suspendEvent = new Suspend_Event(thread, DebugEvent.CHANGE, this.getThreads());
				engine.sendEvent(suspendEvent);
				List<AcceptEventAction> waitingAcceptEventActions = new ArrayList<AcceptEventAction>();
				for (EventAccepter eventAccepter : asyncObjectActivation.waitingEventAccepters) {
					if (eventAccepter instanceof AcceptEventActionEventAccepter) {
						AcceptEventAction action = (AcceptEventAction) ((AcceptEventActionEventAccepter) eventAccepter).actionActivation.node;
						waitingAcceptEventActions.add(action);
						AnimationUtils.getInstance().addAnimationMarker(action);
					}
				}
				objectActivationToWaitingAcceptEventActions.put(asyncObjectActivation, waitingAcceptEventActions);
			}
		}

	}
	
	public void notifyTransitioning(ActiveObjectActivation activation){
		((MoshExecutor) ((FUMLExecutionEngine) this.engine).getLocus().executor).notifyTransitioning((ActiveObjectActivation) activation);
	}

	@Override
	public void notifyWaitingStateExit(AsyncObjectActivation asyncObjectActivation,
			AcceptEventActionEventAccepter accepter) {

		super.notifyWaitingStateExit(asyncObjectActivation, accepter);
	}
	
	@Override
	public synchronized void notifyThreadTermination(ObjectActivation objectActivation) {
		if (this.terminateRequestByClient)
		{
			return; // do nothing
		}
		if (!FUMLExecutionEngine.eInstance.isTerminated()) {
			FUMLThread fUMLThread = this.objectActivationsToFUMLThread.get(objectActivation);
			if (fUMLThread != null) {
				fUMLThread.setIsTerminated(true);
				this.threads.remove(fUMLThread);
				((TestModelExecutionEngine)engine).objectTerminated((ActiveObjectActivation)objectActivation);
			}
			
			
//			if(this.threads.size() == 1){
//				FUMLThread thread = threads.get(0);
//				try {
//					if(thread.getName().equals("Main Thread")){
//						synchronized(this.engine){
//							this.engine.notify();
//						}
//					}
//				} catch (DebugException e) {
//					e.printStackTrace();
//				}
//			}
			
			
			Terminate_Event terminateEvent = null;
			if (this.threads.isEmpty()) {
				this.engine.setIsTerminated(true);
				synchronized (this) {
					notifyAll();
				}
				terminateEvent = new Terminate_Event(this.engine.getDebugTarget(), this.getThreads());
			} else if (fUMLThread != null) {
				terminateEvent = new Terminate_Event(fUMLThread, this.getThreads());
			}
			// else {
			// terminateEvent = new Terminate_Event(this.engine.getDebugTarget(), this.getThreads()) ;
			// }
			if (terminateEvent != null) {
				FUMLExecutionEngine.eInstance.sendEvent(terminateEvent);
			}
		}

	}
	
	public synchronized void notifyEnteringTargetStateTimeout(StateActivation targetStateActivation){
		
		System.err.println("target state (" + targetStateActivation.getNode().getName() + ") not entered");
		
		TestLogger.log("target state (" + targetStateActivation.getNode().getName() + ") not entered");
		
		this.engine.setIsTerminated(true);
		synchronized (this) {
			notifyAll();
		}
	}
	
	public synchronized void notifyCriticalError(String des){
		
		
		System.err.println("critical error happens: " + des);
		
		TestLogger.log("critical error happens: " + des);
		
		this.engine.setIsTerminated(true);
		synchronized (this) {
			notifyAll();
		}
	}
	
	public synchronized void notifyHealingTimeout(ChangeEvent faultEvent){
		
//		Stereotype fault = faultEvent.getAppliedStereotype("healing::Fault");
		
		System.err.println("fault (" + faultEvent.getName() + ") not healed");
		
		TestLogger.log("fault (" + faultEvent.getName() + ") not healed");
		
		this.engine.setIsTerminated(true);
		synchronized (this) {
			notifyAll();
		}
	}

	public synchronized void notifySafeConditionViolated(ConstraintInstance safeCondition){
		
//		Stereotype fault = faultEvent.getAppliedStereotype("healing::Fault");
		
		System.err.println("constraint (" + safeCondition + ") is violated");
		
		TestLogger.log("constraint (" + safeCondition + ") is violated");
		
		this.engine.setIsTerminated(true);
		synchronized (this) {
			notifyAll();
		}
	}
	
}