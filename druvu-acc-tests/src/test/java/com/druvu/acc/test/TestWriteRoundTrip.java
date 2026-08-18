package com.druvu.acc.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

import com.druvu.acc.api.AccStore;
import com.druvu.acc.api.WritableAccStore;
import com.druvu.acc.api.entity.Account;
import com.druvu.acc.api.entity.AccountType;
import com.druvu.acc.api.entity.Commodity;
import com.druvu.acc.api.entity.CommodityId;
import com.druvu.acc.api.entity.Price;
import com.druvu.acc.api.entity.Split;
import com.druvu.acc.api.entity.Transaction;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** Round-trip tests for the writable store: mutate, save, reload, assert. */
public class TestWriteRoundTrip {

    private Path source;

    @BeforeClass
    public void setUp() throws URISyntaxException {
        source = Paths.get(
                TestWriteRoundTrip.class.getResource("/common.gnucash").toURI());
    }

    @Test
    public void addAccountRoundTrips() throws IOException {
        WritableAccStore store = AccStore.loadWritable(source);
        int before = store.accounts().size();
        String rootId = store.rootAccounts().get(0).id();
        String id = store.newId();

        // The book is CHF-only: an account denominated in a commodity the book never declares is a
        // dangling reference, and GnuCash loads such an account with no commodity at all.
        store.addCommodity(Commodity.currency("EUR"));
        store.addAccount(Account.of(id, "Round Trip Test", AccountType.EXPENSE)
                .withDescription("created by test")
                .withCommodity(CommodityId.currency("EUR"))
                .withParent(rootId));

        AccStore reloaded = saveAndReload(store);

        assertEquals(reloaded.accounts().size(), before + 1);
        Optional<Account> found = reloaded.accountById(id);
        assertTrue(found.isPresent(), "added account should be present after reload");
        assertEquals(found.get().name(), "Round Trip Test");
        assertEquals(found.get().type(), AccountType.EXPENSE);
        assertEquals(found.get().description(), Optional.of("created by test"));
        assertEquals(found.get().commodity(), Optional.of(CommodityId.currency("EUR")));
        assertEquals(found.get().parentId(), Optional.of(rootId));
    }

    @Test
    public void addTransactionRoundTrips() throws IOException {
        WritableAccStore store = AccStore.loadWritable(source);
        // Deliberately looked up by name: accounts.get(0) is the ROOT, which must never carry splits.
        String accountA =
                store.accountByName("Root Account:Actif").orElseThrow().id();
        String accountB =
                store.accountByName("Root Account:Revenus").orElseThrow().id();
        LocalDate date = LocalDate.of(2026, 6, 7);
        int before = store.transactions().size();

        String txId = store.newId();
        List<Split> splits = List.of(
                Split.of(store.newId(), txId, accountA, date, new BigDecimal("10.00")),
                Split.of(store.newId(), txId, accountB, date, new BigDecimal("-10.00")));

        store.addCommodity(Commodity.currency("EUR"));
        store.addTransaction(Transaction.of(txId, CommodityId.currency("EUR"), date, "Round Trip Tx", splits)
                .withNumber("42"));

        AccStore reloaded = saveAndReload(store);

        assertEquals(reloaded.transactions().size(), before + 1);
        Optional<Transaction> tx = reloaded.transactionById(txId);
        assertTrue(tx.isPresent(), "added transaction should be present after reload");
        assertEquals(tx.get().description(), "Round Trip Tx");
        assertEquals(tx.get().datePosted(), date);
        assertEquals(tx.get().number(), Optional.of("42"));
        assertEquals(tx.get().splits().size(), 2);
        assertEquals(tx.get().splits().get(0).value().compareTo(new BigDecimal("10.00")), 0);
        assertEquals(tx.get().splits().get(1).value().compareTo(new BigDecimal("-10.00")), 0);
    }

    @Test
    public void removeTransactionRoundTrips() throws IOException {
        WritableAccStore store = AccStore.loadWritable(source);
        Transaction victim = store.transactions().get(0);
        int before = store.transactions().size();

        store.removeTransaction(victim.id());
        AccStore reloaded = saveAndReload(store);

        assertEquals(reloaded.transactions().size(), before - 1);
        assertTrue(reloaded.transactionById(victim.id()).isEmpty(), "removed transaction should be gone");
    }

    @Test
    public void removeAccountRoundTrips() throws IOException {
        WritableAccStore store = AccStore.loadWritable(source);
        String id = store.newId();
        store.addAccount(Account.of(id, "Temp", AccountType.EXPENSE)
                .withCommodity(CommodityId.currency("EUR"))
                .withParent(store.rootAccounts().get(0).id()));
        int afterAdd = store.accounts().size();

        store.removeAccount(id);
        AccStore reloaded = saveAndReload(store);

        assertEquals(reloaded.accounts().size(), afterAdd - 1);
        assertTrue(reloaded.accountById(id).isEmpty());
    }

    @Test
    public void addCommodityRoundTrips() throws IOException {
        WritableAccStore store = AccStore.loadWritable(source);
        int before = store.commodities().size();
        CommodityId apple = new CommodityId("NASDAQ", "AAPL");

        store.addCommodity(Commodity.security("NASDAQ", "AAPL", "Apple Inc.", 10000));

        AccStore reloaded = saveAndReload(store);

        assertEquals(reloaded.commodities().size(), before + 1);
        assertTrue(reloaded.commodities().contains(apple), "added commodity should be present after reload");
    }

    @Test
    public void addPriceRoundTrips() throws IOException {
        WritableAccStore store = AccStore.loadWritable(source);
        store.addCommodity(Commodity.security("NASDAQ", "AAPL", "Apple Inc.", 10000));
        int before = store.prices().size();

        CommodityId apple = new CommodityId("NASDAQ", "AAPL");
        CommodityId usd = CommodityId.currency("USD");
        String priceId = store.newId();
        LocalDateTime when = LocalDateTime.of(2026, 6, 7, 12, 0, 0);

        store.addPrice(new Price(
                priceId, apple, usd, when, "user:price-editor", Optional.of("last"), new BigDecimal("212.50")));

        AccStore reloaded = saveAndReload(store);

        assertEquals(reloaded.prices().size(), before + 1);
        Price found = reloaded.prices().stream()
                .filter(p -> p.id().equals(priceId))
                .findFirst()
                .orElse(null);
        assertTrue(found != null, "added price should be present after reload");
        assertEquals(found.commodity(), apple);
        assertEquals(found.currency(), usd);
        assertEquals(found.value().compareTo(new BigDecimal("212.50")), 0);
        assertEquals(found.source(), "user:price-editor");
        assertEquals(found.type(), Optional.of("last"));
    }

    @Test
    public void savedFileDeclaresGnuCashNamespaces() throws IOException {
        WritableAccStore store = AccStore.loadWritable(source);
        // Save uncompressed so we can read the raw XML text.
        Path out = Files.createTempFile("acc-write", ".xml");
        try {
            store.save(out);
            String xml = Files.readString(out);
            // GnuCash requires the namespace declarations on the <gnc-v2> root; without them
            // it opens the file with an empty book. Our reader strips namespaces, so only a
            // raw-text assertion guards this.
            assertTrue(xml.contains("<gnc-v2"), "root element present");
            assertTrue(xml.contains("xmlns:gnc=\"http://www.gnucash.org/XML/gnc\""), "gnc namespace declared");
            assertTrue(xml.contains("xmlns:trn=\"http://www.gnucash.org/XML/trn\""), "trn namespace declared");
            assertTrue(xml.contains("xmlns:act=\"http://www.gnucash.org/XML/act\""), "act namespace declared");
        } finally {
            Files.deleteIfExists(out);
        }
    }

    @Test
    public void aSoundBookValidatesClean() {
        assertEquals(AccStore.load(source).validate(), List.of());
    }

    @Test
    public void saveRefusesABookWithDanglingSplits() {
        WritableAccStore store = AccStore.loadWritable(source);
        // removeAccount does not cascade: its splits are left pointing at an account that is gone.
        Account victim = store.accountByName("Root Account:Actif").orElseThrow();
        store.removeAccount(victim.id());

        List<String> problems = store.validate();
        assertFalse(problems.isEmpty(), "dangling splits should be reported");
        assertTrue(
                problems.stream().anyMatch(p -> p.contains("split on an account that is not in the book")),
                "expected a dangling-split problem, got: " + problems);

        Path out = Paths.get(System.getProperty("java.io.tmpdir"), "should-not-be-written.gnucash");
        assertThrows(IllegalStateException.class, () -> store.save(out));
        assertFalse(Files.exists(out), "nothing should be written when validation fails");
    }

    @Test
    public void saveRefusesASplitPostedToTheRoot() {
        WritableAccStore store = AccStore.loadWritable(source);
        String rootId = store.rootAccounts().get(0).id();
        String realAccount =
                store.accountByName("Root Account:Actif").orElseThrow().id();
        String txId = store.newId();
        LocalDate date = LocalDate.of(2026, 6, 1);

        // The root holds the account tree, not money - an easy mistake when reaching for
        // "some other account" to balance against.
        store.addTransaction(Transaction.of(
                txId,
                CommodityId.CHF,
                date,
                "wrong leg",
                List.of(
                        Split.of(store.newId(), txId, realAccount, date, new BigDecimal("5.00")),
                        Split.of(store.newId(), txId, rootId, date, new BigDecimal("-5.00")))));

        assertTrue(
                store.validate().stream().anyMatch(p -> p.contains("ROOT account")),
                "a split on the root should be reported: " + store.validate());
        assertThrows(
                IllegalStateException.class,
                () -> store.save(Paths.get(System.getProperty("java.io.tmpdir"), "never-written.gnucash")));
    }

    @Test
    public void saveRefusesAnAccountInAnUndefinedCommodity() {
        WritableAccStore store = AccStore.loadWritable(source);
        String rootId = store.rootAccounts().get(0).id();
        // A currency gets past addAccount because ISO can answer for its precision, but the book still
        // never declares it - GnuCash resolves act:commodity through the book's own table and loads
        // such an account with no commodity at all.
        store.addAccount(Account.of(store.newId(), "Undeclared", AccountType.EXPENSE)
                .withCommodity(CommodityId.JPY)
                .withParent(rootId));

        assertUndefinedCommodityRefused(store);
    }

    @Test
    public void saveRefusesATransactionInAnUndefinedCommodity() {
        WritableAccStore store = AccStore.loadWritable(source);
        String accountA =
                store.accountByName("Root Account:Actif").orElseThrow().id();
        String accountB =
                store.accountByName("Root Account:Revenus").orElseThrow().id();
        LocalDate date = LocalDate.of(2026, 6, 7);
        String txId = store.newId();

        store.addTransaction(Transaction.of(
                txId,
                CommodityId.JPY,
                date,
                "undeclared currency",
                List.of(
                        Split.of(store.newId(), txId, accountA, date, new BigDecimal("10.00")),
                        Split.of(store.newId(), txId, accountB, date, new BigDecimal("-10.00")))));

        assertUndefinedCommodityRefused(store);
    }

    /** The book is reported as broken and nothing is written. */
    private static void assertUndefinedCommodityRefused(WritableAccStore store) {
        List<String> problems = store.validate();
        assertTrue(
                problems.stream().anyMatch(p -> p.contains("commodity the book does not define")),
                "expected an undefined-commodity problem, got: " + problems);

        Path out = Paths.get(System.getProperty("java.io.tmpdir"), "undefined-commodity.gnucash");
        assertThrows(IllegalStateException.class, () -> store.save(out));
        assertFalse(Files.exists(out), "nothing should be written when validation fails");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void addSecondRootAccountFails() {
        WritableAccStore store = AccStore.loadWritable(source);
        // The GnuCash API tolerates several roots; its UI does not display such a book correctly.
        store.addAccount(Account.of(store.newId(), "Another Root", AccountType.ROOT));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void addParentlessAccountFails() {
        WritableAccStore store = AccStore.loadWritable(source);
        // A GnuCash book has one ROOT; a parentless EXPENSE account would become a second root and the
        // book would not open properly. Account.of(...) makes this easy to do by accident.
        store.addAccount(
                Account.of(store.newId(), "Orphan", AccountType.EXPENSE).withCommodity(CommodityId.CHF));
    }

    @Test
    public void accountScuFollowsItsCommodity() throws IOException {
        WritableAccStore store = AccStore.loadWritable(source);
        String rootId = store.rootAccounts().get(0).id();

        store.addCommodity(Commodity.currency("JPY"));
        store.addAccount(Account.of(store.newId(), "Tokyo", AccountType.BANK)
                .withCommodity(CommodityId.JPY)
                .withParent(rootId));

        Path out = Files.createTempFile("acc-scu", ".xml");
        try {
            store.save(out);
            String xml = Files.readString(out);
            String tokyo = xml.substring(xml.indexOf("<act:name>Tokyo</act:name>"));
            // Yen has no minor unit: writing a flat 100 here would misstate the account's precision.
            assertTrue(
                    tokyo.contains("<act:commodity-scu>1</act:commodity-scu>"),
                    "JPY account should record scu 1, got: " + tokyo.substring(0, Math.min(400, tokyo.length())));
        } finally {
            Files.deleteIfExists(out);
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void addDuplicateAccountFails() {
        WritableAccStore store = AccStore.loadWritable(source);
        store.addAccount(store.accounts().get(0));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void removeMissingTransactionFails() {
        WritableAccStore store = AccStore.loadWritable(source);
        store.removeTransaction("does-not-exist");
    }

    private AccStore saveAndReload(WritableAccStore store) throws IOException {
        Path out = Files.createTempFile("acc-write", ".gnucash");
        try {
            store.save(out);
            return AccStore.load(out);
        } finally {
            Files.deleteIfExists(out);
        }
    }
}
