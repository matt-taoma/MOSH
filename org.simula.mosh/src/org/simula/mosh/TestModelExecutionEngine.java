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


package org.simula.mosh;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.papyrus.moka.async.fuml.FUMLAsyncExecutionEngine;
import org.eclipse.papyrus.moka.async.fuml.debug.AsyncControlDelegate;
import org.eclipse.papyrus.moka.debug.MokaDebugTarget;
import org.eclipse.papyrus.moka.fuml.FUMLExecutionEngine;
import org.eclipse.papyrus.moka.fuml.debug.ControlDelegate;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.Class;
import org.simula.mosh.debug.TestControlDelegate;
import org.simula.mosh.model.actions.ShellActionActivation;
import org.simula.mosh.model.loci.ActiveObjectActivation;
import org.simula.mosh.model.loci.MoshExecutionFactory;
import org.simula.mosh.model.loci.MoshExecutor;
import org.simula.mosh.model.loci.MoshLocus;
import org.simula.mosh.test.log.TestLogger;
import org.simula.mosh.test.sut.SUTProxy;

public class TestModelExecutionEngine extends FUMLAsyncExecutionEngine {
	
	@Override
	public void init(EObject eObjectToExecute, String[] args, MokaDebugTarget debugTarget, int requestPort, int replyPort, int eventPort) throws UnknownHostException, IOException {
		super.init(eObjectToExecute, args, debugTarget, requestPort, replyPort, eventPort);
		
		int ins = 0;
		
		for(int i = 0; i < args.length; i ++){
			if(args[i].startsWith("-i")){
				ins = Integer.parseInt(args[i].substring(2).trim());
			}
		}
		
		SUTProxy.instance().init(Integer.toString(20000 + 50 * ins), Integer.toString(10000 + 5760 + 10 * ins));
		ShellActionActivation.ins = ins;
//		TestLogger.disFile += ins + ".txt";
//		TestLogger.logFile += ins + ".txt";
//		TestLogger.disFile += ".txt";
//		TestLogger.logFile += ".txt";
		
		TestLogger.log("instance num: " + ins);
		
	}
	
	// Starts the execution of the given behavior
	@Override
	public void start(Behavior behavior) {
		if (behavior != null) {

			main = behavior;

			// creates the locus, executor and execution factory
			this.locus = new MoshLocus();
			locus.setExecutor(new MoshExecutor());
			locus.setFactory(new MoshExecutionFactory());

			// initializes built-in primitive types
			this.initializeBuiltInPrimitiveTypes(locus);

			// Initializes opaque behavior executions
			this.registerOpaqueBehaviorExecutions(locus);

			// Initializes semantic strategies
			this.registerSemanticStrategies(locus);

			// Initializes system services
			this.registerSystemServices(locus);

			// Initializes arguments
			this.initializeArguments(this.args);

			// Finally launches the execution
			this.started = true;

			TestLogger.init();

			ArrayList<Double> dis = new ArrayList<Double>();
			ArrayList<String> uncertain = new ArrayList<String>();
			
			long beginTime = System.currentTimeMillis();
			
			for (int i = 0; i < 10000 && !FUMLExecutionEngine.eInstance.isTerminated(); i++) {
				
				if(this.isTerminated){
					break;
				}
				
				TestLogger.beginNextTestTrace();

				locus.executor.start((Class) main.getContext(), arguments);

				// uncertain.add(Uncertainty.instance().getLastUncertaintyValues());

				// if(((MoshExecutor)locus.executor).getTestDriver().isDone()){
				// break;
				// }

				try {
					Thread.sleep(3000);
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				System.out.println("\n\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~  test once  \n\n");
				
				long time = System.currentTimeMillis();
				time = time - beginTime;
				
				long mins = time / (60 * 1000); 
				long hours = time / (60 * 60 * 1000); // in hours
				
				TestLogger.logTime(mins);
				
				if( hours > 24){
					break;
				}
				
				
				
			}
			
			((MoshExecutor) locus.executor).clean();
			
			System.out.println("test completes");
//			for (int i = 0; i < dis.size(); i++) {
//				System.out.println(uncertain.get(i) + "   --->    " + dis.get(i));
//			}

			((AsyncControlDelegate) eInstance.getControlDelegate()).notifyMainThreadLogicallyEnded();
		}
	}

	@Override
	public ControlDelegate getControlDelegate() {
		if (this.controlDelegate == null) {
			this.controlDelegate = new TestControlDelegate(this);
		}
		return this.controlDelegate;
	}

	public void objectTerminated(ActiveObjectActivation objectActivation) {
		((MoshExecutor) locus.executor).getTestDriver().notifyObjectTerminated(objectActivation);
	}
}