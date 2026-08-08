package com.druvu.acc.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import com.druvu.acc.api.AccStore;
import com.druvu.acc.api.WritableAccStore;
import com.druvu.acc.api.entity.Account;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * The account flags GnuCash stores as slots, seen from the API - where they are ordinary typed properties and the word
 * "slot" never appears.
 *
 * <p>The fixture {@code slots.gnucash} was written by GnuCash itself and carries the cases that are easy to get wrong:
 * a {@code placeholder} flag stored as the <em>string</em> {@code "true"} rather than a boolean, a false flag stored by
 * <em>omitting</em> the slot, an account colour, and a non-standard smallest-currency-unit.
 *
 * <p>Preservation of everything this library does not model is covered separately by {@link TestStructurePreserved}.
 */
public class TestSlots {

    private static final String SUBSCRIPTIONS = "Root Account:Dépenses:Abonnements";

    private Path source;

    @BeforeClass
    public void setUp() throws URISyntaxException {
        source = Paths.get(TestSlots.class.getResource("/slots.gnucash").toURI());
    }

    @Test
    public void readsPlaceholderAndColour() throws IOException {
        AccStore store = AccStore.load(source);

        Account actif = store.accountByName("Root Account:Actif").orElseThrow();
        assertTrue(actif.placeholder(), "GnuCash marks this account placeholder");
        assertFalse(actif.hidden());

        Account subscriptions = store.accountByName(SUBSCRIPTIONS).orElseThrow();
        assertFalse(subscriptions.placeholder(), "a false flag is stored by omitting the slot, not by writing false");
        assertEquals(subscriptions.color(), Optional.of("rgb(99,69,44)"));
        assertEquals(subscriptions.notes(), Optional.empty());
    }

    @Test
    public void everyPlaceholderAccountIsFound() throws IOException {
        List<String> placeholders = AccStore.load(source).accounts().stream()
                .filter(Account::placeholder)
                .map(Account::name)
                .toList();
        assertEquals(placeholders, List.of("Actif", "Dépenses", "Auto", "Assurances"));
    }

    @Test
    public void setsAndClearsFlagsAcrossASave() throws IOException {
        WritableAccStore store = AccStore.loadWritable(source);
        String id = store.accountByName(SUBSCRIPTIONS).orElseThrow().id();

        store.updateAccount(store.accountById(id)
                .orElseThrow()
                .withPlaceholder(true)
                .withHidden(true)
                .withNotes("grouped"));

        Account set = saveAndReload(store).accountById(id).orElseThrow();
        assertTrue(set.placeholder());
        assertTrue(set.hidden());
        assertEquals(set.notes(), Optional.of("grouped"));
        assertEquals(set.color(), Optional.of("rgb(99,69,44)"), "an untouched flag must not be disturbed");

        store.updateAccount(set.withPlaceholder(false).withHidden(false));
        Account cleared = saveAndReload(store).accountById(id).orElseThrow();
        assertFalse(cleared.placeholder());
        assertFalse(cleared.hidden());
        assertEquals(cleared.notes(), Optional.of("grouped"), "clearing one flag must not clear another");
    }

    /**
     * With every flag off and no notes there is nothing left to write, and the schema declares slots as
     * {@code KvpSlot+} - so the container must be omitted rather than emitted empty.
     */
    @Test
    public void anAccountWithNoFlagsWritesNoSlotsElement() throws IOException {
        WritableAccStore store = AccStore.loadWritable(source);
        String id = store.accountByName("Root Account:Actif").orElseThrow().id();

        store.updateAccount(store.accountById(id).orElseThrow().withPlaceholder(false));

        Path saved = Files.createTempFile("slots-empty", ".xml");
        store.save(saved);
        String xml = Files.readString(saved);
        Files.deleteIfExists(saved);

        assertFalse(xml.contains("<act:slots/>"), "an empty slot container must be omitted entirely");
        assertFalse(xml.contains("<act:slots></act:slots>"), "an empty slot container must be omitted entirely");
    }

    /**
     * The account records amounts at 1/10 CHF rather than the currency's 1/100. The API deliberately offers no way to
     * read or set that - an account's precision should follow its commodity - but an edit must not silently reset it.
     */
    @Test
    public void editingAnAccountKeepsItsNonStandardPrecision() throws IOException {
        WritableAccStore store = AccStore.loadWritable(source);
        Account account = store.accountByName(SUBSCRIPTIONS).orElseThrow();

        store.updateAccount(account.withDescription("edited by the test"));

        Path saved = Files.createTempFile("slots-scu", ".xml");
        store.save(saved);
        String xml = Files.readString(saved);
        Files.deleteIfExists(saved);

        String element = xml.substring(xml.indexOf("<act:name>Abonnements</act:name>"));
        element = element.substring(0, element.indexOf("</gnc:account>"));
        assertTrue(element.contains("<act:commodity-scu>10</act:commodity-scu>"), "the account's own SCU must survive");
        assertTrue(element.contains("<act:non-standard-scu"), "and so must the flag marking it non-standard");
    }

    @Test
    public void editingEveryAccountAndTransactionChangesNothing() throws IOException {
        WritableAccStore store = AccStore.loadWritable(source);
        List<Account> accountsBefore = store.accounts();
        var transactionsBefore = store.transactions();

        accountsBefore.forEach(store::updateAccount);
        transactionsBefore.forEach(store::updateTransaction);

        AccStore reloaded = saveAndReload(store);
        assertEquals(reloaded.accounts(), accountsBefore, "rewriting every account must change nothing");
        assertEquals(reloaded.transactions(), transactionsBefore, "rewriting every transaction must change nothing");
    }

    private AccStore saveAndReload(WritableAccStore store) throws IOException {
        Path saved = Files.createTempFile("slots-round-trip", ".gnucash");
        store.save(saved);
        AccStore reloaded = AccStore.load(saved);
        assertNotNull(reloaded);
        Files.deleteIfExists(saved);
        return reloaded;
    }
}
