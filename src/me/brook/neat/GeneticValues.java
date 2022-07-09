package me.brook.neat;

import java.io.Serializable;

public class GeneticValues implements Serializable, Genetics {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2954608346279734943L;
	private double[] genetics;
	private GVMutater mutater;

	public GeneticValues(double... genetics) {
		this.genetics = genetics;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		for(int i = 0; i < genetics.length; i++) {
			double d = genetics[i];
			sb.append(d);
		}
		
		return sb.toString();
	}

	public GeneticValues(GVMutater mutater, double... genetics) {
		this.genetics = genetics;
		this.mutater = mutater;
	}

	public GeneticValues() {
	}

	public GeneticValues breed() {
		return this.copy();
	}

	public double distanceFrom(GeneticValues compare) {
		return distanceFrom(compare.getGenetics());
	}

	public double distanceFrom(double[] compare) {
		int diff = 0;
		for(int i = 0; i < compare.length; i++) {
			double d = compare[i];
			if(genetics[i] != d) {
				diff++;
			}
		}

		return diff / genetics.length;
	}

	public GeneticValues copy() {
		double[] array = new double[genetics.length];
		System.arraycopy(genetics, 0, array, 0, genetics.length);

		return new GeneticValues(mutater, array);
	}

	public double[] getGenetics() {
		return genetics;
	}

	public static abstract class GVMutater {
		public abstract double[] mutate(double[] genetics);
	}

	public void setMutater(GVMutater mutater) {
		this.mutater = mutater;
	}

	public void mutate() {
		this.genetics = this.mutater.mutate(genetics);
	}

	public void save(String absolutePath) {
		
	}

	public void setGenetics(double[] genetics) {
		this.genetics = genetics;
	}

}
