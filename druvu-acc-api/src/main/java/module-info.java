module druvu.acc.api {
	requires static lombok;
	requires static com.github.spotbugs.annotations;

	requires transitive druvu.lib.loader;
	requires org.slf4j;

	exports com.druvu.acc.api;
	exports com.druvu.acc.loader;
	exports com.druvu.acc.api.entity;
	exports com.druvu.acc.api.service;

	uses com.druvu.lib.loader.ComponentFactory;
}
