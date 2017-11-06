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


package org.simula.constraint.node.operationnode;

import org.simula.constraint.node.ConstraintNode;

public class LessOpNode extends OperationNode {
	
	public static String symbol = "<";
	
	public LessOpNode(ConstraintNode sourceNode, ConstraintNode argumentNode) {
		super(sourceNode, argumentNode);
	}

	@Override
	public Object getValue() {
		Object sourceValue = sourceNode.getValue();
		Object argumentValue = argumentNode.getValue();
		if(sourceValue instanceof Integer && argumentValue instanceof Integer){
			return (Integer)sourceValue < (Integer)argumentValue;
		}
		else if(sourceValue instanceof Double && argumentValue instanceof Integer){
			return (Double)sourceValue - (Integer)argumentValue < -0.0000000001;
		}
		else{
			return (Double)sourceValue - (Double)argumentValue < -0.0000000001;
		}
	}
	
	@Override
	public double getSafeDistance(){
		double r1 = sourceNode.getRange();
		double r2 = argumentNode.getRange();
		double r = (r1 != 0) ? r1 : r2;
		
		Object sourceValue = sourceNode.getValue();
		Object argumentValue = argumentNode.getValue();
		if(sourceValue instanceof Integer && argumentValue instanceof Integer){
			if( (Integer)sourceValue < (Integer)argumentValue){
				double d = (Integer)argumentValue - (Integer)sourceValue;
				if(r == 0){
					return d / (d + 1);
				}
				else{
					return d / r;
				}
				
			}
			else{
				return 0;
			}
		}
		else if(sourceValue instanceof Double && argumentValue instanceof Integer){
			if( (Double)sourceValue - (Integer)argumentValue < 0.0000000001){
				double d = (Integer)argumentValue - (Double)sourceValue;
				if(r == 0){
					return d / (d + 1);
				}
				else{
					return d / r;
				}
			}
			else{
				return 0;
			}
		}
		else{
			if( (Double)sourceValue - (Double)argumentValue < 0.0000000001){
				double d = (Double)argumentValue - (Double)sourceValue;
				if(r == 0){
					return d / (d + 1);
				}
				else{
					return d / r;
				}
			}
			else{
				return 0;
			}
		}
	}
	
	@Override
	public double getViolationDistance(){
		
		Object sourceValue = sourceNode.getValue();
		Object argumentValue = argumentNode.getValue();
		
		if(sourceValue instanceof Integer && argumentValue instanceof Integer){
			
			if( (Integer)sourceValue < (Integer)argumentValue){
				return 0;
			}
			else{
				double d = (Integer)sourceValue - (Integer)argumentValue;
				return 2 - 1.0 / (d + 1.0);
			}
		}
		else{
			
			double src, arg;
			
			if(sourceValue instanceof Double){
				src = (Double)sourceValue;
			}
			else{
				src = (Integer)sourceValue;
			}
			
			if(argumentValue instanceof Double){
				arg = (Double)argumentValue;
			}
			else{
				arg = (Integer)argumentValue;
			}
			
			if(src < arg){
				return 0;
			}
			else{
				double dis = src - arg;
				return 2 - 1 / (dis + 1);
			}
		}
	}
	
}