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

public class ShellTask implements Runnable {
	
	public String[] script = null;
	public Process ps = null;
	private boolean running = true;
	
	public void stop(){
		running = false;
	}
	
	public void run() {
		
		try {
			ps = Runtime.getRuntime().exec(script);
			
//			print(ps.getInputStream());
//			print(ps.getErrorStream());
//			
		} catch (IOException e) {
			e.printStackTrace();
		} 
        
		while(running){
			
			try{
				ps.exitValue();
				break;
			}
			catch(IllegalThreadStateException e){
				try {
					Thread.sleep(1000);
				}
				catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
		
//		try {
//			ps.waitFor();
//		}
//		catch (InterruptedException e) {
//			e.printStackTrace();
//		}
		
        ps.destroy();
		
		System.out.println(script + "   script done");
		
	}
	
	private void print(InputStream stream) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(stream));
		StringBuffer sb = new StringBuffer();
		String line;
		while ((line = br.readLine()) != null) {
			sb.append(line).append("\n");
		}
		String result = sb.toString();
		System.out.println(result);
	}
	
	
}