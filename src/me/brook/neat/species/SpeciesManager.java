package me.brook.neat.species;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;

import me.brook.neat.GeneticCarrier;
import me.brook.neat.randomizer.BoundedRandomizer;

public class SpeciesManager<T extends GeneticCarrier> implements Serializable {

	private static final long serialVersionUID = 3826822217483517383L;

	private LinkedHashMap<UUID, Species<T>> species;
	private Random random;

	private int maxSpecies, minimumSpecies;

	private AdvantageFunction<Species<T>> advantageFunction;

	private boolean allowSpeciation = true;

	private double disjointWeightFactor, topologyFactor, phenotypeFactor, speciationThreshold;

	@SafeVarargs
	public SpeciesManager(int minimumSpecies, int maxSpecies, T... starters) {
		species = new LinkedHashMap<>();
		random = new Random();
		this.minimumSpecies = minimumSpecies;
		this.maxSpecies = maxSpecies;

		for(int i = 0; i < starters.length; i++) {
			Species<T> sp = new Species<T>(starters[i]);
			species.put(sp.getUUID(), sp);
		}
	}

	public void setSpeciationFactors(double topologyFactor, double weightFactor, double phenotypeFactor,
			double speciationThreshold) {
		this.disjointWeightFactor = weightFactor;
		this.topologyFactor = weightFactor;
		this.phenotypeFactor = phenotypeFactor;
		this.speciationThreshold = speciationThreshold;
	}

	private Comparator<Species<T>> speciesSorter = new Comparator<Species<T>>() {

		@Override
		public int compare(Species<T> o1, Species<T> o2) {
			return (int) Math.signum(o2.getTotalFitness() / (o2.size() + 1) - o1.getTotalFitness() / (o1.size() + 1));
		}
	};

	/**
	 * Removes the worst performing species if they exceed the maxSpecies
	 */
	public void trim() {
		int removed = 0;
		for(Entry<UUID, Species<T>> set : new HashMap<>(this.species).entrySet()) {
			// if the list doesn't contain the species, remove them from the this.species
			if(set.getValue().isEmpty() || set.getValue().isPopulationDead()) {
				this.species.remove(set.getKey());
				removed++;
			}
		}

	}

	public void checkForExtinctSpecies() {

		for(UUID id : new HashSet<>(species.keySet())) {
			Species<T> sp = this.species.get(id);
			if(sp.isExtinct()) {
				this.species.remove(id);
			}
		}

	}

	public List<T> getAllGeneticCarriers() {
		return getAllGeneticCarriers(false);
	}

	public List<T> getAllGeneticCarriers(boolean includeDead) {
		List<T> list = new ArrayList<>();

		species.values().forEach(species -> list.addAll(species));
		if(includeDead)
			species.values().forEach(species -> list.addAll(species.getGraveyard()));

		return list;
	}

	public LinkedHashMap<UUID, Species<T>> getSpecies() {
		return species;
	}

	public int checkForSpeciation(boolean includeDead) {
		if(this.species.size() >= maxSpecies) {
			return 0;
		}

		if(!shouldAllowSpeciation()) {
			return 0;
		}

		int lastSpeciesCount = species.size();

		if(includeDead)
			species.values().forEach(sp -> {
				sp.addDeadToPopulation();
				if(!sp.isEmpty())
					sp.chooseRandomRep();
			});

		for(Species<T> sp : new HashSet<>(this.species.values())) {

			List<T> incompatible = new ArrayList<>();
			for(T gc : new ArrayList<>(sp)) {

				if(!sp.isCompatibleWith(disjointWeightFactor, topologyFactor, phenotypeFactor, speciationThreshold, gc)) {
					incompatible.add(gc);
					sp.remove(gc);
				}

			}

			distributeOrMakeNewSpecies(incompatible);

		}

		return lastSpeciesCount - species.size();
	}

	public void distributeOrMakeNewSpecies(List<T> distribute) {
		if(distribute.isEmpty()) {
			return;
		}
		
		if(species.isEmpty()) {

			Species<T> newSpecies = new Species<T>(distribute.get(0));

			if(advantageFunction != null)
				giveAdvantageTo(newSpecies);

			this.species.put(newSpecies.getUUID(), newSpecies);
			distribute.remove(0);
		}

		if(!allowSpeciation) {
			Species<T> sp = new ArrayList<>(getSpecies().values()).get(0);
			distribute.forEach(t -> sp.add(t));
			return;
		}
		

		for(T t : distribute) {
			Species<T> sp = getCompatibleSpecies(t);
			if(sp != null) {
				sp.add(t);
			}
			else {
				Species<T> newSpecies = new Species<T>(t);

				if(advantageFunction != null)
					giveAdvantageTo(newSpecies);
				
				HashSet<T> list = new HashSet<>(1);
				list.add(t);
				
				newSpecies.addAll(list);
				
				this.species.put(newSpecies.getUUID(), newSpecies);
			}
		}
		
	}

	private Species<T> getCompatibleSpecies(T t) {
		if(!allowSpeciation) {
			return null;
		}

		for(Species<T> sp : this.species.values()) {
			if(sp.isCompatibleWith(this.disjointWeightFactor, this.topologyFactor, this.phenotypeFactor,
					this.speciationThreshold, t)) {
				return sp;
			}
		}

		return null;
	}

	/*
	 * A method to give new species a better chance since I dont have billions of years
	 */
	private void giveAdvantageTo(Species<T> sp) {
		advantageFunction.apply(sp);
	}

	public static interface AdvantageFunction<T> {
		public void apply(T t);
	}

	public void setAdvantageFunction(AdvantageFunction<Species<T>> advantageFunction) {
		this.advantageFunction = advantageFunction;
	}

	public void setAllowSpeciation(boolean allowSpeciation) {
		this.allowSpeciation = allowSpeciation;
	}

	public boolean shouldAllowSpeciation() {
		return allowSpeciation;
	}

	public void fillAllWithReps(int population, boolean mutate, int mutations) {
		this.species.values().forEach(sp -> sp.fillFromRep(population, mutate, mutations));
	}

	public int getMaxSpecies() {
		return maxSpecies;
	}

	public double getDisjointWeightsFactor() {
		return disjointWeightFactor;
	}

	public double getTopologyFactor() {
		return topologyFactor;
	}

	public double getPhenotypeFactor() {
		return phenotypeFactor;
	}

	public double getSpeciationThreshold() {
		return speciationThreshold;
	}

	public void checkForStagnantSpecies(int maxIterations) {
		if(this.species.size() <= minimumSpecies) {
			return;
		}
		
		List<Species<T>> list = new ArrayList<>(this.getSpecies().values());
		for(Species<T> sp : list) {
			boolean isStagnant = sp.checkForStagnation(maxIterations);

			if(this.species.size() > minimumSpecies) {
				if(isStagnant) {
					this.species.remove(sp.getUUID());
				}
			}
		}
	}

	public void clearSpecies() {
		this.species.clear();
	}

	public void removeAgent(T t) {
		species.values().forEach(sp -> sp.remove(t));
	}

	public Collection<? extends T> getAllDeadCarriers() {
		List<T> list = new ArrayList<>();

		species.values().forEach(species -> list.addAll(species.getGraveyard()));

		return list;
	}

	public void calculateFitness() {

		List<T> all = getAllGeneticCarriers();
		all.addAll(this.getAllDeadCarriers());

		all.forEach(agent -> agent.calculateFitness());
		// species.values().forEach(sp -> sp.raiseFitnessToZero());
	}

	public void calculateNovelty() {

		List<T> all = getAllGeneticCarriers();
		all.addAll(this.getAllDeadCarriers());

		all.forEach(gc -> gc.calculateNoveltyMetrics());
		// species.values().forEach(sp -> sp.raiseFitnessToZero());
	}

	public void applyRandomizerToAll(BoundedRandomizer randomizer) {
		getAllGeneticCarriers().forEach(t -> t.network().randomizeWeights(randomizer));
	}

	public void addDeadToPopulation() {
		species.values().forEach(sp -> sp.addDeadToPopulation());
	}

	public void shareFitness() {
		species.values().forEach(sp -> sp.shareFitness());
	}

	public void sort(boolean reverseSort) {
		species.values().forEach(sp -> sp.useSorter(reverseSort));
	}

	/**
	 * 
	 * @param includeDead
	 * @param thresholdCutoff The maximum percentile from 0 an entity can be to speciate. (0 = nobody is good enough, 1 = everyone is)
	 * @return
	 */
	public int checkForSpeciation(boolean includeDead, double thresholdCutoff) {
		if(this.species.size() >= maxSpecies) {
			return 0;
		}

		if(!shouldAllowSpeciation()) {
			return 0;
		}

		int lastSpeciesCount = species.size();

		if(includeDead)
			species.values().forEach(sp -> {
				sp.addDeadToPopulation();
				if(!sp.isEmpty())
					sp.chooseRandomRep();
			});
		
		
		for(Species<T> sp : new HashSet<>(this.species.values())) {

			// get all agents who are incompatible with their current species
			List<T> incompatible = new ArrayList<>();
			
			sp.useSorter();
			
			double index = 0;
			for(T gc : new ArrayList<>(sp)) {
				double p = index++ / sp.size();
				if(p > thresholdCutoff) {
					break;
				}

				if(!sp.isCompatibleWith(disjointWeightFactor, topologyFactor, phenotypeFactor, speciationThreshold, gc)) {
					incompatible.add(gc);
					sp.remove(gc);
				}

			}

			distributeOrMakeNewSpecies(incompatible);

		}

		return lastSpeciesCount - species.size();
	}

	public void redivide(boolean includeDead) {
		List<T> all = getAllGeneticCarriers(includeDead);
		clearSpecies();
		distributeOrMakeNewSpecies(all);
		
	}

	public boolean contains(T t) {
		for(Species<T> sp : this.species.values()) {
			if(sp.contains(t)) {
				return true;
			}
		}
		
		return false;
	}

}
