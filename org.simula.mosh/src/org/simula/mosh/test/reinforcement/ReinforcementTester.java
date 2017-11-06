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


package org.simula.mosh.test.reinforcement;

import org.simula.mosh.test.invocation.Invocation;
import org.simula.mosh.test.sut.SUTObject;

public abstract class ReinforcementTester {
	
	public abstract void episodeStart(SUTObject sut);
	
	public abstract Invocation nextEpoch(SUTObject sut, double reward, boolean stop);
	
	public abstract void episodeFinish();
	
	
	
}