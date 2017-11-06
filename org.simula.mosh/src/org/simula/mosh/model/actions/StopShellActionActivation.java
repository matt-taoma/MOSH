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


package org.simula.mosh.model.actions;

import org.eclipse.papyrus.moka.fuml.Semantics.Actions.BasicActions.ActionActivation;
import org.simula.mosh.model.loci.MoshLocus;

public class StopShellActionActivation extends ActionActivation{

	@Override
	public void doAction() {
		
		((MoshLocus)this.getExecutionLocus()).stopShellTasks();
		
	}

}