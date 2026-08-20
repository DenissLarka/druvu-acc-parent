package com.druvu.acc.test;

import static org.testng.Assert.*;

import com.druvu.acc.api.AccStore;
import com.druvu.acc.api.WritableAccStore;
import com.druvu.acc.api.entity.Account;
import com.druvu.acc.api.entity.AccountType;
import com.druvu.acc.api.entity.CommodityId;
import com.druvu.acc.api.entity.Split;
import com.druvu.acc.api.entity.Transaction;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.testng.annotations.Test;

/** {@link AccStore#newBook}: a book started from the bundled GnuCash-written template, no file needed. */
public class TestNewBook {

    @Test
    public void newBookStartsEmptyAndValid() {
        WritableAccStore store = AccStore.newBook(CommodityId.CHF);

        assertEquals(store.accounts().size(), 1, "only the root account exists");
        assertEquals(store.rootAccounts().getFirst().type(), AccountType.ROOT);
        assertTrue(store.transactions().isEmpty());
        assertTrue(store.commodities().contains(CommodityId.CHF));
        assertEquals(store.validate(), List.of(), "a new book must be immediately saveable");
    }

    @Test
    public void newBookCarriesTheRequestedCurrency() {
        WritableAccStore store = AccStore.newBook(CommodityId.currency("PLN"));

        assertTrue(store.commodities().contains(CommodityId.currency("PLN")));
        assertFalse(
                store.commodities().contains(CommodityId.CHF),
                "the template's own currency must not leak into the new book");
        assertEquals(store.rootAccounts().getFirst().commodity(), Optional.of(CommodityId.currency("PLN")));
    }

    @Test
    public void everyNewBookHasItsOwnIdentity() {
        WritableAccStore first = AccStore.newBook(CommodityId.CHF);
        WritableAccStore second = AccStore.newBook(CommodityId.CHF);

        assertNotEquals(first.id(), second.id(), "two books must never share a book GUID");
        assertNotEquals(
                first.rootAccounts().getFirst().id(),
                second.rootAccounts().getFirst().id(),
                "two books must never share a root-account GUID");
    }

    @Test
    public void newBookRefusesASecurityAsBookCurrency() {
        assertThrows(IllegalArgumentException.class, () -> AccStore.newBook(CommodityId.security("NASDAQ", "AAPL")));
    }

    @Test
    public void newBookRefusesACurrencyWithoutAnIsoFraction() {
        assertThrows(IllegalArgumentException.class, () -> AccStore.newBook(CommodityId.currency("XAU")));
    }

    @Test
    public void bookBuiltFromNothingRoundTrips() throws IOException {
        WritableAccStore store = AccStore.newBook(CommodityId.CHF);
        String rootId = store.rootAccounts().getFirst().id();
        LocalDate date = LocalDate.of(2026, 8, 20);

        String bankId = store.newId();
        store.addAccount(Account.of(bankId, "Bank", AccountType.BANK)
                .withCommodity(CommodityId.CHF)
                .withParent(rootId));
        String salaryId = store.newId();
        store.addAccount(Account.of(salaryId, "Salary", AccountType.INCOME)
                .withCommodity(CommodityId.CHF)
                .withParent(rootId));
        String txId = store.newId();
        store.addTransaction(Transaction.of(
                txId,
                CommodityId.CHF,
                date,
                "first salary",
                List.of(
                        Split.of(store.newId(), txId, salaryId, date, new BigDecimal("-5200.00")),
                        Split.of(store.newId(), txId, bankId, date, new BigDecimal("5200.00")))));

        Path out = Files.createTempFile("acc-new-book", ".gnucash");
        try {
            store.save(out);
            AccStore reloaded = AccStore.load(out);
            assertEquals(reloaded.accounts().size(), 3);
            assertTrue(reloaded.transactionById(txId).isPresent());
            assertEquals(reloaded.validate(), List.of());
        } finally {
            Files.deleteIfExists(out);
        }
    }
}
