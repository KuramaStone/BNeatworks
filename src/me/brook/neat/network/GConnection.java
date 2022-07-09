package me.brook.neat.network;

import java.io.Serializable;

public class GConnection implements Serializable {
	private static final long serialVersionUID = -4961667272303786545L;

	private transient NeatNetwork network;
	private transient NeatNeuron from, to;
	private int innovation_number;
	private boolean enabled = true;

	private int fromNeuron, toNeuron;
	private double weight;

	public GConnection(int innovation_number, int fromNeuron, int toNeuron, double weightVal) {
		this.fromNeuron = fromNeuron;
		this.toNeuron = toNeuron;
		this.weight = weightVal;
		this.innovation_number = innovation_number;
	}
	
	public GConnection() {
	}

	public final double getWeightedInput(NeatNeuron fromNeuron) {
		return fromNeuron.getOutput() * weight;
	}

	public int getInnovationNumber() {
		return innovation_number;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public int getFromNeuron() {
		return fromNeuron;
	}

	public int getToNeuron() {
		return toNeuron;
	}
	
	public void setWeight(double weight) {
		this.weight = weight;
	}
	
	public double getWeight() {
		return weight;
	}

	public GConnection copy() {
		GConnection conn =  new GConnection(innovation_number, fromNeuron, toNeuron, weight);
		conn.enabled = this.enabled;
		
		return conn;
	}

	public void assignNetwork(NeatNetwork network) {
		this.network = network;
		from = network.getNeuronByID(fromNeuron);
		to = network.getNeuronByID(toNeuron);
	}
	
	public NeatNeuron getFrom() {
		return from;
	}
	
	public NeatNeuron getTo() {
		return to;
	}

}
