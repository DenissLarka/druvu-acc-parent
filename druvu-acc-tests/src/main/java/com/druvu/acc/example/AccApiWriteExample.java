package com.druvu.acc.example;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.druvu.acc.api.entity.Account;
import com.druvu.acc.api.service.AccountService;
import com.druvu.acc.api.AccStore;
import com.druvu.acc.api.entity.Transaction;
import com.druvu.acc.loader.AccStoreFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * Example with writing operations.
 *
 * @author : Deniss Larka
 * on 13 Jan 2026
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

		final AccountService service = AccountService.create(store,"Root Account2");

		final Account revenus = service.accountByName("Revenus");
		final Account depenses = service.accountByName("Dépenses");

		//IN PROGRESS



		log.info("{}", depenses);

		for (Transaction tx : store.transactions()) {
			log.info("{}", tx);
		}
		log.info("{}", store);
	}
}
