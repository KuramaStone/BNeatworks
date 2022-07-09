package me.brook.neat.network;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

/**
 * This class is for storing values that should mutate yet cannot fit into a standard "brain". Examples are color, size, and speed
 * 
 * @author Brookie
 *
 */
public class Phenotype implements Serializable {

	private static final long serialVersionUID = 3995967461950958897L;
	/*
	 * Values are stores here under the assigned name
	 */
	private LinkedHashMap<String, Pheno> phenotype;

	public Phenotype(LinkedHashMap<String, Pheno> phenotype) {
		this.phenotype = phenotype;
	}

	public Phenotype() {
		this(new LinkedHashMap<>());
	}

	public Phenotype copy() {
		Phenotype copy = new Phenotype();

		for(Entry<String, Pheno> set : this.phenotype.entrySet()) {
			copy.phenotype.put(set.getKey(), set.getValue().copy());
		}

		return copy;
	}

	/*
	 * This measures the distance between the phenotypes's variables normalized for the largest phenotype size
	 */
	public double distanceFrom(Phenotype compare) {
		double sum = 0;

		for(String str : this.phenotype.keySet()) {
			sum += Math.abs(this.getPhenotype(str).value - compare.getPhenotype(str).value);
		}

		return sum / this.phenotype.size();
	}

	public Map<String, Pheno> getPhenotypes() {
		return phenotype;
	}

	public Pheno getPhenotype(String label) {
		return phenotype.get(label);
	}

	public void addPhenotype(String label, Pheno value) {
		this.phenotype.put(label, value);
	}

	public void mutate(double percentage) {

		Random random = new Random();
		for(String label : this.phenotype.keySet()) {
			Pheno pheno = this.phenotype.get(label);

			if(random.nextDouble() < percentage) {
				pheno.value += random.nextDouble() * pheno.mutationFactor * 2 - pheno.mutationFactor;
				pheno.clamp();
			}

			this.phenotype.replace(label, pheno);
		}

	}

	/**
	 * 
	 * @author Brookie
	 *
	 *         Pheno is used to store the value of a gene, the sensitivity to mutations, and the minimum and maximum values. This is to allow quick mutation of values within ranges
	 */
	public static class Pheno implements Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = 313922828491399420L;
		private double value;
		private double mutationFactor;
		private double min, max;
		private boolean loop;
		
		private transient Random random = new Random();

		public Pheno(double value, double mutationFactor, double min, double max) {
			this(value, mutationFactor, min, max, false);
		}

		public Pheno(double value, double mutationFactor, double min, double max, boolean loop) {
			this.value = value;
			this.mutationFactor = mutationFactor;
			this.min = min;
			this.max = max;
			this.loop = loop;
		}

		public Pheno copy() {
			return new Pheno(value, mutationFactor, min, max);
		}

		public Pheno() {
		}

		public void clamp() {
			if(!loop) {
				this.value = Math.max(min, Math.min(max, this.value));
			}
			else {

				while(this.value > max) {
					this.value -= (max - min);
				}

				while(this.value < min) {
					this.value += (max - min);
				}

			}
		}

		public double getValue() {
			return value;
		}

		public double getMutationFactor() {
			return mutationFactor;
		}

		public double getMin() {
			return min;
		}

		public double getMax() {
			return max;
		}

		/**
		 * 
		 * @return Returns a value between 0 and 1 that represents the phenotype between its minimum and max
		 */
		public double getNormalizedValue() {
			double d = (value - min) / (max - min);

			return d;
		}

		public void setValue(double value) {
			this.value = value;
		}

		public void shuffle() {
			value = (random.nextDouble() * (max - min)) + min;
			clamp();
		}

	}

	public void shuffle() {

		for(String label : this.phenotype.keySet()) {
			Pheno pheno = this.phenotype.get(label);
			pheno.shuffle();


			this.phenotype.replace(label, pheno);
		}

	}

}
