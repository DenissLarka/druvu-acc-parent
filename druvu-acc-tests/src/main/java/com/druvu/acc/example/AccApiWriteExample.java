package com.druvu.acc.example;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import com.druvu.acc.api.AccAccount;
import com.druvu.acc.api.AccStore;
import com.druvu.acc.api.AccTransaction;
import com.druvu.acc.loader.AccStoreFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * Example with writing operations.
 *
 * @author : Deniss Larka
 * on 13 janvier 2026
 **/
@Slf4j
public class AccApiWriteExample {

	static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("Usage: AccApiExample <gnucash-file>");
			System.exit(1);
		}
		Path filePath = Paths.get(args[0]);
		new AccApiWriteExample().run(filePath);
	}

	private void run(Path filePath) {
		log.info("Loading file: {}", filePath);
		final AccStore store = AccStoreFactory.load(filePath);

		final Optional<AccAccount> revenusOpt = store.accountByName("Root Account:Revenus");
		if(revenusOpt.isEmpty()) {
			throw new IllegalStateException("Account not found");
		}
		final AccAccount revenus = revenusOpt.get();

		store.rootAccounts()

		log.info("{}", revenus);

		for (AccTransaction tx : store.transactions()) {
			log.info("{}", tx);
		}
		log.info("{}", store);
	}
}
