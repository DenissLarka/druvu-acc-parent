package com.druvu.acc.api;

import com.druvu.acc.api.entity.Account;
import com.druvu.acc.api.entity.BillTerm;
import com.druvu.acc.api.entity.CommodityId;
import com.druvu.acc.api.entity.Customer;
import com.druvu.acc.api.entity.Employee;
import com.druvu.acc.api.entity.Entry;
import com.druvu.acc.api.entity.Invoice;
import com.druvu.acc.api.entity.Job;
import com.druvu.acc.api.entity.Order;
import com.druvu.acc.api.entity.Price;
import com.druvu.acc.api.entity.Split;
import com.druvu.acc.api.entity.TaxTable;
import com.druvu.acc.api.entity.Transaction;
import com.druvu.acc.api.entity.Vendor;
import com.druvu.lib.loader.ComponentLoader;
import com.druvu.lib.loader.Dependencies;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Interface representing an abstraction of storing account entities to underlying backend storage.
 *
 * <p>This is the main entry point for working with accounting data. Implementations may use different backends (XML
 * files, SQL databases, etc.).
 *
 * @author Deniss Larka <br>
 *     on 11 Jan 2026
 */
public interface AccStore {

    // ========== Loading ==========

    /**
     * Loads a store from the given file, auto-discovering the format implementation via ServiceLoader.
     *
     * @param path path to the file to load
     * @return the loaded store
     */
    static AccStore load(Path path) {
        return ComponentLoader.load(AccStore.class, Dependencies.of(Path.class, path));
    }

    /**
     * Loads a store and returns it as a {@link WritableAccStore} for mutation and saving.
     *
     * @param path path to the file to load
     * @return the loaded store, supporting mutation and {@link WritableAccStore#save}
     * @throws UnsupportedOperationException if the discovered implementation does not support writing
     */
    static WritableAccStore loadWritable(Path path) {
        AccStore store = load(path);
        if (store instanceof WritableAccStore writable) {
            return writable;
        }
        throw new UnsupportedOperationException(
                "Loaded store does not support writing: " + store.getClass().getName());
    }

    // ========== Book Metadata ==========

    /** @return the book/store ID */
    String id();

    // ========== Commodities ==========

    /** @return all commodity IDs (currencies, stocks, etc.) defined in this store */
    List<CommodityId> commodities();

    // ========== Prices ==========

    /** @return all price quotes in this store */
    List<Price> prices();

    // ========== Business parties ==========

    /** @return all customers */
    List<Customer> customers();

    /**
     * Finds a customer by its ID.
     *
     * @param id the customer ID
     * @return the customer, if present
     */
    Optional<Customer> customerById(String id);

    /** @return all vendors */
    List<Vendor> vendors();

    /**
     * Finds a vendor by its ID.
     *
     * @param id the vendor ID
     * @return the vendor, if present
     */
    Optional<Vendor> vendorById(String id);

    /** @return all employees */
    List<Employee> employees();

    /**
     * Finds an employee by its ID.
     *
     * @param id the employee ID
     * @return the employee, if present
     */
    Optional<Employee> employeeById(String id);

    /**
     * All tax tables, including the invisible frozen versions GnuCash keeps so old documents retain their rates - a
     * document may reference one of those by ID.
     *
     * @return all tax tables
     */
    List<TaxTable> taxTables();

    /**
     * Finds a tax table by its ID.
     *
     * @param id the tax table ID
     * @return the tax table, if present
     */
    Optional<TaxTable> taxTableById(String id);

    /** @return all billing terms */
    List<BillTerm> billTerms();

    /**
     * Finds a billing term by its ID.
     *
     * @param id the billing term ID
     * @return the billing term, if present
     */
    Optional<BillTerm> billTermById(String id);

    /** @return all jobs */
    List<Job> jobs();

    /**
     * Finds a job by its ID.
     *
     * @param id the job ID
     * @return the job, if present
     */
    Optional<Job> jobById(String id);

    /** @return all orders */
    List<Order> orders();

    /**
     * Finds an order by its ID.
     *
     * @param id the order ID
     * @return the order, if present
     */
    Optional<Order> orderById(String id);

    /** @return all invoice-family documents: customer invoices, vendor bills, employee expense vouchers */
    List<Invoice> invoices();

    /**
     * Finds an invoice-family document by its ID.
     *
     * @param id the document ID
     * @return the document, if present
     */
    Optional<Invoice> invoiceById(String id);

    /** @return all document lines */
    List<Entry> entries();

    /**
     * Finds an entry by its ID.
     *
     * @param id the entry ID
     * @return the entry, if present
     */
    Optional<Entry> entryById(String id);

    /**
     * The lines of one document, whichever side of the entry points at it.
     *
     * @param invoiceId the document ID - an invoice, bill or voucher
     * @return its entries
     */
    List<Entry> entriesForInvoice(String invoiceId);

    /**
     * The document a ledger transaction came from - the reverse of GnuCash's posting.
     *
     * <p>Covers posting transactions only: payments belong to lots, which this library does not model yet.
     *
     * @param transactionId the transaction ID
     * @return the document whose posting created that transaction, if any
     */
    Optional<Invoice> invoiceForTransaction(String transactionId);

    /**
     * The customer behind a document, following the job indirection: an invoice owned by a job resolves through the job
     * to its customer. Empty for vendor bills and employee vouchers.
     *
     * @param invoiceId the document ID
     * @return the customer the document ultimately bills, if it bills one
     */
    Optional<Customer> customerForInvoice(String invoiceId);

    /**
     * The customer behind a ledger transaction: {@link #invoiceForTransaction(String)} chained with
     * {@link #customerForInvoice(String)} - who a posted invoice transaction bills.
     *
     * @param transactionId the transaction ID
     * @return the customer, when the transaction posted a customer invoice
     */
    Optional<Customer> customerForTransaction(String transactionId);

    // ========== Validation ==========

    /**
     * Checks the book's structure and reports what is wrong with it.
     *
     * <p>Reading is deliberately tolerant - a damaged book still loads, because inspecting or repairing one is a
     * legitimate reason to reach for this library. This method is how a caller asks the question explicitly. Writing is
     * strict by contrast: {@link WritableAccStore#save} refuses to emit a book that fails these checks.
     *
     * <p>Structural problems only - a dangling parent, a second root, a split pointing at an account that is not there,
     * a commodity the book never declares. Accounting rules are not checked: whether a transaction's splits sum to zero
     * is the caller's business.
     *
     * @return one description per problem found, empty if the book is structurally sound
     */
    List<String> validate();

    // ========== Accounts ==========

    /** @return all accounts */
    List<Account> accounts();

    /** @return root accounts (accounts without parent) */
    List<Account> rootAccounts();

    /**
     * Finds an account by its ID.
     *
     * @param id the account ID
     * @return the account if found
     */
    Optional<Account> accountById(String id);

    /**
     * Finds an account by its qualified name.
     *
     * @param qualifiedName the qualified name (e.g., "Assets:Bank:Checking")
     * @return the account if found
     */
    Optional<Account> accountByName(String qualifiedName);

    /**
     * Fetches the IDs of child accounts for a given account.
     *
     * @param accountId the parent account ID
     * @return list of child account IDs (empty if no children)
     */
    List<String> fetchChildIds(String accountId);

    // ========== Transactions ==========

    /** @return all transactions sorted by date */
    List<Transaction> transactions();

    /**
     * Finds a transaction by its ID.
     *
     * @param id the transaction ID
     * @return the transaction if found
     */
    Optional<Transaction> transactionById(String id);

    /**
     * Gets transactions in a date range.
     *
     * @param from start date (inclusive)
     * @param to end date (inclusive)
     * @return transactions in the range
     */
    List<Transaction> transactions(LocalDate from, LocalDate to);

    /**
     * Gets all transactions affecting a specific account.
     *
     * @param accountId the account ID
     * @return transactions affecting the account
     */
    List<Transaction> transactionsForAccount(String accountId);

    // ========== Splits ==========

    /**
     * Gets all splits for a specific account.
     *
     * @param accountId the account ID
     * @return splits affecting the account
     */
    List<Split> splitsForAccount(String accountId);
}
