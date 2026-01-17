# Accounting Library

> **Early Development** - This project is in active early development. Feedback is very welcome! I'm looking to add new core accounting functionality and support for additional storage formats (KMyMoney, SQL databases, etc.). If you have suggestions, feature requests, or want to contribute, please open an issue or reach out.

A modular Java library for reading and processing accounting data. The library provides a clean API for working with double-entry bookkeeping data including accounts, transactions, commodities, and prices.

## Features

- **Modular JPMS Design** - Full Java Platform Module System support
- **Pluggable Store Implementations** - Support for multiple accounting file formats via ServiceLoader
- **Record-based Entities** - Immutable data entities using Java records
- **GnuCash Support** - Read GnuCash XML files (plain and gzip-compressed)

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
- `AccStoreFactory` - Factory for loading AccStore implementations via ServiceLoader

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
import com.druvu.acc.loader.AccStoreFactory;

import java.nio.file.Path;

// Load the store (auto-discovers GnuCash implementation via ServiceLoader)
AccStore store = AccStoreFactory.load(Path.of("myfile.gnucash"));

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

### Running the Example

Run `AccApiReadExample` to print account balances and transactions from a GnuCash file:

```powershell
./run-example.ps1 path/to/myfile.gnucash
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
