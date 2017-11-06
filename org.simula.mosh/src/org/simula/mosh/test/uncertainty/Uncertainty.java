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

import java.util.Random;

public class Uncertainty {
	
	public String name;
	public double min;
	public double max;
	
	public Uncertainty(String name, double min, double max) {
		this.name = name;
		this.min = min;
		this.max = max;
	}
	
}