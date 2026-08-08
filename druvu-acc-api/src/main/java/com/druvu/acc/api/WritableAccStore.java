package com.druvu.acc.api;

import com.druvu.acc.api.entity.Account;
import com.druvu.acc.api.entity.Commodity;
import com.druvu.acc.api.entity.CommodityId;
import com.druvu.acc.api.entity.Price;
import com.druvu.acc.api.entity.Transaction;
import java.io.IOException;
import java.nio.file.Path;

/**
 * A mutable {@link AccStore} that supports adding/removing entities and persisting the result back to its backend.
 *
 * <p>Obtain a writable store via {@link AccStore#loadWritable(Path)} (or by casting an {@link AccStore} whose
 * implementation supports writing). Mutations are applied in place; call {@link #save(Path)} to serialize the current
 * state to a file.
 *
 * <p>Entity IDs are caller-supplied; use {@link #newId()} to mint one in whatever format the backend expects.
 *
 * @author Deniss Larka <br>
 *     on 07 Jun 2026
 */
public interface WritableAccStore extends AccStore {

    /**
     * Mints a fresh entity ID in this backend's own format.
     *
     * <p>What a valid ID looks like is a property of the storage format, not of the caller: the GnuCash XML backend
     * wants a 32-character hex GUID, another backend may not. Callers should not hand-roll one.
     *
     * @return an ID not currently used by any entity in this store
     */
    String newId();

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
     * Adds a commodity definition (currency or security).
     *
     * @param commodity the commodity to add
     * @throws IllegalArgumentException if a commodity with the same namespace and symbol already exists
     */
    void addCommodity(Commodity commodity);

    /**
     * Removes a commodity definition by its identifier.
     *
     * @param commodityId the namespace + symbol of the commodity to remove
     * @throws IllegalArgumentException if no matching commodity exists
     */
    void removeCommodity(CommodityId commodityId);

    /**
     * Adds a price quote to the price database.
     *
     * @param price the price to add
     * @throws IllegalArgumentException if a price with the same ID already exists
     */
    void addPrice(Price price);

    /**
     * Removes a price quote by ID.
     *
     * @param priceId the ID of the price to remove
     * @throws IllegalArgumentException if no price with the given ID exists
     */
    void removePrice(String priceId);

    /**
     * Persists the current state of the store to the given path.
     *
     * <p>The output format is determined by the implementation (for GnuCash XML, gzip compression is applied when the
     * path ends with {@code .gnucash} or {@code .gz}).
     *
     * <p>The book is {@link #validate() validated} first and nothing is written if it fails: a structurally broken file
     * is worse than no file, and GnuCash reacts badly to one.
     *
     * @param path the path to write to
     * @throws IOException if the file cannot be written
     * @throws IllegalStateException if the book is structurally invalid; the message lists every problem found
     */
    void save(Path path) throws IOException;
}
