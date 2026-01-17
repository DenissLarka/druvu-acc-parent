package com.druvu.acc.loader;

import java.nio.file.Path;

import com.druvu.acc.api.AccStore;
import com.druvu.lib.loader.ComponentLoader;
import com.druvu.lib.loader.Dependencies;

/**
 * Factory for creating AccStore instances.
 * <p>
 * Implementations should be registered via ServiceLoader for use with druvu-lib-loader.
 * <p>
 * Expected dependencies:
 * <ul>
 *   <li>{@code java.nio.file.Path} - path to the file to load</li>
 * </ul>
 *
 * @author Deniss Larka
 * <br/>on 10 Jan 2026
 */
public interface AccStoreFactory {

	static AccStore load(Path path) {
		return ComponentLoader.load(AccStore.class, Dependencies.of(Path.class, path));
	}
}
