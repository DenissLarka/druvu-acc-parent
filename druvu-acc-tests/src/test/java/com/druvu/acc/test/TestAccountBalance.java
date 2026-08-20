package com.druvu.acc.test;

import static com.druvu.acc.api.entity.CommodityId.CHF;
import static org.testng.Assert.*;

import com.druvu.acc.api.AccStore;
import com.druvu.acc.api.WritableAccStore;
import com.druvu.acc.api.entity.Account;
import com.druvu.acc.api.entity.AccountType;
import com.druvu.acc.api.entity.Amount;
import com.druvu.acc.api.entity.Commodity;
import com.druvu.acc.api.entity.CommodityId;
import com.druvu.acc.api.entity.MultiAmount;
import com.druvu.acc.api.entity.Split;
import com.druvu.acc.api.entity.Transaction;
import com.druvu.acc.api.service.AccountService;
import com.druvu.acc.gnucash.api.GnucashBookFactory;
import com.druvu.lib.loader.Dependencies;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests for account balance calculations.
 *
 * <p>The {@code common.gnucash} fixture holds a single CHF book: {@code Actif} at +1500.00 and {@code Revenus} at
 * -1500.00 over five transactions dated 2026-01-12 .. 2026-01-17; every other account is empty.
 */
public class TestAccountBalance {

    private static final CommodityId AAPL = new CommodityId("NASDAQ", "AAPL");

    private static final BigDecimal ACTIF_BALANCE = new BigDecimal("1500.00");

    private Path source;

    private AccStore store;

    @BeforeClass
    public void setUp() throws URISyntaxException {
        var resourceUrl = TestAccountBalance.class.getResource("/common.gnucash");
        assertNotNull(resourceUrl, "common.gnucash resource not found");

        source = Paths.get(resourceUrl.toURI());
        GnucashBookFactory factory = new GnucashBookFactory();
        store = factory.createComponent(Dependencies.of(Path.class, source));
    }

    @Test
    public void testStoreLoaded() {
        assertNotNull(store);
        assertNotNull(store.id());
    }

    @Test
    public void testAccountByName() {
        var account = store.accountByName("Root Account:Actif");
        assertTrue(account.isPresent(), "Account 'Root Account:Actif' should be found");
        assertEquals(account.get().name(), "Actif");
    }

    @Test
    public void balanceIsOwnSplitsOnly() {
        AccountService service = AccountService.create(store, "Root Account");
        String rootId = store.rootAccounts().get(0).id();

        // The root has no splits of its own, even though the book below it is far from empty.
        assertEquals(service.balance(rootId).value().compareTo(BigDecimal.ZERO), 0);
        assertEquals(
                service.balance(service.accountByName("Actif").id()).value().compareTo(ACTIF_BALANCE), 0);
    }

    @Test
    public void totalBalanceOnLeafMatchesOwnBalance() {
        AccountService service = AccountService.create(store, "Root Account");
        String actifId = service.accountByName("Actif").id();

        MultiAmount total = service.totalBalance(actifId);

        assertTrue(total.isSingle(), "a single-currency leaf holds exactly one commodity");
        assertEquals(total.value(CHF).compareTo(service.balance(actifId).value()), 0);
    }

    @Test
    public void totalBalanceRollsUpDescendants() {
        WritableAccStore writable = AccStore.loadWritable(source);
        AccountService service = AccountService.create(writable, "Root Account");
        String actifId = service.accountByName("Actif").id();

        // Two levels below Actif, so the walk has to recurse rather than just read direct children.
        String bankId = addAccount(writable, "Bank", AccountType.BANK, CHF, actifId);
        String savingsId = addAccount(writable, "Savings", AccountType.BANK, CHF, bankId);
        addTransaction(writable, CHF, LocalDate.of(2026, 2, 1), bankId, new BigDecimal("250.00"));
        addTransaction(writable, CHF, LocalDate.of(2026, 2, 2), savingsId, new BigDecimal("70.00"));

        // The account's own balance must not change - only the total rolls the children up.
        assertEquals(service.balance(actifId).value().compareTo(ACTIF_BALANCE), 0);
        assertEquals(service.totalBalance(actifId).value(CHF).compareTo(new BigDecimal("1820.00")), 0);
        assertEquals(service.totalBalance(bankId).value(CHF).compareTo(new BigDecimal("320.00")), 0);
    }

    @Test
    public void totalAmountServesTheSingleCurrencyCase() {
        AccountService service = AccountService.create(store, "Root Account");

        assertEquals(service.totalAmount(service.accountByName("Actif").id()), Amount.of(ACTIF_BALANCE, CHF));
    }

    @Test
    public void totalAmountRefusesAMixedSubtreeAndSaysWhy() {
        AccountService service = mixedSubtree();
        String actifId = service.accountByName("Actif").id();

        String message = expectThrows(IllegalStateException.class, () -> service.totalAmount(actifId))
                .getMessage();
        // The message has to name the account and what it actually holds, or it is useless in a loop.
        assertTrue(message.contains(actifId), "should name the account: " + message);
        assertTrue(message.contains("NASDAQ/AAPL"), "should name the commodities: " + message);
        assertTrue(message.contains("totalBalance"), "should point at the fix: " + message);
    }

    @Test
    public void totalBalanceKeepsCommoditiesApart() {
        AccountService service = mixedSubtree();
        String actifId = service.accountByName("Actif").id();

        MultiAmount total = service.totalBalance(actifId);

        // 100 shares must not be added to 1500 francs - both are reported, neither is converted.
        assertFalse(total.isSingle(), "a mixed subtree holds more than one commodity");
        assertEquals(total.commodities().size(), 2);
        assertEquals(total.value(CHF).compareTo(ACTIF_BALANCE), 0);
        assertEquals(total.value(AAPL).compareTo(new BigDecimal("100")), 0);
        assertTrue(total.single().isEmpty(), "a mixed subtree has no single figure");
    }

    @Test
    public void totalBalanceHonoursCutOffDate() {
        WritableAccStore writable = AccStore.loadWritable(source);
        AccountService service = AccountService.create(writable, "Root Account");
        String actifId = service.accountByName("Actif").id();

        String bankId = addAccount(writable, "Bank", AccountType.BANK, CHF, actifId);
        addTransaction(writable, CHF, LocalDate.of(2026, 6, 1), bankId, new BigDecimal("250.00"));

        // The child's June transaction is out of range; the fixture's January ones are not.
        LocalDate cutOff = LocalDate.of(2026, 1, 31);
        assertEquals(service.totalBalance(actifId, cutOff).value(CHF).compareTo(ACTIF_BALANCE), 0);
        assertEquals(service.totalBalance(actifId).value(CHF).compareTo(new BigDecimal("1750.00")), 0);
    }

    @Test
    public void totalBalanceOfRootNetsToZero() {
        AccountService service = AccountService.create(store, "Root Account");
        String rootId = store.rootAccounts().get(0).id();

        MultiAmount total = service.totalBalance(rootId);

        // Double entry: every split in the book is inside the root's subtree, so they cancel out.
        assertEquals(total.value(CHF).compareTo(BigDecimal.ZERO), 0);
    }

    @Test
    public void totalBalanceOfEmptySubtreeStillReportsItsCommodity() {
        AccountService service = AccountService.create(store, "Root Account");

        // 'Capitaux propres' and its child 'Soldes initiaux' carry no splits at all.
        MultiAmount total =
                service.totalBalance(service.accountByName("Capitaux propres").id());

        assertFalse(total.isEmpty(), "an empty CHF subtree still reports CHF, at zero");
        assertEquals(total.value(CHF).compareTo(BigDecimal.ZERO), 0);
    }

    /**
     * A book whose 'Actif' subtree mixes CHF with a NASDAQ/AAPL stock account - 100 shares in, the money out on the
     * other side, which is what a real share purchase looks like rather than a CHF amount hidden in a stock account.
     */
    private AccountService mixedSubtree() {
        WritableAccStore writable = AccStore.loadWritable(source);
        AccountService service = AccountService.create(writable, "Root Account");
        String actifId = service.accountByName("Actif").id();

        writable.addCommodity(Commodity.security("NASDAQ", "AAPL", "Apple Inc.", 10000));
        String stockId = addAccount(writable, "Apple", AccountType.STOCK, AAPL, actifId);
        addTransaction(
                writable, CHF, LocalDate.of(2026, 2, 1), stockId, new BigDecimal("100"), new BigDecimal("-21250.00"));
        return service;
    }

    private static String addAccount(
            WritableAccStore store, String name, AccountType type, CommodityId commodity, String parentId) {
        String id = store.newId();
        store.addAccount(Account.of(id, name, type).withCommodity(commodity).withParent(parentId));
        return id;
    }

    private static void addTransaction(
            WritableAccStore store, CommodityId currency, LocalDate date, String accountId, BigDecimal amount) {
        addTransaction(store, currency, date, accountId, amount, amount.negate());
    }

    /**
     * Posts a two-split transaction whose counterpart lands on {@code Revenus} - outside the {@code Actif} subtree the
     * tests assert on. The quantity is passed separately from the counterpart's amount because a share purchase moves a
     * share count on one side and money on the other; the primary split's <em>value</em> mirrors the counterpart's, so
     * the transaction balances the way the store now requires.
     */
    private static void addTransaction(
            WritableAccStore store,
            CommodityId currency,
            LocalDate date,
            String accountId,
            BigDecimal quantity,
            BigDecimal counterpartAmount) {
        String txId = store.newId();
        String counterpartId =
                store.accountByName("Root Account:Revenus").orElseThrow().id();
        List<Split> splits = List.of(
                Split.of(store.newId(), txId, accountId, date, counterpartAmount.negate(), quantity),
                Split.of(store.newId(), txId, counterpartId, date, counterpartAmount));
        store.addTransaction(Transaction.of(txId, currency, date, "balance test", splits));
    }
}
