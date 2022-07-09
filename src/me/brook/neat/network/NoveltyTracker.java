package me.brook.neat.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import me.brook.neat.GeneticCarrier;

public class NoveltyTracker<T extends GeneticCarrier> {

	public LinkedHashMap<T, double[]> previousStates;
	private List<double[]> hardStates;

	public LinkedHashMap<T, double[]> hallOfFame;

	private double varianceThreshold = 0;

	private Random random;

	private boolean allowRepeats = true;

	public NoveltyTracker(double varianceThreshold) {
		this.varianceThreshold = varianceThreshold;
		previousStates = new LinkedHashMap<>();
		hallOfFame = new LinkedHashMap<>();
		hardStates = new ArrayList<>();

		random = new Random();
	}

	public double getDistanceToStates(double[] compare) {

		double sum = 0;

		for(double[] c2 : previousStates.values()) {
			double d = getDistance(compare, c2);

			sum += d;
		}

		for(double[] c2 : hardStates) {
			double d = getDistance(compare, c2);

			sum += d;
		}

		double d = (sum * (1.0 / (hardStates.size() + previousStates.size() + 1.0)));

		return d;
	}

	public double getDistanceToStates(T t, double[] noveltyMetrics) {

		double sum = 0;

		for(T t2 : previousStates.keySet()) {
			double[] c2 = previousStates.get(t2);

			if(t != null && t.getUUID().equals(t2.getUUID()))
				continue;

			double d = getDistance(noveltyMetrics, c2);

			sum += d;
		}

		for(double[] c2 : hardStates) {
			double d = getDistance(noveltyMetrics, c2);

			sum += d;
		}

		double d = (sum / (hardStates.size() + previousStates.size() + 1.0));

		return d;
	}

	public double getDistanceToClosestState(T id, double[] compare) {
		if(previousStates.isEmpty() && hardStates.isEmpty()) {
			return 1;
		}

		double closest = Double.MAX_VALUE;

		for(T key : previousStates.keySet()) {
			double[] c2 = previousStates.get(key);
			// if(doArraysMatch(compare, c2)) {
			// continue;
			// }
			if(key == id)
				continue;

			double d = getDistance(compare, c2);

			if(closest == Double.MAX_VALUE || d < closest) {
				closest = d;
			}

		}

		for(double[] c2 : hardStates) {
			double d = getDistance(compare, c2);

			if(closest == Double.MAX_VALUE || d < closest) {
				closest = d;
			}
		}

		return closest;

	}

	public double getDistance(double[] c1, double[] c2) {
		double sum = 0;

		for(int i = 0; i < c2.length; i++) {
			double d1 = 0, d2 = 0;

			if(c1.length > i) {
				d1 = c1[i];
			}
			if(c2.length > i) {
				d2 = c2[i];
			}

			sum += Math.abs(d1 - d2);
		}

		return sum;
	}

	public boolean submit(T id, double[] novelty) {
		if(novelty == null) {
			throw new IllegalArgumentException("Novelty metric is null.");
		}

		if(!previousStates.containsKey(id)) {

			// compare distance
			if(previousStates.size() == 0 || getDistanceToClosestState(id, novelty) >= varianceThreshold) {
				if(!allowRepeats) {
					// we're going allow new locations in hard states here
					boolean contains = contains(novelty, false);

					if(novelty[0] != 0)
						getHallOfFame();

					// these cant be combined
					if(!contains) {
						previousStates.put(id, novelty);
						return true;
					}
				}
				else {
					previousStates.put(id, novelty);
					return true;
				}
			}
		}

		return false;
	}

	public boolean contains(double[] novelty, boolean checkHardstates) {
		for(double[] state : previousStates.values()) {
			if(doArraysMatch(state, novelty)) {
				return true;
			}
		}
		if(checkHardstates)
			for(double[] state : hardStates) {
				if(doArraysMatch(state, novelty)) {
					return true;
				}
			}

		return false;
	}

	public boolean submit(T id, double value) {
		if(!Double.isFinite(value)) {
			throw new IllegalArgumentException("Novelty metric isn't finite.");
		}

		double[] array = { value };

		return submit(id, array);
	}

	// hall of fame stuff

	public void findAndStoreMostDiverse(List<T> list) {

		T closest = null;
		double distance = 0;

		for(T t : list) {
			double d = getDistanceToClosestState(t, t.getNoveltyMetrics());

			if(closest == null || d > distance) {
				closest = t;
				distance = d;
			}

		}

		if(closest != null) {
			hallOfFame.put(closest, closest.getNoveltyMetrics());
			sortHallOfFame();
		}

	}

	public LinkedHashMap<T, Double> sortByValue(LinkedHashMap<T, Double> hm, boolean reversed) {
		// Create a list from elements of HashMap
		List<Map.Entry<T, Double>> list = new LinkedList<Map.Entry<T, Double>>(hm.entrySet());

		Comparator<Map.Entry<T, Double>> comp = new Comparator<Map.Entry<T, Double>>() {
			public int compare(Map.Entry<T, Double> o1,
					Map.Entry<T, Double> o2) {
				return (o1.getValue()).compareTo(o2.getValue());
			}
		};

		if(reversed) {
			comp = comp.reversed();
		}

		// Sort the list
		Collections.sort(list, comp);

		// put data from sorted list to hashmap
		LinkedHashMap<T, Double> temp = new LinkedHashMap<T, Double>();
		for(Map.Entry<T, Double> aa : list) {
			temp.put(aa.getKey(), aa.getValue());
		}

		return temp;
	}

	private void sortHallOfFame() {

		LinkedHashMap<T, Double> valued = new LinkedHashMap<>();

		for(T t : hallOfFame.keySet()) {
			double d = getDistanceToClosestState(t, hallOfFame.get(t));

			valued.put(t, d);
		}

		LinkedHashMap<T, Double> sorted = sortByValue(valued, true);

		LinkedHashMap<T, double[]> replacement = new LinkedHashMap<>();

		for(T t : sorted.keySet()) {
			replacement.put(t, hallOfFame.get(t));
		}

		hallOfFame = replacement;
		// System.out.println(sorted.toString());
	}

	public class HofInfo {
		T t;
		double distance;
	}

	public T getRandomHallOfFameEntry(double threshold) {
		return new ArrayList<>(hallOfFame.keySet()).get((int) (random.nextDouble() * hallOfFame.size() * threshold));
	}

	// getters and setters

	public LinkedHashMap<T, double[]> getHallOfFame() {
		return hallOfFame;
	}

	/*
	 * Compare the values of the arrays instead of the objects.
	 */
	private boolean doArraysMatch(double[] c1, double[] c2) {
		if(c1.length != c2.length)
			return false;

		for(int i = 0; i < c2.length; i++)
			if(c1[i] != c2[i])
				return false;

		return true;
	}

	public Map<T, double[]> getPreviousStates() {
		return previousStates;
	}

	public String getString() {

		StringBuilder sb = new StringBuilder();

		List<double[]> list = new ArrayList<>(previousStates.values());
		list.sort(new Comparator<double[]>() {

			@Override
			public int compare(double[] o1, double[] o2) {
				return (int) Math.signum(o1[0] - o2[0]);
			}
		});

		for(double[] d : list) {
			sb.append(Arrays.toString(d) + ", ");
		}

		return sb.toString();
	}

	public void setAllowRepeats(boolean allowRepeats) {
		this.allowRepeats = allowRepeats;
	}

	public void clear() {
		this.hallOfFame.clear();
		this.hardStates.clear();
		this.previousStates.clear();
	}

	public void addHardState(double[] state) {
		this.hardStates.add(state);
	}

}
