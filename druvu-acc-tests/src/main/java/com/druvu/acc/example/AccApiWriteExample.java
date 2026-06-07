package com.druvu.acc.example;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.druvu.acc.api.AccStore;
import com.druvu.acc.api.WritableAccStore;
import com.druvu.acc.api.entity.Account;
import com.druvu.acc.api.entity.AccountType;
import com.druvu.acc.api.entity.CommodityId;
import com.druvu.acc.api.entity.ReconcileState;
import com.druvu.acc.api.entity.Split;
import com.druvu.acc.api.entity.Transaction;
import com.druvu.acc.loader.AccStoreFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * Example showing how to load a GnuCash file, add an account and a transaction,
 * and save the result to a new file.
 *
 * @author Deniss Larka
 *         <br/>on 13 Jan 2026
 */
@Slf4j
public class AccApiWriteExample {

	static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println("Usage: AccApiWriteExample <gnucash-file>");
			System.exit(1);
		}
		new AccApiWriteExample().run(Paths.get(args[0]));
	}

	private void run(Path filePath) throws Exception {
		log.info("Loading file: {}", filePath);
		WritableAccStore store = AccStoreFactory.loadWritable(filePath);
		log.info("Loaded {} accounts, {} transactions",
				store.accounts().size(), store.transactions().size());

		String rootId = store.rootAccounts().getFirst().id();
		CommodityId eur = CommodityId.currency("EUR");

		// 1. Add a new expense account under the first root account.
		String accountId = newGuid();
		store.addAccount(new Account(
				accountId,
				"Coffee",
				AccountType.EXPENSE,
				Optional.empty(),
				Optional.of("Created by AccApiWriteExample"),
				Optional.of(eur),
				Optional.of(rootId)));

		// 2. Add a balanced transaction touching that account.
		String txId = newGuid();
		LocalDate today = LocalDate.now();
		List<Split> splits = List.of(
				new Split(newGuid(), txId, accountId, today, ReconcileState.NOT_RECONCILED,
						Optional.empty(), new BigDecimal("4.50"), new BigDecimal("4.50")),
				new Split(newGuid(), txId, rootId, today, ReconcileState.NOT_RECONCILED,
						Optional.empty(), new BigDecimal("-4.50"), new BigDecimal("-4.50")));
		store.addTransaction(new Transaction(
				txId, eur, Optional.empty(), today, "Morning coffee", splits));

		// 3. Save to a sibling file.
		Path output = siblingOutput(filePath);
		store.save(output);
		log.info("Saved modified store to: {}", output);

		// 4. Reload and confirm.
		AccStore reloaded = AccStoreFactory.load(output);
		log.info("Reloaded {} accounts, {} transactions",
				reloaded.accounts().size(), reloaded.transactions().size());
		log.info("New account present: {}", reloaded.accountById(accountId).isPresent());
		log.info("New transaction present: {}", reloaded.transactionById(txId).isPresent());
	}

	private static Path siblingOutput(Path input) {
		String name = input.getFileName().toString();
		int dot = name.lastIndexOf('.');
		String modified = dot < 0 ? name + "-modified" : name.substring(0, dot) + "-modified" + name.substring(dot);
		Path parent = input.toAbsolutePath().getParent();
		return parent == null ? Paths.get(modified) : parent.resolve(modified);
	}

	private static String newGuid() {
		return UUID.randomUUID().toString().replace("-", "");
	}
}
