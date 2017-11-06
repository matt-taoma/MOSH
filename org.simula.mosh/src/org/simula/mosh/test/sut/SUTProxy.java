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


package org.simula.mosh.test.sut;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.simula.mosh.test.sut.SUTConnector.MsgType;

public class SUTProxy {

	private Map<String, SUTConnector> connectors = new HashMap<String, SUTConnector>();

	private static SUTProxy instance = null;
	
	private String sutPort;
	private String mavproxyPort;
	
	private SUTProxy() {

	}
	
	public void init(String sutPort, String mavproxyPort){
		this.sutPort = sutPort;
		this.mavproxyPort = mavproxyPort;
	}
	
	public void reset() {
		for (SUTConnector connector : connectors.values()) {
			connector.close();
		}
		connectors.clear();
	}

	public boolean send(String host, String port, String str) {
		
		if(port.equals("10008")){
			port = sutPort;
		}
		else if(port.equals("20008")){
			port = mavproxyPort;
		}
		else{
			port = mavproxyPort; 
		}
		
		host = host.trim();
		port = port.trim();

		SUTConnector connector = connectors.get(host + port);
		if (connector == null) {
			connector = new SUTConnector(host, port);
			connectors.put(host + port, connector);
		}
		
		return connector.send(str);
	}

	public String receive(String host, String port, MsgType type) {
		host = host.trim();
		port = port.trim();
		
		if(port.equals("10008")){
			port = sutPort;
		}
		else if(port.equals("20008")){
			port = mavproxyPort;
		}
		
		SUTConnector connector = connectors.get(host + port);
		if (connector == null) {
			connector = new SUTConnector(host, port);
			connectors.put(host + port, connector);
		}

		return connector.receive(type);
	}

	public static SUTProxy instance() {
		if (instance == null) {
			instance = new SUTProxy();
		}
		return instance;
	}

}