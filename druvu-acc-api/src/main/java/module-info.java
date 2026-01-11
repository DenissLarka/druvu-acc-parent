module druvu.acc.api {
	requires static lombok;
	requires static com.github.spotbugs.annotations;

	requires transitive druvu.lib.loader;
	requires org.slf4j;

	exports com.druvu.acc.api;
	exports com.druvu.acc.loader;

	uses com.druvu.lib.loader.ComponentFactory;
}
