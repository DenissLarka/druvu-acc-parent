open module druvu.acc.examples {
    requires druvu.acc.api;
    requires druvu.acc.gnucash.xml;
    requires druvu.lib.loader;
    requires org.slf4j;
    requires static lombok;
    requires static com.github.spotbugs.annotations;

    uses com.druvu.lib.loader.ComponentFactory;
}
