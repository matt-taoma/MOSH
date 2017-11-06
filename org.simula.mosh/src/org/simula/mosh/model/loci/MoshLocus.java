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

import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.Object_;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.Loci.SM_Locus;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.Class;
import org.simula.mosh.model.Semantics.ActiveObject;
import org.simula.mosh.model.actions.ShellTask;

public class MoshLocus extends SM_Locus{
	
	private List<ShellTask> shellTasks = new ArrayList<ShellTask>();
	
	@Override
	public Object_ instantiate(Class type) {
		// Instantiate the given class at this locus.
		Object_ object = null;
		if (type instanceof Behavior) {
			object = this.factory.createExecution((Behavior) type, null);
		} else {
			object = new ActiveObject();
			object.locus = this;
			object.types.add(type);
			object.createFeatureValues();
			
			((ActiveObject)object).createOperationExecutions();
			this.add(object);
		}
		return object;
	}
	
	public void addShellTask(ShellTask task){
		shellTasks.add(task);
	}
	
	public void stopShellTasks(){
		for(ShellTask task : shellTasks){
			task.stop();
		}
		shellTasks.clear();
	}
	
}