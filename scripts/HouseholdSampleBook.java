///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//DEPS com.druvu:druvu-acc-gnucash-xml:2.2.1

// Writes household-2026.gnucash: a made-up household, January to June 2026, eight expense categories.
// A sample book to try the other scripts on. Run: jbang HouseholdSampleBook.java

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

public class HouseholdSampleBook {

    private final CommodityId currency = CommodityId.CHF;
    private final WritableAccStore store = AccStore.newBook(currency);
    private final String rootId = store.rootAccounts().getFirst().id();

    public static void main(String[] args) throws Exception {
        new HouseholdSampleBook().run();
    }

    private void run() throws Exception {
        var bank = account("Bank", AccountType.BANK, rootId, false);
        var salary = account("Salary", AccountType.INCOME, rootId, false);
        var expenses = account("Expenses", AccountType.EXPENSE, rootId, true);

        var rent = account("Rent", AccountType.EXPENSE, expenses, false);
        var groceries = account("Groceries", AccountType.EXPENSE, expenses, false);
        var electricity = account("Electricity", AccountType.EXPENSE, expenses, false);
        var insurance = account("Insurance", AccountType.EXPENSE, expenses, false);
        var transport = account("Transport", AccountType.EXPENSE, expenses, false);
        var phone = account("Phone & Internet", AccountType.EXPENSE, expenses, false);
        var health = account("Health", AccountType.EXPENSE, expenses, false);
        var leisure = account("Leisure", AccountType.EXPENSE, expenses, false);

        // Four shopping trips a month; the amounts are made up, and fixed so the book is the same every time.
        String[][] shopping = {
            {"152.40", "138.95", "171.20", "144.60"},
            {"149.85", "162.30", "133.75", "158.10"},
            {"166.05", "141.90", "155.45", "172.80"},
            {"139.70", "187.35", "150.25", "146.15"},
            {"158.90", "143.55", "169.40", "151.05"},
            {"147.30", "176.65", "140.80", "163.95"},
        };
        String[] healthBills = {"0", "184.50", "0", "62.00", "0", "240.75"};
        String[] outings = {"96.00", "148.50", "72.30", "215.00", "88.40", "310.20"};

        for (int month = 1; month <= 6; month++) {
            String name = LocalDate.of(2026, month, 1).getMonth().getDisplayName(
                    java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH);

            move(salary, bank, "Salary " + name, "6200.00", month, 25);
            move(bank, rent, "Rent " + name, "1800.00", month, 1);
            move(bank, insurance, "Health insurance " + name, "385.50", month, 3);
            move(bank, phone, "Phone and internet " + name, "79.90", month, 5);
            move(bank, transport, "Monthly pass " + name, "89.00", month, 2);
            for (int week = 0; week < 4; week++) {
                move(bank, groceries, "Groceries", shopping[month - 1][week], month, 4 + week * 7);
            }
            if (month % 2 == 0) {
                move(bank, electricity, "Electricity, two months", month == 2 ? "214.60" : month == 4 ? "176.30" : "131.85", month, 12);
            }
            if (!healthBills[month - 1].equals("0")) {
                move(bank, health, month == 4 ? "Pharmacy" : "Dentist", healthBills[month - 1], month, 18);
            }
            move(bank, leisure, month == 6 ? "Weekend away" : "Eating out, cinema", outings[month - 1], month, 20);
        }
        move(bank, transport, "Train to Bern", "54.00", 3, 14);
        move(bank, transport, "Car sharing", "67.80", 5, 23);

        store.save(Path.of("household-2026.gnucash"));

        var service = AccountService.create(store);
        System.out.println("household-2026.gnucash written.");
        System.out.println("Expenses, January to June: " + service.totalAmount(expenses));
        System.out.println("Bank balance:              " + service.balance(bank));
    }

    private String account(String name, AccountType type, String parentId, boolean placeholder) {
        var id = store.newId();
        store.addAccount(Account.of(id, name, type)
                .withCommodity(currency)
                .withParent(parentId)
                .withPlaceholder(placeholder));
        return id;
    }

    /** One money movement: it leaves {@code from} and reaches {@code to}. */
    private void move(String from, String to, String what, String amount, int month, int day) {
        var txId = store.newId();
        var date = LocalDate.of(2026, month, day);
        var value = new BigDecimal(amount);
        store.addTransaction(Transaction.of(txId, currency, date, what, List.of(
                Split.of(store.newId(), txId, from, date, value.negate()),
                Split.of(store.newId(), txId, to, date, value))));
    }
}
