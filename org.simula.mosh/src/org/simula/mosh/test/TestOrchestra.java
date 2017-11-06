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

import java.io.IOException;

import org.eclipse.papyrus.moka.fuml.FUMLExecutionEngine;
import org.simula.mosh.debug.TestControlDelegate;
import org.simula.mosh.test.log.TestLogger;
import org.simula.mosh.test.sut.SUTConnector.MsgType;
import org.simula.mosh.test.sut.SUTProxy;
import org.simula.mosh.test.uncertainty.UncertaintyIntroducer;

public class TestOrchestra {

	private TestDriver driver;
	private ConstraintChecker checker;
	private FeatureValueMonitor monitor;
	private UncertaintyIntroducer introducer;

	public TestOrchestra(TestDriver driver, FeatureValueMonitor monitor, ConstraintChecker checker,
			UncertaintyIntroducer introducer) {
		this.driver = driver;
		this.monitor = monitor;
		this.checker = checker;
		this.introducer = introducer;
	}

	private boolean synchronizeWithSUT() {

		int fails = 0;

		while (true) {
			boolean ret = SUTProxy.instance().send("127.0.0.1", "10008", "PROCEED"); // start
			if (!ret) {
				
				if(driver.areAllObjectsTerminated()){
					return false;
				}
				
				fails++;
				if (fails > 3600) { // half hour
					((TestControlDelegate) FUMLExecutionEngine.eInstance.getControlDelegate())
							.notifyCriticalError("connect failed ");
					return false;
				}

				System.err.println("send proceed failed");
				try {
					Thread.sleep(500);
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
				continue;
			}

			String ack = SUTProxy.instance().receive("127.0.0.1", "10008", MsgType.MSG_TYPE_COMPLETE);
			
			if (ack == null || ack.compareToIgnoreCase("COMPLETE") != 0) {
				System.err.println("do not receive complete " + ack);
				TestLogger.log("!!! do not receive complete\n");
				return false;
			}
			else {
				break;
			}
		}
		return true;
	}
	
	int uFlag = 0;
	
	private void introduceUncertainties() {
		
		// TODO
		if(uFlag > 100){
			String uncertaintyValues = introducer.getNextUncertaintyValues(driver.getStateVariableValues());
			SUTProxy.instance().send("127.0.0.1", "10008", "SET UNCERTAINTIES " + uncertaintyValues + "$");
			uFlag = 0;
		}
		uFlag++;
	}
	
	private void updateStateVariableValues() {
		
		boolean ret = monitor.update();
		if (!ret) {
			System.err.println("monitor.update() failed");
			TestLogger.log("!!! monitor.update() failed\n");
		}

		driver.notifyAllObjectActivations(); 
		
	}
	
	private void triggerChangeEvents() {
		
		monitor.evaluateChangeEvents();
		driver.waitUntilAllObjectsStable();
	}
	
	private double checkInvariants(boolean newTriggerGenerated) {
		
		return checker.checkInvariants(newTriggerGenerated);
	}
	
	public void run() throws IOException, ClassNotFoundException {

		Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

		double fragility = 0;

		double highestFragility = 0;

		while (!FUMLExecutionEngine.eInstance.isTerminated() && !driver.areAllObjectsTerminated()) {

			driver.waitUntilAllObjectsStable();
			boolean newTriggerGenerated = driver.nextEpoch(fragility);
			
			introduceUncertainties();
			
			if(!synchronizeWithSUT()){
				try {
					Thread.sleep(500);
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
				continue;
			}
			
			updateStateVariableValues();

			triggerChangeEvents();
			
			double distance2Fail = checkInvariants(newTriggerGenerated);
			if(distance2Fail > 0){
				fragility = 1 - distance2Fail;
			}

			if (highestFragility < fragility) {
				highestFragility = fragility;
			}

			// System.out.println("driver update: " + newTriggerGenerated);

			// if(newTriggerGenerated){
			// if(lastTime == 0){
			// lastTime = System.currentTimeMillis();
			// }
			// else{
			// long t = System.currentTimeMillis();
			// System.err.println("~~time : " + (t - lastTime));
			// lastTime = t;
			// }
			// }

			Thread.yield();
		}

		introducer.episodeFinish(highestFragility);

		// TestLogger.logTestResult(checker.minimumDistances,
		// driver.getMDP().getStateCoverage(),
		// driver.getMDP().getTransitionCoverage());

		TestLogger.logTestResult(checker.minimumDistances, "", "");

		TestLogger.log(driver.toString());

	}

	// private long lastTime = 0;

}