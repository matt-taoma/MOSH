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


package org.simula.mosh.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Random;

public class ReinforcementLearningAgentProxy {
	
	
	
	private AgentConnectorThread agentConnector;
	private AgentThread agent;
	
	// environment information
	private int stateDim;
	private int actionDim;
	private int timestepLimit;
	private String trainDir;
	
	public void start(int stateDim, int actionDim, int timestepLimit, String trainDir) {
		
		if(trainDir == null){
			trainDir = "./trpo_models/";
		}
		
		this.stateDim = stateDim;
		this.actionDim = actionDim;
		this.timestepLimit = timestepLimit;
		this.trainDir = trainDir;
		
		int port = 8086;
		agentConnector = new AgentConnectorThread(port);
		Thread serverThread = new Thread(agentConnector);
		serverThread.start();
		
		agent = new AgentThread();
		Thread agentThread = new Thread(agent);
		agentThread.start();
		
		handleInitialRequests();
	}
	
	private void handleInitialRequests() {
		
		while(true){
			
			String cmd = agentConnector.receiveMsg();
			
			if(cmd.equals("get stateDim")){
				agentConnector.sendMsg(Integer.toString(stateDim));
			}
			else if(cmd.equals("get actionDim")){
				agentConnector.sendMsg(Integer.toString(actionDim));
			}
			else if(cmd.equals("get timestepLimit")){
				agentConnector.sendMsg(Integer.toString(timestepLimit));
			}
			else if(cmd.equals("get train dir")){
				agentConnector.sendMsg(trainDir);
			}
			else if(cmd.equals("done")){
				agentConnector.sendMsg("done");
				break;
			}
			else{
				System.err.println("receive unknown request from agent (handleInitialRequests) : " + cmd);
				break;
			}
		}
	}
	
	public void episodeFinish(double reward) {
		
		agentConnector.sendMsg("episode finish");
		
		String cmd = agentConnector.receiveMsg();
		
		if(cmd.equals("get reward")){
			agentConnector.sendMsg(Double.toString(reward));
		}
		else{
			System.err.println("receive wrong cmd");
		}
		
	}
	
	public double[] nextEpoch(double[] state) {
		
		agentConnector.sendMsg("next epoch");
		
		double[] action = null;
		
		while(true){
			
			String cmd = agentConnector.receiveMsg();
			
			if(cmd.equals("get state")){
				agentConnector.sendMsg(stateStr(state));
			}
			else if(cmd.startsWith("forward action")){
				action = parseAction(cmd.substring(14));
				agentConnector.sendMsg("epoch done");
				break;
			}
			else{
				System.err.println("receive unknown request from agent (nextEpoch): " + cmd);
				break;
			}
		}
		
		return action;
		
	}
	
	private String stateStr(double[] state){
		
		StringBuffer res = new StringBuffer();
		for(int i = 0; i < stateDim; i++){
			res.append(state[i]);
			if(i != stateDim - 1){
				res.append(", ");
			}
		}
		return res.toString();
	}
	
	private double[] parseAction(String str){
		
		String[] items = str.trim().split(",");
		double[] actionValues = new double[actionDim];
		for(int i = 0; i < actionDim; i++){
			actionValues[i] = Double.parseDouble(items[i].trim());
		}
		return actionValues;
	}
	
	public void stop() {
		agentConnector.sendMsg("iteration finish");
		
		agentConnector.stop();
		if(agent != null){
			agent.stop();
		}
	}
	
	public static void main(String[] args) {
		
		Random random = new Random(1);
		
		String path = "./trpo_models";
		
		for(int n = 0; n < 5; n++){
			
			String trainDir = path + n + "/";
			
			ReinforcementLearningAgentProxy agent = new ReinforcementLearningAgentProxy();
			agent.start(5, 5, 30, trainDir);
			
			for(int episode = 0; episode < 3010; episode++){
				
				double[][] states = new double[10][5];
				double[] state = new double[5];
				
				for(int epoch = 0; epoch < 10; epoch++){
					double[] action = agent.nextEpoch(state);
					
					for(int i = 0; i < action.length; i++){
						action[i] = (action[i] + 4.4) / 10.8; // change action value to [0, 1]
						if(action[i] > 1){
							action[i] = 1;
							System.err.println("!!!!!!!!!  action value is over 1");
						}
						if(action[i] < 0){
							action[i] = 0;
							System.err.println("!!!!!!!!!!! action value is less than 0");
						}
						
						action[i] = action[i] * 2 - 1; // change action value to [-1, 1]; 
					}
					
					for(int i = 0; i < state.length; i++){
						state[i] += action[i];
					}
					
					for(int i = 0; i < state.length; i++){
						states[epoch][i] += state[i];
					}
				}
				
				double reward = 25;
				for(int i = 0; i + 1 < 10; i+=2){
					double r = 0;
					for(int j = 0; j < 5; j++){
						r += Math.abs(states[i+1][j] - states[i][j]);
					}
					reward -= r;
				}
				
				reward = reward/25 + random.nextDouble() * 0.3;
				
				System.out.println("\n\n\n episode: " + episode + ", reward: " + reward + "\n\n\n");
				
				agent.episodeFinish(reward);
				
				if(reward < 0.03){
					System.out.println("success");
					break;
				}
			}
			
			agent.stop();
			
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		
	}
	
}





class AgentThread implements Runnable {
	
	private boolean stop = false;
	
	public void stop() {
		stop = true;
	}
	
	public void run() {
		
		Process ps = null;
		
		try {
			ps = Runtime.getRuntime().exec(new String[]{"python", "./py/ReinforcementLearningAgent.py"});
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		while(!stop){
			
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
		
        ps.destroy();
	}
	
}

class AgentConnectorThread implements Runnable{
	
	private boolean stop = false;
	
	private ServerSocket ss;
	private Socket s;
	private BufferedReader in;
	private BufferedWriter out;
	
	private char[] readBuffer = new char[4096];
	
	public AgentConnectorThread(int port) {
		
		try {
			ss = new ServerSocket(port);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void stop() {
		stop = true;
	}
	
	public void run() {
		try {
			s = ss.accept();
			
			InputStream is = s.getInputStream();
			in = new BufferedReader(new InputStreamReader(is));
			OutputStream os = s.getOutputStream();
			out = new BufferedWriter(new OutputStreamWriter(os));
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		while(!stop){
			try {
				Thread.sleep(500);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		try {
			s.close();
			ss.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public void sendMsg(String msg){
		try {
			out.write(msg + '$');
			out.flush();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String receiveMsg() {
		
		while(in == null){
			try {
				Thread.sleep(500);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		try {
			String msg = "";
			while(true){
				Arrays.fill(readBuffer, '\0');
				in.read(readBuffer);
				String str = String.valueOf(readBuffer).trim();
				msg += str;
				if(msg.endsWith("$")){
					break;
				}
			}
			
			String res = msg.substring(0, msg.length() - 1);
			System.out.println(res);
			return res;
		}
		catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

}

//class AgentServer implements Runnable {
//	
//	private ServerSocket ss;
//	
//	private String[] msgs = new String[10];
//	private int firstMsgIndex = -1;
//	private int lastMsgIndex = -1;
//	private int msgsNumber = 0;
//	
//	public AgentServer(int port) {
//		try {
//			ss = new ServerSocket(port);
//		}
//		catch (IOException e) {
//			e.printStackTrace();
//		}
//		new Thread(this).start();
//	}
//	
//	public String getMsg() {
//		
//		synchronized(msgs) {
//			if(msgsNumber == 0){
//				return null;
//			}
//			
//			String res = msgs[firstMsgIndex];
//			firstMsgIndex++;
//			firstMsgIndex %= msgs.length;
//			msgsNumber --;
//			return res;
//		}
//		
//	}
//	
//	public void run() {
//		
//		
//		while(true) {
//			
//			try {
//				Socket s = ss.accept();
//				
//				InputStream is = s.getInputStream();
//				BufferedReader in = new BufferedReader(new InputStreamReader(is));
//				
//				String line;
//				
//				while((line = in.readLine()) != null){
//					synchronized(msgs) {
//						lastMsgIndex++;
//						lastMsgIndex = lastMsgIndex % msgs.length;
//						msgs[lastMsgIndex] = line;
//						msgsNumber++;
//					}
//				}
//				
//			}
//			catch (IOException e) {
//				e.printStackTrace();
//			}
//			
//		}
//		
//	}
//	
//}