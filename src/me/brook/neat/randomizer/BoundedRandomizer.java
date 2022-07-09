package me.brook.neat.randomizer;

import java.util.Random;

import me.brook.neat.network.GConnection;
import me.brook.neat.network.NeatNetwork;
import me.brook.neat.network.NeatNeuron;

public class BoundedRandomizer implements NetworkRandomizer {

	private double distortionFactor;

	private Random random;

	public BoundedRandomizer(double distortionFactor) {
		this.distortionFactor = distortionFactor;

		random = new Random();
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
		double value = (random.nextDouble() * 2 - 1) * distortionFactor;
		return value;
	}

}
