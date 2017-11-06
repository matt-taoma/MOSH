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


package org.simula.mosh.test.utils;

import org.eclipse.uml2.uml.Operation;
import org.simula.mosh.model.loci.ActiveObjectActivation;

public class OperationUtil {
	
	public Operation operation;
	public ActiveObjectActivation objectActivation;
	
	public OperationUtil(Operation operation, ActiveObjectActivation objectActivation) {
		this.operation = operation;
		this.objectActivation = objectActivation;
	}
	
}