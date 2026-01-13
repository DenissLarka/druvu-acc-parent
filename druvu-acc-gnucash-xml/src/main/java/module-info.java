
module druvu.acc.gnucash.xml {
	requires static lombok;
	requires static com.github.spotbugs.annotations;

	requires druvu.acc.api;
	requires druvu.lib.loader;
	requires jakarta.xml.bind;
	requires org.slf4j;

	// Open the generated package to JAXB for reflection
	opens com.druvu.acc.gnucash.generated to jakarta.xml.bind;

	// Export public API
	exports com.druvu.acc.gnucash.api;

	// Register factory with ServiceLoader
	provides com.druvu.lib.loader.ComponentFactory with com.druvu.acc.gnucash.api.GnucashBookFactory;
}
