package com.druvu.acc.loader;

import java.nio.file.Path;

import com.druvu.acc.api.AccStore;
import com.druvu.acc.api.WritableAccStore;
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

	/**
	 * Loads a store and returns it as a {@link WritableAccStore}.
	 *
	 * @param path path to the file to load
	 * @return the loaded store, supporting mutation and {@link WritableAccStore#save(Path) save}
	 * @throws UnsupportedOperationException if the discovered implementation does not support writing
	 */
	static WritableAccStore loadWritable(Path path) {
		AccStore store = load(path);
		if (store instanceof WritableAccStore writable) {
			return writable;
		}
		throw new UnsupportedOperationException(
				"Loaded store does not support writing: " + store.getClass().getName());
	}
}
