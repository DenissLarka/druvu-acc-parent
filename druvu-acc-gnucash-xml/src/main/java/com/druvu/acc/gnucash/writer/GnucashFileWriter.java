package com.druvu.acc.gnucash.writer;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

import com.druvu.acc.gnucash.generated.GncV2;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import lombok.extern.slf4j.Slf4j;

/**
 * Writes GnuCash XML files from GncV2 objects.
 * <p>
 * Supports both plain XML and gzip-compressed files.
 * The output format is compatible with the GnuCash application.
 *
 * @author Deniss Larka
 * <br/>on 13 Jan 2026
 */
@Slf4j
public class GnucashFileWriter {

	private final JAXBContext jaxbContext;

	public GnucashFileWriter() {
		try {
			this.jaxbContext = JAXBContext.newInstance(GncV2.class);
		}
		catch (JAXBException e) {
			throw new IllegalStateException("Failed to create JAXB context", e);
		}
	}

	/**
	 * Writes a GnuCash file to the specified path.
	 * <p>
	 * If the path ends with ".gz" or ".gnucash", the output will be gzip-compressed.
	 *
	 * @param gncV2 the GncV2 object to write
	 * @param path  the path to write to
	 * @throws IOException if the file cannot be written
	 */
	public void write(GncV2 gncV2, Path path) throws IOException {
		log.debug("Writing GnuCash file: {}", path);

		boolean compress = shouldCompress(path);

		try (OutputStream os = Files.newOutputStream(path);
			 BufferedOutputStream bos = new BufferedOutputStream(os);
			 OutputStream effectiveOs = compress ? new GZIPOutputStream(bos) : bos) {

			write(gncV2, effectiveOs);
		}

		log.debug("Successfully wrote GnuCash file: {}", path);
	}

	/**
	 * Writes a GnuCash file to an output stream.
	 * <p>
	 * The stream is not closed by this method.
	 *
	 * @param gncV2        the GncV2 object to write
	 * @param outputStream the output stream to write to
	 * @throws IOException if the stream cannot be written to
	 */
	public void write(GncV2 gncV2, OutputStream outputStream) throws IOException {
		Writer osWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
		Writer namespaceWriter = new NamespaceAddingWriter(osWriter);

		try {
			Marshaller marshaller = jaxbContext.createMarshaller();
			GnucashContentHandler contentHandler = new GnucashContentHandler(namespaceWriter);
			marshaller.marshal(gncV2, contentHandler);
			namespaceWriter.flush();
		}
		catch (JAXBException e) {
			throw new IOException("Failed to marshal GnuCash XML", e);
		}
	}

	private boolean shouldCompress(Path path) {
		String fileName = path.getFileName().toString().toLowerCase();
		return fileName.endsWith(".gz") || fileName.endsWith(".gnucash");
	}
}
