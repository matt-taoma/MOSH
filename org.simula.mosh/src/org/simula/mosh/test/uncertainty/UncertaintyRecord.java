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


package org.simula.mosh.test.uncertainty;

public class UncertaintyRecord {
	
	public double[] state;
	public double[] uncertaintyValues;
	public double value = -1;
	
	public UncertaintyRecord(double[] state, double[] uncertaintyValues) {
		this.state = state;
		this.uncertaintyValues = uncertaintyValues;
	}
	
}