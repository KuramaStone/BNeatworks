package me.brook.neat.randomizer;

import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Random;

import me.brook.neat.network.GConnection;
import me.brook.neat.network.NeatNetwork;
import me.brook.neat.network.NeatNeuron;
import me.brook.neat.network.Phenotype.Pheno;

public class SexualReproductionMixer {

	/**
	 * 
	 * @param base
	 *            This is the one that receives the changes to the network weights
	 * @param insertions
	 * @param cross
	 */
	public static void breed(NeatNetwork base, NeatNetwork insertions, boolean preferBaseTopology, Crossover cross,
			double... parameters) {
		if(base.getConnections().size() == 0) {
			return;
		}

		Random randomGenerator = new Random();

		int breakPoint = -1;
		if(cross == Crossover.SINGLE) {
			breakPoint = randomGenerator.nextInt(base.getConnections().size());
		}

		// be careful with these. these are direct lines to the networks. Changes to these, change the networks.
		// this is to preserve memory as no changes are needed at the time
		LinkedHashMap<Integer, GConnection> base_genome = base.getConnections();
		LinkedHashMap<Integer, GConnection> insertion_genome = insertions.getConnections();

		LinkedHashMap<Integer, GConnection> child_genome = new LinkedHashMap<>();

		boolean isInPointCrossover = false;

		// randomize where the geneIndex starts to ensure random point mutation
		// locations
		int geneIndex = randomGenerator.nextInt(base_genome.size());

		for(Integer innovationID : base_genome.keySet()) {
			GConnection base_gene = base_genome.get(innovationID);

			GConnection insertion_gene = insertion_genome.get(innovationID);

			GConnection gene = base_gene;

			if(insertion_gene != null) {
				switch(cross) {
					// 50/50 chance for each gene
					default: {
						gene = randomGenerator.nextBoolean() ? base_gene : insertion_gene;
						break;
					}
					case SINGLE : {
						gene = geneIndex >= breakPoint ? base_gene : insertion_gene;
					}
					// the genome swaps between which one it uses in segment
					case MULTIPLE : {
						gene = isInPointCrossover ? base_gene : insertion_gene;

						if(geneIndex != 0 && geneIndex % (base_genome.size() / parameters[0]) == 0) {
							isInPointCrossover = !isInPointCrossover;
						}

						break;
					}
				}
			}
			else {
				// disjoint genes added later in the method if needed
			}

			child_genome.put(innovationID, gene.copy());

			geneIndex++;

			// loop index back around
			if(geneIndex >= base_genome.size()) {
				geneIndex = 0;
			}
		}

		// add disjoint genes
		if(!preferBaseTopology) {
			// we cross disjoint genes that only the insertion genome contains. If we prefer the base, then do nothing. Otherwise, add the disjoint ones
			for(Integer innovationID : insertion_genome.keySet()) {
				if(base_genome.containsKey(innovationID))
					continue;

				GConnection insertion_gene = insertion_genome.get(innovationID);

				// check that child has the necessary infrastructure
				if(base.getNeuronByID(insertion_gene.getFromNeuron()) != null
						&& base.getNeuronByID(insertion_gene.getToNeuron()) != null) {
					// child_genome.put(innovationID, insertion_gene.copy());
				}
			}
		}

		// cross phenotype by setting it to the average
		for(Entry<String, Pheno> set : base.getPhenotype().getPhenotypes().entrySet()) {
			double value = set.getValue().getValue() + insertions.getPhenotype().getPhenotype(set.getKey()).getValue();

			base.getPhenotype().getPhenotype(set.getKey()).setValue(value / 2);
		}

		// breed neuron activations
		for(NeatNeuron nn : base.getTotalNeurons().values()) {
			NeatNeuron nn2 = insertions.getNeuronByID(nn.getNeuronID());

			if(nn2 != null) {
				int id1 = nn.getTransferID();
				int id2 = nn2.getTransferID();

				if(id1 == id2) { // average together activation if they're the same
					nn.setTransferValue((nn.getTransferValue() + nn2.getTransferValue() / 2));
				}
				else {
					// 50% chance to go with either network
					if(randomGenerator.nextBoolean()) {
						nn.setTransferID(nn2.getTransferID());
						nn.setTransferValue(nn2.getTransferValue());
					}
				}

			}

		}

		base.setConnections(child_genome);

	}

	public static enum Crossover {
		UNIFORM, MULTIPLE, SINGLE;
	}


}
