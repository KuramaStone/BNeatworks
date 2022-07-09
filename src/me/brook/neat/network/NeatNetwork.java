package me.brook.neat.network;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import me.brook.neat.Genetics;
import me.brook.neat.network.Phenotype.Pheno;
import me.brook.neat.randomizer.NetworkRandomizer;
import me.brook.neat.species.InnovationHistory;

/**
 * 
 * @author Brookie
 * @apiNote I just think theyre neat.
 * 
 *
 */
public class NeatNetwork implements Serializable, Genetics {

	private static DecimalFormat df = new DecimalFormat("#.####");

	private static final long serialVersionUID = 7712364365249269449L;

	private transient InnovationHistory innovationHistory;

	private Phenotype phenotype;
	private transient Random random = new Random();
	private boolean hasMemory;

	private NeatTransferFunction externalFunction;
	private NeatTransferFunction internalFunction;
	private List<NeatTransferFunction> possibleFunctions;

	private LinkedHashMap<Integer, NeatNeuron> totalNeurons, inputNeurons, outputNeurons, hiddenNeurons;
	private NeatNeuron bias;

	private LinkedHashMap<Integer, GConnection> connections;

	private List<List<NeatNeuron>> layers;

	public NeatNetwork(InnovationHistory innovationHistory, int externalFunctionID,
			int defaultFunctionID, Phenotype phenotype,
			boolean hasMemory, List<NeatTransferFunction> possibleFunctions, int... neurons) {
		this.innovationHistory = innovationHistory;
		this.externalFunction = possibleFunctions.get(externalFunctionID);
		this.internalFunction = possibleFunctions.get(defaultFunctionID);
		this.phenotype = phenotype;
		this.hasMemory = hasMemory;
		random = new Random();
		this.possibleFunctions = possibleFunctions;

		if(neurons == null || neurons[0] == -1)
			return;

		totalNeurons = new LinkedHashMap<>();
		inputNeurons = new LinkedHashMap<>(neurons[0]);
		outputNeurons = new LinkedHashMap<>(neurons[neurons.length - 1]);

		int total = 0;
		for(int i = 0; i < neurons.length; i++) {
			int j = neurons[i];
			total += j;
		}

		hiddenNeurons = new LinkedHashMap<>(total);

		connections = new LinkedHashMap<>();

		if(neurons != null && neurons.length != 0) {
			create(neurons);
			recalculateLayers();
		}

	}

	public NeatNetwork(InnovationHistory innovationHistory, NeatTransferFunction externalFunction,
			NeatTransferFunction defaultFunction, Phenotype phenotype,
			boolean hasMemory, int... neurons) {
		this.innovationHistory = innovationHistory;
		this.externalFunction = externalFunction;
		this.internalFunction = defaultFunction;
		this.phenotype = phenotype;
		this.hasMemory = hasMemory;
		random = new Random();
		if(possibleFunctions == null) {
			possibleFunctions = new ArrayList<>();
			possibleFunctions.add(defaultFunction);
		}

		if(neurons == null || neurons[0] == -1)
			return;

		totalNeurons = new LinkedHashMap<>();
		inputNeurons = new LinkedHashMap<>(neurons[0]);
		outputNeurons = new LinkedHashMap<>(neurons[neurons.length - 1]);

		int total = 0;
		for(int i = 0; i < neurons.length; i++) {
			int j = neurons[i];
			total += j;
		}

		hiddenNeurons = new LinkedHashMap<>(total);

		connections = new LinkedHashMap<>();

		if(neurons != null && neurons.length != 0) {
			create(neurons);
			recalculateLayers();
		}

	}

	public NeatNetwork() {
	}

	private void recalculateLayers() {
		layers = new ArrayList<>();

		int layer = 0;
		List<NeatNeuron> l = this.getNeuronsWithLayerIndex(layer++);

		while(!l.isEmpty()) {
			layers.add(l);
			l = this.getNeuronsWithLayerIndex(layer++);
		}
	}

	/*
	 * Split a random connection into two connections, a disabled connection, and a new network. Then increment the layerIndex of all other neurons
	 */
	public boolean generateNeuron(int attemptsRemaining) {
		if(totalNeurons.size() < 2) {
			throw new IllegalArgumentException("There are not enough neurons to form a connection.");
		}

		if(attemptsRemaining <= 0) {
			System.out.println("failed to generate neuron");
			return false;
		}

		// get random connection and disable it
		GConnection randomConnection = getRandomConnection();
		randomConnection.setEnabled(false);

		int id = 0;
		// create intermediate neuron
		NeatNeuron neuron = new NeatNeuron(this, innovationHistory.getNextRawInnovation(), id, 0,
				hasMemory);
		neuron.setLabel("hidden");

		// create two connections
		GConnection toIntermediary = new GConnection(
				innovationHistory.getNextInnovation(randomConnection.getFromNeuron(), neuron.getNeuronID()),
				randomConnection.getFromNeuron(),
				neuron.getNeuronID(), randomConnection.getWeight());

		GConnection fromIntermediary = new GConnection(
				innovationHistory.getNextInnovation(neuron.getNeuronID(), randomConnection.getToNeuron()),
				neuron.getNeuronID(),
				randomConnection.getToNeuron(), 1);

		// add new items to lists
		connections.put(toIntermediary.getInnovationNumber(), toIntermediary);
		connections.put(fromIntermediary.getInnovationNumber(), fromIntermediary);
		totalNeurons.put(neuron.getNeuronID(), neuron);
		hiddenNeurons.put(neuron.getNeuronID(), neuron);

		// add connections to neurons
		getNeuronByID(toIntermediary.getToNeuron()).addInputConnection(this, getNeuronByID(toIntermediary.getFromNeuron()),
				toIntermediary.getInnovationNumber());
		getNeuronByID(fromIntermediary.getToNeuron()).addInputConnection(this, neuron,
				fromIntermediary.getInnovationNumber());

		// if there aren't any layers between the initial from/to neurons, increase the
		// layer index of all neurons equal to or greater than the to neuron
		int n1Layer = getNeuronByID(randomConnection.getFromNeuron()).layerIndex;
		int n2Layer = getNeuronByID(randomConnection.getToNeuron()).layerIndex;

		// if it is bigger than one, then there are already layers to just become part
		// of
		if(n2Layer - n1Layer == 1) {

			for(NeatNeuron n : totalNeurons.values()) {
				if(n != neuron) {
					if(n.layerIndex >= n2Layer) {
						n.layerIndex++;
					}
				}
			}

			neuron.layerIndex = n1Layer + 1;
		}
		else {
			neuron.layerIndex = n1Layer + 1;
		}
		deleteConnection(randomConnection);
		toIntermediary.assignNetwork(this);
		fromIntermediary.assignNetwork(this);

		recalculateLayers();

		return true;
	}

	public NeatNeuron getNeuronByID(int id) {
		return totalNeurons.get(id);
	}

	private GConnection getRandomConnection() {
		int index = random.nextInt(connections.size());

		int i = 0;
		for(GConnection conn : connections.values()) {

			if(i++ == index) {
				return conn;
			}

		}

		return null;
	}

	/*
	 * Get two random neurons and connect them
	 */
	public GConnection generateConnection(int attemptsRemaining) {
		if(totalNeurons.size() < 2) {
			throw new IllegalArgumentException("There are not enough neurons to form a connection.");
		}

		if(attemptsRemaining <= 0) {
			return null;
		}

		NeatNeuron n1 = new ArrayList<>(totalNeurons.values()).get(random.nextInt(totalNeurons.size()));
		NeatNeuron n2 = new ArrayList<>(totalNeurons.values()).get(random.nextInt(totalNeurons.size()));

		// don't pick the same neuron
		if(n1 == n2) {
			return generateConnection(attemptsRemaining - 1);
		}

		// don't pick neurons on same layer
		if(n1.layerIndex >= n2.layerIndex) {
			return generateConnection(attemptsRemaining - 1);
		}

		// don't connect already connected neurons
		if(n2.hasInputConnectionFrom(this, n1)) {
			return generateConnection(attemptsRemaining - 1);
		}

		// do not connect to bias neurons, only from
		if(n2.isBiasNeuron()) {
			return generateConnection(attemptsRemaining - 1);
		}

		GConnection gconn = new GConnection(innovationHistory.getNextInnovation(n1.getNeuronID(), n2.getNeuronID()),
				n1.getNeuronID(), n2.getNeuronID(),
				nextDouble() * 2 - 1);
		// System.out.println(gconn.getWeight());
		connections.put(gconn.getInnovationNumber(), gconn);
		n2.addInputConnection(this, getNeuronByID(gconn.getFromNeuron()), gconn.getInnovationNumber());

		gconn.assignNetwork(this);

		if(gconn.getTo() == null) {
			System.err.println("mutation: using null neuron??");
		}

		attemptsRemaining = 0;

		return gconn;
	}

	private double nextDouble() {
		random.setSeed(System.nanoTime());
		return random.nextDouble();
	}

	private void create(int[] neurons) {

		for(int n = 0; n < neurons[0]; n++) {
			NeatNeuron neuron = new NeatNeuron(this, innovationHistory.getNextRawInnovation(), -1, 0,
					hasMemory);
			neuron.layerIndex = 0;
			neuron.setLabel("input-" + n);
			neuron.setShouldCalculate(false);
			inputNeurons.put(neuron.getNeuronID(), neuron);
			totalNeurons.put(neuron.getNeuronID(), neuron);
		}

		bias = new NeatNeuron(this, innovationHistory.getNextRawInnovation(), -1, 0, false);
		bias.layerIndex = 0;
		bias.setBiasNeuron(true);
		bias.setLabel("bias");
		bias.setShouldCalculate(false);
		totalNeurons.put(bias.getNeuronID(), bias);

		for(int n = 0; n < neurons[neurons.length - 1]; n++) {
			NeatNeuron neuron = new NeatNeuron(this, innovationHistory.getNextRawInnovation(), -2, 0,
					hasMemory);
			neuron.layerIndex = neurons.length - 1;
			neuron.setLabel("out-" + n);
			outputNeurons.put(neuron.getNeuronID(), neuron);
			totalNeurons.put(neuron.getNeuronID(), neuron);
		}

		for(int i = 1; i < neurons.length - 1; i++) {
			for(int j = 0; j < neurons[i]; j++) {
				NeatNeuron neuron = new NeatNeuron(this, innovationHistory.getNextRawInnovation(), 0, 0,
						hasMemory);
				neuron.layerIndex = i;
				neuron.setLabel("hidden");
				hiddenNeurons.put(neuron.getNeuronID(), neuron);
				totalNeurons.put(neuron.getNeuronID(), neuron);
			}
		}
	}

	public void calculate() {

		for(int i = 1; i < layers.size(); i++) {
			layers.get(i).forEach(neuron -> {
				neuron.calculate(this);
			});
		}

	}

	public double[] getOutput() {
		double[] output = new double[outputNeurons.size()];

		List<NeatNeuron> layer = new ArrayList<>(outputNeurons.values());
		for(int n = 0; n < output.length; n++) {
			output[n] = layer.get(n).getOutput();
		}

		return output;
	}

	public void setInputs(double[] inputs) {
		if(inputs.length != inputNeurons.size()) {
			throw new IllegalArgumentException(String.format(
					"Input array size of %s does not match the network's inputs of %s", inputs.length, inputNeurons.size()));
		}

		List<NeatNeuron> neurons = new ArrayList<>(inputNeurons.values());
		for(int n = 0; n < inputNeurons.size(); n++) {
			NeatNeuron neuron = neurons.get(n);
			neuron.setOutput(inputs[n]);
			neuron.setInput(inputs[n]);
		}

		// for(NeatNeuron nn : this.totalNeurons.values()) {
		// if(nn.layerIndex != 0) {
		// nn.setInput(0);
		// nn.setOutput(0);
		// }
		//
		// }

	}

	/**
	 * 
	 * @param weightGainChance
	 *            Chance to gain a weight connection between two random neurons that are not backwards facing.
	 * @param weightLossChance
	 *            Chance to lose a random weight connection.
	 * @param weightSwapChance
	 *            Chance for a weight connection to change to a different neuron
	 * @param g
	 * @param
	 * @param neuronGainChance
	 *            Chance to gain a neuron
	 * @param neuronLossChance
	 *            Chance to lose a neuron
	 */

	public NeatNetwork copy() throws Exception {
		NeatNetwork copy = new NeatNetwork(this.innovationHistory, this.externalFunction, this.internalFunction,
				new Phenotype(), hasMemory, -1);

		copy.inputNeurons = new LinkedHashMap<>(this.inputNeurons.size());
		copy.hiddenNeurons = new LinkedHashMap<>(this.hiddenNeurons.size());
		copy.outputNeurons = new LinkedHashMap<>(this.outputNeurons.size());
		copy.totalNeurons = new LinkedHashMap<>(
				this.inputNeurons.size() + this.hiddenNeurons.size() + this.outputNeurons.size());

		/*
		 * First, we copy the neuron structure and ids. Then we connect them with properly copied variants of the weights
		 */
		for(NeatNeuron neuron : this.inputNeurons.values()) {
			copy.inputNeurons.put(neuron.getNeuronID(), neuron.copy(this));
		}
		for(NeatNeuron neuron : this.hiddenNeurons.values()) {
			copy.hiddenNeurons.put(neuron.getNeuronID(), neuron.copy(this));
		}
		for(NeatNeuron neuron : this.outputNeurons.values()) {
			copy.outputNeurons.put(neuron.getNeuronID(), neuron.copy(this));
		}

		copy.totalNeurons.putAll(copy.inputNeurons);
		copy.totalNeurons.putAll(copy.hiddenNeurons);
		copy.totalNeurons.putAll(copy.outputNeurons);
		copy.bias = this.bias.copy(this);
		copy.totalNeurons.put(copy.bias.getNeuronID(), copy.bias);

		copy.connections = new LinkedHashMap<>(this.connections.size());
		for(Entry<Integer, GConnection> set : this.connections.entrySet()) {
			copy.connections.put(set.getKey(), set.getValue().copy());
		}

		copy.totalNeurons.values().forEach(neuron -> neuron.assignNetwork(copy));

		copy.phenotype = this.phenotype.copy();
		copy.recalculateLayers();

		copy.resetMemory();

		return copy;
	}

	public Phenotype getPhenotype() {
		return phenotype;
	}

	public BufferedImage getImage(int width, int height) {
		return getImage(width, height, false);
	}

	public BufferedImage getImage(int width, int height, boolean drawWeights) {
		DecimalFormat df = new DecimalFormat("#.#####");

		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = (Graphics2D) image.getGraphics();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setStroke(new BasicStroke(3f));

		g2.setColor(Color.WHITE);
		g2.fillRect(0, 0, width, height);

		int r = 15;
		int layerGap = (image.getWidth() - r * 2) / (this.getLayersCount() - 1 + 1);

		for(int layerIndex = 0; layerIndex < this.getLayersCount(); layerIndex++) {

			// draw all the neurons in this layer
			int x = r + layerIndex * layerGap;

			List<NeatNeuron> layer = this.layers.get(layerIndex);
			// layer.removeIf(n -> n.getInputConnections().isEmpty() && n.getOutputConnections().isEmpty());

			double neuronGap = (((double) image.getHeight()) - r * 2.0) / (layer.size() + 2.0);
			for(int neuronIndex = 0; neuronIndex < layer.size(); neuronIndex++) {
				int y = (int) Math.round(r + neuronIndex * neuronGap + ((height / layer.size()) / 2));

				// draw connection to future neurons

				List<GConnection> outConnections = layer.get(neuronIndex).getOutputConnections();

				for(GConnection conn : outConnections) {

					NeatNeuron toNeuron = getNeuronByID(conn.getToNeuron());
					int x1 = r + (toNeuron.layerIndex) * layerGap;

					List<NeatNeuron> next = this.layers.get(toNeuron.layerIndex);
					// next.removeIf(n -> n.getInputConnections().isEmpty() && n.getOutputConnections().isEmpty());
					if(next.isEmpty()) {
						continue;
					}

					int y1 = r + next.indexOf(getNeuronByID(conn.getToNeuron()))
							* ((image.getHeight() - r * 2) / (next.size() + 2));

					y1 += (height / next.size()) / 2;

					float w = (float) conn.getWeight();

					if(drawWeights) {
						int x2 = (x + x1) / 2;
						int y2 = (y + y1) / 2;
						g2.setColor(Color.BLACK);
						g2.drawString(df.format(conn.getWeight()), x2, y2);
					}

					g2.setColor(interpolateColor(Color.RED, Color.BLUE, Math.min(1, Math.max((w + 1) / 2, -1))));

					if(!conn.isEnabled()) {
						g2.setColor(Color.BLACK);
					}

					g2.drawLine(x, y, x1, y1);
				}
				g2.setColor(layer.get(neuronIndex).isBiasNeuron() ? Color.GREEN : Color.BLACK);
				// draw neuron
				g2.fillOval(x - r / 2, y - r / 2, r, r);

				g2.setColor(Color.BLACK);
				g2.drawString(df.format(layer.get(neuronIndex).getOutput()), x + r * 2, y);

				// if(layer.get(neuronIndex).hasMemory())
				// g2.drawString("m: " + df.format(layer.get(neuronIndex).getStoredMemory()), x + r * 2,
				// y + g2.getFontMetrics().getHeight());
				if(layer.get(neuronIndex).getLabel() != null)
					g2.drawString(layer.get(neuronIndex).getLabel(), x + r * 2,
							y + g2.getFontMetrics().getHeight());

				if(layer.get(neuronIndex).getTransferID() >= 0)
					g2.drawString(layer.get(neuronIndex).getTransferID() + "", x + r * 2,
							y + g2.getFontMetrics().getHeight() * 2);
			}

		}

		g2.setColor(Color.BLACK);
		g2.setFont(new Font("verdana", Font.BOLD, 12));

		// draw phenotype
		int y = height - r * 2;
		int index = 0;
		for(Entry<String, Pheno> set : phenotype.getPhenotypes().entrySet()) {
			int x = r + ((width - r * 2) / phenotype.getPhenotypes().size()) * index++;

			g2.drawString(String.valueOf(set.getKey()), x, y - g2.getFontMetrics().getHeight());
			g2.drawString(df.format(set.getValue().getValue()), x, y);
		}

		return image;
	}

	public List<GConnection> getConnectionListFromInnovationNumbers(NeatNetwork network, List<Integer> ids) {
		List<GConnection> list = new ArrayList<GConnection>(ids.size());

		for(int id : ids) {
			GConnection conn = getConnectionFromInnovationID(id);
			conn.assignNetwork(network);
			list.add(conn);
		}

		return list;
	}

	private List<NeatNeuron> getNeuronsWithLayerIndex(int layer) {
		List<NeatNeuron> list = new ArrayList<>(this.totalNeurons.values());

		list.removeIf(n -> n.layerIndex != layer);

		return list;
	}

	public int getLayersCount() {
		int highest = 0;

		for(NeatNeuron nn : this.totalNeurons.values()) {
			if(nn.layerIndex > highest) {
				highest = nn.layerIndex;
			}
		}

		return highest + 1;
	}

	private Color interpolateColor(Color COLOR1, Color COLOR2, float fraction) {
		final float INT_TO_FLOAT_CONST = 1f / 255f;
		fraction = Math.min(fraction, 1f);
		fraction = Math.max(fraction, 0f);

		final float RED1 = COLOR1.getRed() * INT_TO_FLOAT_CONST;
		final float GREEN1 = COLOR1.getGreen() * INT_TO_FLOAT_CONST;
		final float BLUE1 = COLOR1.getBlue() * INT_TO_FLOAT_CONST;
		final float ALPHA1 = COLOR1.getAlpha() * INT_TO_FLOAT_CONST;

		final float RED2 = COLOR2.getRed() * INT_TO_FLOAT_CONST;
		final float GREEN2 = COLOR2.getGreen() * INT_TO_FLOAT_CONST;
		final float BLUE2 = COLOR2.getBlue() * INT_TO_FLOAT_CONST;
		final float ALPHA2 = COLOR2.getAlpha() * INT_TO_FLOAT_CONST;

		final float DELTA_RED = RED2 - RED1;
		final float DELTA_GREEN = GREEN2 - GREEN1;
		final float DELTA_BLUE = BLUE2 - BLUE1;
		final float DELTA_ALPHA = ALPHA2 - ALPHA1;

		float red = RED1 + (DELTA_RED * fraction);
		float green = GREEN1 + (DELTA_GREEN * fraction);
		float blue = BLUE1 + (DELTA_BLUE * fraction);
		float alpha = ALPHA1 + (DELTA_ALPHA * fraction);

		red = Math.min(red, 1f);
		red = Math.max(red, 0f);
		green = Math.min(green, 1f);
		green = Math.max(green, 0f);
		blue = Math.min(blue, 1f);
		blue = Math.max(blue, 0f);
		alpha = Math.min(alpha, 1f);
		alpha = Math.max(alpha, 0f);

		return new Color(red, green, blue, alpha);
	}

	public LinkedHashMap<Integer, GConnection> getConnections() {
		return connections;
	}

	/*
	 * starting values: c1= 1.0 c2 = 0.4 A general threshold can be 1.0
	 */
	public double getDistanceFrom(NeatNetwork compare, double c1, double c2, double c3) {
		int disjointAndExcessGenes = 0;
		double weightDiff = 0;
		double phenotypeDiff = 0;

		Map<Integer, GConnection> gcMap1 = this.getConnections();
		Map<Integer, GConnection> gcMap2 = compare.getConnections();

		List<Integer> allGeneIDs = new ArrayList<>();
		allGeneIDs.addAll(gcMap1.keySet());
		allGeneIDs.addAll(gcMap2.keySet());

		// useinnovation for gene alignment
		for(int inno : allGeneIDs) {
			GConnection gc1 = gcMap1.get(inno);
			GConnection gc2 = gcMap2.get(inno);

			// this gene matches!
			if(gc1 != null && gc2 != null) {
				weightDiff += Math.abs(gc1.getWeight() - gc2.getWeight());
			}
			else {
				disjointAndExcessGenes++;
			}

		}

		int N2 = this.getPhenotype().getPhenotypes().size();
		// compare phenotypes
		for(Entry<String, Pheno> set : this.getPhenotype().getPhenotypes().entrySet()) {
			phenotypeDiff += Math.abs(set.getValue().getNormalizedValue()
					- compare.getPhenotype().getPhenotype(set.getKey()).getNormalizedValue());
		}

		int N = Math.max(Math.max(gcMap1.size(), gcMap2.size()), 1);
		double ab = (disjointAndExcessGenes * c1) / (N);
		double c = (weightDiff * c2) / N;

		double d = (phenotypeDiff * c3) / Math.max(N2, 1);

		double sum = ab + c + d;

		return sum;
	}

	public void setWeightOf(int fromID, int toID, double weight) {
		NeatNeuron from = getNeuronByID(fromID);
		List<GConnection> list = from.getOutputConnections();
		for(GConnection conn : list) {
			if(conn.getToNeuron() == toID) {
				conn.setWeight(weight);
				return;
			}
		}

		// connection doesnt exist so connect
		GConnection conn = new GConnection(innovationHistory.getNextInnovation(fromID, toID), fromID, toID, weight);
		this.connections.put(conn.getInnovationNumber(), conn);

		getNeuronByID(toID).addInputConnection(this, from, conn.getInnovationNumber());

	}

	public void save(ThreadLocal<Kryo> kryoLocal, String absolutePath) throws Exception {
		FileOutputStream fos = new FileOutputStream(new File(absolutePath));
		Output output = new Output(fos);
		kryoLocal.get().writeClassAndObject(output, this);

		output.close();
		output.flush();
		fos.close();
	}

	public static NeatNetwork load(ThreadLocal<Kryo> kryoLocal, String filePath) throws Exception {

		try {
			File file = new File(filePath);
			if(!file.exists()) {
				throw new FileNotFoundException("Cannot find file: " + filePath);
			}

			Input input = new Input(new FileInputStream(filePath));

			Object ob = kryoLocal.get().readClassAndObject(input);
			input.close();
			NeatNetwork network = (NeatNetwork) ob;
			network.totalNeurons.values().forEach(n -> n.assignNetwork(network));
			network.connections.values().forEach(c -> c.assignNetwork(network));

			return network;

		}
		catch(IOException ioe) {
			ioe.printStackTrace();
			throw new Exception("Could not read neural network file!", ioe);
		}
	}

	public List<NeatTransferFunction> getPossibleFunctions() {
		return possibleFunctions;
	}

	public void randomizeWeights(NetworkRandomizer randomizer) {
		randomizer.randomize(this);
	}

	public Map<Integer, NeatNeuron> getTotalNeurons() {
		return totalNeurons;
	}

	public Map<Integer, NeatNeuron> getInputNeurons() {
		return inputNeurons;
	}

	public Map<Integer, NeatNeuron> getOutputNeurons() {
		return outputNeurons;
	}

	public void mutateNetwork(double addConnectionChance, double toggleConnectionChance,
			double deleteConnectionChance, double addNeuronChance, double deleteNeuronChance, NetworkRandomizer randomizer,
			double activationMagnitude, double activationChangeChance, double activationValueChance) {

		if(nextDouble() < addConnectionChance) {
			generateConnection(100);
		}

		if(this.connections.size() > 0) {

			for(int i = 0; i < hiddenNeurons.size() + 1; i++) {
				if(nextDouble() < addNeuronChance) {
					generateNeuron(100);
				}
				if(nextDouble() < deleteNeuronChance) {
					deleteNeuron(100);
				}

				if(!hiddenNeurons.isEmpty()) {
					NeatNeuron nn = this.hiddenNeurons.get(random.nextInt(this.hiddenNeurons.size()));
					if(nn != null)
						nn.mutateActivation(this, activationMagnitude, activationValueChance, activationChangeChance);
				}
			}
			for(int i = 0; i < connections.size(); i++) {

				if(nextDouble() < toggleConnectionChance) {
					toggleConnection();
				}
				if(nextDouble() < deleteConnectionChance) {
					deleteConnection(100);
				}

			}

			if(randomizer != null) {
				this.randomizeWeights(randomizer);
			}
		}

	}

	private void deleteNeuron(int attempt) {
		if(attempt-- <= 0) {
			return;
		}

		List<NeatNeuron> list = new ArrayList<>(hiddenNeurons.values());
		if(list.isEmpty()) { // no neurons to delete
			return;
		}
		// get random neuron
		NeatNeuron n1 = list.get(random.nextInt(list.size()));

		deleteNeuron(n1);

	}

	private void deleteNeuron(NeatNeuron n1) {
		// delete all connections
		for(GConnection gc : List.copyOf(n1.getInputConnections())) {
			getNeuronByID(gc.getFromNeuron()).removeConnection(gc);
			getNeuronByID(gc.getToNeuron()).removeConnection(gc);
			this.connections.remove(gc.getInnovationNumber());
		}
		for(GConnection gc : List.copyOf(n1.getOutputConnections())) {
			getNeuronByID(gc.getFromNeuron()).removeConnection(gc);
			getNeuronByID(gc.getToNeuron()).removeConnection(gc);
			this.connections.remove(gc.getInnovationNumber());
		}

		this.totalNeurons.remove(n1.getNeuronID());
		this.hiddenNeurons.remove(n1.getNeuronID());

		// if the layer is not empty, no need to fix later layers
		if(!getNeuronsWithLayerIndex(n1.layerIndex).isEmpty()) {
			return;
		}

		// now fix layer issues. reduce layer count of all neurons after this random one
		for(NeatNeuron nn : this.totalNeurons.values()) {
			if(nn.layerIndex > n1.layerIndex) {
				nn.layerIndex--;
			}
		}
	}

	private void deleteConnection(int attempt) {
		if(attempt-- <= 0) {
			return;
		}

		GConnection conn = getRandomConnection();
		deleteConnection(conn);
	}

	private void toggleConnection() {
		GConnection conn = getRandomConnection();
		conn.setEnabled(!conn.isEnabled());
	}

	public NeatNetwork connectAllLayers() {
		int layerIndex = 0;

		for(layerIndex = 0; layerIndex < layers.size() - 1; layerIndex++) {
			List<NeatNeuron> layer = layers.get(layerIndex);

			for(NeatNeuron current : layer) {
				if(current == bias) {
					continue;
				}
				// add a connection between this neuron and all of the next layer
				for(NeatNeuron next : layers.get(layerIndex + 1)) {
					if(!next.hasInputConnectionFrom(this, current)) {
						GConnection conn = new GConnection(
								innovationHistory.getNextInnovation(current.getNeuronID(), next.getNeuronID()),
								current.getNeuronID(), next.getNeuronID(), 0);

						connections.put(conn.getInnovationNumber(), conn);
						next.addInputConnection(this, getNeuronByID(conn.getFromNeuron()), conn.getInnovationNumber());
					}
				}
			}

		}

		for(NeatNeuron n : totalNeurons.values()) {
			if(n.layerIndex != 0) {
				if(!n.isBiasNeuron()) {
					GConnection conn = new GConnection(
							innovationHistory.getNextInnovation(bias.getNeuronID(), n.getNeuronID()), bias.getNeuronID(),
							n.getNeuronID(), 0);
					connections.put(conn.getInnovationNumber(), conn);
					n.addInputConnection(this, getNeuronByID(conn.getFromNeuron()), conn.getInnovationNumber());

				}
			}
		}

		totalNeurons.values().forEach(neuron -> neuron.getOutputConnections().forEach(gconn -> gconn.assignNetwork(this)));

		return this;
	}

	public InnovationHistory getInnovationHistory() {
		return innovationHistory;
	}

	public GConnection getConnectionFromInnovationID(int id) {

		if(this.connections.containsKey(id)) {
			return this.connections.get(id);
		}

		throw new IllegalArgumentException("Innovation number's corresponding connection not found.");
	}

	public void labelInputs(String... str) {
		if(str.length < inputNeurons.size())
			throw new IllegalArgumentException("Input name label array shorter than outputs.");

		int index = 0;
		for(NeatNeuron in : inputNeurons.values()) {
			in.setLabel(str[index++]);
		}

	}

	public void labelOutputs(String... str) {
		if(str.length < outputNeurons.size())
			throw new IllegalArgumentException("Output name label array shorter than outputs.");

		int index = 0;
		for(NeatNeuron in : outputNeurons.values()) {
			in.setLabel(str[index++]);
		}

	}

	public void resetMemory() {
		// for(NeatNeuron nn : totalNeurons) {
		// nn.resetMemory();
		// }
	}

	public List<List<NeatNeuron>> getLayers() {
		return layers;
	}

	public NeatTransferFunction getTransferFunction() {
		return externalFunction;
	}

	public void setConnections(LinkedHashMap<Integer, GConnection> connections) {
		this.connections = connections;
	}

	/**
	 * 
	 * @param mutations
	 *            number of mutations to apply
	 * @param mutationMagnitude
	 *            how much to mutate the value by
	 * @param chanceForFlip
	 *            ratio that it will flip the positive/negative value
	 * @param chanceToChange
	 *            ratio that it will modify the value
	 * @param chanceToDelete
	 *            ratio that it will delete the connection
	 * @param randomizer
	 */
	public void mutateConnectionsLimited(int mutations, double mutationMagnitude, double chanceToToggle,
			double chanceForFlip,
			double chanceToChange, double chanceToCreate, double chanceToDelete, NetworkRandomizer randomizer) {

		double sum = chanceForFlip + chanceToChange + chanceToCreate + chanceToDelete + chanceToToggle;
		double flip = chanceForFlip / sum;
		double toggle = chanceToToggle / sum;
		double change = chanceToChange / sum;
		double create = chanceToCreate / sum;
		// double del = chanceToDelete / sum; // this will be the default if none of the others make are chosen

		double goal = random.nextDouble();
		double line = 0;

		GConnection conn = null;
		if(!connections.isEmpty()) { // no connection to mutate
			conn = getRandomConnection();

			if(goal <= (line += flip)) {
				// flip value
				conn.setWeight(conn.getWeight() * -1.0);
			}
			else if(goal <= (line += toggle)) {
				conn = getRandomConnection();
				conn.setEnabled(!conn.isEnabled());
			}
			else if(goal <= (line += change)) {
				// mutate by magnitude
				// double diff = ((random.nextDouble() * 2) - 1) * mutationMagnitude;
				// conn.setWeight(conn.getWeight() + diff);
				randomizeWeights(randomizer);
			}
			else if(goal <= (line += create)) {
				// create
				generateConnection(100);
			}
			else {
				// delete
				deleteConnection(conn);
			}
		}
		else {
			generateConnection(100); // only allow creation if no others exist
		}

		// repeat until the number of mutations have occured
		if(mutations > 0)
			this.mutateConnectionsLimited(mutations - 1, mutationMagnitude, chanceToToggle, chanceForFlip, chanceToChange,
					chanceToCreate,
					chanceToDelete, randomizer);
	}

	private void deleteConnection(GConnection conn) {
		NeatNeuron from = getNeuronByID(conn.getFromNeuron());
		NeatNeuron to = getNeuronByID(conn.getToNeuron());
		from.removeConnection(conn);
		to.removeConnection(conn);

		this.connections.remove(conn.getInnovationNumber());

		// delete neuron if it has no inputs
		if(from.getInputConnections().isEmpty()) {
			deleteNeuron(from);
		}
		if(to.getInputConnections().isEmpty()) {
			deleteNeuron(to);
		}
	}

	public void mutateNeuronsLimited(int mutations, double magnitude, double chanceForActivation, double chanceToCreate,
			double chanceToDelete, double chanceForNeuron, double chanceForValue) {
		if(this.connections.isEmpty()) {
			return;
		}

		double sum = chanceToCreate + chanceToDelete + chanceForActivation;
		double create = chanceToCreate / sum;
		// double del = chanceToDelete / sum;

		double goal = random.nextDouble();

		double line = create;
		if(goal < line) {
			generateNeuron(100);
		}
		else if(goal < (line += chanceForActivation)) {
			if(!hiddenNeurons.isEmpty()) {
				NeatNeuron nn = hiddenNeurons.get(random.nextInt(hiddenNeurons.size()));

				if(nn != null)
					nn.mutateActivation(this, magnitude, chanceForValue, chanceForNeuron);

			}
		}
		else {
			deleteNeuron(100);
		}

		// repeat until the number of mutations have occured
		if(mutations > 0)
			this.mutateNeuronsLimited(mutations - 1, magnitude, chanceForActivation, chanceToCreate, chanceToDelete,
					chanceForValue, chanceForNeuron);
	}

	public boolean addConnection(NeatNeuron from, NeatNeuron to, double weight) {

		// don't pick the same neuron
		if(from == to) {
			return false;
		}

		// don't pick neurons on same layer
		if(from.layerIndex >= to.layerIndex) {
			return false;
		}

		// don't connect already connected neurons
		if(to.hasInputConnectionFrom(this, from)) {
			return false;
		}

		// do not connect to bias neurons, only from
		if(to.isBiasNeuron()) {
			return false;
		}

		GConnection gconn = new GConnection(innovationHistory.getNextInnovation(from.getNeuronID(), to.getNeuronID()),
				from.getNeuronID(), to.getNeuronID(), weight);

		connections.put(gconn.getInnovationNumber(), gconn);
		to.addInputConnection(this, getNeuronByID(gconn.getFromNeuron()), gconn.getInnovationNumber());

		gconn.assignNetwork(this);

		return true;
	}

	public NeatTransferFunction createActivation(int transferID, double transferValue) {

		if(transferID == -2) {
			return externalFunction;
		}
		else if(transferID == -1) {
			return null; // input neurons
		}
		else if(transferID < possibleFunctions.size()) {
			return possibleFunctions.get(transferID).copy();
		}
		// else if(transferID == 1) {
		// return new Tanh(transferValue);
		// }
		// else if(transferID == 2) {
		// return new Sigmoid(transferValue);
		// }
		// else if(transferID == 3) {
		// return new InverseFunction();
		// }
		// else if(transferID == 4) {
		// return new SquareFunction();
		// }
		// else if(transferID == 5) {
		// return new LinearFunction();
		// }
		// else if(transferID == 6) {
		// return new BinaryFunction(transferValue);
		// }
		// else if(transferID == 7) {
		// return new GaussianFunction();
		// }

		throw new IllegalArgumentException("Transfer ID is not valid.");
	}

	public static abstract class NeatTransferFunction {

		public abstract double getOutput(double x);
		
		public abstract NeatTransferFunction copy();

	}

	public static class RectifedLinearFunction extends NeatTransferFunction {

		private static final long serialVersionUID = -1824433391923159453L;

		@Override
		public double getOutput(double x) {
			return 1.0 / (1 + Math.pow(Math.E, -x));
		}

		@Override
		public NeatTransferFunction copy() {
			return new RectifedLinearFunction();
		}

	}

	public static class TanhFunction extends NeatTransferFunction {

		private static final long serialVersionUID = -1824433391923159453L;

		@Override
		public double getOutput(double totalInput) {
			return Math.tanh(totalInput);
		}

		@Override
		public NeatTransferFunction copy() {
			return new TanhFunction();
		}

	}

	public static class SigmoidFunction extends NeatTransferFunction {

		private static final long serialVersionUID = -1824433391923159453L;

		@Override
		public double getOutput(double totalInput) {
			return Math.max(0, totalInput);
		}

		@Override
		public NeatTransferFunction copy() {
			return new SigmoidFunction();
		}

	}

	public static class InverseFunction extends NeatTransferFunction {

		private static final long serialVersionUID = -1824433391923159453L;

		@Override
		public double getOutput(double totalInput) {
			if(totalInput == 0)
				return 1;
			return 1.0 / totalInput;
		}

		@Override
		public NeatTransferFunction copy() {
			return new InverseFunction();
		}

	}

	public static class SquareFunction extends NeatTransferFunction {

		private static final long serialVersionUID = 3196515326195863465L;

		@Override
		public double getOutput(double totalInput) {
			return totalInput * totalInput;
		}

		@Override
		public NeatTransferFunction copy() {
			return new SquareFunction();
		}

	}

	public static class LinearFunction extends NeatTransferFunction {

		private static final long serialVersionUID = 4619424031494605104L;

		@Override
		public double getOutput(double totalInput) {
			return totalInput;
		}

		@Override
		public NeatTransferFunction copy() {
			return new LinearFunction();
		}

	}

	public static class BinaryFunction extends NeatTransferFunction {

		private static final long serialVersionUID = 4619424031494605104L;

		public double threshold;

		public BinaryFunction(double threshold) {
			this.threshold = threshold;
		}

		public BinaryFunction() {
		}

		@Override
		public double getOutput(double totalInput) {
			return totalInput > threshold ? 1 : 0;
		}

		@Override
		public NeatTransferFunction copy() {
			return new BinaryFunction();
		}

	}

	public static class GaussianFunction extends NeatTransferFunction {

		private static final long serialVersionUID = -1824433391923159453L;

		@Override
		public double getOutput(double totalInput) {
			return Math.exp(-Math.pow(totalInput, 2));
		}

		@Override
		public NeatTransferFunction copy() {
			return new GaussianFunction();
		}

	}

	public void setInnovationHistory(InnovationHistory innovationHistory) {
		this.innovationHistory = innovationHistory;
	}

	public void update() {
		this.totalNeurons.values().forEach(n -> n.assignNetwork(this));
		this.connections.values().forEach(c -> c.assignNetwork(this));
		recalculateLayers();

		// for(GConnection conn : this.connections.values()) {
		// if(conn.getTo() == null || conn.getFrom() == null) {
		// Math.abs(1);
		// }
		// if(this.layers.get(conn.getTo().layerIndex).indexOf(conn.getTo()) == -1) {
		// S
		// }
		// }
	}

}
