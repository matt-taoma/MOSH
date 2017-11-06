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


package org.simula.mosh.model.Semantics;

import java.util.List;

import org.eclipse.papyrus.moka.fuml.Semantics.Actions.BasicActions.InvocationActionActivation;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.FeatureValue;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.Reference;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.Value;
import org.eclipse.papyrus.moka.fuml.Semantics.CommonBehaviors.Communications.SignalInstance;
import org.eclipse.uml2.uml.BroadcastSignalAction;
import org.eclipse.uml2.uml.InputPin;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Signal;

public class MoshBroadcastSignalActionActivation extends InvocationActionActivation{
	
	@Override
	public void doAction() {
		// Get the value from the target pin. If the value is not a reference,
		// then do nothing.
		// Otherwise, construct a signal using the values from the argument pins
		// and send it to the referent object.
		BroadcastSignalAction action = (BroadcastSignalAction) (this.node);
		
		Signal signal = action.getSignal();
		SignalInstance signalInstance = new SignalInstance();
		signalInstance.type = signal;
		List<Property> attributes = signal.getOwnedAttributes();
		List<InputPin> argumentPins = action.getArguments();
		for (int i = 0; i < attributes.size(); i++) {
			Property attribute = attributes.get(i);
			InputPin argumentPin = argumentPins.get(i);
			List<Value> values = this.takeTokens(argumentPin);
			signalInstance.setFeatureValue(attribute, values, 0);
		}
		
		ActiveObject object = (ActiveObject)this.getExecutionContext();
		
		for(FeatureValue featureValue : object.featureValues){
			Value value = featureValue.values.get(0);
			if(value instanceof ActiveObject){
				((ActiveObject)value).send(signalInstance);
			}
		}
		
	}
	
}