package org.tarrio.dilate;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class AlgorithmRegistry {

	private static final String XML_CONFIG_FILE = "dilate-algorithms.xml";

	private static final AlgorithmRegistry instance = new AlgorithmRegistry()
			.readConfiguration();

	private Map<String, CompressorProvider> algorithms;

	AlgorithmRegistry() {
		this.algorithms = new HashMap<String, CompressorProvider>();
	}

	AlgorithmRegistry readConfiguration() {
		try {
			registerFromXml(getClass().getResourceAsStream(XML_CONFIG_FILE));
			return this;
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void registerFromProperties(InputStream stream)
			throws ClassNotFoundException, IOException {
		Properties properties = new Properties();
		properties.load(stream);
		for (Object key : properties.keySet()) {
			register((String) key, (String) properties.get(key));
		}
	}

	public void registerFromXml(InputStream stream)
			throws ClassNotFoundException, IOException {
		Properties properties = new Properties();
		properties.loadFromXML(stream);
		for (Object key : properties.keySet()) {
			register((String) key, (String) properties.get(key));
		}
	}

	public static AlgorithmRegistry getInstance() {
		return instance;
	}

	public Compressor get(String algorithm) {
		CompressorProvider provider = algorithms.get(algorithm);
		return provider == null ? null : provider.get();
	}

	public Compressor get(Codec.Decoder decoder) throws IOException {
		return get(decoder.getAlgoritm());
	}

	public Set<String> getAlgorithms() {
		return algorithms.keySet();
	}

	public void register(String algorithm, CompressorProvider compressorProvider) {
		algorithms.put(algorithm, compressorProvider);
	}

	public void register(String algorithm,
			final Class<? extends Compressor> compressorClass) {
		algorithms.put(algorithm, new CompressorProvider() {
			@Override
			public Compressor get() {
				try {
					return compressorClass.newInstance();
				} catch (InstantiationException e) {
					throw new RuntimeException(e);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}

	public void register(String algorithm, final Compressor compressor) {
		algorithms.put(algorithm, new CompressorProvider() {
			@Override
			public Compressor get() {
				return compressor;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public void register(String algorithm, String className)
			throws ClassNotFoundException {
		register(algorithm, (Class<Compressor>) Class.forName(className));
	}

	public interface CompressorProvider {
		public Compressor get();
	}

}
