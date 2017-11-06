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
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.BooleanValue;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.EnumerationValue;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.FeatureValue;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.IntegerValue;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.RealValue;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.StringValue;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.UnlimitedNaturalValue;
import org.eclipse.papyrus.moka.fuml.Semantics.Classes.Kernel.Value;
import org.eclipse.uml2.uml.InputPin;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.OutputPin;
import org.eclipse.uml2.uml.Type;
import org.simula.mosh.test.log.TestLogger;
import org.simula.mosh.test.sut.SUTProxy;
import org.simula.mosh.test.sut.SUTConnector.MsgType;

public class SUTOutputActionActivation extends ActionActivation{
	
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
		
		boolean succ = SUTProxy.instance().send(host, port, cmd);
		if(!succ){
			this.terminate();
			return;
		}
		String res = SUTProxy.instance().receive(host, port, MsgType.MSG_TYPE_STATUS);
		if(res == null){
			this.terminate();
			return;
		}
		
		String[] pairs = res.split(";"); 
    	for(String pair : pairs){
    		String[] key_value = pair.split(":");
    		if(key_value.length < 2){
    			continue;
    		}
    		output(opaqueAction, key_value[0].trim(), key_value[1].trim());
    		
//    		if(key_value[0].trim().equals("obstacle_distance")){
//    			double dis = Double.parseDouble(key_value[1].trim());
//    			if(dis < 1000){
//    				System.out.println("ddddddddddddddddd " + dis);
//    			}
//    			
//    			
//    		}
    	}
		
	}
	
	private void output(OpaqueAction opaqueAction, String name, String strValue){
		
		List<OutputPin> outputPins = opaqueAction.getOutputs();
		for(OutputPin output : outputPins){
			if(output.getName().equals(name)){
				Type type = output.getType();
				Value value = null;
				if(type.getName().equals("Boolean")){
					value = new BooleanValue();
					if(strValue.equals("1")){
						((BooleanValue)value).value = true;
					}
					else if(strValue.equals("0")){
						((BooleanValue)value).value = false;
					}
					else{
						((BooleanValue)value).value = Boolean.parseBoolean(strValue);
					}
				}
				else if(type.getName().equals("String")){
					value = new StringValue();
					((StringValue)value).value = strValue;
				}
				else if (type.getName().equals("Integer")) {
					value = new IntegerValue();
					((IntegerValue)value).value = Integer.parseInt(strValue);
				} else if (type.getName().equals("UnlimitedNatural")) {
					value = new UnlimitedNaturalValue();
					((UnlimitedNaturalValue)value).value = Integer.parseInt(strValue);
				} else if (type.getName().equals("Real")) {
					value = new RealValue();
					((RealValue)value).value = Double.parseDouble(strValue);
				}
				assert(value != null);
				this.putToken(output, value);
			}
		}
		
	}
	

}