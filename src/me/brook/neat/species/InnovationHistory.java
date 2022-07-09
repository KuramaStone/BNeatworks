package me.brook.neat.species;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.brook.neat.GeneticCarrier;
import me.brook.neat.network.NeatNetwork;

public class InnovationHistory implements Serializable {

	private static final long serialVersionUID = 4668872705387069086L;

	private Map<InnoCoords, Innovation> innovations;
	private int innovationIndex;

	public InnovationHistory() {
		innovations = new HashMap<>();
	}

	public int getInnovationIndex() {
		return innovationIndex;
	}

	public Map<InnoCoords, Innovation> getInnovations() {
		return innovations;
	}

	public int getNextInnovation(int fromNeuron, int toNeuron) {

		// check to see if innovation has already occured
		Innovation inno = innovations.get(new InnoCoords(fromNeuron, toNeuron));

		if(inno == null) {
			inno = new Innovation(fromNeuron, toNeuron, innovationIndex++);
			innovations.put(new InnoCoords(fromNeuron, toNeuron), inno);
		}

		return inno.getInnovation();
	}

	public int getNextRawInnovation() {
		return innovationIndex++;
	}

	public static class Innovation implements Serializable {

		private static final long serialVersionUID = -4089481074551507255L;

		private int fromNeuron, toNeuron;
		private int innovation;

		public Innovation(int fromNeuron, int toNeuron, int innovationIndex) {
			this.fromNeuron = fromNeuron;
			this.toNeuron = toNeuron;
			this.innovation = innovationIndex;
		}

		public Innovation() {
		}

		public int getFromNeuron() {
			return fromNeuron;
		}

		public int getToNeuron() {
			return toNeuron;
		}

		public int getInnovation() {
			return innovation;
		}

		@Override
		public int hashCode() {
			return hashCode(fromNeuron, toNeuron);
		}

		public static int hashCode(int fromNeuron, int toNeuron) {
			final int prime = 31;
			int result = 1;
			result = prime * result + fromNeuron;
			result = prime * result + toNeuron;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if(this == obj)
				return true;
			if(obj == null)
				return false;
			if(getClass() != obj.getClass())
				return false;
			Innovation other = (Innovation) obj;
			if(fromNeuron != other.fromNeuron)
				return false;
			if(toNeuron != other.toNeuron)
				return false;
			return true;
		}

	}

	public static class InnoCoords implements Serializable {
		private static final long serialVersionUID = -268593848355809321L;
		public int x, y;

		public InnoCoords(int x, int y) {
			this.x = x;
			this.y = y;
		}

		public InnoCoords() {
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + x;
			result = prime * result + y;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if(this == obj)
				return true;
			if(obj == null)
				return false;
			if(getClass() != obj.getClass())
				return false;
			InnoCoords other = (InnoCoords) obj;
			if(x != other.x)
				return false;
			if(y != other.y)
				return false;
			return true;
		}

	}

	public void removeOldNovelties(List<? extends GeneticCarrier> allGeneticCarriers) {
		List<Integer> neuronsUsedInCurrentPop = new ArrayList<>();

		for(GeneticCarrier gc : allGeneticCarriers) {
			if(gc.getBrain() instanceof NeatNetwork) {
				NeatNetwork network = (NeatNetwork) gc.getBrain();

				for(int id : network.getTotalNeurons().keySet()) {
					if(!neuronsUsedInCurrentPop.contains(id))
						neuronsUsedInCurrentPop.add(id);
				}
			}
		}
		
		// put the innovations to keep inside of this
		HashMap<InnoCoords, Innovation> toKeep = new HashMap<>();
		for(InnoCoords ic : this.innovations.keySet()) {
			Innovation inno = this.innovations.get(ic);
			if(neuronsUsedInCurrentPop.contains(inno.fromNeuron) || neuronsUsedInCurrentPop.contains(inno.toNeuron)) {
				toKeep.put(ic, inno);
			}
		}
		
		this.innovations = toKeep;

	}

}
