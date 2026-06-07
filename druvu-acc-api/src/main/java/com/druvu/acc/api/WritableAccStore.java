package com.druvu.acc.api;

import java.io.IOException;
import java.nio.file.Path;

import com.druvu.acc.api.entity.Account;
import com.druvu.acc.api.entity.Transaction;

/**
 * A mutable {@link AccStore} that supports adding/removing entities and persisting
 * the result back to its backend.
 * <p>
 * Obtain a writable store via {@link AccStoreFactory#loadWritable(Path)} (or by casting an
 * {@link AccStore} whose implementation supports writing). Mutations are applied in place;
 * call {@link #save(Path)} to serialize the current state to a file.
 * <p>
 * Entity IDs are caller-supplied. For GnuCash compatibility use 32-character hex GUIDs
 * (e.g. {@code UUID.randomUUID().toString().replace("-", "")}).
 *
 * @author Deniss Larka
 *         <br/>on 07 Jun 2026
 */
public interface WritableAccStore extends AccStore {

	/**
	 * Adds a new account to the store.
	 *
	 * @param account the account to add
	 * @throws IllegalArgumentException if an account with the same ID already exists
	 */
	void addAccount(Account account);

	/**
	 * Adds a new transaction (with its splits) to the store.
	 *
	 * @param transaction the transaction to add
	 * @throws IllegalArgumentException if a transaction with the same ID already exists
	 */
	void addTransaction(Transaction transaction);

	/**
	 * Removes an account by ID. Splits referencing the account are not cascaded.
	 *
	 * @param accountId the ID of the account to remove
	 * @throws IllegalArgumentException if no account with the given ID exists
	 */
	void removeAccount(String accountId);

	/**
	 * Removes a transaction (and its splits) by ID.
	 *
	 * @param transactionId the ID of the transaction to remove
	 * @throws IllegalArgumentException if no transaction with the given ID exists
	 */
	void removeTransaction(String transactionId);

	/**
	 * Persists the current state of the store to the given path.
	 * <p>
	 * The output format is determined by the implementation (for GnuCash XML, gzip
	 * compression is applied when the path ends with {@code .gnucash} or {@code .gz}).
	 *
	 * @param path the path to write to
	 * @throws IOException if the file cannot be written
	 */
	void save(Path path) throws IOException;
}
