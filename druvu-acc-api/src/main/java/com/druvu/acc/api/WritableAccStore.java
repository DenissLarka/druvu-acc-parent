package com.druvu.acc.api;

import com.druvu.acc.api.entity.Account;
import com.druvu.acc.api.entity.BillTerm;
import com.druvu.acc.api.entity.Commodity;
import com.druvu.acc.api.entity.CommodityId;
import com.druvu.acc.api.entity.Customer;
import com.druvu.acc.api.entity.Employee;
import com.druvu.acc.api.entity.Entry;
import com.druvu.acc.api.entity.Invoice;
import com.druvu.acc.api.entity.Job;
import com.druvu.acc.api.entity.Order;
import com.druvu.acc.api.entity.Price;
import com.druvu.acc.api.entity.TaxTable;
import com.druvu.acc.api.entity.Transaction;
import com.druvu.acc.api.entity.Vendor;
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
     * <p>The transaction must balance: its splits' {@link com.druvu.acc.api.entity.Split#value() values} - all
     * denominated in the transaction's currency - must sum to zero. Quantities are not checked; a share purchase
     * legitimately moves an unequal share count against money.
     *
     * @param transaction the transaction to add
     * @throws IllegalArgumentException if a transaction with the same ID already exists, or if the splits do not sum to
     *     zero
     */
    void addTransaction(Transaction transaction);

    /**
     * Replaces an existing account with an edited copy of itself, in place - the account keeps its position in the
     * book. This is how a change made with one of {@link Account}'s {@code with...} methods is stored:
     *
     * <p>{@code store.updateAccount(store.accountById(id).orElseThrow().withPlaceholder(true));}
     *
     * <p>The replacement must keep the original's ID, since that is what identifies the account being replaced.
     *
     * @param account the edited account
     * @throws IllegalArgumentException if no account with that ID exists
     */
    void updateAccount(Account account);

    /**
     * Replaces an existing transaction, and all of its splits, with an edited copy of itself, in place.
     *
     * <p>The edited transaction must balance, under the same rule as {@link #addTransaction(Transaction)} - an edit
     * must not unbalance the books any more than an addition may.
     *
     * @param transaction the edited transaction
     * @throws IllegalArgumentException if no transaction with that ID exists, or if the splits do not sum to zero
     */
    void updateTransaction(Transaction transaction);

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
     * Adds a customer. The referenced billing terms and tax table, if any, must already exist in the book - their
     * reference counts are maintained by the store.
     *
     * @param customer the customer to add
     * @throws IllegalArgumentException if a customer with the same ID exists, or a referenced billing term or tax table
     *     does not
     */
    void addCustomer(Customer customer);

    /**
     * Updates a customer by writing its fields onto the stored element; anything this library does not model survives
     * the edit untouched. Reference counts of billing terms and tax tables follow the change.
     *
     * @param customer the customer to update, matched by ID
     * @throws IllegalArgumentException if no customer with that ID exists, or a referenced billing term or tax table
     *     does not
     */
    void updateCustomer(Customer customer);

    /**
     * Removes a customer by its ID.
     *
     * @param customerId the customer ID
     * @throws IllegalArgumentException if no matching customer exists
     */
    void removeCustomer(String customerId);

    /**
     * Adds a vendor. The referenced billing terms and tax table, if any, must already exist in the book.
     *
     * @param vendor the vendor to add
     * @throws IllegalArgumentException if a vendor with the same ID exists, or a referenced billing term or tax table
     *     does not
     */
    void addVendor(Vendor vendor);

    /**
     * Updates a vendor by writing its fields onto the stored element; anything this library does not model survives the
     * edit untouched.
     *
     * @param vendor the vendor to update, matched by ID
     * @throws IllegalArgumentException if no vendor with that ID exists, or a referenced billing term or tax table does
     *     not
     */
    void updateVendor(Vendor vendor);

    /**
     * Removes a vendor by its ID.
     *
     * @param vendorId the vendor ID
     * @throws IllegalArgumentException if no matching vendor exists
     */
    void removeVendor(String vendorId);

    /**
     * Adds an employee.
     *
     * @param employee the employee to add
     * @throws IllegalArgumentException if an employee with the same ID exists
     */
    void addEmployee(Employee employee);

    /**
     * Updates an employee by writing its fields onto the stored element; anything this library does not model - the
     * access-control string, the default credit-card account - survives the edit untouched.
     *
     * @param employee the employee to update, matched by ID
     * @throws IllegalArgumentException if no employee with that ID exists
     */
    void updateEmployee(Employee employee);

    /**
     * Removes an employee by its ID.
     *
     * @param employeeId the employee ID
     * @throws IllegalArgumentException if no matching employee exists
     */
    void removeEmployee(String employeeId);

    /**
     * Adds a tax table. Every entry's account must already exist in the book.
     *
     * <p>There is deliberately no tax table update: GnuCash versions a table that is in use by freezing an invisible
     * copy, so documents keep the rates they were posted with - an in-place edit would falsify them. Add a new table
     * instead.
     *
     * @param taxTable the tax table to add
     * @throws IllegalArgumentException if a tax table with the same ID exists, or an entry's account does not
     */
    void addTaxTable(TaxTable taxTable);

    /**
     * Removes a tax table by its ID. Refused while anything in the book - a customer, vendor, invoice entry or another
     * tax table version - still references it.
     *
     * @param taxTableId the tax table ID
     * @throws IllegalArgumentException if no matching tax table exists
     * @throws IllegalStateException if the tax table is still referenced
     */
    void removeTaxTable(String taxTableId);

    /**
     * Adds a billing term.
     *
     * <p>There is deliberately no billing term update, for the same reason as {@link #addTaxTable(TaxTable)}.
     *
     * @param term the billing term to add
     * @throws IllegalArgumentException if a billing term with the same ID exists
     */
    void addBillTerm(BillTerm term);

    /**
     * Removes a billing term by its ID. Refused while anything in the book - a customer, vendor, invoice or another
     * term version - still references it.
     *
     * @param termId the billing term ID
     * @throws IllegalArgumentException if no matching billing term exists
     * @throws IllegalStateException if the billing term is still referenced
     */
    void removeBillTerm(String termId);

    /**
     * Adds a job. Its owner - a customer or vendor - must already exist in the book.
     *
     * @param job the job to add
     * @throws IllegalArgumentException if a job with the same ID exists, the owner is missing, or the owner is not a
     *     customer or vendor
     */
    void addJob(Job job);

    /**
     * Updates a job by writing its fields onto the stored element.
     *
     * @param job the job to update, matched by ID
     * @throws IllegalArgumentException if no job with that ID exists, or the new owner is missing or of the wrong kind
     */
    void updateJob(Job job);

    /**
     * Removes a job by its ID. Refused while a document still names it as owner.
     *
     * @param jobId the job ID
     * @throws IllegalArgumentException if no matching job exists
     * @throws IllegalStateException if a document still references the job
     */
    void removeJob(String jobId);

    /**
     * Adds an order. Its owner must already exist in the book.
     *
     * @param order the order to add
     * @throws IllegalArgumentException if an order with the same ID exists, or the owner is missing
     */
    void addOrder(Order order);

    /**
     * Updates an order by writing its fields onto the stored element.
     *
     * @param order the order to update, matched by ID
     * @throws IllegalArgumentException if no order with that ID exists, or the new owner is missing
     */
    void updateOrder(Order order);

    /**
     * Removes an order by its ID. Refused while an entry still references it.
     *
     * @param orderId the order ID
     * @throws IllegalArgumentException if no matching order exists
     * @throws IllegalStateException if an entry still references the order
     */
    void removeOrder(String orderId);

    /**
     * Adds an invoice-family document. Its owner, chargeback owner and billing terms, where given, must already exist
     * in the book; the terms' reference count is maintained by the store.
     *
     * @param invoice the document to add
     * @throws IllegalArgumentException if a document with the same ID exists, or a referenced party or term does not
     */
    void addInvoice(Invoice invoice);

    /**
     * Updates a document by writing its fields onto the stored element; anything this library does not model - a
     * credit-note slot, for instance - survives the edit untouched.
     *
     * @param invoice the document to update, matched by ID
     * @throws IllegalArgumentException if no document with that ID exists, or a referenced party or term does not
     */
    void updateInvoice(Invoice invoice);

    /**
     * Removes a document by its ID. Refused for a posted document - unpost it in GnuCash first, since the posting's
     * ledger transaction and lot are not this library's to unwind - and while an entry still references it.
     *
     * @param invoiceId the document ID
     * @throws IllegalArgumentException if no matching document exists
     * @throws IllegalStateException if the document is posted, or an entry still references it
     */
    void removeInvoice(String invoiceId);

    /**
     * Adds an entry. Every reference it carries - its documents, accounts, tax tables, order, chargeback owner - must
     * already exist in the book.
     *
     * @param entry the entry to add
     * @throws IllegalArgumentException if an entry with the same ID exists, or a reference points at nothing
     */
    void addEntry(Entry entry);

    /**
     * Updates an entry by writing its fields onto the stored element; slots survive the edit untouched.
     *
     * @param entry the entry to update, matched by ID
     * @throws IllegalArgumentException if no entry with that ID exists, or a reference points at nothing
     */
    void updateEntry(Entry entry);

    /**
     * Removes an entry by its ID.
     *
     * @param entryId the entry ID
     * @throws IllegalArgumentException if no matching entry exists
     */
    void removeEntry(String entryId);

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
