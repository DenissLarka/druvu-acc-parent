# Accounting Library

[![CI](https://github.com/DenissLarka/druvu-acc-parent/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/DenissLarka/druvu-acc-parent/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/DenissLarka/druvu-acc-parent?label=GitHub%20Packages&color=blue)](https://github.com/DenissLarka/druvu-acc-parent/packages)
![Java](https://img.shields.io/badge/Java-25-blue)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue)](LICENSE)

A modular Java library for reading and writing accounting data. The library provides a clean API for working with double-entry bookkeeping data including accounts, transactions, commodities, and prices.

Project page: [druvu.com/projects/druvu-acc](https://druvu.com/projects/druvu-acc.html)

## Features

- **Modular JPMS Design** - Full Java Platform Module System support
- **Pluggable Store Implementations** - Support for multiple accounting file formats via ServiceLoader
- **Record-based Entities** - Immutable data entities using Java records
- **GnuCash Support** - Read *and write* GnuCash XML files (plain and gzip-compressed)

## Supported entities

GnuCash files can hold many entity types. The table below tracks what this library supports
today against the GnuCash XML v2 data model. The core double-entry entities and the
investment/multi-currency entities are covered; the business (accounts-receivable/payable)
and planning entities are not yet implemented.

Editing an entity **preserves everything this library does not model**: an update writes only the
fields it understands onto the record already in the file, so GnuCash's own extensions — including
custom key-value "slots" and the entities in the *not yet* rows — survive a load-modify-save
unchanged.

| Entity | In GnuCash | druvu-acc |
|---|:---:|---|
| Accounts | ✓ | **read + write** |
| Transactions & splits | ✓ | **read + write** |
| Commodities (currencies & securities) | ✓ | **read + write** |
| Prices (price database) | ✓ | **read + write** |
| Account flags (placeholder, hidden, notes, colour) | ✓ | **read + write** |
| Scheduled (recurring) transactions | ✓ | — *not yet* |
| Budgets | ✓ | — *not yet* |
| Customers | ✓ | — *not yet* |
| Vendors | ✓ | — *not yet* |
| Employees | ✓ | — *not yet* |
| Invoices & bills (+ line entries) | ✓ | — *not yet* |
| Jobs | ✓ | — *not yet* |
| Orders | ✓ | — *not yet* |
| Billing terms | ✓ | — *not yet* |
| Tax tables | ✓ | — *not yet* |
| Lots | ✓ | — *not yet* |

## Which direction next?

The service reads and writes the core of a GnuCash book, and several roads lead on from here.
What gets built next is decided the honest way: by whoever turns up and asks. So, what do you
want? [Open an issue](https://github.com/DenissLarka/druvu-acc-parent/issues) (or 👍 an existing
one) and say what you'd use it for:

- **More GnuCash entities** — the *not yet* rows above: business documents (customers, vendors,
  invoices & bills, payments), planning (scheduled transactions, budgets), investment lots.
- **A desktop UI** — a lightweight companion for browsing and editing books.
- **Reports** — balance sheet, income statement, PDF/HTML export.
- **A database backend** — the API is storage-agnostic by design; a SQL store (or reading
  GnuCash's own SQLite format) would be a natural second implementation.
- **Something that isn't on this list** — often the best kind.

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

- Java 25+
- Maven 3.9+

## Usage

### Reading a GnuCash File

```java
var store = AccStore.load(Path.of("myfile.gnucash"));   // format found via ServiceLoader

for (var account : store.accounts()) {
    System.out.printf("%-24s %s%n", account.name(), account.type());
}

for (var tx : store.transactions()) {
    System.out.printf("%s  %s%n", tx.datePosted(), tx.description());
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
    System.out.println(amount);          // 1500.00 CHF, then 100 NASDAQ/AAPL
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

> **Note:** the library does not enforce accounting invariants (e.g. that a transaction's
> splits balance, or that referenced accounts exist) — supply consistent data. `save` writes
> the GnuCash XML and keeps the file's `count-data` headers in sync with its contents.

> ⚠️ **On preserving the whole file.** `save` rewrites the book from this library's own model of the
> GnuCash format, so anything that model does not cover is **not** carried over. Entities the library
> does not yet support (see the table above) *are* preserved, as are slot keys it does not model —
> that is covered by tests against books written by GnuCash itself, including the business entities.
> But **full fidelity for every possible GnuCash file is not guaranteed**: a construct from a version
> newer than this library knows about can be dropped without warning.
>
> **Keep a backup of any book you write to**, and if you find something lost in a save, please
> [open an issue](https://github.com/DenissLarka/druvu-acc-parent/issues) with the element name — that
> is a bug worth fixing, and it is usually a one-line schema addition.

### Running the Example

Run `AccApiReadExample` to print account balances and transactions from a GnuCash file:

```powershell
./Start-Example.ps1 path/to/myfile.gnucash
```

## Installation

This library is published to **GitHub Packages**. To use it, you need to configure Maven authentication.

**1. Generate a GitHub Personal Access Token:**

Create a **classic** token with the single `read:packages` scope — nothing more is needed to
consume a public package.
[This link](https://github.com/settings/tokens/new?scopes=read:packages&description=maven-read-packages)
opens the form with the right type and scope pre-selected.

> Note: it must be a *classic* token. GitHub's token page defaults to the newer fine-grained
> tokens, which the GitHub Packages Maven registry does not accept — the symptom is an
> unexplained `401 Unauthorized` from `maven.pkg.github.com`.

**2. Add the server to `~/.m2/settings.xml`:**

```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>YOUR_GITHUB_TOKEN</password>
    </server>
  </servers>
</settings>
```

**3. Add the repository and dependency to your project `pom.xml`:**

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/DenissLarka/druvu-acc-parent</url>
    </repository>
</repositories>
```

```xml
<dependency>
    <groupId>com.druvu</groupId>
    <artifactId>druvu-acc-api</artifactId>
    <version>2.0.0</version>
</dependency>

<!-- GnuCash XML support (optional) -->
<dependency>
    <groupId>com.druvu</groupId>
    <artifactId>druvu-acc-gnucash-xml</artifactId>
    <version>2.0.0</version>
</dependency>
```

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
