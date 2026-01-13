package com.druvu.acc.test;

import com.druvu.acc.api.AccStore;
import com.druvu.acc.gnucash.api.GnucashBookFactory;
import com.druvu.lib.loader.Dependencies;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.testng.Assert.*;

/**
 * Tests for account balance calculations.
 */
public class TestAccountBalance {

	private AccStore store;

	@BeforeClass
	public void setUp() throws URISyntaxException {
		var resourceUrl = getClass().getResource("/test.gnucash");
		assertNotNull(resourceUrl, "test.gnucash resource not found");

		Path path = Paths.get(resourceUrl.toURI());
		GnucashBookFactory factory = new GnucashBookFactory();
		store = factory.createComponent(Dependencies.of(Path.class, path));
	}

	@Test
	public void testStoreLoaded() {
		assertNotNull(store);
		assertNotNull(store.id());
	}
}
