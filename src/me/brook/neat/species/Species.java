package me.brook.neat.species;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import me.brook.neat.GeneticCarrier;
import me.brook.neat.Population;
import me.brook.neat.randomizer.NetworkRandomizer;

public class Species<T extends GeneticCarrier> extends Population<T> {

	private static final long serialVersionUID = 6561307507766961206L;
	private T representative;

	private static Random random = new Random();

	private final UUID uuid;

	private double highestFitness;
	private int iterationsSinceImproved;
	
	private UUID parentSpecies;

	public Species(T representative) {
		super();
		uuid = UUID.randomUUID();
		this.representative = representative;

		if(representative != null) {
			if(representative.getSpecies() != null) {
				// get uuid of previous species. NEVER keep an instance or you could keep GBs worth of junk data from the matrioshka doll
				this.parentSpecies = representative.getSpecies().uuid;
			}
			representative.setSpecies(this);
		}
	}
	
	@Override
	public void add(int index, T element) {
		super.add(index, element);
		
		element.setSpecies(this);
	}

	@Override
	public boolean add(T e) {
		if(e == null) {
			throw new IllegalArgumentException();
		}
		
		e.setSpecies(this);
		return super.add(e);
	}
	
	public boolean checkForStagnation(int maxIterations) {
		if(this.size() > 1) {
			this.forEach(t -> t.calculateFitness());
			useSorter();
			double highest = this.get(0).getFitness();

			if(highest > highestFitness) {
				iterationsSinceImproved = 0;
				highestFitness = highest;
			}
			else {
				iterationsSinceImproved++;
			}
		}
		else {
			iterationsSinceImproved++;
		}

		return iterationsSinceImproved >= maxIterations;

	}

	public void setRepresentative(T representative) {
		this.representative = representative;
	}

	public List<T> checkForNewSpecies(SpeciesManager<T> sm, boolean includeDead) {
		List<T> list = new ArrayList<>();

		for(T gc : this) {
			if(gc != this.representative) {
				if(!includeDead && !gc.isAlive()) {
					continue;
				}
				if(sm.getMaxSpecies() <= sm.getSpecies().size() + list.size()) {
					break;
				}

				if(!isCompatibleWith(sm.getDisjointWeightsFactor(), sm.getTopologyFactor(), sm.getPhenotypeFactor(),
						sm.getSpeciationThreshold(), gc)) {

					boolean exists = false;
					// make sure species doesn't already exist
					for(T gc2 : list) {
						if(!includeDead && !gc2.isAlive()) {
							continue;
						}
						// if the distance from an already created species is similar to this one, dont
						// recreate the wheel
						if(getDistanceFrom(sm.getDisjointWeightsFactor(), sm.getTopologyFactor(), sm.getPhenotypeFactor(),
								gc, gc2) <= 1.0) {
							exists = true;
						}
					}

					if(!exists) {
						list.add(gc);
					}
				}

			}
		}

		this.removeAll(list);

		return list;
	}

	public boolean isCompatibleWith(double c1, double c2, double c3, double threshold, T gc) {
		return this.getDistanceFrom(c1, c2, c3, gc) <= threshold;
	}

	public void shareFitness() {

		for(T gc1 : this) {
			gc1.setFitness(gc1.getFitness() / this.size());
		}

	}

	public double getDistanceFrom(double c1, double c2, double c3, T compare) {
		return getDistanceFrom(c1, c2, c3, this.getRepresentative(), compare);

	}

	public double getDistanceFrom(double c1, double c2, double c3, T a, T b) {
		if(a.isNetwork())
			return a.network().getDistanceFrom(b.network(), c1, c2, c3);
		else {
			return a.distanceFrom(b);
		}
	}

	public T getRepresentative() {
		return representative;
	}

	public List<T> fillFromRep(int amount, boolean mutate, int mutationAmount) {
		List<T> list = new ArrayList<>(amount);
		for(int i = 0; i < amount; i++) {
			T c = (T) representative.createClone();

			if(mutate)
				for(int j = 0; j < mutationAmount; j++)
					c.mutate(1);

			list.add(c);
			this.add(c);
		}

		return list;
	}

	public void chooseRandomRep() {
		this.representative = this.get(random.nextInt(this.size()));
	}

	public double getTotalFitness() {
		double sum = 0;

		for(T gc : this) {
			sum += gc.getFitness();
		}

		return sum;
	}

	public boolean isExtinct() {

		for(T gc : this) {
			if(gc.isAlive()) {
				return false;
			}
		}

		return true;
	}

	public UUID getUUID() {
		return uuid;
	}

	public void randomize(NetworkRandomizer randomizer) {
		this.forEach(t -> t.network().randomizeWeights(randomizer));
	}

	public void calculateFitness() {
		this.forEach(t -> t.calculateFitness());
	}

	public ArrayList<T> getLiving() {
		ArrayList<T> list = new ArrayList<>(this);
		list.removeIf(t -> !t.isAlive());
		return list;
	}

	// public void raiseFitnessToZero() {
	//
	// double lowest = 0;
	// for(int i = 0; i < this.size(); i++) {
	//
	// if(this.get(i).getFitness() < lowest) {
	// lowest = this.get(i).getFitness();
	// }
	//
	// }
	// for(int i = 0; i < this.graveyard.size(); i++) {
	//
	// if(this.graveyard.get(i).getFitness() < lowest) {
	// lowest = this.graveyard.get(i).getFitness();
	// }
	//
	// }
	//
	// for(int i = 0; i < this.size(); i++) {
	// T t = this.get(i);
	// t.setFitness(t.getFitness() + lowest);
	// }
	// for(int i = 0; i < this.graveyard.size(); i++) {
	// T t = this.graveyard.get(i);
	// t.setFitness(t.getFitness() + lowest);
	// }
	//
	// }

}
