package com.druvu.acc.gnucash.impl;

import com.druvu.acc.api.WritableAccStore;
import com.druvu.acc.api.entity.Account;
import com.druvu.acc.api.entity.AccountType;
import com.druvu.acc.api.entity.BillTerm;
import com.druvu.acc.api.entity.Commodity;
import com.druvu.acc.api.entity.CommodityId;
import com.druvu.acc.api.entity.Customer;
import com.druvu.acc.api.entity.Employee;
import com.druvu.acc.api.entity.Entry;
import com.druvu.acc.api.entity.Invoice;
import com.druvu.acc.api.entity.Job;
import com.druvu.acc.api.entity.Order;
import com.druvu.acc.api.entity.Owner;
import com.druvu.acc.api.entity.OwnerType;
import com.druvu.acc.api.entity.Price;
import com.druvu.acc.api.entity.Split;
import com.druvu.acc.api.entity.TaxTable;
import com.druvu.acc.api.entity.TaxTablePolicy;
import com.druvu.acc.api.entity.Transaction;
import com.druvu.acc.api.entity.Vendor;
import com.druvu.acc.gnucash.generated.GncAccount;
import com.druvu.acc.gnucash.generated.GncCountData;
import com.druvu.acc.gnucash.generated.GncPricedb;
import com.druvu.acc.gnucash.generated.GncTransaction;
import com.druvu.acc.gnucash.generated.GncV2;
import com.druvu.acc.gnucash.mapper.AccountMapper;
import com.druvu.acc.gnucash.mapper.BillTermMapper;
import com.druvu.acc.gnucash.mapper.CommodityMapper;
import com.druvu.acc.gnucash.mapper.CustomerMapper;
import com.druvu.acc.gnucash.mapper.EmployeeMapper;
import com.druvu.acc.gnucash.mapper.EntryMapper;
import com.druvu.acc.gnucash.mapper.InvoiceMapper;
import com.druvu.acc.gnucash.mapper.JobMapper;
import com.druvu.acc.gnucash.mapper.OrderMapper;
import com.druvu.acc.gnucash.mapper.PriceMapper;
import com.druvu.acc.gnucash.mapper.TaxTableMapper;
import com.druvu.acc.gnucash.mapper.TransactionMapper;
import com.druvu.acc.gnucash.mapper.VendorMapper;
import com.druvu.acc.gnucash.writer.GnucashFileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;

/**
 * GnuCash XML implementation of AccStore.
 *
 * <p>Stores only the GncV2 root and computes all derived data on demand. This allows for future mutation support and
 * keeps a single source of truth.
 *
 * @author Deniss Larka <br>
 *     on 11 Jan 2026
 */
public final class GnucashAccStore implements WritableAccStore {

    private static final String CD_TYPE_ACCOUNT = "account";

    private static final String CD_TYPE_TRANSACTION = "transaction";

    private static final String CD_TYPE_COMMODITY = "commodity";

    private static final String CD_TYPE_PRICE = "price";

    private static final String CD_TYPE_CUSTOMER = "gnc:GncCustomer";

    private static final String CD_TYPE_VENDOR = "gnc:GncVendor";

    private static final String CD_TYPE_EMPLOYEE = "gnc:GncEmployee";

    private static final String CD_TYPE_TAX_TABLE = "gnc:GncTaxTable";

    private static final String CD_TYPE_BILL_TERM = "gnc:GncBillTerm";

    private static final String CD_TYPE_JOB = "gnc:GncJob";

    private static final String CD_TYPE_ORDER = "gnc:GncOrder";

    private static final String CD_TYPE_INVOICE = "gnc:GncInvoice";

    private static final String CD_TYPE_ENTRY = "gnc:GncEntry";

    private static final int PRICEDB_VERSION = 1;

    private final GncV2 root;

    public GnucashAccStore(@NonNull GncV2 root) {
        this.root = root;
    }

    // ========== AccStore Interface ==========

    @Override
    public String id() {
        return book().getBookId().getValue();
    }

    @Override
    public List<CommodityId> commodities() {
        return bookElements(GncV2.GncBook.GncCommodity.class)
                .map(c -> new CommodityId(c.getCmdtySpace(), c.getCmdtyId()))
                .toList();
    }

    @Override
    public List<Price> prices() {
        return bookElements(GncPricedb.class)
                .filter(pricedb -> pricedb.getPrice() != null)
                .flatMap(pricedb -> pricedb.getPrice().stream())
                .map(PriceMapper::map)
                .toList();
    }

    @Override
    public List<Customer> customers() {
        return bookElements(GncV2.GncBook.GncGncCustomer.class)
                .map(CustomerMapper::map)
                .toList();
    }

    @Override
    public Optional<Customer> customerById(String id) {
        return customerPeer(id).map(CustomerMapper::map);
    }

    @Override
    public List<Vendor> vendors() {
        return bookElements(GncV2.GncBook.GncGncVendor.class)
                .map(VendorMapper::map)
                .toList();
    }

    @Override
    public Optional<Vendor> vendorById(String id) {
        return vendorPeer(id).map(VendorMapper::map);
    }

    @Override
    public List<Employee> employees() {
        return bookElements(GncV2.GncBook.GncGncEmployee.class)
                .map(EmployeeMapper::map)
                .toList();
    }

    @Override
    public Optional<Employee> employeeById(String id) {
        return employeePeer(id).map(EmployeeMapper::map);
    }

    @Override
    public List<TaxTable> taxTables() {
        return bookElements(GncV2.GncBook.GncGncTaxTable.class)
                .map(TaxTableMapper::map)
                .toList();
    }

    @Override
    public Optional<TaxTable> taxTableById(String id) {
        return taxTablePeer(id).map(TaxTableMapper::map);
    }

    @Override
    public List<BillTerm> billTerms() {
        return bookElements(GncV2.GncBook.GncGncBillTerm.class)
                .map(BillTermMapper::map)
                .toList();
    }

    @Override
    public Optional<BillTerm> billTermById(String id) {
        return billTermPeer(id).map(BillTermMapper::map);
    }

    @Override
    public List<Job> jobs() {
        return bookElements(GncV2.GncBook.GncGncJob.class).map(JobMapper::map).toList();
    }

    @Override
    public Optional<Job> jobById(String id) {
        return jobPeer(id).map(JobMapper::map);
    }

    @Override
    public List<Order> orders() {
        return bookElements(GncV2.GncBook.GncGncOrder.class)
                .map(OrderMapper::map)
                .toList();
    }

    @Override
    public Optional<Order> orderById(String id) {
        return orderPeer(id).map(OrderMapper::map);
    }

    @Override
    public List<Invoice> invoices() {
        return bookElements(GncV2.GncBook.GncGncInvoice.class)
                .map(InvoiceMapper::map)
                .toList();
    }

    @Override
    public Optional<Invoice> invoiceById(String id) {
        return invoicePeer(id).map(InvoiceMapper::map);
    }

    @Override
    public List<Entry> entries() {
        return bookElements(GncV2.GncBook.GncGncEntry.class)
                .map(EntryMapper::map)
                .toList();
    }

    @Override
    public Optional<Entry> entryById(String id) {
        return entryPeer(id).map(EntryMapper::map);
    }

    @Override
    public List<Entry> entriesForInvoice(String invoiceId) {
        return bookElements(GncV2.GncBook.GncGncEntry.class)
                .map(EntryMapper::map)
                .filter(entry -> entry.invoiceLine()
                                .map(line -> line.invoiceId().equals(invoiceId))
                                .orElse(false)
                        || entry.billLine()
                                .map(line -> line.billId().equals(invoiceId))
                                .orElse(false))
                .toList();
    }

    @Override
    public Optional<Invoice> invoiceForTransaction(String transactionId) {
        return bookElements(GncV2.GncBook.GncGncInvoice.class)
                .filter(peer -> peer.getInvoicePosttxn() != null
                        && peer.getInvoicePosttxn().getValue().equals(transactionId))
                .findFirst()
                .map(InvoiceMapper::map);
    }

    @Override
    public Optional<Customer> customerForInvoice(String invoiceId) {
        return invoiceById(invoiceId).flatMap(invoice -> resolveCustomer(invoice.owner()));
    }

    @Override
    public Optional<Customer> customerForTransaction(String transactionId) {
        return invoiceForTransaction(transactionId).flatMap(invoice -> resolveCustomer(invoice.owner()));
    }

    /** Follows the job indirection to the customer behind an owner reference; empty for vendors and employees. */
    private Optional<Customer> resolveCustomer(Owner owner) {
        return switch (owner.type()) {
            case CUSTOMER -> customerById(owner.id());
            case JOB ->
                jobById(owner.id())
                        .map(Job::owner)
                        .filter(jobOwner -> jobOwner.type() == OwnerType.CUSTOMER)
                        .flatMap(jobOwner -> customerById(jobOwner.id()));
            case VENDOR, EMPLOYEE -> Optional.empty();
        };
    }

    @Override
    public List<Account> accounts() {
        return bookElements(GncAccount.class).map(AccountMapper::map).toList();
    }

    @Override
    public List<Account> rootAccounts() {
        return bookElements(GncAccount.class)
                .filter(account -> account.getActParent() == null)
                .map(AccountMapper::map)
                .toList();
    }

    @Override
    public Optional<Account> accountById(String id) {
        return bookElements(GncAccount.class)
                .filter(account -> account.getActId().getValue().equals(id))
                .findFirst()
                .map(AccountMapper::map);
    }

    @Override
    public Optional<Account> accountByName(String qualifiedName) {
        String[] path = qualifiedName.split(":");
        Optional<Account> current = Optional.empty();
        String currentParentId = null;

        for (String name : path) {
            current = accountByNameWithParent(name, currentParentId);
            if (current.isEmpty()) {
                return Optional.empty();
            }
            currentParentId = current.get().id();
        }

        return current;
    }

    @Override
    public List<String> fetchChildIds(String accountId) {
        return bookElements(GncAccount.class)
                .filter(account -> {
                    var parent = account.getActParent();
                    return parent != null && parent.getValue().equals(accountId);
                })
                .map(account -> account.getActId().getValue())
                .toList();
    }

    @Override
    public List<Transaction> transactions() {
        return bookElements(GncTransaction.class)
                .map(TransactionMapper::map)
                .sorted()
                .toList();
    }

    @Override
    public Optional<Transaction> transactionById(String id) {
        return bookElements(GncTransaction.class)
                .filter(transaction -> transaction.getTrnId().getValue().equals(id))
                .findFirst()
                .map(TransactionMapper::map);
    }

    @Override
    public List<Transaction> transactions(LocalDate from, LocalDate to) {
        return bookElements(GncTransaction.class)
                .map(TransactionMapper::map)
                .filter(mapped -> {
                    LocalDate date = mapped.datePosted();
                    return !date.isBefore(from) && !date.isAfter(to);
                })
                .sorted()
                .toList();
    }

    @Override
    public List<Transaction> transactionsForAccount(String accountId) {
        return transactions().stream()
                .filter(transaction -> transaction.splits().stream()
                        .anyMatch(split -> split.accountId().equals(accountId)))
                .toList();
    }

    @Override
    public List<Split> splitsForAccount(String accountId) {
        return transactions().stream()
                .flatMap(transaction -> transaction.splits().stream())
                .filter(split -> split.accountId().equals(accountId))
                .toList();
    }

    // ========== WritableAccStore Interface ==========

    @Override
    public String newId() {
        // GnuCash GUIDs are 32 lowercase hex characters - a UUID with the dashes stripped.
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public void addAccount(@NonNull Account account) {
        if (accountById(account.id()).isPresent()) {
            throw new IllegalArgumentException("Account already exists: " + account.id());
        }
        // A GnuCash book has exactly one ROOT and everything hangs off it. An account written with no
        // <act:parent> becomes a second root, which GnuCash does not consider a valid tree - refuse it
        // here rather than silently producing a book that will not open properly.
        if (account.parentId().isEmpty()) {
            if (account.type() != AccountType.ROOT) {
                throw new IllegalArgumentException(
                        "Account '" + account.name() + "' has no parent; only a ROOT account may be parentless");
            }
            if (!rootAccounts().isEmpty()) {
                throw new IllegalArgumentException("Book already has a root account; a second one would make the "
                        + "account tree ambiguous and GnuCash does not display it correctly: " + account.name());
            }
        }
        book().getBookElements().add(AccountMapper.toGnc(account, scuFor(account)));
        adjustCount(CD_TYPE_ACCOUNT, 1);
    }

    /**
     * The smallest currency unit to record on an account: the fraction its commodity is actually defined with in this
     * book, falling back to the ISO fraction for a currency the book has not defined yet.
     */
    private int scuFor(Account account) {
        Optional<CommodityId> commodityId = account.commodity();
        if (commodityId.isEmpty()) {
            return Commodity.CURRENCY_FRACTION;
        }
        CommodityId id = commodityId.get();
        return bookElements(GncV2.GncBook.GncCommodity.class)
                .map(CommodityMapper::map)
                .filter(commodity -> commodity.id().equals(id))
                .findFirst()
                .map(Commodity::fraction)
                // Not defined in this book: ISO can still answer for a real currency, but nothing can
                // answer for a security, and inventing a precision for it would be a guess about money.
                .orElseGet(() -> id.isCurrency() ? Commodity.currencyFraction(id.id()) : failUndefinedCommodity(id));
    }

    private static int failUndefinedCommodity(CommodityId id) {
        throw new IllegalArgumentException("Commodity " + id + " is not defined in this book, so the account's "
                + "precision is unknown - add it with addCommodity(...) first");
    }

    @Override
    public void addTransaction(@NonNull Transaction transaction) {
        if (transactionById(transaction.id()).isPresent()) {
            throw new IllegalArgumentException("Transaction already exists: " + transaction.id());
        }
        book().getBookElements().add(TransactionMapper.toGnc(transaction));
        adjustCount(CD_TYPE_TRANSACTION, 1);
    }

    /**
     * An update writes the entity's fields onto the element already in the book instead of replacing it. That is what
     * keeps an edit non-destructive: a GnuCash element carries a great deal this library does not model - unknown slot
     * keys, an account's own smallest-currency-unit, a split's memo and lot - and rebuilding it from the record would
     * discard every bit of it. The commodity's SCU is only recomputed when the caller actually changed the commodity,
     * since a precision chosen for the old one says nothing about the new one.
     */
    @Override
    public void updateAccount(@NonNull Account account) {
        GncAccount peer = bookElements(GncAccount.class)
                .filter(existing -> existing.getActId().getValue().equals(account.id()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No account with ID: " + account.id()));

        boolean commodityChanged =
                !sameCommodity(peer.getActCommodity(), account.commodity().orElse(null));

        AccountMapper.applyTo(peer, account);

        if (commodityChanged && account.commodity().isPresent()) {
            peer.setActCommodityScu(scuFor(account));
            peer.setActNonStandardScu(null);
        }
    }

    private static boolean sameCommodity(GncAccount.ActCommodity before, CommodityId after) {
        if (before == null || after == null) {
            return before == null && after == null;
        }
        return Objects.equals(before.getCmdtySpace(), after.namespace())
                && Objects.equals(before.getCmdtyId(), after.id());
    }

    @Override
    public void updateTransaction(@NonNull Transaction transaction) {
        GncTransaction peer = bookElements(GncTransaction.class)
                .filter(existing -> existing.getTrnId().getValue().equals(transaction.id()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No transaction with ID: " + transaction.id()));

        TransactionMapper.applyTo(peer, transaction);
    }

    @Override
    public void removeAccount(String accountId) {
        boolean removed = book().getBookElements()
                .removeIf(element -> element instanceof GncAccount account
                        && account.getActId().getValue().equals(accountId));
        if (!removed) {
            throw new IllegalArgumentException("No account with ID: " + accountId);
        }
        adjustCount(CD_TYPE_ACCOUNT, -1);
    }

    @Override
    public void removeTransaction(String transactionId) {
        boolean removed = book().getBookElements()
                .removeIf(element -> element instanceof GncTransaction transaction
                        && transaction.getTrnId().getValue().equals(transactionId));
        if (!removed) {
            throw new IllegalArgumentException("No transaction with ID: " + transactionId);
        }
        adjustCount(CD_TYPE_TRANSACTION, -1);
    }

    @Override
    public void addCommodity(@NonNull Commodity commodity) {
        CommodityId id = commodity.id();
        boolean exists = bookElements(GncV2.GncBook.GncCommodity.class)
                .anyMatch(
                        c -> id.namespace().equals(c.getCmdtySpace()) && id.id().equals(c.getCmdtyId()));
        if (exists) {
            throw new IllegalArgumentException("Commodity already exists: " + id);
        }
        book().getBookElements().add(CommodityMapper.toGnc(commodity));
        adjustCount(CD_TYPE_COMMODITY, 1);
    }

    @Override
    public void removeCommodity(CommodityId commodityId) {
        boolean removed = book().getBookElements()
                .removeIf(element -> element instanceof GncV2.GncBook.GncCommodity commodity
                        && commodityId.namespace().equals(commodity.getCmdtySpace())
                        && commodityId.id().equals(commodity.getCmdtyId()));
        if (!removed) {
            throw new IllegalArgumentException("No commodity: " + commodityId);
        }
        adjustCount(CD_TYPE_COMMODITY, -1);
    }

    @Override
    public void addPrice(@NonNull Price price) {
        GncPricedb pricedb = priceDb();
        boolean exists = pricedb.getPrice().stream()
                .anyMatch(p -> p.getPriceId().getValue().equals(price.id()));
        if (exists) {
            throw new IllegalArgumentException("Price already exists: " + price.id());
        }
        pricedb.getPrice().add(PriceMapper.toGnc(price));
        adjustCount(CD_TYPE_PRICE, 1);
    }

    @Override
    public void removePrice(String priceId) {
        GncPricedb pricedb = bookElements(GncPricedb.class).findFirst().orElse(null);
        boolean removed = pricedb != null
                && pricedb.getPrice().removeIf(p -> p.getPriceId().getValue().equals(priceId));
        if (!removed) {
            throw new IllegalArgumentException("No price with ID: " + priceId);
        }
        adjustCount(CD_TYPE_PRICE, -1);
    }

    // ========== Business parties ==========

    @Override
    public void addCustomer(@NonNull Customer customer) {
        if (customerPeer(customer.id()).isPresent()) {
            throw new IllegalArgumentException("Customer already exists: " + customer.id());
        }
        requireBillTerm(customer.termsId());
        requireTaxTable(customer.taxTable());
        book().getBookElements().add(CustomerMapper.toGnc(customer));
        adjustBillTermRefcount(customer.termsId(), +1);
        adjustTaxTableRefcount(taxTableIdOf(customer.taxTable()), +1);
        adjustCount(CD_TYPE_CUSTOMER, 1);
    }

    @Override
    public void updateCustomer(@NonNull Customer customer) {
        GncV2.GncBook.GncGncCustomer peer = customerPeer(customer.id())
                .orElseThrow(() -> new IllegalArgumentException("No customer with ID: " + customer.id()));
        requireBillTerm(customer.termsId());
        requireTaxTable(customer.taxTable());

        Optional<String> termsBefore =
                Optional.ofNullable(peer.getCustTerms()).map(GncV2.GncBook.GncGncCustomer.CustTerms::getValue);
        Optional<String> tableBefore =
                Optional.ofNullable(peer.getCustTaxtable()).map(GncV2.GncBook.GncGncCustomer.CustTaxtable::getValue);

        CustomerMapper.applyTo(peer, customer);

        Optional<String> termsAfter =
                Optional.ofNullable(peer.getCustTerms()).map(GncV2.GncBook.GncGncCustomer.CustTerms::getValue);
        Optional<String> tableAfter =
                Optional.ofNullable(peer.getCustTaxtable()).map(GncV2.GncBook.GncGncCustomer.CustTaxtable::getValue);
        adjustBillTermRefcountDiff(termsBefore, termsAfter);
        adjustTaxTableRefcountDiff(tableBefore, tableAfter);
    }

    @Override
    public void removeCustomer(@NonNull String customerId) {
        GncV2.GncBook.GncGncCustomer peer = customerPeer(customerId)
                .orElseThrow(() -> new IllegalArgumentException("No customer with ID: " + customerId));
        refuseWhileOwnerReferenced("Customer", customerId);
        book().getBookElements().remove(peer);
        adjustBillTermRefcount(
                Optional.ofNullable(peer.getCustTerms()).map(GncV2.GncBook.GncGncCustomer.CustTerms::getValue), -1);
        adjustTaxTableRefcount(
                Optional.ofNullable(peer.getCustTaxtable()).map(GncV2.GncBook.GncGncCustomer.CustTaxtable::getValue),
                -1);
        adjustCount(CD_TYPE_CUSTOMER, -1);
    }

    @Override
    public void addVendor(@NonNull Vendor vendor) {
        if (vendorPeer(vendor.id()).isPresent()) {
            throw new IllegalArgumentException("Vendor already exists: " + vendor.id());
        }
        requireBillTerm(vendor.termsId());
        requireTaxTable(vendor.taxTable());
        book().getBookElements().add(VendorMapper.toGnc(vendor));
        adjustBillTermRefcount(vendor.termsId(), +1);
        adjustTaxTableRefcount(taxTableIdOf(vendor.taxTable()), +1);
        adjustCount(CD_TYPE_VENDOR, 1);
    }

    @Override
    public void updateVendor(@NonNull Vendor vendor) {
        GncV2.GncBook.GncGncVendor peer = vendorPeer(vendor.id())
                .orElseThrow(() -> new IllegalArgumentException("No vendor with ID: " + vendor.id()));
        requireBillTerm(vendor.termsId());
        requireTaxTable(vendor.taxTable());

        Optional<String> termsBefore =
                Optional.ofNullable(peer.getVendorTerms()).map(GncV2.GncBook.GncGncVendor.VendorTerms::getValue);
        Optional<String> tableBefore =
                Optional.ofNullable(peer.getVendorTaxtable()).map(GncV2.GncBook.GncGncVendor.VendorTaxtable::getValue);

        VendorMapper.applyTo(peer, vendor);

        Optional<String> termsAfter =
                Optional.ofNullable(peer.getVendorTerms()).map(GncV2.GncBook.GncGncVendor.VendorTerms::getValue);
        Optional<String> tableAfter =
                Optional.ofNullable(peer.getVendorTaxtable()).map(GncV2.GncBook.GncGncVendor.VendorTaxtable::getValue);
        adjustBillTermRefcountDiff(termsBefore, termsAfter);
        adjustTaxTableRefcountDiff(tableBefore, tableAfter);
    }

    @Override
    public void removeVendor(@NonNull String vendorId) {
        GncV2.GncBook.GncGncVendor peer =
                vendorPeer(vendorId).orElseThrow(() -> new IllegalArgumentException("No vendor with ID: " + vendorId));
        refuseWhileOwnerReferenced("Vendor", vendorId);
        book().getBookElements().remove(peer);
        adjustBillTermRefcount(
                Optional.ofNullable(peer.getVendorTerms()).map(GncV2.GncBook.GncGncVendor.VendorTerms::getValue), -1);
        adjustTaxTableRefcount(
                Optional.ofNullable(peer.getVendorTaxtable()).map(GncV2.GncBook.GncGncVendor.VendorTaxtable::getValue),
                -1);
        adjustCount(CD_TYPE_VENDOR, -1);
    }

    @Override
    public void addEmployee(@NonNull Employee employee) {
        if (employeePeer(employee.id()).isPresent()) {
            throw new IllegalArgumentException("Employee already exists: " + employee.id());
        }
        book().getBookElements().add(EmployeeMapper.toGnc(employee));
        adjustCount(CD_TYPE_EMPLOYEE, 1);
    }

    @Override
    public void updateEmployee(@NonNull Employee employee) {
        GncV2.GncBook.GncGncEmployee peer = employeePeer(employee.id())
                .orElseThrow(() -> new IllegalArgumentException("No employee with ID: " + employee.id()));
        EmployeeMapper.applyTo(peer, employee);
    }

    @Override
    public void removeEmployee(@NonNull String employeeId) {
        refuseWhileOwnerReferenced("Employee", employeeId);
        boolean removed = book().getBookElements()
                .removeIf(element -> element instanceof GncV2.GncBook.GncGncEmployee employee
                        && employee.getEmployeeGuid().getValue().equals(employeeId));
        if (!removed) {
            throw new IllegalArgumentException("No employee with ID: " + employeeId);
        }
        adjustCount(CD_TYPE_EMPLOYEE, -1);
    }

    @Override
    public void addTaxTable(@NonNull TaxTable taxTable) {
        if (taxTablePeer(taxTable.id()).isPresent()) {
            throw new IllegalArgumentException("Tax table already exists: " + taxTable.id());
        }
        taxTable.entries().stream()
                .filter(entry -> accountById(entry.accountId()).isEmpty())
                .findFirst()
                .ifPresent(entry -> {
                    throw new IllegalArgumentException(
                            "Tax table entry posts to an account that is not in the book: " + entry.accountId());
                });
        book().getBookElements().add(TaxTableMapper.toGnc(taxTable));
        adjustCount(CD_TYPE_TAX_TABLE, 1);
    }

    @Override
    public void removeTaxTable(@NonNull String taxTableId) {
        GncV2.GncBook.GncGncTaxTable peer = taxTablePeer(taxTableId)
                .orElseThrow(() -> new IllegalArgumentException("No tax table with ID: " + taxTableId));
        List<String> holders = taxTableHolders(taxTableId);
        if (!holders.isEmpty()) {
            throw new IllegalStateException(
                    "Tax table " + taxTableId + " is still referenced by: " + String.join(", ", holders));
        }
        book().getBookElements().remove(peer);
        adjustCount(CD_TYPE_TAX_TABLE, -1);
    }

    @Override
    public void addBillTerm(@NonNull BillTerm term) {
        if (billTermPeer(term.id()).isPresent()) {
            throw new IllegalArgumentException("Billing term already exists: " + term.id());
        }
        book().getBookElements().add(BillTermMapper.toGnc(term));
        adjustCount(CD_TYPE_BILL_TERM, 1);
    }

    @Override
    public void removeBillTerm(@NonNull String termId) {
        GncV2.GncBook.GncGncBillTerm peer = billTermPeer(termId)
                .orElseThrow(() -> new IllegalArgumentException("No billing term with ID: " + termId));
        List<String> holders = billTermHolders(termId);
        if (!holders.isEmpty()) {
            throw new IllegalStateException(
                    "Billing term " + termId + " is still referenced by: " + String.join(", ", holders));
        }
        book().getBookElements().remove(peer);
        adjustCount(CD_TYPE_BILL_TERM, -1);
    }

    @Override
    public void addJob(@NonNull Job job) {
        if (jobPeer(job.id()).isPresent()) {
            throw new IllegalArgumentException("Job already exists: " + job.id());
        }
        requireJobOwner(job.owner());
        book().getBookElements().add(JobMapper.toGnc(job));
        adjustCount(CD_TYPE_JOB, 1);
    }

    @Override
    public void updateJob(@NonNull Job job) {
        GncV2.GncBook.GncGncJob peer =
                jobPeer(job.id()).orElseThrow(() -> new IllegalArgumentException("No job with ID: " + job.id()));
        requireJobOwner(job.owner());
        JobMapper.applyTo(peer, job);
    }

    @Override
    public void removeJob(@NonNull String jobId) {
        GncV2.GncBook.GncGncJob peer =
                jobPeer(jobId).orElseThrow(() -> new IllegalArgumentException("No job with ID: " + jobId));
        refuseWhileOwnerReferenced("Job", jobId);
        book().getBookElements().remove(peer);
        adjustCount(CD_TYPE_JOB, -1);
    }

    @Override
    public void addOrder(@NonNull Order order) {
        if (orderPeer(order.id()).isPresent()) {
            throw new IllegalArgumentException("Order already exists: " + order.id());
        }
        requireOwner(order.owner());
        book().getBookElements().add(OrderMapper.toGnc(order));
        adjustCount(CD_TYPE_ORDER, 1);
    }

    @Override
    public void updateOrder(@NonNull Order order) {
        GncV2.GncBook.GncGncOrder peer = orderPeer(order.id())
                .orElseThrow(() -> new IllegalArgumentException("No order with ID: " + order.id()));
        requireOwner(order.owner());
        OrderMapper.applyTo(peer, order);
    }

    @Override
    public void removeOrder(@NonNull String orderId) {
        GncV2.GncBook.GncGncOrder peer =
                orderPeer(orderId).orElseThrow(() -> new IllegalArgumentException("No order with ID: " + orderId));
        List<String> holders = bookElements(GncV2.GncBook.GncGncEntry.class)
                .filter(entry -> entry.getEntryOrder() != null
                        && orderId.equals(entry.getEntryOrder().getValue()))
                .map(entry -> "entry " + entry.getEntryGuid().getValue())
                .toList();
        if (!holders.isEmpty()) {
            throw new IllegalStateException(
                    "Order " + orderId + " is still referenced by: " + String.join(", ", holders));
        }
        book().getBookElements().remove(peer);
        adjustCount(CD_TYPE_ORDER, -1);
    }

    @Override
    public void addInvoice(@NonNull Invoice invoice) {
        if (invoicePeer(invoice.id()).isPresent()) {
            throw new IllegalArgumentException("Invoice already exists: " + invoice.id());
        }
        requireOwner(invoice.owner());
        invoice.billTo().ifPresent(this::requireOwner);
        requireBillTerm(invoice.termsId());
        book().getBookElements().add(InvoiceMapper.toGnc(invoice));
        adjustBillTermRefcount(invoice.termsId(), +1);
        adjustCount(CD_TYPE_INVOICE, 1);
    }

    @Override
    public void updateInvoice(@NonNull Invoice invoice) {
        GncV2.GncBook.GncGncInvoice peer = invoicePeer(invoice.id())
                .orElseThrow(() -> new IllegalArgumentException("No invoice with ID: " + invoice.id()));
        requireOwner(invoice.owner());
        invoice.billTo().ifPresent(this::requireOwner);
        requireBillTerm(invoice.termsId());

        Optional<String> termsBefore =
                Optional.ofNullable(peer.getInvoiceTerms()).map(GncV2.GncBook.GncGncInvoice.InvoiceTerms::getValue);
        InvoiceMapper.applyTo(peer, invoice);
        Optional<String> termsAfter =
                Optional.ofNullable(peer.getInvoiceTerms()).map(GncV2.GncBook.GncGncInvoice.InvoiceTerms::getValue);
        adjustBillTermRefcountDiff(termsBefore, termsAfter);
    }

    @Override
    public void removeInvoice(@NonNull String invoiceId) {
        GncV2.GncBook.GncGncInvoice peer = invoicePeer(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("No invoice with ID: " + invoiceId));
        if (peer.getInvoicePosted() != null) {
            // The posting's ledger transaction and lot are not this library's to unwind.
            throw new IllegalStateException(
                    "Invoice " + invoiceId + " is posted; unpost it in GnuCash before removing it");
        }
        List<String> lines = bookElements(GncV2.GncBook.GncGncEntry.class)
                .filter(entry -> entry.getEntryInvoice() != null
                                && invoiceId.equals(entry.getEntryInvoice().getValue())
                        || entry.getEntryBill() != null
                                && invoiceId.equals(entry.getEntryBill().getValue()))
                .map(entry -> "entry " + entry.getEntryGuid().getValue())
                .toList();
        if (!lines.isEmpty()) {
            throw new IllegalStateException(
                    "Invoice " + invoiceId + " is still referenced by: " + String.join(", ", lines));
        }
        book().getBookElements().remove(peer);
        adjustBillTermRefcount(
                Optional.ofNullable(peer.getInvoiceTerms()).map(GncV2.GncBook.GncGncInvoice.InvoiceTerms::getValue),
                -1);
        adjustCount(CD_TYPE_INVOICE, -1);
    }

    @Override
    public void addEntry(@NonNull Entry entry) {
        if (entryPeer(entry.id()).isPresent()) {
            throw new IllegalArgumentException("Entry already exists: " + entry.id());
        }
        requireEntryReferences(entry);
        book().getBookElements().add(EntryMapper.toGnc(entry));
        // Entry tax-table references deliberately do not touch stored refcounts: a real GnuCash book
        // carries an entry pointing at a frozen table whose refcount is 0.
        adjustCount(CD_TYPE_ENTRY, 1);
    }

    @Override
    public void updateEntry(@NonNull Entry entry) {
        GncV2.GncBook.GncGncEntry peer = entryPeer(entry.id())
                .orElseThrow(() -> new IllegalArgumentException("No entry with ID: " + entry.id()));
        requireEntryReferences(entry);
        EntryMapper.applyTo(peer, entry);
    }

    @Override
    public void removeEntry(@NonNull String entryId) {
        boolean removed = book().getBookElements()
                .removeIf(element -> element instanceof GncV2.GncBook.GncGncEntry entry
                        && entry.getEntryGuid().getValue().equals(entryId));
        if (!removed) {
            throw new IllegalArgumentException("No entry with ID: " + entryId);
        }
        adjustCount(CD_TYPE_ENTRY, -1);
    }

    // ---------- document helpers ----------

    private Optional<GncV2.GncBook.GncGncJob> jobPeer(String id) {
        return bookElements(GncV2.GncBook.GncGncJob.class)
                .filter(peer -> peer.getJobGuid().getValue().equals(id))
                .findFirst();
    }

    private Optional<GncV2.GncBook.GncGncOrder> orderPeer(String id) {
        return bookElements(GncV2.GncBook.GncGncOrder.class)
                .filter(peer -> peer.getOrderGuid().getValue().equals(id))
                .findFirst();
    }

    private Optional<GncV2.GncBook.GncGncInvoice> invoicePeer(String id) {
        return bookElements(GncV2.GncBook.GncGncInvoice.class)
                .filter(peer -> peer.getInvoiceGuid().getValue().equals(id))
                .findFirst();
    }

    private Optional<GncV2.GncBook.GncGncEntry> entryPeer(String id) {
        return bookElements(GncV2.GncBook.GncGncEntry.class)
                .filter(peer -> peer.getEntryGuid().getValue().equals(id))
                .findFirst();
    }

    /** An owner reference must point at a party that is actually in the book. */
    private void requireOwner(Owner owner) {
        boolean present =
                switch (owner.type()) {
                    case CUSTOMER -> customerPeer(owner.id()).isPresent();
                    case VENDOR -> vendorPeer(owner.id()).isPresent();
                    case EMPLOYEE -> employeePeer(owner.id()).isPresent();
                    case JOB -> jobPeer(owner.id()).isPresent();
                };
        if (!present) {
            throw new IllegalArgumentException(
                    "Owner " + owner.type() + " " + owner.id() + " is not in the book - add it first");
        }
    }

    /** GnuCash jobs belong to a customer or a vendor; a job cannot own a job. */
    private void requireJobOwner(Owner owner) {
        if (owner.type() != OwnerType.CUSTOMER && owner.type() != OwnerType.VENDOR) {
            throw new IllegalArgumentException("A job's owner must be a customer or a vendor, not " + owner.type());
        }
        requireOwner(owner);
    }

    private void requireEntryReferences(Entry entry) {
        entry.invoiceLine().ifPresent(line -> {
            requireInvoice(line.invoiceId());
            line.accountId().ifPresent(this::requireAccount);
            line.tax().taxTableId().ifPresent(this::requireTaxTableExists);
        });
        entry.billLine().ifPresent(line -> {
            requireInvoice(line.billId());
            line.accountId().ifPresent(this::requireAccount);
            line.tax().taxTableId().ifPresent(this::requireTaxTableExists);
            line.billTo().ifPresent(this::requireOwner);
        });
        entry.orderId().ifPresent(orderId -> {
            if (orderPeer(orderId).isEmpty()) {
                throw new IllegalArgumentException("Order is not in the book: " + orderId + " - add it first");
            }
        });
    }

    private void requireInvoice(String invoiceId) {
        if (invoicePeer(invoiceId).isEmpty()) {
            throw new IllegalArgumentException("Invoice is not in the book: " + invoiceId + " - add it first");
        }
    }

    private void requireAccount(String accountId) {
        if (accountById(accountId).isEmpty()) {
            throw new IllegalArgumentException("Account is not in the book: " + accountId + " - add it first");
        }
    }

    private void requireTaxTableExists(String taxTableId) {
        if (taxTablePeer(taxTableId).isEmpty()) {
            throw new IllegalArgumentException("Tax table is not in the book: " + taxTableId + " - add it first");
        }
    }

    /** Refuses removing a party while a job, order, document or chargeback still names it as owner. */
    private void refuseWhileOwnerReferenced(String kind, String ownerId) {
        List<String> holders = ownerHolders(ownerId);
        if (!holders.isEmpty()) {
            throw new IllegalStateException(
                    kind + " " + ownerId + " is still referenced by: " + String.join(", ", holders));
        }
    }

    /** Everything naming the given party as its owner, for the refusal message. */
    private List<String> ownerHolders(String ownerId) {
        List<String> holders = new ArrayList<>();
        bookElements(GncV2.GncBook.GncGncJob.class)
                .filter(peer -> ownerId.equals(peer.getJobOwner().getOwnerId().getValue()))
                .forEach(peer -> holders.add("job '" + peer.getJobName() + "'"));
        bookElements(GncV2.GncBook.GncGncOrder.class)
                .filter(peer -> ownerId.equals(peer.getOrderOwner().getOwnerId().getValue()))
                .forEach(peer -> holders.add("order " + peer.getOrderId()));
        bookElements(GncV2.GncBook.GncGncInvoice.class)
                .filter(peer -> ownerId.equals(
                                peer.getInvoiceOwner().getOwnerId().getValue())
                        || peer.getInvoiceBillto() != null
                                && ownerId.equals(
                                        peer.getInvoiceBillto().getOwnerId().getValue()))
                .forEach(peer -> holders.add("invoice " + peer.getInvoiceId()));
        bookElements(GncV2.GncBook.GncGncEntry.class)
                .filter(peer -> peer.getEntryBillto() != null
                        && ownerId.equals(peer.getEntryBillto().getOwnerId().getValue()))
                .forEach(peer -> holders.add("entry " + peer.getEntryGuid().getValue()));
        return holders;
    }

    // ---------- business helpers ----------

    private Optional<GncV2.GncBook.GncGncCustomer> customerPeer(String id) {
        return bookElements(GncV2.GncBook.GncGncCustomer.class)
                .filter(peer -> peer.getCustGuid().getValue().equals(id))
                .findFirst();
    }

    private Optional<GncV2.GncBook.GncGncVendor> vendorPeer(String id) {
        return bookElements(GncV2.GncBook.GncGncVendor.class)
                .filter(peer -> peer.getVendorGuid().getValue().equals(id))
                .findFirst();
    }

    private Optional<GncV2.GncBook.GncGncEmployee> employeePeer(String id) {
        return bookElements(GncV2.GncBook.GncGncEmployee.class)
                .filter(peer -> peer.getEmployeeGuid().getValue().equals(id))
                .findFirst();
    }

    private Optional<GncV2.GncBook.GncGncTaxTable> taxTablePeer(String id) {
        return bookElements(GncV2.GncBook.GncGncTaxTable.class)
                .filter(peer -> peer.getTaxtableGuid().getValue().equals(id))
                .findFirst();
    }

    private Optional<GncV2.GncBook.GncGncBillTerm> billTermPeer(String id) {
        return bookElements(GncV2.GncBook.GncGncBillTerm.class)
                .filter(peer -> peer.getBilltermGuid().getValue().equals(id))
                .findFirst();
    }

    /** The table a policy references, if it references one. */
    private static Optional<String> taxTableIdOf(TaxTablePolicy policy) {
        return policy instanceof TaxTablePolicy.Table(String taxTableId) ? Optional.of(taxTableId) : Optional.empty();
    }

    private void requireBillTerm(Optional<String> termsId) {
        termsId.filter(id -> billTermPeer(id).isEmpty()).ifPresent(id -> {
            throw new IllegalArgumentException("Billing term is not in the book: " + id + " - add it first");
        });
    }

    private void requireTaxTable(TaxTablePolicy policy) {
        taxTableIdOf(policy).filter(id -> taxTablePeer(id).isEmpty()).ifPresent(id -> {
            throw new IllegalArgumentException("Tax table is not in the book: " + id + " - add it first");
        });
    }

    /**
     * GnuCash counts how many objects reference a term or table and stores that count in the file; the store keeps it
     * in step so GnuCash's own can-this-be-deleted logic stays truthful.
     */
    private void adjustBillTermRefcount(Optional<String> termId, int delta) {
        termId.flatMap(this::billTermPeer)
                .ifPresent(peer -> peer.setBilltermRefcount(peer.getBilltermRefcount() + delta));
    }

    private void adjustTaxTableRefcount(Optional<String> tableId, int delta) {
        tableId.flatMap(this::taxTablePeer)
                .ifPresent(peer -> peer.setTaxtableRefcount(peer.getTaxtableRefcount() + delta));
    }

    private void adjustBillTermRefcountDiff(Optional<String> before, Optional<String> after) {
        if (!before.equals(after)) {
            adjustBillTermRefcount(before, -1);
            adjustBillTermRefcount(after, +1);
        }
    }

    private void adjustTaxTableRefcountDiff(Optional<String> before, Optional<String> after) {
        if (!before.equals(after)) {
            adjustTaxTableRefcount(before, -1);
            adjustTaxTableRefcount(after, +1);
        }
    }

    /** Everything still referencing a tax table, for the refusal message. */
    private List<String> taxTableHolders(String taxTableId) {
        List<String> holders = new ArrayList<>();
        bookElements(GncV2.GncBook.GncGncCustomer.class)
                .filter(peer -> peer.getCustTaxtable() != null
                        && taxTableId.equals(peer.getCustTaxtable().getValue()))
                .forEach(peer -> holders.add("customer '" + peer.getCustName() + "'"));
        bookElements(GncV2.GncBook.GncGncVendor.class)
                .filter(peer -> peer.getVendorTaxtable() != null
                        && taxTableId.equals(peer.getVendorTaxtable().getValue()))
                .forEach(peer -> holders.add("vendor '" + peer.getVendorName() + "'"));
        bookElements(GncV2.GncBook.GncGncEntry.class)
                .filter(peer -> peer.getEntryITaxtable() != null
                                && taxTableId.equals(peer.getEntryITaxtable().getValue())
                        || peer.getEntryBTaxtable() != null
                                && taxTableId.equals(peer.getEntryBTaxtable().getValue()))
                .forEach(peer -> holders.add("entry " + peer.getEntryGuid().getValue()));
        bookElements(GncV2.GncBook.GncGncTaxTable.class)
                .filter(peer -> peer.getTaxtableParent() != null
                                && taxTableId.equals(peer.getTaxtableParent().getValue())
                        || peer.getTaxtableChild() != null
                                && taxTableId.equals(peer.getTaxtableChild().getValue()))
                .forEach(peer -> holders.add("tax table version '" + peer.getTaxtableName() + "'"));
        return holders;
    }

    /** Everything still referencing a billing term, for the refusal message. */
    private List<String> billTermHolders(String termId) {
        List<String> holders = new ArrayList<>();
        bookElements(GncV2.GncBook.GncGncCustomer.class)
                .filter(peer -> peer.getCustTerms() != null
                        && termId.equals(peer.getCustTerms().getValue()))
                .forEach(peer -> holders.add("customer '" + peer.getCustName() + "'"));
        bookElements(GncV2.GncBook.GncGncVendor.class)
                .filter(peer -> peer.getVendorTerms() != null
                        && termId.equals(peer.getVendorTerms().getValue()))
                .forEach(peer -> holders.add("vendor '" + peer.getVendorName() + "'"));
        bookElements(GncV2.GncBook.GncGncInvoice.class)
                .filter(peer -> peer.getInvoiceTerms() != null
                        && termId.equals(peer.getInvoiceTerms().getValue()))
                .forEach(peer -> holders.add("invoice " + peer.getInvoiceId()));
        bookElements(GncV2.GncBook.GncGncBillTerm.class)
                .filter(peer -> peer.getBilltermParent() != null
                                && termId.equals(peer.getBilltermParent().getValue())
                        || peer.getBilltermChild().stream().anyMatch(child -> termId.equals(child.getValue())))
                .forEach(peer -> holders.add("billing term version '" + peer.getBilltermName() + "'"));
        return holders;
    }

    @Override
    public void save(Path path) throws IOException {
        List<String> problems = validate();
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Refusing to write a structurally invalid book:" + System.lineSeparator()
                    + "  - " + String.join(System.lineSeparator() + "  - ", problems));
        }
        new GnucashFileWriter().write(root, path);
    }

    @Override
    public List<String> validate() {
        final List<Account> accounts = accounts();
        final List<String> problems = new ArrayList<>();
        if (accounts.isEmpty()) {
            return List.of();
        }

        final Map<String, Account> byId = new HashMap<>();
        accounts.forEach(account -> byId.put(account.id(), account));

        final List<Account> roots = accounts.stream()
                .filter(account -> account.parentId().isEmpty())
                .toList();
        if (roots.isEmpty()) {
            problems.add("No root account: every account declares a parent, so the tree has no top");
        } else if (roots.size() > 1) {
            // The GnuCash API tolerates this; its UI does not render such a book correctly.
            problems.add("Several root accounts, only one is allowed: "
                    + roots.stream().map(Account::name).collect(Collectors.joining(", ")));
        }

        // A commodity the book never defines is a dangling reference, the same class of problem as a
        // missing parent: nothing in the book can say what the figures are denominated in.
        final Set<CommodityId> defined = Set.copyOf(commodities());

        for (Account account : accounts) {
            if (account.type() != AccountType.ROOT && account.parentId().isEmpty()) {
                problems.add("Account '" + account.name() + "' has no parent but is not a ROOT account");
            }
            account.parentId()
                    .filter(parentId -> !byId.containsKey(parentId))
                    .ifPresent(parentId -> problems.add("Account '" + account.name()
                            + "' points at a parent that is not in the book: " + parentId));
            account.commodity()
                    .filter(commodity -> !defined.contains(commodity))
                    .ifPresent(commodity -> problems.add("Account '" + account.name()
                            + "' is denominated in a commodity the book does not define: " + commodity));
            findCycle(account, byId).ifPresent(problems::add);
        }

        for (Transaction transaction : transactions()) {
            if (!defined.contains(transaction.currency())) {
                problems.add("Transaction '" + transaction.description()
                        + "' is denominated in a commodity the book does not define: " + transaction.currency());
            }
            for (Split split : transaction.splits()) {
                Account account = byId.get(split.accountId());
                if (account == null) {
                    problems.add("Transaction '" + transaction.description()
                            + "' has a split on an account that is not in the book: " + split.accountId());
                } else if (account.type() == AccountType.ROOT) {
                    // The root is structural - it holds the tree, never money.
                    problems.add("Transaction '" + transaction.description()
                            + "' posts a split to the ROOT account; post to a real account instead");
                }
            }
        }

        for (Customer customer : customers()) {
            if (!defined.contains(customer.currency())) {
                problems.add("Customer '" + customer.name()
                        + "' is denominated in a commodity the book does not define: " + customer.currency());
            }
        }
        for (Vendor vendor : vendors()) {
            if (!defined.contains(vendor.currency())) {
                problems.add("Vendor '" + vendor.name() + "' is denominated in a commodity the book does not define: "
                        + vendor.currency());
            }
        }
        for (Employee employee : employees()) {
            if (!defined.contains(employee.currency())) {
                problems.add("Employee '" + employee.username()
                        + "' is denominated in a commodity the book does not define: " + employee.currency());
            }
        }
        for (Invoice invoice : invoices()) {
            if (!defined.contains(invoice.currency())) {
                problems.add("Invoice " + invoice.number() + " is denominated in a commodity the book does not define: "
                        + invoice.currency());
            }
            invoice.posting().ifPresent(posting -> {
                posting.transactionId()
                        .filter(transactionId -> transactionById(transactionId).isEmpty())
                        .ifPresent(transactionId -> problems.add("Invoice " + invoice.number()
                                + " claims a posting transaction that is not in the book: " + transactionId));
                posting.accountId()
                        .filter(accountId -> accountById(accountId).isEmpty())
                        .ifPresent(accountId -> problems.add("Invoice " + invoice.number()
                                + " claims a posting account that is not in the book: " + accountId));
            });
        }

        return List.copyOf(problems);
    }

    /** Walks an account's ancestry; a repeat visit means the parent chain loops back on itself. */
    private Optional<String> findCycle(Account start, Map<String, Account> byId) {
        final Set<String> seen = new HashSet<>();
        Account current = start;
        while (current != null && seen.add(current.id())) {
            current = current.parentId().map(byId::get).orElse(null);
        }
        return current == null
                ? Optional.empty()
                : Optional.of("Account '" + start.name() + "' sits in a parent cycle through '" + current.name() + "'");
    }

    // ========== Helper Methods ==========

    private GncV2.GncBook book() {
        return root.getGncBook();
    }

    /** Returns the book's price database, creating and registering an empty one if absent. */
    private GncPricedb priceDb() {
        return bookElements(GncPricedb.class).findFirst().orElseGet(() -> {
            GncPricedb pricedb = new GncPricedb();
            pricedb.setVersion(PRICEDB_VERSION);
            book().getBookElements().add(pricedb);
            return pricedb;
        });
    }

    /**
     * Adjusts the {@code gnc:count-data} entry for the given type, keeping the file's declared counts consistent with
     * its contents. Creates the entry if absent.
     */
    private void adjustCount(String cdType, int delta) {
        for (GncCountData count : book().getGncCountData()) {
            if (cdType.equals(count.getCdType())) {
                count.setValue(count.getValue() + delta);
                return;
            }
        }
        if (delta > 0) {
            GncCountData count = new GncCountData();
            count.setCdType(cdType);
            count.setValue(delta);
            book().getGncCountData().add(count);
        }
    }

    private <T> Stream<T> bookElements(Class<T> type) {
        return book().getBookElements().stream().filter(type::isInstance).map(type::cast);
    }

    private Optional<Account> accountByNameWithParent(String accountName, String parentId) {
        Predicate<GncAccount> predicate = parentId == null
                ? account -> account.getActParent() == null
                : account -> account.getActParent() != null
                        && parentId.equals(account.getActParent().getValue());
        final List<Account> list = bookElements(GncAccount.class)
                .filter(predicate)
                .filter(account -> accountName.equals(account.getActName()))
                .map(AccountMapper::map)
                .toList();
        if (list.size() > 1) {
            throw new IllegalStateException("Multiple accounts found with name: " + accountName);
        }
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public String toString() {
        return String.format(
                "GnucashAccStore[accounts=%d, transactions=%d]",
                accounts().size(), transactions().size());
    }
}
