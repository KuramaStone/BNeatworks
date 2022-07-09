package me.brook.neat;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

public class Population<T extends GeneticCarrier> extends ArrayList<T> {

	private static final long serialVersionUID = -3894358162233056351L;

	private String label = "";
	private Color color;

	private Random random;
	private Selection selection;

	private List<T> scheduledAdditions;
	protected List<T> graveyard;
	private Comparator<GeneticCarrier> sortingSystem;

	private Map<String, Object> values;

	public Population() {
		scheduledAdditions = new ArrayList<>();
		graveyard = new ArrayList<>();

		random = new Random();
		values = new HashMap<>();

		sortingSystem = HIGH_FITNESS;
	}

	public void checkForAdditions() {
		if(!scheduledAdditions.isEmpty()) {
			this.addAll(scheduledAdditions);
			scheduledAdditions.clear();
		}
	}

	public boolean isPopulationDead() {

		for(T e : this) {
			if(e.isAlive()) {
				return false;
			}
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public ArrayList<T> chooseRouletteGeneration(int amount, double elitismPercentage) {
		ArrayList<T> nextGen = new ArrayList<>();

		if(this.isEmpty()) {
			return nextGen;
		}

		// get sum of fitness
		double sum = 0;
		for(T e : this) {
			sum += e.getFitness();
		}

		int elitismPopulation = (int) Math.ceil(this.size() * elitismPercentage);

		int[] picks = new int[this.size()];
		for(int i = 0; i < amount - elitismPopulation; i++) {

			T base = (T) selectFromGroup(this, random.nextDouble(), sum);
			picks[this.indexOf(base)]++;

			int attempts = 50;
			T insertions = (T) selectFromGroup(this, random.nextDouble(), sum);
			while(attempts-- > 0 && insertions == base)
				insertions = (T) selectFromGroup(this, random.nextDouble(), sum);

			T child = (T) base.createClone(); // create clone child
			child.breed(insertions); // crossover with other selected T
			child.mutate(1); // mutate variables slightly

			nextGen.add(child);
		}
		// System.out.println(Arrays.toString(picks));

		// add top {transferPop} number of entities directly to the next generation
		for(int i = 0; i < elitismPopulation; i++) {
			T e = this.get(i);

			T child = (T) e.createClone();

			nextGen.add(child);
		}

		return nextGen;
	}

	public static GeneticCarrier selectFromGroup(List<? extends GeneticCarrier> entities, double rnd, double sum) {
		GeneticCarrier selected = null;
		double step = 0;
		for(GeneticCarrier e : entities) {
			step = step + e.getFitness() / sum;
			// System.out.println("rnd = " + rnd + " || step = " + step);
			if(rnd < step) {
				selected = e;
				break;
			}

		}

		return selected;
	}

	public ArrayList<GeneticCarrier> tournamentSelection(int population, double tournySizePercentage,
			double selectionCutOffPercentage,
			double elitismPercentage) {
		return tournamentSelection(this, population, tournySizePercentage, selectionCutOffPercentage,
				elitismPercentage, null, sortingSystem);
	}

	public ArrayList<GeneticCarrier> tournamentSelection(int population, double tournySizePercentage,
			double selectionCutOffPercentage,
			double elitismPercentage, Predicate<GeneticCarrier> selectionPredicate) {
		return tournamentSelection(this, population, tournySizePercentage, selectionCutOffPercentage,
				elitismPercentage, selectionPredicate, sortingSystem);
	}

	public static ArrayList<GeneticCarrier> tournamentSelection(List<? extends GeneticCarrier> source, int population,
			double tournySizePercentage, double selectionCutOffPercentage,
			double elitismPercentage, Predicate<GeneticCarrier> selectionPredicate, Comparator<GeneticCarrier> sorter) {
		ArrayList<GeneticCarrier> gen = new ArrayList<>(population);

		int cutoff = (int) Math.ceil(source.size() * selectionCutOffPercentage);

		if(source.isEmpty()) {
			throw new IllegalArgumentException("Source list is empty.");
		}

		// add elites from prior generation
		int elites = (int) Math.min(Math.floor(population * elitismPercentage), source.size());

		if(source.size() == 1) {
			elites = 0;
		}

		for(int i = 0; i < elites; i++) {
			gen.add(source.get(i).createClone());
		}

		// choose the top % to be in the breeding pool
		List<GeneticCarrier> potentials = new ArrayList<>(cutoff);
		for(int i = 0; i < cutoff; i++) {
			potentials.add(source.get(i));
		}

		List<GeneticCarrier> copy = new ArrayList<>(potentials);

		if(selectionPredicate != null) {
			potentials.removeIf(selectionPredicate);

			// if the predicate removed all valid potentials, ignore it
			if(potentials.size() == 0 && copy.size() > 0) {
				potentials = copy;
			}

		}

		int tournySize = (int) Math.max(potentials.size() * tournySizePercentage, 1);

		int[] picks = new int[potentials.size()];

		while(gen.size() < population) {
			// make a random list of entities
			List<GeneticCarrier> rl0 = (List<GeneticCarrier>) getRandomSubsection(potentials, tournySize);
			List<GeneticCarrier> rl1 = (List<GeneticCarrier>) getRandomSubsection(potentials, tournySize);

			GeneticCarrier base = bestFrom(rl0);
			picks[potentials.indexOf(base)]++;

			GeneticCarrier insert = bestFrom(rl1);

			GeneticCarrier child = base.breed(insert);
			child.mutate(1);

			gen.add(child);
		}

		double[] fitness = new double[source.size()];
		for(int i = 0; i < source.size(); i++) {
			fitness[i] = source.get(i).getFitness();
		}
		// System.out.println("picks: " + Arrays.toString(picks));
		// System.out.println("fitness: " + Arrays.toString(fitness));

		return gen;
	}

	private static GeneticCarrier bestFrom(List<GeneticCarrier> list) {
		double highestFitness = Double.MIN_VALUE;
		GeneticCarrier gc = null;

		for(GeneticCarrier g : list) {
			double f = g.getFitness();
			if(gc == null || f > highestFitness) {
				gc = g;
				highestFitness = f;
			}
		}

		return gc;

	}

	public static List<?> getRandomSubsection(List<?> potentials, int tournySize) {
		Random random = new Random();
		ArrayList<Object> list = new ArrayList<>(tournySize);

		for(int i = 0; i < tournySize; i++) {
			Object t = potentials.get(random.nextInt(potentials.size()));
			list.add(t);
		}

		return list;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public Color getColor() {
		return color;
	}

	public void scheduleAddition(T child) {
		scheduledAdditions.add(child);
	}

	@Override
	public void clear() {
		scheduledAdditions.clear();
		graveyard.clear();
		super.clear();
	}

	public void trimDeadEntities(int keepBestAmount) {

		removeNull();
		useSorter();

		// too lazy to do this better rn
		List<T> toRemove = new ArrayList<>();

		int popSize = this.size();
		// move dead entities to the graveyard
		for(int j = 0; j < popSize; j++) {
			T e = this.get(j);

			if(!e.isAlive()) {
				graveyard.add(e);
				toRemove.add(e);

				// randomize which ones are trimmed
				if(selection != Selection.NONE) {
					e.calculateFitness();
				}
				else {
					e.setFitness(1);
				}
			}
		}
		this.removeAll(toRemove);
		toRemove.clear();

		if(graveyard.size() < keepBestAmount) {
			return;
		}

		// sort by fitness
		if(selection != Selection.NONE) {
			// with no selection, we don't even sort
			graveyard.sort(sortingSystem);
		}

		int removed = 0;
		// remove all except the best dead ones
		for(int i = keepBestAmount; i < graveyard.size(); i++) {
			this.remove(graveyard.remove(i));
			removed++;
		}

		// System.out.printf(
		// "Trimming dead entities... %s total entities in memory (living and dead}, %s
		// were trimmed, and %s entities were kept in the deadpool. heh\n",
		// popSize + graveyard.size(), removed, graveyard.size());

	}

	public void removeNull() {
		for(int i = 0; i < size(); i++) {
			T t = get(i);
			if(t == null) {
				remove(i);
				i--;
			}
		}
	}

	public static final Comparator<GeneticCarrier> death_sorter = new Comparator<GeneticCarrier>() {

		@Override
		public int compare(GeneticCarrier o1, GeneticCarrier o2) {
			return (int) Math.signum(o1.getTimeOfDeath() - o2.getTimeOfDeath());
		}
	};

	public int getLivingPopulation() {
		int count = 0;

		for(T e : this) {
			if(e != null)
				if(e.isAlive()) {
					count++;
				}
		}

		return count;
	}

	public static enum Selection {
		ROULETTE, RANKED_ROULETTE, TOURNAMENT, NONE;
	}

	public static final Comparator<GeneticCarrier> HIGH_FITNESS = new Comparator<GeneticCarrier>() {

		@Override
		public int compare(GeneticCarrier o1, GeneticCarrier o2) {
			return o1.compare(o2);
		}
	};

	public void addDeadToPopulation() {
		this.addAll(graveyard);
		graveyard.clear();
	}

	public List<T> getDead() {
		return graveyard;
	}

	public Selection getSelection() {
		return selection;
	}

	public Map<String, Object> getValues() {
		return values;
	}

	public void setFitnessSorter(Comparator<GeneticCarrier> fitnessSorter) {
		this.sortingSystem = fitnessSorter;
	}

	public Comparator<GeneticCarrier> getFitnessSorter() {
		return sortingSystem;
	}

	public void useSorter() {
		useSorter(false);
	}

	public void useSorter(boolean reversed) {
		if(reversed) {
			this.sort(sortingSystem.reversed());
		}
		else {

			this.sort(sortingSystem);

			// System.out.println();
			// for(T t : this) {
			// System.out.println(t.getFitness());
			// }
		}
	}

	public int getDeadEntityCount() {
		int sum = 0;

		for(T t : this) {
			if(!t.isAlive()) {
				sum++;
			}
		}

		return sum;
	}

	public static List<GeneticCarrier> rouletteSelection(List<? extends GeneticCarrier> list, int amount,
			double selectionCutoff,
			double elitism, Comparator<GeneticCarrier> sorter) {

		list.sort(sorter);

		int cutOffPoint = (int) Math.ceil(list.size() * selectionCutoff);
		for(int i = cutOffPoint; i < list.size(); i++) {
			list.remove(i);
		}

		if(list.isEmpty()) {
			throw new IllegalArgumentException("Supplied list is empty.");
		}

		double lowest = 0; // positive values work, so we cap it at 0
		for(GeneticCarrier a : list) {
			if(a.getFitness() < lowest) {
				lowest = a.getFitness();
			}
		}

		double sum = 0;
		for(GeneticCarrier gc : list) {
			sum += Math.max(1, gc.getFitness() + lowest);
		}

		int[] picks = new int[list.size()];

		List<GeneticCarrier> result = new ArrayList<>();
		for(int i = 0; i < amount; i++) {
			GeneticCarrier base = null;
			GeneticCarrier insertion = null;

			double rnd = Math.random();
			double step = 0;
			for(GeneticCarrier gc : list) {
				step += Math.max(1, gc.getFitness() + lowest) / sum;

				if(step >= rnd) {
					base = gc;
					break;
				}
			}
			rnd = Math.random();
			step = 0;
			for(GeneticCarrier gc : list) {
				step += Math.max(1, gc.getFitness() + lowest) / sum;

				if(step >= rnd) {
					insertion = gc;
					break;
				}
			}

			if(base == null) {
				throw new IllegalArgumentException("Base is null.");
			}

			picks[list.indexOf(base)]++;

			double f = base.getFitness();
			base = base.createClone();
			base.setFitness(f);
			base = base.breed(insertion);
			base.setFitness(0);
			base.mutate(1.0f);

			result.add(base);

		}

		double[] fitness = new double[list.size()];
		for(int i = 0; i < list.size(); i++) {
			fitness[i] = list.get(i).getFitness();
		}
		// System.out.println("picks: " + Arrays.toString(picks));
		// System.out.println("fitness: " + Arrays.toString(fitness));

		return result;
	}

	public Collection<? extends T> getGraveyard() {
		return this.graveyard;
	}

}
