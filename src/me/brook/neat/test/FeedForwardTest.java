package me.brook.neat.test;

import me.brook.neat.network.NeatNetwork;
import me.brook.neat.network.NeatNetwork.SigmoidFunction;
import me.brook.neat.network.Phenotype;
import me.brook.neat.species.InnovationHistory;

class FeedForwardTest {
	static DataSet ds = new DataSet(2, 1);

	public static void main(String[] args) {
		ds.add(new DataSetRow(new double[] { 0, 0 }, new double[] { 0 }));
		ds.add(new DataSetRow(new double[] { 0, 1 }, new double[] { 1 }));
		ds.add(new DataSetRow(new double[] { 1, 0 }, new double[] { 1 }));
		ds.add(new DataSetRow(new double[] { 1, 1 }, new double[] { 0 }));

		InnovationHistory history = new InnovationHistory();
		NeatNetwork network = new NeatNetwork(history, new SigmoidFunction(), new SigmoidFunction(), new Phenotype(), false, 2, 2, 1);
		network.setWeightOf(0, 3, 20);
		network.setWeightOf(0, 4, -20);
		network.setWeightOf(1, 3, 20);
		network.setWeightOf(1, 4, -20);

		network.setWeightOf(3, 5, 20);
		network.setWeightOf(4, 5, 20);

		network.setWeightOf(2, 3, -10);
		network.setWeightOf(2, 4, 30);

		network.setWeightOf(2, 5, -30);

		network.setInputs(new double[] { 1, 1 });

		network.calculate();
		
		for(DataSetRow dsr : ds) {
			network.setInputs(dsr.getInput());
			network.calculate();
			double out = network.getOutput()[0];

			System.out.println(dsr.getOutput()[0] + " : " + out);
		}
	}

}
