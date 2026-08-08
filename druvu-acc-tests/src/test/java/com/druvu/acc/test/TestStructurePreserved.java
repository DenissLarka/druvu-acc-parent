package com.druvu.acc.test;

import static org.testng.Assert.assertTrue;

import com.druvu.acc.api.AccStore;
import com.druvu.acc.api.WritableAccStore;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Guards the whole file format rather than any one entity.
 *
 * <p>Only elements declared in {@code gnucash.xsd} exist in the generated JAXB model, and {@code save} rewrites the
 * book from that model - so an element the schema does not declare is not merely unsupported, it is <em>deleted</em>
 * from the user's file on the first save. That is silent and irreversible, and it is how {@code gnc:GncOrder},
 * {@code invoice:slots} and nine other elements were being dropped before 2026-08-09.
 *
 * <p>This test loads a book, saves it untouched, and asserts that no kind of element disappeared. It is deliberately
 * generic: any future element GnuCash adds, or any schema gap introduced by an edit, fails here without anyone having
 * to remember to check.
 */
public class TestStructurePreserved {

    private static final Pattern ELEMENT = Pattern.compile("<([A-Za-z][\\w:.-]*)[\\s/>]");

    @DataProvider(name = "books")
    public Object[][] books() {
        return new Object[][] {
            // A book written by GnuCash carrying customers, vendors, jobs, invoices, entries, billing terms
            // and tax tables - the entities this library does not model, which must survive regardless.
            {"/business.gnucash"},
            // Accounts with placeholder flags, a balance-limit frame, an empty frame and a non-standard SCU.
            {"/slots.gnucash"},
            {"/common.gnucash"},
        };
    }

    @Test(dataProvider = "books")
    public void savingABookLosesNoElementType(String resource) throws IOException, URISyntaxException {
        Path source =
                Paths.get(TestStructurePreserved.class.getResource(resource).toURI());

        Map<String, Integer> before = elementCounts(readPossiblyGzipped(source));

        WritableAccStore store = AccStore.loadWritable(source);
        Path saved = Files.createTempFile("structure", ".xml");
        store.save(saved);
        Map<String, Integer> after = elementCounts(Files.readString(saved));
        Files.deleteIfExists(saved);

        StringBuilder lost = new StringBuilder();
        before.forEach((element, count) -> {
            int kept = after.getOrDefault(element, 0);
            if (kept < count) {
                lost.append("\n  ")
                        .append(element)
                        .append(": ")
                        .append(count)
                        .append(" before, ")
                        .append(kept)
                        .append(" after");
            }
        });

        assertTrue(
                lost.isEmpty(),
                "saving " + resource + " dropped elements the schema does not declare - "
                        + "add them to gnucash.xsd and regenerate:" + lost);
    }

    private static Map<String, Integer> elementCounts(String xml) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        Matcher matcher = ELEMENT.matcher(xml);
        while (matcher.find()) {
            counts.merge(matcher.group(1), 1, Integer::sum);
        }
        // The XML declaration and the namespace-flattened forms are not elements we care about here.
        counts.remove("?xml");
        return counts;
    }

    private static String readPossiblyGzipped(Path path) throws IOException {
        byte[] raw = Files.readAllBytes(path);
        boolean gzipped = raw.length > 1 && (raw[0] & 0xff) == 0x1f && (raw[1] & 0xff) == 0x8b;
        if (!gzipped) {
            return new String(raw, StandardCharsets.UTF_8);
        }
        try (InputStream in = new GZIPInputStream(Files.newInputStream(path))) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
