///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//DEPS com.druvu:druvu-acc-gnucash-xml:2.2.1
//DEPS com.druvu:druvu-lib-excel:1.0.0

// What each month cost, category by category, with totals: writes monthly-expenses.xlsx.
// Run: jbang MonthlyExpensesExcel.java your-book.gnucash

import static com.druvu.excel.Style.MONEY;

import com.druvu.acc.api.AccStore;
import com.druvu.acc.api.entity.Account;
import com.druvu.acc.api.entity.AccountType;
import com.druvu.acc.api.entity.CommodityId;
import com.druvu.excel.Excel;
import com.druvu.excel.Style;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public class MonthlyExpensesExcel {

    /** One line of the report: a category, or the totals at the bottom. */
    record Line(String category, Map<YearMonth, BigDecimal> byMonth, boolean totals) {

        BigDecimal in(YearMonth month) {
            return byMonth.getOrDefault(month, BigDecimal.ZERO);
        }

        BigDecimal sum() {
            return byMonth.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: jbang MonthlyExpensesExcel.java your-book.gnucash");
            System.exit(1);
        }
        var store = AccStore.load(Path.of(args[0]));   // read-only: nothing in the book can change

        var months = new TreeSet<YearMonth>();
        var lines = new ArrayList<Line>();
        var totals = new TreeMap<YearMonth, BigDecimal>();
        var currencies = new HashSet<CommodityId>();

        // No placeholder filter: a heading can still hold entries of its own, and they count.
        for (Account account : store.accounts()) {
            if (account.type() != AccountType.EXPENSE) {
                continue;
            }
            var byMonth = new TreeMap<YearMonth, BigDecimal>();
            for (var tx : store.transactionsForAccount(account.id())) {
                for (var split : tx.splits()) {
                    if (split.accountId().equals(account.id())) {
                        var month = YearMonth.from(tx.datePosted());
                        months.add(month);
                        // quantity() is in the account's own currency; value() would be in the transaction's
                        byMonth.merge(month, split.quantity(), BigDecimal::add);   // an expense is positive
                        totals.merge(month, split.quantity(), BigDecimal::add);
                    }
                }
            }
            if (!byMonth.isEmpty()) {   // an account without entries would only add a row of zeros
                lines.add(new Line(account.name(), byMonth, false));
                account.commodity().ifPresent(currencies::add);
            }
        }

        if (lines.isEmpty()) {
            System.out.println("No entries on any EXPENSE account in " + args[0] + " - nothing written.");
            return;
        }
        if (currencies.size() > 1) {   // one total across several currencies would be a wrong number
            System.err.println("The expense accounts use " + currencies + " - one total across currencies"
                    + " would be wrong, so nothing was written.");
            System.exit(1);
        }
        lines.add(new Line("Total", totals, true));

        var header = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);
        var sheet = Excel.sheet("Monthly expenses", lines)
                .column("Category", line -> line.totals() ? Style.NONE.withBold() : Style.NONE, Line::category);
        for (YearMonth month : months) {
            sheet = sheet.column(month.format(header), MonthlyExpensesExcel::money, line -> line.in(month));
        }
        sheet.column("Total", line -> MONEY.withBold(), Line::sum)
                .save(Path.of("monthly-expenses.xlsx"));

        System.out.println("monthly-expenses.xlsx written - open it in Excel.");
    }

    static Style money(Line line) {
        return line.totals() ? MONEY.withBold() : MONEY;
    }
}
