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

import org.eclipse.uml2.uml.EnumerationLiteral;
import org.simula.constraint.node.ConstraintNode;

public class NotEqualOpNode extends OperationNode {
	
	public static String symbol = "<>";
	
	public NotEqualOpNode(ConstraintNode sourceNode, ConstraintNode argumentNode) {
		super(sourceNode, argumentNode);
	}

	@Override
	public Object getValue() {
		Object sourceValue = sourceNode.getValue();
		Object argumentValue = argumentNode.getValue();
		
		if(sourceValue instanceof EnumerationLiteral){
			return sourceValue != argumentValue;
		}
		else if(sourceValue instanceof Boolean){
			return sourceValue != argumentValue;
		}
		if(sourceValue instanceof Integer && argumentValue instanceof Integer){
			return (Integer)sourceValue != (Integer)argumentValue;
		}
		else if(sourceValue instanceof Double && argumentValue instanceof Integer){
			return Math.abs((Double)sourceValue - ((Integer)argumentValue + 0.0)) > 0.0000000001 ;
		}
		else{
			return Math.abs((Double)sourceValue - (Double)argumentValue) > 0.0000000001;
		}
	}
	
	@Override
	public double getSafeDistance(){
		Object sourceValue = sourceNode.getValue();
		Object argumentValue = argumentNode.getValue();
		
		if(sourceValue instanceof EnumerationLiteral){
			if( sourceValue != argumentValue){
				return 1;
			}
			else{
				return 0;
			}
		}
		else if(sourceValue instanceof Boolean){
			if( sourceValue != argumentValue){
				return 1;
			}
			else{
				return 0;
			}
		}
		if(sourceValue instanceof Integer && argumentValue instanceof Integer){
			if( (Integer)sourceValue != (Integer)argumentValue){
				double d = (Integer)sourceValue - (Integer)argumentValue;
				return d / (d + 1);
			}
			else{
				return 0;
			}
		}
		else if(sourceValue instanceof Double && argumentValue instanceof Integer){
			double d = Math.abs((Double)sourceValue - ((Integer)argumentValue + 0.0));
			if(d > 0.0000000001){
				return d / (d + 1);
			}
			else{
				return 0;
			}
		}
		else{
			double d = Math.abs((Double)sourceValue - ((Double)argumentValue + 0.0));
			if(d > 0.0000000001){
				return d / (d + 1);
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
		
		if(sourceValue instanceof EnumerationLiteral){
			if(!sourceValue.equals(argumentValue)){
				return 0;
			}
			else{
				return 0.5;
			}
		}
		else if(sourceValue instanceof Boolean){
			if(!sourceValue.equals(argumentValue)){
				return 0;
			}
			else{
				return 0.5;
			}
		}
		else if(sourceValue instanceof String && argumentValue instanceof String){
			if(!sourceValue.equals(argumentValue)){
				return 0;
			}
			else{
				return 0.5;
			}
		}
		else if(sourceValue instanceof Integer && argumentValue instanceof Integer){
			if(!sourceValue.equals(argumentValue)){
				return 0;
			}
			else{
				return 0.5;
			}
		}
		else {
			
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
			
			
			double dis = Math.abs(src - arg);
			
			if( dis < 0.0000000001){
				return 0.5;
			}
			else{
				return 0;
			}
		}
	}
	
}