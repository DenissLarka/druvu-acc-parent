package com.druvu.acc.example;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.druvu.acc.api.AccAccount;
import com.druvu.acc.api.AccService;
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

		final AccService service = AccService.create(store,"Root Account");

		final AccAccount revenus = service.accountByName("Revenus");
		final AccAccount depenses = service.accountByName("Dépenses");

		service.transaction(depenses,revenus,new BigDecimal("1000"));

		log.info("{}", depenses);

		for (AccTransaction tx : store.transactions()) {
			log.info("{}", tx);
		}
		log.info("{}", store);
	}
}
