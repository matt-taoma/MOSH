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


package org.simula.mosh.test.log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Map;

import org.eclipse.papyrus.moka.fuml.statemachines.Semantics.StateMachines.VertexActivation;
import org.simula.constraint.ConstraintInstance;
import org.simula.mosh.test.invocation.Invocation;

public class TestLogger {
	
	public static String disFile = "/distances.txt";
	public static String logFile = "/moshTestLog.txt";
	public static String recFile = "/records.txt";
	
	private static String logPath = null;
	
	private static PrintWriter writer;
	
	private static int testCaseIndex = 0;
	
	public static int logIndex = 0;
	
	public static synchronized void init(){
		logIndex = 0;
		testCaseIndex = 0;
	}
	
	private static String getCurPath(){
		File directory = new File("");
		return directory.getAbsolutePath();
	}
	
	public static synchronized void beginNextTestTrace(){
		logIndex = 0;
		testCaseIndex++;
		
		if(logPath == null){
			logPath = getCurPath();
		}
		
		String filePath = logPath + logFile;
		
		try {
			writer = new PrintWriter(new FileOutputStream(new File(filePath), true));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		writer.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\ntest trace " + testCaseIndex + " : ");
		
		writer.close();
		
	}
	
	public static void log(VertexActivation vertexActivation, boolean isLeaving){
		
		StringBuffer str = new StringBuffer();
		
		str.append("\t\t");
		str.append(vertexActivation.getExecutionContext().types.get(0).getName());
		str.append(".");
		str.append(vertexActivation.getNode().getName());
		if(isLeaving)
			str.append("\t\tleave");
		else
			str.append("\t\tenter");
		
		log(str.toString());
		
	}
	
	public static synchronized void logTime(long mins){
		if(logPath == null){
			logPath = getCurPath();
		}
		
		String filePath = logPath + disFile;
		try {
			writer = new PrintWriter(new FileOutputStream(new File(filePath), true));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		
		writer.println("\n time (min): " + mins);
		
		writer.close();
	}
	
	public static void logTestResult(Map<ConstraintInstance, Double> minimumDistances, String stateCoverage, String transitionCoverage){
		if(logPath == null){
			logPath = getCurPath();
		}
		
		String filePath = logPath + disFile;
		try {
			writer = new PrintWriter(new FileOutputStream(new File(filePath), true));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		writer.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\ntest trace " + testCaseIndex + " : ");
		for(ConstraintInstance constraint : minimumDistances.keySet()){
			writer.println(constraint + ": " + minimumDistances.get(constraint));
		}
		
		writer.println("\n test trace length: " + logIndex);
		
		writer.println("\n state coverage: " + stateCoverage);
		
		writer.println("\n transition coverage: " + transitionCoverage);
		
		writer.close();
	}
	
	public static void logError(String str){
		
		log("!!! Error happens: " + str);
		
	}
	
	public static void log(String str){
		if(logPath == null){
			logPath = getCurPath();
		}
		
		String filePath = logPath + logFile;
		try {
			writer = new PrintWriter(new FileOutputStream(new File(filePath), true));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		writer.println(str);
		
		writer.close();
		
	}
	
//	public static void saveStateOperationRecord(double[] state0, double[] state1, int opIndex, double opInput, List<TestCandidate> opMask, double reward){
//		
//		if(state0 == null){
//			return;
//		}
//		
//		String filePath = logPath + recFile;
//		try {
//			writer = new PrintWriter(new FileOutputStream(new File(filePath), true));
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}
//		
//		StringBuffer str = new StringBuffer();
//		str.append("\nstate0: ");
//		for(int i = 0; i < state0.length; i ++){
//			str.append(state0[i]);
//			str.append(", ");
//		}
//		
//		
//		str.append("\nstate1: ");
//		for(int i = 0; i < state1.length; i ++){
//			str.append(state1[i]);
//			str.append(", ");
//		}
//		
//		str.append("\noperation: " + opIndex + " ( " + opInput + " ) ");
//		
//		str.append("\nopMask: ");
//		for(TestCandidate candidate : opMask){
//			str.append(candidate.getOpIndex());
//			str.append(":");
//			str.append(candidate.randomInput());
//			str.append(", ");
//		}
//		
//		str.append("\nreward: ");
//		str.append(reward);
//		
//		writer.println(str);
//		
//		writer.close();
//		
//	}
	
	public static void log(Invocation invocation){
		
		StringBuffer str = new StringBuffer();
		
		str.append(logIndex);
		str.append(". invoke ");
		str.append(invocation.toString());
		
		log(str.toString());
		logIndex++;
	}
	
	
}