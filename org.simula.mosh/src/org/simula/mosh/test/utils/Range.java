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

import java.util.Random;

import org.eclipse.papyrus.moka.fuml.Semantics.CommonBehaviors.BasicBehaviors.ParameterValue;
import org.eclipse.uml2.uml.Parameter;

public abstract class Range {
	
	public abstract void setEqualValue(String value);
	public abstract void setInequalValue(String value);
	public abstract void setMinValue(String value);
	public abstract void setMaxValue(String value);
	public abstract ParameterValue getValidValue(Parameter p, Random random);
	
	public abstract double getMinValue();
	public abstract double getMaxValue();
	
	public abstract boolean isSingle();
}