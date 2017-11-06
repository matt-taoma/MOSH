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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.papyrus.moka.fuml.Semantics.Actions.BasicActions.ActionActivation;
import org.eclipse.uml2.uml.OpaqueAction;
import org.simula.mosh.model.loci.MoshLocus;

public class ShellActionActivation extends ActionActivation{
	
	public static int ins = 0;
	
	@Override
	public void doAction() {
		
		String shellScript = ((OpaqueAction)node).getBodies().get(0);
		
		ShellTask task = new ShellTask();
		if(shellScript.indexOf("running") >= 0){
			task.script = new String[]{"sh", shellScript, "-i", Integer.toString(ins)};
		}
		else{
			task.script = new String[]{"sh", shellScript};
		}
		
		Thread thread = new Thread(task);
		thread.start();
		
		((MoshLocus)this.getExecutionLocus()).addShellTask(task);
		
	}
	
	public static void main(String[] args){
		
		
		
	}
	
}