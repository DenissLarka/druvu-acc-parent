# Accounting Library

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

| Entity | In GnuCash | druvu-acc |
|---|:---:|---|
| Accounts | ✓ | **read + write** |
| Transactions & splits | ✓ | **read + write** |
| Commodities (currencies & securities) | ✓ | **read + write** |
| Prices (price database) | ✓ | **read + write** |
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

> **Which entity should come next?** If you need one of the *not yet* rows, please
> [open an issue](https://github.com/DenissLarka/druvu-acc-parent/issues) (or vote on an
> existing one) describing your use case. Prioritisation follows real demand.

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
- `CommodityId` - Identifies currencies and securities (namespace + id)
- `AccountType` - Enum for account types (ASSET, LIABILITY, INCOME, EXPENSE, EQUITY, etc.)
- `ReconcileState` - Reconciliation state (NOT_RECONCILED, CLEARED, RECONCILED)

**Services:**
- `AccountService` - Business logic for account operations (balance calculations)
- `AccStore.load(Path)` / `AccStore.loadWritable(Path)` - static factory methods that load a store via ServiceLoader

### druvu-acc-gnucash-xml

Implementation for reading and writing GnuCash XML files (`.gnucash`). Supports both plain XML and gzip-compressed files.

## Requirements

- Java 25+
- Maven 3.9+

## Usage

### Reading a GnuCash File

```java
import com.druvu.acc.api.AccStore;
import com.druvu.acc.api.entity.Account;
import com.druvu.acc.api.entity.Transaction;

import java.nio.file.Path;

// Load the store (auto-discovers GnuCash implementation via ServiceLoader)
AccStore store = AccStore.load(Path.of("myfile.gnucash"));

// Access accounts
for (Account account : store.accounts()) {
    System.out.println(account.name() + " [" + account.type() + "]");
}

// Access transactions
for (Transaction tx : store.transactions()) {
    System.out.println(tx.datePosted() + " - " + tx.description());
    for (var split : tx.splits()) {
        System.out.println("  " + split.quantity());
    }
}
```

### Using AccountService for Balance Calculations

```java
import com.druvu.acc.api.service.AccountService;
import com.druvu.acc.api.entity.Account;

import java.math.BigDecimal;
import java.time.LocalDate;

// Create service with optional root account prefix
AccountService service = AccountService.create(store, "Root Account");

// Find account by name (relative to root)
Account revenue = service.accountByName("Revenue");

// Calculate balance
BigDecimal currentBalance = service.balance(revenue);

// Calculate balance up to a specific date
BigDecimal historicBalance = service.balance(revenue, LocalDate.of(2026, 1, 1));
```

### Working with Commodities

```java
import com.druvu.acc.api.entity.CommodityId;

// Create a currency ID
CommodityId eur = CommodityId.currency("EUR");
CommodityId usd = new CommodityId("CURRENCY", "USD");

// Create a stock ID
CommodityId stock = new CommodityId("NASDAQ", "AAPL");

// Check if commodity is a currency
boolean isCurrency = eur.isCurrency(); // true
```

### Writing and Modifying

Load a store as a `WritableAccStore` to add or remove accounts and transactions and
save the result. Mutations are applied in place; call `save(Path)` to persist them.
Entity IDs are caller-supplied — use 32-character hex GUIDs for GnuCash compatibility.

```java
import com.druvu.acc.api.AccStore;
import com.druvu.acc.api.WritableAccStore;
import com.druvu.acc.api.entity.*;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Load as writable
WritableAccStore store = AccStore.loadWritable(Path.of("myfile.gnucash"));

String rootId = store.rootAccounts().getFirst().id();
CommodityId eur = CommodityId.currency("EUR");

// Add a new expense account under the root
String accountId = UUID.randomUUID().toString().replace("-", "");
store.addAccount(new Account(
        accountId, "Coffee", AccountType.EXPENSE,
        Optional.empty(), Optional.of("Daily coffee"),
        Optional.of(eur), Optional.of(rootId)));

// Add a balanced transaction (splits must sum to zero in the transaction currency)
String txId = UUID.randomUUID().toString().replace("-", "");
LocalDate today = LocalDate.now();
List<Split> splits = List.of(
        new Split(UUID.randomUUID().toString().replace("-", ""), txId, accountId, today,
                ReconcileState.NOT_RECONCILED, Optional.empty(),
                new BigDecimal("4.50"), new BigDecimal("4.50")),
        new Split(UUID.randomUUID().toString().replace("-", ""), txId, rootId, today,
                ReconcileState.NOT_RECONCILED, Optional.empty(),
                new BigDecimal("-4.50"), new BigDecimal("-4.50")));
store.addTransaction(new Transaction(
        txId, eur, Optional.empty(), today, "Morning coffee", splits));

// Add a security commodity and a price quote (investments / multi-currency)
store.addCommodity(Commodity.security("NASDAQ", "AAPL", "Apple Inc.", 10000));
store.addPrice(new Price(
        UUID.randomUUID().toString().replace("-", ""),
        new CommodityId("NASDAQ", "AAPL"),
        CommodityId.currency("USD"),
        today.atStartOfDay(),
        "user:price-editor",
        Optional.of("last"),
        new BigDecimal("212.50")));

// Remove entities by ID
store.removeTransaction(txId);
store.removeAccount(accountId);

// Persist (gzip-compressed when the path ends with .gnucash or .gz)
store.save(Path.of("myfile-modified.gnucash"));
```

> **Note:** the library does not enforce accounting invariants (e.g. that a transaction's
> splits balance, or that referenced accounts exist) — supply consistent data. `save` writes
> the GnuCash XML and keeps the file's `count-data` headers in sync with its contents.

### Running the Example

Run `AccApiReadExample` to print account balances and transactions from a GnuCash file:

```powershell
./run-example.ps1 path/to/myfile.gnucash
```

## Installation

This library is published to **GitHub Packages**. To use it, you need to configure Maven authentication.

**1. Generate a GitHub Personal Access Token:**

Go to [GitHub Settings > Developer settings > Personal access tokens](https://github.com/settings/tokens) and create a token with the `read:packages` scope.

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
    <version>1.1.0</version>
</dependency>

<!-- GnuCash XML support (optional) -->
<dependency>
    <groupId>com.druvu</groupId>
    <artifactId>druvu-acc-gnucash-xml</artifactId>
    <version>1.1.0</version>
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
