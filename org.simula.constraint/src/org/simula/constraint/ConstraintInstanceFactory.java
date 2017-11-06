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


package org.simula.constraint;

import java.util.List;

import org.eclipse.ocl.cst.BooleanLiteralExpCS;
import org.eclipse.ocl.cst.CallExpCS;
import org.eclipse.ocl.cst.FeatureCallExpCS;
import org.eclipse.ocl.cst.IfExpCS;
import org.eclipse.ocl.cst.IntegerLiteralExpCS;
import org.eclipse.ocl.cst.InvCS;
import org.eclipse.ocl.cst.LetExpCS;
import org.eclipse.ocl.cst.LiteralExpCS;
import org.eclipse.ocl.cst.MessageExpCS;
import org.eclipse.ocl.cst.NullLiteralExpCS;
import org.eclipse.ocl.cst.OCLExpressionCS;
import org.eclipse.ocl.cst.OperationCallExpCS;
import org.eclipse.ocl.cst.PathNameCS;
import org.eclipse.ocl.cst.PrimitiveLiteralExpCS;
import org.eclipse.ocl.cst.RealLiteralExpCS;
import org.eclipse.ocl.cst.SimpleNameCS;
import org.eclipse.ocl.cst.StringLiteralExpCS;
import org.eclipse.ocl.cst.TupleLiteralExpCS;
import org.eclipse.ocl.cst.VariableExpCS;
import org.eclipse.ocl.expressions.UnlimitedNaturalLiteralExp;
import org.eclipse.ocl.parser.OCLLexer;
import org.eclipse.ocl.parser.OCLParser;
import org.eclipse.ocl.uml.UMLEnvironment;
import org.eclipse.ocl.uml.UMLEnvironmentFactory;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.FeatureValue;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.Object_;
import org.eclipse.papyrus.moka.fuml.Semantics.CommonBehaviors.BasicBehaviors.ParameterValue;
import org.eclipse.papyrus.moka.fuml.Semantics.CommonBehaviors.Communications.EventOccurrence;
import org.eclipse.papyrus.moka.fuml.Semantics.CommonBehaviors.Communications.SignalEventOccurrence;
import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.CommonBehavior.CallEventOccurrence;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.ValueSpecification;
import org.simula.constraint.node.BooleanLiteralNode;
import org.simula.constraint.node.ConstraintNode;
import org.simula.constraint.node.EnumerationLiteralNode;
import org.simula.constraint.node.IntegerLiteralNode;
import org.simula.constraint.node.NullLiteralNode;
import org.simula.constraint.node.RealLiteralNode;
import org.simula.constraint.node.StringLiteralNode;
import org.simula.constraint.node.VariableNode;
import org.simula.constraint.node.operationnode.AbsOpNode;
import org.simula.constraint.node.operationnode.AndOpNode;
import org.simula.constraint.node.operationnode.DivideOpNode;
import org.simula.constraint.node.operationnode.EqualOpNode;
import org.simula.constraint.node.operationnode.GreaterEqualOpNode;
import org.simula.constraint.node.operationnode.GreaterOpNode;
import org.simula.constraint.node.operationnode.ImplyOpNode;
import org.simula.constraint.node.operationnode.LessEqualOpNode;
import org.simula.constraint.node.operationnode.LessOpNode;
import org.simula.constraint.node.operationnode.MinusOpNode;
import org.simula.constraint.node.operationnode.MultiplyOpNode;
import org.simula.constraint.node.operationnode.NotEqualOpNode;
import org.simula.constraint.node.operationnode.NotOpNode;
import org.simula.constraint.node.operationnode.OperationNode;
import org.simula.constraint.node.operationnode.OrOpNode;
import org.simula.constraint.node.operationnode.PlusOpNode;
import org.simula.constraint.node.operationnode.XorOpNode;

public class ConstraintInstanceFactory {

	public static ConstraintInstanceFactory instance = new ConstraintInstanceFactory();

	private OCLParser parser;
	
	private Object_ context = null;
	private Object_ childContext = null;
	private EventOccurrence eventOccurrence = null;
	private List<ParameterValue> operationParameterValues = null;
	
	private ConstraintInstanceFactory() {
		UMLEnvironmentFactory factory = new UMLEnvironmentFactory();
		UMLEnvironment env = factory.createEnvironment();

		OCLLexer lexer = new OCLLexer(env);
		parser = new OCLParser(lexer);
	}
	
	public ConstraintInstance createConstraintInstance(Constraint constraint, EventOccurrence eventOccurrence) {
		
		this.eventOccurrence = eventOccurrence;
		context = null;
		childContext = null;
		this.operationParameterValues = null;
		
		String specification = constraint.getSpecification().stringValue();
		specification = "inv : " + specification;
		
		System.out.println(specification);
		
		parser.getLexer().reset(specification.toCharArray(), "UML");
		parser.reset(parser.getLexer().getILexStream());
		parser.getLexer().lexer(parser.getIPrsStream());
		InvCS invCS = (InvCS) parser.parser();

		ConstraintNode root = oclExpressionCS(invCS.getExpressionCS());

		ConstraintInstance instance = new ConstraintInstance(constraint, root);
		return instance;
	}
	
	public BooleanValueSpecificationInstance createBooleanValueSpecificationInstance(ValueSpecification booleanSpecification, Object_ context) {
		
		this.context = context;
		this.childContext = null;
		this.eventOccurrence = null;
		this.operationParameterValues = null;
		
		String specification = booleanSpecification.stringValue();
		specification = "inv : " + specification;

		parser.getLexer().reset(specification.toCharArray(), "UML");
		parser.reset(parser.getLexer().getILexStream());
		parser.getLexer().lexer(parser.getIPrsStream());
		InvCS invCS = (InvCS) parser.parser();

		ConstraintNode root = oclExpressionCS(invCS.getExpressionCS());

		BooleanValueSpecificationInstance instance = new BooleanValueSpecificationInstance(booleanSpecification, root);
		return instance;
	}
	
	public synchronized ConstraintInstance createConstraintInstance(Constraint constraint, List<ParameterValue> values) {
		
		this.context = null;
		this.childContext = null;
		this.eventOccurrence = null;
		this.operationParameterValues = values;
		
		String specification = constraint.getSpecification().stringValue();
		specification = "inv : " + specification;

		System.out.println(specification);
		
		parser.getLexer().reset(specification.toCharArray(), "UML");
		parser.reset(parser.getLexer().getILexStream());
		parser.getLexer().lexer(parser.getIPrsStream());
		InvCS invCS = (InvCS) parser.parser();

		if(invCS == null){
			System.err.println(specification);
			return null;
		}
		ConstraintNode root = oclExpressionCS(invCS.getExpressionCS());

		ConstraintInstance instance = new ConstraintInstance(constraint, root);
		return instance;
	}
	
	public synchronized ConstraintInstance createConstraintInstance(Constraint constraint, Object_ context) {
		
		this.context = context;
		this.childContext = null;
		this.eventOccurrence = null;
		this.operationParameterValues = null;
		
		String specification = constraint.getSpecification().stringValue();
		specification = "inv : " + specification;

		System.out.println(specification);
		
		parser.getLexer().reset(specification.toCharArray(), "UML");
		parser.reset(parser.getLexer().getILexStream());
		parser.getLexer().lexer(parser.getIPrsStream());
		InvCS invCS = (InvCS) parser.parser();

		if(invCS == null){
			System.err.println(specification);
			return null;
		}
		ConstraintNode root = oclExpressionCS(invCS.getExpressionCS());

		ConstraintInstance instance = new ConstraintInstance(constraint, root);
		return instance;
	}

	private ConstraintNode oclExpressionCS(OCLExpressionCS oclExpressionCS) {
		if (oclExpressionCS instanceof IfExpCS) {
			ifExpCS((IfExpCS) oclExpressionCS);
		} else if (oclExpressionCS instanceof CallExpCS) {
			return callExpCS((CallExpCS) oclExpressionCS);
		} else if (oclExpressionCS instanceof VariableExpCS) {
			return variableExpCS((VariableExpCS) oclExpressionCS);
		} else if (oclExpressionCS instanceof LiteralExpCS) {
			return literalExpCS((LiteralExpCS) oclExpressionCS);
		} else if (oclExpressionCS instanceof LetExpCS) {
			letExp((LetExpCS) oclExpressionCS);
		} else if (oclExpressionCS instanceof MessageExpCS) {
			messageExpCS((MessageExpCS) oclExpressionCS);
		} else if (oclExpressionCS instanceof SimpleNameCS) { // SimpleNameCS is
																// the parent of
																// LiteralExpCS
			simpleNameCS((SimpleNameCS) oclExpressionCS);
		}

		return null;
	}

	private void simpleNameCS(SimpleNameCS simpleNameCS) {
		//System.out.println(simpleNameCS);
	}

	private void ifExpCS(IfExpCS ifExpCS) {
		
		OCLExpressionCS condition = ifExpCS.getCondition();
		OCLExpressionCS then = ifExpCS.getThenExpression();
		OCLExpressionCS else_ = ifExpCS.getElseExpression();

		oclExpressionCS(condition);
		oclExpressionCS(then);
		oclExpressionCS(else_);
	}

	private ConstraintNode callExpCS(CallExpCS callExpCS) {

		if (callExpCS instanceof OperationCallExpCS) { // OperationCallExpCS is
														// a subclass of
														// FeatureCallExpCS
			return operationCallExpCS((OperationCallExpCS)callExpCS);
		} else if (callExpCS instanceof FeatureCallExpCS) {
			return featureCallExpCS((FeatureCallExpCS) callExpCS);
		}

		return null;
	}

	private ConstraintNode featureCallExpCS(FeatureCallExpCS featureCallExpCS) {
		
		OCLExpressionCS source = featureCallExpCS.getSource();
		if(source != null) {  // refer a property
			if(((VariableExpCS)source).getSimpleNameCS().getValue().equals("self")){
				SimpleNameCS featurePropertyName = featureCallExpCS.getSimpleNameCS();
				return new VariableNode(getFeatureValue(featurePropertyName.getValue()));
			}
			else{
				VariableNode sourceNode = (VariableNode)oclExpressionCS(source);
				Object_ feature = (Object_)sourceNode.getValue();
				SimpleNameCS featurePropertyName = featureCallExpCS.getSimpleNameCS();
				childContext = feature;
				VariableNode result = new VariableNode(getFeatureValue(featurePropertyName.getValue()));
				childContext = null;
				return result;
			}
		}
		else{ // refer an enumeration
			PathNameCS pathName = featureCallExpCS.getPathNameCS();
			Enumeration enumeration = getEnumeration(pathName.getSimpleNames().get(0).getValue());
			SimpleNameCS literanlName = featureCallExpCS.getSimpleNameCS();
			EnumerationLiteral literal = getLiteral(literanlName.getValue(), enumeration);
			return new EnumerationLiteralNode(literal);
		}
		
	}
	
	private EnumerationLiteral getLiteral(String literalName, Enumeration enumeration){
		
		for(EnumerationLiteral literal : enumeration.getOwnedLiterals()){
			if(literal.getName().equals(literalName)){
				return literal;
			}
		}
		return null;
	}
	
	private Enumeration getEnumeration(String enumerationName){
		if (context != null) {
			for (FeatureValue featureValue : context.featureValues) {
				Type type = featureValue.feature.getType();
				if (type!= null && type instanceof Enumeration) {
					if(((Enumeration)type).getName().equals(enumerationName))
					return (Enumeration)type;
				}
			}
		}
		else{
			if(eventOccurrence instanceof CallEventOccurrence){
				CallEventOccurrence callEvent = (CallEventOccurrence)eventOccurrence;
				for (FeatureValue featureValue : callEvent.execution.context.featureValues) {
					Type type = featureValue.feature.getType();
					if (type!= null && type instanceof Enumeration) {
						if(((Enumeration)type).getName().equals(enumerationName))
						return (Enumeration)type;
					}
				}
			}
		}
		
		return null;
		
	}
	
	private ConstraintNode operationCallExpCS(OperationCallExpCS operationCallExpCS){
		
		ConstraintNode sourceNode = oclExpressionCS(operationCallExpCS.getSource());
		ConstraintNode argumentNode = null;
		if(!operationCallExpCS.getArguments().isEmpty()){
			argumentNode = oclExpressionCS(operationCallExpCS.getArguments().get(0));
		}
		
		String operationName = operationCallExpCS.getSimpleNameCS().getValue();
		OperationNode opNode = null;
		if(operationName.equals(AbsOpNode.symbol)){
			opNode = new AbsOpNode(sourceNode, argumentNode);
		}
		else if(operationName.equals(AndOpNode.symbol)){
			opNode = new AndOpNode(sourceNode, argumentNode);
		}
		else if(operationName.equals(OrOpNode.symbol)){
			opNode = new OrOpNode(sourceNode, argumentNode);
		}
		else if(operationName.equals(XorOpNode.symbol)){
			opNode = new XorOpNode(sourceNode, argumentNode);
		}
		else if(operationName.equals(NotOpNode.symbol)){
			opNode = new NotOpNode(sourceNode, argumentNode);
		}
		else if(operationName.equals(ImplyOpNode.symbol)){
			opNode = new ImplyOpNode(sourceNode, argumentNode);
		}
		else if(operationName.equals(EqualOpNode.symbol)){
			opNode = new EqualOpNode(sourceNode, argumentNode);
		}
		else if(operationName.equals(GreaterEqualOpNode.symbol)){
			opNode = new GreaterEqualOpNode(sourceNode, argumentNode);
		}
		else if(operationName.equals(GreaterOpNode.symbol)){
			opNode = new GreaterOpNode(sourceNode, argumentNode);
		}
		else if(operationName.equals(LessEqualOpNode.symbol)){
			opNode = new LessEqualOpNode(sourceNode, argumentNode);
		}
		else if(operationName.equals(LessOpNode.symbol)){
			opNode = new LessOpNode(sourceNode, argumentNode);
		}
		else if(operationName.equals(NotEqualOpNode.symbol)){
			opNode = new NotEqualOpNode(sourceNode, argumentNode);
		}
		else if(operationName.equals(PlusOpNode.symbol)){
			opNode = new PlusOpNode(sourceNode, argumentNode);
		}
		else if(operationName.equals(MinusOpNode.symbol)){
			opNode = new MinusOpNode(sourceNode, argumentNode);
		}
		else if(operationName.equals(MultiplyOpNode.symbol)){
			opNode = new MultiplyOpNode(sourceNode, argumentNode);
		}
		else if(operationName.equals(DivideOpNode.symbol)){
			opNode = new DivideOpNode(sourceNode, argumentNode);
		}
		
		return opNode;
		
	}

	private ConstraintNode variableExpCS(VariableExpCS variableExpCS) {
		SimpleNameCS simpleName = variableExpCS.getSimpleNameCS();

		return new VariableNode(getFeatureValue(simpleName.getValue()));
	}
	
	private Object getFeatureValue(String name) {

		if(childContext != null){
			for (FeatureValue featureValue : childContext.featureValues) {
				if (featureValue.feature.getName().equals(name)) {
					return featureValue;
				}
			}
			return null;
		}
		else if(context != null){
			for (FeatureValue featureValue : context.featureValues) {
				if (featureValue.feature.getName().equals(name)) {
					return featureValue;
				}
			}
			return null;
		}
		else if(eventOccurrence != null){
			
			if(eventOccurrence instanceof CallEventOccurrence){
				CallEventOccurrence callEvent = (CallEventOccurrence)eventOccurrence;
				for(ParameterValue parameter : callEvent.execution.parameterValues){
					if(parameter.parameter.getName().equals(name)){
						return parameter;
					}
				}
				return null;
			}
			else if(eventOccurrence instanceof SignalEventOccurrence){
				SignalEventOccurrence signalEvent = (SignalEventOccurrence)eventOccurrence;
				for (FeatureValue featureValue : signalEvent.signalInstance.featureValues) {
					if (featureValue.feature.getName().equals(name)) {
						return featureValue;
					}
				}
				return null;
			}
			
		}
		else if(operationParameterValues != null){
			for(ParameterValue value : operationParameterValues){
				if(value.parameter.getName().equals(name)){
					return value;
				}
			}
		}
		
		return null;
		
		
	}

	private ConstraintNode literalExpCS(LiteralExpCS literalExpCS) {

		if (literalExpCS instanceof PrimitiveLiteralExpCS) {
			if (literalExpCS instanceof BooleanLiteralExpCS) {
				return new BooleanLiteralNode(((BooleanLiteralExpCS) literalExpCS).getBooleanSymbol());
			} else if (literalExpCS instanceof IntegerLiteralExpCS) {
				return new IntegerLiteralNode(((IntegerLiteralExpCS) literalExpCS).getIntegerSymbol());
			} else if (literalExpCS instanceof RealLiteralExpCS) {
				return new RealLiteralNode(((RealLiteralExpCS) literalExpCS).getRealSymbol());
			} else if (literalExpCS instanceof StringLiteralExpCS) {
				return new StringLiteralNode(((StringLiteralExpCS) literalExpCS).getStringSymbol());
			} else if (literalExpCS instanceof UnlimitedNaturalLiteralExp) {
				return null;
			}
		} else if (literalExpCS instanceof NullLiteralExpCS) {
			return new NullLiteralNode();
		} else if (literalExpCS instanceof TupleLiteralExpCS) {
			return null;
		}
		return null;
	}

	private void letExp(LetExpCS letExp) {

	}

	private void messageExpCS(MessageExpCS messageExpCS) {

	}

	public static void main(String[] args) {
		//ConstraintInstanceFactory.instance.createConstraintInstance("ekf_variance > 0.8 or (gps_mode = 1 and flightmode = Flightmode::GUIDED)");
	}

}