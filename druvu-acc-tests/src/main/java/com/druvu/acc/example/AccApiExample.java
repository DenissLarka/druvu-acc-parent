package com.druvu.acc.example;

import java.nio.file.Paths;

import com.druvu.acc.api.AccBook;
import com.druvu.acc.loader.AccBookFactory;

/**
 * Example usage of Acc API.
 *
 * @author : Deniss Larka
 * <br/>on 11 jan 2026
 **/
public class AccApiExample {

	static void main() {
		AccApiExample accApiExample = new AccApiExample();
		accApiExample.go();
	}

	private void go() {

		final AccBook book = AccBookFactory.load(Paths.get("/Users/deniss/data/GNUCASH/book3.gnucash"));

		book.accounts().forEach(System.out::println);
		System.out.println(book);
	}
}
