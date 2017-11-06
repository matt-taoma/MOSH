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

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.eclipse.papyrus.moka.async.fuml.Semantics.CommonBehaviors.Communications.AsyncObjectActivation.ObjectActivationState;
import org.eclipse.papyrus.moka.fuml.FUMLExecutionEngine;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.ExtensionalValue;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.StateMachines.TransitionActivation.TransitionMetadata;
import org.eclipse.uml2.uml.ChangeEvent;
import org.simula.mosh.debug.TestControlDelegate;
import org.simula.mosh.model.loci.ActiveObjectActivation;
import org.simula.mosh.test.invocation.Invocation;
import org.simula.mosh.test.log.TestLogger;
import org.simula.mosh.test.reinforcement.ReinforcementTester;
import org.simula.mosh.test.reinforcement.qlearning.QLearningTester;
import org.simula.mosh.test.sut.SUTObject;
import org.simula.mosh.test.sut.SUTProxy;

public class TestDriver{

	private boolean testDone = false;

	private ReinforcementTester tester;
	private SUTObject sut;
	
	private Invocation lastInvocation = null;
	private boolean faultHandled = false;
	private Instant faultInjectedTime = null;
	
	private int waitTimes = 0;
	
	public TestDriver(List<ExtensionalValue> extensionalValues) {
		
		sut = new SUTObject();
		
		tester = new QLearningTester();
		
		preEpisode(extensionalValues);
	}
	
	public synchronized void preEpisode(List<ExtensionalValue> extensionalValues){
		
		
		waitUntilAllObjectsStable();
		
		sut.init(extensionalValues);
		
		tester.episodeStart(sut);
		
		waitTimes = 0;
		lastInvocation = null;
		faultHandled = false;
		
		SUTProxy.instance().reset();
	}
	
	public boolean nextEpoch(double fragility) {
		
		if(areAllObjectsTerminated()){
			return false;
		}
		
		if(!isLastInvocationExecuted()) {
			return false;
		}
		
		if(!isFaultHealed()) {
			return false;
		}
		
		if(!areAllObjectsWaiting()) {
			return false;
		}
		
		if(waitTimes == 0){
			triggerNextTransition(fragility);
			waitTimes = 5000;
			return true;
		}
		else{
			waitTimes--;
			return false;
		}
		
	}
	
	private void triggerNextTransition(double fragility) {

		// try {
		// Thread.sleep(1000);
		// } catch (InterruptedException e) {
		// e.printStackTrace();
		// }
		boolean faultHasInjected = false;
		if (faultHandled) {
			faultHandled = false;
			faultInjectedTime = null;
			faultHasInjected = true;
		}

		Invocation invocation = null;
		// ~~~~~~~~   q learning
		if(TestLogger.logIndex > 30 || faultHasInjected){
			invocation = tester.nextEpoch(sut, fragility, true);
		}
		else {
			invocation = tester.nextEpoch(sut, fragility, false);
		}
		
		if(invocation == null){
			System.err.println("do not return invocation");
		}
		else{
			lastInvocation = invocation;
		}
		
		// ~~~~~~~~   coverage
//		if(TestLogger.logIndex > 30){
//			faultInjection = qlearning.coverageExplore(true);
//		}
//		else {
//			faultInjection = qlearning.coverageExplore(false);
//		}
		
		
		// ~~~~~~~~ random
//		if(TestLogger.logIndex > 30){
//			faultInjection = qlearning.randomExplore(true);
//		}
//		else{
//			faultInjection = qlearning.randomExplore(false);
//		}
		
		// ~~~~~~~~~ replay
//		faultInjection = qlearning.replayExplore();
		
		
		if(lastInvocation != null && lastInvocation.isFaultInjection()){
			faultInjectedTime = Instant.now();
		}

	}
	
	private boolean areAllObjectsWaiting() {
		
		for(ActiveObjectActivation activation : sut.getActiveObjectActivations().values()){
			if(activation.operationBeingInvoked() || activation.getCurrentState() != ObjectActivationState.WAITING){
				Thread.yield();
				return false;
			}
		}
		return true;
	}
	
	private boolean isLastInvocationExecuted() {
		
		if(lastInvocation == null || lastInvocation.transitionActivation.getStatus() == TransitionMetadata.TRAVERSED){
			return true;
		}
		else{
			return false;
		}
	}
	
	private boolean isFaultHealed() {
		
		if(lastInvocation != null && lastInvocation.isFaultInjection() && (!faultHandled) ){
			
			Instant now = Instant.now();
			long nanos = Duration.between(faultInjectedTime, now).toNanos();
			long ms = nanos / 1000000;
			if(ms > 300000){ // has been waiting for 300s
				((TestControlDelegate) FUMLExecutionEngine.eInstance.getControlDelegate()).notifyHealingTimeout(lastInvocation.faultEvent);
			}
			
			// wait for the injected fault to be handled
			return false;
		}
		return true;
	}
	
	public boolean areAllObjectsTerminated(){
		return sut.areAllObjectsTerminated();
	}
	
	public int getActiveObjectNum() {
		return sut.getActiveObjectNum();
	}
	
	public double[] getStateVariableValues() {
		return sut.getStateVariableValues();
	}
	
	public synchronized void notifyObjectTerminated(ActiveObjectActivation objectActivation) {
		
		sut.objectTerminated(objectActivation);
		if(sut.areAllObjectsTerminated()){
			this.notify();
		}
	}
	
	public synchronized void notifyObjectIsWaiting(ActiveObjectActivation objectActivation) {

		System.out.println(objectActivation.getName() + " is waiting");

		if (lastInvocation == null || (!lastInvocation.isFaultInjection()) || faultHandled) {
			System.out.println("trigger~~");
			this.notify();
		}

	}
	
	public synchronized void notifyFaultHandled(ChangeEvent faultEvent) {

		if (!lastInvocation.isFaultInjection()) { // no fault has been injected
			return;
		}

		if (lastInvocation.faultEvent.equals(faultEvent)) {
			faultHandled = true;
		}
					
	}
	
	public void waitUntilAllObjectsStable(){
		
		Map<String, ActiveObjectActivation> activeObjectActivations = sut.getActiveObjectActivations();
		if(activeObjectActivations == null || activeObjectActivations.isEmpty()){
			return;
		}
		
		int stable = 0;
		
		while (!FUMLExecutionEngine.eInstance.isTerminated()) {
			
			boolean allWaiting = true;
			for(ActiveObjectActivation activation : activeObjectActivations.values()){
				if( activation.operationBeingInvoked() ||
						(activation.getCurrentState() != ObjectActivationState.WAITING && (!activation.isTraversing())) ){
					allWaiting = false;
					break;
				}
			}
			
			if(!allWaiting){
				stable ++;
				if(stable == 100){
					stable = 0;
				}
				synchronized(this){
					try {
//						System.out.println("waitUntilAllObjectsStable..");
						this.wait(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			else{
				break;
			}
			
		}
		
	}
	
	public void notifyAllObjectActivations(){
		
		for(ActiveObjectActivation activation : sut.getActiveObjectActivations().values()){
			synchronized(activation){
				activation.notify();
			}
		}
	}
	
	
	public boolean isDone() {
		return this.testDone;
	}
	
	public String toString(){
		
		return tester.toString();
		
	}
	
	
}