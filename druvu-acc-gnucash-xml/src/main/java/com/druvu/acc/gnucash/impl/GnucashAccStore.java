package com.druvu.acc.gnucash.impl;

import com.druvu.acc.api.WritableAccStore;
import com.druvu.acc.api.entity.Account;
import com.druvu.acc.api.entity.AccountType;
import com.druvu.acc.api.entity.Commodity;
import com.druvu.acc.api.entity.CommodityId;
import com.druvu.acc.api.entity.Price;
import com.druvu.acc.api.entity.Split;
import com.druvu.acc.api.entity.Transaction;
import com.druvu.acc.gnucash.generated.GncAccount;
import com.druvu.acc.gnucash.generated.GncCountData;
import com.druvu.acc.gnucash.generated.GncPricedb;
import com.druvu.acc.gnucash.generated.GncTransaction;
import com.druvu.acc.gnucash.generated.GncV2;
import com.druvu.acc.gnucash.mapper.AccountMapper;
import com.druvu.acc.gnucash.mapper.CommodityMapper;
import com.druvu.acc.gnucash.mapper.PriceMapper;
import com.druvu.acc.gnucash.mapper.TransactionMapper;
import com.druvu.acc.gnucash.writer.GnucashFileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

        for (Account account : accounts) {
            if (account.type() != AccountType.ROOT && account.parentId().isEmpty()) {
                problems.add("Account '" + account.name() + "' has no parent but is not a ROOT account");
            }
            account.parentId()
                    .filter(parentId -> !byId.containsKey(parentId))
                    .ifPresent(parentId -> problems.add("Account '" + account.name()
                            + "' points at a parent that is not in the book: " + parentId));
            findCycle(account, byId).ifPresent(problems::add);
        }

        for (Transaction transaction : transactions()) {
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
