package me.brook.neat.test;

import java.util.HashSet;

public class DataSet extends HashSet<DataSetRow> {

	private static final long serialVersionUID = 7232479851799143508L;
	private int lengthofSetA, lengthofSetB;

	public DataSet(int lengthofSetA, int lengthofSetB) {
		this.lengthofSetA = lengthofSetA;
		this.lengthofSetB = lengthofSetB;
	}

	@Override
	public boolean add(DataSetRow dataSetRow) {
		if(dataSetRow.getInput().length != lengthofSetA) {
			throw new IllegalArgumentException("Length of inputs does not match assigned width.");
		}
		if(dataSetRow.getOutput().length != lengthofSetB) {
			throw new IllegalArgumentException("Length of outputs does not match assigned width.");
		}

		return super.add(dataSetRow);
	}
	
}
