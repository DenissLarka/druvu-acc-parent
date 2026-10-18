///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//DEPS com.druvu:druvu-acc-gnucash-xml:2.2.1
//DEPS com.druvu:druvu-lib-excel:1.0.0

// Every transaction with the customer behind it, customers' money highlighted: writes whose-money.xlsx.
// Run: jbang WhoseMoneyExcel.java your-book.gnucash

import static com.druvu.excel.Style.MONEY;

import com.druvu.acc.api.AccStore;
import com.druvu.acc.api.entity.Customer;
import com.druvu.acc.api.entity.Split;
import com.druvu.acc.api.entity.Transaction;
import com.druvu.excel.Excel;
import com.druvu.excel.Fill;
import java.math.BigDecimal;
import java.nio.file.Path;

public class WhoseMoneyExcel {

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: jbang WhoseMoneyExcel.java your-book.gnucash");
            System.exit(1);
        }
        var store = AccStore.load(Path.of(args[0]));   // read-only: nothing in the book can change

        Excel.sheet("Whose money", store.transactions())
                .column("Date", Transaction::datePosted)
                .column("Description", Transaction::description)
                .column("Customer", tx -> store.customerForTransaction(tx.id()).map(Customer::name))
                .column("Amount", tx -> isCustomers(store, tx) ? MONEY.withFill(Fill.LAVENDER) : MONEY, WhoseMoneyExcel::amount)
                .save(Path.of("whose-money.xlsx"));

        System.out.println("whose-money.xlsx written - open it in Excel.");
    }

    static boolean isCustomers(AccStore store, Transaction tx) {
        return store.customerForTransaction(tx.id()).isPresent();
    }

    /** What moved: the debit side of the transaction. */
    static BigDecimal amount(Transaction tx) {
        return tx.splits().stream()
                .map(Split::value)
                .filter(value -> value.signum() > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
