package com.druvu.acc.gnucash.reader;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

import org.xml.sax.InputSource;

import com.druvu.acc.gnucash.generated.GncV2;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;

/**
 * Reads GnuCash XML files and parses them into GncV2 objects.
 * <p>
 * Supports both plain XML and gzip-compressed files (typically .gnucash extension).
 *
 * @author Deniss Larka
 *         <br/>on 10 Jan 2026
 */
@Slf4j
public class GnucashFileReader {

	private static final int GZIP_MAGIC_1 = 0x1f;
	private static final int GZIP_MAGIC_2 = 0x8b;

	private final JAXBContext jaxbContext;

	public GnucashFileReader() {
		try {
			this.jaxbContext = JAXBContext.newInstance(GncV2.class);
		}
		catch (JAXBException e) {
			throw new IllegalStateException("Failed to create JAXB context", e);
		}
	}

	/**
	 * Reads a GnuCash file from the specified path.
	 *
	 * @param path the path to the GnuCash file
	 * @return the parsed GncV2
	 * @throws IOException if the file cannot be read
	 */
	public GncV2 read(Path path) throws IOException {
		log.debug("Reading GnuCash file: {}", path);

		try (InputStream is = Files.newInputStream(path); BufferedInputStream bis = new BufferedInputStream(is)) {
			return read(bis);
		}
	}

	/**
	 * Reads a GnuCash file from an input stream.
	 * <p>
	 * The stream will be automatically decompressed if it's gzip-compressed.
	 *
	 * @param inputStream the input stream (must support mark/reset)
	 * @return the parsed GncV2
	 * @throws IOException if the stream cannot be read
	 */
	public GncV2 read(InputStream inputStream) throws IOException {
		InputStream effectiveStream = inputStream.markSupported() ? inputStream : new BufferedInputStream(inputStream);

		effectiveStream.mark(2);
		int b1 = effectiveStream.read();
		int b2 = effectiveStream.read();
		effectiveStream.reset();

		if (b1 == GZIP_MAGIC_1 && b2 == GZIP_MAGIC_2) {
			log.debug("Detected gzip-compressed file");
			effectiveStream = new GZIPInputStream(effectiveStream);
		}

		try {
			// Wrap the stream in a reader that transforms namespace prefixes
			// GnuCash XML uses <gnc:book> but our XSD expects <gnc_book>
			InputStreamReader isr = new InputStreamReader(effectiveStream, StandardCharsets.UTF_8);
			NamespaceRemovingReader namespaceReader = new NamespaceRemovingReader(isr);
			BufferedReader bufferedReader = new BufferedReader(namespaceReader);

			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			GncV2 gncV2 = (GncV2) unmarshaller.unmarshal(new InputSource(bufferedReader));

			if (gncV2.getGncBook() == null) {
				throw new IOException("No gnc:book element found in file");
			}

			log.debug("Successfully parsed GnuCash file with book ID: {}", gncV2.getGncBook().getBookId().getValue());

			return gncV2;

		}
		catch (JAXBException e) {
			throw new IOException("Failed to parse GnuCash XML", e);
		}
	}
}
