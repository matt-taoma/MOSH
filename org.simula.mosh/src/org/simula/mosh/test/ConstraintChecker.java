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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.papyrus.moka.fuml.FUMLExecutionEngine;
import org.simula.constraint.ConstraintInstance;
import org.simula.mosh.debug.TestControlDelegate;
import org.simula.mosh.model.loci.MoshExecutor;

public class ConstraintChecker {

	public ConstraintInstance violatedConstraint = null;
	
	public Map<ConstraintInstance, Double> minimumDistances;
	
	private double minimumSafeDistance = 1;

	private List<ConstraintInstance> constraints;

	private MoshExecutor executor;

	public ConstraintChecker(MoshExecutor executor) {
		this.executor = executor;
		
		minimumDistances = new HashMap<ConstraintInstance, Double>();
		constraints = new ArrayList<ConstraintInstance>();
	}
	
	public void init(){
		constraints.clear();
		minimumDistances.clear();
		minimumSafeDistance = 1;
		
		violatedConstraint = null;
	}
	
	public synchronized void addConstraint(ConstraintInstance constraint) {
		constraints.add(constraint);
		System.out.println("add constraint " + constraint);
	}

	public synchronized void removeConstraint(ConstraintInstance constraint) {
		constraints.remove(constraint);
		System.out.println("remove constraint " + constraint);
	}

	/**
	 * 
	 * @param newTriggerGenerated
	 * @return distance to fail
	 */
	public double checkInvariants(boolean newTriggerGenerated) {


		if (newTriggerGenerated) {
			minimumSafeDistance = 1;
		}

		for (ConstraintInstance constraint : constraints) {
			if (FUMLExecutionEngine.eInstance.isTerminated()) {
				return -1;
			}

			if (newTriggerGenerated) {
				constraint.violatedTimes = 0;
			}

			if (constraint.isSafeCondition()) {
				double d = constraint.getSafeDistance();
				Double record = minimumDistances.get(constraint);
				if(record == null){
					minimumDistances.put(constraint, d);
				}
				else{
					if(record > d){
						minimumDistances.put(constraint, d);
					}
				}
				
				if (d == 0) {
					System.err.println("crucial error detected: " + constraint);
					((TestControlDelegate) FUMLExecutionEngine.eInstance.getControlDelegate()).notifySafeConditionViolated(constraint);
					return 0;
				}
				else if (d < minimumSafeDistance) {
					minimumSafeDistance = d;
				}
			}
			else {
				if (!constraint.evaluate()) {
					constraint.violatedTimes++;
					if (constraint.violatedTimes > 800) {
						violatedConstraint = constraint;
//						System.err.println("！！！！！！！！！！！！！！！！！！！                    error detected: " + constraint);
						return -1;
					}
				}
				else {
//					if (minimumSafeDistance > 0.9) {
//						minimumSafeDistance = 0.9;
//					}
					constraint.violatedTimes = 0;
				}
			}

			// double d = constraint.getSafeDistance();
			// if(d == 0){
			// System.out.println(constraint + " violated");
			// constraint.violatedTimes++;
			// if(constraint.violatedTimes > 2400){
			// violatedConstraint = constraint;
			// return false;
			// }
			// }
			//
			// if(d != 0 && d < minimiumSafeDistance){
			// minimiumSafeDistance = d;
			// }

		}
		return minimumSafeDistance;
	}

}