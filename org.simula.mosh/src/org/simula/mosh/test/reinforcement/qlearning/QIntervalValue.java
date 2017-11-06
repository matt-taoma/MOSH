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


package org.simula.mosh.test.reinforcement.qlearning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

class Interval{
	public double lower;
	public double upper;
	
	public Interval(double min, double max){
		this.lower = min;
		this.upper = max;
	}
}

public class QIntervalValue {
	
	private static Random random = new Random();
	
	private int zeroNum = 0;
	
	private List<Double> samples = null;
	private double sum = 0;
	
	private Interval confidenceInterval = new Interval(0, 0);
	
	private boolean changed = false;
	
	public void addSample(double sample){
		
		if(sample == 0){
			if(samples != null){
				System.err.println("sample zero!!");
			}
			else{
				zeroNum ++;
				return;
			}
		}
		else{
			if(zeroNum > 0){
				System.err.println("sample not zero!!");
				zeroNum ++;
				return;
			}
			else if(samples == null){
				samples = new ArrayList<Double>();
			}
		}
		
		boolean added = false;
		for(int i = 0; i < samples.size(); i++){
			double value = samples.get(i);
			if(sample > value){
				samples.add(i, sample);
				added = true;
				break;
			}
		}
		
		if(!added){
			samples.add(sample);
		}
		
		sum += sample;
		changed = true;
	}
	
	public int getSampleSize(){
		if(samples == null){
			return zeroNum;
		}
		else{
			return samples.size();
		}
	}
	
	public double getMax(){
		if(samples == null){
			return 0;
		}
		
		double max = 0;
		for(int i = 0; i < samples.size(); i++){
			if(max < samples.get(i)){
				max = samples.get(i);
			}
		}
		return max;
	}
	
	public double getUpperValue(){
		if(changed){
			bootstrapping();
			changed = false;
		}
		
		return confidenceInterval.upper;
	}
	
	public double getLowerValue(){
		if(changed){
			bootstrapping();
			changed = false;
		}
		
		return confidenceInterval.lower;
	}
	
	public double getMean(){
		if(samples == null){
			return 0;
		}
		
		return sum/samples.size();
	}
	
	private void bootstrapping(){
		
		double mean = sum / samples.size();
		
		double variances[] = new double[100];
		
		for(int i = 0; i < 100; i ++){
			double bootstrappingMean = getBootstrappingMean();
			double bootstrappingVariance = bootstrappingMean - mean;
			variances[i] = bootstrappingVariance;
		}
		
		Arrays.sort(variances);
		
		confidenceInterval.lower = mean - variances[94];
		confidenceInterval.upper = mean - variances[4];
	}
	
	private double getBootstrappingMean(){
		double sum = 0;
		for(int i = 0; i < 10; i++){
			double sample = samples.get(random.nextInt(samples.size() / 2 + 1));
			sum += sample;
		}
		return sum / 10;
	}
	
	public String toString(){
		
		StringBuffer str = new StringBuffer();
		str.append("(");
		
		if(samples == null){
			str.append("zero sample n:");
			str.append(zeroNum);
		}
		else{
			for(Double sample : samples){
				str.append(sample);
				str.append(", ");
			}
		}
		
		str.append(")");
		
		return str.toString();
	}
	
	public static void main(String[] args){
		
//		double[] values = {0.8862318166666667, 0.9552096566666667, 0.7901750433333333, 0.8704115800000001, 0.6906959033333333, 0.76031344, 0.7392956533333334, 0.6696223433333334, 0.77250951};
//		QIntervalValue faultDetectionProbability = new QIntervalValue();
//		
//		for(int i = 0; i < values.length; i ++){
//			faultDetectionProbability.addSample(values[i]);
//		}
//		
//		System.out.println(faultDetectionProbability.getUpperValue() - faultDetectionProbability.getLowerValue());
//		System.out.println(" lower:" + faultDetectionProbability.getLowerValue() + ", upper:" + faultDetectionProbability.getUpperValue());
		
		double[] values = {0.1, 0.1, 0.4, 0.9, 0.1, 0.1, 0.1, 0.1}; 
		double sum = 0;
		for(int i = 0; i < values.length; i ++){
			sum += values[i];
		}
		for(int i = 0; i < values.length; i ++){
			values[i] = values[i]/sum;
		}
		for(int i = 0; i < values.length; i ++){
			System.out.print(values[i] + ", ");
		}
		
	}
	
}