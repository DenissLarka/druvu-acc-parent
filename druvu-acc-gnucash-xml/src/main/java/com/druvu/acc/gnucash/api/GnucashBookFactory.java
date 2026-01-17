package com.druvu.acc.gnucash.api;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

import com.druvu.acc.api.AccStore;
import com.druvu.acc.gnucash.impl.GnucashAccStore;
import com.druvu.acc.gnucash.reader.GnucashFileReader;
import com.druvu.lib.loader.ComponentFactory;
import com.druvu.lib.loader.Dependencies;

import lombok.extern.slf4j.Slf4j;

/**
 * Factory for creating AccStore instance from GnuCash XML file.
 * <p>
 * This factory is registered via ServiceLoader for use with druvu-lib-loader.
 * <p>
 * Expected dependencies:
 * <ul>
 *   <li>{@code java.nio.file.Path} - path to the file to load</li>
 * </ul>
 *
 * @author Deniss Larka
 *         <br/>on 10 Jan 2026
 */
@Slf4j
public class GnucashBookFactory implements ComponentFactory<AccStore> {

	private final GnucashFileReader reader = new GnucashFileReader();

	@Override
	public AccStore createComponent(Dependencies dependencies) {
		var pathOpt = dependencies.getOptionalDependency(Path.class);
		if (pathOpt.isPresent()) {
			Path path = pathOpt.get();
			log.info("Loading GnuCash file from path: {}", path);
			try {
				return new GnucashAccStore(reader.read(path));
			}
			catch (IOException e) {
				throw new UncheckedIOException("Failed to read GnuCash file: " + path, e);
			}
		}

		throw new IllegalArgumentException("Dependencies must contain java.nio.file.Path");
	}

	@Override
	public Class<AccStore> getComponentType() {
		return AccStore.class;
	}
}
