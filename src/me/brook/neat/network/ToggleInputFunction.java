package me.brook.neat.network;

import java.io.Serializable;
import java.util.List;

/**
 * Optimized version of weighted input function
 *
 * @author Zoran Sevarac
 */
public class ToggleInputFunction implements Serializable {

	private static final long serialVersionUID = 1L;

	public double getOutput(NeatNetwork network, List<GConnection> inputConnections) {
		double output = 0;

		for(GConnection connection : inputConnections) {
			if(connection.isEnabled()) {
				NeatNeuron from = network.getNeuronByID(connection.getFromNeuron());
				output += from.getOutput() * connection.getWeight();
			}
		}

		return output;
	}

	public static double[] getOutput(double[] inputs, double[] weights) {
		double[] output = new double[inputs.length];

		for(int i = 0; i < inputs.length; i++) {
			output[i] += inputs[i] * weights[i];
		}

		return output;
	}
}
