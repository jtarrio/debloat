package org.tarrio.dilate.registry;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.tarrio.dilate.Codec;
import org.tarrio.dilate.CompressionAlgorithm;
import org.tarrio.dilate.Codec.Decoder;

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
	 * Registers new algorithm implementations programmatically from a
	 * {@link CompressionAlgorithmRegistrationModule}.
	 * 
	 * @param module
	 *            The module to get the configuration from.
	 */
	public void registerFromModule(CompressionAlgorithmRegistrationModule module) {
		module.configure(new CompressionAlgorithmBinderImpl());
	}

	@SuppressWarnings("unchecked")
	private void register(String name, String className)
			throws ClassNotFoundException {
		algorithms.put(name, new CompressionAlgorithmFromClassProviderImpl(
				(Class<CompressionAlgorithm>) Class.forName(className)));
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
	 * A private implementation of {@link CompressionAlgorithmBinder}.
	 */
	private class CompressionAlgorithmBinderImpl implements
			CompressionAlgorithmBinder {
		@Override
		public CompressionAlgorithmSelector bind(final String name) {
			return new CompressionAlgorithmSelectorImpl(name);
		}
	}

	/**
	 * A private implementation of {@link CompressionAlgorithmSelector}.
	 */
	private class CompressionAlgorithmSelectorImpl implements
			CompressionAlgorithmSelector {
		private final String name;

		private CompressionAlgorithmSelectorImpl(String name) {
			this.name = name;
		}

		@Override
		public void to(Class<? extends CompressionAlgorithm> algorithm) {
			toProvider(new CompressionAlgorithmFromClassProviderImpl(algorithm));
		}

		@Override
		public void toProvider(CompressionAlgorithmProvider provider) {
			algorithms.put(name, provider);
		}

		@Override
		public void toInstance(final CompressionAlgorithm instance) {
			toProvider(new CompressionAlgorithmFromInstanceProviderImpl(
					instance));
		}
	}

	/**
	 * A {@link CompressionAlgorithmProvider} that always returns the same
	 * instance.
	 */
	private class CompressionAlgorithmFromInstanceProviderImpl implements
			CompressionAlgorithmProvider {
		private final CompressionAlgorithm instance;

		private CompressionAlgorithmFromInstanceProviderImpl(
				CompressionAlgorithm instance) {
			this.instance = instance;
		}

		@Override
		public CompressionAlgorithm get() {
			return instance;
		}
	}

	/**
	 * A {@link CompressionAlgorithmProvider} that creates new instances of a
	 * class using its default constructor and returns that instance.
	 */
	private class CompressionAlgorithmFromClassProviderImpl implements
			CompressionAlgorithmProvider {
		private final Class<? extends CompressionAlgorithm> algorithm;

		private CompressionAlgorithmFromClassProviderImpl(
				Class<? extends CompressionAlgorithm> algorithm) {
			this.algorithm = algorithm;
		}

		@Override
		public CompressionAlgorithm get() {
			try {
				return algorithm.newInstance();
			} catch (InstantiationException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
