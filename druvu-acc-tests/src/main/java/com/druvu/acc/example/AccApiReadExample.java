package com.druvu.acc.example;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.Month;

import com.druvu.acc.api.entity.Account;
import com.druvu.acc.api.service.AccountService;
import com.druvu.acc.api.AccStore;
import com.druvu.acc.api.entity.Transaction;
import com.druvu.acc.loader.AccStoreFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * Example usage of Acc API.
 *
 * @author Deniss Larka
 *         <br/>on 11 jan 2026
 */
@Slf4j
public class AccApiReadExample {

	static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("Usage: AccApiExample <gnucash-file>");
			System.exit(1);
		}
		Path filePath = Paths.get(args[0]);
		new AccApiReadExample().run(filePath);
	}

	void run(Path filePath) {
		log.info("Loading file: {}", filePath);
		final AccStore store = AccStoreFactory.load(filePath);

		store.accounts().forEach(account -> log.info("{}", account));

		for (Transaction tx : store.transactions()) {
			log.info("{}", tx);
		}

		final AccountService service = AccountService.create(store, "Root Account2");
		final Account revenue = service.accountByName("Revenus");

		final BigDecimal balance = service.balance(revenue, LocalDate.of(2026, Month.JANUARY, 14));

		log.info("{}", balance.toPlainString());
	}
}
