# Learn druvu-acc by doing the books

Three small stories, told the way an accountant sees them — and solved in a few lines of Java.
Each page is a complete, runnable program: you can copy it, point it at a GnuCash file, and open
the result in GnuCash to see what it did.

No prior Java-plus-accounting experience is assumed beyond the basics of each: if you know what
double-entry bookkeeping is and can run a small Java program, you are the reader these pages were
written for.

## The use cases

1. **[The house owner's books](household-books.md)** — a salary, a rent, some groceries.
   Build a small account tree, record a month of money movements, and answer the question every
   household asks: *what did this month actually cost?*

2. **[The contractor sends a bill](contractor-bill.md)** — a finished kitchen, a client,
   16 hours of labour and a worktop, VAT on top, payable in 30 days. Build the customer, the job
   and the invoice in code, then post it from GnuCash itself.

3. **[Whose money is this?](whose-money.md)** — the bank statement shows a payment; which
   customer is behind it? One call resolves any ledger transaction back to the customer it
   belongs to — the first step towards per-customer reporting.

Read them in order — each story continues where the previous one ends.

## Run a story in one command

Each story is also a single Java file next to this page — `HouseholdBooks.java`,
`ContractorBill.java`, `WhoseMoney.java` — that runs with [JBang](https://www.jbang.dev), which
fetches the library from Maven Central and the right Java on its own. Install JBang once:

```bash
# macOS / Linux
curl -Ls https://sh.jbang.dev | bash -s - app setup
```

```powershell
# Windows (PowerShell)
iex "& { $(iwr -useb https://ps.jbang.dev) } app setup"
```

then, in a folder of your choice, run the file you downloaded — or run it straight from GitHub:

```bash
jbang HouseholdBooks.java
jbang https://github.com/DenissLarka/druvu-acc-parent/blob/main/docs/HouseholdBooks.java
```

No project, no build file, nothing to add to a classpath. If you already have a Maven or Gradle
project, the two coordinates are in [Installation](../README.md#installation) instead.

## What you need

- **JBang** (above) — or Java 25 and the library on your classpath if you prefer your own setup.
- **GnuCash** ([gnucash.org](https://www.gnucash.org), free) is recommended, not required: the
  first story starts its book from code with `AccStore.newBook(...)`. But the library reads and
  writes GnuCash's own file format, so GnuCash is the natural place to *look at* what your
  programs wrote — and the later stories use it for the steps that are deliberately its
  (posting an invoice).

One habit worth keeping from day one: **work on a copy of your book**. Every program here writes
to a *new* file and leaves its input untouched — do the same with books you care about.
