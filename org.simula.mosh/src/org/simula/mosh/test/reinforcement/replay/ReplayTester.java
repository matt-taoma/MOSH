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


package org.simula.mosh.test.reinforcement.replay;

import org.simula.mosh.test.invocation.Invocation;
import org.simula.mosh.test.log.TestLogger;
import org.simula.mosh.test.reinforcement.qlearning.MDPTransition;
import org.simula.mosh.test.reinforcement.qlearning.QLearningTester;

public class ReplayTester extends QLearningTester{
	
	private String[] replayList = {
			"start", "arm", "throttle", "throttle", "mode()", 
			"pitch()[pit < 1400 and pit > 1000]",
			"setThreat()",
			"pitch()[pit < 1400 and pit > 1000]",
			"mode()", "stop"
	};
	
	/**
	 * replay a path
	 * @return
	 */
	@Override
	protected Invocation explore(boolean want2stop){
		
		String operationName = replayList[TestLogger.logIndex];
		
		MDPTransition bestTransition = null;
		
		for(MDPTransition transition : mdp.getCurrentState().outgoingTransitions){
			
			if(!transition.isNullTransition()){
				if(transition.getInvocation().toString().indexOf(operationName) >= 0){
					bestTransition = transition;
					break;
				}
			}
		}
		
		if(bestTransition == null){
			System.err.println("bestTransition");
		}
		
		mdp.invoke(bestTransition);
		
		recordTransition(bestTransition);
		
		return bestTransition.getInvocation();
		
	}
	
}