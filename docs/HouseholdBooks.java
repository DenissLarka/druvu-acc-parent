///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//DEPS com.druvu:druvu-acc-gnucash-xml:2.2.0

// The house owner's books - see household-books.md. Run: jbang HouseholdBooks.java

import com.druvu.acc.api.AccStore;
import com.druvu.acc.api.WritableAccStore;
import com.druvu.acc.api.entity.Account;
import com.druvu.acc.api.entity.AccountType;
import com.druvu.acc.api.entity.CommodityId;
import com.druvu.acc.api.entity.Split;
import com.druvu.acc.api.entity.Transaction;
import com.druvu.acc.api.service.AccountService;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

public class HouseholdBooks {

    private final WritableAccStore store = AccStore.newBook(CommodityId.CHF);
    private final CommodityId chf = CommodityId.CHF;

    public static void main(String[] args) throws Exception {
        new HouseholdBooks().run();
    }

    private void run() throws Exception {
        var rootId = store.rootAccounts().getFirst().id();

        var bankId = store.newId();
        store.addAccount(
          Account.of(bankId, "Bank", AccountType.BANK)
            .withCommodity(chf)
            .withParent(rootId)
        );

        var salaryId = store.newId();
        store.addAccount(
          Account.of(salaryId, "Salary", AccountType.INCOME)
            .withCommodity(chf)
            .withParent(rootId)
        );

        var expensesId = store.newId();
        store.addAccount(
          Account.of(expensesId, "Household", AccountType.EXPENSE)
              .withCommodity(chf)
              .withParent(rootId)
              .withPlaceholder(true)
        );

        var rentId = store.newId();
        store.addAccount(
          Account.of(rentId, "Rent", AccountType.EXPENSE)
              .withCommodity(chf)
              .withParent(expensesId)
        );

        var groceriesId = store.newId();
        store.addAccount(
          Account.of(groceriesId, "Groceries", AccountType.EXPENSE)
                .withCommodity(chf)
                .withParent(expensesId)
        );

        var electricityId = store.newId();
        store.addAccount(
          Account.of(electricityId, "Electricity", AccountType.EXPENSE)
                .withCommodity(chf)
                .withParent(expensesId)
        );

        move(salaryId, bankId, "Salary August", "5200.00", 25);
        move(bankId, rentId, "Rent September", "1800.00", 28);
        move(bankId, groceriesId, "Weekly groceries", "160.45", 2);
        move(bankId, groceriesId, "Weekly groceries", "142.80", 9);
        move(bankId, electricityId, "Electricity July", "87.30", 12);

        store.save(Path.of("household-august.gnucash"));

        var service = AccountService.create(store);
        IO.println("Household spending: " + service.totalAmount(expensesId));
        IO.println("  of which rent:    " + service.balance(rentId));
        IO.println("  groceries:        " + service.balance(groceriesId));
        IO.println("  electricity:      " + service.balance(electricityId));
        IO.println("Bank balance:       " + service.balance(bankId));
    }

    /** One money movement: it leaves {@code from} and reaches {@code to}. */
    private void move(String from, String to, String what, String amount, int day) {
        var txId = store.newId();
        var date = LocalDate.of(2026, 8, day);
        var value = new BigDecimal(amount);
        store.addTransaction(
          Transaction.of(txId, chf, date, what, List.of(
                Split.of(store.newId(), txId, from, date, value.negate()),
                Split.of(store.newId(), txId, to, date, value)))
        );
    }
}
