package com.druvu.acc.gnucash.io;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

import com.druvu.acc.api.AccBook;
import com.druvu.lib.loader.ComponentFactory;
import com.druvu.lib.loader.Dependencies;

import lombok.extern.slf4j.Slf4j;

/**
 * Factory for creating AccBook instance from GnuCash XML file.
 * <p>
 * This factory is registered via ServiceLoader for use with druvu-lib-loader.
 * <p>
 * Expected dependencies:
 * <ul>
 *   <li>{@code java.nio.file.Path} - path to the file to load</li>
 * </ul>
 *
 * @author Deniss Larka
 * <br/>on 2026 Jan 10
 */
@Slf4j
public class GnucashBookFactory implements ComponentFactory<AccBook> {

	private final GnucashFileReader reader = new GnucashFileReader();

	@Override
	public AccBook createComponent(Dependencies dependencies) {
		// Try Path first
		var pathOpt = dependencies.getOptionalDependency(Path.class);
		if (pathOpt.isPresent()) {
			Path path = pathOpt.get();
			log.info("Loading GnuCash file from path: {}", path);
			try {
				return reader.read(path);
			}
			catch (IOException e) {
				throw new UncheckedIOException("Failed to read GnuCash file: " + path, e);
			}
		}

		throw new IllegalArgumentException("Dependencies must contain java.nio.file.Path");
	}

	@Override
	public Class<AccBook> getComponentType() {
		return AccBook.class;
	}
}
