# druvu-acc

[![CI](https://github.com/DenissLarka/druvu-acc-parent/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/DenissLarka/druvu-acc-parent/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.druvu/druvu-acc-api.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/com.druvu/druvu-acc-api)
![Java](https://img.shields.io/badge/Java-25-blue)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue)](LICENSE)

A modular Java library for reading and writing accounting data - today, **GnuCash** files. The library provides a clean API for working with double-entry bookkeeping data including accounts, transactions, commodities, and prices.

Project page: [druvu.com/projects/druvu-acc](https://druvu.com/projects/druvu-acc.html)

> 📖 **New to the library? [Read how to use it](docs/README.md)** — three small accounting
> stories, each solved by a complete program that runs with one `jbang` command: a household's
> books, a contractor's invoice with VAT, and finding the customer behind a ledger transaction.

## Quick start: one file, no project

Save this as `Balances.java` and run `jbang Balances.java mybook.gnucash`.
[JBang](https://www.jbang.dev) fetches Java 25 and the library from Maven Central by itself - there
is nothing else to install or configure.

```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//DEPS com.druvu:druvu-acc-gnucash-xml:2.2.1

import com.druvu.acc.api.AccStore;
import com.druvu.acc.api.service.AccountService;
import java.nio.file.Path;

public class Balances {

    public static void main(String[] args) throws Exception {
        var store = AccStore.load(Path.of(args[0]));   // read-only: the file cannot be changed
        var service = AccountService.create(store);

        for (var account : store.accounts()) {
            System.out.printf("%-28s %s%n", account.name(), service.balance(account.id()));
        }
    }
}
```

`AccStore.load` opens the book read-only; to change one, see [Writing and Modifying](#writing-and-modifying).
The API lives in three packages: `com.druvu.acc.api` (`AccStore`, `WritableAccStore`),
`com.druvu.acc.api.entity` (`Account`, `Transaction`, `Split`, `Amount`, `Customer`, `Invoice`, ...)
and `com.druvu.acc.api.service` (`AccountService`). The snippets further down print with
`IO.println` - that is `java.lang.IO`, standard since Java 25; `System.out.println` works just as well.

## Features

- **Modular JPMS Design** - Full Java Platform Module System support
- **Pluggable Store Implementations** - Support for multiple accounting file formats via ServiceLoader
- **Record-based Entities** - Immutable data entities using Java records
- **GnuCash Support** - Read *and write* GnuCash XML files (plain and gzip-compressed)

## What it covers, and what decides the rest

The whole of a GnuCash book except its planning side: accounts, transactions, commodities and
prices; customers, vendors, employees, jobs, orders, invoices and bills, billing terms, tax tables,
lots. Scheduled transactions and budgets are out of scope - they are GnuCash's planning tools, not
bookkeeping - and are left exactly as GnuCash wrote them. Whatever the library does not model
survives a load-modify-save untouched - an update writes only the fields it understands.

What gets built next is decided by whoever turns up and asks. If you use this, or tried to and
could not, [open an issue](https://github.com/DenissLarka/druvu-acc-parent/issues) and say what
you were after. The business entities and the lots both started as somebody's question.

## Modules

```
druvu-acc-parent
├── druvu-acc-api           # Core API: AccStore interface, entities, services
├── druvu-acc-gnucash-xml   # GnuCash XML format implementation
└── druvu-acc-tests         # Integration tests and examples
```

### druvu-acc-api

Core interfaces and entities for accounting data:

**Main Interface:**
- `AccStore` - Main entry point for accessing accounting data (accounts, transactions, splits, prices)

**Entity Records:**
- `Account` - Account with id, name, type, code, description, commodity, and parentId
- `Transaction` - Transaction with currency, date, description, and splits
- `Split` - Transaction split with value, quantity, and reconciliation state
- `Price` - Price quote for commodities
- `CommodityId` - Identifies currencies and securities (namespace + id); constants for the
  common currencies (`CommodityId.USD`, `.EUR`, `.GBP`, `.CHF`, `.JPY`), anything else via
  `CommodityId.currency(code)`
- `Amount` - A quantity of one commodity (1500.00 CHF, or 100 NASDAQ/AAPL shares)
- `MultiAmount` - A quantity held across one or more commodities, returned by subtree totals
- `AccountType` - Enum for account types (ASSET, LIABILITY, INCOME, EXPENSE, EQUITY, etc.)
- `ReconcileState` - Reconciliation state (NOT_RECONCILED, CLEARED, RECONCILED)

**Account flags:**
- `placeholder()` - a grouping account that transactions may not be posted to
- `hidden()`, `taxRelated()`, `notes()`, `color()` - and their `with...` counterparts

  GnuCash stores these in its key-value "slots" extension, with quirks of its own (there is no boolean
  slot type; `false` is stored by deleting the key). None of that reaches the API: they are ordinary
  typed properties here, and how any given format records them is the backend's business.

**Validation:**
- `AccStore.validate()` - reports structural problems (a second root, a dangling parent, a split on a
  missing account). Reading is tolerant so a damaged book can still be inspected or repaired;
  `save(Path)` is strict and refuses to write a book that fails these checks.

**Services:**
- `AccountService` - Business logic for account operations (own balances and subtree totals)
- `AccStore.load(Path)` / `AccStore.loadWritable(Path)` - static factory methods that load a store via ServiceLoader

### druvu-acc-gnucash-xml

Implementation for reading and writing GnuCash XML files (`.gnucash`). Supports both plain XML and gzip-compressed files.

## Requirements

- Java 25+ (a JBang script fetches it by itself)
- Maven 3.9+ - only to build the library from source

## Usage

### Reading a GnuCash File

```java
var store = AccStore.load(Path.of("myfile.gnucash"));   // format found via ServiceLoader

for (var account : store.accounts()) {
    IO.println("%-24s %s".formatted(account.name(), account.type()));
}

for (var tx : store.transactions()) {
    IO.println("%s  %s".formatted(tx.datePosted(), tx.description()));
}
```

### Balances

```java
var service = AccountService.create(store, "Root Account");
var revenue = service.accountByName("Revenue");

var balance = service.balance(revenue.id());                             // 1500.00 CHF
var total = service.totalAmount(revenue.id());                           // incl. sub-accounts
var atYearEnd = service.balance(revenue.id(), LocalDate.of(2026, 12, 31));
```

Balances carry their commodity, so a figure can never be read as the wrong currency: `Amount` is a
quantity plus its `CommodityId`, and it prints as `1500.00 CHF`.

The two methods mirror the two columns of the GnuCash account tree:

| Method | GnuCash column | Covers |
|---|---|---|
| `balance(accountId)` | **Balance** | the account's own splits only |
| `totalAmount(accountId)` | **Total** | the account and everything beneath it |

#### When a subtree mixes commodities

A subtree can hold more than one commodity — a EUR savings account and a NASDAQ/AAPL stock account
under the same parent — and adding those into one number would be meaningless. Most books are
single-currency, so `totalAmount` is the everyday call; it throws if that assumption breaks, naming
the account and what it actually holds.

For a book that genuinely mixes them, `totalBalance` returns a `MultiAmount` — one `Amount` per
commodity, nothing converted and nothing dropped, because applying a rate needs a rate source and a
policy for missing quotes, which is your decision and not the library's:

```java
var assets = service.accountByName("Assets");

for (var amount : service.totalBalance(assets.id()).amounts()) {
    IO.println(amount);                  // 1500.00 CHF, then 100 NASDAQ/AAPL
}
```

Use `store.prices()` if you do want to collapse a mixed total into one currency.

Both types are immutable: `plus`/`minus`/`negate` return new instances, and `MultiAmount.summing()`
is a collector for aggregating a stream of them. `Amount` arithmetic refuses to mix commodities, and
equality is by numeric value rather than `BigDecimal` scale — a figure read from `1500/1` equals the
same figure read from `150000/100`.

### Working with Commodities

`CommodityId` refers to a commodity; `Commodity` defines one in the book's commodity table.

```java
var chf = CommodityId.CHF;                        // constants for USD, EUR, GBP, CHF, JPY
var pln = CommodityId.currency("PLN");            // anything else by code
var apple = CommodityId.security("NASDAQ", "AAPL");

var plnDef = Commodity.currency("PLN");           // definition; fraction read from ISO 4217
var appleDef = Commodity.security("NASDAQ", "AAPL", "Apple Inc.", 10000);
```

Definitions go into a book with `addCommodity` on a writable store, below.

`Commodity.currency` refuses codes ISO defines no fraction for — crypto, pseudo-currencies such as
XAU — rather than guessing a precision; construct those directly with the fraction they use.

### Writing and Modifying

Load a store as a `WritableAccStore` to mutate it in place, then `save(Path)`. Use `store.newId()`
for entity IDs — it mints one in whatever format the backend expects, so you never hand-roll a GUID.

A book can also start from nothing — no file, no GnuCash involved:

```java
var store = AccStore.newBook(CommodityId.CHF);   // one root account, the currency, nothing else
```

The currency is required rather than defaulted (every account must be denominated in a commodity
the book defines); further currencies are added with `addCommodity`.

```java
var store = AccStore.loadWritable(Path.of("myfile.gnucash"));
var rootId = store.rootAccounts().getFirst().id();
var eur = CommodityId.EUR;

// An expense account under the root
var accountId = store.newId();
store.addAccount(Account.of(accountId, "Coffee", AccountType.EXPENSE)
        .withDescription("Daily coffee")
        .withCommodity(eur)
        .withParent(rootId));

// The cash it is paid from
var cashId = store.newId();
store.addAccount(Account.of(cashId, "Cash", AccountType.CASH)
        .withCommodity(eur)
        .withParent(rootId));

// A balanced transaction between the two. Both legs must be real accounts —
// the root holds the tree, never money.
var txId = store.newId();
var today = LocalDate.now();
var price = new BigDecimal("4.50");
store.addTransaction(Transaction.of(txId, eur, today, "Morning coffee", List.of(
        Split.of(store.newId(), txId, accountId, today, price),
        Split.of(store.newId(), txId, cashId, today, price.negate()))));

// A security and a price quote
store.addCommodity(Commodity.security("NASDAQ", "AAPL", "Apple Inc.", 10000));
store.addPrice(new Price(store.newId(), CommodityId.security("NASDAQ", "AAPL"), CommodityId.USD,
        today.atStartOfDay(), "user:price-editor", Optional.of("last"), new BigDecimal("212.50")));

// Editing an existing entity: change a copy, then put it back.
store.updateAccount(store.accountById(accountId).orElseThrow()
        .withPlaceholder(true)
        .withNotes("groups the drink accounts"));

store.removeTransaction(txId);
store.removeAccount(accountId);

store.save(Path.of("myfile-modified.gnucash"));   // gzipped for .gnucash and .gz
```

`save` validates the book's structure first and writes nothing if it fails — a second root, a
dangling parent, a split on an account that is not there or on the root itself. Reading stays tolerant so a damaged book
can still be opened and repaired; call `store.validate()` yourself to see what is wrong with one.

> **Note:** `addTransaction` and `updateTransaction` enforce double entry's core invariant — the
> splits' *values* must sum to zero (quantities are exempt: a share purchase legitimately moves an
> unequal share count against money). Other accounting invariants are checked at `save`/`validate`
> time, structural ones on the spot. `save` writes the GnuCash XML and keeps the file's
> `count-data` headers in sync with its contents.

> ⚠️ **On preserving the whole file.** `save` rewrites the book from this library's own model of the
> GnuCash format, so anything that model does not cover is **not** carried over. Entities the library
> does not model (scheduled transactions, budgets) *are* preserved, as are slot keys it does
> not model — that is covered by tests against books written by GnuCash itself.
> But **full fidelity for every possible GnuCash file is not guaranteed**: a construct from a version
> newer than this library knows about can be dropped without warning.
>
> **Keep a backup of any book you write to**, and if you find something lost in a save, please
> [open an issue](https://github.com/DenissLarka/druvu-acc-parent/issues) with the element name — that
> is a bug worth fixing, and it is usually a one-line schema addition.

### Business documents

The business entities cover GnuCash's accounts-receivable/payable side. Parties are built with
factories and refined with `with...` methods; an address attaches fluently:

```java
WritableAccStore store = AccStore.loadWritable(Path.of("business.gnucash"));

String termId = store.newId();
store.addBillTerm(BillTerm.netDays(termId, "Net 30", 30));

store.addCustomer(Customer.of(store.newId(), "C-100", "ACME AG", CommodityId.CHF)
        .withAddress(Address.of("Bahnhofstrasse 1", "8001 Zurich").withEmail("billing@acme.example"))
        .withTerms(termId)
        .withCreditLimit(new BigDecimal("5000")));
```

Documents follow the same shape - an invoice belongs to an owner (a customer, or a job that
resolves to one), and its lines are entries:

```java
String invoiceId = store.newId();
store.addInvoice(Invoice.of(invoiceId, "INV-100", Owner.customer(customerId),
        LocalDateTime.now(), CommodityId.CHF).withTerms(termId));

store.addEntry(Entry.of(store.newId(), LocalDateTime.now(), "Consulting", new BigDecimal("3"))
        .withAction("Hours")
        .withInvoiceLine(InvoiceLine.of(invoiceId, incomeAccountId, new BigDecimal("150"))));
```

And the question that prompted the feature - *which customer is behind this ledger transaction?* -
is one call, for a posted invoice and for the payment that settles it alike. It follows GnuCash's
posting link, the receivable lot behind a payment, and the job indirection:

```java
Optional<Customer> customer = store.customerForTransaction(transactionId);
```

The lots themselves are readable and writable. A lot groups splits of one account: GnuCash pairs a
posted document with its payments in one, and ties a purchase of shares to the sales that consume
it in another. Whether a lot is settled is not stored anywhere - it is the sum of its splits:

```java
// Is the posted invoice paid? GnuCash keeps no flag: a lot is settled when its splits sum to zero.
String lotId = invoice.posting().flatMap(Invoice.Posting::lotId).orElseThrow();
BigDecimal open = store.splitsInLot(lotId).stream()
        .map(Split::value)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
boolean paid = open.signum() == 0;

// A lot of your own - only splits of the lot's account can join it, the rule GnuCash enforces.
String purchaseLot = store.newId();
store.addLot(Lot.of(purchaseLot, brokerageAccountId, "ACME bought 2026-03-02"));
store.assignSplitToLot(purchaseSplitId, purchaseLot);
```

GnuCash's own posting mechanics belong to GnuCash: this library reads a posted document's trace and
the lots it leaves behind, but neither posts nor applies payments. Tax tables and billing terms have
no in-place update, deliberately - GnuCash freezes an invisible copy of a table that is in use so
posted documents keep their rates, and an in-place edit would falsify them.

### Running the Example

Run `AccApiReadExample` to print account balances and transactions from a GnuCash file:

```powershell
./Start-Example.ps1 path/to/myfile.gnucash
```

## Installation

On **Maven Central** from version 2.2.0 - use 2.2.0 or newer. No extra repository, no credentials.

One dependency is enough to work with GnuCash files; it brings the API with it:

```xml
<dependency>
    <groupId>com.druvu</groupId>
    <artifactId>druvu-acc-gnucash-xml</artifactId>
    <version>2.2.1</version>
</dependency>
```

The same thing elsewhere:

```
//DEPS com.druvu:druvu-acc-gnucash-xml:2.2.1                  (JBang script)
implementation("com.druvu:druvu-acc-gnucash-xml:2.2.1")       (Gradle)
```

`druvu-acc-api` on its own is for code that must not depend on a file format, or for writing
another backend. By itself it cannot open a file: `AccStore.load` finds the format implementation
on the classpath, and `druvu-acc-gnucash-xml` is that implementation.

## Building

```bash
mvn clean install
```

## Running Tests

```bash
mvn test
```


## License

[Apache License Version 2.0](LICENSE)
