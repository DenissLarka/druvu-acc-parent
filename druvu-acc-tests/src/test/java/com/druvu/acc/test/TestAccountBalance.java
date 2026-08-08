package com.druvu.acc.test;

import static org.testng.Assert.*;

import com.druvu.acc.api.AccStore;
import com.druvu.acc.api.WritableAccStore;
import com.druvu.acc.api.entity.Account;
import com.druvu.acc.api.entity.AccountType;
import com.druvu.acc.api.entity.Commodity;
import com.druvu.acc.api.entity.CommodityId;
import com.druvu.acc.api.entity.MultiAsset;
import com.druvu.acc.api.entity.ReconcileState;
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
import java.util.Optional;
import java.util.UUID;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests for account balance calculations.
 *
 * <p>The {@code common.gnucash} fixture holds a single CHF book: {@code Actif} at +1500.00 and {@code Revenus} at
 * -1500.00 over five transactions dated 2026-01-12 .. 2026-01-17; every other account is empty.
 */
public class TestAccountBalance {

    private static final CommodityId CHF = CommodityId.currency("CHF");

    private static final CommodityId AAPL = new CommodityId("NASDAQ", "AAPL");

    private static final BigDecimal ACTIF_BALANCE = new BigDecimal("1500.00");

    private Path source;

    private AccStore store;

    @BeforeClass
    public void setUp() throws URISyntaxException {
        var resourceUrl = getClass().getResource("/common.gnucash");
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
        assertEquals(service.balance(rootId).compareTo(BigDecimal.ZERO), 0);
        assertEquals(service.balance(service.accountByName("Actif").id()).compareTo(ACTIF_BALANCE), 0);
    }

    @Test
    public void totalBalanceOnLeafMatchesOwnBalance() {
        AccountService service = AccountService.create(store, "Root Account");
        String actifId = service.accountByName("Actif").id();

        MultiAsset total = service.totalBalance(actifId);

        assertTrue(total.isSingle(), "a single-currency leaf holds exactly one commodity");
        assertEquals(total.amount(CHF).compareTo(service.balance(actifId)), 0);
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
        assertEquals(service.balance(actifId).compareTo(ACTIF_BALANCE), 0);
        assertEquals(service.totalBalance(actifId).amount(CHF).compareTo(new BigDecimal("1820.00")), 0);
        assertEquals(service.totalBalance(bankId).amount(CHF).compareTo(new BigDecimal("320.00")), 0);
    }

    @Test
    public void totalBalanceKeepsCommoditiesApart() {
        WritableAccStore writable = AccStore.loadWritable(source);
        AccountService service = AccountService.create(writable, "Root Account");
        String actifId = service.accountByName("Actif").id();

        writable.addCommodity(Commodity.security("NASDAQ", "AAPL", "Apple Inc.", 10000));
        String stockId = addAccount(writable, "Apple", AccountType.STOCK, AAPL, actifId);
        // 100 shares in, the money out on the other side - a real share purchase, not a CHF amount
        // hidden in a stock account.
        addTransaction(
                writable, CHF, LocalDate.of(2026, 2, 1), stockId, new BigDecimal("100"), new BigDecimal("-21250.00"));

        MultiAsset total = service.totalBalance(actifId);

        // 100 shares must not be added to 1500 francs - both are reported, neither is converted.
        assertFalse(total.isSingle(), "a mixed subtree holds more than one commodity");
        assertEquals(total.commodities().size(), 2);
        assertEquals(total.amount(CHF).compareTo(ACTIF_BALANCE), 0);
        assertEquals(total.amount(AAPL).compareTo(new BigDecimal("100")), 0);
        assertTrue(total.singleAmount().isEmpty(), "a mixed subtree has no single figure");
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
        assertEquals(service.totalBalance(actifId, cutOff).amount(CHF).compareTo(ACTIF_BALANCE), 0);
        assertEquals(service.totalBalance(actifId).amount(CHF).compareTo(new BigDecimal("1750.00")), 0);
    }

    @Test
    public void totalBalanceOfRootNetsToZero() {
        AccountService service = AccountService.create(store, "Root Account");
        String rootId = store.rootAccounts().get(0).id();

        MultiAsset total = service.totalBalance(rootId);

        // Double entry: every split in the book is inside the root's subtree, so they cancel out.
        assertEquals(total.amount(CHF).compareTo(BigDecimal.ZERO), 0);
    }

    @Test
    public void totalBalanceOfEmptySubtreeStillReportsItsCommodity() {
        AccountService service = AccountService.create(store, "Root Account");

        // 'Capitaux propres' and its child 'Soldes initiaux' carry no splits at all.
        MultiAsset total =
                service.totalBalance(service.accountByName("Capitaux propres").id());

        assertFalse(total.isEmpty(), "an empty CHF subtree still reports CHF, at zero");
        assertEquals(total.amount(CHF).compareTo(BigDecimal.ZERO), 0);
    }

    private static String addAccount(
            WritableAccStore store, String name, AccountType type, CommodityId commodity, String parentId) {
        String id = newGuid();
        store.addAccount(new Account(
                id, name, type, Optional.empty(), Optional.empty(), Optional.of(commodity), Optional.of(parentId)));
        return id;
    }

    private static void addTransaction(
            WritableAccStore store, CommodityId currency, LocalDate date, String accountId, BigDecimal amount) {
        addTransaction(store, currency, date, accountId, amount, amount.negate());
    }

    /**
     * Posts a two-split transaction whose counterpart lands on {@code Revenus} - outside the {@code Actif} subtree the
     * tests assert on. The counterpart quantity is passed separately because a share purchase moves a share count on
     * one side and money on the other.
     */
    private static void addTransaction(
            WritableAccStore store,
            CommodityId currency,
            LocalDate date,
            String accountId,
            BigDecimal amount,
            BigDecimal counterpartAmount) {
        String txId = newGuid();
        String counterpartId =
                store.accountByName("Root Account:Revenus").orElseThrow().id();
        List<Split> splits = List.of(
                new Split(
                        newGuid(),
                        txId,
                        accountId,
                        date,
                        ReconcileState.NOT_RECONCILED,
                        Optional.empty(),
                        amount,
                        amount),
                new Split(
                        newGuid(),
                        txId,
                        counterpartId,
                        date,
                        ReconcileState.NOT_RECONCILED,
                        Optional.empty(),
                        counterpartAmount,
                        counterpartAmount));
        store.addTransaction(new Transaction(txId, currency, Optional.empty(), date, "balance test", splits));
    }

    private static String newGuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
