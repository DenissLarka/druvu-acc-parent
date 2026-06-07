package com.druvu.acc.test;

import com.druvu.acc.api.AccStore;
import com.druvu.acc.api.WritableAccStore;
import com.druvu.acc.api.entity.Account;
import com.druvu.acc.api.entity.AccountType;
import com.druvu.acc.api.entity.Commodity;
import com.druvu.acc.api.entity.CommodityId;
import com.druvu.acc.api.entity.Price;
import com.druvu.acc.api.entity.ReconcileState;
import com.druvu.acc.api.entity.Split;
import com.druvu.acc.api.entity.Transaction;
import com.druvu.acc.loader.AccStoreFactory;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

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
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Round-trip tests for the writable store: mutate, save, reload, assert.
 */
public class TestWriteRoundTrip {

	private Path source;

	@BeforeClass
	public void setUp() throws URISyntaxException {
		source = Paths.get(getClass().getResource("/common.gnucash").toURI());
	}

	@Test
	public void addAccountRoundTrips() throws IOException {
		WritableAccStore store = AccStoreFactory.loadWritable(source);
		int before = store.accounts().size();
		String rootId = store.rootAccounts().get(0).id();
		String id = newGuid();

		store.addAccount(new Account(
				id,
				"Round Trip Test",
				AccountType.EXPENSE,
				Optional.empty(),
				Optional.of("created by test"),
				Optional.of(CommodityId.currency("EUR")),
				Optional.of(rootId)));

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
		WritableAccStore store = AccStoreFactory.loadWritable(source);
		List<Account> accounts = store.accounts();
		String accountA = accounts.get(0).id();
		String accountB = accounts.get(1).id();
		LocalDate date = LocalDate.of(2026, 6, 7);
		int before = store.transactions().size();

		String txId = newGuid();
		List<Split> splits = List.of(
				new Split(newGuid(), txId, accountA, date, ReconcileState.NOT_RECONCILED,
						Optional.empty(), new BigDecimal("10.00"), new BigDecimal("10.00")),
				new Split(newGuid(), txId, accountB, date, ReconcileState.NOT_RECONCILED,
						Optional.empty(), new BigDecimal("-10.00"), new BigDecimal("-10.00")));

		store.addTransaction(new Transaction(
				txId, CommodityId.currency("EUR"), Optional.of("42"), date, "Round Trip Tx", splits));

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
		WritableAccStore store = AccStoreFactory.loadWritable(source);
		Transaction victim = store.transactions().get(0);
		int before = store.transactions().size();

		store.removeTransaction(victim.id());
		AccStore reloaded = saveAndReload(store);

		assertEquals(reloaded.transactions().size(), before - 1);
		assertTrue(reloaded.transactionById(victim.id()).isEmpty(), "removed transaction should be gone");
	}

	@Test
	public void removeAccountRoundTrips() throws IOException {
		WritableAccStore store = AccStoreFactory.loadWritable(source);
		String id = newGuid();
		store.addAccount(new Account(id, "Temp", AccountType.EXPENSE, Optional.empty(),
				Optional.empty(), Optional.of(CommodityId.currency("EUR")),
				Optional.of(store.rootAccounts().get(0).id())));
		int afterAdd = store.accounts().size();

		store.removeAccount(id);
		AccStore reloaded = saveAndReload(store);

		assertEquals(reloaded.accounts().size(), afterAdd - 1);
		assertTrue(reloaded.accountById(id).isEmpty());
	}

	@Test
	public void addCommodityRoundTrips() throws IOException {
		WritableAccStore store = AccStoreFactory.loadWritable(source);
		int before = store.commodities().size();
		CommodityId apple = new CommodityId("NASDAQ", "AAPL");

		store.addCommodity(Commodity.security("NASDAQ", "AAPL", "Apple Inc.", 10000));

		AccStore reloaded = saveAndReload(store);

		assertEquals(reloaded.commodities().size(), before + 1);
		assertTrue(reloaded.commodities().contains(apple), "added commodity should be present after reload");
	}

	@Test
	public void addPriceRoundTrips() throws IOException {
		WritableAccStore store = AccStoreFactory.loadWritable(source);
		store.addCommodity(Commodity.security("NASDAQ", "AAPL", "Apple Inc.", 10000));
		int before = store.prices().size();

		CommodityId apple = new CommodityId("NASDAQ", "AAPL");
		CommodityId usd = CommodityId.currency("USD");
		String priceId = newGuid();
		LocalDateTime when = LocalDateTime.of(2026, 6, 7, 12, 0, 0);

		store.addPrice(new Price(priceId, apple, usd, when, "user:price-editor",
				Optional.of("last"), new BigDecimal("212.50")));

		AccStore reloaded = saveAndReload(store);

		assertEquals(reloaded.prices().size(), before + 1);
		Price found = reloaded.prices().stream()
				.filter(p -> p.id().equals(priceId)).findFirst().orElse(null);
		assertTrue(found != null, "added price should be present after reload");
		assertEquals(found.commodity(), apple);
		assertEquals(found.currency(), usd);
		assertEquals(found.value().compareTo(new BigDecimal("212.50")), 0);
		assertEquals(found.source(), "user:price-editor");
		assertEquals(found.type(), Optional.of("last"));
	}

	@Test
	public void savedFileDeclaresGnuCashNamespaces() throws IOException {
		WritableAccStore store = AccStoreFactory.loadWritable(source);
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
		}
		finally {
			Files.deleteIfExists(out);
		}
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void addDuplicateAccountFails() {
		WritableAccStore store = AccStoreFactory.loadWritable(source);
		store.addAccount(store.accounts().get(0));
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void removeMissingTransactionFails() {
		WritableAccStore store = AccStoreFactory.loadWritable(source);
		store.removeTransaction("does-not-exist");
	}

	private AccStore saveAndReload(WritableAccStore store) throws IOException {
		Path out = Files.createTempFile("acc-write", ".gnucash");
		try {
			store.save(out);
			return AccStoreFactory.load(out);
		}
		finally {
			Files.deleteIfExists(out);
		}
	}

	private static String newGuid() {
		return UUID.randomUUID().toString().replace("-", "");
	}
}
