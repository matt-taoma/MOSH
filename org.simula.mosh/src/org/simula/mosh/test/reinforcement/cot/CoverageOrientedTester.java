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


package org.simula.mosh.test.reinforcement.cot;

import java.util.ArrayList;
import java.util.List;

import org.simula.mosh.test.invocation.Invocation;
import org.simula.mosh.test.reinforcement.qlearning.MDPTransition;
import org.simula.mosh.test.reinforcement.qlearning.QLearningTester;

public class CoverageOrientedTester extends QLearningTester{
	
	/**
	 * coverage oriented explore
	 * @param want2stop
	 * @return
	 */
	@Override
	protected Invocation explore(boolean want2stop){
		
		MDPTransition bestTransition = null;
		
		double highestReward = -9999999;
		List<MDPTransition> bestTransitions = new ArrayList<MDPTransition>();
		
		for(MDPTransition transition : mdp.getCurrentState().outgoingTransitions){
			
			if(transition.isNullTransition()){
				continue;
			}
			
			double reward = transition.getCoverage();
			if(reward > highestReward){
				highestReward = reward;
				bestTransitions = new ArrayList<MDPTransition>();
				bestTransitions.add(transition);
			}
			else if(reward == highestReward){
				bestTransitions.add(transition);
			}
			
//			if(want2stop){
//				if(transition.getOperationName().equals("mode") || 
//						transition.getOperationName().equals("stop")){
//					bestTransition = transition;
//				}
//			}
			
			if(transition.getOperationName().equals("stop")){
				bestTransition = transition;
			}
			
		}
		
		if(bestTransition == null){
			bestTransition = bestTransitions.get(random.nextInt(bestTransitions.size()));
		}
		
		mdp.invoke(bestTransition);
		
		recordTransition(bestTransition);
		
		return bestTransition.getInvocation();
		
	}
	
}