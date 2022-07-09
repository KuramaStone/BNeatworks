package me.brook.neat.network;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import me.brook.neat.network.NeatNetwork.NeatTransferFunction;

public class NeatNeuron implements Serializable {

	private static final long serialVersionUID = 2431974572176312905L;

	private boolean hasMemory = false;
	private boolean shouldCalculate = true;

	public int layerIndex;

	private ToggleInputFunction inputFunction;
	private NeatTransferFunction transferFunction;
	private double transferValue; // value to use within the transfer function
	private int transferID;

	private double input;
	private double output;
	private boolean isBiasNeuron = false;

	private int neuronID;

	private String label;
	private List<Integer> inputConnectionsIDs, outputConnectionsIDs;
	private List<GConnection> inputConnections, outputConnections;
	private transient Random random = new Random();

	public NeatNeuron(NeatNetwork network, int neuronId, int transferID, double transferValue,
			boolean hasMemory) {
		this.neuronID = neuronId;
		this.inputFunction = new ToggleInputFunction();
		this.transferID = transferID;
		this.transferFunction = network.createActivation(transferID, transferValue);
		this.hasMemory = hasMemory;
		random = new Random();

		inputConnections = new ArrayList<>();
		outputConnections = new ArrayList<>();
		inputConnectionsIDs = new ArrayList<>();
		outputConnectionsIDs = new ArrayList<>();
	}

	public NeatNeuron() {
	}

	public void mutateActivation(NeatNetwork network, double magnitude, double chanceForValue, double chanceForActivationChange) {

		if(random.nextDouble() < chanceForValue) {
			this.transferValue += random.nextDouble() * 2 * magnitude - magnitude;
		}
		if(random.nextDouble() < chanceForActivationChange) {
			this.transferID = random.nextInt(network.getPossibleFunctions().size());
			transferFunction = network.createActivation(transferID, transferValue);
		}
	}

	public void assignNetwork(NeatNetwork network) {
		this.inputConnections = network.getConnectionListFromInnovationNumbers(network, this.inputConnectionsIDs);
		this.outputConnections = network.getConnectionListFromInnovationNumbers(network, this.outputConnectionsIDs);
	}

	public void calculate(NeatNetwork network) {
		if(!shouldCalculate)
			return;

		if(this.inputConnections.isEmpty()) {
			if(transferFunction == null)
				this.output = 0;
			else
				this.output = transferFunction.getOutput(0);
			return;
		}

		this.input = inputFunction.getOutput(network, this.inputConnections);
		this.output = transferFunction.getOutput(input);

		if(isBiasNeuron)
			this.output = 1;
	}

	public void setShouldCalculate(boolean should) {
		this.shouldCalculate = should;
	}

	public boolean shouldCalculate() {
		return shouldCalculate;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void setOutput(double output) {
		this.output = output;
	}

	public void addInputConnection(NeatNetwork network, NeatNeuron fromNeuron, int connectionID) {
		this.inputConnections.add(network.getConnectionFromInnovationID(connectionID));
		this.inputConnectionsIDs.add(connectionID);
		fromNeuron.addOutputConnection(network, connectionID);
	}

	public void addOutputConnection(NeatNetwork network, int connection) {

		this.outputConnections.add(network.getConnectionFromInnovationID(connection));
		this.outputConnectionsIDs.add(connection);
	}

	public boolean hasInputConnectionFrom(NeatNetwork network, NeatNeuron n1) {

		for(GConnection conn : inputConnections) {
			if(conn.getFromNeuron() == n1.getNeuronID()) {
				return true;
			}
		}

		return false;
	}

	public List<GConnection> getInputConnections() {
		return inputConnections;
	}

	public double getOutput() {
		return isBiasNeuron ? 1.0 : output;
	}

	public void setInput(double d) {
		this.input = d;
	}

	public List<GConnection> getOutputConnections() {
		return outputConnections;
	}

	public NeatTransferFunction getTransferFunction() {
		return transferFunction;
	}

	public int getTransferID() {
		return transferID;
	}

	public double getTransferValue() {
		return transferValue;
	}

	public void setTransferValue(double transferValue) {
		this.transferValue = transferValue;
	}

	public void setTransferID(int transferID) {
		this.transferID = transferID;
	}

	public int getNeuronID() {
		return neuronID;
	}

	public NeatNeuron copy(NeatNetwork network) {
		NeatNeuron copy = new NeatNeuron(network, neuronID, transferID,
				transferValue, hasMemory);
		copy.layerIndex = this.layerIndex;

		copy.input = this.input;
		copy.output = this.output;
		copy.shouldCalculate = this.shouldCalculate;
		copy.isBiasNeuron = this.isBiasNeuron;

		copy.label = this.label + "";

		this.inputConnectionsIDs.forEach(inno -> copy.inputConnectionsIDs.add(inno));
		this.outputConnectionsIDs.forEach(inno -> copy.outputConnectionsIDs.add(inno));

		return copy;
	}

	public void setBiasNeuron(boolean b) {
		isBiasNeuron = b;
	}

	public boolean isBiasNeuron() {
		return isBiasNeuron;
	}

	public void removeConnection(GConnection conn) {
		this.inputConnections.remove(conn);
		this.inputConnectionsIDs.remove(Integer.valueOf(conn.getInnovationNumber()));

		this.outputConnections.remove(conn);
		this.outputConnectionsIDs.remove(Integer.valueOf(conn.getInnovationNumber()));
	}

	public boolean hasMemory() {
		return hasMemory;
	}

	public double getInput() {
		return isBiasNeuron ? 1 : input;
	}

	public double calculateOutput() {
		if(isBiasNeuron)
			this.output = 1;
		else if(!shouldCalculate)
			this.output = this.input;
		else
			this.output = transferFunction.getOutput(this.input);

		return this.output;
	}

}
