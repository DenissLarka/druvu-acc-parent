///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//DEPS com.druvu:druvu-acc-gnucash-xml:2.2.1
//DEPS com.druvu:druvu-lib-excel:1.0.0

// Every entry of one account, with a bold total: writes register.xlsx.
// Run: jbang AccountRegisterExcel.java your-book.gnucash "Account name"

import com.druvu.acc.api.AccStore;
import com.druvu.acc.api.entity.Account;
import com.druvu.excel.Excel;
import com.druvu.excel.Style;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;

public class AccountRegisterExcel {

    /** One line of the sheet: an entry of the account, or the total at the bottom. */
    record Line(LocalDate date, String description, BigDecimal amount, boolean total) {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: jbang AccountRegisterExcel.java your-book.gnucash \"Account name\"");
            System.exit(1);
        }
        var store = AccStore.load(Path.of(args[0]));   // read-only: nothing in the book can change
        String wanted = args[1];

        Account account = store.accounts().stream()
                .filter(candidate -> candidate.name().equals(wanted))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No account named " + wanted));

        var lines = new ArrayList<Line>();
        BigDecimal sum = BigDecimal.ZERO;
        for (var tx : store.transactionsForAccount(account.id())) {
            for (var split : tx.splits()) {
                if (split.accountId().equals(account.id())) {
                    // quantity() is in the account's own currency; value() would be in the transaction's
                    lines.add(new Line(tx.datePosted(), tx.description(), split.quantity(), false));
                    sum = sum.add(split.quantity());
                }
            }
        }
        lines.add(new Line(null, "Total", sum, true));   // no date on the total line: an empty cell

        Excel.sheet(account.name(), lines)
                .column("Date", Line::date)
                .column("Description", line -> line.total() ? Style.NONE.withBold() : Style.NONE, Line::description)
                .column("Amount", line -> line.total() ? Style.MONEY.withBold() : Style.MONEY, Line::amount)
                .save(Path.of("register.xlsx"));

        System.out.println("register.xlsx written - open it in Excel.");
    }
}
