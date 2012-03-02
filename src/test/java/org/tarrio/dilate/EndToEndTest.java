package org.tarrio.dilate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Random;

import junit.framework.TestCase;

public class EndToEndTest extends TestCase {

	private static final String TEST_DATA = "En un lugar de la Mancha, de cuyo"
			+ " nombre no quiero acordarme, no ha mucho tiempo que vivía un"
			+ " hidalgo de los de lanza en astillero, adarga antigua, rocín"
			+ " flaco y galgo corredor. Una olla de algo más vaca que carnero,"
			+ " salpicón las más noches, duelos y quebrantos los sábados,"
			+ " lantejas los viernes, algún palomino de añadidura los"
			+ " domingos, consumían las tres partes de su hacienda."
			+ " El resto della concluían sayo de velarte, calzas de velludo"
			+ " para las fiestas, con sus pantuflos de lo mesmo, y los días"
			+ " de entresemana se honraba con su vellorí de lo más fino."
			+ " Tenía en su casa una ama que pasaba de los cuarenta, y una"
			+ " sobrina que no llegaba a los veinte, y un mozo de campo y"
			+ " plaza, que así ensillaba el rocín como tomaba la podadera."
			+ " Frisaba la edad de nuestro hidalgo con los cincuenta años; era"
			+ " de complexión recia, seco de carnes, enjuto de rostro, gran"
			+ " madrugador y amigo de la caza. Quieren decir que tenía el"
			+ " sobrenombre de Quijada, o Quesada, que en esto hay alguna"
			+ " diferencia en los autores que deste caso escriben; aunque,"
			+ " por conjeturas verosímiles, se deja entender que se llamaba"
			+ " Quejana. Pero esto importa poco a nuestro cuento; basta que"
			+ " en la narración dél no se salga un punto de la verdad.";

	public void testCompressUncompressText() throws Exception {
		XmlCodec codec = new XmlCodec();
		CompressionAlgorithm compressor = CompressionAlgorithmRegistry.getInstance().get("LZ77");
		ByteArrayOutputStream compressedStream = new ByteArrayOutputStream();
		compressor.compress(new ByteArrayInputStream(TEST_DATA.getBytes()),
				codec.getEncoder(compressedStream));

		ByteArrayOutputStream uncompressedStream = new ByteArrayOutputStream();
		compressor.decompress(codec.getDecoder(new ByteArrayInputStream(
				compressedStream.toByteArray())), uncompressedStream);

		assertEquals(TEST_DATA, uncompressedStream.toString());
	}

	public void testCompressUncompressBinaryData() throws Exception {
		Random random = new Random(1337L);
		byte[] bytes = new byte[200000];
		for (int i = 0; i < bytes.length; ++i) {
			bytes[i] = (byte) random.nextInt(256);
		}

		XmlCodec codec = new XmlCodec();
		CompressionAlgorithm compressor = CompressionAlgorithmRegistry.getInstance().get("LZ77");
		ByteArrayOutputStream compressedStream = new ByteArrayOutputStream();
		compressor.compress(new ByteArrayInputStream(bytes),
				codec.getEncoder(compressedStream));

		ByteArrayOutputStream uncompressedStream = new ByteArrayOutputStream();
		compressor.decompress(codec.getDecoder(new ByteArrayInputStream(
				compressedStream.toByteArray())), uncompressedStream);

		assertTrue(Arrays.equals(bytes, uncompressedStream.toByteArray()));
	}
}
