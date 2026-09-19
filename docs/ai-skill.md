# Ask your AI: a brief for assistants

**You do not have to program.** Give your AI assistant the address of this page, then say in plain words what you
want from your GnuCash book:

> Use https://github.com/DenissLarka/druvu-acc-parent/blob/main/docs/ai-skill.md and write me a jbang script that
> opens a GnuCash file given as argument and prints what I spent in each category.

If your assistant cannot open web pages, copy this whole page into the chat instead — it works either way.

Everything below is written for the assistant.

---

## What you are writing

One complete Java file that runs with [JBang](https://www.jbang.dev): no project, no build file, nothing to install
beyond JBang itself. Both libraries are on **Maven Central** — no other repository, no credentials, no other
dependency.

The file starts with exactly these lines:

```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//DEPS com.druvu:druvu-acc-gnucash-xml:2.2.1
```

and, when an Excel file is wanted, also:

```java
//DEPS com.druvu:druvu-lib-excel:1.0.0
```

## Rules

- **Write every import.** Use only the classes and methods on this page. Do not invent others.
- **The file name and the class name must be the same word** — `class Report` goes in `Report.java`. Java refuses
  anything else, and the user chooses the name, so **tell them the exact file name to save your answer under**, and
  give the run line to match.
- **The book is the first argument**, opened with `AccStore.load(Path.of(args[0]))`. That is **read-only**: nothing in
  the user's book can change. Say so — it is the thing they worry about.
- **"The total" is never the whole book.** A balanced book sums to **zero**, so the root account's total is always
  `0.00` and saying so is useless. If a total is asked for without saying of what, assume the **EXPENSE** accounts,
  and state in one line which accounts you added up.
- **Never guess account names.** They may be anything, in any language. Select by `AccountType` (`EXPENSE`, `INCOME`,
  `BANK`), or print the names first and let the user say which one they mean.
- **Money is `BigDecimal`.** When you divide, give a scale and a rounding mode:
  `a.divide(b, 2, RoundingMode.HALF_UP)`.
- **Print the library's `Amount`, never `printf("%.2f")`** — `Amount` prints as `19911.00 CHF` on every machine, while
  `%f` follows the computer's language and turns into `19911,00` on many of them.
- **Print messages with `System.out.println`.**
- **Give back the whole file, ready to save**, then the one line that runs it:
  `jbang FileName.java mybook.gnucash`.

## Reading the book

```java
import com.druvu.acc.api.AccStore;
import com.druvu.acc.api.entity.Account;        // also here: AccountType, Transaction, Split, Amount
import com.druvu.acc.api.service.AccountService;

AccStore store = AccStore.load(Path.of(args[0]));
```

| Call | Answers |
|---|---|
| `store.accounts()` | `List<Account>` — every account in the book |
| `store.accountById(id)` | `Optional<Account>` |
| `store.transactions()` | `List<Transaction>` |
| `store.transactions(from, to)` | the same, between two `LocalDate`s |
| `store.transactionsForAccount(accountId)` | only those touching one account |
| `store.splitsForAccount(accountId)` | `List<Split>` |

```
Account      id()  name()  type() -> AccountType   placeholder() -> boolean   parentId() -> Optional<String>
AccountType  ROOT BANK CASH CREDIT ASSET LIABILITY STOCK MUTUAL CURRENCY INCOME EXPENSE EQUITY RECEIVABLE PAYABLE
Transaction  id()  datePosted() -> LocalDate   description() -> String   splits() -> List<Split>
Split        accountId() -> String   value() -> BigDecimal   datePosted() -> LocalDate
```

**Signs.** A split that adds to an EXPENSE account is positive. INCOME splits are negative — negate them to show
income as a positive number. A **placeholder** account only groups others: nothing is posted to it, so skip it in
reports.

**Balances, as GnuCash shows them:**

```java
AccountService service = AccountService.create(store);

service.balance(accountId)       // Amount - the account's own entries      (GnuCash column "Balance")
service.totalAmount(accountId)   // Amount - the account and all beneath it (GnuCash column "Total")
```

`Amount` is a number plus its currency: `value()` gives the `BigDecimal`, `plus`/`minus` return new amounts and refuse
to mix currencies, and it prints as `1500.00 CHF`.

## Writing an Excel file

```java
import com.druvu.excel.Excel;
import com.druvu.excel.Fill;
import com.druvu.excel.Style;

Excel.sheet("Sheet name", rows)                              // rows: a List (or Stream) of your own record type
     .column("Header", Row::accessor)                        // Excel's default look
     .column("Header", Style.MONEY, Row::accessor)           // one style for the whole column
     .column("Header", row -> aStyleForThisRow, Row::accessor)  // a style chosen row by row
     .save(Path.of("report.xlsx"));
```

`column(...)` returns the sheet, so calls chain; **inside a loop write `sheet = sheet.column(...)`**.

A cell value may be: `String`, an enum, `int`, `long`, `BigDecimal`, `double`, `LocalDate`, `LocalDateTime`,
`boolean`, `null` (an empty cell), or an `Optional` of those. Anything else is refused, naming the column.

```
Styles  Style.NONE  Style.INTEGER  Style.MONEY (12,345.60)  Style.PERCENT  Style.DATE  Style.TIMESTAMP
        refine with .withBold()  .withFill(Fill.ORANGE)  .withFormat("0.0000") - they chain, the original never changes
Fills   Fill.NONE GREEN RED YELLOW ORANGE BLUE LAVENDER GREY, or Fill.of("1F3864")
```

Every sheet already has a bold header that stays in view, a filter on every column, and fitted column widths — do not
build those yourself.

**There is no footer.** A totals row is one more row at the end, styled bold: give the row type a boolean flag that
says "I am the totals row", and let the columns that do not apply to it answer `null`.

## A complete example — every entry of one account, with a bold total

```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//DEPS com.druvu:druvu-acc-gnucash-xml:2.2.1
//DEPS com.druvu:druvu-lib-excel:1.0.0

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
                    lines.add(new Line(tx.datePosted(), tx.description(), split.value(), false));
                    sum = sum.add(split.value());
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
```

Run it with `jbang AccountRegisterExcel.java mybook.gnucash Groceries`.

---

Full documentation: [druvu-acc](https://github.com/DenissLarka/druvu-acc-parent) ·
[druvu-lib-excel](https://github.com/DenissLarka/druvu-lib-excel) · [druvu.com](https://druvu.com)

Something you needed and could not get? [Tell us](https://github.com/DenissLarka/druvu-acc-parent/issues) — what gets
built next is decided by whoever turns up and asks.
