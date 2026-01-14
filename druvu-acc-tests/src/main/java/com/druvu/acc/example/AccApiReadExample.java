package com.druvu.acc.example;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.druvu.acc.api.AccStore;
import com.druvu.acc.api.AccTransaction;
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

		for (AccTransaction tx : store.transactions()) {
			log.info("{}", tx);
		}
		log.info("{}", store);
	}
}
