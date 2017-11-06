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


package org.simula.mosh.model.Semantics;

import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.FeatureValue;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.Object_;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.Value;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.StateMachines.StateMachineExecution;
import org.eclipse.uml2.uml.Class;

public class MoshStateMachineExecution extends StateMachineExecution{
	
	public MoshStateMachineExecution(){
		super();
	}
	
	@Override
	public void execute() {
		
		for(FeatureValue feature : context.featureValues){
			if(feature.feature instanceof Class){
				for(Value object : feature.values){
					((Object_)object).startBehavior((Class)feature.feature, null);
				}
			}
		}
		
		super.execute();
		
	}
	
}