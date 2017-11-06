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

public class IfNode extends OperationNode {
	
	private ConstraintNode elseNode;
	
	public IfNode(ConstraintNode conditionNode, ConstraintNode thenNode, ConstraintNode elseNode){
		super(conditionNode, thenNode);
		this.sourceNode = conditionNode;
		this.argumentNode = thenNode;
		this.elseNode = elseNode;
	}
	
	@Override
	public Object getValue() {
		if((Boolean)sourceNode.getValue()){
			return this.argumentNode.getValue();
		}
		else{
			return this.elseNode.getValue();
		}
	}
	
	@Override
	public double getSafeDistance(){
		double dt1 = sourceNode.getSafeDistance();
		double dt2 = argumentNode.getSafeDistance();
		double dt3 = elseNode.getSafeDistance();
		
		double d1 = (1 - dt1) * dt3;
		double d2 = dt1 * dt2;
		
		return (d1 < d2) ? d1 : d2;
	}
	
	@Override
	public double getViolationDistance(){
		double d1 = sourceNode.getViolationDistance();
		double d2 = argumentNode.getViolationDistance();
		double d3 = elseNode.getViolationDistance();
		
		if(d1 == 0 && d2 == 0){
			return 0;
		}
		else if(d1 != 0 && d3 == 0){
			return 0;
		}
		else{
			double dt1 = 1 - 1 / (d1 + d2 + 1);
			double dt2 = 1 - 1 / (1 - d1 + d3 + 1);
			
			return (dt1 < dt2) ? dt1 : dt2;
		}
	}
	
}