package com.druvu.acc.example;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.druvu.acc.api.AccStore;
import com.druvu.acc.api.entity.Transaction;
import com.druvu.acc.api.service.AccountService;
import com.druvu.acc.loader.AccStoreFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * Example usage of Acc API.
 *
 * @author Deniss Larka
 * <br/>on 11 jan 2026
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

		final AccStore store = AccStoreFactory.load(filePath);
		final AccountService accountService = AccountService.create(store, "Root Account");

		for (var account : store.accounts()) {
			log.info("{} balance: {}", account, accountService.balance(account.id()).toPlainString());
		}

		for (Transaction tx : store.transactions()) {
			log.info("{}", tx);
		}

	}
}
