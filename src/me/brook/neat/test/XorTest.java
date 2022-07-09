package me.brook.neat.test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.UUID;

import javax.swing.JFrame;

import me.brook.neat.GeneticCarrier;
import me.brook.neat.network.NeatNetwork;
import me.brook.neat.network.NeatNetwork.SigmoidFunction;
import me.brook.neat.network.Phenotype;
import me.brook.neat.randomizer.AsexualReproductionRandomizer;
import me.brook.neat.species.InnovationHistory;
import me.brook.neat.species.Species;
import me.brook.neat.species.SpeciesManager;

class XorTest {

	static DecimalFormat df = new DecimalFormat("#.##");
	static DataSet ds = new DataSet(2, 1);

	public static void main(String[] args) throws Exception {
//		NeatNetwork n2 = new NeatNetwork(new InnovationHistory(), new Tanh(), new RectifiedLinear(), new Phenotype(), false, 10, 5, 2);
//		n2.connectAllLayers();
//		while(ds != null) {
//			n2.calculate();
//		}
		
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(768, 768);
		frame.setTitle("Xor Test");
		Insets in = frame.getInsets();
		frame.setVisible(true);
		Graphics2D g2 = (Graphics2D) frame.getGraphics();

		BufferedImage image = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);

		ds.add(new DataSetRow(new double[] { 0, 0 }, new double[] { 0 }));
		ds.add(new DataSetRow(new double[] { 0, 1 }, new double[] { 1 }));
		ds.add(new DataSetRow(new double[] { 1, 0 }, new double[] { 1 }));
		ds.add(new DataSetRow(new double[] { 1, 1 }, new double[] { 0 }));

		int totalIterations = 0;
		for(int attempts = 0; attempts < 100; attempts++) {
			InnovationHistory history = new InnovationHistory();
			XorTester rep = new XorTester(history);

			SpeciesManager<XorTester> sm = new SpeciesManager<>(1, 100000, rep);
			sm.setSpeciationFactors(1.0, 0.45, 0, 1.0);

			int popSize = 150;
			sm.getSpecies().values().forEach(sp -> sp.fillFromRep(popSize, true, 2));
			sm.checkForSpeciation(true);

			int iterations = 0;

			top: for(int i = 0; i < 10000; i++) {
				double highest = 0;
				for(Species<XorTester> sp : sm.getSpecies().values()) {
					if(sp.isEmpty()) {
						continue;
					}

					sp.forEach(gc -> gc.calculateFitness());
					double f = sp.get(0).getFitness();
					if(highest < f) {
						highest = f;
					}
				}

				double sumOfFitness = 0;
				for(Species<XorTester> sp : sm.getSpecies().values()) {
					sp.useSorter();
					sp.shareFitness();
					sumOfFitness += sp.getTotalFitness();
				}
				for(Species<XorTester> sp : sm.getSpecies().values()) {

					XorTester best = sp.get(0);
					g2.setColor(Color.WHITE);
					g2.fillRect(0, 0, frame.getWidth(), frame.getHeight());
					g2.drawImage(best.getBrain().getImage(image.getWidth(), image.getHeight(), true), 0, in.top,
							image.getWidth(), image.getHeight(), frame);

					int rel = (int) Math.ceil((sp.getTotalFitness() / sumOfFitness) * popSize);
					ArrayList<GeneticCarrier> nextGeneration = sp.tournamentSelection(rel, 0.5, 1,
							0.1);

					sp.clear();
					nextGeneration.forEach(gc -> sp.add((XorTester) gc));
				}

				frame.setTitle("" + highest);

				if(highest >= 3.9999) {
					break;
				}

				sm.checkForStagnantSpecies(3);
				sm.checkForSpeciation(true);
				sm.trim();
				iterations++;
			}

			XorTester best = null;
			for(Species<XorTester> sp : sm.getSpecies().values()) {
				sp.useSorter();

				XorTester test = (XorTester) sp.get(0);

				if(best == null || test.getFitness() > best.getFitness()) {
					best = test;
				}

			}

			// System.out.println(" === Best species's results:");
			// for(DataSetRow dsr : ds) {
			// best.network.setInputs(dsr.getInput());
			// best.network.calculate();
			// double out = best.network.getOutput()[0];
			//
			// System.out.println(dsr.getDesiredOutput()[0] + " : " + df.format(out));
			// }
			// System.out.println();
			totalIterations += iterations;
			System.out.println("Average iterations for success: " + ((double) totalIterations / (attempts + 1)));
		}
	}

	public static class XorTester implements GeneticCarrier<NeatNetwork> {

		NeatNetwork network;

		public XorTester(InnovationHistory innoHistory) {
			this(innoHistory,
					new NeatNetwork(innoHistory, new SigmoidFunction(), new SigmoidFunction(), new Phenotype(), false, 2, 2, 1).connectAllLayers());
		}

		public XorTester(InnovationHistory innoHistory, NeatNetwork network) {
			this.network = network;
		}

		@Override
		public boolean isAlive() {
			return true;
		}

		@Override
		public NeatNetwork getBrain() {
			return network;
		}

		double fitness;

		@Override
		public double calculateFitness() {

			float distance = 0;
			for(DataSetRow dsr : ds) {
				network.setInputs(dsr.getInput());
				network.calculate();
				double out = network.getOutput()[0];
				// System.out.println(Arrays.toString(dsr.getInput()) + " " +
				// dsr.getDesiredOutput()[0] + " : " + out);

				distance += Math.abs(out - dsr.getOutput()[0]);
			}

			fitness = 4 - distance;

			return fitness;
		}

		@Override
		public double getFitness() {
			return fitness;
		}

		@Override
		public void setFitness(double fitness) {
			this.fitness = fitness;
		}

		@Override
		public long getTimeOfDeath() {
			return 0;
		}

		@Override
		public void setTimeOfDeath(long time) {

		}

		@Override
		public void mutate(float mutationFactor) {
			network.randomizeWeights(new AsexualReproductionRandomizer(0.5, 1));
//			network.mutateNetwork(0.1, 0.02, 0.1, 0.1, 0.01);
		}

		@Override
		public GeneticCarrier createClone() {
			XorTester tester = null;
			try {
				tester = new XorTester(network.getInnovationHistory(), this.network.copy());
			}
			catch(Exception e) {
				e.printStackTrace();
			}

			return tester;
		}

		@Override
		public GeneticCarrier breed(GeneticCarrier insertions) {
			GeneticCarrier c = createClone();
//			SexualReproductionMixer.breed((NeatNetwork)c.getBrain(), (NeatNetwork) insertions.getBrain(),
//					true, Crossover.MULTIPLE,
//					((NeatNetwork) c.getBrain()).getConnections().size() / 4);

			return c;
		}

		@Override
		public void setSpecies(Species<? extends GeneticCarrier> species) {

		}

		@Override
		public Species<? extends GeneticCarrier> getSpecies() {
			return null;
		}

		@Override
		public double[] getNoveltyMetrics() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public double[] calculateNoveltyMetrics() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public UUID getUUID() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setBrain(NeatNetwork brain) {
			// TODO Auto-generated method stub

		}

	}

}
