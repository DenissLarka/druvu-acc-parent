# Druvu Accounting Library

A modular Java library for reading and processing accounting data. The library provides a clean API for working with double-entry bookkeeping data including accounts, transactions, commodities, and prices.

## Features

- **Modular JPMS Design** - Full Java Platform Module System support
- **Pluggable Store Implementations** - Support for multiple accounting file formats via ServiceLoader
- **Read-Only API** - Safe, immutable access to accounting data
- **Balance Calculations** - Account balances, recursive balances, and currency conversion
- **Clean Separation** - Store layer (raw data access) separate from API layer (business logic)

## Modules

```
druvu-acc-parent
├── druvu-acc-store-parent          # Parent for store implementations
│   ├── druvu-acc-store-api         # Core interfaces for data access
│   └── druvu-acc-store-gnucash-xml # GnuCash XML format implementation
├── druvu-acc-api                   # High-level accounting API
└── druvu-acc-tests                 # Integration tests
```

### druvu-acc-store-api

Core interfaces for accessing accounting data:

- `AccBook` - The accounting book containing all data
- `AccAccount` - Chart of accounts with hierarchy
- `AccTransaction` - Transactions with splits
- `AccTransactionSplit` - Individual journal entries
- `AccCommodity` - Currencies and securities
- `AccPrice` - Price quotes for commodities

### druvu-acc-store-gnucash-xml

Implementation for reading GnuCash XML files (`.gnucash`). Supports both plain XML and gzip-compressed files.

### druvu-acc-api

High-level API with business logic:

- `AccountingBook` - Main entry point with account/transaction lookups
- `AccountingAccount` - Account with balance calculations and hierarchy navigation
- `AccountingTransaction` - Transaction with split access
- `AccountingSplit` - Split with parsed values and running balance
- `CurrencyTable` - Currency conversion using price quotes

## Requirements

- Java 25+
- Maven 3.9+

## Usage

### Reading a GnuCash File

```java
import com.druvu.acc.api.AccountingBook;
import com.druvu.acc.api.DefaultAccountingBook;
import com.druvu.acc.store.gnucash.io.GnucashFileReader;

// Read the file
GnucashFileReader reader = new GnucashFileReader();
var store = reader.read(Path.of("myfile.gnucash"));

// Create the high-level API wrapper
AccountingBook book = new DefaultAccountingBook(store);

// Access accounts
for (var account : book.rootAccounts()) {
    System.out.println(account.qualifiedName() + ": " + account.balanceRecursive());
}

// Access transactions
for (var transaction : book.transactions()) {
    System.out.println(transaction.date() + " - " + transaction.description());
    for (var split : transaction.splits()) {
        System.out.println("  " + split.account().name() + ": " + split.quantity());
    }
}
```

### Using ServiceLoader for Dynamic Loading

```java
import com.druvu.acc.store.loader.AccBookFactory;
import com.druvu.lib.loader.ComponentLoader;

// Load implementation dynamically
var factory = ComponentLoader.load(AccBookFactory.class);
var store = factory.load(Path.of("myfile.gnucash"));
```

### Balance Calculations

```java
// Current balance
BigDecimal balance = account.balance();

// Balance at specific date
BigDecimal historicBalance = account.balance(LocalDate.of(2024, 1, 1));

// Recursive balance (including sub-accounts)
BigDecimal totalBalance = account.balanceRecursive();

// Recursive balance in specific currency
BigDecimal balanceInEur = account.balanceRecursive(
    LocalDate.now(),
    new CommodityId("CURRENCY", "EUR")
);
```

### Currency Conversion

```java
CurrencyTable currencyTable = book.currencyTable();

// Get latest exchange rate
Optional<BigDecimal> rate = currencyTable.getLatestPrice(
    new CommodityId("CURRENCY", "USD"),
    new CommodityId("CURRENCY", "EUR")
);

// Convert amount
Optional<BigDecimal> converted = currencyTable.convert(
    new BigDecimal("100.00"),
    new CommodityId("CURRENCY", "USD"),
    new CommodityId("CURRENCY", "EUR")
);
```

## Building

```bash
mvn clean install
```

## Running Tests

```bash
mvn test
```

## Adding New Store Implementations

To add support for a new accounting file format:

1. Create a new module under `druvu-acc-store-parent`
2. Implement the interfaces from `druvu-acc-store-api`
3. Create an `AccBookFactory` implementation
4. Register it via `provides` in `module-info.java`:

```java
module druvu.acc.store.myformat {
    requires druvu.acc.store.api;

    provides com.druvu.acc.store.loader.AccBookFactory
        with com.mycompany.MyFormatBookFactory;
}
```

## License

[Your license here]
