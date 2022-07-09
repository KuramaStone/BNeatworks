package me.brook.neat.test;

import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JFrame;

import me.brook.neat.network.NeatNetwork;
import me.brook.neat.network.NeatNetwork.SigmoidFunction;
import me.brook.neat.network.Phenotype;
import me.brook.neat.randomizer.AsexualReproductionRandomizer;
import me.brook.neat.randomizer.SexualReproductionMixer;
import me.brook.neat.randomizer.SexualReproductionMixer.Crossover;
import me.brook.neat.species.InnovationHistory;

class VisualTest {

	public static void main(String[] args) {
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(500, 500);
		frame.setTitle("Network Test");
		Insets i = frame.getInsets();
		frame.setVisible(true);

		InnovationHistory history = new InnovationHistory();
		NeatNetwork network2 = new NeatNetwork(history, new SigmoidFunction(), new SigmoidFunction(), new Phenotype(), false, 1, 2);
		network2.setWeightOf(0, 2, 1);
		// network.connectAllLayers();

		// network.setWeightOf(0, 3, 20);
		// network.setWeightOf(0, 4, -20);
		// network.setWeightOf(1, 3, 20);
		// network.setWeightOf(1, 4, -20);
		//
		// network.setWeightOf(3, 5, 20);
		// network.setWeightOf(4, 5, 20);
		//
		// network.setWeightOf(2, 3, -10);
		// network.setWeightOf(2, 4, 30);
		//
		// network.setWeightOf(2, 5, -30);

		frame.getGraphics().drawImage(network2.getImage(frame.getWidth(), frame.getHeight(), true), 0, i.top,
				frame.getWidth(), frame.getHeight(), frame);

		frame.addKeyListener(new KeyAdapter() {
			
			NeatNetwork network = network2;
			@Override
			public void keyReleased(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_C) {
					network.generateConnection(100);
					frame.getGraphics().drawImage(network.getImage(frame.getWidth(), frame.getHeight(), true), 0, i.top,
							frame.getWidth(), frame.getHeight(), frame);
				}
				if(e.getKeyCode() == KeyEvent.VK_N) {
					network.generateNeuron(100);
					frame.getGraphics().drawImage(network.getImage(frame.getWidth(), frame.getHeight(), true), 0, i.top,
							frame.getWidth(), frame.getHeight(), frame);
				}
				if(e.getKeyCode() == KeyEvent.VK_M) {
//					network.mutateNetwork(0.75, 0.25, 0.25, 0.05, null, 0, 0, 0);
					frame.getGraphics().drawImage(network.getImage(frame.getWidth(), frame.getHeight(), true), 0, i.top,
							frame.getWidth(), frame.getHeight(), frame);
				}
				if(e.getKeyCode() == KeyEvent.VK_SPACE) {
					network.randomizeWeights(new AsexualReproductionRandomizer(1, 1));
					frame.getGraphics().drawImage(network.getImage(frame.getWidth(), frame.getHeight(), true), 0, i.top,
							frame.getWidth(), frame.getHeight(), frame);
				}
				if(e.getKeyCode() == KeyEvent.VK_P) {
					try {
						frame.getGraphics().drawImage(network.copy().getImage(frame.getWidth(), frame.getHeight(), true), 0,
								i.top, frame.getWidth(), frame.getHeight(), frame);
					}
					catch(Exception e1) {
						e1.printStackTrace();
					}
				}
				if(e.getKeyCode() == KeyEvent.VK_T) {
					getClass();
				}
				if(e.getKeyCode() == KeyEvent.VK_F) {
					network.setInputs(new double[] { 0 });
					network.calculate();
					try {
						frame.getGraphics().drawImage(network.getImage(frame.getWidth(), frame.getHeight(), true), 0, i.top,
								frame.getWidth(), frame.getHeight(), frame);
					}
					catch(Exception e1) {
						e1.printStackTrace();
					}
				}
				if(e.getKeyCode() == KeyEvent.VK_L) {
					network.randomizeWeights(new AsexualReproductionRandomizer(1, 1));
					try {
						frame.getGraphics().drawImage(network.getImage(frame.getWidth(), frame.getHeight(), true), 0, i.top,
								frame.getWidth(), frame.getHeight(), frame);
					}
					catch(Exception e1) {
						e1.printStackTrace();
					}
				}
				if(e.getKeyCode() == KeyEvent.VK_B) {
					try {
						NeatNetwork base = network.copy();
						
						SexualReproductionMixer.breed(base, network, true, Crossover.MULTIPLE,
								base.getConnections().size() / 4);
						frame.getGraphics().drawImage(base.getImage(frame.getWidth(), frame.getHeight(), true), 0, i.top,
								frame.getWidth(), frame.getHeight(), frame);
					}
					catch(Exception e1) {
						e1.printStackTrace();
					}
				}

			}
		});

	}

}
