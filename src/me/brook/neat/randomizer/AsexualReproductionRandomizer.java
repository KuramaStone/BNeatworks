package me.brook.neat.randomizer;

import java.util.Random;

import me.brook.neat.network.GConnection;
import me.brook.neat.network.NeatNetwork;
import me.brook.neat.network.NeatNeuron;

public class AsexualReproductionRandomizer implements NetworkRandomizer {
	
	private double mutation_rate;
	private double distortionFactor;

	private Random random;

	public AsexualReproductionRandomizer(double mutation_rate, double distortionFactor) {
		this.mutation_rate = mutation_rate;
		this.distortionFactor = distortionFactor;

		random = new Random();
	}

	public double getMutation_rate() {
		return mutation_rate;
	}

	public double getDistortionFactor() {
		return distortionFactor;
	}

	/**
	 * Iterates and randomizes all layers in specified network
	 *
	 * @param neuralNetwork
	 *            neural network to randomize
	 */
	public void randomize(NeatNetwork neat) {
		neat.getTotalNeurons().values().forEach(neuron -> randomize(neuron));
	}

	private void randomize(NeatNeuron neuron) {
		neuron.getInputConnections().forEach(conn -> randomize(conn));

	}

	public void randomize(GConnection connection) {
		double weight = connection.getWeight();
		connection.setWeight(distort(weight));
	}

	/**
	 * Returns distorted weight value
	 * 
	 * @param weight
	 *            current weight value
	 * @return distorted weight value
	 */
	private double distort(double weight) {
		if(random.nextDouble() < mutation_rate) {
			// mutate slightly
			double change = (random.nextDouble() * 2 - 1) * this.distortionFactor;
			weight = weight + change;
		}
		
		return weight;
	}

}
