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


package org.simula.mosh.model.actions;

import java.util.List;

import org.eclipse.papyrus.moka.fuml.Semantics.Actions.BasicActions.ActionActivation;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.Value;
import org.eclipse.uml2.uml.InputPin;
import org.eclipse.uml2.uml.OpaqueAction;
import org.simula.mosh.test.sut.SUTProxy;

public class SUTInputActionActivation extends ActionActivation{

	@Override
	public void doAction() {
		
		OpaqueAction opaqueAction = (OpaqueAction)node;
		String script = opaqueAction.getBodies().get(0);
		
		int hostEnd = script.indexOf(':');
		assert(hostEnd > 0);
		String host = script.substring(0, hostEnd);
		int portEnd = script.indexOf('/', hostEnd);	
		assert(portEnd > 0);
		String port = script.substring(hostEnd + 1, portEnd);
		String cmd = script.substring(portEnd + 1);
		
		List<InputPin> inputPins = opaqueAction.getInputs();
		for(InputPin inputPin : inputPins){
			String name = inputPin.getName();
			List<Value> values = this.takeTokens(inputPin);
			assert(values.size() == 1);
			String value = values.get(0).specify().stringValue();
			
			cmd = cmd.replace("$" + name, value);
		}
		
		System.out.println("cmd: " + cmd);
		
		if(!SUTProxy.instance().send(host, port, cmd)){
			this.terminate();
		}
		
		if(cmd.equals("stop")){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}

}