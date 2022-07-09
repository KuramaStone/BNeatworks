package me.brook.neat;

import java.util.UUID;

import me.brook.neat.network.NeatNetwork;
import me.brook.neat.species.Species;

public interface GeneticCarrier<T extends Genetics> {

	public boolean isAlive();

	public T getBrain();

	public void setBrain(NeatNetwork brain);

	public double calculateFitness();

	public double getFitness();

	public void setFitness(double fitness);

	public long getTimeOfDeath();

	public void setTimeOfDeath(long time);

	public void mutate(float multiplier);

	public GeneticCarrier createClone();

	public GeneticCarrier breed(GeneticCarrier insertions);

	public void setSpecies(Species<? extends GeneticCarrier> species);

	public Species<? extends GeneticCarrier> getSpecies();

	public double[] getNoveltyMetrics();

	public double[] calculateNoveltyMetrics();

	public UUID getUUID();

	public default NeatNetwork network() {
		return (NeatNetwork) getBrain();
	}

	public default boolean isNetwork() {
		return getBrain() instanceof NeatNetwork;
	}

	public default double distanceFrom(GeneticCarrier<T> b) {
		return 0;
	}

	/**
	 * 
	 * @param o2
	 * @return -1, 0, or 1 for comparison
	 */
	public default int compare(GeneticCarrier<T> t) {
		return (int) Math.signum(t.getFitness() - this.getFitness());
	}

}
