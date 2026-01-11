package com.druvu.acc.loader;

import java.nio.file.Path;

import com.druvu.acc.api.AccBook;
import com.druvu.lib.loader.ComponentLoader;
import com.druvu.lib.loader.Dependencies;

/**
 * Factory for creating AccBook instances.
 * <p>
 * Implementations should be registered via ServiceLoader for use with druvu-lib-loader.
 * <p>
 * Expected dependencies:
 * <ul>
 *   <li>{@code java.nio.file.Path} - path to the file to load</li>
 *   <li>{@code java.io.InputStream} - stream to read from</li>
 * </ul>
 *
 * @author Deniss Larka
 * <br/>on 2026 Jan 10
 */
public interface AccBookFactory {

	static AccBook load(Path path) {
		return ComponentLoader.load(AccBook.class, Dependencies.of(Path.class, path));
	}

}
