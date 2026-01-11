module druvu.acc.api {
	requires static lombok;
	requires static com.github.spotbugs.annotations;

	requires druvu.lib.loader;
	requires org.slf4j;

	exports com.druvu.acc.api;
	exports com.druvu.acc.auxiliary;
	exports com.druvu.acc.currency;
	exports com.druvu.acc.loader;

	uses com.druvu.acc.loader.AccBookFactory;
}
