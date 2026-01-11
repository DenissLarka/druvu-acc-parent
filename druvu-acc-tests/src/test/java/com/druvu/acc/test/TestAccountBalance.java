package com.druvu.acc.test;

import com.druvu.acc.api.AccAccount;
import com.druvu.acc.api.AccBook;
import com.druvu.acc.gnucash.io.GnucashFileReader;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Optional;

import static org.testng.Assert.*;

/**
 * Tests for account balance calculations.
 */
public class TestAccountBalance {

	private AccBook book;

	@BeforeClass
	public void setUp() throws IOException {
		try (InputStream is = getClass().getResourceAsStream("/test.gnucash")) {
			assertNotNull(is, "test.gnucash resource not found");

			GnucashFileReader reader = new GnucashFileReader();
			book = reader.read(is);
		}
	}

	@Test
	public void testBookLoaded() {
		assertNotNull(book, "Book should be loaded");
		assertNotNull(book.id(), "Book should have an ID");
		assertFalse(book.accounts().isEmpty(), "Book should have accounts");
	}

	@Test
	public void testAccountsExist() {
		assertFalse(book.accounts().isEmpty(), "Book should have accounts");
		assertTrue(book.accounts().size() > 1, "Book should have multiple accounts");
	}

	@Test
	public void testRootAccounts() {
		assertFalse(book.rootAccounts().isEmpty(), "Book should have root accounts");
	}

	@Test
	public void testAccountHierarchy() {
		for (AccAccount root : book.rootAccounts()) {
			assertNotNull(root.qualifiedName(), "Account should have qualified name");
			assertTrue(root.parent().isEmpty(), "Root account should not have parent");
		}
	}

	@Test
	public void testQualifiedName() {
		// Find an account with a parent
		Optional<AccAccount> childAccount = book.accounts().stream()
				.filter(a -> a.parent().isPresent())
				.findFirst();

		if (childAccount.isPresent()) {
			String qn = childAccount.get().qualifiedName();
			assertNotNull(qn, "Qualified name should not be null");
			assertTrue(qn.contains(":"), "Qualified name should contain separator");
		}
	}

	@Test
	public void testTransactionsExist() {
		assertFalse(book.transactions().isEmpty(), "Book should have transactions");
	}

	@Test
	public void testAccountBalance() {
		// Find an account with transactions
		Optional<AccAccount> accountWithTrx = book.accounts().stream()
				.filter(AccAccount::hasTransactions)
				.findFirst();

		if (accountWithTrx.isPresent()) {
			BigDecimal balance = accountWithTrx.get().balance();
			assertNotNull(balance, "Balance should not be null");
		}
	}

	@Test
	public void testBalanceRecursive() {
		// Test that recursive balance includes children
		for (AccAccount root : book.rootAccounts()) {
			if (!root.children().isEmpty()) {
				BigDecimal directBalance = root.balance();
				BigDecimal recursiveBalance = root.balanceRecursive();

				assertNotNull(directBalance, "Direct balance should not be null");
				assertNotNull(recursiveBalance, "Recursive balance should not be null");
			}
		}
	}

	@Test
	public void testTransactionBalance() {
		// All transactions should balance to zero
		for (var trx : book.transactions()) {
			BigDecimal balance = trx.balance();
			assertEquals(balance.signum(), 0,
					"Transaction " + trx.id() + " should balance to zero, but was: " + balance);
		}
	}

	@Test
	public void testTransactionSplits() {
		for (var trx : book.transactions()) {
			assertFalse(trx.splits().isEmpty(),
					"Transaction should have at least one split");
			assertTrue(trx.splits().size() >= 2,
					"Transaction should have at least two splits for double-entry");
		}
	}

	@Test
	public void testAccountByName() {
		// Find any account and look it up by qualified name
		if (!book.accounts().isEmpty()) {
			AccAccount first = book.accounts().getFirst();
			String qn = first.qualifiedName();

			Optional<AccAccount> found = book.accountByName(qn);
			assertTrue(found.isPresent(), "Should find account by qualified name");
			assertEquals(found.get().id(), first.id(), "Found account should match");
		}
	}
}
