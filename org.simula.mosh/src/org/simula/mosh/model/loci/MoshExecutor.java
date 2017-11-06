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

import java.io.IOException;
import java.util.List;

import org.eclipse.papyrus.moka.composites.Semantics.CompositeStructures.StructuredClasses.CS_Object;
import org.eclipse.papyrus.moka.composites.Semantics.CompositeStructures.StructuredClasses.CS_Reference;
import org.eclipse.papyrus.moka.composites.Semantics.Loci.LociL3.CS_Executor;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.Object_;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.Reference;
import org.eclipse.papyrus.moka.fuml.Semantics.CommonBehaviors.BasicBehaviors.ParameterValue;
import org.eclipse.uml2.uml.ChangeEvent;
import org.eclipse.uml2.uml.Class;
import org.simula.mosh.model.Semantics.MoshStateActivation;
import org.simula.mosh.test.ConstraintChecker;
import org.simula.mosh.test.FeatureValueMonitor;
import org.simula.mosh.test.TestDriver;
import org.simula.mosh.test.TestOrchestra;
import org.simula.mosh.test.sut.SUTProxy;
import org.simula.mosh.test.uncertainty.RandomUncertaintyIntroducer;
import org.simula.mosh.test.uncertainty.UncertaintyIntroducer;

public class MoshExecutor extends CS_Executor {
	
	private TestDriver driver;
	private ConstraintChecker checker;
	private FeatureValueMonitor monitor;
	private UncertaintyIntroducer introducer;
	private TestOrchestra ochestra;
	
	public void clean() {
		
		String host = "127.0.0.1";
		String port = "20008";
		String cmd = "exit";
		SUTProxy.instance().send(host, port, cmd);
		
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		host = "127.0.0.1";
		port = "10008";
		cmd = "stop";
		SUTProxy.instance().send(host, port, cmd);
		
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
	
	@Override
	public Reference start(Class type, List<ParameterValue> inputs) {
		// Instantiate the given class and start any behavior of the resulting
		// object.
		// (The behavior of an object includes any classifier behaviors for an
		// active object or the class of the object itself, if that is a
		// behavior.)
		// fUML semantics is extended in the sense that when the instantiated object
		// is a CS_Object, a CS_Reference is returned (instead of a Reference)
		
		locus.extensionalValues.clear();
		
		Object_ object = this.locus.instantiate(type);
		object.startBehavior(type, inputs);
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if(driver == null){
			driver = new TestDriver(locus.extensionalValues);
			checker = new ConstraintChecker(this);
			monitor = new FeatureValueMonitor(locus.extensionalValues, this);
			introducer = new RandomUncertaintyIntroducer(locus.extensionalValues);//new RLUncertaintyIntroducer(locus.extensionalValues);
			ochestra = new TestOrchestra(driver, monitor, checker, introducer);
		}
		else{
			driver.preEpisode(locus.extensionalValues);
			monitor.init(locus.extensionalValues);
			checker.init();
		}
		
		try {
			ochestra.run();
			
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		((MoshLocus)this.locus).stopShellTasks();
		
		Reference reference;
		if (object instanceof CS_Object) {
			reference = new CS_Reference();
			((CS_Reference) reference).compositeReferent = (CS_Object) object;
		} else {
			reference = new Reference();
		}
		reference.referent = object;

		return reference;
	}
	
	public void notifyWaitingStateEntered(ActiveObjectActivation objectActivation){
		
		// driver has not been initialized, wait..
		while(driver == null || monitor == null){
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		monitor.registerChangeEvent(objectActivation);
		driver.notifyObjectIsWaiting(objectActivation);
	}
	
	public void notifyTransitioning(ActiveObjectActivation activation){
		while(driver == null){
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		synchronized(driver){
			driver.notify();
		}
	}
	
	public void notifyStateEntered(MoshStateActivation stateActivation){
		checker.addConstraint(stateActivation.stateInvariant);
	}
	
	public void notifyStateExisted(MoshStateActivation stateActivation){
		checker.removeConstraint(stateActivation.stateInvariant);
	}
	
	public void notifyFaultHandled(ChangeEvent faultEvent){
		driver.notifyFaultHandled(faultEvent);
	}
	
	public TestDriver getTestDriver(){
		return this.driver;
	}
	
}