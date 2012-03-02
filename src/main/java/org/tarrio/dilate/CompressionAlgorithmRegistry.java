package org.tarrio.dilate;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * A central registry for compression algorithms.
 * 
 * @author Jacobo Tarrio
 */
public class CompressionAlgorithmRegistry {

	/**
	 * The name of the file resource containing the default configuration.
	 */
	private static final String XML_CONFIG_FILE = "dilate-algorithms.xml";

	/**
	 * The singleton instance.
	 */
	private static final CompressionAlgorithmRegistry instance = new CompressionAlgorithmRegistry()
			.readConfiguration();

	private Map<String, CompressionAlgorithmProvider> algorithms;

	/**
	 * Constructs a new instance. Visible for unit tests.
	 */
	CompressionAlgorithmRegistry() {
		this.algorithms = new HashMap<String, CompressionAlgorithmProvider>();
	}

	/**
	 * Returns the algorithm registry singleton instance.
	 */
	public static CompressionAlgorithmRegistry getInstance() {
		return instance;
	}

	/**
	 * Returns an instance of the registered algorithm with the given name.
	 * 
	 * @param algorithm
	 *            The name of the algorithm to retrieve.
	 * @return The algorithm, or null if no algorithm was registered with that
	 *         name.
	 */
	public CompressionAlgorithm get(String algorithm) {
		CompressionAlgorithmProvider provider = algorithms.get(algorithm);
		return provider == null ? null : provider.get();
	}

	/**
	 * Returns an instance of the registered algorithm that can decompress the
	 * contents provided by the given decoder.
	 * 
	 * @param decoder
	 *            The decoder to get the data from.
	 * @return The algorithm, or null if no algorithm was registered.
	 * @throws IOException
	 *             If there was a problem reading from the decoder.
	 */
	public CompressionAlgorithm get(Codec.Decoder decoder) throws IOException {
		return get(decoder.getAlgoritm());
	}

	/**
	 * Returns all the registered algorithm names.
	 */
	public Set<String> getAlgorithms() {
		return algorithms.keySet();
	}

	/**
	 * Registers a compression algorithm provider.
	 * 
	 * @param algorithm
	 *            The name of the algorithm to register.
	 * @param compressorProvider
	 *            The provider for the algorithm instances.
	 */
	public void register(String algorithm,
			CompressionAlgorithmProvider compressorProvider) {
		algorithms.put(algorithm, compressorProvider);
	}

	/**
	 * Registers a compression algorithm instance.
	 * 
	 * @param algorithm
	 *            The name of the algorithm to register.
	 * @param algorithmInstance
	 *            The singleton instance to use.
	 */
	public void register(String algorithm,
			final CompressionAlgorithm algorithmInstance) {
		algorithms.put(algorithm, new CompressionAlgorithmProvider() {
			@Override
			public CompressionAlgorithm get() {
				return algorithmInstance;
			}
		});
	}

	/**
	 * Registers a compression algorithm class.
	 * 
	 * @param algorithm
	 *            The name of the algorithm to register.
	 * @param algorithmClass
	 *            The class that implements the algorithm.
	 */
	public void register(String algorithm,
			final Class<? extends CompressionAlgorithm> algorithmClass) {
		algorithms.put(algorithm, new CompressionAlgorithmProvider() {
			@Override
			public CompressionAlgorithm get() {
				try {
					return algorithmClass.newInstance();
				} catch (InstantiationException e) {
					throw new RuntimeException(e);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}

	/**
	 * Registers a compression algorithm class from its name.
	 * 
	 * @param algorithm
	 *            The name of the algorithm to register.
	 * @param className
	 *            The full name of the class that implements the algorithm.
	 * @throws ClassNotFoundException
	 *             If the specified class was not found.
	 */
	@SuppressWarnings("unchecked")
	public void register(String algorithm, String className)
			throws ClassNotFoundException {
		register(algorithm,
				(Class<CompressionAlgorithm>) Class.forName(className));
	}

	/**
	 * Registers new algorithm implementations from a properties file.
	 * 
	 * Each property key is the name of the algorithm; the value is the full
	 * name of the {@link CompressionAlgorithm} subclass that implements it.
	 * 
	 * @param stream
	 *            The input stream for the contents of the properties file.
	 * @throws ClassNotFoundException
	 *             If there was a problem finding one of the classes.
	 * @throws IOException
	 *             If there was a problem reading the properties file.
	 */
	public void registerFromProperties(InputStream stream)
			throws ClassNotFoundException, IOException {
		Properties properties = new Properties();
		properties.load(stream);
		for (Object key : properties.keySet()) {
			register((String) key, (String) properties.get(key));
		}
	}

	/**
	 * Registers new algorithm implementations from properties in an XML
	 * document.
	 * 
	 * Each property key is the name of the algorithm; the value is the full
	 * name of the {@link CompressionAlgorithm} subclass that implements it.
	 * 
	 * @param stream
	 *            The input stream for the contents of the XML document file.
	 * @throws ClassNotFoundException
	 *             If there was a problem finding one of the classes.
	 * @throws IOException
	 *             If there was a problem reading the XML file.
	 */
	public void registerFromXml(InputStream stream)
			throws ClassNotFoundException, IOException {
		Properties properties = new Properties();
		properties.loadFromXML(stream);
		for (Object key : properties.keySet()) {
			register((String) key, (String) properties.get(key));
		}
	}

	/**
	 * Reads the default configuration from the default configuration XML file
	 * resource.
	 * 
	 * @return The registry, for chaining operations.
	 */
	private CompressionAlgorithmRegistry readConfiguration() {
		try {
			registerFromXml(getClass().getResourceAsStream(XML_CONFIG_FILE));
			return this;
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * An interface for classes that return compression algorithm instances.
	 */
	public interface CompressionAlgorithmProvider {
		public CompressionAlgorithm get();
	}
}
