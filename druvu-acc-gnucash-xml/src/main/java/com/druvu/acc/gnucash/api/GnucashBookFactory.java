package com.druvu.acc.gnucash.api;

import com.druvu.acc.api.AccStore;
import com.druvu.acc.api.entity.CommodityId;
import com.druvu.acc.gnucash.impl.GnucashAccStore;
import com.druvu.acc.gnucash.reader.GnucashFileReader;
import com.druvu.lib.loader.ComponentFactory;
import com.druvu.lib.loader.Dependencies;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;

/**
 * Factory for creating AccStore instance from GnuCash XML file.
 *
 * <p>This factory is registered via ServiceLoader for use with druvu-lib-loader.
 *
 * <p>Expected dependencies, one of:
 *
 * <ul>
 *   <li>{@code java.nio.file.Path} - path to the file to load
 *   <li>{@code CommodityId} - currency for a new, empty book built from the bundled template
 * </ul>
 *
 * @author Deniss Larka <br>
 *     on 10 Jan 2026
 */
@Slf4j
public class GnucashBookFactory implements ComponentFactory<AccStore> {

    private final GnucashFileReader reader = new GnucashFileReader();

    /**
     * An empty book written by GnuCash itself (one root account, no other entities), so a book started from it is
     * GnuCash's own shape rather than this library's idea of one.
     */
    private static final String EMPTY_BOOK_TEMPLATE = "/com/druvu/acc/gnucash/empty-book.xml";

    @Override
    public AccStore createComponent(Dependencies dependencies) {
        var pathOpt = dependencies.getOptionalDependency(Path.class);
        if (pathOpt.isPresent()) {
            Path path = pathOpt.get();
            log.info("Loading GnuCash file from path: {}", path);
            try {
                return new GnucashAccStore(reader.read(path));
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to read GnuCash file: " + path, e);
            }
        }

        var currencyOpt = dependencies.getOptionalDependency(CommodityId.class);
        if (currencyOpt.isPresent()) {
            CommodityId currency = currencyOpt.get();
            log.info("Creating a new empty GnuCash book in {}", currency);
            try (InputStream template = GnucashBookFactory.class.getResourceAsStream(EMPTY_BOOK_TEMPLATE)) {
                return GnucashAccStore.newBook(reader.read(template), currency);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to read the bundled empty-book template", e);
            }
        }

        throw new IllegalArgumentException("Dependencies must contain java.nio.file.Path or CommodityId");
    }

    @Override
    public Class<? extends AccStore> type() {
        return AccStore.class;
    }
}
